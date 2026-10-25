package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;
import java.util.*;

/** Closed instrument casing and independent process connectors; metres, dial normal +z. */
public final class InstrumentGeometry {
    private InstrumentGeometry(){}
    public static final double RADIUS=.17,FACE=.045,TICKS=.047,LABEL=.050,NEEDLE=.057,RIM=.068;
    public record Quad(double[] vertices,int colour) {
        public Quad{vertices=vertices.clone();}
        @Override public double[] vertices(){return vertices.clone();}
        public Quad front(){double z=(vertices[3]-vertices[0])*(vertices[7]-vertices[1])-(vertices[4]-vertices[1])*(vertices[6]-vertices[0]);return z>=0?this:new Quad(new double[]{vertices[9],vertices[10],vertices[11],vertices[6],vertices[7],vertices[8],vertices[3],vertices[4],vertices[5],vertices[0],vertices[1],vertices[2]},colour);}
    }
    private static final List<Quad> HOUSING=build();
    public static List<Quad> housing(){return HOUSING;}
    private static List<Quad> build(){
        var q=new ArrayList<Quad>();double inner=.143,back=-.07;
        for(int i=0;i<48;i++){
            double a=i*2*Math.PI/48,b=(i+1)*2*Math.PI/48;
            double x=DeterministicMath.cos(a),y=DeterministicMath.sin(a),u=DeterministicMath.cos(b),v=DeterministicMath.sin(b);
            // Outer wall, front bezel, inner bezel wall, dial and rear cap form one closed envelope.
            q.add(quad(RADIUS*x,RADIUS*y,back,RADIUS*u,RADIUS*v,back,RADIUS*u,RADIUS*v,RIM,RADIUS*x,RADIUS*y,RIM,0xff8b949b));
            q.add(quad(RADIUS*x,RADIUS*y,RIM,RADIUS*u,RADIUS*v,RIM,inner*u,inner*v,RIM,inner*x,inner*y,RIM,0xffc5cbd0));
            q.add(quad(inner*x,inner*y,FACE,inner*x,inner*y,RIM,inner*u,inner*v,RIM,inner*u,inner*v,FACE,0xff778189));
            q.add(quad(0,0,FACE,inner*x,inner*y,FACE,inner*u,inner*v,FACE,0,0,FACE,0xfffaf7e8));
            q.add(quad(0,0,back,RADIUS*u,RADIUS*v,back,RADIUS*x,RADIUS*y,back,0,0,back,0xff59626a));
        }
        for(int i=0;i<=20;i++){
            double a=Math.PI*(1.25-i*1.5/20),x=DeterministicMath.cos(a),y=DeterministicMath.sin(a),r=i%5==0?.105:.119;
            double w=i%5==0?.003:.0022;
            q.add(quad(r*x-w*y,r*y+w*x,TICKS,.136*x-w*y,.136*y+w*x,TICKS,.136*x+w*y,.136*y-w*x,TICKS,r*x+w*y,r*y-w*x,TICKS,0xff101517).front());
        }
        return List.copyOf(q);
    }
    private static Quad quad(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,int c){return new Quad(new double[]{ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz},c);}
    public static Quad needle(double ratio){double a=Math.PI*(1.25-Math.clamp(ratio,0,1)*1.5),x=DeterministicMath.cos(a),y=DeterministicMath.sin(a);return quad(-.021*x-.006*y,-.021*y+.006*x,NEEDLE,.104*x-.002*y,.104*y+.002*x,NEEDLE,.104*x+.002*y,.104*y-.002*x,NEEDLE,-.021*x+.006*y,-.021*y-.006*x,NEEDLE,0xff101517).front();}
    public static List<Quad> connection(double[] tap,boolean bottomEntry,boolean thermometer){return connection(tap,bottomEntry,thermometer,.12);}
    public static List<Quad> connection(double[] tap,boolean bottomEntry,boolean thermometer,double probeDepth){
        if(!Double.isFinite(probeDepth)||probeDepth<=0)throw new IllegalArgumentException("Invalid probe depth");
        var q=new ArrayList<Quad>();double[] end=bottomEntry?new double[]{0,-.15,0}:new double[]{0,0,-.055};
        double[] elbow=bottomEntry?new double[]{0,tap[1],0}:tap;
        if(bottomEntry&&Math.abs(tap[2])>.001)cylinder(q,tap,elbow,.014,12,0xff8e999f);
        cylinder(q,elbow,end,.014,12,0xff8e999f);
        var direction=unit(sub(bottomEntry&&Math.abs(tap[2])>.001?elbow:end,tap));
        var bossEnd=add(tap,direction,.045);cylinder(q,add(tap,direction,-.01),bossEnd,thermometer?.036:.032,6,0xffb3bbbd);
        if(thermometer)cylinder(q,add(tap,direction,-probeDepth),bossEnd,.006,12,0xff89979d);
        return List.copyOf(q);
    }
    private static void cylinder(List<Quad> out,double[] start,double[] end,double radius,int sides,int colour){
        var axis=unit(sub(end,start));var ref=unit(cross(axis,Math.abs(axis[1])>.9?new double[]{1,0,0}:new double[]{0,1,0}));var side=cross(axis,ref);
        for(int i=0;i<sides;i++){
            double a=2*Math.PI*i/sides,b=2*Math.PI*(i+1)/sides;var p=radial(start,ref,side,a,radius);var q=radial(start,ref,side,b,radius);var r=radial(end,ref,side,b,radius);var s=radial(end,ref,side,a,radius);
            out.add(points(p,q,r,s,colour));out.add(points(start,q,p,start,colour));out.add(points(end,s,r,end,colour));
        }
    }
    private static Quad points(double[] a,double[] b,double[] c,double[] d,int colour){return quad(a[0],a[1],a[2],b[0],b[1],b[2],c[0],c[1],c[2],d[0],d[1],d[2],colour);}
    private static double[] radial(double[] p,double[] ref,double[] side,double angle,double r){return add(add(p,ref,r*DeterministicMath.cos(angle)),side,r*DeterministicMath.sin(angle));}
    private static double[] sub(double[] a,double[] b){return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
    private static double[] add(double[] p,double[] axis,double amount){return new double[]{p[0]+axis[0]*amount,p[1]+axis[1]*amount,p[2]+axis[2]*amount};}
    private static double[] unit(double[] a){double length=Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]);if(length<1e-12)throw new IllegalArgumentException("Empty instrument connector");return new double[]{a[0]/length,a[1]/length,a[2]/length};}
    private static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
}
