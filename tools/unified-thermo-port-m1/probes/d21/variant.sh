#!/usr/bin/env bash
# M1 D21 probe runner (unified thermo port, batch 2026-09-24-coolprop-low-temperature). Builds FluidTpEquilibrium of the
# worktree WT (default: the M1 worktree) with STEP_FLUID_TRACES = $1 (or the near-pure rule off with "off") into
# /tmp/m1/var-$1 over the last Gradle compile, then runs D21Probe in mode $2: uv (row A's vessel with N2 or CH4 traces),
# ph (101.325 kPa across the sublimation line), deep (deep deposition), above (traces above theta), triple3 (inside the
# triple point's triangle), all. No timing.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
WT="${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}"
SRC="$WT/src/main/java/com/wormzjl/createcheme/science/thermo/phase"
V="$1"; MODE="${2:-all}"
OUT=/tmp/m1/var-$V; rm -rf "$OUT" "$OUT-src"; mkdir -p "$OUT" "$OUT-src"
if [ "$V" = off ]; then
  sed 's/return traces(major, w) <= NEAR_PURE_TRACES \* w.hydrocarbonTotal ? major : -1;/return -1;/' "$SRC/FluidTpEquilibrium.java" > "$OUT-src/FluidTpEquilibrium.java"
else
  sed "s/private static final double STEP_FLUID_TRACES = 1.0e-4;/private static final double STEP_FLUID_TRACES = $V;/" "$SRC/FluidTpEquilibrium.java" > "$OUT-src/FluidTpEquilibrium.java"
fi
JDK="/c/Program Files/Java/jdk-21.0.11/bin"
G=/c/Users/wormz/.gradle/caches/modules-2/files-2.1
GSON=$(cygpath -m "$(find $G/com.google.code.gson -name gson-2.10.1.jar | head -1)")
EJML="$(cygpath -m "$(find $G/org.ejml -name ejml-core-0.44.0.jar | head -1)");$(cygpath -m "$(find $G/org.ejml -name ejml-ddense-0.44.0.jar | head -1)")"
BASE="$WT/build/classes/java/main;$WT/build/resources/main;$GSON;$EJML"
"$JDK/javac" -nowarn -proc:none -d "$(cygpath -m $OUT)" -cp "$BASE" "$(cygpath -m $OUT-src/FluidTpEquilibrium.java)" "$SRC/EquilibriumResult.java"
mkdir -p /tmp/m1/probe/d21out
"$JDK/javac" -nowarn -proc:none -d "$(cygpath -m /tmp/m1/probe/d21out)" -cp "$(cygpath -m $OUT);$BASE" "$(cygpath -m "$HERE/D21Probe.java")"
"$JDK/java" -Xshare:off -cp "$(cygpath -m /tmp/m1/probe/d21out);$(cygpath -m $OUT);$BASE" D21Probe "$MODE"
