#!/usr/bin/env bash
# t14 (2026-10-13): runs a command under the orchestrator's lane protocol (marker files in claude-threads/lanes/).
# Waits while another thread holds alone.lock.d, and while two other lanes run (AGENTS.md: at most two Gradle runs on the
# machine); marks lane-t14.running for the duration. With --alone: takes alone.lock.d (owner t14) and waits until no other
# lane runs. Usage: t14-lane.sh [--alone] <command...>
L="D:/Minecraft/Modding/claude-threads/lanes"
alone=0; [ "$1" = "--alone" ] && { alone=1; shift; }
others(){ ls "$L"/lane-*.running 2>/dev/null | grep -v lane-t14.running | wc -l; }
if [ $alone = 1 ]; then
  until mkdir "$L/alone.lock.d" 2>/dev/null; do [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" = t14 ] && break; sleep 60; done
  echo t14 > "$L/alone.lock.d/owner"
  while [ "$(others)" -gt 0 ]; do sleep 60; done
else
  while { [ -d "$L/alone.lock.d" ] && [ "$(cat "$L/alone.lock.d/owner" 2>/dev/null)" != t14 ]; } || [ "$(others)" -ge 2 ]; do sleep 60; done
fi
touch "$L/lane-t14.running"
"$@"; rc=$?
rm -f "$L/lane-t14.running"
[ $alone = 1 ] && rm -r "$L/alone.lock.d"
exit $rc
