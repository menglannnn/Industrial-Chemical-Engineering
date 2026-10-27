#!/usr/bin/env bash
# D34: the fluid network test classes the static column touches, on a compiled lane export (lane.sh export/compile):
# every class of science.fluid.network (classes-science.txt), then every class of runtime.fluid (classes-runtime.txt) with
# the scheduler's verify switch, as fluidRuntimeTest sets it. Logs: $LOGS/<name>-science.txt, $LOGS/<name>-runtime.txt
# (LOGS default /tmp/d34-logs). Compare two runs with the main checkout's
# tools/unified-thermo-port-quick-fixes/scripts/compare-test-output.pl <new log> <baseline log>.
set -u
n=${1:?usage: run-touching.sh <name>}; LOGS=${LOGS:-/tmp/d34-logs}; mkdir -p "$LOGS"
D=$(cd "$(dirname "$0")" && pwd)
bash "$D/lane.sh" run "$n" $(tr -d '\r' < "$D/classes-science.txt") > "$LOGS/$n-science.txt" 2>&1
echo "science $n exit=$? $(grep -E '^M3RUN SUMMARY' "$LOGS/$n-science.txt")"
JAVA_OPTS="-Dcreatecheme.fluid.scheduler.verify=true" bash "$D/lane.sh" run "$n" $(tr -d '\r' < "$D/classes-runtime.txt") > "$LOGS/$n-runtime.txt" 2>&1
echo "runtime $n exit=$? $(grep -E '^M3RUN SUMMARY' "$LOGS/$n-runtime.txt")"
grep -hE '^M3RUN (FAILED|ABORTED)' "$LOGS/$n-science.txt" "$LOGS/$n-runtime.txt" | head -20
