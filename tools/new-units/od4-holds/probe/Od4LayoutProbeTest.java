package com.wormzjl.createcheme.runtime.fluid;
import org.junit.jupiter.api.Test;
import java.util.Random;
/** Probe (OD4 holds, not tracked): one seed's blocks with their bores and controls. */
class Od4LayoutProbeTest {
    @Test void dump() {
        long seed=Long.getLong("seed",889);var r=new Random(seed);var blocks=HoldFuzzTest.world(r);
        for(var b:blocks)System.out.println("OD4_BLOCK seed="+seed+" "+b);
    }
}
