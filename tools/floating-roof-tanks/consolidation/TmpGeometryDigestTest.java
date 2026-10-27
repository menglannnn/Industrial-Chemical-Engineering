package com.wormzjl.icheme.science.equipment;

import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import org.junit.jupiter.api.Test;
import java.util.*;

/** Temporary consolidation probe (not committed): one digest of every pipe-fitting, gauge and tank-gauge mesh. */
class TmpGeometryDigestTest {
    private static long hash=1125899906842597L;
    private static void mix(long v){hash=31*hash+v;}
    private static void mix(double v){mix(Double.doubleToRawLongBits(v));}
    private static void mix(List<Mesh.Polygon> polygons){mix(polygons.size());for(var p:polygons){mix(p.material().ordinal());for(double v:p.positions())mix(v);for(double v:p.uvs())mix(v);for(double v:p.normals())mix(v);}}

    @Test void digest() throws Exception {
        var keys=new ArrayList<LineGeometry.Key>();
        for(int face=0;face<6;face++)for(int mask:new int[]{0,1<<face,(1<<face)|(1<<(face^1)),3,12,48})for(double bore:new double[]{.001,.05,.2,.21,.4,.8})for(long faces:new long[]{0,0x0101010101L})for(int flanged:new int[]{0,63})
            keys.add(new LineGeometry.Key(Kind.PIPE,face,mask,PipeSizes.code(PipeSizes.outside(bore)),faces,flanged,false));
        for(var key:keys)for(boolean accessory:List.of(false,true)){
            mix(ValveGeometry.check(key,accessory));
            for(boolean open:List.of(false,true))mix(ValveGeometry.shutoff(key,open,accessory));
            mix(ValveGeometry.pressure(key,accessory));
            if(SightGlassGeometry.supports(key.connections()))for(var axis:SightGlassGeometry.Axis.values()){
                try{var g=SightGlassGeometry.build(new SightGlassGeometry.Key(key,axis),accessory);mix(g.metal());mix(g.glass());mix(g.liquid());}catch(IllegalArgumentException e){mix(-1);}
            }
            if(InlineMoverGeometry.supports(key.connections(),key.facing()))for(boolean compressor:List.of(false,true))mix(InlineMoverGeometry.polygons(key,compressor,accessory));
        }
        for(var key:keys){
            mix(LineGeometry.filterPolygons(key));
            mix(LineGeometry.polygons(new LineGeometry.Key(Kind.VALVE,key.facing(),key.connections(),key.outside(),key.faces(),key.flanged(),key.spool())));
        }
        mix(TankGaugeGeometry.bandFaces());mix(TankGaugeGeometry.markerFaces());
        var m=new Mesh();TankGaugeGeometry.sleeve(m,.3,.7,1.5,7.25);mix(m.cell(0,0,0));
        for(var q:InstrumentGeometry.housing()){for(double v:q.vertices())mix(v);mix(q.colour());}
        for(double r:new double[]{0,.3,1})for(double v:InstrumentGeometry.needle(r).vertices())mix(v);
        for(boolean b:List.of(false,true))for(boolean t:List.of(false,true))for(var q:InstrumentGeometry.connection(new double[]{.01,-.2,.03},b,t,.1)){for(double v:q.vertices())mix(v);mix(q.colour());}
        for(var heading:EquipmentShape.Heading.values())for(double d:new double[]{.5,2,10}){
            for(var s:List.of(EquipmentShape.drum(d,4,EquipmentShape.Orientation.VERTICAL,heading,0),EquipmentShape.storage(EquipmentShape.Kind.TANK,Math.max(4,d),4,heading,0),EquipmentShape.storage(EquipmentShape.Kind.SPHERE,Math.max(4,d),1,heading,2)))
                for(var socket:InstrumentGeometry.sockets(s)){mix(socket.id().hashCode());mix(socket.x());mix(socket.y());mix(socket.z());mix(socket.pressure()?1:0);mix(socket.top()?1:0);}
        }
        for(int strength:new int[]{0,1,128,255})for(var motion:List.of(new SightGlassGeometry.Motion(1,0),new SightGlassGeometry.Motion(0,-1))){mix(SightGlassGeometry.bubbleColour(0xC0336699,strength));for(var b:SightGlassGeometry.bubbles(1234.5,99,strength,motion)){mix(b.x());mix(b.y());mix(b.radius());}}
        for(var kind:List.of(EquipmentShape.Kind.PUMP,EquipmentShape.Kind.COMPRESSOR,EquipmentShape.Kind.CENTRIFUGAL_COMPRESSOR)){var design=UnitDesign.of(kind);for(double size=design.minSize();size<=design.maxSize()+1e-9;size+=design.sizeStep())mix(UnitDesign.soundGain(EquipmentShape.unit(kind,size,design.minLength(size),EquipmentShape.Heading.NORTH,design.defaultSupport(size,design.minLength(size)))));}
        var out=java.nio.file.Path.of(System.getProperty("user.dir"),"build","tmp","geometry-digest.txt");
        java.nio.file.Files.writeString(out,keys.size()+" keys digest "+Long.toHexString(hash)+"\n",java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);
    }
}
