#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 (markers in claude-threads/lanes/), for t22 (group L, lane 1, 8 workers).
# Usage: t22-lane.sh lane <command...>   waits while another thread holds alone.lock.d, holds lane-t22.running while the command runs
#        t22-lane.sh alone <command...>  takes alone.lock.d, waits until no other lane file exists, runs, releases
# START: the junction start form (A284), passed to every JVM as -Dcreatecheme.junctionStart through JAVA_TOOL_OPTIONS;
# XOPTS: further -D switches appended to JAVA_TOOL_OPTIONS.
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t22.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t22.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t22 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t22 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while foreign_lock; do sleep 60; done
  touch "$me"
fi
opts="${XOPTS:-}"; [ -n "${START:-}" ] && opts="-Dcreatecheme.junctionStart=$START $opts"
[ -n "$opts" ] && export JAVA_TOOL_OPTIONS="$opts"
echo "t22-lane: $mode start $(date +%H:%M:%S) other lanes=$(others) opts=${opts:-none}"
export THREADS=${THREADS:-8}
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t22-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
