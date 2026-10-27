package com.wormzjl.icheme.runtime.instrument;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SensorStoreTest {
    @Test void valveHasOneModuleAndRemovalRefundsOnlyOnceAcrossReload(){
        var store=new InstrumentService.Store();store.installLegacy(7,Instrument.Type.PRESSURE_VALVE);
        var key=new Instrument.Key(7,"valve_sensor");var id=new UUID(1,2);
        var sensor=store.installSensor(key,id,Sensor.Kind.PRESSURE,store.revision(7));assertNotNull(sensor);
        assertNull(store.installSensor(key,new UUID(1,3),Sensor.Kind.FLOW,store.revision(7)));
        var loaded=InstrumentService.Store.load(store.save(new CompoundTag()));assertEquals(sensor,loaded.sensor(key));
        assertNotNull(loaded.removeSensor(key,id));assertNull(loaded.removeSensor(key,id));
        var again=InstrumentService.Store.load(loaded.save(new CompoundTag()));assertNull(again.sensor(key));
        assertNotNull(again.legacy(7));
    }
    @Test void vesselHasIndependentBodyAndBootMetersAndDamagedFactsRefuse(){
        var store=new InstrumentService.Store();
        assertNotNull(store.installSensor(new Instrument.Key(9,"level_body"),new UUID(1,1),Sensor.Kind.LEVEL,0));
        assertNotNull(store.installSensor(new Instrument.Key(9,"level_boot"),new UUID(1,2),Sensor.Kind.LEVEL,store.revision(9)));
        var saved=store.save(new CompoundTag());assertEquals(store.sensors(9),InstrumentService.Store.load(saved).sensors(9));
        saved.getList("sensors",10).getCompound(0).putString("kind","UNKNOWN");
        assertThrows(IllegalArgumentException.class,()->InstrumentService.Store.load(saved));
    }
    @Test void legacyPressureAssemblyConvertsExactlyOnceAndNeverRecreatesARemovedProbe(){
        var store=new InstrumentService.Store();store.installLegacy(5,Instrument.Type.PRESSURE_VALVE);
        var legacy=new com.wormzjl.icheme.science.fluid.network.FlowControl.PressureValve(350000);
        var control=store.convertLegacyValve(5,legacy);assertFalse(control.bare());assertEquals(350000,control.targetPressure());
        var key=new Instrument.Key(5,"valve_sensor");assertEquals(control.sensor(),store.sensor(key).instance());
        store=InstrumentService.Store.load(store.save(new CompoundTag()));
        assertEquals(control.sensor(),store.convertLegacyValve(5,legacy).sensor());
        store.removeSensor(key,control.sensor());store=InstrumentService.Store.load(store.save(new CompoundTag()));
        var removed=store.convertLegacyValve(5,legacy);assertTrue(removed.bare());assertEquals(350000,removed.targetPressure());assertNull(store.sensor(key));
    }
    @Test void duplicatedPersistentSensorIdentitiesRefuseTheFacts(){
        var store=new InstrumentService.Store();store.installSensor(new Instrument.Key(1,"level_body"),new UUID(1,1),Sensor.Kind.LEVEL,0);store.installSensor(new Instrument.Key(2,"level_body"),new UUID(1,2),Sensor.Kind.LEVEL,0);
        var saved=store.save(new CompoundTag());saved.getList("sensors",10).getCompound(1).putUUID("instance",new UUID(1,1));
        assertThrows(IllegalArgumentException.class,()->InstrumentService.Store.load(saved));
    }

}
