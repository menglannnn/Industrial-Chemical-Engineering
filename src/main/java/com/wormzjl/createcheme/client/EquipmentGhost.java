package com.wormzjl.createcheme.client;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.client.gui.screens.EquipmentConstructorScreen;
import com.wormzjl.createcheme.registry.ModBlocks;
import com.wormzjl.createcheme.science.equipment.EquipmentShape;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.world.level.block.EquipmentStructure;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentConstructorBlockEntity;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The ghost of the vessel an equipment constructor would build: shown while its screen is open (the screen's draft)
 * and while the player looks at it (its saved design). The shell is drawn translucent with the plate texture, and every
 * cell the vessel cannot take is outlined in red through Catnip's outliner. Client only; the server checks again
 * before it builds.
 */
@EventBusSubscriber(modid = CreateChemE.MOD_ID, value = Dist.CLIENT)
public final class EquipmentGhost {
    private static final Object BLOCKED_SLOT = "createcheme:equipment_ghost_blocked";
    /** Blocks around the ghost change while it is shown: check its cells again twice a second. */
    private static final int RECHECK_TICKS = 10;
    private static final int BLOCKED_COLOUR = 0xFF5555;
    /** Render thread only, like everything here. */
    private static @Nullable EquipmentStructure.Vessel shown;
    private static List<BlockPos> blocked = List.of();
    /** The design's vessel already stands where it would be built, so no ghost is shown. */
    private static boolean standing;
    private static int age;
    /** Layout and mesh of the last shape shown; a ghost's shape changes only when the player edits it. */
    private static @Nullable ShapeRasterizer.Layout layout;
    private static @Nullable Mesh mesh;

    private EquipmentGhost() {
    }

    /** Cells of the shown vessel that are occupied, unloaded or outside the world, as last checked. */
    public static int blockedCount() {
        return blocked.size();
    }

    public static boolean standing() {
        return standing;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        var target = mc.level == null ? null : target(mc);
        standing = target != null && built(mc.level, target);
        if (standing) target = null;
        if (!Objects.equals(target, shown)) {
            shown = target;
            age = 0;
        }
        var outliner = Outliner.getInstance();
        if (shown == null) {
            blocked = List.of();
            outliner.remove(BLOCKED_SLOT);
            return;
        }
        if (age++ % RECHECK_TICKS == 0) {
            blocked = blocked(mc.level, shown);
            if (!blocked.isEmpty()) outliner.showCluster(BLOCKED_SLOT, blocked).colored(BLOCKED_COLOUR).lineWidth(1 / 16F);
        }
        if (blocked.isEmpty()) outliner.remove(BLOCKED_SLOT);
        else outliner.keep(BLOCKED_SLOT);
    }

    private static @Nullable EquipmentStructure.Vessel target(Minecraft mc) {
        if (mc.screen instanceof EquipmentConstructorScreen screen) return screen.vessel();
        if (mc.screen == null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof EquipmentConstructorBlockEntity constructor)
            return constructor.vessel(constructor.design());
        return null;
    }

    /** The vessel already stands there: its controller cell is a shell cell of the same vessel. */
    private static boolean built(ClientLevel level, EquipmentStructure.Vessel vessel) {
        var cell = layout(vessel.shape()).controller();
        return level.getBlockEntity(vessel.origin().offset(cell.x(), cell.y(), cell.z())) instanceof EquipmentShellBlockEntity shell
                && vessel.shape().equals(shell.shape()) && vessel.origin().equals(shell.origin());
    }

    private static List<BlockPos> blocked(ClientLevel level, EquipmentStructure.Vessel vessel) {
        var cells = layout(vessel.shape());
        var out = new ArrayList<BlockPos>();
        for (var list : List.of(cells.shell(), cells.interior()))
            for (var cell : list) {
                var position = vessel.origin().offset(cell.x(), cell.y(), cell.z());
                if (level.isOutsideBuildHeight(position) || !level.hasChunkAt(position) || !level.getBlockState(position).canBeReplaced()) out.add(position);
            }
        return out;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || shown == null) return;
        var mc = Minecraft.getInstance();
        var sprite = mc.getBlockRenderer().getBlockModel(ModBlocks.EQUIPMENT_SHELL.get().defaultBlockState()).getParticleIcon(ModelData.EMPTY);
        var vertices = mesh(shown.shape(), sprite).vertices;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(shown.origin().getX() - camera.x, shown.origin().getY() - camera.y, shown.origin().getZ() - camera.z);
        var last = pose.last();
        var buffers = mc.renderBuffers().bufferSource();
        var consumer = buffers.getBuffer(RenderType.translucent());
        for (int i = 0; i < vertices.length; i += Mesh.STRIDE)
            consumer.addVertex(last, vertices[i], vertices[i + 1], vertices[i + 2]).setColor(.65F, .85F, 1F, .45F)
                    .setUv(vertices[i + 3], vertices[i + 4]).setLight(LightTexture.FULL_BRIGHT).setNormal(last, vertices[i + 5], vertices[i + 6], vertices[i + 7]);
        pose.popPose();
        buffers.endBatch(RenderType.translucent());
    }

    private static ShapeRasterizer.Layout layout(EquipmentShape shape) {
        if (layout == null || !layout.shape().equals(shape)) layout = ShapeRasterizer.layout(shape);
        return layout;
    }

    /** The shell's quads in vessel-local coordinates, rebuilt when the shape or the plate sprite (resource reload) changes. */
    private static Mesh mesh(EquipmentShape shape, TextureAtlasSprite sprite) {
        if (mesh != null && mesh.shape.equals(shape) && mesh.sprite == sprite) return mesh;
        var out = new ArrayList<float[]>();
        for (var cell : layout(shape).shell())
            for (var polygon : ShapeRasterizer.surface(ShapeRasterizer.key(shape, cell.x(), cell.y(), cell.z())))
                // A fan of quads, as the chunk model bakes it; a last single triangle repeats its final vertex.
                for (int first = 1; first + 1 < polygon.size(); first += 2)
                    for (int corner : new int[]{0, first, first + 1, Math.min(first + 2, polygon.size() - 1)}) {
                        double[] p = polygon.positions(), uv = polygon.uvs(), n = polygon.normals();
                        out.add(new float[]{(float) (cell.x() + p[3 * corner]), (float) (cell.y() + p[3 * corner + 1]), (float) (cell.z() + p[3 * corner + 2]),
                                sprite.getU((float) uv[2 * corner]), sprite.getV((float) uv[2 * corner + 1]),
                                (float) n[3 * corner], (float) n[3 * corner + 1], (float) n[3 * corner + 2]});
                    }
        var vertices = new float[out.size() * Mesh.STRIDE];
        for (int i = 0; i < out.size(); i++) System.arraycopy(out.get(i), 0, vertices, i * Mesh.STRIDE, Mesh.STRIDE);
        mesh = new Mesh(shape, sprite, vertices);
        return mesh;
    }

    /** Position, texture coordinate and normal of every quad vertex of the shell. */
    private record Mesh(EquipmentShape shape, TextureAtlasSprite sprite, float[] vertices) {
        static final int STRIDE = 8;
    }
}
