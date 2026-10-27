package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.diagnostics.SolverDiagnostics;
import com.wormzjl.createcheme.science.fluid.network.*;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * t42 probe (lane exports only): one or more of MixedGasJunctionTransientTest's twelve cases at one interval, with every
 * SolverDiagnostics counter per interval and in all, plus each interval's end state (tank and junction P, T, edge flows
 * and caps as the step solver's model states them). -Dt42.cases=0.02:4:true,... (default all twelve), -Dt42.interval=5.
 */
class T42MixedGasProbeTest {
    private static final String[] KEYS={"newtonSolves","newtonIterations","newtonBacktracks","residualEvaluations","residualEvaluationsInJacobian",
            "jacobianBuilds","jacobianBlockBuilds","activeSetPasses","stepAttempts","stepAttemptsAccepted","newtonRefreshesStalled","newtonRefreshesAged",
            "newtonRefreshesFailed","newtonIterationsFresh","newtonIterationsStale","stateCalls"};
    @Test void probe() {
        var model=FluidTestSupport.networkModel();var mw=model.molecularWeights();
        double interval=Double.parseDouble(System.getProperty("t42.interval","5"));
        var cases=new ArrayList<String>();
        String only=System.getProperty("t42.cases","");
        for(double bore:new double[]{.05,.02})for(int ports=4;ports<=6;ports++)for(boolean unequal:new boolean[]{false,true}){String key=bore+":"+ports+":"+unequal;if(only.isEmpty()||Arrays.asList(only.split(",")).contains(key))cases.add(key);}
        var total=new long[KEYS.length];
        for(String key:cases) {
            String[] k=key.split(":");double bore=Double.parseDouble(k[0]);int ports=Integer.parseInt(k[1]);boolean unequal=Boolean.parseBoolean(k[2]);
            var graph=MixedGasJunctionTransientTest.island(model,bore,ports,unequal);
            int tank=-1;for(int i=0;i<graph.reservoirs().size();i++)if(graph.reservoirs().get(i).id()==1)tank=i;
            for(int i=0;i<graph.reservoirs().size();i++){var r=graph.reservoirs().get(i);System.out.printf(Locale.ROOT,"T42NODE case=%s node=%d id=%d kind=%s P=%.1f T=%.3f m=%.6g held=%.6g vlim=%.4g%n",key,i,r.id(),r.kind(),r.state().pressure(),r.state().temperature(),r.state().mass(),r.inventory().moles().length>0?Arrays.stream(r.inventory().moles()).sum():0,model.velocityLimit(r.state()));}
            for(int e=0;e<graph.pipes().size();e++){var p=graph.pipes().get(e);System.out.printf(Locale.ROOT,"T42PIPE case=%s edge=%d %d->%d area=%.4g sections=%d%n",key,e,p.first(),p.second(),p.minimumArea(),p.sections().size());}
            double[] feed=new double[mw.length];feed[0]=.5;
            var feedState=model.flashTP(350,150000,feed,()->{});double feedEnergy=feedState.enthalpy();
            var solver=new PassiveIntervalSolver(model);PassiveIntervalSolver.Result committed=null;
            var schedule=MixedGasJunctionTransientTest.schedule(interval);
            var sum=new long[KEYS.length];
            for(int step=0;step<schedule.size();step++){
                List<ScheduledTransfer> transfers=schedule.get(step)[1]>0?List.of(new ScheduledTransfer.Injection(900,tank,feed,feedEnergy)):List.of();
                graph=new PassiveNetwork(graph.reservoirs(),graph.pipes(),transfers);
                solver.replayStart(graph,committed);
                boolean enabled=SolverDiagnostics.ENABLED;SolverDiagnostics.reset();SolverDiagnostics.ENABLED=true;
                PassiveIntervalSolver.Result result;
                try{result=solver.solve(graph,schedule.get(step)[0],PassiveIntervalSolver.Settings.defaults(),()->{});}
                finally{SolverDiagnostics.ENABLED=enabled;}
                var sample=SolverDiagnostics.sample();var sb=new StringBuilder();
                for(int i=0;i<KEYS.length;i++){long v=sample.value(KEYS[i]);sum[i]+=v;total[i]+=v;sb.append(' ').append(KEYS[i]).append('=').append(v);}
                SolverDiagnostics.reset();
                committed=result;graph=result.graph();
                var flows=result.averageMassFlows();var sbs=new StringBuilder();
                for(int i=0;i<graph.reservoirs().size();i++){var s=graph.reservoirs().get(i).state();sbs.append(String.format(Locale.ROOT," n%d[P=%.1f T=%.2f vlim=%.1f]",i,s.pressure(),s.temperature(),model.velocityLimit(s)));}
                System.out.printf(Locale.ROOT,"T42INT case=%s interval=%d dt=%.2f%s | flows=%s |%s%n",key,step,schedule.get(step)[0],sb,Arrays.toString(flows),sbs);
            }
            var sb=new StringBuilder();for(int i=0;i<KEYS.length;i++)sb.append(' ').append(KEYS[i]).append('=').append(sum[i]);
            System.out.println("T42CASE case="+key+sb);
        }
        var sb=new StringBuilder();for(int i=0;i<KEYS.length;i++)sb.append(' ').append(KEYS[i]).append('=').append(total[i]);
        System.out.println("T42TOTAL cases="+cases.size()+sb);
    }
}
