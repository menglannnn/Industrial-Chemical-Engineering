package com.wormzjl.createcheme.science.fluid.thermo;

import java.util.Objects;

/**
 * A node's phases the fluid network holds on with a typed reason, never a fabricated answer: a phase set the network
 * cannot carry ({@link FluidThermodynamics.UnsupportedPhases}) or that the engine could not converge inside the declared
 * critical band ({@link FluidThermodynamics.CriticalBandHold}). These are capability limits, where the model cannot
 * represent a state; leaving a data range is not one (owner rule of 2026-10-16: properties continue past their data and
 * a notice reports it). A hold is an {@link IllegalArgumentException}, so a Newton trial, a substep or an interval that
 * meets one is refused like any unevaluable state (owner decision D21, option C; the adaptation plan's A3 and B3): a
 * caller that knows the node a refusal belongs to attaches it with {@link #at}, a seed that meets one keeps its guess, a
 * rejected substep counts it under its own {@link #reasonKey}, a failed interval carries it as its cause, and an island
 * whose fresh-solver retry meets the same hold ({@link #sameAs}) waits for a change to its network instead of retrying.
 * No split is fabricated and no phase set is answered with fewer phases.
 */
public abstract sealed class ThermoHold extends IllegalArgumentException
        permits FluidThermodynamics.CriticalBandHold,FluidThermodynamics.UnsupportedPhases {
    /** The node id of a hold whose node is not known. */
    public static final long NO_NODE=Long.MIN_VALUE;
    /** The prefix of every rejection-reason key a hold produces ({@link #reasonKey}). */
    public static final String REASON_PREFIX="thermo-domain: ";
    final String packageId,detail;
    final double temperature,pressure;
    private final long node;
    ThermoHold(String message,String packageId,double temperature,double pressure,String detail,long node) {
        super(message+where(node));
        this.packageId=Objects.requireNonNull(packageId);this.temperature=temperature;this.pressure=pressure;this.detail=detail;this.node=node;
    }
    /** The network node whose state was refused, or {@link #NO_NODE}. */
    public long node(){return node;}
    /** The same hold at a known network node; a hold that already names a node keeps it. */
    public abstract ThermoHold at(long node);
    /** The property package that refused the state. */
    public String packageId(){return packageId;}
    /** The state's temperature (K), NaN where the request had none (a crystal answer the engine refused). */
    public double temperature(){return temperature;}
    /** The state's pressure (Pa), NaN where the request had none. */
    public double pressure(){return pressure;}
    /**
     * The rejection-reason key of this hold, {@link #REASON_PREFIX} first: one key per phase set, so a solve that meets
     * the same hold a thousand times counts it under one key; the numbers are in {@link #getMessage()}.
     */
    public abstract String reasonKey();
    /** Whether {@code other} is the same hold at the same node: a retry reproduced this failure. */
    public abstract boolean sameAs(ThermoHold other);
    /** The hold's own words, without its state: what the network cannot carry or the engine could not converge. */
    abstract String what();
    /** What happened, for an island's status line, {@code where} the node's label (empty when unknown). */
    public String status(String where) {
        String state=Double.isFinite(temperature)&&Double.isFinite(pressure)
                ?String.format(java.util.Locale.ROOT," at %.2f K, %.0f Pa",temperature,pressure):"";
        return what()+state+(where==null||where.isEmpty()?"":" in "+where)+" (package "+packageId+")";
    }
    /** The suffix of a message naming {@code node}, empty for {@link #NO_NODE}. */
    static String where(long node){return node==NO_NODE?"":" at node "+node;}
}
