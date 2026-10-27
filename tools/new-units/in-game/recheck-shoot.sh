#!/usr/bin/env bash
# Compat recheck (2026-10-18): each stdin line "name x y z yaw pitch" (a spectator tp) writes recheck-view-<name>.txt and recheck-<name>.png.
# Run from the worktree root with MSYS_NO_PATHCONV=1. WAIT (s) is the pause before each capture (default 4). Prints the local time of each capture.
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name x y z yaw pitch; do
  [ -z "$name" ] && continue
  printf 'gamemode spectator\ntp @s %s %s %s %s %s\n' "$x" "$y" "$z" "$yaw" "$pitch" > $D/recheck-view-$name.txt
  $B cmds $D/recheck-view-$name.txt >/dev/null
  sleep "${WAIT:-4}"
  $B shot "$(pwd -W)/$D/recheck-$name.png" >/dev/null
  echo "$(date +%H:%M:%S.%3N) shot recheck-$name"
done
