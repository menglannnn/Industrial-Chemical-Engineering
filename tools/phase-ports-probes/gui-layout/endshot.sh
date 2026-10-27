#!/usr/bin/env bash
# endshot.sh <shot name> [framebuffer x y]: wheel the rail down 10 times (3 steps of 10 GUI px each), flattened screenshot.
B="node $TEMP/minecraft-mcp-skill/scripts/bridge.js"
OUT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/remove-agents-attribution-line-3d0c97/tools/phase-ports-probes/gui-layout
X=${2:-150}; Y=${3:-340}
for i in 1 2 3 4 5 6 7 8 9 10; do $B direct_scroll "{\"mouseX\":$X,\"mouseY\":$Y,\"delta\":-1}" >/dev/null; done
sleep 2
$B shot "$OUT/$1" | tail -1
