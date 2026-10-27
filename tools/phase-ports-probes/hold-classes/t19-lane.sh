#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 for t19 (lane 2; markers in claude-threads/lanes/).
# Usage: t19-lane.sh lane <command...>   waits while another thread holds alone.lock.d, holds lane-t19.running; THREADS=4
#        t19-lane.sh alone <command...>  takes alone.lock.d (owner t19), waits until no other lane file exists, runs, releases
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t19.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t19.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t19 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t19 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while foreign_lock || [ "$(others)" -gt 1 ]; do sleep 60; done
  touch "$me"
  export THREADS=${THREADS:-4}
fi
echo "t19-lane: $mode start $(date +%H:%M:%S) other lanes=$(others)"
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t19-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
