package com.wormzjl.createcheme.runtime.fluid;

import com.google.gson.JsonParser;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class FluidPropertyReloadGuardTest {
    private final MaterialCatalog original=MaterialCatalog.bundled();
    private final String packageId=FluidPresetCatalog.NETWORK_PACKAGE;
    private FluidPropertyReloadGuard guard(){return new FluidPropertyReloadGuard(original,packageId,FluidThermodynamics.forNetwork(original,packageId));}
    private MaterialCatalog changed(String path,java.util.function.Consumer<com.google.gson.JsonObject> edit) {
        var resources=new HashMap<>(original.resources());var full="data/createcheme/materials/"+path;
        var json=JsonParser.parseString(resources.get(full)).getAsJsonObject();edit.accept(json);resources.put(full,json.toString());return MaterialCatalog.parse(resources);
    }
    @Test void metadataOnlyReloadKeepsQualifiedScienceAndNumericalChangeWithoutRevisionBumpHolds() {
        var guard=guard();assertTrue(guard.inspect(original).isEmpty());
        var names=changed("components/crude_pc07.json",j->j.addProperty("fallback","Renamed petroleum"));
        assertTrue(guard.inspect(names).isEmpty());
        var science=changed("properties/crude_pc07.json",j->j.addProperty("molecular_weight_kg_per_mol",.31));
        assertEquals(original.requirePackage(packageId).revision(),science.requirePackage(packageId).revision());
        assertTrue(guard.inspect(science).isPresent());assertTrue(guard.inspect(science).isPresent());
        assertTrue(guard.inspect(MaterialCatalog.parse(original.resources())).isEmpty());
    }
    @Test void viscosityOnlyChangeInvalidatesThePinnedFluidModel() {
        var resources=new HashMap<>(original.resources());
        var path=resources.keySet().stream().filter(p->p.contains("/water/")).findFirst().orElseThrow();
        var json=JsonParser.parseString(resources.get(path)).getAsJsonObject();
        var coefficients=json.getAsJsonObject("viscosity").getAsJsonObject("liquid").getAsJsonArray("coefficients");
        coefficients.set(0,new com.google.gson.JsonPrimitive(coefficients.get(0).getAsDouble()*1.01));
        resources.put(path,json.toString());var changed=MaterialCatalog.parse(resources);
        assertNotEquals(original.viscosityFingerprint(packageId),changed.viscosityFingerprint(packageId));
        assertTrue(guard().inspect(changed).isPresent());
    }
    /**
     * A crystal record is science of the package that admits it (owner decision D23: its crystals are solids the vessels
     * hold and exchange with the fluid): a reload that changes the CO2-I Gibbs function under a running pilot world holds
     * it, as a property record's change does; a reload of the same data, or of the record's text alone, does not.
     */
    @Test void aChangedCrystalRecordHoldsTheWorld() {
        var pilot=com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog.catalog();String id=com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog.PACKAGE_ID;
        var guard=new FluidPropertyReloadGuard(pilot,id,FluidThermodynamics.forNetwork(pilot,id));
        assertTrue(guard.inspect(MaterialCatalog.parse(com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog.resources())).isEmpty(),"the same data reloaded");
        String record="data/createcheme/materials/crystals/carbon_dioxide_i.json";
        assertTrue(guard.inspect(com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog.parseWith(record,o->o.addProperty("source","edited text"))).isEmpty(),"a text edit");
        var edited=com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog.parseWith(record,o->{
            var g=o.getAsJsonObject("gibbs_function").getAsJsonArray("g2_to_g10");g.set(0,new com.google.gson.JsonPrimitive(g.get(0).getAsDouble()*1.001));});
        assertEquals(FluidPropertyReloadGuard.HOLD,guard.inspect(edited).orElse("no hold"),"a changed crystal Gibbs function");
    }
}
