#!/bin/bash
# render.sh <lane> <KIND> <size> <length> <support> <name> [views]
# Renders a unit with the lane's compiled classes (check.sh first) to build/tmp/units/<lane>/render/<name>-<view>.png.
# Views: front, back, left, right, top, iso (front left, above), iso2 (front right), iso3 (back left), low (eye height).
set -u
export MSYS_NO_PATHCONV=1  # Git Bash would rewrite the ;-separated class path
cd "$(dirname "$0")/../.."
lane=$1
CP="build/tmp/units/$lane/out;build/classes/java/main;build/resources/main"
java -Xshare:off -Xmx1536m -cp "$CP" com.wormzjl.createcheme.science.equipment.UnitRender "$2" "$3" "$4" "$5" "build/tmp/units/$lane/render/$6" "${7:-iso,front,right,top}"
