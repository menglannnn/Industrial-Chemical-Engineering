package com.wormzjl.icheme.runtime.instrument;
import com.wormzjl.icheme.runtime.fluid.*;
import com.wormzjl.icheme.science.fluid.network.VesselGeometry;
import com.wormzjl.icheme.science.fluid.state.SolidInventory;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SensorReadingTest {
    @Test void levelPercentageIsHeightAndEmptyIsAValidZero(){
        var g=VesselGeometry.round(VesselGeometry.Form.SPHERE,4,0,0,0);
        double[][] moles={new double[]{0,0},new double[]{0,1},new double[]{1,0}};
        var state=new FluidView.State(200000,298.15,g.capacity(),1,new double[]{0,.1*g.capacity(),.9*g.capacity()},moles,SolidInventory.EMPTY,g.capacity());
        var sensor=new Sensor.Installed(new Instrument.Key(1,"level_body"),new UUID(1,1),1,Sensor.Kind.LEVEL);
        var view=new FluidView(1,0,100,100,"READY",state,0,List.of()).withTank(new FluidView.Tank(g.height(),false,0,0,g.capacity(),200000.0,210000.0,g));
        var reading=Sensor.read(sensor,view,null);assertEquals(.7832004226363669,reading.value(),1e-12);assertEquals(19.58001056590917,reading.secondary(),1e-10);
        var empty=new FluidView(1,0,100,100,"EMPTY",null,0,List.of()).withTank(new FluidView.Tank(g.height(),false,0,0,g.capacity(),null,null,g));
        var zero=Sensor.read(sensor,empty,null);assertEquals(0,zero.value());assertEquals(Instrument.Availability.AVAILABLE,zero.availability());
    }
    @Test void aPipeTemperatureNeedsAcceptedFlowAndRetainsItsOriginalSampleTimeWhenStopped(){
        var sensor=new Sensor.Installed(new Instrument.Key(1,"pipe_north"),new UUID(1,1),1,Sensor.Kind.TEMPERATURE);
        var view=new FluidView(1,0,100,100,"READY",null,0,List.of());
        assertEquals(Instrument.Availability.UNAVAILABLE,Sensor.read(sensor,view,new FluidWorldAuthority.InstrumentReading(200000,300,false),null).availability());
        var flowing=Sensor.read(sensor,view,new FluidWorldAuthority.InstrumentReading(200000,310,true),null);
        var later=new FluidView(1,0,200,200,"READY",null,0,List.of());
        var stopped=Sensor.read(sensor,later,new FluidWorldAuthority.InstrumentReading(200000,290,false),flowing);
        assertEquals(310,stopped.value());assertEquals(100,stopped.tick());assertEquals(Instrument.Availability.NO_FLOW,stopped.availability());
    }
    @Test void upstreamModulePressureDoesNotDependOnPipePresentationMapping(){
        var sensor=new Sensor.Installed(new Instrument.Key(1,"valve_sensor"),new UUID(1,1),1,Sensor.Kind.PRESSURE);
        var state=new FluidView.State(350000,298.15,1,1,new double[]{0,0,1},new double[][]{new double[]{0,0},new double[]{0,0},new double[]{1,0}},SolidInventory.EMPTY,1);
        var view=new FluidView(1,0,100,100,"READY",state,0,List.of());
        assertEquals(350000,Sensor.read(sensor,view,null).value());assertEquals(Instrument.Availability.AVAILABLE,Sensor.read(sensor,view,null).availability());
    }
    @Test void futureTargetsUseHeightGeometryAndRetainBootDatum(){
        var g=VesselGeometry.round(VesselGeometry.Form.HORIZONTAL_DRUM,2,4,1,1);
        var target=ControlBinding.LevelTarget.defaults();assertEquals(g.volumeAt(1),target.volume(g,false));assertEquals(g.volumeAt(-.625),target.volume(g,true));
        assertThrows(IllegalArgumentException.class,()->new ControlBinding.LevelTarget(101,ControlBinding.Basis.HEIGHT_PERCENT));
        var source=new Sensor.Installed(new Instrument.Key(1,"level_boot"),new UUID(1,1),1,Sensor.Kind.LEVEL);
        assertEquals(ControlBinding.Action.OUTLET,new ControlBinding(2,source,1,target,ControlBinding.Action.OUTLET).action());
    }

}

