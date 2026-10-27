#!/usr/bin/env bash
# Kit in-game check: reshoot the earlier check's views. Each stdin line "name" runs view-<name>.txt and writes
# shots/k-<name>.png, one to one with shots/<name>.png. Run from the worktree root with MSYS_NO_PATHCONV=1.
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
while read -r name; do
  [ -z "$name" ] && continue
  $B cmds $D/view-$name.txt >/dev/null
  sleep "${WAIT:-6}"
  $B shot "$(pwd -W)/$D/shots/k-$name.png" >/dev/null
  echo "shot k-$name"
done
