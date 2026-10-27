#!/usr/bin/env bash
# compile.sh <label> [commit=HEAD] (PROBES_ONLY=1: recompile the probes on the existing export; PATCH="<files>": apply them to the
# export before compiling; EXTRA_PROBES="<files>": more probe sources, e.g. the M5 trace consumer for a patched export): the javac index lane of the unified thermo port test world (adapted from
# tools/unified-thermo-port-m3/scripts/index-compile.sh). Exports the commit's sources and resources with git archive
# into /tmp/testworld-<label>/ (the worktree's files are never read, so another agent's edits do not leak in), compiles
# every main source, then the probes in probes/ with the test sources they reach (-sourcepath). One javac JVM each,
# -Xmx6g. Refuses while a Minecraft dev client runs (a java process with fml.modFolders), as the owner's rules ask.
# The classpath is the dev client's and the game-test server's libraries from the worktree's build/moddev (read only).
set -u
LABEL=${1:?label}; COMMIT=${2:-HEAD}
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
HERE=$(cd "$(dirname "$0")" && pwd)
JDK="C:/Program Files/Java/jdk-21.0.11/bin"
G=C:/Users/wormz/.gradle/caches/modules-2/files-2.1
JUNIT="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-params/5.11.4/e4c86fbe2a39c60c6b87260ef7f7e7c1a1906481/junit-jupiter-params-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
live=$(powershell.exe -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { \$_.CommandLine -match 'fml.modFolders' }).Count" | tr -d '\r')
if [ "${live:-0}" != "0" ]; then echo "compile.sh: a Minecraft dev client is running ($live); wait until it has exited"; exit 3; fi
X=/c/Users/wormz/.claude/jobs/41914354/tmp/tw-$LABEL
if [ "${PROBES_ONLY:-0}" = 1 ] && [ -s "$X/main.txt" ]; then sha=$(cat "$X/COMMIT"); rm -rf "$X/test"; mkdir -p "$X/test"
else
rm -rf "$X"; mkdir -p "$X/main" "$X/test"
sha=$(git -C "$WT" rev-parse "$COMMIT") || exit 2
git -C "$WT" archive "$sha" src/main/java src/test/java src/main/resources src/test/resources | tar -x -C "$X"
echo "$sha" > "$X/COMMIT"
# PATCH=<file> (2026-10-18): temporary instrumentation applied to the export only (the worktree is never patched).
if [ -n "${PATCH:-}" ]; then for p in $PATCH; do (cd "$X" && git apply -p1 "$p") || { echo "compile.sh: PATCH $p did not apply"; exit 2; }; done; echo "$PATCH" > "$X/PATCH"; fi
fi
XW=$(cygpath -m "$X")
libs="$WT/build/moddev/artifacts/neoforge-21.1.219-merged.jar;$G/org.jetbrains/annotations/24.1.0/7af6a669488450c4a07c2c3254e2151df42d7d04/annotations-24.1.0.jar;$(cat "$WT/build/moddev/fluidGameTestServerLegacyClasspath.txt" "$WT/build/moddev/mcpClientLegacyClasspath.txt" | tr -d '\r' | tr '\n' ';')"
libs=$(printf '%s' "$libs" | tr '\' '/')
printf -- '-cp "%s"\n' "$libs" > "$X/main-cp.txt"
printf -- '-cp "%s;%s;%s" -sourcepath "%s"\n' "$XW/main" "$libs" "$JUNIT" "$XW/src/test/java" > "$X/test-cp.txt"
(cd "$X" && find src/main/java -name '*.java') | sed "s|^|$XW/|" > "$X/main.txt"
shopt -s nullglob; for p in "$HERE"/probes/*.java ${EXTRA_PROBES:-}; do cygpath -m "$p"; done > "$X/test.txt"
if [ "${PROBES_ONLY:-0}" = 1 ] && [ -s "$X/main.txt" ]; then m=0; else
"$JDK/javac" -J-Xmx6g -nowarn -encoding UTF-8 -proc:none -d "$XW/main" @"$XW/main-cp.txt" @"$XW/main.txt" > "$X/main.log" 2>&1; m=$?; fi
t=skipped
if [ $m -eq 0 ]; then "$JDK/javac" -J-Xmx6g -nowarn -encoding UTF-8 -proc:none -implicit:class -d "$XW/test" @"$XW/test-cp.txt" @"$XW/test.txt" > "$X/test.log" 2>&1; t=$?; fi
echo "compile.sh label=$LABEL commit=$sha main=$m probes=$t"
[ $m -eq 0 ] || grep -E "error|\.java:[0-9]+" "$X/main.log" | head -20
[ "$t" = 0 ] || [ "$t" = skipped ] || grep -E "error|\.java:[0-9]+" "$X/test.log" | head -30
