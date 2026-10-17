package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.PassiveIntervalSolver;
import com.wormzjl.createcheme.science.fluid.network.PassiveNetwork;

/**
 * The ledger of owner decision D46 over a transient of one island (no topology edit): the change of its tanks since the
 * start equals the boundary transfers plus the junction term, the change of the junctions' buffers since the start. A
 * buffer is the state of a junction's relaxation row (m_J w as amounts, m_J h as the energy field), not inventory.
 *
 * <p>{@link #add} reads each committed interval and keeps the worst figures: the ledger's closure relative to
 * max(1, initial) as the transient tests always measured it, and the junction term against its bound. The bound is
 * the row's: a buffer's composition is a convex combination of its old one and its inflows', so each component's
 * content stays in [0, m_J] and its change within m_J; its enthalpy stays inside the span of the specific enthalpies
 * of the states it mixes, here read on every committed state of the island.
 */
final class JunctionLedger {
    private final double[] mw;
    private final double[] tanks0,buffers0,external;
    private final double tankEnergy0,bufferEnergy0;
    private double externalEnergy;
    /** The sum of the junctions' m_J at the start. */
    final double holdup;
    private double lowest=Double.POSITIVE_INFINITY,highest=Double.NEGATIVE_INFINITY;
    /** Worst relative closure of the tanks-and-boundaries ledger with the junction term, per component and for energy. */
    double worstMoles,worstEnergy;
    /** Worst component mass of the junction term, |J_c| M_c, over the sum of m_J (at most 1). */
    double worstShare;
    /** Worst |J_E| over its bound, the sum of m_J times the specific enthalpy span seen so far (at most 1). */
    double worstEnergyShare;
    /** Worst relative drift of a junction's buffer mass from its m_J. */
    double worstMassDrift;

    JunctionLedger(PassiveNetwork graph,double[] molecularWeights) {
        mw=molecularWeights.clone();
        tanks0=amounts(graph,false);buffers0=amounts(graph,true);external=new double[mw.length];
        tankEnergy0=energy(graph,false);bufferEnergy0=energy(graph,true);
        double sum=0;for(var node:graph.reservoirs())if(node.junction())sum+=mass(node);
        holdup=sum;span(graph);
    }
    /** Books one committed interval and checks the island it left. */
    void add(PassiveIntervalSolver.Result result) {
        for(var transfer:result.boundaries()){var n=transfer.moles();for(int c=0;c<n.length;c++)external[c]+=n[c];externalEnergy+=transfer.totalEnergyJoule();}
        var graph=result.graph();span(graph);
        var tanks=amounts(graph,false);var buffers=amounts(graph,true);
        for(int c=0;c<mw.length;c++) {
            double term=buffers[c]-buffers0[c];
            worstMoles=Math.max(worstMoles,Math.abs(tanks[c]-tanks0[c]+term-external[c])/Math.max(1,tanks0[c]+buffers0[c]));
            if(holdup>0)worstShare=Math.max(worstShare,Math.abs(term)*mw[c]/holdup);
        }
        double term=energy(graph,true)-bufferEnergy0;
        worstEnergy=Math.max(worstEnergy,Math.abs(energy(graph,false)-tankEnergy0+term-externalEnergy)/Math.max(1,Math.abs(tankEnergy0+bufferEnergy0)));
        double bound=holdup*(highest-lowest);
        if(bound>0)worstEnergyShare=Math.max(worstEnergyShare,Math.abs(term)/bound);
        else if(term!=0)worstEnergyShare=Double.POSITIVE_INFINITY;
        for(int i=0;i<graph.reservoirs().size();i++) {
            var node=graph.reservoirs().get(i);if(!node.junction())continue;
            worstMassDrift=Math.max(worstMassDrift,Math.abs(mass(node)-minted(i))/minted(i));
        }
    }
    private double[] mintedMass;
    private double minted(int node){return mintedMass[node];}
    private void span(PassiveNetwork graph) {
        if(mintedMass==null){mintedMass=new double[graph.reservoirs().size()];for(int i=0;i<mintedMass.length;i++)mintedMass[i]=mass(graph.reservoirs().get(i));}
        for(var node:graph.reservoirs()) {
            if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR&&node.empty())continue;
            var state=node.state();double h=state.enthalpy()/state.mass();
            lowest=Math.min(lowest,h);highest=Math.max(highest,h);
            if(node.junction()){double b=node.inventory().internalEnergy()/mass(node);lowest=Math.min(lowest,b);highest=Math.max(highest,b);}
        }
    }
    private double[] amounts(PassiveNetwork graph,boolean junctions) {
        var totals=new double[mw.length];
        for(var node:graph.reservoirs())if(junctions?node.junction():node.kind()==PassiveNetwork.NodeKind.RESERVOIR){var n=node.inventory().moles();for(int c=0;c<n.length;c++)totals[c]+=n[c];}
        return totals;
    }
    /** The energy field (a buffer's is m h, a tank's its internal energy; no potential energy, owner decision D37 S1). */
    private double energy(PassiveNetwork graph,boolean junctions) {
        double energy=0;
        for(var node:graph.reservoirs())if(junctions?node.junction():node.kind()==PassiveNetwork.NodeKind.RESERVOIR)
            energy+=node.inventory().internalEnergy();
        return energy;
    }
    private double mass(PassiveNetwork.Reservoir node){double m=node.inventory().solids().massKg();var n=node.inventory().moles();for(int c=0;c<n.length;c++)m+=mw[c]*n[c];return m;}
}
