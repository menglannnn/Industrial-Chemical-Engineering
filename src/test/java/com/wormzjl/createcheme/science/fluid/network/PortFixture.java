package com.wormzjl.createcheme.science.fluid.network;

import com.wormzjl.createcheme.fluid.support.FluidTestSupport;
import com.wormzjl.createcheme.science.fluid.thermo.FluidThermodynamics;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The network model, its supplies and vessels, and the ledger of what a graph's tanks and junctions own: shared by
 * the port, level-head, inlet-phase and compressor tests, which check component and energy ledgers slice by slice.
 */
abstract class PortFixture {
    static final Runnable NOOP=()->{};
    final FluidThermodynamics model=FluidTestSupport.networkModel();
    final double[] mw=model.molecularWeights();
    final int water=index("Water"),nitrogen=index("Nitrogen");

    int index(String component){int i=model.components().indexOf(component);assertTrue(i>=0,component);return i;}
    /** A 1 m3 nitrogen charge at 298.15 K. */
    FluidThermodynamics.State nitrogenAt(double pressure){return model.initialNitrogenCharge(1,298.15,pressure,NOOP);}
    /** One mole of water flashed at 298.15 K: a generator's supply. */
    FluidThermodynamics.State waterSupply(double pressure){var n=new double[mw.length];n[water]=1;return model.flashTP(298.15,pressure,n,NOOP);}
    /** A 1 m3 vessel at 298.15 K holding {@code waterVolume} m3 of water under nitrogen at {@code pressure}. */
    FluidThermodynamics.State waterUnderNitrogen(double waterVolume,double pressure) {
        double t=298.15;double[] n=new double[mw.length];
        n[water]=waterVolume*997/mw[water];n[nitrogen]=pressure*(1-waterVolume)/(FluidThermodynamics.R*t);
        var unit=model.flashTP(t,pressure,n,NOOP);for(int c=0;c<n.length;c++)n[c]/=unit.volume();
        return model.flashTP(t,pressure,n,NOOP);
    }
    double mass(double[] moles){double m=0;for(int c=0;c<moles.length;c++)m+=moles[c]*mw[c];return m;}
    /** The moles held by the graph's tanks and junctions. */
    double[] owned(PassiveNetwork graph) {
        var totals=new double[mw.length];
        for(var node:graph.reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction()){var n=node.inventory().moles();for(int c=0;c<n.length;c++)totals[c]+=n[c];}
        return totals;
    }
    /** Their internal energy plus their potential energy at the node elevations. */
    double ownedEnergy(PassiveNetwork graph) {
        double energy=0;
        for(var node:graph.reservoirs())if(node.kind()==PassiveNetwork.NodeKind.RESERVOIR||node.junction())
            energy+=node.inventory().internalEnergy()+mass(node.inventory().moles())*PassiveStepSolver.GRAVITY*node.elevation();
        return energy;
    }
}
