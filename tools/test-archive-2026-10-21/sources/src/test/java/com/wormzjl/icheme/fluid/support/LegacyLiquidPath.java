package com.wormzjl.icheme.fluid.support;

import com.wormzjl.icheme.science.fluid.thermo.TranslatedPengRobinson;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.thermo.PengRobinson78;
import com.wormzjl.icheme.science.thermo.PengRobinsonKernel.Root;
import com.wormzjl.icheme.science.thermo.ThermoComponent;
import java.util.List;

/**
 * The retired reference-pressure liquid path (before P3 WP4), rebuilt from the same catalog for the continuity and
 * deviation checks only: the translated PR78 liquid at 2 MPa, anchored through {@code exp(k (P_anchor - 2 MPa))},
 * carried to the state pressure by one global compressibility {@code k} ({@code v = v_ref exp(-k (P - Pref))}, the
 * enthalpy and {@code ln phi} by the matching integrals). The arithmetic is the deleted {@code GlobalLiquidResponse}'s
 * and {@code HydrocarbonModel}'s, line for line. It also exposes both formulations' translations, so a first-order
 * estimate can separate the pressure term from the anchor term.
 */
public final class LegacyLiquidPath {
    public static final double REFERENCE_PRESSURE=2e6,COMPRESSIBILITY=1e-9;
    private static final double R=8.31446261815324;
    private final TranslatedPengRobinson legacy;
    private final double[] legacyShifts,directShifts;
    private final double k;

    public LegacyLiquidPath(MaterialCatalog catalog,String packageId){this(catalog,packageId,COMPRESSIBILITY);}
    public LegacyLiquidPath(MaterialCatalog catalog,String packageId,double compressibility) {
        k=compressibility;
        var propertyPackage=catalog.requirePackage(packageId);
        List<ThermoComponent> components=propertyPackage.properties().stream().map(p->new ThermoComponent(
                p.component(),p.pr().criticalTemperature(),p.pr().criticalPressure(),p.pr().acentricFactor(),p.molecularWeight())).toList();
        int count=components.size();
        double[][] interactions=new double[count][count],cp=new double[count][6];
        for(int i=0;i<count;i++) {
            for(int j=0;j<count;j++)interactions[i][j]=propertyPackage.interactions().get(i).get(j);
            var property=propertyPackage.properties().get(i);
            for(int j=0;j<6;j++)cp[i][j]=property.cp().get(j);
        }
        var raw=new PengRobinson78(components,interactions);
        var calibrations=catalog.fluidData().volumeReferences();
        legacyShifts=new double[count];directShifts=new double[count];
        for(int i=0;i<count;i++) {
            var property=propertyPackage.properties().get(i);
            var point=calibrations.get(property.component());
            double t=point==null?property.standardTemperature():point.temperatureKelvin();
            double target=point==null?property.molecularWeight()/property.density():point.molarVolumeCubicMetres();
            double anchorP=point==null?property.standardPressure():point.pressurePascal();
            double[] pure=new double[count];pure[i]=1;
            double atReference=raw.evaluate(t,REFERENCE_PRESSURE,pure,Root.LIQUID).compressibilityFactor()*PengRobinson78.GAS_CONSTANT*t/REFERENCE_PRESSURE;
            double atAnchor=raw.evaluate(t,anchorP,pure,Root.LIQUID).compressibilityFactor()*PengRobinson78.GAS_CONSTANT*t/anchorP;
            legacyShifts[i]=target*Math.exp(k*(anchorP-REFERENCE_PRESSURE))-atReference;
            directShifts[i]=target-atAnchor;
        }
        legacy=new TranslatedPengRobinson(components,interactions,cp,legacyShifts);
    }

    /** {@code c_i} of the retired path, m3/mol. */
    public double legacyShift(int component){return legacyShifts[component];}
    /** {@code c_i} of the direct path (the anchor at its own pressure), m3/mol, as {@code HydrocarbonModel} computes it. */
    public double directShift(int component){return directShifts[component];}
    public double compressibility(){return k;}

    /** The retired liquid at (T, P): volume, enthalpy, {@code dv/dP}, {@code dh/dP}, {@code ln phi_i} and partial molar volumes. */
    public record Liquid(double molarVolume,double molarEnthalpy,double volumePressureDerivative,double enthalpyPressureDerivative,
                         double[] logFugacity,double[] partialMolarVolumes) {}

    public Liquid liquid(double temperature,double pressure,double[] amounts) {
        var reference=legacy.evaluate(temperature,REFERENCE_PRESSURE,amounts,Root.LIQUID,legacy.prepare(temperature));
        double exponent=-k*(pressure-REFERENCE_PRESSURE);
        if(Math.abs(exponent)>1)throw new IllegalArgumentException("Liquid compression approximation outside domain");
        double factor=Math.exp(exponent),integral=-Math.expm1(exponent)/k;
        double v=reference.molarVolume()*factor,vt=reference.volumeTemperatureDerivative()*factor;
        double h=reference.molarEnthalpy()+(reference.molarVolume()-temperature*reference.volumeTemperatureDerivative())*integral;
        double[] phi=reference.logFugacityCoefficients(),partial=reference.partialMolarVolumes();
        for(int i=0;i<phi.length;i++) {
            phi[i]+=Math.log(REFERENCE_PRESSURE/pressure)+partial[i]*integral/(R*temperature);
            partial[i]*=factor;
        }
        return new Liquid(v,h,-k*v,v-temperature*vt,phi,partial);
    }
}
