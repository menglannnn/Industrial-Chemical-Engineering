param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $PSScriptRoot 'classes'
$taskRenders='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-25-accessory-refinements/renders'
New-Item -ItemType Directory -Force $taskClasses,$taskRenders | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskClasses "$PSScriptRoot/AccessoryRefinementRender.java"
if($LASTEXITCODE -ne 0){throw 'Render tool compilation failed'}
Push-Location $SourceRoot
try {& "$taskJdk/java.exe" -Xmx3g -cp "$taskClasses;$SourceRoot/build/classes/java/main" com.wormzjl.icheme.science.equipment.AccessoryRefinementRender $taskRenders
if($LASTEXITCODE -ne 0){throw 'Render/audit failed'}} finally {Pop-Location}
