package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.fluid.support.FluidTestSupport;
import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * The unified thermo port's test world, equilibrium numbers (tools/unified-thermo-port-test-world, not a gate): what a
 * generator of the test world holds at its setting (the TP answer the network states), and where its stream ends in a
 * slack tank at 1 atm or a taut one (the PH answer with the CO2-I crystal competing, as a vessel's refresh states it). Basis
 * N2, CH4, C2H6, CO2, water.
 */
class TestWorldEngineProbeTest {
    static final String PILOT="icheme:pilot_cryogenic";
    final FluidThermodynamics model=FluidTestSupport.model(MaterialCatalog.bundled(),PILOT);
    static final double ATM=101325;

    static double[] z(double n2,double ch4,double c2h6,double co2,double water){double s=n2+ch4+c2h6+co2+water;return new double[]{n2/s,ch4/s,c2h6/s,co2/s,water/s};}
    static String v(double[] a){var b=new StringBuilder("[");for(int i=0;i<a.length;i++){if(i>0)b.append(' ');b.append(String.format(Locale.ROOT,"%.4g",a[i]));}return b.append(']').toString();}
    static void out(String f,Object...a){System.out.println("ENGINE "+String.format(Locale.ROOT,f,a));}

    /** The generator's state per mole of feed, and the PH answers of its stream at the given pressures. */
    void feed(String label,double t,double p,double[] feed,double...pressures) {
        FluidThermodynamics.State g;
        try{g=model.flashTP(t,p,feed,()->{});}
        catch(RuntimeException e){out("%s @ %.2f K %.0f Pa: generator refused: %s: %s",label,t,p,e.getClass().getSimpleName(),e.getMessage());return;}
        double n=0;for(double x:feed)n+=x;
        out("%s @ %.2f K %.0f Pa: generator liquid %.4e vapour %.4e m3/mol, x=%s y=%s, h=%.1f J/mol",label,t,p,g.liquidVolume()/n,g.vaporVolume()/n,v(g.liquidView()),v(g.vaporView()),g.enthalpy()/n);
        var crystal=model.crystalSolids().all().isEmpty()?null:model.crystalSolids().all().getFirst();
        for(double q:pressures) {
            try {
                var a=model.crystalsAtPressure(feed,g.enthalpy(),q,true,()->{});
                var f=a.fluid();double nc=a.holdsCrystal()?a.crystalMoles()[0]:0;
                double vc=crystal==null?0:nc*crystal.molarMass()/crystal.material().density();
                double nl=0,nv=0;for(double x:f.liquidView())nl+=x;for(double x:f.vaporView())nv+=x;
                double condensed=f.liquidVolume()+vc;
                out("   -> %.0f Pa: T=%.2f K crystal %.4f of the feed moles (%.4e m3/mol at the record's density), liquid %.4f vapour %.4f of the moles; liquid x=%s; solids/(liquid+solids)=%s",
                        q,a.temperature(),nc/n,vc/n,nl/n,nv/n,v(f.liquidView()),condensed>0?String.format(Locale.ROOT,"%.3f",vc/condensed):"-");
            } catch(RuntimeException e){out("   -> %.0f Pa: %s: %s",q,e.getClass().getSimpleName(),e.getMessage());}
        }
    }

    @Test void numbers() {
        var c=model.crystalSolids().all().getFirst();
        out("crystal %s M=%.5f kg/mol record density %.2f kg/m3",c.material().id(),c.molarMass(),c.material().density());
        // D23 row 1 and E4 row 3: liquid CO2 let down; CO2 at 210 K and 400 kPa (below its sublimation line, 400 kPa at 212.9 K).
        feed("liquid CO2",250,2.5e6,z(0,0,0,1,0),ATM,2e5);
        feed("liquid CO2",290,6e6,z(0,0,0,1,0),ATM);
        feed("CO2 R3",210,4e5,z(0,0,0,1,0),ATM,2e5,4e5);
        feed("CO2 R2 gas",250,2.5e5,z(0,0,0,1,0),ATM);
        // D23 row 2: LNG with 0.5 % CO2; the slush candidates (CH4 + 25, 30, 35 % CO2).
        feed("LNG 0.5% CO2",120,1.2e6,z(1,93.5,5,.5,0),ATM,2e5);
        feed("methane",115,2e5,z(0,1,0,0,0),ATM);
        feed("methane",120,1.2e6,z(0,1,0,0,0),ATM);
        for(double x:new double[]{.2,.25,.3,.35})for(double t:new double[]{140,150})feed("CH4 + "+(int)Math.round(x*100)+"% CO2",t,2e6,z(0,1-x,0,x,0),ATM);
        // E4 rows 3, 4, 8, 11, 13: N2 with 10 % CO2.
        for(double t:new double[]{140,150,160,170,180})feed("N2 + 10% CO2",t,ATM,z(.9,0,0,.1,0),ATM);
        feed("N2 + 10% CO2",160,3e5,z(.9,0,0,.1,0),ATM,2e5,3e5);
        feed("N2 + 10% CO2",250,3e5,z(.9,0,0,.1,0),3e5,5e5);
        feed("N2 + 10% CO2",220,8e6,z(.9,0,0,.1,0),ATM,3e5,1e6,4e6);
        feed("N2",150,3e5,z(1,0,0,0,0),ATM);
        // E4 row 10: N2/C2H6/CO2 40/55/5 at 2 MPa.
        for(double t:new double[]{95,110,130,150})feed("N2/C2H6/CO2 40/55/5",t,2e6,z(40,0,55,5,0),ATM,2e6);
        // The revised rows: R3's CO2 as a gas above its sublimation line (220 K, 400 kPa); R11's feed at 175 K; R4's cold
        // charge (N2 at 110 K); R2's methane let down to the tank's pressures.
        feed("CO2 R3 gas",220,4e5,z(0,0,0,1,0),ATM,2e5,3e5,4e5);
        feed("N2 + 10% CO2",175,3e5,z(.9,0,0,.1,0),ATM,2e5,2.5e5,3e5);
        feed("N2",110,3e5,z(1,0,0,0,0),ATM,3e5);
        feed("methane",115,2e5,z(0,1,0,0,0),1.5e5,2e5);
    }
}
