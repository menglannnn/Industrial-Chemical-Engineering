package com.wormzjl.icheme.science.equipment;

import java.util.*;

/** Actual cap geometry: reproduce the solid marker's overlap, require a clear hollow collar at every gauge level. */
public final class GaugeCapAudit {
    record Rect(double x0,double x1,double z0,double z1) {
        double overlap(Rect b){return Math.max(0,Math.min(x1,b.x1)-Math.max(x0,b.x0))*Math.max(0,Math.min(z1,b.z1)-Math.max(z0,b.z0));}
        double area(){return (x1-x0)*(z1-z0);}
    }
    static List<Rect> caps(List<Mesh.Polygon> faces,double half,boolean top){
        var result=new ArrayList<Rect>();
        for(var f:faces){var p=f.positions();var n=f.normals();if(n[1]!=(top?1:-1))continue;
            double x0=1,x1=-1,z0=1,z1=-1;
            for(int i=0;i<p.length;i+=3){double x=half*(2*p[i]-1),z=half*(2*p[i+2]-1);x0=Math.min(x0,x);x1=Math.max(x1,x);z0=Math.min(z0,z);z1=Math.max(z1,z);}
            result.add(new Rect(x0,x1,z0,z1));
        }
        return result;
    }
    public static void main(String[] args){
        double liquid=TankGaugeGeometry.BAND_HALF,outer=TankGaugeGeometry.MARKER_HALF,inner=TankGaugeGeometry.MARKER_INNER_HALF;
        var surface=new Rect(-liquid,liquid,-liquid,liquid);
        double control=0,repaired=0;
        for(boolean top:new boolean[]{false,true}){
            for(var r:caps(TankGaugeGeometry.bandFaces(),outer,top))control+=r.overlap(surface);
            var ring=caps(TankGaugeGeometry.markerFaces(),outer,top);double area=0;
            for(int i=0;i<ring.size();i++){var r=ring.get(i);repaired+=r.overlap(surface);area+=r.area();for(int j=0;j<i;j++)if(r.overlap(ring.get(j))>1e-14)throw new AssertionError("collar overlaps itself");}
            if(Math.abs(area-4*(outer*outer-inner*inner))>1e-12)throw new AssertionError("wrong annular cap area");
        }
        if(!(control>0)||repaired>1e-14)throw new AssertionError("cap overlap control="+control+" repaired="+repaired);
        int samples=0,oldConflicts=0;
        for(double high:new double[]{3,11,23})for(int step=0;step<=1002;step++){
            double level=high*step/1000,ring=Math.max(.3,Math.min(high,level));
            double top=Math.min(high,ring+TankGaugeGeometry.MARKER_HEIGHT/2);
            double liquidTop=Math.min(high,level);
            if(top==liquidTop){oldConflicts++;for(var r:caps(TankGaugeGeometry.markerFaces(),outer,true))if(r.overlap(surface)>1e-14)throw new AssertionError("clipped cap flicker");}
            samples++;
        }
        System.out.printf(Locale.ROOT,"PASS: %d level/end-stop cases; %d old full/overflow cap coincidences; old two-cap overlap %.8f m2, hollow collar %.8f m2; annular area and no self-overlap verified%n",samples,oldConflicts,control,repaired);
    }
}
