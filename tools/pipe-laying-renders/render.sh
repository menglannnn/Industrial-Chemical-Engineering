#!/bin/bash
# render.sh [out dir]: compiles PipeLayingRender against the worktree's compiled classes (./gradlew compileJava first) and
# renders the rack exit scenes to <out dir> (default build/tmp/pipe-laying-render). Run from the worktree root.
set -eu
export MSYS_NO_PATHCONV=1  # Git Bash would rewrite the ;-separated class path
cd "$(dirname "$0")/../.."
here=tools/pipe-laying-renders
out=${1:-build/tmp/pipe-laying-render}
classes=build/tmp/pipe-laying-render-classes
mkdir -p "$classes" "$out"
javac -d "$classes" -cp "build/classes/java/main" "$here/PipeLayingRender.java"
CP="$classes;build/classes/java/main;build/resources/main"
run() { java -Xshare:off -Xmx2g -cp "$CP" com.wormzjl.icheme.science.equipment.PipeLayingRender "$@"; }
run tier "$out/tier" iso,iso2,front
run exits "$out/exits" iso,iso2,iso3,top,front
run lowered "$out/lowered" iso,iso2,front
run corner "$out/corner" iso,iso2,top
run widths "$out/widths" iso,top
run stacked "$out/stacked" iso,iso2
