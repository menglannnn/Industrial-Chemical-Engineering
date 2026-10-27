#!/usr/bin/env bash
# Windows (Git Bash) javac + MiniLauncher runner for the fluid and column science tests, without Gradle and without the
# JUnit console jar. Same selection and stand-ins as tools/cloud-science-harness/harness.sh; jars from the Gradle cache.
# Batch 2026-09-27-jdk-determinism, pow performance study. See ../README.md, section "perf/".
#
#   SRC=<tree> OUT=<dir> wbuild.sh compile [fluid|column|all]   compile SRC/src/{main,test}/java into OUT
#   OUT=<dir> wbuild.sh run <MiniLauncher selectors...>          run tests (working directory SRC)
#   OUT=<dir> wbuild.sh java <main class> [args...]               run any class on the test classpath
# Env: PERF_JAVA_HOME (default C:/Program Files/Java/jdk-21.0.11), JAVA (runner override), JVM_OPTS (default -Xmx2g),
#      RES (tree holding the resources and the tests' working directory, default SRC), EXTRA_CP (prepended to the runtime classpath, e.g. an instrumented DeterministicMath).
set -uo pipefail
unset JAVA_TOOL_OPTIONS
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
TOOLS=$(cd "$HERE/../.." && pwd)
SRC=$(cd "${SRC:-$TOOLS/..}" && pwd)
RES=$(cd "${RES:-$SRC}" && pwd)   # tree whose resources and working directory the tests use (scratch copies hold java only)
OUT=${OUT:?OUT required}
JAVA_HOME=${PERF_JAVA_HOME:-"C:/Program Files/Java/jdk-21.0.11"}   # not $JAVA_HOME: the machine sets it to Zulu 25
JAVAC="$JAVA_HOME/bin/javac"
JAVA=${JAVA:-"$JAVA_HOME/bin/java"}
JVM_OPTS=${JVM_OPTS:--Xmx2g}
G=$HOME/.gradle/caches/modules-2/files-2.1
jar() { find "$G/$1" -name "$2" | grep -v -e sources -e javadoc | head -1; }
DEPS=(
  "$(jar com.google.code.gson/gson/2.10.1 gson-2.10.1.jar)"
  "$(jar org.ejml/ejml-core/0.44.0 ejml-core-0.44.0.jar)"
  "$(jar org.ejml/ejml-ddense/0.44.0 ejml-ddense-0.44.0.jar)"
  "$(jar org.ejml/ejml-dsparse/0.44.0 ejml-dsparse-0.44.0.jar)"
)
JUNIT=(
  "$(jar org.junit.jupiter/junit-jupiter-api/5.11.4 junit-jupiter-api-5.11.4.jar)"
  "$(jar org.junit.jupiter/junit-jupiter-engine/5.11.4 junit-jupiter-engine-5.11.4.jar)"
  "$(jar org.junit.jupiter/junit-jupiter-params/5.11.4 junit-jupiter-params-5.11.4.jar)"
  "$(jar org.junit.platform/junit-platform-commons/1.11.4 junit-platform-commons-1.11.4.jar)"
  "$(jar org.junit.platform/junit-platform-engine/1.11.4 junit-platform-engine-1.11.4.jar)"
  "$(jar org.junit.platform/junit-platform-launcher/1.11.4 junit-platform-launcher-1.11.4.jar)"
  "$(jar org.opentest4j/opentest4j/1.3.0 opentest4j-1.3.0.jar)"
  "$(jar org.apiguardian/apiguardian-api/1.1.2 apiguardian-api-1.1.2.jar)"
)
win() { cygpath -w "$1"; }
cp_of() { local r="" p; for p in "$@"; do [ -n "$p" ] && r="$r;$(win "$p")"; done; echo "${r:1}"; }
PKG=com/wormzjl/createcheme
EXCLUDED='/(runtime/fluid/FluidPacketCodecTest|runtime/fluid/FluidDeviceSpecDomainTest|science/column/v3/ColumnInputPresetTest)\.java$'

compile() {
  local set=${1:-fluid}
  rm -rf "$OUT/main" "$OUT/test" "$OUT/launcher"; mkdir -p "$OUT/main" "$OUT/test" "$OUT/launcher"
  { find "$SRC/src/main/java/$PKG/science" "$SRC/src/main/java/$PKG/runtime" -name '*.java'
    echo "$SRC/src/main/java/$PKG/world/level/block/entity/ColumnInputPreset.java"
    # harness stand-ins, except those overridden by a file at the same relative path in perf/stubs
    (cd "$TOOLS/cloud-science-harness/stubs" && find . -name '*.java' | while read -r s; do
       [ -f "$HERE/stubs/$s" ] || echo "$TOOLS/cloud-science-harness/stubs/${s#./}"; done)
    find "$HERE/stubs" -name '*.java'; } | while read -r f; do win "$f"; done > "$OUT/main-sources.txt"
  "$JAVAC" -encoding UTF-8 --release 21 -nowarn -Xmaxerrs 50 -d "$(win "$OUT/main")" -cp "$(cp_of "${DEPS[@]}")" @"$(win "$OUT/main-sources.txt")" || { echo "main compile failed"; return 2; }
  local roots=()
  case $set in
    fluid) roots=("$SRC/src/test/java/$PKG/science/fluid" "$SRC/src/test/java/$PKG/runtime" "$SRC/src/test/java/$PKG/fluid") ;;
    column) roots=("$SRC/src/test/java/$PKG/science/column" "$SRC/src/test/java/$PKG/science/thermo" "$SRC/src/test/java/$PKG/science/material") ;;
    all) roots=("$SRC/src/test/java/$PKG/science" "$SRC/src/test/java/$PKG/runtime" "$SRC/src/test/java/$PKG/fluid") ;;
  esac
  find "${roots[@]}" -name '*.java' | grep -vE "$EXCLUDED" | while read -r f; do win "$f"; done > "$OUT/test-sources.txt"
  "$JAVAC" -encoding UTF-8 --release 21 -nowarn -Xmaxerrs 50 -d "$(win "$OUT/test")" -cp "$(cp_of "$OUT/main" "${DEPS[@]}" "${JUNIT[@]}")" \
    -sourcepath "$(win "$SRC/src/test/java")" -implicit:class @"$(win "$OUT/test-sources.txt")" || { echo "test compile failed"; return 2; }
  # MiniLauncher and ColumnProbe against the compiled main classes (FastDet* need the branch's facade: kernel-sweep.sh)
  "$JAVAC" -encoding UTF-8 --release 21 -nowarn -d "$(win "$OUT/launcher")" -cp "$(cp_of "$OUT/main" "${DEPS[@]}" "${JUNIT[@]}")" \
    "$(win "$HERE/src/MiniLauncher.java")" "$(win "$HERE/src/ColumnProbe.java")" || return 2
  echo "wbuild: compiled $(wc -l < "$OUT/main-sources.txt") main and $(wc -l < "$OUT/test-sources.txt") test sources ($set) from $SRC into $OUT"
}

runcp() { cp_of ${EXTRA_CP:-} "$OUT/launcher" "$OUT/test" "$RES/src/test/resources" "$OUT/main" "$RES/src/main/resources" "$RES/src/generated/resources" "${DEPS[@]}" "${JUNIT[@]}"; }

cmd=${1:-}; shift || true
case $cmd in
  compile) compile "$@" ;;
  run) (cd "$RES" && "$JAVA" $JVM_OPTS -Dcreatecheme.fluid.scheduler.verify=true -Dfluid.modJar=no-mod-jar -cp "$(runcp)" MiniLauncher "$@") ;;
  java) (cd "$RES" && "$JAVA" $JVM_OPTS -Dcreatecheme.fluid.scheduler.verify=true -cp "$(runcp)" "$@") ;;
  *) sed -n 2,11p "$0"; exit 2 ;;
esac
