param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $PSScriptRoot 'classes'
$taskRenders=Join-Path $PSScriptRoot 'renders'
New-Item -ItemType Directory -Force $taskClasses,$taskRenders | Out-Null
$taskGson=(Get-ChildItem -LiteralPath 'C:/Users/wormz/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1' -Recurse -Filter 'gson-2.10.1.jar' | Select-Object -First 1).FullName
$taskClassPath="$taskGson;$taskClasses;$SourceRoot/build/classes/java/main;$SourceRoot/build/resources/main"
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$taskGson;$SourceRoot/build/classes/java/main" -d $taskClasses "$PSScriptRoot/GaugeEnclosureRender.java" "$PSScriptRoot/GaugePalette.java"
if($LASTEXITCODE -ne 0){throw 'Preview compilation failed'}
Push-Location $SourceRoot
try {
    $taskCases=@(
        @{Name='small-filled';Size=4;Height=4;Level=1.8;Region='full';Views='low,side'},
        @{Name='small-empty';Size=4;Height=4;Level=0;Region='full';Views='front,side'},
        @{Name='medium-top';Size=20;Height=12;Level=6.6;Region='top';Views='low,side'},
        @{Name='medium-bottom';Size=20;Height=12;Level=6.6;Region='bottom';Views='low,side'},
        @{Name='large-top';Size=40;Height=24;Level=13.8;Region='top';Views='low,side'},
        @{Name='large-bottom';Size=40;Height=24;Level=13.8;Region='bottom';Views='low,side'}
    )
    foreach($taskCase in $taskCases){
        & "$taskJdk/java.exe" -Xmx2g '-Djava.awt.headless=true' "-DgaugeRegion=$($taskCase.Region)" -cp $taskClassPath com.wormzjl.icheme.science.equipment.GaugeEnclosureRender TANK $taskCase.Size $taskCase.Height 0 $taskCase.Level "$taskRenders/$($taskCase.Name)" $taskCase.Views
        if($LASTEXITCODE -ne 0){throw "Preview failed: $($taskCase.Name)"}
    }
}finally{Pop-Location}
& 'C:/Users/wormz/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' "$PSScriptRoot/assemble.py"
if($LASTEXITCODE -ne 0){throw 'Sheet assembly failed'}
