param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $PSScriptRoot 'classes'
$taskBaseline=Join-Path $PSScriptRoot 'baseline-classes'
$taskOutput='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-25-sight-glass-ratio-seams-sound'
New-Item -ItemType Directory -Force $taskClasses,$taskBaseline | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskClasses "$PSScriptRoot/BubblePreview.java"
if($LASTEXITCODE -ne 0){throw 'Preview compilation failed'}
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskBaseline "$PSScriptRoot/baseline-src/SightGlassGeometry.java"
if($LASTEXITCODE -ne 0){throw 'Baseline geometry compilation failed'}
Push-Location $SourceRoot
try {
    & "$taskJdk/java.exe" '-Djava.awt.headless=true' -cp "$taskClasses;$SourceRoot/build/classes/java/main" com.wormzjl.icheme.runtime.fluid.BubblePreview $taskOutput
    if($LASTEXITCODE -ne 0){throw 'Current preview failed'}
    & "$taskJdk/java.exe" '-Djava.awt.headless=true' -cp "$taskBaseline;$taskClasses;$SourceRoot/build/classes/java/main" com.wormzjl.icheme.runtime.fluid.BubblePreview "$taskOutput/before" geometry-only
    if($LASTEXITCODE -ne 0){throw 'Baseline preview failed'}
}finally{Pop-Location}
