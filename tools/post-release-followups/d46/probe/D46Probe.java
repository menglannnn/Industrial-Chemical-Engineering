package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.ConservativeTransport;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * t52 probe (lane exports only, never tracked): what decision D46's unbooked junction buffers create, per seed.
 * A buffer keeps its mass m_J (the mass pin), so the vessels gain exactly what the boundaries book in total mass; per
 * component and for energy the world gains sum over buffer lives of (mint - end). Measured as minus the sum of every
 * committed change of the junction buffers (a solve's commit and a STEADY replay), per component, signed. The re-mint
 * part is what an installed junction holds beyond the last buffer the same junction id held (a carried junction adds
 * nothing). One accumulator per thread: the hold-fuzz harness runs each seed on one pool thread, synchronously.
 */
public final class D46Probe {
    private D46Probe(){}
    private static final class Acc {
        double[] w;final Map<Long,double[]> last=new HashMap<>();
        double[] created,remint;double solidCreatedKg;int installs,fresh,carried,reminted;double freshKg,remintedKg,turnoverKg,largestFreshKg;
    }
    private static final boolean TRACE=Boolean.getBoolean("d46.trace");
    private static final ThreadLocal<Acc> ACC=ThreadLocal.withInitial(Acc::new);
    public static void reset(){ACC.set(new Acc());}
    /** Moles, then the energy field (m_J h for a junction) as the last entry. */
    private static double[] vec(PassiveNetwork.Reservoir n){var m=n.inventory().moles();var v=Arrays.copyOf(m,m.length+1);v[m.length]=n.inventory().internalEnergy();return v;}
    private static Acc acc(FluidThermodynamics model){var a=ACC.get();if(a.w==null){a.w=model.molecularWeights();a.created=new double[a.w.length+1];a.remint=new double[a.w.length+1];}return a;}
    private static double kg(Acc a,double[] v){double s=0;for(int c=0;c<a.w.length;c++)s+=v[c]*a.w[c];return s;}
    /** A topology event installed {@code graph}: its junctions, minted, re-minted or carried. */
    public static void installed(PassiveNetwork graph,FluidThermodynamics model) {
        var a=acc(model);
        for(var n:graph.reservoirs()) {
            if(!n.junction())continue;var v=vec(n);var old=a.last.put(n.id(),v);a.installs++;
            if(TRACE)System.out.printf(java.util.Locale.ROOT,"D46INSTALL id=%d %s kg=%.4f T=%.1f P=%.0f liq=%.3f vap=%.3f water=%.3f%n",n.id(),old==null?"fresh":Arrays.equals(old,v)?"carried":"remint",kg(a,v),n.state().temperature(),n.state().pressure(),n.state().liquidVolume()/n.state().volume(),n.state().vaporVolume()/n.state().volume(),n.state().waterVolume()/n.state().volume());
            if(old==null){a.fresh++;double m=kg(a,v);a.freshKg+=m;a.largestFreshKg=Math.max(a.largestFreshKg,m);}
            else if(Arrays.equals(old,v))a.carried++;
            else{a.reminted++;a.remintedKg+=kg(a,v);for(int c=0;c<v.length;c++)a.remint[c]+=v[c]-old[c];}
        }
    }
    /** A commit replaced {@code before} by {@code after} (one structure); {@code boundaries} may be null (a replay). */
    public static void committed(PassiveNetwork before,PassiveNetwork after,List<ConservativeTransport.BoundaryTransfer> boundaries,FluidThermodynamics model) {
        var a=acc(model);
        if(before.reservoirs().size()==after.reservoirs().size())for(int i=0;i<after.reservoirs().size();i++) {
            var n=after.reservoirs().get(i);var o=before.reservoirs().get(i);if(!n.junction()||o.id()!=n.id())continue;
            var vb=vec(o);var va=vec(n);if(TRACE){double d=Math.abs(kg(a,va)-kg(a,vb)),w=(va[a.w.length-1]-vb[a.w.length-1])*a.w[a.w.length-1];if(Math.abs(w)>1e-3)System.out.printf(java.util.Locale.ROOT,"D46COMMIT id=%d waterOutKg=%.4f bufferKg=%.4f T=%.1f P=%.0f liq=%.3f vap=%.3f%n",n.id(),-w,kg(a,va),n.state().temperature(),n.state().pressure(),n.state().liquidVolume()/n.state().volume(),n.state().vaporVolume()/n.state().volume());}a.solidCreatedKg-=solidKg(n)-solidKg(o);for(int c=0;c<va.length;c++)a.created[c]-=va[c]-vb[c];a.last.put(n.id(),va);
        }
        if(boundaries!=null)for(var b:boundaries){var m=b.moles();for(int c=0;c<a.w.length;c++)a.turnoverKg+=Math.abs(m[c])*a.w[c];}
    }
    private static double solidKg(PassiveNetwork.Reservoir n){double s=0;for(var p:n.inventory().solids().populations())s+=p.massKg();return s;}
    public static void flush(long seed) {
        var a=ACC.get();
        if(a.w==null){System.out.println("D46SEED seed="+seed+" installs=0");reset();return;}
        int n=a.w.length;double abs=0,net=0,worst=0,remAbs=0;int worstC=-1;
        for(int c=0;c<n;c++){double m=a.created[c]*a.w[c];abs+=Math.abs(m);net+=m;if(Math.abs(m)>Math.abs(worst)){worst=m;worstC=c;}remAbs+=Math.abs(a.remint[c]*a.w[c]);}
        System.out.printf(java.util.Locale.ROOT,"D46SEED seed=%d installs=%d fresh=%d carried=%d reminted=%d freshKg=%.6f largestFreshKg=%.6f remintedKg=%.6f createdAbsKg=%.6f createdNetKg=%.3e solidCreatedKg=%.3e worstKg=%.6f worstC=%d waterKg=%.6f createdMJ=%.6f remintAbsKg=%.6f remintMJ=%.6f turnoverKg=%.3f%n",
                seed,a.installs,a.fresh,a.carried,a.reminted,a.freshKg,a.largestFreshKg,a.remintedKg,abs,net,a.solidCreatedKg,worst,worstC,a.created[n-1]*a.w[n-1],a.created[n]/1e6,remAbs,a.remint[n]/1e6,a.turnoverKg);
        reset();
    }
}
