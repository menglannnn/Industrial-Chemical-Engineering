package com.wormzjl.icheme.client;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.network.FluidNetwork;
import com.wormzjl.icheme.registry.ModItems;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.LineGeometry;
import com.wormzjl.icheme.science.equipment.PipeRoute;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.PipeAnchor;
import com.wormzjl.icheme.world.level.block.PipeLaying;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The laying tool on the client (batch 2026-10-26-pipe-laying-system, plan 4.1, 4.6 and 4.7). The client decides what a
 * use means, because only it knows whether a line has a start, and sends the server one command: open the settings, lay
 * a route, or dismantle a pipe. It searches the route, draws it as a ghost and reports it on the action bar, and marks
 * the equipment ports around the player. Everything here reads blocks the client already holds: nothing is requested
 * from the server and no process data is read. Render thread only.
 */
@EventBusSubscriber(modid = IChemE.MOD_ID, value = Dist.CLIENT)
public final class PipeLayingClient {
    private static final Object START_SLOT = "icheme:pipe_start", ROUTE_SLOT = "icheme:pipe_route";
    /** A pipe block is one box to outline, a lane at most two arms. */
    private static final Object[] REMOVE_SLOTS = {"icheme:pipe_remove", "icheme:pipe_remove_arm"};
    private static final int START_COLOUR = 0x55FF55, REFUSED_COLOUR = 0xFF5555, CONNECTED_COLOUR = 0x808080, OPEN_COLOUR = 0xFFDD33, FREE_COLOUR = 0x55FF55;
    /** Port classes' outline colours: vapour, bulk, liquid, overflow ({@link EquipmentShape.PortClass} order). */
    private static final int[] PORT_COLOURS = {0xF0F0F0, 0x55DDFF, 0x3377FF, 0xFF9933};
    /** A start this far from the player is dropped, blocks; ports and open ends are marked this far, at most {@link #MAX_MARKS} of each. */
    private static final double DROP_RANGE = 96, PORT_RANGE = 48;
    private static final int MAX_MARKS = 256, RECHECK_TICKS = 20, HUD_TICKS = 5;
    private static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "block/painted_shell");
    private static final RenderType TYPE = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final int GHOST = 0xB0D8FF, ALPHA = 140;

    private record Start(PipeAnchor.Click click, PipeAnchor anchor, ResourceKey<Level> dimension) {
    }

    /** The route to the aimed click, or why there is none; its ghost cells and their joints when found. */
    private record Plan(PipeAnchor.Click end, @Nullable PipeAnchor anchor, PipeRoute.Result route, @Nullable Component refusal, int[] joints, int[] facings) {
    }

    private static @Nullable Start start;
    private static @Nullable Plan plan;
    private static boolean armed = true;
    private static int age;
    /** The outliner slots of the marks shown last tick: ports, open ends and an aimed rack's free slots. */
    private static Set<Object> markSlots = Set.of();

    private PipeLayingClient() {
    }

    public static boolean holding(LocalPlayer player) {
        return tool(player.getMainHandItem()) || tool(player.getOffhandItem());
    }

    private static boolean tool(ItemStack stack) {
        return stack.is(ModItems.PIPE_LAYING_TOOL.get());
    }

    /** One command per press of the use key; a held key does not repeat it. */
    @SubscribeEvent
    public static void use(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (!event.isUseItem() || player == null || mc.level == null || !tool(player.getItemInHand(event.getHand()))) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!armed) return;
        armed = false;
        boolean offHand = event.getHand() == InteractionHand.OFF_HAND;
        var hit = aimed(mc);
        if (player.isShiftKeyDown()) {
            if (start != null) {
                start = null;
                plan = null;
                say(player, PipeLaying.message("cancelled"));
            } else if (hit != null && removable(mc.level, hit) != null) {
                FluidNetwork.dismantlePipe(offHand, hit);
                event.setSwingHand(true);
            } else FluidNetwork.openPipeLaying(offHand);
            return;
        }
        if (hit == null) return;
        if (start == null) {
            var resolved = PipeAnchor.resolve(mc.level, hit);
            var refusal = resolved.anchor() == null ? resolved.refusal() : resolved.anchor().refuses(PipeSizes.outside(settingsDiameter(player)));
            if (refusal != null) {
                say(player, PipeLaying.message("anchor." + name(refusal)));
                return;
            }
            start = new Start(hit, resolved.anchor(), mc.level.dimension());
            plan = null;
            say(player, PipeLaying.message("started", describe(resolved.anchor())));
            return;
        }
        var current = plan(mc.level, hit, settingsDiameter(player));
        plan = current;
        if (current.refusal != null) {
            say(player, current.refusal);
            return;
        }
        var cells = current.route.cells().stream().map(c -> new BlockPos(c.x(), c.y(), c.z())).toList();
        FluidNetwork.layPipe(offHand, start.click, hit, cells, current.route.slots());
        event.setSwingHand(true);
        start = null;
        plan = null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        var outliner = Outliner.getInstance();
        if (!mc.options.keyUse.isDown()) armed = true;
        if (player == null || mc.level == null || !holding(player)) {
            if (start != null || plan != null || !markSlots.isEmpty()) {
                start = null;
                plan = null;
                for (var slot : List.of(START_SLOT, ROUTE_SLOT, REMOVE_SLOTS[0], REMOVE_SLOTS[1])) outliner.remove(slot);
                for (var slot : markSlots) outliner.remove(slot);
                markSlots = Set.of();
            }
            return;
        }
        age++;
        if (start != null && (start.dimension != mc.level.dimension() || start.click.clicked().getCenter().distanceToSqr(player.position()) > DROP_RANGE * DROP_RANGE)) {
            start = null;
            plan = null;
            say(player, PipeLaying.message("dropped"));
        }
        var hit = mc.screen == null ? aimed(mc) : null;
        if (start != null) {
            if (hit == null) plan = null;
            else if (plan == null || !same(mc.level, plan.end, hit) || age % RECHECK_TICKS == 0) plan = plan(mc.level, hit, settingsDiameter(player));
            outliner.showAABB(START_SLOT, new AABB(start.anchor.cell())).colored(START_COLOUR).lineWidth(1 / 16F);
            if (plan != null && plan.refusal != null)
                outliner.showLine(ROUTE_SLOT, start.anchor.cell().getCenter(), (plan.anchor != null ? plan.anchor.cell() : plan.end.clicked()).getCenter()).colored(REFUSED_COLOUR).lineWidth(1 / 16F);
            else outliner.remove(ROUTE_SLOT);
            if (age % HUD_TICKS == 0) say(player, hud(player));
        } else {
            plan = null;
            outliner.remove(START_SLOT);
            outliner.remove(ROUTE_SLOT);
        }
        // While Shift is held, the pipe or lane a use would dismantle is outlined in red.
        var removed = start == null && player.isShiftKeyDown() && hit != null ? removable(mc.level, hit) : null;
        for (int i = 0; i < REMOVE_SLOTS.length; i++)
            if (removed != null && i < removed.size()) outliner.showAABB(REMOVE_SLOTS[i], removed.get(i)).colored(REFUSED_COLOUR).lineWidth(1 / 16F);
            else outliner.remove(REMOVE_SLOTS[i]);
        if (start == null && hit != null && age % HUD_TICKS == 0) {
            var label = portLabel(mc.level, hit);
            if (label != null) say(player, label);
        }
        var shown = new HashSet<Object>();
        ports(mc.level, player, outliner, shown);
        openEnds(mc.level, player, outliner, shown);
        freeSlots(mc.level, hit, outliner, shown);
        for (var slot : markSlots) if (!shown.contains(slot)) outliner.remove(slot);
        markSlots = shown;
    }

    private static @Nullable PipeAnchor.Click aimed(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return new PipeAnchor.Click(hit.getBlockPos(), hit.getDirection(), hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos())));
    }

    /** The rack lane and arm a click picks, as one number; -1 off a lane, 0 on any other block. */
    private static int lanePick(Level level, PipeAnchor.Click click) {
        if (!(level.getBlockEntity(click.clicked()) instanceof PipeRackBlockEntity rack)) return 0;
        var pick = rack.pick(click.local().x, click.local().y, click.local().z);
        return pick.lane() < 0 ? -1 : pick.lane() * 8 + pick.face();
    }

    /** Two clicks that give the same anchor: the same block and face, and on a rack the same lane and arm. */
    private static boolean same(Level level, PipeAnchor.Click a, PipeAnchor.Click b) {
        return a.clicked().equals(b.clicked()) && a.face() == b.face() && lanePick(level, a) == lanePick(level, b);
    }

    /** The boxes of what a dismantle at {@code click} removes, in world coordinates: a pipe block or a rack lane's arms; null for anything else. */
    private static @Nullable List<AABB> removable(Level level, PipeAnchor.Click click) {
        var at = click.clicked();
        if (level.getBlockEntity(at) instanceof PipeRackBlockEntity rack) {
            var pick = rack.pick(click.local().x, click.local().y, click.local().z);
            if (pick.lane() < 0) return null;
            return RackGeometry.boxes(rack.drawnLanes().get(pick.lane()), rack.lanes().get(pick.lane()).facing()).stream()
                    .map(b -> new AABB(b[0], b[1], b[2], b[3], b[4], b[5]).move(at)).toList();
        }
        return level.getBlockState(at).getBlock() instanceof FluidDeviceBlock block && block.kind() == TopologyCompiler.Kind.PIPE ? List.of(new AABB(at)) : null;
    }

    /** The slots a line of the tool's size takes in a rack. */
    private static int width(double diameter) {
        return RackGeometry.width(PipeSizes.outside(diameter));
    }

    private static double settingsDiameter(LocalPlayer player) {
        var stack = tool(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
        var settings = stack.get(ModItems.PIPE_LAYING_SETTINGS.get());
        return settings == null ? .05 : settings.diameter();
    }

    /** The route from the start to {@code end}, searched now on this thread within the search limits. */
    private static Plan plan(Level level, PipeAnchor.Click end, double diameter) {
        var none = new int[0];
        var nothing = new PipeRoute.Result(List.of(), List.of(), null);
        var resolved = PipeAnchor.resolve(level, end);
        if (resolved.anchor() == null) return new Plan(end, null, nothing, PipeLaying.message("anchor." + name(resolved.refusal())), none, none);
        int width = width(diameter);
        var from = start.anchor;
        for (var anchor : List.of(from, resolved.anchor()))
            if (anchor.refuses(PipeSizes.outside(diameter)) != null) return new Plan(end, resolved.anchor(), nothing, PipeLaying.message("anchor." + name(anchor.refuses(PipeSizes.outside(diameter)))), none, none);
        int heading = from.heading(resolved.anchor().end().cell());
        var route = PipeRoute.search(from.end(), heading, resolved.anchor().end(), width, PipeAnchor.space(level), PipeRoute.Limits.DEFAULT);
        if (!route.found()) return new Plan(end, resolved.anchor(), route, PipeLaying.message("route." + name(route.refusal())), none, none);
        var joints = PipeRoute.laidJoints(route.cells(), from.end(), resolved.anchor().end());
        return new Plan(end, resolved.anchor(), route, null, joints, PipeRoute.facings(route.cells(), heading));
    }

    /** Start → end, the length, the elbows, the size and the pipes needed; or why the aimed spot takes no line. */
    private static Component hud(LocalPlayer player) {
        if (plan == null) return PipeLaying.message("hud.aim", describe(start.anchor));
        if (plan.refusal != null) return plan.refusal;
        int cells = plan.route.cells().size();
        double diameter = settingsDiameter(player);
        int elbows = PipeRoute.elbows(plan.route.cells(), start.anchor.end(), plan.anchor.end());
        return PipeLaying.message("hud.route", describe(start.anchor), describe(plan.anchor), cells, elbows, String.format(Locale.ROOT, "%.3f", diameter),
                player.getAbilities().instabuild ? Component.literal("—") : Component.literal(Integer.toString(cells)));
    }

    /**
     * The off-centre step of the lane route cell {@code i} meets through {@code face} (the same along every axis: a lane's
     * centre lies on its rack's diagonal): the route's own lane next to it, or the lane the line starts or ends on; 0
     * where it meets none.
     */
    private static int laneStep(Plan shown, int i, int face, int width) {
        var cells = shown.route.cells();
        var step = LineGeometry.STEPS[face];
        var next = new PipeRoute.Cell(cells.get(i).x() + step[0], cells.get(i).y() + step[1], cells.get(i).z() + step[2]);
        for (int j : new int[]{i - 1, i + 1})
            if (j >= 0 && j < cells.size() && cells.get(j).equals(next) && shown.route.slots().get(j) >= 0) return RackGeometry.offset(shown.route.slots().get(j), width);
        var first = start;
        for (var anchor : new PipeAnchor[]{i == 0 && first != null ? first.anchor : null, i == cells.size() - 1 ? shown.anchor : null})
            // A lane anchor takes lines no wider than its lane: its fit's widest is the lane's width.
            if (anchor != null && anchor.kind() == PipeAnchor.Kind.LANE && anchor.joint() != null && anchor.joint().ordinal() == face)
                return RackGeometry.offset(anchor.hostLane() - 1, anchor.fit().maxWidth());
        return 0;
    }

    private static Component describe(PipeAnchor anchor) {
        return PipeLaying.message("end." + name(anchor.kind()));
    }

    /** The aimed nozzle: its role, whether a pipe is on it, and the largest bore it takes. */
    private static @Nullable Component portLabel(Level level, PipeAnchor.Click hit) {
        if (!(level.getBlockEntity(hit.clicked()) instanceof EquipmentShellBlockEntity shell)) return null;
        var port = shell.port(hit.face());
        var nozzle = shell.nozzleFacing(hit.face());
        if (port == null || nozzle == null) return null;
        var role = nozzle.role().name().toLowerCase(Locale.ROOT);
        var roleName = Component.translatableWithFallback("message.icheme.pipe_laying.role." + role, humanize(role));
        boolean connected = connected(level, hit.clicked(), hit.face());
        return PipeLaying.message("port", roleName, PipeLaying.message(connected ? "port.connected" : "port.free"), String.format(Locale.ROOT, "%.1f", PipeSizes.MAX_BORE));
    }

    private static String humanize(String name) {
        var words = name.replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static boolean connected(Level level, BlockPos shell, Direction face) {
        var at = shell.relative(face);
        var state = level.getBlockState(at);
        return state.getBlock() instanceof FluidDeviceBlock && state.getValue(FluidDeviceBlock.connection(face.getOpposite()));
    }

    /**
     * Marks the ports of the equipment around the player: a square on each nozzle's flange face in its port class's
     * colour, grey when a pipe is on it. Re-shown each tick from the client's index of port cells; at most
     * {@link #MAX_MARKS}, nearest first.
     */
    private static void ports(Level level, LocalPlayer player, Outliner outliner, Set<Object> shown) {
        var marks = new ArrayList<Mark>();
        for (var shell : EquipmentShellBlockEntity.clientPortCells()) {
            if (shell.isRemoved() || shell.getLevel() != level || shell.shape() == null) continue;
            var at = shell.getBlockPos();
            double distance = at.getCenter().distanceToSqr(player.position());
            if (distance > PORT_RANGE * PORT_RANGE) continue;
            for (var face : Direction.values()) {
                var port = shell.port(face);
                if (port != null) marks.add(new Mark(at, face, port.port(), distance));
            }
        }
        marks.sort((a, b) -> Double.compare(a.distance, b.distance));
        for (var mark : marks.subList(0, Math.min(MAX_MARKS, marks.size()))) {
            var slot = "icheme:port:" + mark.cell.asLong() + ":" + mark.face.ordinal();
            shown.add(slot);
            int colour = connected(level, mark.cell, mark.face) ? CONNECTED_COLOUR : PORT_COLOURS[mark.port.ordinal()];
            outliner.showAABB(slot, square(mark.cell, mark.face, .5, .3)).colored(colour).lineWidth(1 / 32F);
        }
    }

    private record Mark(BlockPos cell, Direction face, EquipmentShape.PortClass port, double distance) {
    }

    /**
     * Marks the open line ends around the player in yellow (plan 4.7), where a line laid to them joins: a square on each
     * open face of a pipe, and on each open face of a rack lane at the lane's point there; at most {@link #MAX_MARKS},
     * nearest first.
     */
    private static void openEnds(Level level, LocalPlayer player, Outliner outliner, Set<Object> shown) {
        var ends = new ArrayList<End>();
        for (var pipe : FluidDeviceBlockEntity.clientOpenEnds()) {
            if (pipe.isRemoved() || pipe.getLevel() != level) continue;
            double distance = pipe.getBlockPos().getCenter().distanceToSqr(player.position());
            if (distance > PORT_RANGE * PORT_RANGE) continue;
            int open = pipe.openEnds();
            for (var face : Direction.values())
                if ((open >> face.ordinal() & 1) != 0) ends.add(new End(pipe.getBlockPos(), face, -1, .5, PipeSizes.outside(pipe.bore()), distance));
        }
        for (var rack : PipeRackBlockEntity.clientRacks()) {
            if (rack.isRemoved() || rack.getLevel() != level) continue;
            double distance = rack.getBlockPos().getCenter().distanceToSqr(player.position());
            if (distance > PORT_RANGE * PORT_RANGE) continue;
            var lanes = rack.lanes();
            for (int i = 0; i < lanes.size(); i++) {
                int open = rack.openFaces(i);
                var lane = lanes.get(i);
                for (var face : Direction.values())
                    if ((open >> face.ordinal() & 1) != 0)
                        ends.add(new End(rack.getBlockPos(), face, lane.slot(), RackGeometry.centre(lane.slot(), lane.width()), PipeSizes.outside(lane.bore()), distance));
            }
        }
        ends.sort((a, b) -> Double.compare(a.distance, b.distance));
        for (var end : ends.subList(0, Math.min(MAX_MARKS, ends.size()))) {
            var slot = "icheme:open:" + end.cell.asLong() + ":" + end.face.ordinal() + ":" + end.slot;
            shown.add(slot);
            outliner.showAABB(slot, square(end.cell, end.face, end.at, Math.min(.45, end.outside / 2 + .04))).colored(OPEN_COLOUR).lineWidth(1 / 32F);
        }
    }

    /** An open end: its cell and face, the lane's first slot (-1 for a pipe), where on the face it lies, and its size. */
    private record End(BlockPos cell, Direction face, int slot, double at, double outside, double distance) {
    }

    /** While a rack is aimed, a small cube at the centre of each slot no lane uses: where a new lane may go. */
    private static void freeSlots(Level level, @Nullable PipeAnchor.Click hit, Outliner outliner, Set<Object> shown) {
        if (hit == null || !(level.getBlockEntity(hit.clicked()) instanceof PipeRackBlockEntity rack)) return;
        int used = rack.used();
        for (int slot = 0; slot < RackGeometry.SLOTS; slot++) {
            if ((used >> slot & 1) != 0) continue;
            double c = RackGeometry.centre(slot), half = .04;
            var key = "icheme:free_slot:" + slot;
            shown.add(key);
            outliner.showAABB(key, new AABB(c - half, c - half, c - half, c + half, c + half, c + half).move(hit.clicked())).colored(FREE_COLOUR).lineWidth(1 / 32F);
        }
    }

    /**
     * A flat square {@code 2 half} blocks across on {@code face} of {@code cell}, centred at {@code at} along both of the
     * face's own axes (0.5: the face's centre; a lane's diagonal coordinate for a lane).
     */
    private static AABB square(BlockPos cell, Direction face, double at, double half) {
        double[] low = new double[3], high = new double[3];
        for (int a = 0; a < 3; a++) {
            boolean normal = a == face.getAxis().ordinal();
            double centre = normal ? face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0 : at, extent = normal ? .01 : half;
            low[a] = centre - extent;
            high[a] = centre + extent;
        }
        return new AABB(low[0], low[1], low[2], high[0], high[1], high[2]).move(cell);
    }

    /** The found route drawn translucent at the tool's size, as the pipe blocks it would place are drawn. */
    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        var shown = plan;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || start == null || shown == null || shown.refusal != null) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var sprite = mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(PLATE);
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        var buffers = mc.renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(TYPE);
        double diameter = settingsDiameter(mc.player);
        int code = PipeSizes.code(PipeSizes.outside(diameter)), width = width(diameter);
        var cells = shown.route.cells();
        for (int i = 0; i < cells.size(); i++) {
            var cell = cells.get(i);
            int slot = shown.route.slots().get(i);
            List<com.wormzjl.icheme.science.equipment.Mesh.Polygon> polygons;
            // A lane is drawn in its slots as the rack will draw it, welded at every joint.
            if (slot >= 0) polygons = RackGeometry.lane(new RackGeometry.Lane(slot, width, shown.joints[i], shown.joints[i], code), shown.facings[i]);
            else {
                long faces = 0, offsets = 0;
                for (int face = 0; face < 6; face++) {
                    if ((shown.joints[i] >> face & 1) == 0) continue;
                    faces |= (long) code << 7 * face;
                    // A plain cell beside a lane is a transition pipe to the lane's off-centre point.
                    int at = laneStep(shown, i, face, width);
                    if (at != 0) offsets |= LineGeometry.Key.offsets(face, at, at);
                }
                polygons = LineGeometry.polygons(new LineGeometry.Key(TopologyCompiler.Kind.PIPE, shown.facings[i], shown.joints[i], code, faces, 0, false, offsets));
            }
            pose.pushPose();
            pose.translate(cell.x() - camera.x, cell.y() - camera.y, cell.z() - camera.z);
            var last = pose.last();
            for (var polygon : polygons) {
                double[] p = polygon.positions(), uv = polygon.uvs(), n = polygon.normals();
                // A fan of quads, as the chunk model bakes it; a last single triangle repeats its final vertex.
                for (int first = 1; first + 1 < polygon.size(); first += 2)
                    for (int corner : new int[]{0, first, first + 1, Math.min(first + 2, polygon.size() - 1)})
                        consumer.addVertex(last, (float) p[3 * corner], (float) p[3 * corner + 1], (float) p[3 * corner + 2])
                                .setColor(GHOST >> 16 & 0xFF, GHOST >> 8 & 0xFF, GHOST & 0xFF, ALPHA)
                                .setUv(sprite.getU((float) uv[2 * corner]), sprite.getV((float) uv[2 * corner + 1]))
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                                .setNormal(last, (float) n[3 * corner], (float) n[3 * corner + 1], (float) n[3 * corner + 2]);
            }
            pose.popPose();
        }
        buffers.endBatch(TYPE);
    }

    private static void say(LocalPlayer player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
