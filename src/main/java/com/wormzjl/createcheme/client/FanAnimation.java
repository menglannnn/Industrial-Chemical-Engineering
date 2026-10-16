package com.wormzjl.createcheme.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.client.model.EquipmentShellModel;
import com.wormzjl.createcheme.science.equipment.Mesh;
import com.wormzjl.createcheme.science.equipment.ProcessUnit;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Air coolers' turning fans (owner request 2026-10-16: "add a working animation for AC (can be switched on and off) and
 * check FPS, try to be FPS efficient"). While it is on, the shell model leaves the rotors out of the chunk mesh
 * ({@link EquipmentShellModel}) and this draws each rotor within {@link #RANGE} blocks and in view once a frame, turned
 * about its hub. A rotor is uploaded to the GPU once per size and light; the shader is set up once a frame, and each fan
 * then costs a light lookup, its matrix and one draw call, whatever its triangles (measured 2026-10-16 on a 6 × 4 bank
 * in view: 48 turning fans cost about 0.03 ms a frame against still ones). A real fan turns at about 5 rev/s (tip speed 50 to
 * 60 m/s), where its blades would alias at frame rates, so these turn at {@link #SPEED}. The switch is on an air
 * cooler's datasheet, for debugging; it is not saved.
 */
@EventBusSubscriber(modid = CreateChemE.MOD_ID, value = Dist.CLIENT)
public final class FanAnimation {
    /** How far from the camera fans turn, blocks: beyond it a rotor is a few pixels and not drawn at all. */
    public static final double RANGE = 96;
    /** Revolutions per second; {@link #PERIOD} ticks hold a whole number of turns, so the angle wraps without a jump. */
    static final double SPEED = 1.5;
    static final int PERIOD = 40;
    /** Rotor buffers kept, by size and light; all are dropped when it fills. */
    private static final int MAX_BUFFERS = 64;
    private static boolean enabled = true;
    private static final Map<Long, VertexBuffer> BUFFERS = new HashMap<>();
    /** The fan sprite the buffers hold texture coordinates of: a resource reload makes them stale. */
    private static @Nullable TextureAtlasSprite builtWith;

    private FanAnimation() {
    }

    public static boolean enabled() {
        return enabled;
    }

    /** The datasheet's switch: the chunks are meshed again with or without the still rotors. */
    public static void toggle() {
        enabled = !enabled;
        Minecraft.getInstance().levelRenderer.allChanged();
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (!enabled || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
        var level = Minecraft.getInstance().level;
        var sprite = EquipmentShellModel.sprite(Mesh.Material.AC_FAN);
        if (level == null || sprite == null) return;
        var cells = EquipmentShellBlockEntity.clientRotorCells();
        if (cells.isEmpty()) return;
        if (sprite != builtWith) {
            release();
            builtWith = sprite;
        }
        var camera = event.getCamera().getPosition();
        var frustum = event.getFrustum();
        double turns = SPEED * (level.getGameTime() % PERIOD + event.getPartialTick().getGameTimeDeltaPartialTick(false)) / 20;
        var type = RenderType.solid();
        ShaderInstance shader = null;
        VertexBuffer bound = null;
        for (var cell : cells) {
            if (cell.isRemoved() || cell.getLevel() != level) continue;
            for (var r : cell.rotors()) {
                double dx = r[0] - camera.x, dy = r[1] - camera.y, dz = r[2] - camera.z, half = r[3] / 2;
                if (dx * dx + dy * dy + dz * dz > RANGE * RANGE) continue;
                if (!frustum.isVisible(new AABB(r[0] - half, r[1] - .5, r[2] - half, r[0] + half, r[1] + .5, r[2] + half))) continue;
                if (shader == null) {
                    type.setupRenderState();
                    shader = RenderSystem.getShader();
                    if (shader == null) {
                        type.clearRenderState();
                        return;
                    }
                    shader.setDefaultUniforms(VertexFormat.Mode.TRIANGLES, event.getModelViewMatrix(), event.getProjectionMatrix(), Minecraft.getInstance().getWindow());
                    // The chunk shader offsets by the last section drawn; a rotor's matrix carries its whole offset.
                    if (shader.CHUNK_OFFSET != null) shader.CHUNK_OFFSET.set(0F, 0F, 0F);
                    shader.apply();
                }
                int cached = BUFFERS.size();
                var buffer = buffer(r[3], LevelRenderer.getLightColor(level, BlockPos.containing(r[0], r[1], r[2])), sprite);
                // Building or dropping buffers binds others.
                if (BUFFERS.size() != cached) bound = null;
                if (buffer == null || shader.MODEL_VIEW_MATRIX == null) continue;
                // Each fan starts at its own angle, so a bank's blades do not turn in step.
                double phase = Math.floorMod(31L * (long) Math.floor(4 * r[0]) + (long) Math.floor(4 * r[2]), 97) / 97.0;
                var pose = new Matrix4f(event.getModelViewMatrix()).translate((float) dx, (float) dy, (float) dz)
                        .rotateY((float) (-2 * Math.PI * (turns + phase)));
                shader.MODEL_VIEW_MATRIX.set(pose);
                shader.MODEL_VIEW_MATRIX.upload();
                if (buffer != bound) {
                    buffer.bind();
                    bound = buffer;
                }
                buffer.draw();
            }
        }
        if (shader != null) {
            shader.clear();
            VertexBuffer.unbind();
            type.clearRenderState();
        }
    }

    private static @Nullable VertexBuffer buffer(double diameter, int light, TextureAtlasSprite sprite) {
        long key = Math.round(diameter * 1000) << 32 | light & 0xFFFFFFFFL;
        var buffer = BUFFERS.get(key);
        if (buffer != null) return buffer;
        if (BUFFERS.size() >= MAX_BUFFERS) release();
        buffer = build(diameter, light, sprite);
        if (buffer != null) BUFFERS.put(key, buffer);
        return buffer;
    }

    /**
     * A rotor's triangles as the shell model bakes them: its smooth normals' directional shade in the vertex colour, the
     * fan sprite, and one light for the whole rotor, the light of its hub's cell.
     */
    private static @Nullable VertexBuffer build(double diameter, int light, TextureAtlasSprite sprite) {
        var polygons = ProcessUnit.rotor(diameter);
        int vertices = 0;
        for (var polygon : polygons) vertices += 3 * (polygon.size() - 2);
        try (var bytes = new ByteBufferBuilder(Math.max(256, vertices * DefaultVertexFormat.BLOCK.getVertexSize()))) {
            var builder = new BufferBuilder(bytes, VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.BLOCK);
            for (var polygon : polygons) {
                double[] p = polygon.positions(), uv = polygon.uvs(), n = polygon.normals();
                for (int k = 1; k + 1 < polygon.size(); k++)
                    for (int c : new int[]{0, k, k + 1}) {
                        float nx = (float) n[3 * c], ny = (float) n[3 * c + 1], nz = (float) n[3 * c + 2];
                        int shade = Math.round(255 * QuadLighter.calculateShade(nx, ny, nz, false));
                        builder.addVertex((float) p[3 * c], (float) p[3 * c + 1], (float) p[3 * c + 2])
                                .setColor(shade, shade, shade, 255)
                                .setUv(sprite.getU((float) uv[2 * c]), sprite.getV((float) uv[2 * c + 1]))
                                .setLight(light)
                                .setNormal(nx, ny, nz);
                    }
            }
            var mesh = builder.build();
            if (mesh == null) return null;
            var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(mesh);
            VertexBuffer.unbind();
            return buffer;
        }
    }

    private static void release() {
        BUFFERS.values().forEach(VertexBuffer::close);
        BUFFERS.clear();
    }
}
