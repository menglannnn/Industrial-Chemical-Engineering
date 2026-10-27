package com.wormzjl.createcheme.runtime.fluid;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

/**
 * t40 trace (tools/fluid-solver-simplification/trace, never tracked): the class of a KEEP list of a bundled seed with one
 * generator restated at other pressures. {@code -Dt40.seed}, {@code -Dt40.keep} ("i@tick,..."), {@code -Dt40.index} (the
 * generator's placement index in the seed), {@code -Dt40.pressures} (Pa, comma separated), {@code -Dminimize.threads} (at
 * most 4), {@code -Dt40.slices} (40). One line per variant: {@code T40VAR p= class= tick= status=}.
 */
class T40VariantsTest {
    @Test void pressures() throws Exception {
        long seed=Long.getLong("t40.seed",407);String keep=System.getProperty("t40.keep");int index=Integer.getInteger("t40.index",16);int slices=Integer.getInteger("t40.slices",40);
        var original=HoldFuzzTest.placements(new Random(seed),HoldFuzzTest.world(new Random(seed)));var block=original.get(index).block();var s0=block.spec();
        var pool=Executors.newFixedThreadPool(Math.min(4,Integer.getInteger("minimize.threads",2)));var futures=new LinkedHashMap<String,Future<M5MinimizerTest.Outcome>>();
        for(var p:System.getProperty("t40.pressures","316000").split(",")) {
            var moved=new HoldFuzzTest.Block(block.x(),block.y(),block.z(),block.kind(),block.facing(),block.bore(),block.control(),
                    new FluidDeviceSpec(s0.volume(),s0.temperature(),Double.parseDouble(p),s0.composition()),block.fluid());
            var list=new ArrayList<>(M5SolveLogTest.list(seed,keep));
            for(int i=0;i<list.size();i++)if(list.get(i).block().equals(block))list.set(i,new HoldFuzzTest.Placement(list.get(i).tick(),moved));
            futures.put(p,pool.submit(()->M5MinimizerTest.run(list,slices)));
        }
        for(var e:futures.entrySet()){var o=e.getValue().get();var st=o.status().length()>120?o.status().substring(0,120):o.status();System.out.println("T40VAR p="+e.getKey()+" class="+o.cls()+" tick="+o.tick()+" status="+st);}
        pool.shutdown();
    }
}
