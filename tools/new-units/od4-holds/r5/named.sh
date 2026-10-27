#!/usr/bin/env bash
# r5: the named worlds on export $1 (label $2): cold 509 592 170 757 768 285 583 743 145 322 354 889 729, bundled 583 743 145 322 354 889 285
set -u
n=$1; l=$2; F=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms
VERBOSE=false bash /d/tmp_285/scripts/run.sh $n $l-cold 509,592,170,757,768,285,583,743,145,322,354,889,729 | tail -2
bash /d/tmp_285/scripts/lane.sh compile $n "$(cygpath -m $F/hold-fuzz-bundled/src/HoldFuzzTest.java)" | tail -1
JAVA_OPTS="-DholdFuzz.seeds=583,743,145,322,354,889,285 -DholdFuzz.threads=1 -DholdFuzz.counters=true -DholdFuzz.defaults=false -DholdFuzz.verbose=false -DholdFuzz.out=D:/tmp_285/out/$l-bundled-report.txt" \
  bash /d/tmp_285/scripts/lane.sh run $n com.wormzjl.createcheme.runtime.fluid.HoldFuzzTest > /d/tmp_285/out/$l-bundled.txt 2>&1
grep -E '^SUMMARY' /d/tmp_285/out/$l-bundled-report.txt | cut -c1-200
