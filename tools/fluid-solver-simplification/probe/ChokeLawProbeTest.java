package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.column.v3.thermo.V3PengRobinsonThermo;
import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.thermo.FluidMaterialCatalog;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Locale;

/**
 * t38 form D probe (run on a form-D1 export): the choke law's numbers at the network's usual states and the cold package's
 * extremes, against the cap the velocity limit gave before (min of 100 m/s and the isothermal acoustic bound). One CHOKE
 * line per state and phase: density, isothermal sound speed, gamma, the choke flux and velocity, and the old cap velocity.
 */
class ChokeLawProbeTest {
    private static void print(String label,FluidThermodynamics model,FluidThermodynamics.State s,double oldLimit) {
        double rho=s.mass()/s.volume();
        System.out.println(String.format(Locale.ROOT,"CHOKE %s T=%.2f P=%.0f rhoBulk=%.3f liqV=%.4g vapV=%.4g cT=%.1f bulkChoke=%.4g kg/m2s v=%.2f m/s oldCap=%.2f m/s",
                label,s.temperature(),s.pressure(),rho,s.liquidVolume(),s.vaporVolume(),model.isothermalAcousticBound(s),model.chokeFlux(s,-1),model.chokeFlux(s,-1)/rho,Math.min(oldLimit,model.isothermalAcousticBound(s))));
        if(s.vaporVolume()>0){double rg=model.vaporDensity(s);double g=model.chokeFlux(s,FluidThermodynamics.GAS);
            System.out.println(String.format(Locale.ROOT,"CHOKE %s gas rho=%.4f G*=%.4g v*=%.1f m/s (old: min of %.0f and the vapour acoustic bound)",label,rg,g,g/rg,oldLimit));}
        if(s.liquidVolume()>0){double rl=model.phaseMass(s,FluidThermodynamics.OIL)/FluidThermodynamics.phaseVolume(s,FluidThermodynamics.OIL);double g=model.chokeFlux(s,FluidThermodynamics.OIL);
            System.out.println(String.format(Locale.ROOT,"CHOKE %s oil rho=%.2f G=%.4g v=%.2f m/s (old 100 m/s)",label,rl,g,g/rl));}
        if(s.waterVolume()>0){double rw=model.phaseMass(s,FluidThermodynamics.WATER)/FluidThermodynamics.phaseVolume(s,FluidThermodynamics.WATER);double g=model.chokeFlux(s,FluidThermodynamics.WATER);
            System.out.println(String.format(Locale.ROOT,"CHOKE %s water rho=%.2f G=%.4g v=%.2f m/s (old 100 m/s)",label,rw,g,g/rw));}
    }
    private static FluidThermodynamics.State at(FluidThermodynamics model,double t,double p,double[] n) {
        var unit=model.flashTP(t,p,n,()->{});double[] m=n.clone();for(int c=0;c<m.length;c++)m[c]/=unit.volume();
        return model.flashTP(t,p,m,()->{});
    }
    @Test void coldPackage() {
        var catalog=PilotCryogenicTestCatalog.catalog();var network=FluidMaterialCatalog.networkPackage(catalog);
        MaterialRuntime.with(catalog,network,()->{
            var model=FluidThermodynamics.forNetwork(catalog,network);double old=model.maximumVelocityMetresPerSecond();
            double[] methane={0,1,0,0,0},nitrogen={1,0,0,0,0},lng={0.0019051322577280617,0.8927278421557977,0.06093423437004952,0.04443279121642472,0};
            print("methane-300K-1atm",model,at(model,300,101325,methane),old);
            print("methane-300K-1MPa",model,at(model,300,1e6,methane),old);
            print("nitrogen-298K-1atm",model,at(model,298.15,101325,nitrogen),old);
            print("methane-200K-6MPa",model,at(model,200,6e6,methane),old);
            print("methane-195K-4.5MPa-near-critical",model,at(model,195,4.5e6,methane),old);
            print("methane-230K-6MPa",model,at(model,230,6e6,methane),old);
            print("lng-111.8K-1atm-two-phase",model,at(model,111.8,101325,lng),old);
            print("lng-112K-222kPa-subcooled",model,at(model,111.65722637305922,222268.7652191971,lng),old);
            print("lng-112K-3MPa-subcooled",model,at(model,112,3e6,lng),old);
            print("lng-150K-6MPa",model,at(model,150,6e6,lng),old);
            return null;});
    }
    @Test void bundledPackage() {
        var model=FluidTestSupport.networkModel();double old=model.maximumVelocityMetresPerSecond();
        int count=MaterialTestBasis.NETWORK+1;
        double[] water=new double[count];water[MaterialTestBasis.NETWORK]=1;
        double[] nitrogen=new double[count];nitrogen[MaterialTestBasis.NITROGEN]=1;
        double[] crude=Arrays.copyOf(V3PengRobinsonThermo.fromRegisteredPackage("createcheme:tjl20_methane_nitrogen").crudeFeed("createcheme:tia_juana_light_methane").moleFractions(),count);
        print("water-298K-1atm",model,at(model,298.15,101325,water),old);
        print("water-372K-1atm",model,at(model,372.5,101325,water),old);
        print("nitrogen-298K-1atm",model,at(model,298.15,101325,nitrogen),old);
        print("crude-311K-316kPa-subcooled",model,at(model,311.1640418552006,315670.8797145338,crude),old);
        print("crude-311K-139kPa-two-phase",model,at(model,311.1640418552006,139000,crude),old);
        print("crude-311K-120kPa-two-phase",model,at(model,311.1640418552006,120000,crude),old);
    }
}
