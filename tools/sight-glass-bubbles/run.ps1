param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $PSScriptRoot 'classes'
$taskOutput='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-25-sight-glass-bubbles'
New-Item -ItemType Directory -Force $taskClasses | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskClasses "$PSScriptRoot/BubblePreview.java"
if($LASTEXITCODE -ne 0){throw 'Preview compilation failed'}
Push-Location $SourceRoot
try {
    & "$taskJdk/java.exe" '-Djava.awt.headless=true' -cp "$taskClasses;$SourceRoot/build/classes/java/main" com.wormzjl.icheme.runtime.fluid.BubblePreview $taskOutput
    if($LASTEXITCODE -ne 0){throw 'Preview failed'}
}finally{Pop-Location}
