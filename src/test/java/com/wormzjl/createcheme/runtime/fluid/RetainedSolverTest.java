package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import static org.junit.jupiter.api.Assertions.*;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import com.wormzjl.createcheme.science.material.MaterialCatalog;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RetainedSolverTest {
    private static FluidThermodynamics model() {
        return FluidTestSupport.networkModel();
    }
    private static PassiveNetwork graph(FluidThermodynamics model) {
        return new PassiveNetwork(List.of(
                new PassiveNetwork.Reservoir(1,0,model.initialNitrogenCharge(1,350,150000,()->{}),PassiveNetwork.NodeKind.GENERATOR),
                new PassiveNetwork.Reservoir(2,0,model.initialNitrogenCharge(1,350,149000,()->{}))),
                List.of(new PassiveNetwork.Pipe(1,0,1,new PipeResistance.Geometry(100,.01,.000045,0))));
    }

    @Test void aRetainedSolverReusesItsStructureAcrossTheIntervalBoundary() {
        var model=model();var retained=new RetainedSolver();var settings=PassiveIntervalSolver.Settings.defaults();
        var next=retained.solve(model,graph(model),5,settings,()->{}).graph();
        var results=new PassiveIntervalSolver.Result[2];
        var carried=measure(()->results[0]=retained.solve(model,next,5,settings,()->{}));
        var cold=measure(()->results[1]=new RetainedSolver().solve(model,next,5,settings,()->{}));
        assertEquals(0,carried.value("luOrderings"),"the fill-reducing ordering must survive the interval boundary");
        assertTrue(cold.value("luOrderings")>0,"a fresh handle still orders the matrix");
        // Nothing numeric crosses a job boundary (deterministic replay, WP1 of the mixed-gas junction batch): the
        // carried job builds exactly the Jacobians a fresh handle builds and reaches the same state bit for bit.
        assertEquals(cold.value("jacobianBuilds"),carried.value("jacobianBuilds"),"carried vs fresh Jacobian builds");
        for(int i=0;i<next.reservoirs().size();i++) {
            assertEquals(results[1].graph().reservoirs().get(i).inventory(),results[0].graph().reservoirs().get(i).inventory(),"a job must not depend on what the handle solved before");
            assertEquals(results[1].graph().reservoirs().get(i).state().pressure(),results[0].graph().reservoirs().get(i).state().pressure(),0.0);
        }
    }
    private static SolverDiagnostics.Sample measure(Runnable work) {
        SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
        try{work.run();}finally{SolverDiagnostics.ENABLED=false;}
        return SolverDiagnostics.sample();
    }

    @Test void aRetainedSolverIsExclusiveAndIsHandedBackAfterEveryOutcome() {
        var model=model();var retained=new RetainedSolver();var settings=PassiveIntervalSolver.Settings.defaults();
        var graph=graph(model);var concurrent=new AtomicReference<Throwable>();
        retained.solve(model,graph,5,settings,()->{
            if(concurrent.get()!=null)return;
            var other=new Thread(()->{
                try{retained.solve(model,graph,5,settings,()->{});concurrent.set(new AssertionError("second job was admitted"));}
                catch(Throwable expected){concurrent.set(expected);}
            });
            other.start();
            try{other.join();}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
        });
        assertInstanceOf(IllegalStateException.class,concurrent.get(),"a second concurrent job must fail fast");
        // The failed interval below must still hand the latch back, or this last solve would throw.
        assertThrows(RuntimeException.class,()->retained.solve(model,graph,5,settings,()->{throw new IllegalStateException("cancelled");}));
        assertNotNull(retained.solve(model,graph,5,settings,()->{}));
    }
}
