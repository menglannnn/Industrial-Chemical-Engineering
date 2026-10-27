package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind;
import com.wormzjl.createcheme.science.material.MaterialTestBasis;
import org.junit.jupiter.api.Test;
import static com.wormzjl.createcheme.runtime.fluid.PhysicalFluidTopology.Direction.*;

/** Hold class E3 (vapour partial-pressure floor): reductions of fuzz seed 30. Probe, not a gate. */
class E3ProbeTest {
    static final int WATER=MaterialTestBasis.NETWORK,NITROGEN=MaterialTestBasis.NITROGEN;
    static void report(EventPathWorld w,String label) {
        System.out.println("E3PROBE "+label+" holds="+w.held.size()+(w.held.isEmpty()?"":" first="+w.held.getFirst()));
        for(var s:w.coordinator.snapshots())System.out.println("E3PROBE "+label+" island "+s.id()+" committed="+s.clock().committedTick()+" "+s.status());
    }
    /** Seed 30's held island alone: a water generator, a junction pipe with a void above it, a pipe to a nitrogen generator placed later. */
    @Test void seed30Island() {
        var w=new EventPathWorld();
        w.runTo(100);
        w.place(0,0,0,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(WATER,355.6753347596607,361077.84297592135),.02);
        w.place(0,0,1,Kind.PIPE,NORTH,.02);w.place(0,1,1,Kind.VOID,NORTH,.02);w.place(0,0,2,Kind.PIPE,NORTH,.02);
        w.runTo(812);
        w.place(0,0,3,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(NITROGEN,333.2954119245145,366182.8050784859),.02);
        w.runTo(2000);report(w,"seed30-island");
    }
    /** The same, every block in one tick. */
    @Test void seed30IslandAtOnce() {
        var w=new EventPathWorld();
        w.runTo(100);
        w.place(0,0,0,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(WATER,355.6753347596607,361077.84297592135),.02);
        w.place(0,0,1,Kind.PIPE,NORTH,.02);w.place(0,1,1,Kind.VOID,NORTH,.02);w.place(0,0,2,Kind.PIPE,NORTH,.02);
        w.place(0,0,3,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(NITROGEN,333.2954119245145,366182.8050784859),.02);
        w.runTo(2000);report(w,"seed30-at-once");
    }
    /** The nitrogen generator first (the junction minted with nitrogen), the water generator later. */
    @Test void seed30NitrogenFirst() {
        var w=new EventPathWorld();
        w.runTo(100);
        w.place(0,0,3,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(NITROGEN,333.2954119245145,366182.8050784859),.02);
        w.place(0,0,1,Kind.PIPE,NORTH,.02);w.place(0,1,1,Kind.VOID,NORTH,.02);w.place(0,0,2,Kind.PIPE,NORTH,.02);
        w.runTo(812);
        w.place(0,0,0,Kind.GENERATOR,NORTH,EventPathWorld.defaultControl(Kind.GENERATOR),EventPathWorld.pure(WATER,355.6753347596607,361077.84297592135),.02);
        w.runTo(2000);report(w,"seed30-n2-first");
    }
    static FluidDeviceSpec n2(){return EventPathWorld.pure(NITROGEN,298.15,101325);}
    static FluidDeviceSpec water1atm(){return EventPathWorld.pure(WATER,298.15,101325);}
    static final com.wormzjl.createcheme.science.fluid.network.FlowControl PASSIVE=new com.wormzjl.createcheme.science.fluid.network.FlowControl.Passive();
    /** Defaults seed 30 reduced (HoldMinimizerTest, pass >= 1): a 1 atm water generator, a nitrogen tank and two voids at 1 atm, three junction pipes, all in one tick. */
    @Test void defaults30Reduced() {
        var w=new EventPathWorld();w.runTo(103);
        w.place(0,0,1,Kind.PIPE,NORTH,.05);w.place(-1,0,0,Kind.PIPE,NORTH,.05);
        w.place(0,0,0,Kind.GENERATOR,NORTH,PASSIVE,water1atm(),.05);w.place(0,1,1,Kind.VOID,NORTH,PASSIVE,n2(),.05);
        w.place(-1,0,1,Kind.PIPE,NORTH,.05);w.place(-1,0,-2,Kind.RESERVOIR,NORTH,PASSIVE,n2(),.05);
        w.place(-2,0,1,Kind.VOID,NORTH,PASSIVE,n2(),.05);w.place(-1,0,-1,Kind.PIPE,NORTH,.05);
        w.runTo(2000);report(w,"defaults30-reduced");
    }
}
