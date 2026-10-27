#!/usr/bin/env bash
# onejvm.sh <label> <commit> <probes> <selector> [-Dkey=value ...]: the index lane in one JVM per commit (F1/F2 bisect,
# 2026-10-16). <probes>: a probe directory, or probe files separated by commas. Exports <commit> with git archive into
# /tmp/testworld-<label>/ (reused when it already holds that commit, its compiled main classes too) and starts one JVM
# (-Xshare:off -Xmx6g) that compiles
# main and the probes in-process (OneRun.java, source-file mode) and runs the selector through M3Run. Refuses while a
# Minecraft dev client runs. Classpath: the worktree's build/moddev libraries, as compile.sh and run.sh.
set -u
LABEL=${1:?label}; COMMIT=${2:?commit}; PROBES=${3:?probes}; SELECTOR=${4:?selector}; shift 4
WT=${WT:-D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/coolprop-multiphase-thermo-37f6b0}
HERE=$(cd "$(dirname "$0")" && pwd)
JDK="C:/Program Files/Java/jdk-21.0.11/bin"
G=C:/Users/wormz/.gradle/caches/modules-2/files-2.1
JUNIT="$G/org.junit.jupiter/junit-jupiter-api/5.11.4/308315b28e667db4091b2ba1f7aa220d1ddadb97/junit-jupiter-api-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-engine/5.11.4/dc10ec209623986a68ea07f67cdc7d2a65a60355/junit-jupiter-engine-5.11.4.jar;$G/org.junit.jupiter/junit-jupiter-params/5.11.4/e4c86fbe2a39c60c6b87260ef7f7e7c1a1906481/junit-jupiter-params-5.11.4.jar;$G/org.junit.platform/junit-platform-commons/1.11.4/8898eea3ed0da2641548d602c3e308804f166303/junit-platform-commons-1.11.4.jar;$G/org.junit.platform/junit-platform-engine/1.11.4/21f61b123ad6ac8f7e73971bff3a096c8d8e1cd0/junit-platform-engine-1.11.4.jar;$G/org.junit.platform/junit-platform-launcher/1.11.4/3d83c201899d8c5e74e1a5d628eab900342a0e48/junit-platform-launcher-1.11.4.jar;$G/org.opentest4j/opentest4j/1.3.0/152ea56b3a72f655d4fd677fc0ef2596c3dd5e6e/opentest4j-1.3.0.jar;$G/org.apiguardian/apiguardian-api/1.1.2/a231e0d844d2721b0fa1b238006d15c6ded6842a/apiguardian-api-1.1.2.jar"
live=$(powershell.exe -NoProfile -Command "@(Get-CimInstance Win32_Process -Filter \"Name='java.exe' OR Name='javaw.exe'\" | Where-Object { \$_.CommandLine -match 'fml.modFolders' }).Count" | tr -d '\r')
if [ "${live:-0}" != "0" ]; then echo "onejvm.sh: a Minecraft dev client is running ($live); wait until it has exited"; exit 3; fi
[ -e /tmp/testworld-launcher/M3Run.class ] || { echo "onejvm.sh: /tmp/testworld-launcher/M3Run.class missing (run.sh builds it)"; exit 2; }
X=/tmp/testworld-$LABEL
sha=$(git -C "$WT" rev-parse "$COMMIT") || exit 2
if [ -s "$X/COMMIT" ] && [ "$(cat "$X/COMMIT")" = "$sha" ] && [ -d "$X/src" ]; then echo "onejvm.sh: reusing the export of $sha in $X"
else rm -rf "$X"; mkdir -p "$X/main" "$X/test"; git -C "$WT" archive "$sha" src/main/java src/test/java src/main/resources src/test/resources | tar -x -C "$X"; echo "$sha" > "$X/COMMIT"; fi
rm -rf "$X/test"; mkdir -p "$X/main" "$X/test"
XW=$(cygpath -m "$X")
libs="$WT/build/moddev/artifacts/neoforge-21.1.219-merged.jar;$G/org.jetbrains/annotations/24.1.0/7af6a669488450c4a07c2c3254e2151df42d7d04/annotations-24.1.0.jar;$(cat "$WT/build/moddev/fluidGameTestServerLegacyClasspath.txt" "$WT/build/moddev/mcpClientLegacyClasspath.txt" | tr -d '\r' | tr '\n' ';')"
libs=$(printf '%s' "$libs" | tr '\' '/')
printf -- '-cp "%s"\n' "$libs" > "$X/main-cp.txt"
printf -- '-cp "%s;%s;%s" -sourcepath "%s"\n' "$XW/main" "$libs" "$JUNIT" "$XW/src/test/java" > "$X/test-cp.txt"
(cd "$X" && find src/main/java -name '*.java') | sed "s|^|$XW/|" > "$X/main.txt"
if [ -d "$PROBES" ]; then for p in "$PROBES"/*.java; do cygpath -m "$p"; done; else IFS=',' read -r -a files <<< "$PROBES"; for p in "${files[@]}"; do cygpath -m "$p"; done; fi > "$X/test.txt"
cp="$XW/test;$XW/src/test/resources;$XW/main;$XW/src/main/resources;$libs;$JUNIT;$(cygpath -m /tmp/testworld-launcher)"
printf -- '-cp "%s"\n' "$cp" > "$X/run-cp.txt"
echo "onejvm.sh label=$LABEL commit=$sha probes=$PROBES selector=$SELECTOR $*"
"$JDK/java" -Xshare:off -Xmx6g "$@" @"$XW/run-cp.txt" "$(cygpath -m "$HERE/OneRun.java")" "$XW" "$SELECTOR"
