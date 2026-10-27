#!/bin/bash
cd "D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/custom-equipment-rendering-805e7d"
L=build/tmp/shader-lanes
run() { while [ "$(powershell -NoProfile -File $L/busy.ps1)" != "0" ]; do echo "waiting for another Gradle test run ($(date +%H:%M))"; sleep 60; done; bash $L/lane.sh "$@"; }
run iris-compl-reimagined 0.2 40 2 shader-check-2026-10-17 $L/camera.txt "C:/Users/wormz/curseforge/minecraft/Instances/All The Mods - Gravitas²_/shaderpacks/ComplementaryReimagined_r5.7.1.zip"
run iris-compl-unbound 0.2 40 2 shader-check-2026-10-17 $L/camera.txt "C:/Users/wormz/curseforge/minecraft/Instances/All The Mods - Gravitas²_/shaderpacks/ComplementaryUnbound_r5.7.1.zip"
run iris-bliss 0.2 40 2 shader-check-2026-10-17 $L/camera.txt "C:/Users/wormz/curseforge/minecraft/Instances/All The Mods - Gravitas²_/shaderpacks/Bliss-Shader-main.zip"
run iris-makeup 0.2 40 2 shader-check-2026-10-17 $L/camera.txt "C:/Users/wormz/curseforge/minecraft/Instances/All The Mods - Gravitas²_/shaderpacks/MakeUp-UltraFast-9.4b.zip"
run iris-photon 0.2 40 2 shader-check-2026-10-17 $L/camera.txt "C:/Users/wormz/curseforge/minecraft/Instances/All The Mods - Gravitas²_/shaderpacks/photon-main.zip"
echo "all lanes done $(date +%H:%M)"
