#!/usr/bin/env bash
# Runs test class groups on a compiled lane export (lane.sh export/compile), one JVM per group, with the scheduler's
# verify switch as the Gradle test, fluidScienceTest and fluidRuntimeTest tasks set it.
#   bash run-touching.sh <name> [group...]     groups are classes-<group>.txt in this folder (default:
#                                              science-fluid-network runtime-fluid)
# Logs: $LOGS/<name>-<group>.txt (LOGS default /tmp/simp-logs). Compare two runs with the main checkout's
# tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl <new log> <baseline log>.
set -u
n=${1:?usage: run-touching.sh <name> [group...]}; shift
LOGS=${LOGS:-/tmp/simp-logs}; mkdir -p "$LOGS"
D=$(cd "$(dirname "$0")" && pwd)
groups=${*:-science-fluid-network runtime-fluid}
for g in $groups; do
  [ -f "$D/classes-$g.txt" ] || { echo "no group $g"; continue; }
  JAVA_OPTS="-Dcreatecheme.fluid.scheduler.verify=true ${JAVA_OPTS:-}" bash "$D/lane.sh" run "$n" $(tr -d '\r' < "$D/classes-$g.txt") > "$LOGS/$n-$g.txt" 2>&1
  echo "$g $n exit=$? $(grep -E '^M3RUN SUMMARY' "$LOGS/$n-$g.txt")"
  grep -hE '^M3RUN (FAILED|ABORTED)' "$LOGS/$n-$g.txt" | head -20
done
