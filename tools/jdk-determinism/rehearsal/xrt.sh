#!/usr/bin/env bash
# Cross-runtime runs of the integration rehearsal (batch 2026-09-27-jdk-determinism): one javac build (perf/wbuild.sh),
# run under a named runtime, console to a log.
#   xrt.sh <build: merged|base> <runtime: jdk21|zulu25|jbr25|openj9|graal25> <log> <MiniLauncher selectors...>
# merged = this worktree compiled into build/rehearsal/out, base = git archive db3ac1b9 in build/rehearsal/tree-base
# compiled into build/rehearsal/out-base (both with JDK 21.0.11 javac --release 21).
set -uo pipefail
HERE=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
ROOT=$(cd "$HERE/../../.." && pwd)
build=$1; rt=$2; log=$3; shift 3
case $build in
  merged) export SRC="$ROOT" OUT="$ROOT/build/rehearsal/out" ;;
  base) export SRC="$ROOT/build/rehearsal/tree-base" OUT="$ROOT/build/rehearsal/out-base" ;;
  *) echo "unknown build $build"; exit 2 ;;
esac
case $rt in
  jdk21) export JAVA="C:/Program Files/Java/jdk-21.0.11/bin/java" ;;
  zulu25) export JAVA="C:/Program Files/Zulu/zulu-25/bin/java" ;;
  jbr25) export JAVA="C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.1/jbr/bin/java" ;;
  openj9) export JAVA="D:/Minecraft/Modding/jdks/jdk-21.0.12.15/bin/java" ;;
  graal25) export JAVA="D:/Minecraft/Modding/jdks/graalvm-community-25.4.4.1.1+1.1/bin/java" ;;
  *) echo "unknown runtime $rt"; exit 2 ;;
esac
export JVM_OPTS="${JVM_OPTS:--Xmx4g}"
{ echo "# xrt build=$build runtime=$rt java=$("$JAVA" -version 2>&1 | head -1)"; bash "$ROOT/tools/jdk-determinism/perf/wbuild.sh" run "$@"; } > "$log" 2>&1
grep -E "MINILAUNCHER" "$log" | tail -2
