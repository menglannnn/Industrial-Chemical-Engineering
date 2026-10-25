package com.wormzjl.icheme.science.equipment;

import java.util.*;

/** Fixed front service taps, using each native vessel design's own axis and surface. */
public final class InstrumentSockets {
    private InstrumentSockets(){}
    public record Socket(String id,double x,double y,double z,boolean pressure,boolean top) {}
    public static List<Socket> of(EquipmentShape s){boolean drum=s.kind()==EquipmentShape.Kind.DRUM&&s.orientation()==EquipmentShape.Orientation.VERTICAL,tank=s.kind()==EquipmentShape.Kind.TANK,sphere=s.kind()==EquipmentShape.Kind.SPHERE;if(!drum&&!tank&&!sphere)return List.of();double a=s.axis(),r=s.radius(),lower=s.bottomTangent()+Math.min(.5,s.tangentLength()*.25),upper=s.topTangent()-Math.min(.5,s.tangentLength()*.25);
        if(tank){var p=TankDesign.Plan.of(s.diameter(),(int)s.tangentLength());a=p.a();lower=.8;}
        if(sphere){var p=SphereDesign.Plan.of(s.diameter(),s.support());a=p.a();upper=p.yc()+p.r()*.65;}
        double offset=Math.min(.3,r*.75);var result=new ArrayList<Socket>();
        if(drum||sphere)add(result,s,a,r,"pressure_top",upper,-offset,true,true);
        if(drum||tank)add(result,s,a,r,"pressure_bottom",lower,-offset,true,false);
        add(result,s,a,r,sphere?"temperature_top":"temperature_bottom",sphere?upper:lower,offset,false,sphere);
        return List.copyOf(result);
    }
    private static void add(List<Socket> out,EquipmentShape s,double a,double r,String id,double y,double offset,boolean pressure,boolean top){double radialSquared=r*r;if(s.kind()==EquipmentShape.Kind.SPHERE){double dy=y-s.support()-r;radialSquared-=dy*dy;}double z=a-Math.sqrt(Math.max(.0001,radialSquared-offset*offset))-.16;double[] p=s.transform().point(new double[]{a+offset,y,z});out.add(new Socket(id,p[0],p[1],p[2],pressure,top));}
}
