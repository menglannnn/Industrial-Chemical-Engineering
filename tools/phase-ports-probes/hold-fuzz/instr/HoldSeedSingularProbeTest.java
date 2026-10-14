package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.linalg.SparseMatrix;
import com.wormzjl.createcheme.science.fluid.network.PassiveStepSolver;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Needs tools/phase-ports-probes/holds3/instr/instr-f8-singular-dump.patch applied. Replays each seed of
 * {@code holdFuzz.seeds} and prints, for the first singular factorizations it meets, the Newton layout and the Jacobian's
 * smallest singular values with their right (unknowns) and left (rows) singular vectors (F8NullVectorProbeTest.dump).
 */
class HoldSeedSingularProbeTest {
    @Test void singularJacobiansOfTheSeeds() {
        for(long seed:HoldFuzzTest.seeds(System.getProperty("holdFuzz.seeds","61"))) {
            var layouts=new ArrayList<String>();var matrices=new ArrayList<SparseMatrix>();var values=new ArrayList<double[]>();
            PassiveStepSolver.INSTR_LAYOUT=s->{if(s.contains("Singular")&&layouts.size()<2)layouts.add(s);};
            SparseNewton.INSTR_SINGULAR=(m,f)->{if(matrices.size()<2){matrices.add(m);values.add(f);}};
            try{var o=HoldFuzzTest.run(seed,Integer.getInteger("holdFuzz.slices",120),false);System.out.println("SINGULAR_PROBE seed="+seed+" holds="+o.held().size()+(o.held().isEmpty()?"":" first="+o.held().getFirst()));}
            finally{PassiveStepSolver.INSTR_LAYOUT=null;SparseNewton.INSTR_SINGULAR=null;}
            for(int k=0;k<Math.min(1,Math.min(layouts.size(),matrices.size()));k++){System.out.println("SINGULAR_PROBE seed="+seed+" layout:\n"+layouts.get(k));F8NullVectorProbeTest.dump(matrices.get(k),values.get(k));}
        }
    }
}
