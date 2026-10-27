#!/usr/bin/env bash
# t18: traces minimized worlds to their first hold with t13's failing-row trace (src-t13/T13TraceProbeTest.trace; needs
# src/instr-t13-failing-rows-and-levers.patch in the working tree, applied with --reject --ignore-whitespace, its report hunk
# re-anchored by hand and the lever hunks left out). One Gradle invocation per world; the KEEP line is read from the file
# named by KEEPFILE_<world> or logs/ec06-contrast-column-<seed>.gradle.txt. EXTRA forwards switches; JAVA_TOOL_OPTIONS is
# passed through (-Dt18mode=...). From the worktree root. Usage: t18-trace.sh <logs/NN-tag> <d|r><seed> ...
set -u
H=tools/phase-ports-probes/hold-classes; p="$H/$1"; shift; mkdir -p "$p"
export JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" JAVA_OPTS=-Xshare:off
for world in "$@"; do
  d=${world:0:1}; seed=${world:1}; flag=false; [ "$d" = d ] && flag=true
  var="KEEPFILE_$world"; src="${!var:-$H/logs/ec06-contrast-column-$seed.gradle.txt}"
  keep=$(grep -h -E "seed=$seed KEEP" "$src" | tail -1 | sed 's/.* KEEP //' | tr -d '\r')
  ./gradlew --no-configuration-cache --init-script $H/t13.init.gradle test --tests '*T13TraceProbeTest.trace' -Pt13seeds=$seed "-Pt13keep=$keep" \
    -PholdFuzzDefaults=$flag -Pt13out="$PWD/$p" ${EXTRA:-} --console=plain > "$p-$world.gradle.txt" 2>&1
  echo "$world: $(grep -h 'T13_DONE' "$p-$world.gradle.txt" | cut -c1-200) $(grep -h -E 'error:|FAILED' "$p-$world.gradle.txt" | head -2)"
done
