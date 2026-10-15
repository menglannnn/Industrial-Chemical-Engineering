package com.wormzjl.createcheme.science.material;

import com.google.gson.JsonParser;
import com.wormzjl.createcheme.science.fluid.state.SolidInventory;
import java.util.*;

/** Separate solid-property identity; adding solids never changes the conserved fluid axis. */
public final class SolidMaterialCatalog {
    private final Map<String, SolidMaterial> materials;
    private SolidMaterialCatalog(Map<String, SolidMaterial> materials) { this.materials = Map.copyOf(materials); }
    public Map<String, SolidMaterial> materials() { return materials; }
    public SolidMaterial require(String id) {
        var material = materials.get(id);
        if (material == null) throw new IllegalArgumentException("Unknown solid material: " + id);
        return material;
    }
    public void validate(SolidInventory inventory) {
        for (var p : inventory.populations())
            if (!require(p.material().id()).equals(p.material())) throw new IllegalArgumentException("Saved solid properties differ from the loaded data (no migration; create a fresh world): " + p.material().id());
    }
    static SolidMaterialCatalog parse(Map<String, String> resources) {
        var solids = new TreeMap<String, SolidMaterial>();
        for (var entry : resources.entrySet()) if (entry.getKey().contains("/materials/solids/")) {
            var o = JsonParser.parseString(entry.getValue()).getAsJsonObject();
            var material = new SolidMaterial(o.get("id").getAsString(), o.get("revision").getAsString(),
                    o.get("density_kg_per_m3").getAsDouble(), o.get("heat_capacity_j_per_kg_kelvin").getAsDouble());
            if (solids.putIfAbsent(material.id(), material) != null) throw new IllegalArgumentException("Duplicate solid material");
        }
        return new SolidMaterialCatalog(solids);
    }
    /** Inert incompressible solid; sensible energy is zero at 298.15 K, with no formation offset. */
    public record SolidMaterial(String id, String revision, double density, double heatCapacity) {
        public static final double REFERENCE_TEMPERATURE = 298.15;
        public SolidMaterial {
            Objects.requireNonNull(id); Objects.requireNonNull(revision);
            if (!id.matches("[a-z][a-z0-9_.-]*:[a-z0-9_./-]+") || id.length()>128 || revision.isBlank() || revision.length()>128
                    || !Double.isFinite(density) || density <= 0 || !Double.isFinite(heatCapacity) || heatCapacity <= 0)
                throw new IllegalArgumentException("Invalid solid material");
        }
        public double specificInternalEnergy(double temperature) {
            if (!Double.isFinite(temperature) || temperature <= 0) throw new IllegalArgumentException("Invalid solid temperature");
            return heatCapacity * (temperature - REFERENCE_TEMPERATURE);
        }
        public double specificEnthalpy(double temperature, double pressure) {
            if (!Double.isFinite(pressure) || pressure <= 0) throw new IllegalArgumentException("Invalid solid pressure");
            return specificInternalEnergy(temperature) + pressure / density;
        }
    }
}