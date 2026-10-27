param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $PSScriptRoot 'classes'
$taskOutput=Join-Path $PSScriptRoot 'preview'
New-Item -ItemType Directory -Force $taskClasses,$taskOutput | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskClasses "$PSScriptRoot/FlowPreview.java"
if($LASTEXITCODE -ne 0){throw 'Preview compilation failed'}
& "$taskJdk/java.exe" '-Djava.awt.headless=true' -cp "$taskClasses;$SourceRoot/build/classes/java/main" com.wormzjl.icheme.runtime.fluid.FlowPreview $taskOutput
if($LASTEXITCODE -ne 0){throw 'Native preview failed'}
& 'C:/Users/wormz/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' "$PSScriptRoot/assemble.py"
if($LASTEXITCODE -ne 0){throw 'Preview assembly failed'}
