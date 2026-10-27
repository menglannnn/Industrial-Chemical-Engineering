#!/usr/bin/env bash
# t285: compile the cold harness into export <name> and run seed(s) <seeds> on one thread; output to /d/tmp_285/out/<label>.txt
set -u
name=$1; label=$2; seeds=${3:-285}; H=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms/hold-fuzz-cold/src/HoldFuzzTest.java
mkdir -p /d/tmp_285/out
bash /d/tmp_285/scripts/lane.sh compile "$name" "$H" | tail -3
JAVA_OPTS="-DholdFuzz.seeds=$seeds -DholdFuzz.threads=1 -DholdFuzz.counters=true -DholdFuzz.cold=true -DholdFuzz.defaults=false -DholdFuzz.verbose=${VERBOSE:-true} -DholdFuzz.out=D:/tmp_285/out/$label-report.txt ${EXTRA:-}" \
  bash /d/tmp_285/scripts/lane.sh run "$name" com.wormzjl.createcheme.runtime.fluid.HoldFuzzTest > /d/tmp_285/out/$label.txt 2>&1
echo "rc=$? lines=$(wc -l < /d/tmp_285/out/$label.txt)"; grep -E '^(SUMMARY|COUNTERS)' /d/tmp_285/out/$label-report.txt | cut -c1-600
