#!/usr/bin/env bash
# Compat recheck: after "Play Selected World", poll every 0.5 s until no screen is open, then shoot the 2.0 m cyclone's inlet side at once.
# Placement-relative cameras of the refreshed cyclone (placed at 0 -60 185) = the fresh f20 cameras moved 40 blocks south (-z).
set -euo pipefail
D=tools/new-units/in-game
B="node tools/custom-equipment-rendering-poc/bridge.js"
for i in $(seq 1 600); do
  if $B get_screen_buttons 2>/dev/null | grep -q '"no screen"'; then break; fi
  sleep 0.5
done
echo "$(date +%H:%M:%S.%3N) in world"
WAIT=2 bash $D/recheck-shoot.sh <<'V'
t0-inlet-se 5.98 -44.35 187.48 135 10
V
WAIT=2 bash $D/recheck-shoot.sh <<'V'
t0-iso 7.74 -41.12 177.76 45 30
t0-side 6.47 -45.01 182.00 90 5
V
