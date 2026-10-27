#!/bin/sh
# Runs one chat command in the dev client through the bridge: bash tmp/w4/cmd.sh "/tp @s 0 -60 0"
M="node tmp/w4/mcp.js"
$M open_chat '{}' >/dev/null 2>&1
$M paste_text "{\"text\":\"$1\"}" >/dev/null 2>&1
$M hotkey '{"keys":"enter"}' >/dev/null 2>&1
