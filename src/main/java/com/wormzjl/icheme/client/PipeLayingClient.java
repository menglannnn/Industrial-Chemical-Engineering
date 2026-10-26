package com.wormzjl.icheme.client;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.network.FluidNetwork;
import com.wormzjl.icheme.registry.ModItems;
import com.wormzjl.icheme.science.equipment.EquipmentShape;
import com.wormzjl.icheme.science.equipment.LineGeometry;
import com.wormzjl.icheme.science.equipment.PipeRoute;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.ShapeRasterizer;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.PipeAnchor;
import com.wormzjl.icheme.world.level.block.PipeLaying;
import com.wormzjl.icheme.world.level.block.entity.EquipmentShellBlockEntity;
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
    private static final Object START_SLOT = "icheme:pipe_start", ROUTE_SLOT = "icheme:pipe_route", REMOVE_SLOT = "icheme:pipe_remove";
    private static final int START_COLOUR = 0x55FF55, REFUSED_COLOUR = 0xFF5555, CONNECTED_COLOUR = 0x808080;
    /** Port classes' outline colours: vapour, bulk, liquid, overflow ({@link EquipmentShape.PortClass} order). */
    private static final int[] PORT_COLOURS = {0xF0F0F0, 0x55DDFF, 0x3377FF, 0xFF9933};
    /** A start this far from the player is dropped, blocks; ports are marked this far, at most {@link #MAX_PORTS}. */
    private static final double DROP_RANGE = 96, PORT_RANGE = 48;
    private static final int MAX_PORTS = 256, RECHECK_TICKS = 20, HUD_TICKS = 5;
    private static final ResourceLocation PLATE = ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "block/painted_shell");
    private static final RenderType TYPE = RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final int GHOST = 0xB0D8FF, ALPHA = 140;

    private record Click(BlockPos clicked, Direction face) {
    }

    private record Start(Click click, PipeAnchor anchor, ResourceKey<Level> dimension) {
    }

    /** The route to the aimed click, or why there is none; its ghost cells and their joints when found. */
    private record Plan(Click end, @Nullable PipeAnchor anchor, PipeRoute.Result route, @Nullable Component refusal, int[] joints, int[] facings) {
    }

    private static @Nullable Start start;
    private static @Nullable Plan plan;
    private static boolean armed = true;
    private static int age;
    private static Set<Object> portSlots = Set.of();

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
            } else if (hit != null && pipe(mc.level, hit.clicked)) {
                FluidNetwork.dismantlePipe(offHand, hit.clicked);
                event.setSwingHand(true);
            } else FluidNetwork.openPipeLaying(offHand);
            return;
        }
        if (hit == null) return;
        if (start == null) {
            var resolved = PipeAnchor.resolve(mc.level, hit.clicked, hit.face);
            if (resolved.anchor() == null) {
                say(player, PipeLaying.message("anchor." + name(resolved.refusal())));
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
        FluidNetwork.layPipe(offHand, start.click.clicked, start.click.face, hit.clicked, hit.face, cells);
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
            if (start != null || plan != null || !portSlots.isEmpty()) {
                start = null;
                plan = null;
                for (var slot : List.of(START_SLOT, ROUTE_SLOT, REMOVE_SLOT)) outliner.remove(slot);
                for (var slot : portSlots) outliner.remove(slot);
                portSlots = Set.of();
            }
            return;
        }
        age++;
        if (start != null && (start.dimension != mc.level.dimension() || start.click.clicked.getCenter().distanceToSqr(player.position()) > DROP_RANGE * DROP_RANGE)) {
            start = null;
            plan = null;
            say(player, PipeLaying.message("dropped"));
        }
        var hit = mc.screen == null ? aimed(mc) : null;
        if (start != null) {
            if (hit == null) plan = null;
            else if (plan == null || !plan.end.equals(hit) || age % RECHECK_TICKS == 0) plan = plan(mc.level, hit, settingsDiameter(player));
            outliner.showAABB(START_SLOT, new AABB(start.anchor.cell())).colored(START_COLOUR).lineWidth(1 / 16F);
            if (plan != null && plan.refusal != null)
                outliner.showLine(ROUTE_SLOT, start.anchor.cell().getCenter(), (plan.anchor != null ? plan.anchor.cell() : plan.end.clicked).getCenter()).colored(REFUSED_COLOUR).lineWidth(1 / 16F);
            else outliner.remove(ROUTE_SLOT);
            if (age % HUD_TICKS == 0) say(player, hud(player));
        } else {
            plan = null;
            outliner.remove(START_SLOT);
            outliner.remove(ROUTE_SLOT);
        }
        // While Shift is held, the pipe a use would dismantle is outlined in red.
        if (start == null && player.isShiftKeyDown() && hit != null && pipe(mc.level, hit.clicked))
            outliner.showAABB(REMOVE_SLOT, new AABB(hit.clicked)).colored(REFUSED_COLOUR).lineWidth(1 / 16F);
        else outliner.remove(REMOVE_SLOT);
        if (start == null && hit != null && age % HUD_TICKS == 0) {
            var label = portLabel(mc.level, hit);
            if (label != null) say(player, label);
        }
        ports(mc.level, player, outliner);
    }

    private static @Nullable Click aimed(Minecraft mc) {
        return mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK ? new Click(hit.getBlockPos(), hit.getDirection()) : null;
    }

    private static boolean pipe(Level level, BlockPos position) {
        return level.getBlockState(position).getBlock() instanceof FluidDeviceBlock block && block.kind() == TopologyCompiler.Kind.PIPE;
    }

    private static double settingsDiameter(LocalPlayer player) {
        var stack = tool(player.getMainHandItem()) ? player.getMainHandItem() : player.getOffhandItem();
        var settings = stack.get(ModItems.PIPE_LAYING_SETTINGS.get());
        return settings == null ? .05 : settings.diameter();
    }

    /** The route from the start to {@code end}, searched now on this thread within the search limits. */
    private static Plan plan(Level level, Click end, double diameter) {
        var none = new int[0];
        var resolved = PipeAnchor.resolve(level, end.clicked, end.face);
        if (resolved.anchor() == null) return new Plan(end, null, new PipeRoute.Result(List.of(), null), PipeLaying.message("anchor." + name(resolved.refusal())), none, none);
        var from = start.anchor;
        var route = PipeRoute.search(from.end(), from.heading().ordinal(), resolved.anchor().end(), PipeAnchor.space(level), PipeRoute.Limits.DEFAULT);
        if (!route.found()) return new Plan(end, resolved.anchor(), route, PipeLaying.message("route." + name(route.refusal())), none, none);
        var joints = PipeRoute.laidJoints(route.cells(), from.end(), resolved.anchor().end());
        return new Plan(end, resolved.anchor(), route, null, joints, PipeRoute.facings(route.cells(), from.heading().ordinal()));
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

    private static Component describe(PipeAnchor anchor) {
        return PipeLaying.message("end." + name(anchor.kind()));
    }

    /** The aimed nozzle: its role, whether a pipe is on it, and the largest bore it takes. */
    private static @Nullable Component portLabel(Level level, Click hit) {
        if (!(level.getBlockEntity(hit.clicked) instanceof EquipmentShellBlockEntity shell)) return null;
        var port = shell.port(hit.face);
        var nozzle = shell.nozzleFacing(hit.face);
        if (port == null || nozzle == null) return null;
        var role = nozzle.role().name().toLowerCase(Locale.ROOT);
        var roleName = Component.translatableWithFallback("message.icheme.pipe_laying.role." + role, humanize(role));
        boolean connected = connected(level, hit.clicked, hit.face);
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
     * {@link #MAX_PORTS}, nearest first.
     */
    private static void ports(Level level, LocalPlayer player, Outliner outliner) {
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
        var shown = new HashSet<Object>();
        for (var mark : marks.subList(0, Math.min(MAX_PORTS, marks.size()))) {
            var slot = mark.slot();
            shown.add(slot);
            int colour = connected(level, mark.cell, mark.face) ? CONNECTED_COLOUR : PORT_COLOURS[mark.port.ordinal()];
            outliner.showAABB(slot, flange(mark.cell, mark.face)).colored(colour).lineWidth(1 / 32F);
        }
        for (var slot : portSlots) if (!shown.contains(slot)) outliner.remove(slot);
        portSlots = shown;
    }

    private record Mark(BlockPos cell, Direction face, EquipmentShape.PortClass port, double distance) {
        Object slot() {
            return "icheme:port:" + cell.asLong() + ":" + face.ordinal();
        }
    }

    /** A flat square 0.6 blocks across on {@code face} of {@code cell}. */
    private static AABB flange(BlockPos cell, Direction face) {
        var centre = Vec3.atCenterOf(cell).add(face.getStepX() * .5, face.getStepY() * .5, face.getStepZ() * .5);
        double x = face.getAxis() == Direction.Axis.X ? .01 : .3, y = face.getAxis() == Direction.Axis.Y ? .01 : .3, z = face.getAxis() == Direction.Axis.Z ? .01 : .3;
        return new AABB(centre.x - x, centre.y - y, centre.z - z, centre.x + x, centre.y + y, centre.z + z);
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
        int code = PipeSizes.code(PipeSizes.outside(settingsDiameter(mc.player)));
        var cells = shown.route.cells();
        for (int i = 0; i < cells.size(); i++) {
            var cell = cells.get(i);
            long faces = 0;
            for (int face = 0; face < 6; face++) if ((shown.joints[i] >> face & 1) != 0) faces |= (long) code << 7 * face;
            var key = new LineGeometry.Key(TopologyCompiler.Kind.PIPE, shown.facings[i], shown.joints[i], code, faces, 0, false);
            pose.pushPose();
            pose.translate(cell.x() - camera.x, cell.y() - camera.y, cell.z() - camera.z);
            var last = pose.last();
            for (var polygon : LineGeometry.polygons(key)) {
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
