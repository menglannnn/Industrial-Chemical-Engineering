#!/usr/bin/env bash
# M1 D21 trace runner: FluidTpEquilibrium of WT with trace prints in the nested UV (trace-inner.js), then D21Probe in
# mode $1 with -Dm1.trace=1 -Dm1.one=1 (one triple-point case). Used to find the 281 k-call hold before the triple
# point moved ahead of the search. Historical: trace-inner.js matches the engine of 2026-10-15 before commit 7a3ea017.
set -e
HERE="$(cd "$(dirname "$0")" && pwd)"
WT="${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}"
SRC="$WT/src/main/java/com/wormzjl/createcheme/science/thermo/phase"
OUT=/tmp/m1/var-trace1; rm -rf "$OUT" "$OUT-src"; mkdir -p "$OUT" "$OUT-src"
node "$(cygpath -w "$HERE/trace-inner.js")" "$SRC/FluidTpEquilibrium.java" "$OUT-src/FluidTpEquilibrium.java"
JDK="/c/Program Files/Java/jdk-21.0.11/bin"
G=/c/Users/wormz/.gradle/caches/modules-2/files-2.1
GSON=$(cygpath -m "$(find $G/com.google.code.gson -name gson-2.10.1.jar | head -1)")
EJML="$(cygpath -m "$(find $G/org.ejml -name ejml-core-0.44.0.jar | head -1)");$(cygpath -m "$(find $G/org.ejml -name ejml-ddense-0.44.0.jar | head -1)")"
BASE="$WT/build/classes/java/main;$WT/build/resources/main;$GSON;$EJML"
"$JDK/javac" -nowarn -proc:none -d "$(cygpath -m $OUT)" -cp "$BASE" "$(cygpath -m $OUT-src/FluidTpEquilibrium.java)" "$SRC/EquilibriumResult.java"
mkdir -p /tmp/m1/probe/d21out
"$JDK/javac" -nowarn -proc:none -d "$(cygpath -m /tmp/m1/probe/d21out)" -cp "$(cygpath -m $OUT);$BASE" "$(cygpath -m "$HERE/D21Probe.java")"
"$JDK/java" -Xshare:off -Dm1.trace=1 -Dm1.one=1 -cp "$(cygpath -m /tmp/m1/probe/d21out);$(cygpath -m $OUT);$BASE" D21Probe "$@"
