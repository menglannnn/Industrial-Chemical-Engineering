#!/usr/bin/env bash
# Alternated cost runs of compiled builds (batch 2026-09-27-jdk-determinism, pow performance study).
#   timing.sh <log-file> <reps> <what> <build-dir>...
#   what = suite   the fluid suites (science.fluid minus PackagedSparseLuTest, runtime.fluid, runtime.Fluid*Test) in
#                  one JVM: MiniLauncher wallMs, MIXED_GAS_COST ms, test counts
#          mixed   MixedGasJunctionTransientTest 5 s method alone (MIXED_GAS_COST ms)
#          column  ColumnProbe time 5 (median ms per column case)
#          colsuite the column, thermo and material test packages (wall, failures); needs a `compile column` build
# Before every run it waits (up to 60 min, checking every 30 s) while another Gradle build, Gradle test JVM or dev client runs on
# the machine (gradle-wrapper.jar client, GradleWorkerMain, a NeoForge dev client), so no run overlaps another agent's measurement.
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
LOG=${1:?log}; REPS=${2:?reps}; WHAT=${3:?what}; shift 3
busy() { powershell -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"name='java.exe'\" | Where-Object { \$_.CommandLine -match 'gradle-wrapper.jar|GradleWorkerMain|Gradle Test Executor|runMcpClient|mcp-client|mcpClient|net.neoforged|minecraft.client' }).Count" | tr -d '\r'; }
wait_idle() {
  local n=0
  while [ "$(busy)" != "0" ]; do
    [ $n = 0 ] && echo "# $(date +%T) waiting: another Gradle build/test JVM is running" | tee -a "$LOG"
    n=$((n+1)); [ $n -gt 120 ] && { echo "# gave up waiting" | tee -a "$LOG"; exit 3; }
    sleep 30
  done
}
echo "# $(date '+%F %T') timing $WHAT reps=$REPS builds=$* java=$("${PERF_JAVA_HOME:-C:/Program Files/Java/jdk-21.0.11}/bin/java" -version 2>&1 | head -1)" | tee -a "$LOG"
for rep in $(seq 1 "$REPS"); do
  for build in "$@"; do
    wait_idle
    export OUT=$build
    case $WHAT in
      suite) out=$(bash "$HERE/wbuild.sh" run package:com.wormzjl.createcheme.science.fluid package:com.wormzjl.createcheme.runtime.fluid \
                 $(cd "$build/test" && ls com/wormzjl/createcheme/runtime/Fluid*Test.class 2>/dev/null | sed -E 's#/#.#g; s#\.class$##; s#^#class:#') \
                 'exclude:.*PackagedSparseLuTest' 2>&1)
             echo "$rep $(basename "$build") $(echo "$out" | grep -oE 'MIXED_GAS_COST .*') $(echo "$out" | grep -oE 'MINILAUNCHER .*')" | tee -a "$LOG" ;;
      mixed) out=$(bash "$HERE/wbuild.sh" run "method:com.wormzjl.createcheme.runtime.fluid.MixedGasJunctionTransientTest#everyCaseConvergesAtTheFiveSecondCycleWithinTwiceTheMeasuredCost" 2>&1)
             echo "$rep $(basename "$build") $(echo "$out" | grep -oE 'MIXED_GAS_COST .*')" | tee -a "$LOG" ;;
      colsuite) out=$(bash "$HERE/wbuild.sh" run package:com.wormzjl.createcheme.science.column package:com.wormzjl.createcheme.science.thermo \
                 package:com.wormzjl.createcheme.science.material 2>&1)
             echo "$rep $(basename "$build") $(echo "$out" | grep -oE 'MINILAUNCHER .*') failed: $(echo "$out" | grep -oE '^  JUnit Jupiter:[^(]*' | sed 's/  JUnit Jupiter://' | tr '\n' ' ')" | tee -a "$LOG" ;;
      column) out=$(bash "$HERE/wbuild.sh" java ColumnProbe time 5 2>&1)
             echo "$out" | grep COLUMN_TIME | sed "s/^/$rep $(basename "$build") /" | tee -a "$LOG" ;;
    esac
  done
done
