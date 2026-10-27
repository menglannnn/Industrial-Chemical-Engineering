#!/usr/bin/env bash
# Batch 2026-10-21-floating-roof-tanks, in-game check (adapted from tools/custom-equipment-rendering-poc/new-world.sh):
# from the title screen of the runMcpClient lane, create a creative superflat world (no structures), then run setup.txt.
# Run from the worktree root: bash <this folder>/new-world.sh
set -euo pipefail
T=D:/Minecraft/Modding/1.21/CreateChemE/tools/floating-roof-tanks/in-game
B="node $T/bridge.js"
$B wait-ready 300 >/dev/null
$B enter_control_mode >/dev/null
$B click_button_index '{"index":0}' >/dev/null      # Singleplayer
sleep 2
if [ -n "$(ls run/mcp-client/saves 2>/dev/null)" ]; then
  $B click_button_index '{"index":2}' >/dev/null    # SelectWorldScreen: Create New World
  sleep 2
fi
$B click_button_index '{"index":5}' >/dev/null      # Game Mode: Survival -> Hardcore
$B click_button_index '{"index":5}' >/dev/null      # -> Creative (commands on)
$B switch_tab '{"index":1}' >/dev/null
$B click_button_index '{"index":3}' >/dev/null      # World Type -> Superflat
$B click_button_index '{"index":8}' >/dev/null      # Generate Structures off
$B switch_tab '{"index":0}' >/dev/null
$B shot "$T/shots/last-create-world.png" >/dev/null
$B click_button_index '{"index":1}' >/dev/null      # Create New World
$B wait-world 120
$B cmds $T/setup.txt >/dev/null
echo "world ready"
