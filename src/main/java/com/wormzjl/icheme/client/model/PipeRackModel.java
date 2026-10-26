package com.wormzjl.icheme.client.model;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.science.equipment.RackGeometry;
import com.wormzjl.icheme.world.level.block.PipeRackBlock;
import com.wormzjl.icheme.world.level.block.entity.PipeRackBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Chunk-meshed model of a pipe rack cell (batch 2026-10-26-pipe-laying-system, plan 5.4): the frame along the block's
 * axis and the lanes its block entity hands over as model data; {@link RackGeometry} builds the polygons and each key is
 * baked once. Without model data, and as an item, the bare frame along x.
 */
public final class PipeRackModel implements IDynamicBakedModel {
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "pipe_rack");
    /** {@code "loader": "icheme:pipe_rack"}, textures per material (the frame takes "steelwork", the lanes "pipe"). */
    public static final IGeometryLoader<Geometry> LOADER = (json, context) -> new Geometry();
    private static final int CACHE = 4096;
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());

    private record Look(int axis, List<RackGeometry.Lane> lanes, List<Integer> facings) {
    }

    private final TextureAtlasSprite[] sprites;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final Map<Look, List<BakedQuad>> quads = new ConcurrentHashMap<>();

    private PipeRackModel(TextureAtlasSprite[] sprites, TextureAtlasSprite particle, ItemTransforms transforms) {
        this.sprites = sprites;
        this.particle = particle;
        this.transforms = transforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        if (side != null) return List.of();
        int axis = state != null && state.getBlock() instanceof PipeRackBlock ? state.getValue(PipeRackBlock.AXIS).ordinal() : 0;
        var drawn = data.get(PipeRackBlockEntity.DRAWN);
        var key = drawn == null ? new Look(axis, List.of(), List.of()) : new Look(axis, drawn.lanes(), drawn.facings());
        var cached = quads.get(key);
        if (cached == null) {
            if (quads.size() >= CACHE) quads.clear();
            cached = quads.computeIfAbsent(key, look -> EquipmentShellModel.bake(RackGeometry.polygons(look.axis, look.lanes, look.facings), sprites, false));
        }
        return cached;
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
        return true;
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
    public ItemTransforms getTransforms() {
        return transforms;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    public record Geometry() implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides) {
            return new PipeRackModel(EquipmentShellModel.sprites(context, sprites), sprites.apply(context.getMaterial("particle")), context.getTransforms());
        }
    }
}
