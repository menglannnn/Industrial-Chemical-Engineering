import com.wormzjl.icheme.runtime.fluid.*;
import com.wormzjl.icheme.science.material.MaterialCatalog;
import com.wormzjl.icheme.science.fluid.thermo.*;
import com.wormzjl.icheme.science.fluid.network.*;
import com.wormzjl.icheme.science.fluid.topology.TopologyCompiler.Kind;
import java.util.*;

/** Fresh configured feed only; never opens a world, advances time or changes product state. */
public final class GeneratorPhaseAudit {
    public static void main(String[] args) {
        var catalog=MaterialCatalog.bundled();
        var model=FluidThermodynamics.forNetwork(catalog,FluidMaterialCatalog.networkPackage(catalog));
        var preset=FluidPresetCatalog.resolve(catalog).stream().filter(p->p.name().equals("WTI Light Export")).findFirst().orElseThrow();
        var device=new PhysicalFluidTopology.Device(1,new PhysicalFluidTopology.Position("minecraft:overworld",0,64,0),Kind.GENERATOR,PhysicalFluidTopology.Direction.NORTH,new PipeResistance.Geometry(1,.05,.000045,0),new FlowControl.Passive(),List.of(),false);
        System.out.println("preset="+preset.name()+" package="+FluidMaterialCatalog.networkPackage(catalog));
        for(double pressure:new double[]{300000,401325}) {
            var spec=new FluidDeviceSpec(1,473.15,pressure,preset.moleFractions(),preset.solids());
            var node=spec.initialize(device,model,()->{});var s=node.state();
            double lv=s.liquidVolume(),wv=s.waterVolume(),gv=s.vaporVolume(),total=lv+wv+gv+s.solids().volume();
            double liquidMass=0,vaporMass=0;var l=s.liquid();var v=s.vapor();var mw=model.molecularWeights();
            for(int c=0;c<l.length;c++){liquidMass+=l[c]*mw[c];vaporMass+=v[c]*mw[c];}
            double lm=Arrays.stream(l).sum(),vm=Arrays.stream(v).sum();
            System.out.printf(Locale.ROOT,"T=%.2f K P=%.0f Pa V=%.12f m3 mass=%.9f kg organicV=%.9f aqueousV=%.9f vaporV=%.9f organicVolumePct=%.9f vaporVolumePct=%.9f organicMassPct=%.9f vaporMassPct=%.9f organicMolePct=%.9f vaporMolePct=%.9f liquidDensity=%.9f vaporDensity=%.9f%n",s.temperature(),s.pressure(),total,s.mass(),lv,wv,gv,100*lv/total,100*gv/total,100*liquidMass/s.mass(),100*vaporMass/s.mass(),100*lm/(lm+vm),100*vm/(lm+vm),liquidMass/lv,vaporMass/gv);
            var compiled=PhysicalFluidTopology.compile(List.of(device),Map.of(1L,node));
            var isolated=compiled.islands().getFirst().graph().reservoirs().stream().filter(n->n.id()==1).findFirst().orElseThrow();
            if(!node.equals(isolated))throw new AssertionError("Isolation changed source state");
            System.out.println("isolated_generator_state_unchanged=true");
        }
    }
}
