package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.SolverOwnership;
import com.wormzjl.createcheme.science.fluid.network.PassiveIntervalSolver;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.network.PassiveIntervalSolver.StageGuard;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.Objects;

/**
 * One interval solver kept for one island across its jobs, so the sparsity pattern, the colouring and the
 * fill-reducing ordering survive between intervals instead of being rebuilt per job.
 *
 * <p>Nothing numeric crosses a job boundary but the committed interval: at the start of every job the solver is reset to
 * what a fresh solver would derive from the island's committed interval (its first step starts from the interval's end
 * flows, heads and modes) ({@link PassiveIntervalSolver#replayStart}; the coordinator hands the
 * committed interval over at dispatch, {@link #committed}), and an ordinary interval starts at
 * {@code min(initialStep, duration)}, a function of the interval alone. A certified island is woken with a new handle,
 * and its replay must reproduce the island that solved every interval bit for bit; a factorization or a step estimate
 * carried from an earlier job made them differ in the last digits (HANDOFF_REVIEW.md 8.9 of the mixed-gas junction
 * batch). The cost is one fresh Jacobian per job, +8 % wall time at 5 s slices.
 *
 * <p>This is ephemeral cost state, never material state: dropping it costs one Jacobian build and a
 * few substeps and changes nothing that is committed. The coordinator replaces the handle whenever
 * an island's revision changes (property suspension, topology repartition), and a new island gets a
 * new handle, so a stale model or topology can never reach a retained workspace.
 *
 * <p>Exclusive use is enforced by {@link SolverOwnership}: each call claims the latch and returns it
 * in a {@code finally}, including on cancellation and on the wall/soft deadlines, and a second
 * concurrent job on the same island fails fast instead of corrupting the cached factorization.
 * Workers therefore still touch only the immutable snapshot and this handle.
 */
public final class RetainedSolver {
    private final SolverOwnership ownership=SolverOwnership.released();
    private PassiveIntervalSolver solver;
    private FluidThermodynamics model;
    /** The island's committed interval (null for none), handed over at each dispatch, and whether the job in progress
     * has not yet reset the solver from it. */
    private PassiveIntervalSolver.Result committed;
    private boolean replayPending;
    /** Records the island's committed interval (null for none) for the next job's start. Called by the coordinator when
     * it dispatches, while no job holds this handle. */
    public void committed(PassiveIntervalSolver.Result result){this.committed=result;}
    private void begin(PassiveIntervalSolver solver,PassiveNetwork snapshot) {
        if(!replayPending)return;
        replayPending=false;solver.replayStart(snapshot,committed);
    }

    /** One job's exclusive use of this island's solver: claims the latch and returns it in a {@code finally}, including
     * on cancellation and on the wall/soft deadlines. */
    public PassiveIntervalSolver.Result solve(FluidThermodynamics model,PassiveNetwork snapshot,double duration,
                                              PassiveIntervalSolver.Settings settings,Runnable checkpoint) {
        var solver=acquire(model);replayPending=true;
        try {
            begin(solver,snapshot);
            return solver.solve(snapshot,duration,settings,checkpoint,Math.min(settings.initialStep(),duration));
        }
        finally{ownership.release();}
    }
    public PassiveIntervalSolver.Result solveApproximate(FluidThermodynamics model,PassiveNetwork snapshot,double duration,
                                                         PassiveIntervalSolver.Settings settings,Runnable checkpoint,StageGuard guard) {
        var solver=acquire(model);replayPending=true;
        try {
            begin(solver,snapshot);
            return solver.solveApproximate(snapshot,duration,settings,checkpoint,guard);
        }
        finally{ownership.release();}
    }
    private PassiveIntervalSolver acquire(FluidThermodynamics model) {
        Objects.requireNonNull(model);ownership.acquire();
        try {
            if(solver==null||this.model!=model){solver=new PassiveIntervalSolver(model,ownership);this.model=model;}
            return solver;
        }catch(RuntimeException|Error failure){ownership.release();throw failure;}
    }
}
