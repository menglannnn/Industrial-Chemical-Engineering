package com.wormzjl.createcheme.client.model;

import com.wormzjl.createcheme.CreateChemE;
import com.wormzjl.createcheme.science.equipment.LineGeometry;
import com.wormzjl.createcheme.science.equipment.PipeSizes;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.createcheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.createcheme.world.level.block.entity.FluidDeviceBlockEntity;
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
import net.minecraft.util.GsonHelper;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Chunk-meshed model of a pipe or pipeline device (batch 2026-10-15-custom-equipment-rendering, owner request
 * 2026-10-16). Its key is the block state (kind, facing, connections) and the drawn geometry its block entity hands over
 * as model data (bore, the line met at each face, flanged joints, spool end); {@link LineGeometry} builds the polygons
 * and each key is baked once. Without model data the block draws at the default bore with nothing met; as an item, at
 * {@link LineGeometry.Key#item}.
 */
public final class FluidDeviceModel implements IDynamicBakedModel {
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(CreateChemE.MOD_ID, "fluid_device");
    /** {@code "loader": "createcheme:fluid_device", "kind": "pipe"} (a {@link TopologyCompiler.Kind}), textures per material. */
    public static final IGeometryLoader<Geometry> LOADER = (json, context) ->
            new Geometry(TopologyCompiler.Kind.valueOf(GsonHelper.getAsString(json, "kind").toUpperCase(Locale.ROOT)));
    private static final int CACHE = 4096;
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private final TopologyCompiler.Kind kind;
    private final TextureAtlasSprite[] sprites;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private final Map<LineGeometry.Key, List<BakedQuad>> quads = new ConcurrentHashMap<>();

    private FluidDeviceModel(TopologyCompiler.Kind kind, TextureAtlasSprite[] sprites, TextureAtlasSprite particle, ItemTransforms transforms) {
        this.kind = kind;
        this.sprites = sprites;
        this.particle = particle;
        this.transforms = transforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        if (side != null) return List.of();
        var key = key(state, data);
        var cached = quads.get(key);
        if (cached != null) return cached;
        if (quads.size() >= CACHE) quads.clear();
        return quads.computeIfAbsent(key, k -> EquipmentShellModel.bake(LineGeometry.polygons(k), sprites));
    }

    private LineGeometry.Key key(@Nullable BlockState state, ModelData data) {
        if (state == null || !(state.getBlock() instanceof FluidDeviceBlock block)) return LineGeometry.Key.item(kind);
        int connections = 0;
        for (var direction : Direction.values()) if (state.getValue(FluidDeviceBlock.connection(direction))) connections |= 1 << direction.ordinal();
        int facing = state.getValue(FluidDeviceBlock.FACING).ordinal();
        var drawn = data.get(FluidDeviceBlockEntity.DRAWN);
        if (drawn == null) return new LineGeometry.Key(block.kind(), facing, connections, PipeSizes.code(PipeSizes.outside(.05)), 0, 0, false);
        return new LineGeometry.Key(block.kind(), facing, connections, PipeSizes.code(PipeSizes.outside(drawn.bore())), drawn.faces(), drawn.flanged(), drawn.spool());
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

    public record Geometry(TopologyCompiler.Kind kind) implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides) {
            return new FluidDeviceModel(kind, EquipmentShellModel.sprites(context, sprites), sprites.apply(context.getMaterial("particle")), context.getTransforms());
        }
    }
}
