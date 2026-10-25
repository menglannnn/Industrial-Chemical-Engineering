package com.wormzjl.icheme.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.wormzjl.icheme.IChemE;
import com.wormzjl.icheme.network.InstrumentNetwork;
import com.wormzjl.icheme.runtime.instrument.*;
import com.wormzjl.icheme.science.equipment.InstrumentGeometry;
import com.wormzjl.icheme.world.item.*;
import com.wormzjl.icheme.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.*;

/** Shared metre-sized geometry for installed dials and placement ghosts. Only needles read live publications. */
@EventBusSubscriber(modid=IChemE.MOD_ID,value=Dist.CLIENT)
public final class InstrumentRenderer {
    private InstrumentRenderer(){}
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath(IChemE.MOD_ID,"textures/misc/instrument.png");
    private static final RenderType TYPE=RenderType.entitySolid(WHITE);
    private static final RenderType GHOST=RenderType.entityTranslucentCull(WHITE);
    private record Connector(List<Double> tap,boolean bottom,boolean temperature,double probeDepth){}
    private static final Map<Connector,List<InstrumentGeometry.Quad>> CONNECTORS=new HashMap<>();
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        var camera=event.getCamera().getPosition();var buffers=mc.renderBuffers().bufferSource();var pose=event.getPoseStack();
        GaugeSettings draft=null;boolean wrench=false;
        for(var hand:net.minecraft.world.InteractionHand.values()) {
            var stack=mc.player.getItemInHand(hand);
            if(stack.getItem() instanceof GaugeItem)draft=stack.get(ModItems.GAUGE_SETTINGS.get());
            if(stack.getItem() instanceof AccessoryWrenchItem&&mc.player.isShiftKeyDown())wrench=true;
        }
        var hosts=InstrumentHosts.nearby(mc.level,mc.player.position(),120,draft==null&&!wrench);
        var selected=draft==null?null:InstrumentHosts.select(hosts,mc.player.getEyePosition(),mc.player.getLookAngle(),mc.player.blockInteractionRange(),draft.kind(),false);
        for(var host:hosts) {
            for(var installed:host.metadata().installed()) {
                var mount=host.mount(installed.key().mount());if(mount==null)continue;
                var display=InstrumentDisplays.get(new InstrumentNetwork.Target(host.entity().getBlockPos(),installed.key(),installed.instance()));
                double ratio=display==null||display.sample().availability()==Instrument.Availability.UNAVAILABLE?0:installed.settings().needle(display.sample().value());
                draw(pose,buffers,mount,camera,installed.settings(),ratio,0,255);
            }
            if(draft!=null&&!host.pipe())for(var mount:host.mounts()) {
                if(!mount.kinds().contains(draft.kind())||mount.centre().distanceToSqr(mc.player.getEyePosition())>64)continue;
                boolean blocked=host.occupied(mount)||!InstrumentHosts.clear(mc.level,host,mount);
                boolean aimed=selected!=null&&selected.host().identity()==host.identity()&&selected.mount().id().equals(mount.id())&&InstrumentHosts.reachable(mc.player,host,mount);
                int colour=blocked?0xad7770:aimed?0x73a38e:0x779ba9;
                // Occupied instruments already supply the silhouette. An outline avoids coincident ghost/model planes.
                if(!blocked)draw(pose,buffers,mount,camera,draft,.5,0,aimed?88:56);
                outline(pose,buffers,mount,camera,colour);
            }
        }
        if(wrench) {
            var target=InstrumentHosts.select(hosts,mc.player.getEyePosition(),mc.player.getLookAngle(),mc.player.blockInteractionRange(),null,true);
            if(target!=null&&!target.host().pipe()&&InstrumentHosts.reachable(mc.player,target.host(),target.mount()))outline(pose,buffers,target.mount(),camera,0x73a38e);

        }
        buffers.endBatch(TYPE);buffers.endBatch(GHOST);buffers.endBatch(RenderType.lines());
    }
    private static void outline(PoseStack pose,MultiBufferSource buffers,Instrument.Selectable selection,Vec3 camera,int colour) {
        var lines=buffers.getBuffer(RenderType.lines());
        if(selection instanceof Instrument.Mount mount) {
            for(int i=0;i<32;i++) {
                double a=2*Math.PI*i/32,b=2*Math.PI*(i+1)/32;
                var first=mount.point(.177*Math.cos(a),.177*Math.sin(a),.073).subtract(camera);
                var second=mount.point(.177*Math.cos(b),.177*Math.sin(b),.073).subtract(camera);
                var direction=second.subtract(first).normalize();
                for(var point:List.of(first,second))lines.addVertex(pose.last(),(float)point.x,(float)point.y,(float)point.z).setColor(colour>>16&255,colour>>8&255,colour&255,135).setNormal(pose.last(),(float)direction.x,(float)direction.y,(float)direction.z);
            }
        } else LevelRenderer.renderLineBox(pose,lines,selection.bounds().move(camera.scale(-1)),(colour>>16&255)/255f,(colour>>8&255)/255f,(colour&255)/255f,.53f);
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,Instrument.Mount mount,Vec3 camera,GaugeSettings settings,double ratio,int tint,int alpha){
        var consumer=buffers.getBuffer(alpha==255?TYPE:GHOST);
        for(var q:InstrumentGeometry.housing())quad(pose,consumer,mount,camera,q,tint,alpha);
        var tap=mount.localTap();var key=new Connector(List.of(tap[0],tap[1],tap[2]),mount.bottomEntry(),settings.kind()==GaugeSettings.Kind.TEMPERATURE,mount.probeDepth());
        if(CONNECTORS.size()>2048)CONNECTORS.clear();
        for(var q:CONNECTORS.computeIfAbsent(key,k->InstrumentGeometry.connection(new double[]{tap[0],tap[1],tap[2]},k.bottom,k.temperature,k.probeDepth)))quad(pose,consumer,mount,camera,q,tint,alpha);
        quad(pose,consumer,mount,camera,InstrumentGeometry.needle(ratio),tint,alpha);
        if(tint==0&&alpha==255){
            text(pose,buffers,mount,camera,settings.label(),0,-.098,.0025f);
            for(int i=0;i<=4;i++){
                double fraction=i/4.0,angle=Math.PI*(1.25-1.5*fraction);
                String label=compact(settings.display(settings.minimum()+fraction*(settings.maximum()-settings.minimum())));
                float scale=Math.min(.0025f,.061f/Math.max(1,Minecraft.getInstance().font.width(label)));
                text(pose,buffers,mount,camera,label,.081*Math.cos(angle),.081*Math.sin(angle)+.010,scale);
            }
        }
    }
    private static String compact(double value){return String.format(Locale.ROOT,Math.abs(value)>=10000?"%.1e":"%.3g",value);}
    private static void quad(PoseStack pose,VertexConsumer consumer,Instrument.Mount mount,Vec3 camera,InstrumentGeometry.Quad q,int tint,int alpha){var v=q.vertices();Vec3 p0=mount.point(v[0],v[1],v[2]),p1=mount.point(v[3],v[4],v[5]),p2=mount.point(v[6],v[7],v[8]);var n=p1.subtract(p0).cross(p2.subtract(p0)).normalize();int c=tint==0?q.colour():tint;for(int j=0;j<4;j++){var p=mount.point(v[j*3],v[j*3+1],v[j*3+2]).subtract(camera);consumer.addVertex(pose.last(),(float)p.x,(float)p.y,(float)p.z).setColor(c>>16&255,c>>8&255,c&255,alpha).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(tint==0?LevelRenderer.getLightColor(Minecraft.getInstance().level,net.minecraft.core.BlockPos.containing(mount.centre())):LightTexture.FULL_BRIGHT).setNormal(pose.last(),(float)n.x,(float)n.y,(float)n.z);}}
    private static void text(PoseStack pose,MultiBufferSource buffers,Instrument.Mount m,Vec3 camera,String text,double x,double y,float scale){var mc=Minecraft.getInstance();var p=m.point(x,y,InstrumentGeometry.LABEL).subtract(camera);pose.pushPose();pose.translate(p.x,p.y,p.z);float angle=switch(m.face()){case NORTH->180;case EAST->90;case WEST->-90;default->0;};pose.mulPose(Axis.YP.rotationDegrees(angle));pose.scale(scale,-scale,scale);mc.font.drawInBatch(text,-mc.font.width(text)/2f,0,0xff252b2e,false,pose.last().pose(),buffers,net.minecraft.client.gui.Font.DisplayMode.NORMAL,0,LightTexture.FULL_BRIGHT);pose.popPose();}
    @SubscribeEvent public static void hud(RenderGuiEvent.Post event){var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.screen!=null)return;var selected=InstrumentHosts.select(InstrumentHosts.nearby(mc.level,mc.player.getEyePosition(),8,true),mc.player.getEyePosition(),mc.player.getLookAngle(),mc.player.blockInteractionRange(),null,true);if(selected==null||!InstrumentHosts.reachable(mc.player,selected.host(),selected.mount()))return;var h=selected.host();var i=h.metadata().installed().stream().filter(v->v.key().mount().equals(selected.mount().id())).findFirst().orElse(null);if(i==null)return;var s=i.settings();var d=InstrumentDisplays.get(new InstrumentNetwork.Target(h.entity().getBlockPos(),i.key(),i.instance()));var sample=d==null?null:d.sample();var lines=List.of(sample==null||sample.availability()==Instrument.Availability.UNAVAILABLE?Component.translatable("gui.icheme.gauge.unavailable"):Component.literal(s.number(sample.value())+" "+s.label()));var g=event.getGuiGraphics();int x=g.guiWidth()/2+14,y=g.guiHeight()/2+12;for(var line:lines){g.fill(x-3,y-2,x+mc.font.width(line)+3,y+10,0xe0202a31);g.drawString(mc.font,line,x,y,0xffeaf1f5,false);y+=12;}}
}
