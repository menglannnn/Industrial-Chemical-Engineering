#!/bin/bash
# One unattended autopilot lane: lane.sh <label> <sheen> <warmup s> <measure s> <save> <camera file> [shader pack zip]
# Copies Sodium + Sodium Extra + EntityCulling + FerriteCore + ModernFix (and Iris with a pack) into run/mcp-client,
# sets the sheen, runs the FrameTimeProbe autopilot, keeps the log lines and the shot, and removes everything it added.
# The bridge reads the real window handle through src/mcpCompat WindowHandleMixin (it faulted under Sodium without it).
set -u
cd "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d"
OUT=build/tmp/shader-lanes
MODS="C:/Users/wormz/curseforge/minecraft/Instances/Simple TFC/mods"
label=$1 sheen=$2 warmup=$3 measure=$4 save=$5 camera=$6 pack=${7:-}
jars=(sodium-neoforge-0.8.12+mc1.21.1.jar sodium-extra-neoforge-0.9.3+mc1.21.1.jar entityculling-neoforge-1.10.5-mc1.21.1.jar ferritecore-7.0.3-neoforge.jar modernfix-neoforge-5.27.15+mc1.21.1.jar)
[ -n "$pack" ] && jars+=(iris-neoforge-1.8.14-beta.1+mc1.21.1.jar)
ls run/mcp-client/config > $OUT/config-before.txt
for j in "${jars[@]}"; do cp "$MODS/$j" run/mcp-client/mods/; done
if [ -n "$pack" ]; then
  mkdir -p run/mcp-client/shaderpacks; cp "$pack" run/mcp-client/shaderpacks/
  printf 'enableShaders=true\nshaderPack=%s\n' "$(basename "$pack")" > run/mcp-client/config/iris.properties
fi
sed -i "s/^\tmetallicSheen = .*/\tmetallicSheen = $sheen/" run/mcp-client/config/createcheme-common.toml
mkdir -p run/mcp-client/fps-autopilot
printf 'save=%s\nlabel=%s\nwarmup=%s\nmeasure=%s\n' "$save" "$label" "$warmup" "$measure" > run/mcp-client/fps-autopilot/autopilot.properties
cp "$camera" run/mcp-client/fps-autopilot/camera.txt
echo "# none" > run/mcp-client/fps-autopilot/columns.txt; echo "# none" > run/mcp-client/fps-autopilot/empty.txt
JAVA_OPTS=-Xshare:off timeout 900 ./gradlew.bat runMcpClient --no-configuration-cache --offline --console=plain > $OUT/client-$label.log 2>&1
echo "exit $?" >> $OUT/client-$label.log
grep -aE "FPS_PROBE|FPS_AUTOPILOT|Exception|ERROR|A fatal error" $OUT/client-$label.log | grep -v "^\s*at " | head -40 > $OUT/summary-$label.txt
mv run/mcp-client/screenshots/fps-$label-columns.png $OUT/ 2>/dev/null
for h in run/mcp-client/hs_err_pid*.log; do [ -f "$h" ] && mv "$h" $OUT/hs_err-$label.log; done
# Clean up: the jars, the pack, the autopilot, every config file the lane created.
for j in "${jars[@]}"; do rm -f "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d/run/mcp-client/mods/$j"; done
rm -rf "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d/run/mcp-client/shaderpacks" "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d/run/mcp-client/fps-autopilot"
ls run/mcp-client/config > $OUT/config-after.txt
comm -13 <(sort $OUT/config-before.txt) <(sort $OUT/config-after.txt) | while read -r f; do
  [ -n "$f" ] && rm -rf "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d/run/mcp-client/config/$f"
done
echo "lane $label done: $(tail -1 $OUT/client-$label.log)"; head -12 $OUT/summary-$label.txt
