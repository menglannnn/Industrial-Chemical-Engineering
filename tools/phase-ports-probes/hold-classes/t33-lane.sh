#!/usr/bin/env bash
# The thread lane protocol of 2026-10-13 (markers in claude-threads/lanes/), for t33 (the close's in-game check C3: holds alone for the whole client session).
# Usage: t33-lane.sh lane <command...>   waits while another thread holds alone.lock.d, holds lane-t33.running while the command runs
#        t33-lane.sh alone <command...>  takes alone.lock.d, waits until no other lane file exists, runs, releases
# XOPTS: -D switches passed to every JVM through JAVA_TOOL_OPTIONS.
set -u
L=D:/Minecraft/Modding/claude-threads/lanes; me="$L/lane-t33.running"; mode=$1; shift
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t33.running | wc -l; }
foreign_lock(){ [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t33 ]; }
if [ "$mode" = alone ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do sleep 60; done; echo t33 > "$L/alone.lock.d/owner"
  touch "$me"; while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while foreign_lock; do sleep 60; done
  touch "$me"
fi
opts="${XOPTS:-}"
[ -n "$opts" ] && export JAVA_TOOL_OPTIONS="$opts"
echo "t33-lane: $mode start $(date +%H:%M:%S) other lanes=$(others) opts=${opts:-none}"
export THREADS=${THREADS:-8}
"$@"; rc=$?
rm -f "$me"; [ "$mode" = alone ] && rm -r "$L/alone.lock.d"
echo "t33-lane: $mode end $(date +%H:%M:%S) rc=$rc"
exit $rc
