#!/usr/bin/env bash
# m23 (2026-10-20, owner scope): no full unit suite; steps regression gametest campaign.
# M5's measurement lane, alone, one Gradle invocation at a time through gradle-locked.sh (refuses while a dev client runs):
#   01 full test (serves as G1 and G3; results copied to 01-results), 02 G2 chain-100 exact, 03 fluid GameTests on a fresh
#   run id, then the campaign: 1000 seeds, cold random, bundled random, bundled defaults, 8 workers (fuzz.sh, classify.sh).
# Usage (worktree root): [LOGS=build/m5-logs] [RUNID=m5] bash <this> <head-sha> [steps...]
#   steps default: test regression gametest campaign; quick = the 200-seed check (cold random, bundled random and defaults)
set -u
WT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/stoic-bell-ae6019
cd "$WT" || exit 2
sha=${1:?usage: lane.sh <head-sha> [steps]}; shift
steps=${*:-regression gametest campaign}
L=${LOGS:-build/m5-logs}; mkdir -p "$L"
G="bash /d/tmp_fl/scripts/gradle-locked.sh"
F=D:/Minecraft/Modding/1.21/CreateChemE/tools/unified-thermo-port-forms
for s in $steps; do
  case $s in
    test) echo "== test start $(date '+%H:%M:%S')"; $G $L/01-full-test.gradle.txt test --continue --rerun; rm -rf $L/01-results; cp -r build/test-results/test $L/01-results;
          grep -hoE 'tests="[0-9]+"|failures="[0-9]+"|errors="[0-9]+"|skipped="[0-9]+"' $L/01-results/*.xml | awk -F'"' '{s[$1]+=$2} END{for(k in s) printf "%s%d ", k, s[k]; print ""}' ;;
    regression) echo "== regression start $(date '+%H:%M:%S')"; $G $L/02-G2-regression-exact.gradle.txt --no-configuration-cache fluidSolverRegression -PfluidRegressionMode=exact; grep -iE "max|0\.000e|differ|BUILD" $L/02-G2-regression-exact.gradle.txt | tail -8 ;;
    gametest) echo "== gametest start $(date '+%H:%M:%S')"; $G $L/03-G5-gametests.gradle.txt --no-configuration-cache runFluidGameTestServer -PfluidGameTestRunId=${RUNID:-m5}-$sha; grep -E "required tests|BUILD|failed" $L/03-G5-gametests.gradle.txt | tail -5 ;;
    quick) echo "== quick start $(date '+%H:%M:%S')";
          HARNESS=$F/hold-fuzz-cold DISTS=random EXTRA=-PholdFuzzCold=true OUT=$L bash /d/tmp_fl/scripts/fuzz.sh 00-quick-cold 1-200;
          HARNESS=$F/hold-fuzz-bundled OUT=$L bash /d/tmp_fl/scripts/fuzz.sh 00-quick-bundled 1-200 ;;
    campaign) echo "== campaign start $(date '+%H:%M:%S')";
          HARNESS=$F/hold-fuzz-cold DISTS=random EXTRA=-PholdFuzzCold=true OUT=$L bash /d/tmp_fl/scripts/fuzz.sh 04-campaign-cold 1-1000;
          HARNESS=$F/hold-fuzz-bundled OUT=$L bash /d/tmp_fl/scripts/fuzz.sh 05-campaign-bundled 1-1000 ;;
    *) echo "unknown step $s"; exit 2 ;;
  esac
done
echo "== lane done $(date '+%H:%M:%S')"
