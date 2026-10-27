package com.wormzjl.createcheme.runtime.fluid;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Replays the reduced worlds of boundary-split-worlds.txt (boundary split, dead ends and rest, 2026-09-28) on the fuzz's world
 * and prints what is solved: each block with its exact spec, each island (its devices and its graph) after the placements
 * and at the end, and every hold. The blocks are found in the seed's own placement list by their label, so the specs are
 * the seed's exact doubles. One distribution per JVM (holdFuzz.defaults decides, as in the fuzz).
 */
class BoundarySplitProbeTest {
    @Test void replayReducedWorlds() throws Exception {
        var file=Path.of(System.getProperty("user.dir"),"tools/phase-ports-probes/hold-classes/src/boundary-split-worlds.txt");
        String distribution=HoldFuzzTest.DEFAULTS?"defaults":"random";int slices=Integer.getInteger("minimize.slices",40);
        for(var line:Files.readAllLines(file)) {
            if(line.isBlank()||line.startsWith("#"))continue;var parts=line.split("\t");if(!parts[0].equals(distribution))continue;
            long seed=Long.parseLong(parts[1]);var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var original=HoldFuzzTest.placements(r,blocks);
            var list=new ArrayList<HoldFuzzTest.Placement>();
            for(var item:parts[2].trim().split(" (?=\\d+:)")) {
                int colon=item.indexOf(':');long tick=Long.parseLong(item.substring(0,colon));String label=item.substring(colon+1);
                var block=original.stream().map(HoldFuzzTest.Placement::block).filter(b->b.label().equals(label)).findFirst().orElseThrow(()->new AssertionError("seed "+seed+": no block "+label+" in "+HoldMinimizerTest.render(original)));
                list.add(new HoldFuzzTest.Placement(tick,block));
            }
            System.out.println("PROBE "+distribution+" seed="+seed+" blocks="+list.size());
            for(var p:list){var b=p.block();System.out.println("PROBE_BLOCK "+p.tick()+" "+b.label()+" kind="+b.kind()+" facing="+b.facing()+" bore="+b.bore()+" control="+b.control()
                    +(b.spec()!=null?" T="+b.spec().temperature()+" P="+b.spec().pressure()+" fluid="+b.fluid():""));}
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            for(var p:list){w.runTo(p.tick());w.place(p.block());}
            w.runTo(list.getLast().tick()+2);
            System.out.println("PROBE_AFTER_PLACEMENT seed="+seed);describe(w);
            long end=list.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end)w.tick();
            System.out.println("PROBE_END seed="+seed+" tick="+w.ledger.onlineTick()+" holds="+w.held.size());describe(w);
            for(var h:w.held)System.out.println("PROBE_HELD seed="+seed+" "+h);
        }
    }
    /**
     * -Dprobe.seeds=9,30 (-PprobeSeeds): each seed's whole world run to its first hold; prints the held island as it is
     * solved - its blocks with their exact specs, placement ticks and positions, its compiled nodes and connections at the
     * hold - the smallest-world-per-class listing of the review.
     */
    @Test void heldIslands() {
        String spec=System.getProperty("probe.seeds","");if(spec.isBlank())return;int slices=Integer.getInteger("holdFuzz.slices",120);
        for(long seed:HoldFuzzTest.seeds(spec)) {
            var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var placements=HoldFuzzTest.placements(r,blocks);
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());var ticks=new HashMap<List<Integer>,Long>();
            for(var p:placements){w.runTo(p.tick());try{w.place(p.block());}catch(IllegalArgumentException refused){}ticks.put(List.of(p.block().x(),p.block().y(),p.block().z()),p.tick());}
            long end=placements.getLast().tick()+100L*slices;
            while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
            String distribution=HoldFuzzTest.DEFAULTS?"defaults":"random";
            if(w.held.isEmpty()){System.out.println("HELDISLAND "+distribution+" seed="+seed+" clean");continue;}
            var h=w.held.getFirst();
            var byPosition=new HashMap<List<Integer>,HoldFuzzTest.Block>();for(var b:blocks)byPosition.put(List.of(b.x(),b.y(),b.z()),b);
            var list=new StringBuilder();
            for(long id:w.registry.members(h.island())){var d=w.ledger.active().get(id).device();var key=List.of(d.position().x(),d.position().y(),d.position().z());var b=byPosition.get(key);
                list.append(ticks.get(key)).append(':').append(b.label()).append(b.spec()!=null&&b.kind()!=com.wormzjl.createcheme.science.fluid.topology.TopologyCompiler.Kind.PIPE?"{T="+b.spec().temperature()+",P="+b.spec().pressure()+"}":"").append(' ');}
            System.out.println("HELDISLAND "+distribution+" seed="+seed+" tick="+h.tick()+" status="+h.status()+"\nHELDISLAND_BLOCKS "+list+"\n"+w.describe(h.island()));
        }
    }
    /**
     * -Dprobe.seeds=... (-PprobeSeeds): for each seed, the greedy minimizer of HoldMinimizerTest (the seed's first-hold
     * class kept, 8 threads), then the smallest world replayed with its held island printed: the cards of
     * REMAINING_HOLDS.md. Prints CARD_KEEP (replay with HoldMinimizerTest.replay), CARD_LIST, CARD_BLOCKS (exact specs),
     * CARD_HELD and the island.
     */
    @Test void minimizeForCards() throws Exception {
        String spec=System.getProperty("probe.seeds","");if(spec.isBlank()||!Boolean.getBoolean("probe.cards"))return;int slices=40;
        var pool=java.util.concurrent.Executors.newFixedThreadPool(8);
        for(long seed:HoldFuzzTest.seeds(spec)) {
            var r=new Random(seed);var blocks=HoldFuzzTest.world(r);var list=new ArrayList<>(HoldFuzzTest.placements(r,blocks));var start=List.copyOf(list);
            var first=HoldMinimizerTest.run(list,slices);String target=first.cls();
            if(target.equals("clean")){System.out.println("CARD seed="+seed+" clean at "+slices+" slices");continue;}
            boolean progress=true;
            while(progress) {
                progress=false;var futures=new ArrayList<java.util.concurrent.Future<HoldMinimizerTest.Outcome>>();
                for(int i=0;i<list.size();i++){var candidate=new ArrayList<>(list);candidate.remove(i);if(candidate.isEmpty()){futures.add(null);continue;}futures.add(pool.submit(()->HoldMinimizerTest.run(candidate,slices)));}
                for(int i=0;i<futures.size();i++){if(futures.get(i)==null)continue;var o=futures.get(i).get();if(o.cls().equals(target)){list.remove(i);progress=true;for(var f:futures)if(f!=null)f.cancel(true);break;}}
            }
            var last=HoldMinimizerTest.run(list,slices);
            var keep=new StringBuilder();for(var p:list)for(int k=0;k<start.size();k++)if(start.get(k).block().equals(p.block()))keep.append(keep.length()==0?"":",").append(k).append("@").append(p.tick());
            String distribution=HoldFuzzTest.DEFAULTS?"defaults":"random";
            System.out.println("CARD "+distribution+" seed="+seed+" class="+target+" start="+start.size()+" blocks="+list.size()+"\nCARD_KEEP "+keep+"\nCARD_LIST "+HoldMinimizerTest.render(list));
            var b=new StringBuilder();for(var p:list){var x=p.block();b.append(p.tick()).append(':').append(x.label()).append(" control=").append(x.control()).append(x.spec()!=null?" T="+x.spec().temperature()+" P="+x.spec().pressure()+" fluid="+x.fluid():"").append(" | ");}
            System.out.println("CARD_BLOCKS "+b);
            var w=new HoldFuzzTest.World(HoldFuzzTest.MODELS.get());
            for(var p:list){w.runTo(p.tick());try{w.place(p.block());}catch(IllegalArgumentException refused){}}
            long end=list.getLast().tick()+100L*slices;while(w.ledger.onlineTick()<end&&w.held.isEmpty())w.tick();
            if(w.held.isEmpty()){System.out.println("CARD_HELD none");continue;}
            var h=w.held.getFirst();System.out.println("CARD_HELD "+h+"\n"+w.describe(h.island()));
            var labels=new ArrayList<String>();for(long id:w.registry.members(h.island())){var rec=w.ledger.active().get(id);if(rec!=null){var p=rec.device().position();labels.add(rec.device().kind()+"@"+p.x()+","+p.y()+","+p.z());}}
            System.out.println("CARD_ISLAND_DEVICES "+labels+(last.cls().equals(target)?"":" (last run class "+last.cls()+")"));
        }
        pool.shutdown();
    }
    static void describe(HoldFuzzTest.World w) {
        for(var s:w.coordinator.observe()) {
            var labels=new ArrayList<String>();
            for(long id:w.registry.members(s.id())){var r=w.ledger.active().get(id);if(r!=null){var p=r.device().position();labels.add(r.device().kind()+"@"+p.x()+","+p.y()+","+p.z());}}
            System.out.println("PROBE_ISLAND "+s.id()+" devices="+labels+"\n"+w.describe(s.id()));
        }
        var unowned=new ArrayList<String>();
        for(var r:w.ledger.active().values())if(w.registry.owner(r.device().id())==null){var p=r.device().position();unowned.add(r.device().kind()+"@"+p.x()+","+p.y()+","+p.z()+" ("+w.registry.diagnostic(r.device().id())+")");}
        System.out.println("PROBE_UNOWNED "+unowned);
    }
}
