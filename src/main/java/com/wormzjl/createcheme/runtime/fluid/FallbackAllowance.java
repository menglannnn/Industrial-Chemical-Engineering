package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.fluid.network.ApproximationAnchor;
import java.util.*;

/** Persistent allowance for one approximate episode. Held results and restart do not reset it. */
public record FallbackAllowance(int acceptedIntervals,long advancedTicks,int capturedCadenceTicks) {
    public static final FallbackAllowance NONE=new FallbackAllowance(0,0,0);
    public FallbackAllowance {
        if(acceptedIntervals<0||acceptedIntervals>3||advancedTicks<0
                ||acceptedIntervals==0&&(advancedTicks!=0||capturedCadenceTicks!=0)
                ||acceptedIntervals>0&&(capturedCadenceTicks<20||capturedCadenceTicks>400||advancedTicks==0||advancedTicks>3L*capturedCadenceTicks)) {
            throw new IllegalArgumentException("Invalid fallback allowance snapshot");
        }
    }
    public boolean permits(long durationTicks,int currentCadenceTicks) {
        if(durationTicks<=0||currentCadenceTicks<20||currentCadenceTicks>400)throw new IllegalArgumentException("Invalid approximate duration/cadence");
        int captured=acceptedIntervals==0?currentCadenceTicks:capturedCadenceTicks;
        return acceptedIntervals<3&&durationTicks<=3L*captured-advancedTicks;
    }
    public FallbackAllowance accept(long durationTicks,int currentCadenceTicks) {
        if(!permits(durationTicks,currentCadenceTicks))throw new IllegalStateException("Approximate allowance exhausted");
        return new FallbackAllowance(acceptedIntervals+1,advancedTicks+durationTicks,acceptedIntervals==0?currentCadenceTicks:capturedCadenceTicks);
    }
    /** Call only after a valid full result commits from the current inventory. */
    public FallbackAllowance fullRecovery(){return NONE;}
    /** Immutable admission-time policy. Proposed allowance changes take effect only with an island commit. */
    public record Policy(boolean enabled,Optional<ApproximationAnchor> anchor,FallbackAllowance allowance,
                                      int cadenceTicks,long softBudgetNanos) {
        public Policy {
            Objects.requireNonNull(anchor);Objects.requireNonNull(allowance);
            if(cadenceTicks<20||cadenceTicks>400||softBudgetNanos<0||enabled&&(anchor.isEmpty()||softBudgetNanos==0)
                    ||allowance.acceptedIntervals()>0&&anchor.isEmpty())throw new IllegalArgumentException("Invalid fallback policy");
        }
        public static Policy disabled(){return new Policy(false,Optional.empty(),NONE,100,0);}
        public static Policy active(ApproximationAnchor anchor,FallbackAllowance allowance,int cadenceTicks,long softBudgetNanos) {
            return new Policy(true,Optional.of(anchor),allowance,cadenceTicks,softBudgetNanos);
        }
    }
}
