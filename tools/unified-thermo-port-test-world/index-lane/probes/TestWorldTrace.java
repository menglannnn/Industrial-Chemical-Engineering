package com.wormzjl.icheme.science.fluid.network;

import com.wormzjl.icheme.science.fluid.thermo.FluidThermodynamics;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * F1/F2 bisect traces for the test world rig (tools/unified-thermo-port-test-world, 2026-10-16; not a gate). Called by
 * the rig's "trace" op on an island's committed graph; reads the engine through its own entry points and changes
 * nothing. Version-sensitive calls go through reflection, so one file compiles from main 0.8.0 to the port's head.
 */
public final class TestWorldTrace {
    private TestWorldTrace() {}
    static final double ATM=101325;

    static String fmt(double[] v){var b=new StringBuilder("[");for(int i=0;i<v.length;i++){if(i>0)b.append(' ');b.append(String.format(Locale.ROOT,"%.4g",v[i]));}return b.append(']').toString();}
    static double sum(double[] v){double s=0;for(double x:v)s+=x;return s;}
    static double moles(FluidThermodynamics.State s){return sum(s.liquidView())+sum(s.vaporView())+s.waterLiquid()+s.waterVapor();}
    static String f(String format,Object...args){return String.format(Locale.ROOT,format,args);}
    static Throwable root(Throwable t){while(t.getCause()!=null&&t.getCause()!=t)t=t.getCause();return t;}
    static Object call(Object target,String name,Object...args) throws Exception {
        for(Class<?> c=target.getClass();c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){m.setAccessible(true);return m.invoke(target,args);}
        throw new NoSuchMethodException(name+"/"+args.length);
    }
    static Object callStatic(Class<?> c,String name,Object...args) throws Exception {
        for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length&&java.lang.reflect.Modifier.isStatic(m.getModifiers())){m.setAccessible(true);return m.invoke(null,args);}
        throw new NoSuchMethodException(name+"/"+args.length);
    }
    static String standIn(FluidThermodynamics model,FluidThermodynamics.State s) {
        try{return String.valueOf(call(model,"condensedShareOnly",s));}catch(Exception e){return "n/a";}
    }
    static String state(FluidThermodynamics model,FluidThermodynamics.State s) {
        return f("T=%.2f K P=%.0f Pa liquid %s (%.4g m3) vapour %s (%.4g m3) stand-in=%s h=%.1f J",s.temperature(),s.pressure(),fmt(s.liquidView()),s.liquidVolume(),fmt(s.vaporView()),s.vaporVolume(),standIn(model,s),s.enthalpy());
    }
    /** The phase check on {@code s} as a vessel's (no own split given: the TP-based seed, which refuses nothing). */
    static FluidThermodynamics.State phaseCheck(FluidThermodynamics model,FluidThermodynamics.State s) throws Exception {
        Runnable none=()->{};
        for(Method m:FluidThermodynamics.class.getDeclaredMethods()) {
            if(!m.getName().equals("phaseCheck"))continue;m.setAccessible(true);
            // 67e5f3e7 (D38 C) on: the solve's start state "from" after "junction"; null reads no vanishing phase.
            if(m.getParameterCount()==8)return (FluidThermodynamics.State)m.invoke(model,s,false,null,new int[1],0,null,null,none);
            if(m.getParameterCount()==7)return (FluidThermodynamics.State)m.invoke(model,s,false,new int[1],0,null,null,none);
            if(m.getParameterCount()==6)return (FluidThermodynamics.State)m.invoke(model,s,false,0,null,null,none);
        }
        throw new NoSuchMethodException("phaseCheck");
    }
    /**
     * A step solve or the port-graph rate solve with full acceptance, on either API: from 65036f2b (D37 S2, the approximate
     * fallback and PassiveStepSolver.Acceptance deleted) {@code solve(graph, dt, checkpoint)} and {@code portRate(graph,
     * checkpoint)}; before it the same with a trailing {@code Acceptance.FULL}. By reflection, so one file still compiles from
     * main 0.8.0 to the port's head.
     */
    static PassiveStepSolver.Result fullSolve(PassiveStepSolver solver,String name,Object...args) throws Exception {
        for(Method m:PassiveStepSolver.class.getDeclaredMethods()) {
            if(!m.getName().equals(name))continue;
            var types=m.getParameterTypes();
            if(types.length==args.length&&accepts(types,args)){m.setAccessible(true);return (PassiveStepSolver.Result)m.invoke(solver,args);}
            if(types.length==args.length+1&&types[args.length].isEnum()&&accepts(types,args)) {
                var all=Arrays.copyOf(args,args.length+1);
                for(Object constant:types[args.length].getEnumConstants())if(((Enum<?>)constant).name().equals("FULL"))all[args.length]=constant;
                m.setAccessible(true);return (PassiveStepSolver.Result)m.invoke(solver,all);
            }
        }
        throw new NoSuchMethodException(name+"/"+args.length);
    }
    private static boolean accepts(Class<?>[] types,Object[] args) {
        for(int i=0;i<args.length;i++) {
            Class<?> t=types[i]==double.class?Double.class:types[i]==int.class?Integer.class:types[i]==boolean.class?Boolean.class:types[i]==long.class?Long.class:types[i];
            if(args[i]!=null&&!t.isInstance(args[i]))return false;
        }
        return true;
    }

    /**
     * F1: the tank's committed state, the refresh the next interval starts with, the arriving stream, the phase check's
     * verdict on the tank's vapour cooled at 1 atm, steps from the refreshed state, the energy a vapour-only layout must
     * absorb, the network's PH answer with a liquid slot, and the interval as the island runs it.
     */
    public static void f1(FluidThermodynamics model,PassiveNetwork g,PassiveIntervalSolver.Result committed,int tank,Consumer<String> out) {
        try {
            var node=g.reservoirs().get(tank);var st=node.state();
            out.accept("F1 committed tank: "+state(model,st)+f(" solids %.6f kg",node.inventory().solids().massKg()));
            // The arriving stream: every connection whose committed flow enters the tank, read on its donor's state.
            double arrivingMolesPerSecond=0,arrivingEnthalpy=Double.NaN;int n=st.liquidView().length;
            if(committed!=null)for(int e=0;e<g.pipes().size();e++) {
                var p=g.pipes().get(e);double q=committed.averageMassFlows()[e];
                int donor=p.second()==tank&&q>0?p.first():p.first()==tank&&q<0?p.second():-1;
                if(donor<0)continue;
                var d=g.reservoirs().get(donor).state();double m=moles(d),mass=d.mass();
                arrivingMolesPerSecond+=Math.abs(q)/(mass/m);arrivingEnthalpy=d.enthalpy()/m;
                out.accept(f("F1 arrival through edge %d from node %d (%s): %.4f kg/s = %.3f mol/s, donor %s, h=%.1f J/mol",e,donor,g.reservoirs().get(donor).kind(),Math.abs(q),Math.abs(q)/(mass/m),state(model,d),d.enthalpy()/m));
            }
            // The refresh at the next interval's start (InventoryEquilibrium.refresh, D23: the one place crystal mass moves).
            Object refreshed=InventoryEquilibrium.refresh(g,model,()->{});
            PassiveNetwork rg=refreshed instanceof PassiveNetwork pn?pn:(PassiveNetwork)call(refreshed,"graph");
            String converted=refreshed instanceof PassiveNetwork?"-":String.valueOf(call(refreshed,"converted"));
            var rn=rg.reservoirs().get(tank);var rs=rn.state();
            out.accept("F1 refreshed tank (converted="+converted+"): "+state(model,rs));
            for(var p:rn.inventory().solids().populations())out.accept(f("F1   solid %s %.1f um %.6f kg",p.material().id(),p.size().diameterMetres()*1e6,p.massKg()));
            // The vapour's own composition at 1 atm.
            double[] y=rs.vaporView().clone();double ny=sum(y);if(!(ny>0)){y=st.vaporView().clone();ny=sum(y);}
            for(int i=0;i<n;i++)y[i]/=ny;
            double[] zero=new double[n];
            // The phase check on that vapour, cooled at 1 atm: does the TP answer's liquid (a stand-in below CO2's triple point) get a slot?
            for(double t:new double[]{194,185,180,150,120,91}) {
                try {
                    var vapour=model.adoptingState(t,ATM,zero.clone(),y.clone(),0,0,ATM,null);
                    var checked=phaseCheck(model,vapour);
                    out.accept(f("F1 phaseCheck vapour-only %.1f K, 1 atm: %s (liquid %.4g of 1 mol)",t,checked==vapour?"kept, nothing proposed":checked.liquidVolume()>0?"LIQUID PROPOSED":"changed, no liquid",sum(checked.liquidView())));
                } catch(Exception e){out.accept(f("F1 phaseCheck %.1f K: %s",t,root(e)));}
            }
            // The energy a vapour-only layout has to absorb (1 mol basis at 1 atm).
            double hv=Double.NaN;
            for(double t:new double[]{rs.temperature(),185,150,120,90.01}) {
                try{var v=model.adoptingState(t,ATM,zero.clone(),y.clone(),0,0,ATM,null);double h=v.enthalpy();if(t==rs.temperature())hv=h;
                    out.accept(f("F1 vapour-only h(%.2f K, 1 atm)=%.1f J/mol",t,h));
                    if(t==90.01&&Double.isFinite(arrivingEnthalpy)) {
                        double nv=sum(rs.vaporView());double toFloor=nv*(hv-h)/(h-arrivingEnthalpy);
                        out.accept(f("F1 arrival h=%.1f J/mol: %.1f J/mol below the vapour at %.2f K; a vapour-only node of %.4g mol reaches 90 K after %.4g mol arrive (%.4g s at %.3f mol/s)",
                                arrivingEnthalpy,hv-arrivingEnthalpy,rs.temperature(),nv,toFloor,toFloor/arrivingMolesPerSecond,arrivingMolesPerSecond));
                    }
                }catch(Exception e){out.accept(f("F1 vapour-only %.2f K: %s",t,root(e)));}
            }
            // The network's PH answer at 1 atm (the frost carried by a stand-in liquid), for the vapour plus what arrives.
            if(Double.isFinite(arrivingEnthalpy))for(double dt:new double[]{.01,.05,1,5}) {
                double[] totals=new double[n+1];double h=rs.enthalpy();
                double[] lv=rs.liquidView(),vv=rs.vaporView();for(int i=0;i<n;i++)totals[i]=lv[i]+vv[i];
                double arrived=arrivingMolesPerSecond*dt;for(int i=0;i<n;i++)totals[i]+=arrived*y[i];h+=arrived*arrivingEnthalpy;
                try {
                    var ph=(FluidThermodynamics.State)call(model,"equilibriumPH",rs.temperature(),ATM,h,totals,(Runnable)()->{});
                    out.accept(f("F1 PH answer after %.2f s of arrival (%.4g mol): T=%.2f K liquid %.4g vapour %.4g mol stand-in=%s",dt,sum(totals),ph.temperature(),sum(ph.liquidView()),sum(ph.vaporView()),standIn(model,ph)));
                } catch(Exception e){out.accept(f("F1 PH answer after %.2f s: %s",dt,root(e)));}
            }
            // Steps from the committed state before its refresh (the stand-in still in the liquid slot), for comparison.
            for(double dt:new double[]{5e-2,1}) {
                try {
                    var r=fullSolve(new PassiveStepSolver(model),"solve",g,dt,(Runnable)()->{});
                    out.accept(f("F1 step %.0e s from the committed state, not refreshed: tank %s",dt,state(model,r.states().get(tank))));
                } catch(Exception e){out.accept(f("F1 step %.0e s from the committed state, not refreshed: %s: %s",dt,root(e).getClass().getSimpleName(),root(e).getMessage()));}
            }
            // Steps from the refreshed state, as the interval's controller takes them.
            for(double dt:new double[]{1e-4,1e-3,1e-2,5e-2,1}) {
                try {
                    var r=fullSolve(new PassiveStepSolver(model),"solve",rg,dt,(Runnable)()->{});
                    out.accept(f("F1 step %.0e s from the refreshed state: tank %s",dt,state(model,r.states().get(tank))));
                } catch(Exception e){out.accept(f("F1 step %.0e s from the refreshed state: %s: %s",dt,root(e).getClass().getSimpleName(),root(e).getMessage()));}
            }
            // The interval as the island runs it (replayed from the committed interval).
            try {
                var solver=new PassiveIntervalSolver(model);solver.replayStart(g,committed);
                var r=solver.solve(g,5.0,PassiveIntervalSolver.Settings.defaults(),()->{},1.0);
                out.accept(f("F1 interval 5 s: committed, tank %s; substeps %d/%d; reasons %s",state(model,r.graph().reservoirs().get(tank).state()),r.acceptedSubsteps(),r.rejectedSubsteps(),r.rejectionReasons()));
            } catch(Exception e){out.accept(f("F1 interval 5 s: %s: %s",root(e).getClass().getSimpleName(),root(e).getMessage()));}
        } catch(Exception e){out.accept("F1 trace failed: "+root(e));}
    }

    /**
     * H4 (2026-10-18): a taut vessel's crystal test at its committed state (VesselCrystals.settle, then the engine's UV call
     * with the inputs settle forms, warm-started and cold), the UV search's steps where the M10 hook is patched in
     * (tools/unified-thermo-port-m5/trace/patches/instr-m10-uv-search-hook-on-00ee8385.patch), and V(P) of the vessel's
     * totals by the engine's TP answer, the crystals competing, at the failing temperature and at the vessel's own.
     */
    public static void h4(FluidThermodynamics model,PassiveNetwork g,int tank,Consumer<String> out) {
        try {
            var node=g.reservoirs().get(tank);var inv=node.inventory();var st=node.state();
            out.accept(f("H4 tank T=%.4f K P=%.1f Pa U=%.10g J V=%.8g m3 moles=%s liquid %.6g water %.6g vapour %.6g m3",st.temperature(),st.pressure(),inv.internalEnergy(),inv.volume(),fmt(inv.moles()),st.liquidVolume(),st.waterVolume(),st.vaporVolume()));
            for(var p:inv.solids().crystals())out.accept(f("H4   crystal %s %.6f kg",p.material().id(),p.massKg()));
            var crystals=model.crystalSolids();var all=crystals.all();double[] held=new double[all.size()];
            for(var p:inv.solids().crystals())held[all.indexOf(crystals.of(p.material()))]+=p.massKg();
            double[] totals=inv.moles();for(int k=0;k<all.size();k++)totals[all.get(k).component()]+=all.get(k).moles(held[k]);
            double trueEnergy=inv.internalEnergy();for(int k=0;k<all.size();k++)trueEnergy+=held[k]*all.get(k).material().crystal().energyOffset();
            var inertMoments=inv.solids().withoutCrystals().moments();
            double crystalEnergy=trueEnergy-inertMoments.internalEnergy(st.temperature()),volume=inv.volume()-inertMoments.volume();
            boolean competing=model.crystalsCompete(st.temperature(),totals);
            out.accept(f("H4 settle's inputs: totals %s, crystal energy %.10g J, volume %.10g m3, competing %s",fmt(totals),crystalEnergy,volume,competing));
            int[] left={Integer.getInteger("m10.lines",400)};double[] failing={Double.NaN},target={Double.NaN};
            Consumer<Object[]> hook=a->{
                if(a.length>0&&"outer".equals(a[0])&&a[3]!=null)failing[0]=(Double)a[1];
                if(a.length>0&&"outer".equals(a[0]))target[0]=(Double)a[5];
                if(left[0]--<=0)return;
                if("inner".equals(a[0])) {
                    var r=(com.wormzjl.icheme.science.thermo.phase.EquilibriumResult)a[4];
                    out.accept(f("H4 UV inner #%d T=%.6f P=%.2f Pa target V=%.8g: %s %s V=%s phases %s bracket [%s, %s] stored slope %s",(Integer)a[8],(Double)a[1],(Double)a[2],(Double)a[3],r.status(),r.classification(),r.converged()?f("%.8g",r.volume()):"-",phases(r),
                            Double.isNaN((Double)a[5])?"-":f("%.2f",Math.exp((Double)a[5])),Double.isNaN((Double)a[6])?"-":f("%.2f",Math.exp((Double)a[6])),a.length>9?String.valueOf(a[9]):"-"));
                } else out.accept("H4 UV "+Arrays.toString(a));
            };
            boolean hooked=false;
            try{Class.forName("com.wormzjl.icheme.science.thermo.phase.FluidTpEquilibrium").getField("M10_UV").set(null,hook);hooked=true;}catch(ReflectiveOperationException e){out.accept("H4 no M10 hook in this build");}
            try {
                try{Object settled=callStatic(VesselCrystals.class,"settle",model,inv,st,true,(Runnable)()->{});out.accept("H4 settle (closed): "+(settled==null?"null":"answered"));}
                catch(Exception e){out.accept("H4 settle threw: "+root(e));}
                // Warm-started at the tank's state, cold, and (warm=2) at the liquid-side point the nested search found
                // (-Dh4.hintT, -Dh4.hintP): whether a state of the model meets the target there.
                double hintT=Double.parseDouble(System.getProperty("h4.hintT","124.71992")),hintP=Double.parseDouble(System.getProperty("h4.hintP","330000"));
                for(int warm=2;warm>=0;warm--) {
                    double ht=warm==2?hintT:warm==1?st.temperature():Double.NaN,hp=warm==2?hintP:warm==1?st.pressure():Double.NaN;
                    out.accept("H4 crystalsAtVolume "+(warm==2?f("warm-started at %.5f K, %.0f Pa",ht,hp):warm==1?"warm-started at the tank's T, P":"cold"));
                    try{var a=model.crystalsAtVolume(totals,crystalEnergy,volume,ht,hp,competing,()->{});
                        out.accept(f("H4   answer T=%.4f K P=%.1f Pa crystals %s fluid %s",a.temperature(),a.pressure(),fmt(a.crystalMoles()),a.fluid()==null?"-":state(model,a.fluid())));}
                    catch(RuntimeException e){out.accept("H4   threw: "+e.getMessage());}
                }
            } finally {
                if(hooked)try{Class.forName("com.wormzjl.icheme.science.thermo.phase.FluidTpEquilibrium").getField("M10_UV").set(null,null);}catch(ReflectiveOperationException e){}
            }
            // The free water's energy per mole: the stand-in liquid (below its melting line Region 1 held at its 273.16 K
            // values, WP4) against the ice at the same state.
            for(double t:new double[]{273.16,st.temperature(),124.7}) {
                try {
                    var wl=model.waterLiquid(t,st.pressure());var ice=model.freeWaterIce().properties(t,st.pressure());
                    out.accept(f("H4 water at %.2f K, %.0f Pa: stand-in liquid h=%.2f v=%.5e; ice h=%.2f v=%.5e cp=%.3f; liquid - ice h=%.2f",t,st.pressure(),wl.molarEnthalpy(),wl.molarVolume(),ice.enthalpy(),ice.volume(),ice.heatCapacity(),wl.molarEnthalpy()-ice.enthalpy()));
                } catch(RuntimeException e){out.accept("H4 water at "+t+" K threw "+e.getMessage());}
            }
            // V(P) of the totals by the engine's TP answer (the crystals competing as settle has them), at the failing
            // temperature the UV search reported and at the tank's own, with each answer's energy against the target.
            Object engine=call(model,"phaseEngine");
            double ft=Double.isNaN(failing[0])?Double.parseDouble(System.getProperty("h4.t","124.71836321193986")):failing[0];
            for(double t:new double[]{ft,st.temperature(),127.0})for(int k=0;k<=32;k++) {
                double p=1e5*Math.pow(20,k/32.0);
                try {
                    var r=(com.wormzjl.icheme.science.thermo.phase.EquilibriumResult)call(engine,"tp",t,p,totals,competing);
                    out.accept(f("H4 TP %.4f K %10.1f Pa %s %s V=%s m3 (target %.8g) U-target=%s J phases %s",t,p,r.status(),r.classification(),r.converged()?f("%.8g",r.volume()):"-",volume,
                            r.converged()&&!Double.isNaN(target[0])?f("%.6g",r.internalEnergy()-target[0]):"-",phases(r)));
                } catch(Exception e){out.accept(f("H4 TP %.4f K %.1f Pa threw %s",t,p,root(e)));}
            }
        } catch(Exception e){out.accept("H4 trace failed: "+root(e));}
    }
    static String phases(com.wormzjl.icheme.science.thermo.phase.EquilibriumResult r) {
        if(!r.converged())return String.valueOf(r.detail());
        var b=new StringBuilder();
        for(var ph:r.phases())b.append(ph.kind()).append(ph.crystal()==null?"":"("+ph.crystal()+")").append('=').append(f("%.6g",ph.total())).append(' ');
        return b.toString().trim();
    }

    /**
     * F2: the pump's slice decision on the committed graph and with its transport closures lifted, and the deposition test
     * (SolidEventIntegrator.failed) on the port-graph rate solve with the pump free and with the pump refused for the slice.
     */
    public static void f2(FluidThermodynamics model,PassiveNetwork g,PassiveIntervalSolver.Result committed,int pumpNode,Consumer<String> out) {
        try {
            int pe=-1,se=-1;
            for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);if(p.control() instanceof FlowControl.Mover&&p.first()==pumpNode)pe=e;}
            if(pe<0){out.accept("F2 no mover starts at node "+pumpNode);return;}
            for(int e=0;e<g.pipes().size();e++){var p=g.pipes().get(e);if(e!=pe&&(p.first()==pumpNode||p.second()==pumpNode))se=e;}
            var modes=committed==null?null:committed.endpointModes();
            var pump=g.pipes().get(pe);
            var junction=g.reservoirs().get(pumpNode);var js=junction.state();
            out.accept(f("F2 pump's own node %d (%s): T=%.2f K P=%.0f Pa mass %.5g kg in %.5g m3 (%.4g kg/m3) liquid %.4g m3 water %.4g m3 vapour %.4g m3",pumpNode,junction.kind(),js.temperature(),js.pressure(),js.mass(),js.volume(),js.mass()/js.volume(),js.liquidVolume(),js.waterVolume(),js.vaporVolume()));
            if(se>=0){var sp=g.pipes().get(se);int donor=sp.first()==pumpNode?sp.second():sp.first();var ds=g.reservoirs().get(donor).state();
                double liquidDensity=(ds.liquidVolume()+ds.waterVolume())>0?(ds.mass()-model.vaporMass(ds))/(ds.liquidVolume()+ds.waterVolume()):Double.NaN;
                out.accept(f("F2 suction donor node %d (%s): liquid %.4g m3 water %.4g m3 vapour %.4g m3, condensed density %.4g kg/m3; the pump's target %.5g m3/s of it is %.5g kg/s, %.4g m/s in the suction bore",
                        donor,g.reservoirs().get(donor).kind(),ds.liquidVolume(),ds.waterVolume(),ds.vaporVolume(),liquidDensity,((FlowControl.Mover)pump.control()).targetVolumeFlow(),
                        ((FlowControl.Mover)pump.control()).targetVolumeFlow()*liquidDensity,((FlowControl.Mover)pump.control()).targetVolumeFlow()/sp.maximumArea()));}
            out.accept(f("F2 committed: pump edge %d mode %s; suction edge %d blocked=%d committed q=%s",pe,modes==null?"-":modes.get(pe),se,se<0?-1:g.pipes().get(se).blockedDirections(),
                    committed==null||se<0?"-":f("%.5g kg/s",committed.averageMassFlows()[se])));
            // The junction mint (D54/D55): what the island-wide fixed point gives the pump's node, closures in place and lifted.
            try {
                out.accept(f("F2 pump node unsized=%s",callStatic(PassiveNetwork.class,"unsized",junction)));
            } catch(Exception e){out.accept("F2 unsized: "+root(e));}
            for(boolean lifted:new boolean[]{false,true}) {
                try {
                    var pl=new ArrayList<PassiveNetwork.Pipe>();for(var p:g.pipes())pl.add(lifted?p.withBlockedDirections(0):p);
                    var gg=new PassiveNetwork(g.reservoirs(),pl,g.scheduledTransfers());
                    @SuppressWarnings("unchecked") var starts=(List<FluidThermodynamics.State>)call(new PassiveStepSolver(model),"fixedPointStarts",gg);
                    var s=starts.get(pumpNode);
                    out.accept(f("F2 mint fixed point, closures %s: pump node T=%.2f K P=%.0f Pa %.4g kg/m3 liquid %s vapour %s",lifted?"lifted":"in place",s.temperature(),s.pressure(),s.mass()/s.volume(),fmt(s.liquidView()),fmt(s.vaporView())));
                } catch(Exception e){out.accept("F2 mint fixed point: "+root(e));}
            }
            boolean[] r1=InletPhase.decide(model,g,modes,5.0);
            out.accept(f("F2 slice decision on the committed graph (its closures in place): refused=%s%s",r1[pe],r1[pe]?" / "+InletPhase.reason(model,g,pe,5.0):""));
            var pipes=new ArrayList<PassiveNetwork.Pipe>();for(var p:g.pipes())pipes.add(p.withBlockedDirections(0));
            var g0=new PassiveNetwork(g.reservoirs(),pipes,g.scheduledTransfers());
            boolean[] r2=InletPhase.decide(model,g0,modes,5.0);boolean[] r3=InletPhase.decide(model,g0,null,5.0);
            out.accept(f("F2 slice decision with every transport closure lifted: refused=%s%s; without the committed modes: refused=%s",r2[pe],r2[pe]?" / "+InletPhase.reason(model,g0,pe,5.0):"",r3[pe]));
            Method failed=SolidEventIntegrator.class.getDeclaredMethod("failed",PassiveNetwork.class,List.class,double[].class);failed.setAccessible(true);
            var integrator=new SolidEventIntegrator(model,new PassiveIntervalSolver(model));
            for(boolean refuse:new boolean[]{false,true}) {
                var solver=new PassiveStepSolver(model);if(refuse)solver.refuseInlets(Set.of(pump.id()));
                var rate=fullSolve(solver,"portRate",g0,(Runnable)()->{});
                String label=refuse?"pump refused for the slice (PassiveIntervalSolver.run's t0 rate solve)":"pump free (SolidEventIntegrator.solve's t0 rate solve)";
                if(rate==null){out.accept("F2 rate solve, "+label+": failed (null)");continue;}
                Object t=failed.invoke(integrator,g0,rate.states(),rate.massFlows());
                out.accept(f("F2 rate solve, %s: pump mode %s q=%.5g kg/s, suction q=%.5g kg/s; deposition test: %s",label,rate.modes().get(pe),rate.massFlows()[pe],se<0?Double.NaN:rate.massFlows()[se],
                        t==null?"no closure":((Throwable)t).getMessage()));
            }
        } catch(Exception e){out.accept("F2 trace failed: "+root(e));}
    }
}
