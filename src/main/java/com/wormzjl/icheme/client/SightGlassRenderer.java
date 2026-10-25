package com.wormzjl.icheme.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.client.model.FluidDeviceModel;
import com.wormzjl.icheme.network.SightGlassNetwork.Target;
import com.wormzjl.icheme.science.equipment.Mesh;
import com.wormzjl.icheme.science.equipment.SightGlassGeometry;
import com.wormzjl.icheme.science.equipment.SightGlassBubbles;
import com.wormzjl.icheme.science.math.DeterministicMath;
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

/** Liquid and illustrative bubble motion are dynamic; metal/glass remain chunk-baked. Ordinary pipes do not render here. */
@EventBusSubscriber(modid=IChemE.MOD_ID,value=Dist.CLIENT)
public final class SightGlassRenderer {
    private static final double RANGE=120;
    private static final RenderType LIQUID=RenderType.entityTranslucentCull(InventoryMenu.BLOCK_ATLAS);
    private static final ResourceLocation WHITE=ResourceLocation.withDefaultNamespace("block/white_concrete");
    private record Surface(List<Mesh.Polygon> liquid,SightGlassGeometry.Window window){}
    private static final Map<SightGlassGeometry.Key,Surface> SURFACES=new HashMap<>();
    private static final float[][] RING=ring();
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
            var sample=SightGlassDisplays.display(entity);int argb=sample.argb();var pos=entity.getBlockPos();
            if(argb>>>24==0||entity.isRemoved()||entity.getLevel()!=level||!FluidDeviceBlock.hasSightGlass(entity.getBlockState())
                    ||pos.distToCenterSqr(camera)>128*128||!event.getFrustum().isVisible(new AABB(pos).inflate(.15)))continue;
            var key=FluidDeviceModel.sightGlassKey(entity.getBlockState(),entity.getModelData());
            if(!SightGlassGeometry.supports(key.pipe().connections()))continue;
            if(SURFACES.size()>=256)SURFACES.clear();
            var surface=SURFACES.computeIfAbsent(key,k->new Surface(SightGlassGeometry.build(k,false).liquid(),SightGlassGeometry.window(k)));
            int light=LevelRenderer.getLightColor(level,pos);
            if(consumer==null)consumer=buffers.getBuffer(LIQUID);
            pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
            for(var polygon:surface.liquid())for(int first=1;first+1<polygon.size();first+=2)
                for(int corner:new int[]{0,first,first+1,Math.min(first+2,polygon.size()-1)}) {
                    var p=polygon.positions();var n=polygon.normals();
                    consumer.addVertex(pose.last(),(float)p[3*corner],(float)p[3*corner+1],(float)p[3*corner+2])
                            .setColor(argb>>16&255,argb>>8&255,argb&255,argb>>>24).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                            .setNormal(pose.last(),(float)n[3*corner],(float)n[3*corner+1],(float)n[3*corner+2]);
                }
            if(sample.bubbles()>0){
                double ticks=level.getGameTime()+event.getPartialTick().getGameTimeDeltaPartialTick(false);
                bubbles(pose,consumer,surface.window(),SightGlassBubbles.at(ticks,entity.fluidIdentity()^pos.asLong(),sample.bubbles()),argb,sample.bubbles(),light,u,v);
            }
            pose.popPose();
        }
        if(consumer!=null)buffers.endBatch(LIQUID);
    }

    private static void bubbles(PoseStack pose,VertexConsumer consumer,SightGlassGeometry.Window window,List<SightGlassBubbles.Bubble> bubbles,int argb,int strength,int light,float u,float v){
        int colour=SightGlassBubbles.colour(argb,strength),red=colour>>16&255,green=colour>>8&255,blue=colour&255,alpha=colour>>>24;
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(window.axis()){case X->pose.mulPose(Axis.YP.rotationDegrees(90));case Y->pose.mulPose(Axis.XP.rotationDegrees(-90));default->{}}
        for(int side:new int[]{1,-1}){
            pose.pushPose();if(side<0)pose.mulPose(Axis.YP.rotationDegrees(180));pose.translate(0,0,window.plane());
            for(var bubble:bubbles){
                if(bubble.radius()<.0001)continue;
                for(int i=0;i<RING.length;i++)for(int corner=0;corner<4;corner++){
                    int angle=(corner==0||corner==3)?i:(i+1)%RING.length;double radius=bubble.radius()*(corner<2?1:.72);
                    consumer.addVertex(pose.last(),(float)(window.radius()*(bubble.x()+RING[angle][0]*radius)),(float)(window.radius()*(bubble.y()+RING[angle][1]*radius)),0)
                            .setColor(red,green,blue,alpha).setUv(u,v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,1);
                }
            }
            pose.popPose();
        }
        pose.popPose();
    }
    private static float[][] ring(){var points=new float[12][2];for(int i=0;i<points.length;i++){double a=2*Math.PI*i/points.length;points[i][0]=(float)DeterministicMath.cos(a);points[i][1]=(float)DeterministicMath.sin(a);}return points;}
}
