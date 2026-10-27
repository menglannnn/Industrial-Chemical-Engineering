#!/usr/bin/env bash
# M2 three-phase probe: instruments copies of the engine's stability test, compiles them with ThreeProbe against the
# Gradle-compiled main classes of the worktree, and replays states.txt. Usage: run.sh [maxStates] > out.txt
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
WT="${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}"
JDK="/c/Program Files/Java/jdk-21.0.11/bin"
G=/c/Users/wormz/.gradle/caches/modules-2/files-2.1
m() { cygpath -m "$1"; }
jar() { m "$(find "$G/$1" -name "$2" | head -1)"; }
JARS="$(jar com.google.code.gson gson-2.10.1.jar);$(jar org.ejml ejml-core-0.44.0.jar);$(jar org.ejml ejml-ddense-0.44.0.jar);$(jar org.ejml ejml-dsparse-0.44.0.jar)"
GEN="$HERE/build/gen"; OUT="$HERE/build/classes"
rm -rf "$HERE/build"; mkdir -p "$GEN" "$OUT"
node "$(m "$HERE/instrument.js")" "$WT" "$(m "$GEN")" >&2
mapfile -t SOURCES < <(find "$GEN" "$HERE/src" -name '*.java')
MAIN="$WT/build/classes/java/main;$WT/build/resources/main"
"$JDK/javac" -nowarn -encoding UTF-8 -proc:none -d "$(m "$OUT")" -cp "$MAIN;$JARS" $(for f in "${SOURCES[@]}"; do m "$f"; done)
"$JDK/java" -Xshare:off -cp "$(m "$OUT");$MAIN;$JARS" com.wormzjl.createcheme.science.fluid.thermo.ThreeProbe "$(m "${STATES:-$HERE/states.txt}")" "${1:-1000}"
