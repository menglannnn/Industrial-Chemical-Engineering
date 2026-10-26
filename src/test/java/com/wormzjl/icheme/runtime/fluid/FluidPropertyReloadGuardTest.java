package com.wormzjl.icheme.runtime.fluid;

import com.google.gson.JsonParser;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import static org.junit.jupiter.api.Assertions.*;

class FluidPropertyReloadGuardTest {
    private final MaterialCatalog original=MaterialCatalog.bundled();
    private final String packageId=FluidPresetCatalog.NETWORK_PACKAGE;
    private FluidPropertyReloadGuard guard(){return new FluidPropertyReloadGuard(original,packageId,FluidThermodynamics.forNetwork(original,packageId));}
    /** The bundled resources with each record of {@code edits} (a path under {@code materials/}) edited as JSON. */
    private MaterialCatalog changed(java.util.Map<String,java.util.function.Consumer<com.google.gson.JsonObject>> edits) {
        var resources=new HashMap<>(original.resources());
        edits.forEach((path,edit)->{var full="data/icheme/materials/"+path;
            var json=JsonParser.parseString(resources.get(full)).getAsJsonObject();edit.accept(json);resources.put(full,json.toString());});
        return MaterialCatalog.parse(resources);
    }
    private MaterialCatalog changed(String path,java.util.function.Consumer<com.google.gson.JsonObject> edit){return changed(java.util.Map.of(path,edit));}
    /** The record's {@code revision} with {@code -edited} appended: the bump a numerical edit declares. */
    private static void bump(com.google.gson.JsonObject record){record.addProperty("revision",record.get("revision").getAsString()+"-edited");}
    /**
     * Data are identified by their declared revisions, nothing is hashed, and each record declares its own (owner decisions
     * 2026-10-26): a name edit keeps the qualified science; a numerical edit holds the world through the bump of the edited
     * record's revision alone, the package record untouched; the same edit left undeclared is not seen.
     */
    @Test void metadataOnlyReloadKeepsQualifiedScienceAndARecordsRevisionBumpHolds() {
        var guard=guard();assertTrue(guard.inspect(original).isEmpty());
        var names=changed("components/crude_pc07.json",j->j.addProperty("fallback","Renamed petroleum"));
        assertTrue(guard.inspect(names).isEmpty());
        java.util.function.Consumer<com.google.gson.JsonObject> heavier=j->j.addProperty("molecular_weight_kg_per_mol",.31);
        assertTrue(guard.inspect(changed("properties/crude_pc07.json",heavier)).isEmpty(),"an undeclared numerical edit: the revision is the identity");
        var science=changed("properties/crude_pc07.json",heavier.andThen(FluidPropertyReloadGuardTest::bump));
        assertEquals(original.requirePackage(packageId).revision(),science.requirePackage(packageId).revision(),"the package record is untouched");
        assertNotEquals(original.fluidRevision(packageId),science.fluidRevision(packageId));
        assertTrue(guard.inspect(science).isPresent());assertTrue(guard.inspect(science).isPresent());
        assertTrue(guard.inspect(MaterialCatalog.parse(original.resources())).isEmpty());
    }
    @Test void aBumpedWaterViscosityInvalidatesThePinnedFluidModel() {
        var resources=new HashMap<>(original.resources());
        var path=resources.keySet().stream().filter(p->p.contains("/water/")).findFirst().orElseThrow();
        var json=JsonParser.parseString(resources.get(path)).getAsJsonObject();
        var liquid=json.getAsJsonObject("viscosity").getAsJsonObject("liquid");var coefficients=liquid.getAsJsonArray("coefficients");
        coefficients.set(0,new com.google.gson.JsonPrimitive(coefficients.get(0).getAsDouble()*1.01));bump(liquid);
        resources.put(path,json.toString());var changed=MaterialCatalog.parse(resources);
        assertNotEquals(original.fluidRevision(packageId),changed.fluidRevision(packageId));
        assertTrue(guard().inspect(changed).isPresent());
    }
    /**
     * A crystal record is science of the package that admits it (owner decision D23: its crystals are solids the vessels
     * hold and exchange with the fluid): a reload that changes the CO2-I Gibbs function under a running pilot world, with
     * the record's revision bumped, holds it, as a property record's change does; a reload of the same data, or of the
     * record's text alone, does not.
     */
    @Test void aChangedCrystalRecordHoldsTheWorld() {
        var pilot=com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog.catalog();String id=com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog.PACKAGE_ID;
        var guard=new FluidPropertyReloadGuard(pilot,id,FluidThermodynamics.forNetwork(pilot,id));
        assertTrue(guard.inspect(MaterialCatalog.parse(com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog.resources())).isEmpty(),"the same data reloaded");
        String record="data/icheme/materials/crystals/carbon_dioxide_i.json";
        assertTrue(guard.inspect(com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog.parseWith(record,o->o.addProperty("source","edited text"))).isEmpty(),"a text edit");
        var edited=com.wormzjl.icheme.science.material.PilotCryogenicTestCatalog.parseWith(record,o->{
            var g=o.getAsJsonObject("gibbs_function").getAsJsonArray("g2_to_g10");g.set(0,new com.google.gson.JsonPrimitive(g.get(0).getAsDouble()*1.001));bump(o);});
        assertEquals(FluidPropertyReloadGuard.HOLD,guard.inspect(edited).orElse("no hold"),"a changed crystal Gibbs function");
    }
}
