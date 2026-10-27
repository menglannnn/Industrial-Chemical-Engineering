#!/usr/bin/env bash
# Compat in-game check: each stdin line "name x y z yaw pitch" (a spectator tp) writes compat-view-<name>.txt and compat-<name>.png.
# Run from the worktree root with MSYS_NO_PATHCONV=1. WAIT (s) is the pause before each capture (default 6).
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name x y z yaw pitch; do
  [ -z "$name" ] && continue
  printf 'gamemode spectator\ntp @s %s %s %s %s %s\n' "$x" "$y" "$z" "$yaw" "$pitch" > $D/compat-view-$name.txt
  $B cmds $D/compat-view-$name.txt >/dev/null
  sleep "${WAIT:-6}"
  $B shot "$(pwd -W)/$D/compat-$name.png" >/dev/null
  echo "shot compat-$name"
done
