package com.wormzjl.icheme.science.equipment;

import java.util.ArrayList;
import java.util.List;
import static com.wormzjl.icheme.science.equipment.Mesh.*;

/** Flanged globe casting, bolted bonnet, exposed stem/yoke and a full-height diaphragm actuator. */
public final class PressureValveGeometry {
    private static final double[] CENTRE={.5,.5,.5};
    private PressureValveGeometry(){}

    public static List<Polygon> polygons(LineGeometry.Key key,boolean accessory) {
        var mesh=new Mesh();body(mesh,key,accessory);
        double half=half(PipeSizes.outsideOf(key.outside()));
        var arrows=MoverPortMarkings.fitting(mesh.cell(0,0,0),key.facing(),-.42*half,.42*half,Math.PI/4);
        finish(mesh,key,accessory);var out=new ArrayList<>(LineGeometry.overhanging(mesh));out.addAll(arrows);
        return List.copyOf(out);
    }
    static void build(Mesh mesh,LineGeometry.Key key){body(mesh,key,false);finish(mesh,key,false);}
    private static double radius(double od){return Math.min(.495,Math.max(.105,.62*od+.06));}
    private static double half(double od){return Math.min(.31,Math.max(.105,.20+.12*od));}
    private static double[] stem(int facing){return (facing&~1)==0?new double[]{0,0,-1}:new double[]{0,1,0};}

    private static void body(Mesh mesh,LineGeometry.Key key,boolean accessory) {
        double od=PipeSizes.outsideOf(key.outside()),r=radius(od),h=half(od);int sides=Math.max(16,PipeSizes.sides(2*r));
        var frame=new Frame(CENTRE,LineGeometry.step(key.facing()|1),stem(key.facing()));
        mesh.frustum(frame,.67*r,r,-h,-.50*h,sides,Material.FLANGE);
        mesh.prism(frame,r,-.50*h,.50*h,sides,Material.FLANGE);
        mesh.frustum(frame,r,.67*r,.50*h,h,sides,Material.FLANGE);
        double hole=accessory?Math.min(.44*od,.60*r):0;
        for(int sign:new int[]{-1,1})mesh.annulus(frame,hole,.67*r,sign*h,sides,sign,Material.FLANGE);
        if(hole>0)mesh.bore(frame,hole,-h,h,sides,Material.OPENING);
    }

    private static void finish(Mesh mesh,LineGeometry.Key key,boolean accessory) {
        double od=PipeSizes.outsideOf(key.outside()),r=radius(od),h=half(od);
        int axis=key.facing()&~1;var run=LineGeometry.step(axis|1);var stem=stem(key.facing());var f=new Frame(CENTRE,stem,run);
        double bonnet=Math.max(.075,.19*od),yoke=Math.max(.22,.42*od),casing=Math.min(.96,Math.max(.34,.30+.75*od));
        double shoulder=.82*r,seat=r+bonnet,top=seat+yoke,rc=casing/2;
        int n=Math.max(16,PipeSizes.sides(casing));
        mesh.frustum(f,.48*r,.30*r,shoulder,seat,12,Material.MACHINE);
        mesh.annulus(f,0,.30*r,seat,12,1,Material.MACHINE);
        mesh.ring(f,.28*r,.54*r,seat-.035,seat+.015,12,3,Material.FLANGE);
        mesh.bolts(f,od,.29*r,.54*r,seat+.015,1,Material.BOLT);
        double leg=Math.max(.016,.045*casing),spread=Math.max(.045,.28*r);var across=f.side();
        for(int sign:new int[]{-1,1}) {
            var a=f.along(seat+.015);var b=f.along(top);
            for(int i=0;i<3;i++){a[i]+=sign*spread*across[i];b[i]+=sign*.72*spread*across[i];}
            mesh.bar(a,b,leg,run,Material.STEELWORK);
        }
        mesh.cylinder(f,Math.max(.007,.020*od),seat,top+.03,10,Material.BOLT);
        mesh.cylinder(f,Math.max(.013,.035*od),seat+.06*yoke,seat+.20*yoke,6,Material.GUARD);
        mesh.ring(f,0,.30*rc,top-.025,top+.025,12,3,Material.FLANGE);
        // Full-sized diaphragm and bolted split rim; no scaling down to the host block's top face.
        double rim=top+.15*casing,edge=rim+.035,cap=edge+.23*casing;
        mesh.frustum(f,.30*rc,rc,top,rim,n,Material.ACTUATOR);
        mesh.annulus(f,0,.30*rc,top,n,-1,Material.ACTUATOR);
        mesh.ring(f,rc,.96*rc+.025,rim,edge,n,3,Material.FLANGE);
        mesh.prism(f,rc,rim,edge,n,Material.ACTUATOR);
        mesh.frustum(f,rc,.34*rc,edge,cap,n,Material.ACTUATOR);
        mesh.annulus(f,0,.34*rc,cap,n,1,Material.ACTUATOR);
        mesh.cylinder(f,.13*rc,cap,cap+.025,8,Material.BOLT);
        for(int i=0;i<8;i++)mesh.cylinder(new Frame(f.at(i*Math.PI/4,.92*rc,edge),stem,run),Math.max(.007,.018*casing),0,.022,6,Material.BOLT);
        if(!accessory)for(int face:new int[]{axis,axis^1}) {
            var end=LineGeometry.end(key,face,od);
            end=new LineGeometry.End(end.outside(),true,end.reducer(),end.reducer()+PipeSizes.flangeThickness(end.outside())+PipeSizes.hub(end.outside()),end.joined());
            LineGeometry.arm(mesh,face,h,od,end,LineGeometry.ref(face),PipeSizes.sides(od),true);
        }
    }
}
