#!/bin/sh
# A flattened client screenshot: bash tmp/review-world/shot.sh <name> -> tmp/review-world/shots/<name>-f.png
W=$(pwd -W)
node tmp/w4/mcp.js screenshot_to_file "{\"path\":\"$W/tmp/review-world/shots/$1.png\"}" 2>&1 | grep -v minecraft-mcp | tail -2
"/c/Program Files/Java/jdk-21.0.11/bin/java.exe" -cp tmp/review-world/flatten Flatten "tmp/review-world/shots/$1.png" "tmp/review-world/shots/$1-f.png"
