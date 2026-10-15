package com.wormzjl.createcheme.science.fluid.thermo;

/**
 * A node's state the fluid network holds on with a typed reason from its thermodynamics, never a fabricated answer: a
 * state outside the property domain ({@link ThermoDomainViolation}) or a phase set the network cannot carry or the
 * engine could not converge ({@link FluidThermodynamics.PhaseHold}). Both are {@link IllegalArgumentException}s, so a
 * Newton trial, a substep or an interval that meets one is refused like any unevaluable state. The network treats the two
 * kinds alike (owner decision D21, option C; the adaptation plan's A3 and B3): a caller that knows the node a refusal
 * belongs to attaches it with {@link #at}, a seed that meets one keeps its guess, a rejected substep counts it under its
 * own {@link #reasonKey}, a failed interval carries it as its cause, and an island whose fresh-solver retry meets the
 * same hold ({@link #sameAs}) waits for a change to its network instead of retrying.
 */
public abstract sealed class ThermoHold extends IllegalArgumentException permits ThermoDomainViolation,FluidThermodynamics.PhaseHold {
    /** The node id of a hold whose node is not known. */
    public static final long NO_NODE=Long.MIN_VALUE;
    /** The prefix of every rejection-reason key a hold produces ({@link #reasonKey}). */
    public static final String REASON_PREFIX="thermo-domain: ";
    ThermoHold(String message){super(message);}
    /** The network node whose state was refused, or {@link #NO_NODE}. */
    public abstract long node();
    /** The same hold at a known network node; a hold that already names a node keeps it. */
    public abstract ThermoHold at(long node);
    /** The property package that refused the state. */
    public abstract String packageId();
    /**
     * The rejection-reason key of this hold, {@link #REASON_PREFIX} first: one key per boundary or phase set, so a solve
     * that meets the same hold a thousand times counts it under one key; the numbers are in {@link #getMessage()}.
     */
    public abstract String reasonKey();
    /** Whether {@code other} is the same hold at the same node: a retry reproduced this failure. */
    public abstract boolean sameAs(ThermoHold other);
    /** What happened, for an island's status line, {@code where} the node's label (empty when unknown). */
    public abstract String status(String where);
    /** The suffix of a message naming {@code node}, empty for {@link #NO_NODE}. */
    static String where(long node){return node==NO_NODE?"":" at node "+node;}
}
