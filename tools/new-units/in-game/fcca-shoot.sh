#!/usr/bin/env bash
# FCC form A in-game check (2026-10-18): each stdin line "name tx ty tz dist yaw pitch" (the eye looks at the target from dist
# blocks, view.js) writes fcca-view-<name>.txt and fcca-<name>.png. Run from the worktree root with MSYS_NO_PATHCONV=1.
# WAIT (s) is the pause before each capture (default 5). Prints the local time of each capture.
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name tx ty tz d yaw pitch; do
  [ -z "$name" ] && continue
  case "$name" in \#*) continue;; esac
  node $D/view.js $D/fcca-view-$name.txt $tx $ty $tz $d $yaw $pitch >/dev/null
  $B cmds $D/fcca-view-$name.txt >/dev/null
  sleep "${WAIT:-5}"
  $B shot "$(pwd -W)/$D/fcca-$name.png" >/dev/null
  echo "$(date +%H:%M:%S.%3N) shot fcca-$name"
done
