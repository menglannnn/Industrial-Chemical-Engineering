#!/usr/bin/env bash
# Recheck shots: each stdin line "name tx ty tz dist yaw pitch" writes r-view-<name>.txt and shots/r-<name>.png.
# Run from the worktree root with MSYS_NO_PATHCONV=1. WAIT (s) is the pause before each capture (default 6).
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name tx ty tz d yaw pitch; do
  [ -z "$name" ] && continue
  node $D/view.js $D/r-view-$name.txt $tx $ty $tz $d $yaw $pitch >/dev/null
  $B cmds $D/r-view-$name.txt >/dev/null
  sleep "${WAIT:-6}"
  $B shot "$(pwd -W)/$D/shots/r-$name.png" >/dev/null
  echo "shot r-$name"
done
