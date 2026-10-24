package com.wormzjl.icheme.science.equipment;

import java.util.List;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Full-port ball-valve silhouette: cast body, split joint, bonnet and quarter-turn safety-yellow lever. */
public final class ShutoffValveGeometry {
    private static final double[] CENTRE={.5,.5,.5};
    private ShutoffValveGeometry(){}

    /** Pure geometry; the open flag changes only the lever, never pipe diameter or collision. */
    public static List<Polygon> polygons(LineGeometry.Key key,boolean open,boolean accessory) {
        var mesh=new Mesh();double od=PipeSizes.outsideOf(key.outside());
        int axis=key.facing()&~1,sides=Math.max(16,PipeSizes.sides(od));
        double radius=Math.min(.495,Math.max(.065,.64*od)),half=Math.min(.31,Math.max(.085,.55*od));
        double[] run=LineGeometry.step(axis|1),stem=axis==0?new double[]{0,0,-1}:new double[]{0,1,0};
        var body=new Frame(CENTRE,run,stem);
        mesh.prism(body,radius,-.78*half,.78*half,sides,Material.FLANGE);
        mesh.frustum(body,.88*radius,radius,-half,-.78*half,sides,Material.FLANGE);
        mesh.frustum(body,radius,.88*radius,.78*half,half,sides,Material.FLANGE);
        double aperture=accessory?Math.min(.46*od,.82*radius):0;
        mesh.annulus(body,aperture,.88*radius,-half,sides,-1,Material.FLANGE);
        mesh.annulus(body,aperture,.88*radius,half,sides,1,Material.FLANGE);
        if(aperture>0)mesh.bore(body,aperture,-half,half,sides,Material.PIPE);
        mesh.hoop(body,radius,radius+.002,-.015*half,.015*half,sides,Material.OPENING);
        double bolt=Math.min(.03,Math.max(.008,.065*od));
        for(int sign:new int[]{-1,1})for(int i=0;i<4;i++) {
            var cap=new Frame(body.at(Math.PI/4+i*Math.PI/2,.90*radius,sign*half),new double[]{sign*run[0],sign*run[1],sign*run[2]},stem);
            mesh.cylinder(cap,bolt,0,1.2*bolt,6,Material.BOLT);
        }
        var bonnet=new Frame(CENTRE,stem,run);double neck=Math.max(.016,.12*od),rise=Math.max(.055,.17*od);
        mesh.cylinder(bonnet,1.65*neck,.80*radius,radius+.30*rise,12,Material.FLANGE);
        mesh.cylinder(bonnet,neck,radius,radius+rise,12,Material.BOLT);
        mesh.cylinder(bonnet,1.35*neck,radius+.50*rise,radius+.66*rise,6,Material.BOLT);
        double length=Math.min(.95,Math.max(.36,1.10*od)),width=Math.max(.065,Math.min(.19,.24*od)),thickness=Math.max(.035,.085*od);
        var pivot=bonnet.along(radius+rise);double[] lever=open?run:cross(stem,run);
        bar(mesh,pivot,lever,stem,1.35*width,1.15*thickness,-.28*length,.12*length,Material.FLANGE);
        bar(mesh,pivot,lever,stem,width,thickness,0,length,Material.FLANGE);
        var rib=pivot.clone();for(int i=0;i<3;i++)rib[i]-=.65*thickness*stem[i];
        bar(mesh,rib,lever,stem,.48*width,.65*thickness,-.10*length,.65*length,Material.STEELWORK);
        bar(mesh,pivot,lever,stem,1.28*width,1.50*thickness,.48*length,length,Material.GUARD);
        bar(mesh,pivot,lever,stem,1.30*width,1.55*thickness,.94*length,length,Material.BOLT);
        mesh.cylinder(new Frame(pivot,stem,run),Math.max(1.35*neck,.55*width),-thickness,1.05*thickness,6,Material.BOLT);
        if(!accessory)for(int face:new int[]{axis,axis^1}) {
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            LineGeometry.arm(mesh,face,half,od,end,LineGeometry.ref(face),sides,true);
        }
        return LineGeometry.overhanging(mesh);
    }

    private static void bar(Mesh mesh,double[] origin,double[] direction,double[] up,double width,double height,double from,double to,Material material) {
        double[] side=cross(up,direction);var previous=mesh.transform();
        mesh.transform(new Transform(new int[]{(int)side[0],(int)up[0],(int)direction[0],(int)side[1],(int)up[1],(int)direction[1],(int)side[2],(int)up[2],(int)direction[2]},origin));
        mesh.box(new double[]{-width/2,-height/2,from},new double[]{width/2,height/2,to},63,material);
        mesh.transform(previous);
    }
}
