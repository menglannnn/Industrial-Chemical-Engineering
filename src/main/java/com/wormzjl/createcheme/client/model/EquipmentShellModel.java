package com.wormzjl.createcheme.client.model;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.client.FanAnimation;
import com.wormzjl.createcheme.science.equipment.Mesh;
import com.wormzjl.createcheme.science.equipment.ShapeRasterizer;
import com.wormzjl.createcheme.world.level.block.entity.EquipmentShellBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Chunk-meshed model of a vessel shell cell (batch 2026-10-15-custom-equipment-rendering, route A1). The cell's key
 * arrives as model data from its block entity; the quads of a key are built once from {@link ShapeRasterizer#surface}
 * and cached, and body cells share one key per column. Quads stay inside the cell, carry smooth vertex normals and have
 * no ambient occlusion; their directional shade is baked into the vertex colour from the smooth normal (owner report
 * 2026-10-16: the per-quad six-step shade banded a round shell), so vanilla and Sodium shade them alike.
 * Without model data (Distant Horizons asks that way) the model is empty and the particle sprite gives the colour.
 * While air coolers' fans turn ({@link FanAnimation}) their rotors are left out: a cell holding a rotor keeps both lists,
 * every other cell one list for both states.
 */
public final class EquipmentShellModel implements IDynamicBakedModel {
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(CreateChemE.MOD_ID, "equipment_shell");
    public static final IGeometryLoader<Geometry> LOADER = (json, context) -> new Geometry();
    private static final int CACHE = 4096;
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private final TextureAtlasSprite[] sprites;
    private final TextureAtlasSprite particle;
    /** Rebuilt with the model on every resource reload, since quads embed atlas coordinates. Meshing threads share them. */
    private final Map<ShapeRasterizer.CellKey, Baked> quads = new ConcurrentHashMap<>();
    /** The sprites of the model last baked, for parts drawn outside the chunk mesh ({@link #sprite}). */
    private static volatile TextureAtlasSprite @Nullable [] current;

    private EquipmentShellModel(TextureAtlasSprite[] sprites, TextureAtlasSprite particle) {
        this.sprites = sprites;
        this.particle = particle;
        current = sprites;
    }

    /** The sprite of {@code material} in the model last baked, or null before the first bake. */
    public static @Nullable TextureAtlasSprite sprite(Mesh.Material material) {
        var sprites = current;
        return sprites == null ? null : sprites[material.ordinal()];
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        var key = data.get(EquipmentShellBlockEntity.CELL);
        if (side != null || key == null) return List.of();
        var baked = quads.get(key);
        if (baked == null) {
            if (quads.size() >= CACHE) quads.clear();
            baked = quads.computeIfAbsent(key, this::bake);
        }
        return FanAnimation.enabled() ? baked.turning() : baked.still();
    }

    /** A cell's quads with its rotors at rest, and without them while the fans turn: one list where it holds none. */
    private record Baked(List<BakedQuad> still, List<BakedQuad> turning) {
    }

    /**
     * Each convex polygon becomes a fan of quads; a last single triangle repeats its final vertex. A painted material's
     * quads carry its paint channel as their tint index, so the cell's paint colours them as the chunk is meshed.
     */
    private Baked bake(ShapeRasterizer.CellKey key) {
        var polygons = ShapeRasterizer.surface(key);
        var still = bake(polygons, sprites, true);
        if (polygons.stream().noneMatch(p -> p.material() == Mesh.Material.AC_FAN)) return new Baked(still, still);
        return new Baked(still, bake(polygons.stream().filter(p -> p.material() != Mesh.Material.AC_FAN).toList(), sprites, true));
    }

    /**
     * Each convex polygon as a fan of quads, a last single triangle repeating its final vertex; sprites by material, and
     * {@code painted}: tinted by the material's paint channel (a vessel's), or never (a pipeline device's).
     */
    public static List<BakedQuad> bake(List<Mesh.Polygon> polygons, TextureAtlasSprite[] sprites, boolean painted) {
        var out = new ArrayList<BakedQuad>();
        for (var polygon : polygons) {
            int tint = painted ? polygon.material().channel() : -1;
            for (int first = 1; first + 1 < polygon.size(); first += 2)
                out.add(quad(polygon, sprites[polygon.material().ordinal()], tint, 0, first, first + 1, Math.min(first + 2, polygon.size() - 1)));
        }
        return List.copyOf(out);
    }

    /** One sprite per material from the model's textures (named {@link Mesh.Material#texture()}), the particle where one is missing. */
    public static TextureAtlasSprite[] sprites(IGeometryBakingContext context, Function<Material, TextureAtlasSprite> sprites) {
        var particle = sprites.apply(context.getMaterial("particle"));
        var out = new TextureAtlasSprite[Mesh.Material.values().length];
        for (var material : Mesh.Material.values())
            out[material.ordinal()] = context.hasMaterial(material.texture()) ? sprites.apply(context.getMaterial(material.texture())) : particle;
        return out;
    }

    private static BakedQuad quad(Mesh.Polygon polygon, TextureAtlasSprite sprite, int tint, int... corners) {
        double[] p = polygon.positions(), uv = polygon.uvs(), n = polygon.normals();
        // Face direction from the corners' winding (Newell), for vanilla's directional shade.
        double fx = 0, fy = 0, fz = 0;
        for (int i = 0; i < 4; i++) {
            int a = 3 * corners[i], b = 3 * corners[(i + 1) % 4];
            fx += (p[a + 1] - p[b + 1]) * (p[a + 2] + p[b + 2]);
            fy += (p[a + 2] - p[b + 2]) * (p[a] + p[b]);
            fz += (p[a] - p[b]) * (p[a + 1] + p[b + 1]);
        }
        var baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(tint);
        baker.setDirection(Direction.getNearest(fx, fy, fz));
        // The directional shade is baked into each vertex from its smooth normal, by the formula NeoForge uses for its own
        // normal-based lighting (an axis-facing face shades exactly as a block face does). Vanilla's per-quad shade would
        // band a round shell in six steps, so the quad asks for none; vanilla and Sodium both multiply the vertex colour.
        baker.setShade(false);
        baker.setHasAmbientOcclusion(false);
        for (int c : corners) {
            float nx = (float) n[3 * c], ny = (float) n[3 * c + 1], nz = (float) n[3 * c + 2];
            int shade = Math.round(255 * QuadLighter.calculateShade(nx, ny, nz, false));
            baker.addVertex((float) p[3 * c], (float) p[3 * c + 1], (float) p[3 * c + 2])
                    .setColor(shade, shade, shade, 255)
                    .setUv(sprite.getU((float) uv[2 * c]), sprite.getV((float) uv[2 * c + 1]))
                    .setNormal(nx, ny, nz);
        }
        return baker.bakeQuad();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return SOLID;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /** {@code "loader": "createcheme:equipment_shell"} with a texture per material ({@link #sprites}) and {@code particle}. */
    public record Geometry() implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides) {
            return new EquipmentShellModel(sprites(context, sprites), sprites.apply(context.getMaterial("particle")));
        }
    }
}
