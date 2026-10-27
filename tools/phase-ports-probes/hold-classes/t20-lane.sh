#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 (two Gradle lanes on the machine, markers in claude-threads/lanes/), for t20 (lane 1, 8 workers).
# Usage: t20-lane.sh lane <command...>   waits while another thread holds alone.lock.d, holds lane-t20.running while the command runs
#        t20-lane.sh alone <command...>  takes alone.lock.d, waits until no other lane file exists, runs, releases
# START: the junction start form (A284), passed to every JVM as -Dcreatecheme.junctionStart through JAVA_TOOL_OPTIONS.
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t20.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t20.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t20 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t20 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while foreign_lock; do sleep 60; done
  touch "$me"
fi
[ -n "${START:-}" ] && export JAVA_TOOL_OPTIONS="-Dcreatecheme.junctionStart=$START"
echo "t20-lane: $mode start $(date +%H:%M:%S) other lanes=$(others) start=${START:-default}"
export THREADS=${THREADS:-8}
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t20-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
