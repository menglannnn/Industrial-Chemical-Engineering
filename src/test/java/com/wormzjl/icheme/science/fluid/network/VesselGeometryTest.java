package com.wormzjl.icheme.science.fluid.network;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VesselGeometryTest {
    @Test void independentTenPercentHeightReferences(){
        var drum=VesselGeometry.round(VesselGeometry.Form.HORIZONTAL_DRUM,2,6,0,0);
        assertEquals(.32041532283655283,drum.heightAt(.1*drum.capacity()),1e-12);
        var sphere=VesselGeometry.round(VesselGeometry.Form.SPHERE,4,0,0,0);
        assertEquals(.7832004226363669,sphere.heightAt(.1*sphere.capacity()),1e-12);
    }
    @Test void everyProfileIsMonotoneAndRoundTripsIncludingBootAndEndpoints(){
        for(var g:List.of(VesselGeometry.prism(3,2),VesselGeometry.round(VesselGeometry.Form.CYLINDER,4,3,0,0),
                VesselGeometry.round(VesselGeometry.Form.SPHERE,4,0,0,0),
                VesselGeometry.round(VesselGeometry.Form.VERTICAL_DRUM,2,6,0,0),
                VesselGeometry.round(VesselGeometry.Form.HORIZONTAL_DRUM,2,6,0,0),
                VesselGeometry.round(VesselGeometry.Form.HORIZONTAL_DRUM,2,6,2.0/3,.7))){
            double previous=-Double.MAX_VALUE;
            for(int i=0;i<=1000;i++){double volume=g.capacity()*i/1000,h=g.heightAt(volume);assertTrue(h>=previous);assertEquals(volume,g.volumeAt(h),1e-12*g.capacity());previous=h;}
            assertEquals(g.capacity(),g.volumeAt(g.height()),1e-12*g.capacity());
        }
    }
    @Test void bootWeirIsAHeightThresholdAndIncludesBootVolume(){
        var g=VesselGeometry.round(VesselGeometry.Form.HORIZONTAL_DRUM,2,6,2.0/3,.7);
        assertEquals(0,g.heightAt(g.bootCapacity()),1e-12);
        assertEquals(2.0/3,g.heightAt(g.weirVolume()),1e-12);
        assertTrue(g.weirVolume()>g.bootCapacity());
        assertNotEquals(g.capacity()/3,g.weirVolume(),1e-3);
        assertTrue(g.heightAt(.5*g.bootCapacity())<0);
    }
    @Test void hydrostaticHeadUsesLayerHeightsAndDensities(){
        var g=VesselGeometry.round(VesselGeometry.Form.SPHERE,4,0,0,0);
        double water=g.volumeAt(1),total=g.volumeAt(2.5);
        assertEquals(PassiveStepSolver.GRAVITY*(1000+800*1.5),g.head(water,1000*water,total-water,800*(total-water)),1e-9);
    }
    @Test void invalidAndDamagedGeometryRefuses(){
        assertThrows(IllegalArgumentException.class,()->new VesselGeometry(VesselGeometry.Form.SPHERE,1,2,2,0,0,0));
        assertThrows(IllegalArgumentException.class,()->VesselGeometry.prism(Double.NaN,1));
    }
}
