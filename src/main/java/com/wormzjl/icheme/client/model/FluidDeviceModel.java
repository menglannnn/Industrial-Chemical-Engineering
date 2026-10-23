package com.wormzjl.icheme.client.model;

import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.science.equipment.LineGeometry;
import com.wormzjl.icheme.science.equipment.PipeSizes;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
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
    public static final ResourceLocation LOADER_ID = ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID, "fluid_device");
    /** {@code "loader": "icheme:fluid_device", "kind": "pipe"} (a {@link TopologyCompiler.Kind}), textures per material. */
    public static final IGeometryLoader<Geometry> LOADER = (json, context) ->
            new Geometry(TopologyCompiler.Kind.valueOf(GsonHelper.getAsString(json, "kind").toUpperCase(Locale.ROOT)),GsonHelper.getAsBoolean(json,"sight_glass_accessory",false));
    private static final int CACHE = 4096;
    private static final ChunkRenderTypeSet SOLID = ChunkRenderTypeSet.of(RenderType.solid());
    private static final ChunkRenderTypeSet GLASS = ChunkRenderTypeSet.of(RenderType.solid(),RenderType.translucent());
    private final TopologyCompiler.Kind kind;
    private final boolean accessory;
    private final TextureAtlasSprite[] sprites;
    private final TextureAtlasSprite particle;
    private final ItemTransforms transforms;
    private record Look(LineGeometry.Key pipe,boolean sightGlass){}
    private record Baked(List<BakedQuad> solid,List<BakedQuad> glass){}
    private final Map<Look,Baked> quads = new ConcurrentHashMap<>();

    private FluidDeviceModel(TopologyCompiler.Kind kind,boolean accessory, TextureAtlasSprite[] sprites, TextureAtlasSprite particle, ItemTransforms transforms) {
        this.kind = kind;
        this.accessory=accessory;
        this.sprites = sprites;
        this.particle = particle;
        this.transforms = transforms;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        if (side != null) return List.of();
        var key = new Look(key(state,data),accessory||state!=null&&FluidDeviceBlock.hasSightGlass(state));
        var cached = quads.get(key);
        if(cached==null){if(quads.size()>=CACHE)quads.clear();cached=quads.computeIfAbsent(key,this::bake);}
        if(renderType==RenderType.translucent())return cached.glass;
        if(renderType!=null)return cached.solid;
        return java.util.stream.Stream.concat(cached.solid.stream(),cached.glass.stream()).toList();
    }

    private Baked bake(Look look) {
        if(!look.sightGlass)return new Baked(EquipmentShellModel.bake(LineGeometry.polygons(look.pipe),sprites,false),List.of());
        var geometry=SightGlassGeometry.build(look.pipe,accessory);
        return new Baked(EquipmentShellModel.bake(geometry.metal(),sprites,false),EquipmentShellModel.bake(geometry.glass(),sprites,false));
    }

    private LineGeometry.Key key(@Nullable BlockState state, ModelData data) {
        if (state == null || !(state.getBlock() instanceof FluidDeviceBlock block)) return LineGeometry.Key.item(kind);
        return blockKey(state,data);
    }

    /** The same diameter/connection key for chunk geometry and the transient liquid overlay. */
    public static LineGeometry.Key blockKey(BlockState state,ModelData data) {
        var block=(FluidDeviceBlock)state.getBlock();
        int connections = 0;
        for (var direction : Direction.values()) if (state.getValue(FluidDeviceBlock.connection(direction))) connections |= 1 << direction.ordinal();
        int facing = state.getValue(FluidDeviceBlock.FACING).ordinal();
        var drawn = data.get(FluidDeviceBlockEntity.DRAWN);
        if (drawn == null) return new LineGeometry.Key(block.kind(), facing, connections, PipeSizes.code(PipeSizes.outside(.05)), 0, 0, false);
        return new LineGeometry.Key(block.kind(), facing, connections, PipeSizes.code(PipeSizes.outside(drawn.bore())), drawn.faces(), drawn.flanged(), drawn.spool());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return FluidDeviceBlock.hasSightGlass(state)?GLASS:SOLID;
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

    public record Geometry(TopologyCompiler.Kind kind,boolean accessory) implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides) {
            return new FluidDeviceModel(kind,accessory, EquipmentShellModel.sprites(context, sprites), sprites.apply(context.getMaterial("particle")), context.getTransforms());
        }
    }
}
