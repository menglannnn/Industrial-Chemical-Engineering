#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 (two Gradle lanes on the machine, markers in claude-threads/lanes/), for t16.
# Usage: t16-lane.sh lane <command...>   waits for a free lane, holds lane-t16.running while the command runs
#        t16-lane.sh alone <command...>  takes alone.lock.d, waits until no other lane file exists, runs, releases
# Prints how many lane files existed when the run started (THREADS for a campaign: 4 if another lane runs, else 8)
# into $LANE_INFO when set.
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t16.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t16.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t16 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t16 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while [ "$(others)" -gt 1 ] || foreign_lock; do sleep 60; done
  touch "$me"
fi
n=$(others); [ -n "${LANE_INFO:-}" ] && echo "$n" > "$LANE_INFO"
echo "t16-lane: $mode start $(date +%H:%M:%S) other lanes=$n"
if [ "$mode" != alone ] && [ -z "${THREADS:-}" ]; then if [ "$n" -gt 0 ]; then export THREADS=4; else export THREADS=8; fi; fi
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t16-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
