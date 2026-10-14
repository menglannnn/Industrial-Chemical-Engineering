#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 (two Gradle lanes on the machine, markers in claude-threads/lanes/), for t18 (lane 1, 8 workers).
# Usage: t18-lane.sh lane <command...>   waits while another thread holds alone.lock.d, holds lane-t18.running while the command runs
#        t18-lane.sh alone <command...>  takes alone.lock.d, waits until no other lane file exists, runs, releases
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t18.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t18.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t18 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t18 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while foreign_lock; do sleep 60; done
  touch "$me"
fi
echo "t18-lane: $mode start $(date +%H:%M:%S) other lanes=$(others)"
export THREADS=${THREADS:-8}
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t18-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
