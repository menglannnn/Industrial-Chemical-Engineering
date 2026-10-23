package com.wormzjl.icheme.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.client.model.FluidDeviceModel;
import com.wormzjl.icheme.network.SightGlassNetwork.Target;
import com.wormzjl.icheme.science.equipment.Mesh;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.world.level.block.FluidDeviceBlock;
import com.wormzjl.icheme.world.level.block.entity.FluidDeviceBlockEntity;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Only the liquid is dynamic. Metal and clear glass are chunk-baked, so ordinary pipes gain no per-frame renderer. */
@EventBusSubscriber(modid=IChemE.MOD_ID,value=Dist.CLIENT)
public final class SightGlassRenderer {
    private static final double RANGE=120;
    private static final RenderType LIQUID=RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final Map<SightGlassGeometry.Key,List<Mesh.Polygon>> SURFACES=new HashMap<>();
    private static ClientLevel world;
    private static int age;
    private SightGlassRenderer(){}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(world!=mc.level){if(mc.getConnection()!=null)SightGlassDisplays.track(List.of());else SightGlassDisplays.clear();SURFACES.clear();world=mc.level;age=0;}
        if(mc.level==null||mc.player==null||mc.getConnection()==null||age++%20!=0)return;
        var point=mc.player.position();var near=new ArrayList<FluidDeviceBlockEntity>();
        for(var entity:FluidDeviceBlockEntity.clientSightGlasses())if(!entity.isRemoved()&&entity.getLevel()==mc.level&&entity.fluidIdentity()>0
                &&FluidDeviceBlock.hasSightGlass(entity.getBlockState())&&entity.getBlockPos().distToCenterSqr(point)<=RANGE*RANGE)near.add(entity);
        near.sort(Comparator.comparingDouble((FluidDeviceBlockEntity entity)->entity.getBlockPos().distToCenterSqr(point)).thenComparingLong(entity->entity.getBlockPos().asLong()));
        SightGlassDisplays.track(near.stream().map(entity->new Target(entity.getBlockPos(),entity.fluidIdentity())).toList());
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        // Liquid must be drawn before the terrain's translucent glass writes depth in front of it.
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null)return;
        var sprite=mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(WHITE);
        float u=sprite.getU(.5F),v=sprite.getV(.5F);var camera=event.getCamera().getPosition();
        var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();VertexConsumer consumer=null;
        for(var entity:FluidDeviceBlockEntity.clientSightGlasses()) {
            int argb=SightGlassDisplays.colour(entity);var pos=entity.getBlockPos();
            if(argb>>>24==0||entity.isRemoved()||entity.getLevel()!=level||!FluidDeviceBlock.hasSightGlass(entity.getBlockState())
                    ||pos.distToCenterSqr(camera)>128*128||!event.getFrustum().isVisible(new AABB(pos)))continue;
            var key=FluidDeviceModel.sightGlassKey(entity.getBlockState(),entity.getModelData());
            if(SURFACES.size()>=256)SURFACES.clear();
            var polygons=SURFACES.computeIfAbsent(key,k->SightGlassGeometry.build(k,false).liquid());
            int light=LevelRenderer.getLightColor(level,pos);
            if(consumer==null)consumer=buffers.getBuffer(LIQUID);
            pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
            for(var polygon:polygons)for(int first=1;first+1<polygon.size();first+=2)
                for(int corner:new int[]{0,first,first+1,Math.min(first+2,polygon.size()-1)}) {
                    var p=polygon.positions();var n=polygon.normals();
                    consumer.addVertex(pose.last(),(float)p[3*corner],(float)p[3*corner+1],(float)p[3*corner+2])
                            .setColor(argb>>16&255,argb>>8&255,argb&255,argb>>>24).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(pose.last(),(float)n[3*corner],(float)n[3*corner+1],(float)n[3*corner+2]);
                }
            pose.popPose();
        }
        if(consumer!=null)buffers.endBatch(LIQUID);
    }
}
