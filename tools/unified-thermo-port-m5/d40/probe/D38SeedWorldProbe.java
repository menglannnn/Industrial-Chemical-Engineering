package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * D38 probe (tools/unified-thermo-port-m5/d38, never tracked): a cold seed's KEEP-list world (the M7 trace's
 * pair-minimal lists) with every block printed to full precision, so a tracked test can place the same blocks, and the
 * world's class at the current code (M5MinimizerTest.run: placed at its ticks, then -Dd38.slices slices, stopped at the
 * first hold). Needs the cold harness HoldFuzzTest and the trace's M5TraceWorld, M5MinimizerTest and M5SolveLogTest in the
 * same package, and -DholdFuzz.cold=true. -Dd38.seed (30), -Dd38.keep (seed 30's pair-minimal list), -Dd38.slices (40).
 */
class D38SeedWorldProbe {
    static final String SEED30="0@100,1@466,2@470,3@470,4@470,5@599,6@647,7@696,8@971,11@1283,12@1593";
    @Test void world() {
        long seed=Long.getLong("d38.seed",30);String keep=System.getProperty("d38.keep",SEED30);int slices=Integer.getInteger("d38.slices",40);
        var list=M5SolveLogTest.list(seed,keep);
        System.out.println("D38WORLD seed="+seed+" keep="+keep+" basis="+HoldFuzzTest.BASIS);
        for(var p:list){var b=p.block();
            System.out.println("D38BLOCK tick="+p.tick()+" "+b.label()+" kind="+b.kind()+" at="+b.x()+","+b.y()+","+b.z()+" facing="+b.facing()+" bore="+b.bore()+" control="+b.control()
                    +(b.spec()==null?" spec=null":" T="+b.spec().temperature()+" P="+b.spec().pressure()+" V="+b.spec().volume()+" n="+Arrays.toString(b.spec().composition())));}
        var o=M5MinimizerTest.run(list,slices);
        System.out.println("D38OUTCOME seed="+seed+" class="+o.cls()+" tick="+o.tick()+" island="+o.island()+" maxCheckpoints="+o.maxCheckpoints()+" status="+o.status());
    }
}
