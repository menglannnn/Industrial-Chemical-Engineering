package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.math.DeterministicMath;
import java.util.*;

/** Metre-sized, cached round instrument mesh. Front faces wind counter-clockwise toward +z. */
public final class InstrumentGeometry {
    private InstrumentGeometry(){}
    public record Quad(double[] vertices,int colour){public Quad{vertices=vertices.clone();}@Override public double[] vertices(){return vertices.clone();} public Quad front(){double z=(vertices[3]-vertices[0])*(vertices[7]-vertices[1])-(vertices[4]-vertices[1])*(vertices[6]-vertices[0]);return z>=0?this:new Quad(new double[]{vertices[9],vertices[10],vertices[11],vertices[6],vertices[7],vertices[8],vertices[3],vertices[4],vertices[5],vertices[0],vertices[1],vertices[2]},colour);}}
    private static final List<Quad> HOUSING=build();
    public static List<Quad> housing(){return HOUSING;}
    private static List<Quad> build(){var q=new ArrayList<Quad>();for(int i=0;i<32;i++){double a=i*2*Math.PI/32,b=(i+1)*2*Math.PI/32;double x=DeterministicMath.cos(a),y=DeterministicMath.sin(a),u=DeterministicMath.cos(b),v=DeterministicMath.sin(b);q.add(quad(.15*x,.15*y,-.06,.15*u,.15*v,-.06,.15*u,.15*v,.06,.15*x,.15*y,.06,0xff8b949b));q.add(quad(.15*x,.15*y,.06,.15*u,.15*v,.06,.127*u,.127*v,.06,.127*x,.127*y,.06,0xffc5cbd0));q.add(quad(0,0,.041,.127*x,.127*y,.041,.127*u,.127*v,.041,0,0,.041,0xfff0eddd));q.add(quad(0,0,-.061,.15*u,.15*v,-.061,.15*x,.15*y,-.061,0,0,-.061,0xff59626a));}for(int i=0;i<=20;i++){double a=Math.PI*(1.25-i*1.5/20),x=DeterministicMath.cos(a),y=DeterministicMath.sin(a),r=i%5==0?.089:.101;double w=.0017;q.add(quad(r*x-w*y,r*y+w*x,.043,.117*x-w*y,.117*y+w*x,.043,.117*x+w*y,.117*y-w*x,.043,r*x+w*y,r*y-w*x,.043,0xff20282c).front());}box(q,-.022,-.022,-.18,.022,.022,-.061,0xff7e888c);return List.copyOf(q);}
    private static Quad quad(double ax,double ay,double az,double bx,double by,double bz,double cx,double cy,double cz,double dx,double dy,double dz,int c){return new Quad(new double[]{ax,ay,az,bx,by,bz,cx,cy,cz,dx,dy,dz},c);}
    private static void box(List<Quad> q,double x,double y,double z,double X,double Y,double Z,int c){q.add(quad(x,y,z,X,y,z,X,y,Z,x,y,Z,c));q.add(quad(x,Y,Z,X,Y,Z,X,Y,z,x,Y,z,c));q.add(quad(x,y,Z,x,Y,Z,x,Y,z,x,y,z,c));q.add(quad(X,y,z,X,Y,z,X,Y,Z,X,y,Z,c));}
    public static Quad needle(double ratio){double a=Math.PI*(1.25-Math.clamp(ratio,0,1)*1.5),x=DeterministicMath.cos(a),y=DeterministicMath.sin(a);return quad(-.018*x-.005*y,-.018*y+.005*x,.048,.094*x-.0015*y,.094*y+.0015*x,.048,.094*x+.0015*y,.094*y-.0015*x,.048,-.018*x+.005*y,-.018*y-.005*x,.048,0xff192127).front();}
}
