#!/usr/bin/env bash
# open.sh "<tp args>" <shot name>: close any screen, teleport, right-click, wait 1.5 s, flattened screenshot.
B="node $TEMP/minecraft-mcp-skill/scripts/bridge.js"
export MSYS_NO_PATHCONV=1
OUT=D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/remove-agents-attribution-line-3d0c97/tools/phase-ports-probes/gui-layout
$B close_screen >/dev/null
$B chat "/tp @s $1" >/dev/null
sleep 1
$B right_click >/dev/null
sleep 2.5
$B get_screen_buttons | head -c 60; echo
$B shot "$OUT/$2" | tail -1
