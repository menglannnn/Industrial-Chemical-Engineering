package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;

/** Actual-mesh sweep of the reported coplanar tip, with the integrated 4ad6dc97 geometry as control. */
public final class HandleTipAudit {
    private record Tip(Set<Mesh.Material> materials,double area) {}
    public static void main(String[] args) {
        int cases=0,oldConflicts=0;double worstAreaError=0;
        for(int code=4;code<=64;code++)for(int facing=0;facing<6;facing++)for(boolean open:new boolean[]{true,false})for(boolean accessory:new boolean[]{true,false}) {
            int mask=3<<(facing&~1);long faces=(long)code<<(7*(facing&~1))|(long)code<<(7*((facing&~1)^1));
            var key=new LineGeometry.Key(Kind.PIPE,facing,mask,code,faces,mask,false);
            double od=PipeSizes.outsideOf(code),length=Math.min(.95,Math.max(.36,1.10*od));
            double width=Math.max(.065,Math.min(.19,.24*od)),thickness=Math.max(.035,.085*od);
            var run=LineGeometry.step(facing|1);double[] stem=facing<2?new double[]{0,0,-1}:new double[]{0,1,0};
            var lever=open?run:Mesh.cross(stem,run);
            var old=FlickeringShutoffValveGeometry.polygons(key,open,accessory);var fixed=ShutoffValveGeometry.polygons(key,open,accessory);
            var before=tip(old,lever,length);var after=tip(fixed,lever,length);
            if(before.materials.size()>1)oldConflicts++;
            if(!before.materials.equals(EnumSet.of(Mesh.Material.FLANGE,Mesh.Material.GUARD,Mesh.Material.BOLT)))throw new AssertionError("Control did not reproduce the reported three tip layers");
            if(!after.materials.equals(EnumSet.of(Mesh.Material.BOLT)))throw new AssertionError("Competing tip face remains");
            double wanted=1.30*width*1.55*thickness,error=Math.abs(after.area-wanted)/wanted;
            worstAreaError=Math.max(worstAreaError,error);if(error>1e-8)throw new AssertionError("Tip face missing/duplicated: "+error);
            var a=bounds(old);var b=bounds(fixed);for(int i=0;i<6;i++)if(Math.abs(a[i]-b[i])>1e-9)throw new AssertionError("Exterior bounds changed");
            cases++;
        }
        System.out.printf(Locale.ROOT,"{\"cases\":%d,\"baseline_conflicts\":%d,\"fixed_conflicts\":0,\"unchanged_bounds\":%d,\"worst_tip_area_relative_error\":%.12g}%n",cases,oldConflicts,cases,worstAreaError);
    }
    private static Tip tip(List<Mesh.Polygon> polygons,double[] lever,double length) {
        var materials=EnumSet.noneOf(Mesh.Material.class);double area=0;
        for(var polygon:polygons) {
            var p=polygon.positions();boolean atTip=true;var normal=new double[3];
            for(int i=0;i<polygon.size();i++) {
                int a=3*i,b=3*((i+1)%polygon.size());
                if(Math.abs((p[a]-.5)*lever[0]+(p[a+1]-.5)*lever[1]+(p[a+2]-.5)*lever[2]-length)>1e-9)atTip=false;
                normal[0]+=(p[a+1]-p[b+1])*(p[a+2]+p[b+2]);
                normal[1]+=(p[a+2]-p[b+2])*(p[a]+p[b]);
                normal[2]+=(p[a]-p[b])*(p[a+1]+p[b+1]);
            }
            double directed=Mesh.dot(normal,lever);
            if(atTip&&directed>1e-12){materials.add(polygon.material());area+=directed/2;}
        }
        return new Tip(materials,area);
    }
    private static double[] bounds(List<Mesh.Polygon> polygons) {
        double[] out={1e9,1e9,1e9,-1e9,-1e9,-1e9};
        for(var polygon:polygons)for(int i=0;i<polygon.positions().length;i++){int a=i%3;double v=polygon.positions()[i];out[a]=Math.min(out[a],v);out[a+3]=Math.max(out[a+3],v);}
        return out;
    }
}
