package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.linalg.SparseMatrix;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.solver.SparseNewton;
import java.util.*;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.junit.jupiter.api.Test;

/**
 * Needs src/instr-f8-singular-dump.patch. Replays the line up to the filter (the state of hold F8), and at the first
 * singular factorization prints the Newton layout (every node's and edge's columns) and the Jacobian's smallest singular
 * values with their right (unknowns) and left (rows) singular vectors. Then solves the held island's graph directly: the
 * rate solve of its port graph (what SolidEventIntegrator runs at the interval's t0) and one backward-Euler step.
 */
class F8NullVectorProbeTest {
    @Test void dumpTheSingularJacobian() {
        var outer=new InlineFilterLineHoldTest();
        var layouts=new ArrayList<String>();var matrices=new ArrayList<SparseMatrix>();var residuals=new ArrayList<double[]>();
        PassiveStepSolver.INSTR_LAYOUT=s->{if(layouts.size()<4)layouts.add(s);};
        SparseNewton.INSTR_SINGULAR=(m,f)->{if(matrices.size()<4){matrices.add(m);residuals.add(f);}};
        try {
            var w=outer.new World();outer.run(w,7,10);
            System.out.println("F8_PROBE holds="+w.held);
            for(int k=0;k<Math.min(1,matrices.size());k++) {
                System.out.println("F8_PROBE layout at the failure:\n"+layouts.get(k));
                dump(matrices.get(k),residuals.get(k));
            }
            // The held island, solved directly.
            var island=w.coordinator.observe().stream().filter(s->s.status().startsWith("HELD")).findFirst().orElseThrow();
            var graph=PassiveNetwork.sizeJunctionHoldups(island.graph(),outer.model);
            var ports=new ArrayList<PassiveNetwork.Reservoir>();
            for(var n:graph.reservoirs())ports.add(new PassiveNetwork.Reservoir(n.id(),n.elevation(),n.state(),n.kind()==PassiveNetwork.NodeKind.RESERVOIR?PassiveNetwork.NodeKind.PORT:n.kind(),n.inventory()));
            layouts.clear();matrices.clear();
            try{var r=new PassiveStepSolver(outer.model).solveRate(new PassiveNetwork(ports,graph.pipes()),()->{},PassiveStepSolver.Acceptance.FULL);System.out.println("F8_PROBE rate solve converged: flows="+Arrays.toString(r.massFlows())+" modes="+r.modes());}
            catch(RuntimeException e){System.out.println("F8_PROBE rate solve: "+e.getMessage());}
            for(double dt:new double[]{.05,.0125,1e-3}) {
                layouts.clear();
                try{var r=new PassiveStepSolver(outer.model).solve(graph,dt,()->{},PassiveStepSolver.Acceptance.FULL);System.out.println("F8_PROBE BE step dt="+dt+" converged: flows="+Arrays.toString(r.massFlows())+" modes="+r.modes());}
                catch(RuntimeException e){System.out.println("F8_PROBE BE step dt="+dt+": "+e.getMessage()+(layouts.isEmpty()?"":"\n"+layouts.getFirst()));}
            }
            try{var r=new PassiveIntervalSolver(outer.model).solve(graph,.05,PassiveIntervalSolver.Settings.defaults(),()->{});System.out.println("F8_PROBE interval 1 tick converged: modes="+r.endpointModes()+" accepted="+r.acceptedSubsteps()+" rejected="+r.rejectedSubsteps()+" reasons="+r.rejectionReasons());}
            catch(RuntimeException e){System.out.println("F8_PROBE interval 1 tick: "+e.getMessage());}
        } finally {PassiveStepSolver.INSTR_LAYOUT=null;SparseNewton.INSTR_SINGULAR=null;}
    }
    static void dump(SparseMatrix m,double[] f) {
        int n=m.size();var a=new DMatrixRMaj(n,n);
        for(int c=0;c<n;c++)for(int e=m.columnStart(c);e<m.columnEnd(c);e++)a.set(m.rowAt(e),c,m.valueAt(e));
        var svd=DecompositionFactory_DDRM.svd(n,n,true,true,false);
        if(!svd.decompose(a.copy()))throw new AssertionError("svd failed");
        double[] s=svd.getSingularValues();var u=svd.getU(null,false);var v=svd.getV(null,false);
        Integer[] order=new Integer[n];for(int i=0;i<n;i++)order[i]=i;Arrays.sort(order,Comparator.comparingDouble(i->s[i]));
        System.out.println("F8_PROBE n="+n+" nnz="+m.nonzeroCount()+" largest sigma="+s[order[n-1]]+" smallest five="+s[order[0]]+", "+s[order[1]]+", "+s[order[2]]+", "+s[order[3]]+", "+s[order[4]]);
        for(int k=0;k<3;k++) {
            int j=order[k];var right=new StringBuilder();var left=new StringBuilder();
            for(int i=0;i<n;i++){if(Math.abs(v.get(i,j))>1e-3)right.append(" col").append(i).append('=').append(String.format(Locale.ROOT,"%.4f",v.get(i,j)));if(Math.abs(u.get(i,j))>1e-3)left.append(" row").append(i).append('=').append(String.format(Locale.ROOT,"%.4f",u.get(i,j)));}
            System.out.println("F8_PROBE sigma="+s[j]+"\n  right (unknowns):"+right+"\n  left (rows):"+left);
        }
        var rows=new StringBuilder();for(int i=0;i<n;i++)if(Math.abs(f[i])>1e-12)rows.append(" f").append(i).append('=').append(f[i]);
        System.out.println("F8_PROBE residual rows above 1e-12:"+rows);
        // The columns and rows of the dependent block, printed whole.
        for(int c=0;c<n;c++){var col=new StringBuilder();for(int e=m.columnStart(c);e<m.columnEnd(c);e++)col.append(' ').append(m.rowAt(e)).append(':').append(String.format(Locale.ROOT,"%.4g",m.valueAt(e)));System.out.println("F8_PROBE col"+c+col);}
    }
}
