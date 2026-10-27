param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$handleJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$handleClasspath="$PSScriptRoot/classes;$SourceRoot/build/classes/java/main"
$handleRenders='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-24-cutoff-handle-flicker/renders'
& "$handleJdk/javac.exe" -cp $handleClasspath -d "$PSScriptRoot/classes" "$PSScriptRoot/FlickeringShutoffValveGeometry.java" "$PSScriptRoot/HandleTipAudit.java" "$PSScriptRoot/HandleTipRender.java"
if($LASTEXITCODE -ne 0){throw 'Offline handle tool compilation failed'}
& "$handleJdk/java.exe" -Xmx512m -cp $handleClasspath com.wormzjl.icheme.science.equipment.HandleTipAudit *> "$PSScriptRoot/sweep.log"
if($LASTEXITCODE -ne 0){throw 'Handle tip sweep failed'}
& "$handleJdk/java.exe" '-Djava.awt.headless=true' -cp $handleClasspath HandleTipRender $handleRenders *> "$PSScriptRoot/renders.log"
if($LASTEXITCODE -ne 0){throw 'Handle render failed'}
Get-Content "$PSScriptRoot/sweep.log"
