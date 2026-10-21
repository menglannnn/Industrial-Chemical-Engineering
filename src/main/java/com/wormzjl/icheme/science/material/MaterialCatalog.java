package com.wormzjl.icheme.science.material;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static com.wormzjl.icheme.science.material.JsonFields.*;

/** Immutable validated SI database. Resource keys are distinct from plain component IDs. */
public final class MaterialCatalog {
    public record Pr78(double criticalTemperature, double criticalPressure, double acentricFactor) {}
    public record NrtlPair(String first, String second, double a12, double b12, double a21,
            double b21, double alpha, double minimumTemperature, double maximumTemperature) {}
    /**
     * {@code idealGasMinimumTemperature} is the lower end of the {@code ideal_gas_cp} fit where it lies above the
     * component's own range (nitrogen's 273.16 K: below it the fluid network holds the fit's heat capacity, owner decision
     * D37, simplification S4); {@code null} for every other record, which keeps their serialized form, and so every
     * physics fingerprint that hashes it, exactly what it was.
     *
     * <p>{@code cp} is empty for a record without {@code ideal_gas_cp}, which only a package with a reference spine may
     * use (D6: its ideal gas comes from the spine; {@code referenceTemperature} is then 298.15). {@code volumeTranslation}
     * is the record's Tr = 0.8 saturated-liquid anchor ({@link VolumeTranslation}, P3), {@code null} when absent, which
     * again keeps every existing serialized form and fingerprint unchanged.</p>
     */
    public record Property(String id, String component, double molecularWeight, double normalBoilingPoint,
            double density, double standardTemperature, double standardPressure, double minimumTemperature,
            double maximumTemperature, double referenceTemperature, List<Double> cp, Pr78 pr, boolean estimated,
            Double idealGasMinimumTemperature, VolumeTranslation volumeTranslation) {
        public Property { cp = List.copyOf(cp); }
    }
    /**
     * The data range of a component (or, for a package, its envelope) as the fluid network reads it, with the evidence it
     * rests on: a component's is the data range of a coefficient record's ideal-gas fit on the network (a spine has its own),
     * a package's the envelope of its states and the input box of device settings. Leaving either never holds a state
     * (owner rule of 2026-10-16: the models continue and a notice reports it). Read from the {@code fluid_domain} object
     * of a property or package record. It is kept beside the records rather than in them: the records' own ranges are the
     * column's, and they and every other numeric field are hashed into the physics fingerprints the column's neural
     * initializer and the fluid presets pin, which this must not move. The fluid network's thermodynamic revision hashes
     * it instead ({@link #fluidThermoFingerprint}).
     */
    public record Validity(double minimumTemperature, double maximumTemperature, double minimumPressure, double maximumPressure,
            String evidence) {
        public Validity {
            if (!(minimumTemperature > 0) || !(maximumTemperature > minimumTemperature) || !Double.isFinite(maximumTemperature)
                    || !(minimumPressure > 0) || !(maximumPressure > minimumPressure) || !Double.isFinite(maximumPressure))
                throw new IllegalArgumentException("fluid_domain: invalid temperature or pressure range");
            Objects.requireNonNull(evidence);
        }
        List<Double> numbers() { return List.of(minimumTemperature, maximumTemperature, minimumPressure, maximumPressure); }
    }
    /**
     * The free-water model of a package. {@code ice} is its ice below the melting line (WP4 of batch
     * 2026-09-24-coolprop-low-temperature), null for a record without one (a frozen fixture): the fluid network needs it.
     */
    public record Water(String revision, double molarMass, double triplePoint, double criticalTemperature,
            double criticalPressure, double maximumTemperature, double boilingTemperature,
            double vaporizationEnthalpy, double watsonExponent, List<Double> saturation, List<Double> shomate, Ice ice) {
        public Water { saturation = List.copyOf(saturation); shomate = List.copyOf(shomate); }
        /**
         * The water's ice: its crystal record (a usable {@link CrystalReference} of water anchored at the water's triple
         * point) and the coefficients and exponents of its sublimation pressure, IAPWS R14-08 equation 6
         * ({@code ln(p_sub/p_t) = (T_t/T) sum a_i (T/T_t)^b_i}, at the record's triple point).
         */
        public record Ice(String crystal, List<Double> coefficients, List<Double> exponents) {
            public Ice { Objects.requireNonNull(crystal); coefficients = List.copyOf(coefficients); exponents = List.copyOf(exponents); }
        }
    }
    public record Assay(String id, String basis, List<String> components, List<Double> amounts,
            double volumeScale, double amountTotal) {
        public Assay { components = List.copyOf(components); amounts = List.copyOf(amounts); }
    }
    /**
     * {@code spine} lists the reference-spine record of each component in component order, or is empty for a package
     * without one; {@code crystals} lists the crystal records the package admits. {@code spineFingerprint} hashes the
     * numeric content of both ({@link ReferenceSpine}, {@link CrystalReference}); it is empty when the package has
     * neither, and it is kept apart from {@code fingerprint}, which does not see these records, so adding a spine to a
     * package never moves its physics fingerprint.
     *
     * <p>{@code groupContributions} is {@code null} unless the package's interactions record carries a {@code rule}
     * (E-PPR78, P3): it then holds the temperature-dependent pairs resolved from the group matrix and the spines'
     * group counts ({@link GroupContributionInteractions}). For such a pair the constant {@code interactions} entry is
     * 0.0 and is not the pair's kij: a consumer takes kij(T) from {@code groupContributions} for every pair it lists
     * and the constant matrix for the others. Its content is hashed into {@code fingerprint} and
     * {@link #physicsFingerprint} only when it is present, so packages without a rule keep their pins.</p>
     */
    public record Package(String id, String revision, String fingerprint, String model, List<String> components,
            List<Property> properties, List<List<Double>> interactions, List<NrtlPair> nrtlPairs,
            Map<String, String> aliases, Map<String, Assay> assays, Water water,
            double minimumTemperature, double maximumTemperature, double minimumPressure,
            double maximumPressure, List<String> evidence, List<String> spine, List<String> crystals, String spineFingerprint,
            GroupContributionInteractions groupContributions) {
        public Package {
            components = List.copyOf(components); properties = List.copyOf(properties);
            interactions = interactions.stream().map(List::copyOf).toList(); nrtlPairs = List.copyOf(nrtlPairs);
            aliases = Map.copyOf(aliases); assays = Map.copyOf(assays); evidence = List.copyOf(evidence);
            spine = List.copyOf(spine); crystals = List.copyOf(crystals); Objects.requireNonNull(spineFingerprint);
        }
        public String canonicalId(String id) { return aliases.getOrDefault(id, id); }
        public String scientificRevision() { return revision + ":" + fingerprint; }
    }
    private final SolidMaterialCatalog solids;
    public SolidMaterialCatalog solids(){return solids;}
    private final FluidData fluidData;
    private final Map<String, MaterialName> names;
    private final Map<String, Package> packages;
    private final Map<String, String> resources;
    private final Map<String, Map<ViscosityCorrelation.Phase, ViscosityCorrelation>> viscosities;
    private final Map<String, String> waterModels;
    private final Map<String, LiquidMixtureCorrection> liquidMixtures;
    private final MaterialPresets presets;
    private final Map<String, FluidAppearance> assayAppearances;
    private final Map<String, FluidAppearance> componentAppearances;
    /** Fluid-network validity by property record id and by package id; see {@link Validity}. */
    private final Map<String, Validity> propertyValidity, packageValidity;
    /** Reference-spine and crystal records by record id (kinds {@code spine} and {@code crystals}). */
    private final Map<String, ReferenceSpine> spines;
    private final Map<String, CrystalReference> crystals;
    private MaterialCatalog(Map<String, MaterialName> names, Map<String, Package> packages, Map<String, String> resources,
            Map<String, Map<ViscosityCorrelation.Phase, ViscosityCorrelation>> viscosities, Map<String, String> waterModels,
            Map<String, FluidAppearance> assayAppearances, Map<String, FluidAppearance> componentAppearances, Map<String, LiquidMixtureCorrection> liquidMixtures, MaterialPresets presets, FluidData fluidData,
            Map<String, Validity> propertyValidity, Map<String, Validity> packageValidity,
            Map<String, ReferenceSpine> spines, Map<String, CrystalReference> crystals) {
        this.propertyValidity = Map.copyOf(propertyValidity); this.packageValidity = Map.copyOf(packageValidity);
        this.spines = Map.copyOf(spines); this.crystals = Map.copyOf(crystals);
        this.solids=SolidMaterialCatalog.parse(resources);
        // A solid population names its record by id (owner decision D23: crystals are solids), a crystal or a custom solid:
        // the two kinds never share an id.
        for(String id:this.crystals.keySet())if(solids.materials().containsKey(id))throw new IllegalArgumentException("crystals: "+id+" is also a solid material record; a solid id names one record");
        this.fluidData=Objects.requireNonNull(fluidData);
        this.liquidMixtures = Map.copyOf(liquidMixtures);
        this.presets=Objects.requireNonNull(presets);
        this.names = Map.copyOf(names); this.packages = Map.copyOf(packages); this.resources = Map.copyOf(resources);
        this.viscosities = Map.copyOf(viscosities); this.waterModels = Map.copyOf(waterModels);
        this.assayAppearances = Map.copyOf(assayAppearances);
        this.componentAppearances = Map.copyOf(componentAppearances);
    }
    public Package requirePackage(String id) {
        Package p = packages.get(id);
        if (p == null) throw new IllegalArgumentException("Unknown material package: " + id);
        return p;
    }
    public MaterialName name(String id) { MaterialName n=names.get(id); return n==null?MaterialName.chemical(id):n; }
    public Map<String, Package> packages() { return packages; }
    /** Crystal records by id. */
    public Map<String, CrystalReference> crystals() { return crystals; }
    /** The reference spine of a component in a package that declares one. */
    public ReferenceSpine requireSpine(String packageId, String componentId) {
        var p=requirePackage(packageId);
        if(p.spine().isEmpty())throw new IllegalArgumentException("Package declares no reference spine: "+packageId);
        int i=p.components().indexOf(p.canonicalId(componentId));
        if(i<0)throw new IllegalArgumentException("Component outside package: "+componentId);
        return spines.get(p.spine().get(i));
    }
    public FluidData fluidData(){return fluidData;}
    /** A component's Tr = 0.8 volume anchor in a package: its property record's {@code volume_translation}, if any (P3). */
    public Optional<VolumeTranslation> volumeTranslation(String packageId,String componentId) {
        var p=requirePackage(packageId);int i=p.components().indexOf(p.canonicalId(componentId));
        if(i<0)throw new IllegalArgumentException("Component outside package: "+componentId);
        return Optional.ofNullable(p.properties().get(i).volumeTranslation());
    }
    /**
     * The fluid network's thermodynamic identity of a package: its physics fingerprint, the liquid volume references,
     * and the fluid-network validity of the package and of every component ({@link Validity}), so a changed domain is a
     * changed thermodynamic revision (a fresh world, like any other property change).
     */
    public String fluidThermoFingerprint(String packageId) {
        var p=requirePackage(packageId);var points=new TreeMap<String,FluidData.VolumeReference>();
        // The calibration point a component's liquid volume is anchored by, where the network's model reads one: a spine
        // package anchors a component whose property record carries its own volume_translation by it (hashed through the
        // physics fingerprint; SpinePackageData), every other component, and every component of a package without a spine,
        // by the global calibration point (HydrocarbonModel reads no volume_translation): one anchor per species per package.
        for(int i=0;i<p.components().size();i++) {
            String id=p.components().get(i);
            boolean translated=!p.spine().isEmpty()&&p.properties().get(i).volumeTranslation()!=null;
            if(!translated&&fluidData.volumeReferences().containsKey(id))points.put(id,fluidData.volumeReferences().get(id));
        }
        var domains=new ArrayList<Object>();
        domains.add(fluidValidity(packageId).map(Validity::numbers).orElse(List.of()));
        for(String id:p.components())domains.add(fluidValidity(packageId,id).map(Validity::numbers).orElse(List.of()));
        // The free water's ice (WP4), which the physics fingerprint leaves out: the network's numbers below the melting line.
        var ice=p.water().ice();
        List<Object> iceScience=ice==null?List.of():List.of(crystals.get(ice.crystal()).science(),ice.coefficients(),ice.exponents());
        return hash(new Gson().toJson(List.of(physicsFingerprint(packageId,p.components()),points,domains,iceScience)));
    }
    /** The fluid network's envelope for a package: its {@code fluid_domain}, absent for a package the network cannot be built on. */
    public Optional<Validity> fluidValidity(String packageId) {
        requirePackage(packageId);return Optional.ofNullable(packageValidity.get(packageId));
    }
    /** A component's own fluid-network data range in a package: its property record's {@code fluid_domain}, if it declares one. */
    public Optional<Validity> fluidValidity(String packageId,String componentId) {
        var p=requirePackage(packageId);int i=p.components().indexOf(p.canonicalId(componentId));
        if(i<0)throw new IllegalArgumentException("Component outside package: "+componentId);
        return Optional.ofNullable(propertyValidity.get(p.properties().get(i).id()));
    }
    public MaterialPresets presets(){return presets;}
    public LiquidMixtureCorrection liquidMixture(String packageId) { requirePackage(packageId); return liquidMixtures.get(packageId); }
    /** Immutable source strings for tooling and isolated override tests. */
    public Map<String, String> resources() { return resources; }

    /** Liquid visual settings for a component, including package aliases and the water model. */
    public FluidAppearance componentAppearance(String packageId, String componentId) {
        Package p = requirePackage(packageId);
        String id = p.canonicalId(Objects.requireNonNull(componentId, "componentId"));
        if (!p.components().contains(id) && !id.equals("Water"))
            throw new IllegalArgumentException("Component is outside package: " + componentId);
        return Objects.requireNonNull(componentAppearances.get(id), "No appearance for component " + id);
    }

    /** Whole-feed liquid visual settings; no component-color mixing rule is implied. */
    public FluidAppearance assayAppearance(String packageId, String assayId) {
        if (!requirePackage(packageId).assays().containsKey(assayId))
            throw new IllegalArgumentException("Assay is outside package: " + assayId);
        // Assay identity is (package, id) everywhere else in this class, so the appearance is keyed
        // that way too: one composition may be declared on two packages with different bases.
        return Objects.requireNonNull(assayAppearances.get(packageId + "/" + assayId), "No appearance for assay " + assayId);
    }

    /** Selected pure-component viscosity data, absent when that phase has no dataset. No mixing rule is implied. */
    public Optional<ViscosityCorrelation> viscosity(String packageId, String componentId, ViscosityCorrelation.Phase phase) {
        Objects.requireNonNull(phase, "phase");
        Package p = requirePackage(packageId);
        String id = p.canonicalId(Objects.requireNonNull(componentId, "componentId"));
        int index = p.components().indexOf(id);
        String key;
        if (index >= 0) key = "properties/" + p.properties().get(index).id();
        else if (id.equals("Water")) key = "water/" + waterModels.get(packageId);
        else throw new IllegalArgumentException("Component is outside package: " + componentId);
        return Optional.ofNullable(viscosities.getOrDefault(key, Map.of()).get(phase));
    }

    private static JsonObject propertyScience(Property property) {
        var json=new Gson().toJsonTree(property).getAsJsonObject();json.remove("id");return json;
    }
    /** Ordered numerical sub-basis, excluding assays, names, transport, record IDs and declared revisions. */
    public String physicsFingerprint(String packageId,List<String> axis) {
        var p=requirePackage(packageId);new MaterialAxis(axis);
        var indices=axis.stream().map(id->{int i=p.components().indexOf(id);if(i<0)throw new IllegalArgumentException("Component outside package: "+id);return i;}).toList();
        var properties=indices.stream().map(i->propertyScience(p.properties().get(i))).toList();
        var matrix=indices.stream().map(i->indices.stream().map(j->p.interactions().get(i).get(j)).toList()).toList();
        var nrtl=p.nrtlPairs().stream().filter(pair->axis.contains(pair.first())&&axis.contains(pair.second()))
                .sorted(Comparator.comparing(NrtlPair::first).thenComparing(NrtlPair::second)).toList();
        // The ice (WP4) is left out with the revision: the column, whose neural sidecars pin this, never reaches the water's
        // melting line; the fluid network's revision hashes it (fluidThermoFingerprint).
        var water=new Gson().toJsonTree(p.water()).getAsJsonObject();water.remove("revision");water.remove("ice");
        var hashed=new ArrayList<Object>(List.of(p.model(),axis,properties,matrix,nrtl,water,
                p.minimumTemperature(),p.maximumTemperature(),p.minimumPressure(),p.maximumPressure()));
        // Only a package with a group-contribution rule hashes it, so every other package keeps its exact pin.
        if(p.groupContributions()!=null)hashed.add(p.groupContributions().science(p.components(),axis));
        return hash(new Gson().toJson(hashed));
    }

    /**
     * Immutable PR view for a qualified inference axis. Missing zero species can use an explicitly
     * selected reference package; the caller must verify the complete projected physics fingerprint.
     * This does not convert saved inventories or publish a replacement runtime catalog.
     */
    public MaterialCatalog inferenceView(String packageId,List<String> axis,String referencePackage,boolean allowMissing) {
        var source=requirePackage(packageId);new MaterialAxis(axis);
        if(source.components().equals(axis))return this;
        // A projection is a PR78 axis without crystal records (P6): a package that admits crystals is refused, never
        // projected fluid-only.
        if(!source.crystals().isEmpty())throw new CrystalPhasesUnavailable(packageId,source.crystals(),"a neural inference projection");
        if(!source.model().equals("pr78"))throw new IllegalArgumentException("Inference projection requires PR78");
        if(source.groupContributions()!=null)throw new IllegalArgumentException("Inference projection of a package with temperature-dependent interactions is not supported: "+packageId);
        var reference=requirePackage(referencePackage);
        var properties=new ArrayList<Property>();
        for(String id:axis) {
            int i=source.components().indexOf(id);
            if(i>=0)properties.add(source.properties().get(i));
            else {
                int j=reference.components().indexOf(id);
                if(!allowMissing||j<0)throw new IllegalArgumentException("Unqualified missing inference species: "+id);
                properties.add(reference.properties().get(j));
            }
        }
        var interactions=new ArrayList<List<Double>>();
        for(String a:axis) {
            var row=new ArrayList<Double>();
            for(String b:axis) {
                int i=source.components().indexOf(a),j=source.components().indexOf(b);
                if(i>=0&&j>=0)row.add(source.interactions().get(i).get(j));
                else {
                    int ri=reference.components().indexOf(a),rj=reference.components().indexOf(b);
                    if(ri<0||rj<0)throw new IllegalArgumentException("Missing qualified inference interaction: "+a+"/"+b);
                    row.add(reference.interactions().get(ri).get(rj));
                }
            }
            interactions.add(row);
        }
        // A projected inference view is a PR78 axis only: it carries no reference spine or crystals.
        var view=new Package(source.id(),source.revision(),source.fingerprint(),source.model(),axis,properties,
                interactions,List.of(),Map.of(),Map.of(),source.water(),source.minimumTemperature(),
                source.maximumTemperature(),source.minimumPressure(),source.maximumPressure(),source.evidence(),List.of(),List.of(),"",null);
        var projected=new HashMap<>(packages);projected.put(packageId,view);
        return new MaterialCatalog(names,projected,resources,viscosities,waterModels,assayAppearances,
                componentAppearances,liquidMixtures,presets,fluidData,propertyValidity,packageValidity,spines,crystals);
    }

    /** Presets may share a network only when the selected sub-basis has the same physics and transport. */
    public void requireSharedFluidPhysics(String sourceId,String targetId) {
        var source=requirePackage(sourceId);var target=requirePackage(targetId);var axis=source.components();
        if(!target.components().containsAll(axis)||!physicsFingerprint(sourceId,axis).equals(physicsFingerprint(targetId,axis)))
            throw new IllegalArgumentException("Fluid preset physics differ from network: "+sourceId);
        var a=liquidMixture(sourceId);var b=liquidMixture(targetId);
        if(!a.coefficients().equals(b.coefficients())||a.referenceKelvin()!=b.referenceKelvin()||a.slopeCapKelvin()!=b.slopeCapKelvin())
            throw new IllegalArgumentException("Fluid preset mixture correction differs from network: "+sourceId);
        for(int i=0;i<axis.size();i++) {
            int j=target.components().indexOf(axis.get(i));
            if(!a.descriptors().get(i).equals(b.descriptors().get(j))
                    ||!viscosityScience("properties/"+source.properties().get(i).id()).equals(viscosityScience("properties/"+target.properties().get(j).id())))
                throw new IllegalArgumentException("Fluid preset transport differs from network: "+sourceId+"/"+axis.get(i));
            if(!Objects.equals(fluidValidity(sourceId,axis.get(i)).map(Validity::numbers),fluidValidity(targetId,axis.get(i)).map(Validity::numbers)))
                throw new IllegalArgumentException("Fluid preset fluid_domain differs from network: "+sourceId+"/"+axis.get(i));
        }
        if(!viscosityScience("water/"+waterModels.get(sourceId)).equals(viscosityScience("water/"+waterModels.get(targetId))))
            throw new IllegalArgumentException("Fluid preset water transport differs from network: "+sourceId);
    }

    /** Transport cache key; viscosity changes do not invalidate PR/enthalpy seeds which never consume viscosity. */
    public String viscosityFingerprint(String packageId) {
        Package p = requirePackage(packageId);
        Map<String, Object> selected = new TreeMap<>();
        for (Property property : p.properties()) selected.put(property.id(), viscosityScience("properties/" + property.id()));
        selected.put("liquid_mixture", liquidMixture(packageId));
        selected.put("molecular_weights",p.properties().stream().map(property->List.of(property.component(),property.molecularWeight())).toList());
        selected.put("water_molecular_weight",p.water().molarMass());
        var conditional=new TreeMap<String,Object>();
        for(String id:p.components()) {
            var curve=fluidData.conditionalLiquidViscosities().get(id);
            if(curve!=null)conditional.put(id,List.of(curve.minimumTemperatureKelvin(),curve.maximumTemperatureKelvin(),
                    curve.minimumPressurePascal(),curve.temperaturesKelvin(),curve.coefficients()));
        }
        selected.put("conditional_solutes",conditional);
        selected.put("water/" + waterModels.get(packageId), viscosityScience("water/" + waterModels.get(packageId)));
        return hash(new Gson().toJson(selected));
    }

    private Map<String, Object> viscosityScience(String key) {
        Map<String, Object> data = new TreeMap<>();
        viscosities.getOrDefault(key, Map.of()).forEach((phase, v) -> data.put(phase.name(), List.of(v.model(),
                v.minimumTemperatureKelvin(), v.maximumTemperatureKelvin(), v.minimumPressurePascal(), v.maximumPressurePascal(),
                v.referenceTemperatureKelvin(), v.coefficients(), v.exponents(), v.temperaturesKelvin(), v.estimated())));
        return data;
    }

    /** Parse a complete winning resource set; never publishes partially validated data. */
    public static MaterialCatalog parse(Map<String, String> resources) {
        var groups = new HashMap<String, Map<String, JsonObject>>();
        var origins = new IdentityHashMap<JsonObject, String>();
        for (var entry : new TreeMap<>(resources).entrySet()) {
            try {
                JsonObject o = JsonParser.parseString(entry.getValue()).getAsJsonObject();
                if (number(o,"schema_version") != 1) throw new IllegalArgumentException("schema_version must be 1");
                String path = entry.getKey(); int start = path.indexOf("materials/");
                if (start < 0) throw new IllegalArgumentException("Expected materials resource path");
                String kind = path.substring(start + 10).split("/")[0];
                if (!Set.of("components","properties","interactions","packages","assays","water","bases","transport","presets","networks","solids","spine","crystals","group_interactions").contains(kind))
                    throw new IllegalArgumentException("Unknown record kind: " + kind);
                String id = text(o, "id");
                if (!kind.equals("components") && !id.matches("[a-z][a-z0-9_.:-]{0,127}"))
                    throw new IllegalArgumentException("id: expected lowercase stable identifier");
                if (!id.matches("[A-Za-z][A-Za-z0-9_.:-]{0,127}")) throw new IllegalArgumentException("Invalid id");
                String key = kind.equals("assays") ? text(o,"package") + "/" + id : id;
                if (groups.computeIfAbsent(kind, unused -> new LinkedHashMap<>()).putIfAbsent(key,o) != null)
                    throw new IllegalArgumentException("Duplicate " + kind + " id: " + key);
                origins.put(o,path);
            } catch (RuntimeException e) { throw error(entry.getKey(), e); }
        }
        Authoring.expand(groups,origins);
        var names = new HashMap<String, MaterialName>();
        var properties = new HashMap<String, Property>();
        var waters = new HashMap<String, Water>();
        var packages = new HashMap<String, Package>();
        var viscosities = new HashMap<String, Map<ViscosityCorrelation.Phase, ViscosityCorrelation>>();
        var waterModels = new HashMap<String, String>();
        var liquidMixtures = new HashMap<String, LiquidMixtureCorrection>();
        var descriptors = new HashMap<String, LiquidMixtureCorrection.Descriptor>();
        var assayAppearances = new HashMap<String, FluidAppearance>();
        var componentAppearances = new HashMap<String, FluidAppearance>();
        var propertyValidity = new HashMap<String, Validity>();
        var packageValidity = new HashMap<String, Validity>();
        var spines = new HashMap<String, ReferenceSpine>();
        var crystals = new HashMap<String, CrystalReference>();
        for (var o : group(groups,"components").values()) checked(origins,o,() -> {
            JsonObject cut = o.has("cut") ? object(o,"cut") : null;
            String id=text(o,"id"), kind=text(o,"kind");
            componentAppearances.put(id, readAppearance(o));
            if (kind.equals("petroleum_fraction")) {
                if (cut == null || !cut.has("lower_kelvin") && !cut.has("upper_kelvin")) throw new IllegalArgumentException("cut boundaries required");
                text(cut,"derivation"); text(cut,"series");
            }
            names.put(id,new MaterialName(id,text(o,"translation_key"),text(o,"fallback"),kind,
                    cut != null && cut.has("lower_kelvin") ? number(cut,"lower_kelvin") : null,
                    cut != null && cut.has("upper_kelvin") ? number(cut,"upper_kelvin") : null,
                    cut != null && bool(cut,"estimated")));
        });
        for (var o : group(groups,"spine").values()) checked(origins,o,() -> {
            var spine=ReferenceSpine.read(o); require(names,spine.component(),"component"); spines.put(spine.id(),spine);
        });
        for (var o : group(groups,"crystals").values()) checked(origins,o,() -> {
            var crystal=CrystalReference.read(o); require(names,crystal.component(),"component"); crystals.put(crystal.id(),crystal);
        });
        var matrices = new HashMap<String, GroupInteractionMatrix>();
        for (var o : group(groups,"group_interactions").values()) checked(origins,o,() -> {
            var matrix=GroupInteractionMatrix.read(o); matrices.put(matrix.id(),matrix);
        });
        for (var o : group(groups,"properties").values()) checked(origins,o,() -> {
            String id=text(o,"id"), component=text(o,"component"); require(names,component,"component");
            viscosities.put("properties/" + id, readViscosities(o));
            descriptors.put(id,LiquidMixtureCorrection.Descriptor.read(o));
            text(o,"revision"); text(o,"source");
            // ideal_gas_cp is optional: a record without it is usable only in a package that declares a reference
            // spine (D6, one ideal-gas source per species; checked per package below). Its cp is then empty.
            JsonObject cp=o.has("ideal_gas_cp")?object(o,"ideal_gas_cp"):null;
            List<Double> coefficients=List.of(); double reference=298.15;
            if (cp!=null) {
                if (!text(cp,"type").equals("shifted_polynomial_5")) throw new IllegalArgumentException("Unsupported ideal_gas_cp.type");
                coefficients=counted(cp,"coefficients",6); reference=positive(cp,"reference_kelvin");
                if (reference != 298.15) throw new IllegalArgumentException("shifted_polynomial_5.reference_kelvin must be 298.15");
            }
            JsonObject models=object(o,"models"); Pr78 pr=null;
            for (String model : models.keySet()) if (!model.equals("pr78")) throw new IllegalArgumentException("Unsupported pure-component model: " + model);
            if (models.has("pr78")) {
                JsonObject p=object(models,"pr78");
                pr=new Pr78(positive(p,"critical_temperature_kelvin"),positive(p,"critical_pressure_pascal"),number(p,"acentric_factor"));
            }
            double min=positive(o,"temperature_min_kelvin"), max=positive(o,"temperature_max_kelvin"); range(min,max,"temperature");
            Double floor=null;
            if (cp!=null && cp.has("temperature_min_kelvin")) {
                floor=positive(cp,"temperature_min_kelvin");
                if (floor<=min || floor>reference) throw new IllegalArgumentException("ideal_gas_cp.temperature_min_kelvin must lie in (temperature_min_kelvin, 298.15]");
            }
            VolumeTranslation translation=o.has("volume_translation")?VolumeTranslation.read(o,pr):null;
            properties.put(id,new Property(id,component,positive(o,"molecular_weight_kg_per_mol"),positive(o,"normal_boiling_point_kelvin"),
                    positive(o,"standard_liquid_density_kg_per_m3"),positive(o,"standard_temperature_kelvin"),positive(o,"standard_pressure_pascal"),
                    min,max,reference,coefficients,pr,bool(o,"estimated_heavy_residue"),floor,translation));
            if (o.has("fluid_domain")) propertyValidity.put(id,validity(object(o,"fluid_domain")));
        });
        for (var o : group(groups,"water").values()) checked(origins,o,() -> {
            viscosities.put("water/" + text(o,"id"), readViscosities(o));
            if (!text(o,"type").equals("iapws_shomate_watson")) throw new IllegalArgumentException("Unsupported water.type");
            text(o,"source");
            Water.Ice ice=null;
            if(o.has("ice")) {
                // The water's ice (WP4): a usable crystal record of water anchored at the water's own triple point, and its
                // sublimation pressure, IAPWS R14-08 equation 6 at that point.
                JsonObject i=object(o,"ice"),sublimation=object(i,"sublimation_pressure");
                var crystal=require(crystals,text(i,"crystal"),"ice.crystal");
                if(!crystal.component().equals("Water"))throw new IllegalArgumentException("ice.crystal: "+crystal.id()+" is made of "+crystal.component()+", not Water");
                if(!crystal.usable())throw new IllegalArgumentException("ice.crystal: "+crystal.id()+" is not usable, missing "+crystal.missing());
                if(crystal.anchor().temperature()!=positive(o,"triple_point"))
                    throw new IllegalArgumentException("ice.crystal: "+crystal.id()+" is anchored at "+crystal.anchor().temperature()+" K, not the water's triple point");
                if(!text(sublimation,"type").equals("iapws_r14_08"))throw new IllegalArgumentException("Unsupported ice.sublimation_pressure.type");
                text(sublimation,"source");
                ice=new Water.Ice(crystal.id(),counted(sublimation,"coefficients",3),counted(sublimation,"exponents",3));
            }
            Water w=new Water(text(o,"revision"),positive(o,"molar_mass"),positive(o,"triple_point"),positive(o,"critical_temperature"),
                    positive(o,"critical_pressure"),positive(o,"max_enthalpy_temperature"),positive(o,"reference_boiling_temperature"),
                    positive(o,"reference_vaporization_enthalpy"),positive(o,"watson_exponent"),counted(o,"saturation_coefficients",6),counted(o,"shomate_coefficients",7),ice);
            range(w.triplePoint(),w.boilingTemperature(),"water boiling"); range(w.boilingTemperature(),w.criticalTemperature(),"water critical");
            range(w.triplePoint(),w.maximumTemperature(),"water enthalpy"); require(names,"Water","component"); waters.put(text(o,"id"),w);
        });
        for (var o : group(groups,"interactions").values()) checked(origins,o,() -> validateInteractions(o,names,matrices));
        for (var o : group(groups,"packages").values()) checked(origins,o,() -> {
            String id=text(o,"id"), model=text(o,"model"), policy=text(o,"missing_interactions");
            if(text(o,"revision").length()>63)throw new IllegalArgumentException("revision: maximum 63 characters with fingerprint");
            var evidence=strings(o,"advisory_evidence");
            if(evidence.size()>32 || evidence.stream().anyMatch(e->e.length()>256))throw new IllegalArgumentException("advisory_evidence: exceeds reporting bounds");
            if (!Set.of("pr78","nrtl").contains(model)) throw new IllegalArgumentException("Unsupported model: " + model);
            if (!Set.of("zero","error").contains(policy) || model.equals("nrtl") && !policy.equals("error"))
                throw new IllegalArgumentException("Invalid missing_interactions policy");
            List<String> ids=strings(o,"components"), refs=strings(o,"properties");
            if (ids.isEmpty() || ids.size()>64 || new HashSet<>(ids).size()!=ids.size() || ids.size()!=refs.size())
                throw new IllegalArgumentException("Invalid ordered components/properties");
            var ps=new ArrayList<Property>();
            double tmin=positive(o,"temperature_min_kelvin"),tmax=positive(o,"temperature_max_kelvin"); range(tmin,tmax,"temperature");
            double pmin=positive(o,"pressure_min_pascal"),pmax=positive(o,"pressure_max_pascal"); range(pmin,pmax,"pressure");
            for(int i=0;i<ids.size();i++) {
                require(names,ids.get(i),"component"); Property p=require(properties,refs.get(i),"property");
                if(!p.component().equals(ids.get(i)) || model.equals("pr78") && p.pr()==null) throw new IllegalArgumentException("Property/model does not match component " + ids.get(i));
                if(tmin<p.minimumTemperature() || tmax>p.maximumTemperature()) throw new IllegalArgumentException("Package temperature outside property validity: " + p.id());
                // D6, one ideal-gas source per species: a package with a reference spine takes every component's ideal
                // gas from it, a package without one from the property's shifted_polynomial_5 fit.
                if(p.cp().isEmpty() && !o.has("spine"))
                    throw new IllegalArgumentException("Property "+p.id()+" has no ideal_gas_cp: a package without a reference spine needs a shifted_polynomial_5 fit for "+ids.get(i));
                if(!p.cp().isEmpty() && o.has("spine"))
                    throw new IllegalArgumentException("Property "+p.id()+" carries ideal_gas_cp in a package with a reference spine: one ideal-gas source per species (D6), "+ids.get(i));
                ps.add(p);
            }
            List<String> spineIds=o.has("spine")?strings(o,"spine"):List.of(), crystalIds=o.has("crystals")?strings(o,"crystals"):List.of();
            var spineRecords=new ArrayList<ReferenceSpine>();var spineScience=new ArrayList<Object>();
            if(o.has("spine")) {
                if(spineIds.size()!=ids.size())throw new IllegalArgumentException("spine: expected one reference-spine record per component ("+ids.size()+"), got "+spineIds.size());
                for(int i=0;i<ids.size();i++) {
                    var spine=require(spines,spineIds.get(i),"spine");
                    if(!spine.component().equals(ids.get(i)))throw new IllegalArgumentException("spine: "+spine.id()+" is for "+spine.component()+", not component "+ids.get(i));
                    double difference=Math.abs(spine.molarMass()/ps.get(i).molecularWeight()-1);
                    if(difference>1e-4)throw new IllegalArgumentException("spine: molar mass of "+spine.id()+" ("+spine.molarMass()+" kg/mol) differs from property "
                            +ps.get(i).id()+" ("+ps.get(i).molecularWeight()+" kg/mol) by "+difference+" relative, above 1e-4");
                    spineRecords.add(spine);spineScience.add(spine.science());
                }
            }
            liquidMixtures.put(id,LiquidMixtureCorrection.read(o,refs.stream().map(descriptors::get).toList()));
            JsonObject interaction=require(group(groups,"interactions"),text(o,"interactions"),"interactions");
            if(!model.equals(text(interaction,"model"))) throw new IllegalArgumentException("Interaction model mismatch");
            double[][] matrix=new double[ids.size()][ids.size()]; var nrtl=new ArrayList<NrtlPair>(); var seen=new HashSet<String>();
            boolean[][] constant=new boolean[ids.size()][ids.size()];
            for(var value:array(interaction,"pairs")) {
                JsonObject pair=value.getAsJsonObject(); String a=text(pair,"first"), b=text(pair,"second");
                int i=ids.indexOf(a),j=ids.indexOf(b); if(i<0 || j<0) throw new IllegalArgumentException("Interaction component outside package");
                seen.add(pairKey(a,b));constant[i][j]=constant[j][i]=true;
                if(model.equals("pr78")) matrix[i][j]=matrix[j][i]=number(pair,"kij");
                else {
                    NrtlPair n=nrtl(pair); if(tmin<n.minimumTemperature() || tmax>n.maximumTemperature()) throw new IllegalArgumentException("NRTL temperature validity"); nrtl.add(n);
                }
            }
            GroupContributionInteractions groupContributions=null;
            if(interaction.has("rule")) {
                // Every pair without an explicit constant is resolved by the group-contribution rule from the spines'
                // group counts; its constant matrix entry stays 0.0 and is not its kij (see Package).
                JsonObject rule=object(interaction,"rule");
                if(spineRecords.isEmpty())throw new IllegalArgumentException("interactions rule "+text(rule,"type")+": the package declares no spine, whose groups decompose each component");
                groupContributions=GroupContributionInteractions.resolve(text(rule,"type"),require(matrices,text(rule,"group_interactions"),"rule.group_interactions"),
                        ids,spineRecords,constant);
                for(var resolved:groupContributions.pairs())seen.add(pairKey(ids.get(resolved.first()),ids.get(resolved.second())));
            }
            if(policy.equals("error")) for(int i=0;i<ids.size();i++) for(int j=i+1;j<ids.size();j++)
                if(!seen.contains(pairKey(ids.get(i),ids.get(j)))) throw new IllegalArgumentException("Missing interaction: " + ids.get(i)+" / "+ids.get(j));
            Map<String,String> aliases=new HashMap<>();
            for(var e:object(o,"aliases").entrySet()) {
                String target=e.getValue().getAsString();
                if(!e.getKey().matches("[A-Za-z][A-Za-z0-9_.:-]{0,63}") || !ids.contains(target) || ids.contains(e.getKey()) && !e.getKey().equals(target))
                    throw new IllegalArgumentException("Ambiguous/invalid alias: "+e.getKey());
                aliases.put(e.getKey(),target);
            }
            Water water=require(waters,text(o,"water_model"),"water_model");
            waterModels.put(id, text(o,"water_model"));
            if(o.has("fluid_domain")) {
                // The package's envelope: the data range of the package as a whole and the input box of device settings
                // (science.fluid.thermo.FluidDomain). Leaving it, or a component's range, never holds a state (owner rule of
                // 2026-10-16): no range has to lie inside another.
                packageValidity.put(id,validity(object(o,"fluid_domain")));
            }
            var assays=new HashMap<String,Assay>();
            for(var a:group(groups,"assays").values()) if(text(a,"package").equals(id)) checked(origins,a,() -> {
                assayAppearances.put(id + "/" + text(a,"id"), readAppearance(a));
                List<String> axis; List<Double> amounts;
                if(a.has("amounts_by_component")) {
                    if(a.has("components")||a.has("amounts"))throw new IllegalArgumentException("amounts_by_component: cannot combine sparse and positional forms");
                    var sparse=object(a,"amounts_by_component");
                    for(String key:sparse.keySet())if(!ids.contains(key))throw new IllegalArgumentException("amounts_by_component: unknown component "+key);
                    axis=ids;var ordered=new ArrayList<Double>();
                    for(String component:ids)ordered.add(sparse.has(component)?number(sparse,component):0.0);
                    amounts=List.copyOf(ordered);
                } else {axis=strings(a,"components");amounts=counted(a,"amounts",ids.size());}
                double total=amounts.stream().mapToDouble(Double::doubleValue).sum();
                if(!axis.equals(ids) || amounts.stream().anyMatch(v->v<0) || total<=0 || !Double.isFinite(total)) throw new IllegalArgumentException("Invalid assay components/amounts");
                String basis=text(a,"basis"); if(!Set.of("mole","mass","standard_liquid_volume").contains(basis)) throw new IllegalArgumentException("Unknown assay basis");
                if(basis.equals("standard_liquid_volume")) for(Property p:ps)
                    if(number(a,"standard_temperature_kelvin")!=p.standardTemperature() || number(a,"standard_pressure_pascal")!=p.standardPressure())
                        throw new IllegalArgumentException("Assay density reference conditions differ");
                assays.put(text(a,"id"),new Assay(text(a,"id"),basis,axis,amounts,a.has("volume_scale")?positive(a,"volume_scale"):1,
                        a.has("amount_total")?positive(a,"amount_total"):1));
            });
            var rows=new ArrayList<List<Double>>(); for(double[] row:matrix) rows.add(Arrays.stream(row).boxed().toList());
            var hashed=new ArrayList<Object>(List.of(model,ids,ps,rows,nrtl,new TreeMap<>(assays),water,tmin,tmax,pmin,pmax));
            // Only a package with a group-contribution rule hashes it, so every other package keeps its exact pin.
            if(groupContributions!=null)hashed.add(groupContributions.science(ids,ids));
            String fingerprint=hash(new Gson().toJson(hashed));
            var crystalScience=new ArrayList<Object>();
            if(new HashSet<>(crystalIds).size()!=crystalIds.size())throw new IllegalArgumentException("crystals: duplicate crystal record");
            // A crystal is anchored at its triple point to the spine family's liquid and moved to a consumer's datum by the
            // spine's formation enthalpies (P2, D10): a package without a spine cannot carry one, and a consumer would run it
            // fluid-only where a crystal could form (P6).
            if(!crystalIds.isEmpty()&&spineIds.isEmpty())throw new IllegalArgumentException("crystals: package "+id+" declares crystals without a reference spine, which anchors them");
            for(String crystalId:crystalIds) {
                var crystal=require(crystals,crystalId,"crystals");
                if(!ids.contains(crystal.component()))throw new IllegalArgumentException("crystals: "+crystalId+" is for "+crystal.component()+", outside the package components");
                crystalScience.add(crystal.science());
            }
            String spineFingerprint=spineIds.isEmpty()&&crystalIds.isEmpty()?"":hash(new Gson().toJson(List.of(spineScience,crystalScience)));
            packages.put(id,new Package(id,text(o,"revision"),fingerprint,model,ids,ps,rows,nrtl,aliases,assays,water,tmin,tmax,pmin,pmax,strings(o,"advisory_evidence"),
                    spineIds,crystalIds,spineFingerprint,groupContributions));
        });
        for(var a:group(groups,"assays").values()) checked(origins,a,() -> require(packages,text(a,"package"),"assay package"));
        if(packages.isEmpty()) throw new IllegalArgumentException("Material catalog has no packages");
        var result=new MaterialCatalog(names,packages,resources,viscosities,waterModels,assayAppearances,componentAppearances,liquidMixtures,MaterialPresets.read(group(groups,"presets"),group(groups,"networks"),origins,packages,names.keySet()),FluidData.read(group(groups,"transport"),origins,names.keySet()),
                propertyValidity,packageValidity,spines,crystals);
        for(var row:group(groups,"presets").values())if(text(row,"kind").equals("column"))checked(origins,row,()->
                MaterialRuntime.with(result,text(row,"package"),()->{com.wormzjl.icheme.science.column.v3.V3ColumnProblemResolver.validateInput(result.presets().column(text(row,"id")).input(result));return null;}));
        for(var row:group(groups,"networks").values())checked(origins,row,()->{
            var config=result.presets().network();var network=result.requirePackage(config.packageId());
            for(String id:config.fluidPresets()) {
                var preset=result.presets().fluid(id);
                if(!preset.component().isEmpty()) {
                    if(!preset.component().equals("Water")&&!network.components().contains(preset.component()))throw new IllegalArgumentException("fluid_presets: component outside network "+preset.component());
                } else result.requireSharedFluidPhysics(preset.packageId(),network.id());
            }
            for(var phase:ViscosityCorrelation.Phase.values())if(result.viscosity(network.id(),"Nitrogen",phase).isEmpty())throw new IllegalArgumentException("package: nitrogen transport is required");
            if(result.fluidValidity(network.id()).isEmpty())throw new IllegalArgumentException("package: the network package needs a fluid_domain envelope");
        });
        // The fluid network freezes free water below its melting line on every package (WP4), so the network package's
        // water model needs its ice: refused here, with the water record's file and the field, so a /reload of such data is
        // refused before publication (owner decision of 2026-10-19) rather than holding a running network at its model build.
        if(!group(groups,"networks").isEmpty()) {
            var network=result.requirePackage(result.presets().network().packageId());
            if(network.water().ice()==null)throw new IllegalArgumentException(origins.get(group(groups,"water").get(result.waterModels.get(network.id())))
                    +": ice: required, the fluid network's package "+network.id()+" freezes its free water as this record's ice");
        }
        return result;
    }

    /** Every component and assay record carries its appearance; there is no default tint. */
    private static FluidAppearance readAppearance(JsonObject record) {
        try {
            JsonObject o = object(record, "appearance");
            String color = text(o, "color");
            if (!color.matches("#[0-9a-fA-F]{6}"))
                throw new IllegalArgumentException("color must be #RRGGBB (alpha is specified by transparency)");
            return new FluidAppearance(Integer.parseInt(color.substring(1), 16), number(o, "transparency"), bool(o, "estimated"));
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("appearance: " + invalid.getMessage(), invalid);
        }
    }

    private static Map<ViscosityCorrelation.Phase, ViscosityCorrelation> readViscosities(JsonObject record) {
        if (!record.has("viscosity")) return Map.of();
        var result = new EnumMap<ViscosityCorrelation.Phase, ViscosityCorrelation>(ViscosityCorrelation.Phase.class);
        for (var entry : object(record, "viscosity").entrySet()) {
            try {
                var phase = switch (entry.getKey()) {
                    case "liquid" -> ViscosityCorrelation.Phase.LIQUID;
                    case "vapor" -> ViscosityCorrelation.Phase.VAPOR;
                    default -> throw new IllegalArgumentException("unknown phase");
                };
                JsonObject o = entry.getValue().getAsJsonObject();
                var model = switch (text(o,"type")) {
                    case "andrade" -> ViscosityCorrelation.Model.ANDRADE;
                    case "sutherland" -> ViscosityCorrelation.Model.SUTHERLAND;
                    case "power_sum" -> ViscosityCorrelation.Model.POWER_SUM;
                    case "log_table" -> ViscosityCorrelation.Model.LOG_TABLE;
                    default -> throw new IllegalArgumentException("unsupported type");
                };
                if (model == ViscosityCorrelation.Model.ANDRADE && phase != ViscosityCorrelation.Phase.LIQUID
                        || model == ViscosityCorrelation.Model.SUTHERLAND && phase != ViscosityCorrelation.Phase.VAPOR)
                    throw new IllegalArgumentException("correlation type does not match phase");
                int count = model == ViscosityCorrelation.Model.POWER_SUM || model == ViscosityCorrelation.Model.LOG_TABLE
                        ? array(o,"coefficients").size() : 2;
                result.put(phase, new ViscosityCorrelation(model, positive(o,"temperature_min_kelvin"), positive(o,"temperature_max_kelvin"),
                        positive(o,"pressure_min_pascal"), positive(o,"pressure_max_pascal"), positive(o,"reference_temperature_kelvin"),
                        counted(o,"coefficients",count), o.has("exponents") ? counted(o,"exponents",count) : List.of(),
                        text(o,"revision"), text(o,"source"), bool(o,"estimated"),
                        o.has("temperatures_kelvin") ? counted(o,"temperatures_kelvin",count) : List.of()));
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("viscosity." + entry.getKey() + ": " + error.getMessage(), error);
            }
        }
        return Map.copyOf(result);
    }
    private static void validateInteractions(JsonObject o,Map<String,MaterialName> names,Map<String,GroupInteractionMatrix> matrices) {
        String model=text(o,"model"); text(o,"source");
        if(!Set.of("pr78","nrtl").contains(model)) throw new IllegalArgumentException("Unknown interaction model");
        if(o.has("rule")) {
            // A group-contribution rule resolves, per package, every pair without an explicit constant (P3 E-PPR78).
            if(!model.equals("pr78"))throw new IllegalArgumentException("rule: a group-contribution kij rule needs model pr78");
            JsonObject rule=object(o,"rule");String type=text(rule,"type");
            if(!type.equals(GroupContributionInteractions.RULE_EPPR78))throw new IllegalArgumentException("rule.type: expected "+GroupContributionInteractions.RULE_EPPR78+", got "+type);
            require(matrices,text(rule,"group_interactions"),"rule.group_interactions");
        }
        var seen=new HashSet<String>();
        for(var value:array(o,"pairs")) {
            JsonObject p=value.getAsJsonObject(); String a=text(p,"first"),b=text(p,"second"); require(names,a,"first"); require(names,b,"second");
            if(a.equals(b) || !seen.add(pairKey(a,b))) throw new IllegalArgumentException("Duplicate/self interaction pair");
            if(model.equals("pr78")) number(p,"kij"); else nrtl(p);
        }
    }
    private static Validity validity(JsonObject o) {
        try {
            String evidence=text(o,"evidence");
            double tmin=positive(o,"temperature_min_kelvin"),tmax=positive(o,"temperature_max_kelvin");range(tmin,tmax,"temperature");
            double pmin=positive(o,"pressure_min_pascal"),pmax=positive(o,"pressure_max_pascal");range(pmin,pmax,"pressure");
            return new Validity(tmin,tmax,pmin,pmax,evidence);
        } catch (RuntimeException invalid) { throw new IllegalArgumentException("fluid_domain: "+invalid.getMessage(),invalid); }
    }
    private static NrtlPair nrtl(JsonObject p) {
        double min=positive(p,"temperature_min_kelvin"),max=positive(p,"temperature_max_kelvin"); range(min,max,"NRTL temperature");
        double alpha=number(p,"alpha"); if(alpha<0 || alpha>1) throw new IllegalArgumentException("NRTL alpha must be in [0,1]");
        return new NrtlPair(text(p,"first"),text(p,"second"),number(p,"a12"),number(p,"b12_kelvin"),number(p,"a21"),number(p,"b21_kelvin"),alpha,min,max);
    }
    private static String pairKey(String a,String b) { return a.compareTo(b)<0?a+"/"+b:b+"/"+a; }
    private static Map<String,JsonObject> group(Map<String,Map<String,JsonObject>> groups,String key) { return groups.getOrDefault(key,Map.of()); }
    private static <T> T require(Map<String,T> map,String id,String field) { T v=map.get(id); if(v==null)throw new IllegalArgumentException(field+": unresolved "+id);return v; }
    private static void range(double a,double b,String field) { if(a>=b)throw new IllegalArgumentException(field+": invalid range"); }
    /** An array of exactly {@code count} finite numbers. */
    private static List<Double> counted(JsonObject o,String key,int count) {
        var r=numbers(o,key);
        if(r.size()!=count)throw new IllegalArgumentException(key+": expected "+count+" coefficients/amounts");return r;
    }
    private static void checked(Map<JsonObject,String> origins,JsonObject o,Runnable action) { try{action.run();}catch(RuntimeException e){throw error(origins.get(o),e);} }
    private static IllegalArgumentException error(String path,RuntimeException e) { return new IllegalArgumentException(path+": "+e.getMessage(),e); }
    private static String hash(String s) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    /** Classpath loader shared by standalone tools and regression tests. */
    public static MaterialCatalog bundled() { return Bundled.INSTANCE; }
    private static final class Bundled {
        static final MaterialCatalog INSTANCE=load();
        static MaterialCatalog load() {
            var resources=new LinkedHashMap<String,String>();
            for(var path:JsonParser.parseString(read("materials-index.json")).getAsJsonArray()) {
                String key=path.getAsString();
                if(!key.matches("data/[a-z0-9_.-]+/materials/[a-z0-9_./-]+\\.json")||key.contains("..")||resources.putIfAbsent(key,read(key))!=null)
                    throw new IllegalArgumentException("Invalid or duplicate material index entry: "+key);
            }
            return parse(resources);
        }
        static String read(String path) {
            try(InputStream in=MaterialCatalog.class.getClassLoader().getResourceAsStream(path)) {
                if(in==null)throw new IllegalArgumentException("Missing bundled material resource "+path);
                return new String(in.readAllBytes(),StandardCharsets.UTF_8);
            }catch(IOException e){throw new UncheckedIOException(path,e);}
        }
    }
    /** Explicit shared-basis references resolve to whole ordered records before scientific validation. */
    private static final class Authoring {
        private record Basis(List<String> components,List<String> properties) {}
        private Authoring() {}
        static void expand(Map<String,Map<String,JsonObject>> groups,Map<JsonObject,String> origins) {
            var bases=groups.getOrDefault("bases",Map.of());var resolved=new HashMap<String,Basis>();
            for(String id:bases.keySet())resolve(id,bases,resolved,new LinkedHashSet<>(),origins);
            for(var entry:resolved.entrySet()) {
                var axis=entry.getValue();var row=bases.get(entry.getKey());
                for(int i=0;i<axis.components().size();i++) {
                    String component=axis.components().get(i),property=axis.properties().get(i);
                    var definition=groups.getOrDefault("properties",Map.of()).get(property);
                    if(!groups.getOrDefault("components",Map.of()).containsKey(component)||definition==null
                            ||!definition.has("component")||!component.equals(definition.get("component").getAsString()))
                        throw error(origins,row,"basis.properties: missing or mismatched component/property "+component+" / "+property);
                }
            }
            for(var p:groups.getOrDefault("packages",Map.of()).values())if(p.has("basis")) {
                if(p.has("components")||p.has("properties"))throw error(origins,p,"basis: cannot combine a reference with inline components/properties");
                var basis=resolved.get(p.get("basis").getAsString());
                if(basis==null)throw error(origins,p,"basis: missing reference "+p.get("basis"));
                p.add("components",new Gson().toJsonTree(basis.components()));p.add("properties",new Gson().toJsonTree(basis.properties()));
            }
        }
        private static Basis resolve(String id,Map<String,JsonObject> source,Map<String,Basis> resolved,
                Set<String> visiting,Map<JsonObject,String> origins) {
            if(resolved.containsKey(id))return resolved.get(id);
            var row=source.get(id);if(row==null)throw new IllegalArgumentException("basis.extends: missing reference "+id);
            if(!visiting.add(id))throw error(origins,row,"basis.extends: cycle "+visiting+" -> "+id);
            var components=new ArrayList<String>();var properties=new ArrayList<String>();
            try {
                if(row.has("extends")) {
                    var parent=resolve(row.get("extends").getAsString(),source,resolved,visiting,origins);
                    components.addAll(parent.components());properties.addAll(parent.properties());
                }
                components.addAll(strings(row,"components","basis.components"));properties.addAll(strings(row,"properties","basis.properties"));
                if(components.size()>64||components.size()!=properties.size())throw new IllegalArgumentException("basis: invalid ordered components/properties");
                new MaterialAxis(components);
                var result=new Basis(List.copyOf(components),List.copyOf(properties));resolved.put(id,result);return result;
            }catch(RuntimeException invalid){throw error(origins,row,invalid.getMessage());}
            finally {visiting.remove(id);}
        }
        private static IllegalArgumentException error(Map<JsonObject,String> origins,JsonObject row,String message) {
            return new IllegalArgumentException(origins.get(row)+": "+message);
        }
    }
    /** Validated, immutable reference data used by fluid EOS volume translation and dissolved-solute transport. */
    public static final class FluidData {
        public record VolumeReference(double temperatureKelvin,double pressurePascal,double molarVolumeCubicMetres) {}
        private final Map<String,VolumeReference> volumes;
        private final Map<String,ViscosityCorrelation> conditional;
        private FluidData(Map<String,VolumeReference> volumes,Map<String,ViscosityCorrelation> conditional) {
            this.volumes=Map.copyOf(volumes);this.conditional=Map.copyOf(conditional);
        }
        public Map<String,VolumeReference> volumeReferences(){return volumes;}
        public Map<String,ViscosityCorrelation> conditionalLiquidViscosities(){return conditional;}
        static FluidData read(Map<String,JsonObject> records,Map<JsonObject,String> origins,Set<String> components) {
            var volumes=new HashMap<String,VolumeReference>();var curves=new HashMap<String,ViscosityCorrelation>();
            for(var row:records.values())try {
                String type=text(row,"type"),revision=text(row,"revision"),source=text(row,"source");
                if(type.equals("liquid_volume_reference_points")) {
                    for(var entry:array(row,"points")) {
                        var point=entry.getAsJsonObject();String id=component(point,components);
                        text(point,"source");
                        var value=new VolumeReference(positive(point,"temperatureKelvin"),positive(point,"pressurePascal"),positive(point,"molarVolumeCubicMetres"));
                        if(volumes.putIfAbsent(id,value)!=null)throw new IllegalArgumentException("points: duplicate component "+id);
                    }
                } else if(type.equals("conditional_solute_log_tables")) {
                    double pressure=positive(row,"reference_pressure_pascal");
                    for(var entry:array(row,"curves")) {
                        var curve=entry.getAsJsonObject();String id=component(curve,components);
                        var t=numbers(curve,"temperatures_kelvin");var mu=numbers(curve,"viscosities_pascal_seconds");
                        if(t.size()<2||t.size()!=mu.size())throw new IllegalArgumentException("curves: temperature/value count mismatch");
                        var value=new ViscosityCorrelation(ViscosityCorrelation.Model.LOG_TABLE,t.getFirst(),t.getLast(),pressure,pressure,t.getFirst(),mu,List.of(),revision,source,true,t);
                        if(curves.putIfAbsent(id,value)!=null)throw new IllegalArgumentException("curves: duplicate component "+id);
                        if(curve.has("checks"))for(var check:array(curve,"checks")) {
                            var sample=check.getAsJsonObject();double temperature=positive(sample,"temperature_kelvin");positive(sample,"viscosity_pascal_seconds");
                            if(temperature<t.getFirst()||temperature>t.getLast())throw new IllegalArgumentException("checks: temperature outside sampled domain");
                        }
                    }
                } else throw new IllegalArgumentException("type: unsupported fluid reference record "+type);
            } catch(RuntimeException invalid) {throw new IllegalArgumentException(origins.get(row)+": "+invalid.getMessage(),invalid);}
            return new FluidData(volumes,curves);
        }
        private static String component(JsonObject row,Set<String> components){String id=text(row,"component");if(!components.contains(id))throw new IllegalArgumentException("component: unknown "+id);return id;}
    }
    /** Liquid render tint, independent of thermodynamic and optical properties. */
    public record FluidAppearance(int rgb, double transparency, boolean estimated) {
        public FluidAppearance {
            if (rgb < 0 || rgb > 0xFFFFFF) throw new IllegalArgumentException("appearance.color must be 24-bit RGB");
            if (!Double.isFinite(transparency) || transparency < 0 || transparency > 1)
                throw new IllegalArgumentException("appearance.transparency must be finite and between 0 and 1");
        }

        /** Packed AARRGGBB tint. Transparency 0 is opaque; 1 is invisible. */
        public int argb() { return ((int) Math.round((1 - transparency) * 255) << 24) | rgb; }

    }
}
