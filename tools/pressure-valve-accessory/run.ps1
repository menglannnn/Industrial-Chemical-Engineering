param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$valveJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$valveGson=(Get-ChildItem -LiteralPath 'C:/Users/wormz/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1' -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
$valveClasspath="$PSScriptRoot/classes;$SourceRoot/build/classes/java/main;$valveGson"
$valveRenderRoot='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-24-pressure-valve-accessory/renders'
& "$valveJdk/javac.exe" -cp $valveClasspath -d "$PSScriptRoot/classes" "$PSScriptRoot/ValveAccessoryRender.java" "$PSScriptRoot/BaselineShutoffValveGeometry.java" "$PSScriptRoot/ValveIconRender.java"
if($LASTEXITCODE -ne 0){throw 'Renderer compilation failed'}
& "$valveJdk/java.exe" '-Djava.awt.headless=true' -cp $valveClasspath ValveAccessoryRender $valveRenderRoot *> "$PSScriptRoot/renders.log"
if($LASTEXITCODE -ne 0){throw 'Geometry render failed'}
& "$valveJdk/java.exe" '-Djava.awt.headless=true' -cp $valveClasspath ValveIconRender $SourceRoot $valveRenderRoot *> "$PSScriptRoot/icons.log"
if($LASTEXITCODE -ne 0){throw 'Inventory icon bounds failed'}
Get-Content "$PSScriptRoot/icons.log"
