package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialRuntime;
import com.wormzjl.createcheme.science.material.PilotCryogenicTestCatalog;
import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * M3 probe (not a gate; compiled with the cold fuzz harness on the index lane): the cold fuzz's new C holds (methane's
 * 90.6941 K floor; seeds 34, 75, 78, 98, 114 and 170 at the M3 head, clean on the port head). For a seed, its LNG
 * generators' specs, their TP answers at the generator's temperature and at the ambient pressure, and one interval of the
 * smallest world they hold: the generator through a 1 m pipe of the seed's bore into an empty 1 m3 tank.
 */
class M3ColdArrivalProbeTest {
    static { System.setProperty("holdFuzz.cold","true"); }
    static final String PILOT="createcheme:pilot_cryogenic";
    static void p(String format,Object... args){System.out.println("M3PROBE "+String.format(Locale.ROOT,format,args));}
    static String phases(FluidThermodynamics.State s) {
        return String.format(Locale.ROOT,"T=%.4f K P=%.1f Pa liq=%.4e vap=%.4e V=%.6f x=%s y=%s",s.temperature(),s.pressure(),s.liquidVolume(),s.vaporVolume(),s.volume(),
                Arrays.toString(s.liquidView()),Arrays.toString(s.vaporView()));
    }

    @Test void seed34(){seed(34);}
    @Test void seed75(){seed(75);}

    void seed(long seed) {
        var catalog=PilotCryogenicTestCatalog.catalog();
        MaterialRuntime.with(catalog,PILOT,()->{
            var model=FluidThermodynamics.forNetwork(catalog,PILOT);
            var blocks=HoldFuzzTest.world(new Random(seed));
            double bore=Double.NaN;for(var b:blocks)if(b.kind()==Kind.PIPE){bore=b.bore();break;}
            for(var b:blocks) {
                if(b.kind()!=Kind.GENERATOR)continue;
                var spec=b.spec();
                p("[%d] %s: T=%.4f K P=%.1f Pa z=%s",seed,b.label(),spec.temperature(),spec.pressure(),Arrays.toString(spec.composition()));
                var generator=model.flashTP(spec.temperature(),spec.pressure(),spec.composition(),()->{});
                p("[%d]   at the generator: %s",seed,phases(generator));
                var ambient=model.flashTP(spec.temperature(),101325,spec.composition(),()->{});
                p("[%d]   at its temperature and the ambient pressure: %s",seed,phases(ambient));
                double[] n=spec.composition();double scale=1/generator.volume();for(int i=0;i<n.length;i++)n[i]*=scale;
                var source=model.flashTP(spec.temperature(),spec.pressure(),n,()->{});
                var graph=new PassiveNetwork(List.of(new PassiveNetwork.Reservoir(1,0,source,PassiveNetwork.NodeKind.GENERATOR),PassiveNetwork.emptyVessel(2,0,1,model)),
                        List.of(new PassiveNetwork.Pipe(10,0,1,new PipeResistance.Geometry(1,Double.isNaN(bore)?.01:bore,PipeResistance.DEFAULT_ROUGHNESS_METRES,0))));
                try {
                    var result=new PassiveIntervalSolver(model).solve(graph,5,PassiveIntervalSolver.Settings.defaults(),()->{},.05);
                    p("[%d]   one interval into an empty tank: %s; accepted %d rejected %d reasons %s",seed,phases(result.graph().reservoirs().get(1).state()),
                            result.acceptedSubsteps(),result.rejectedSubsteps(),result.rejectionReasons());
                } catch(RuntimeException failure) {
                    p("[%d]   one interval into an empty tank failed: %s",seed,failure.getMessage());
                }
            }
            return null;
        });
    }
}
