#!/bin/bash
# render.sh [out dir]: compiles StorageRender against the worktree's compiled classes (./gradlew compileJava first) and
# renders three tanks and three spheres to <out dir> (default build/tmp/storage-render). Run from the worktree root.
set -eu
export MSYS_NO_PATHCONV=1  # Git Bash would rewrite the ;-separated class path
cd "$(dirname "$0")/../.."
here=tools/storage-vessel-renders
out=${1:-build/tmp/storage-render}
classes=build/tmp/storage-render-classes
mkdir -p "$classes" "$out"
javac -d "$classes" -cp "build/classes/java/main" "$here/StorageRender.java"
CP="$classes;build/classes/java/main;build/resources/main"
run() { java -Xshare:off -Xmx2g -cp "$CP" com.wormzjl.icheme.science.equipment.StorageRender "$@"; }
# kind size length support level name: the least, a mid and the largest tank (the roof at 60 % of its fill height), the
# least sphere on its highest legs, the concept sheet's and the largest.
run TANK 4 4 0 1.8 "$out/tank-4x4" iso,front,top,iso3
run TANK 20 12 0 6.6 "$out/tank-20x12" iso,front,top,iso3
run TANK 40 24 0 13.8 "$out/tank-40x24" iso,front,top,iso3
run SPHERE 4 1 8 0 "$out/sphere-4-on-8" iso,front,top,iso3
run SPHERE 14 1 2 0 "$out/sphere-14-on-2" iso,front,top,iso3
run SPHERE 24 1 2 0 "$out/sphere-24-on-2" iso,front,top,iso3
# The ladder forms under the stair sizes (owner change 2026-10-21, tank D < 6, sphere D < 10): a tall small tank with two
# rests, a sphere with its rest on posts, and the first tank with the wound stair.
run TANK 5 22 0 12 "$out/tank-5x22" iso,front,top,iso3
run SPHERE 9 1 8 0 "$out/sphere-9-on-8" iso,front,top,iso3
run TANK 6 8 0 3 "$out/tank-6x8" iso,iso3
