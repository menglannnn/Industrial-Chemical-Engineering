#!/usr/bin/env bash
# Kit in-game check: each stdin line "name tx ty tz dist yaw pitch" writes k-view-<name>.txt and shots/k-<name>.png.
# Run from the worktree root with MSYS_NO_PATHCONV=1. WAIT (s) is the pause before each capture (default 6).
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name tx ty tz d yaw pitch; do
  [ -z "$name" ] && continue
  node $D/view.js $D/k-view-$name.txt $tx $ty $tz $d $yaw $pitch >/dev/null
  $B cmds $D/k-view-$name.txt >/dev/null
  sleep "${WAIT:-6}"
  $B shot "$(pwd -W)/$D/shots/k-$name.png" >/dev/null
  echo "shot k-$name"
done
