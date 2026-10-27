package com.wormzjl.createcheme.runtime.fluid;

import com.wormzjl.createcheme.science.material.MaterialRuntime;
import org.junit.jupiter.api.Test;
import java.util.Random;

/** t38: prints one fuzz seed's placements with their exact specs and controls (-Dseed=397 -DholdFuzz.cold=true), for a
 * hand-written replay-world gate test. Compiled on the javac lane beside the hold-fuzz harness; not tracked. */
class SeedLayoutDumpTest {
    @Test void dump() {
        long seed=Long.getLong("seed",397);
        Runnable body=()->{var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
            for(var p:placements)System.out.println("LAYOUT tick="+p.tick()+" "+p.block()+(p.block().spec()==null?"":" moles="+java.util.Arrays.toString(p.block().spec().composition())));};
        if(HoldFuzzTest.COLD)MaterialRuntime.with(HoldFuzzTest.CATALOG,HoldFuzzTest.PACKAGE,()->{body.run();return null;});else body.run();
    }
}
