#!/usr/bin/env bash
# M3a probe runner (D21 follow-ups, batch 2026-09-24-coolprop-low-temperature). Compiles the FluidTpEquilibrium.java and
# EquilibriumResult.java of <src-dir> (with innerUv trace prints when <trace> is "trace") over the compiled classes
# <main-classes> and resources <res-main>, then runs M3aProbe in <mode> (ph, uvrow, triple, grid, all). No timing.
# Usage: run.sh <label> <src-dir> <main-classes> <res-main> <mode> [trace]
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
LABEL="$1"; SRC="$2"; MAIN="$3"; RES="$4"; MODE="$5"; TRACE="${6:-}"
OUT=/tmp/m3a/var-$LABEL; rm -rf "$OUT" "$OUT-src"; mkdir -p "$OUT" "$OUT-src"
if [ "$TRACE" = trace ]; then
  node "$(cygpath -w "$HERE/trace-inner.js")" "$(cygpath -w "$SRC/FluidTpEquilibrium.java")" "$(cygpath -w "$OUT-src/FluidTpEquilibrium.java")" >&2
else
  cp "$SRC/FluidTpEquilibrium.java" "$OUT-src/FluidTpEquilibrium.java"
fi
cp "$SRC/EquilibriumResult.java" "$OUT-src/EquilibriumResult.java"
JDK="/c/Program Files/Java/jdk-21.0.11/bin"
G=/c/Users/wormz/.gradle/caches/modules-2/files-2.1
GSON=$(cygpath -m "$(find $G/com.google.code.gson -name gson-2.10.1.jar | head -1)")
EJML="$(cygpath -m "$(find $G/org.ejml -name ejml-core-0.44.0.jar | head -1)");$(cygpath -m "$(find $G/org.ejml -name ejml-ddense-0.44.0.jar | head -1)")"
BASE="$(cygpath -m "$MAIN");$(cygpath -m "$RES");$GSON;$EJML"
"$JDK/javac" -nowarn -proc:none -encoding UTF-8 -d "$(cygpath -m $OUT)" -cp "$BASE" "$(cygpath -m $OUT-src/FluidTpEquilibrium.java)" "$(cygpath -m $OUT-src/EquilibriumResult.java)"
mkdir -p /tmp/m3a/probe-classes
"$JDK/javac" -nowarn -proc:none -encoding UTF-8 -d "$(cygpath -m /tmp/m3a/probe-classes)" -cp "$(cygpath -m $OUT);$BASE" "$(cygpath -m "$HERE/M3aProbe.java")"
PROPS=""; [ "$TRACE" = trace ] && PROPS="-Dm3a.trace=1"
"$JDK/java" -Xshare:off $PROPS -cp "$(cygpath -m /tmp/m3a/probe-classes);$(cygpath -m $OUT);$BASE" M3aProbe "$MODE"
