param([string]$SourceRoot='C:/Users/wormz/.codex/worktrees/pipeline-tool/CreateChemE')
$ErrorActionPreference='Stop'
$taskToolRoot=$PSScriptRoot
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskClasses=Join-Path $taskToolRoot 'classes'
$taskBaseline=Join-Path $taskToolRoot 'baseline-classes'
$taskRenders='D:/Minecraft/Modding/1.21/CreateChemE/documentation/2026-10-24-floating-roof-indicator-ports/renders'
New-Item -ItemType Directory -Force $taskClasses,$taskBaseline,$taskRenders | Out-Null
$taskGson=(Get-ChildItem -LiteralPath "C:/Users/wormz/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1" -Recurse -Filter "gson-2.10.1.jar" | Select-Object -First 1).FullName
$taskClassPath="$taskGson;$taskClasses;$SourceRoot/build/classes/java/main;$SourceRoot/build/resources/main"
Push-Location $SourceRoot
try {
    & "$taskJdk/javac.exe" -encoding UTF-8 -cp "$taskGson;$SourceRoot/build/classes/java/main" -d $taskClasses "$taskToolRoot/TankIndicatorAudit.java" "$taskToolRoot/TankIndicatorRender.java" "$taskToolRoot/GaugePalette.java"
    if($LASTEXITCODE -ne 0){throw 'Tool compilation failed'}
    & "$taskJdk/javac.exe" -encoding UTF-8 -cp "$SourceRoot/build/classes/java/main" -d $taskBaseline "$taskToolRoot/baseline/TankDesign.java"
    if($LASTEXITCODE -ne 0){throw 'Baseline compilation failed'}
    & "$taskJdk/java.exe" -Xmx2g -cp "$taskBaseline;$taskClassPath" com.wormzjl.icheme.science.equipment.TankIndicatorAudit baseline "$taskToolRoot/baseline-nodes.csv"
    if($LASTEXITCODE -ne 0){throw 'Baseline node audit failed'}
    & "$taskJdk/java.exe" -Xmx3g -cp $taskClassPath com.wormzjl.icheme.science.equipment.TankIndicatorAudit current "$taskToolRoot/current-nodes.csv" "$taskToolRoot/baseline-nodes.csv"
    if($LASTEXITCODE -ne 0){throw 'Current audit failed'}
    foreach($taskSize in @(@(4,4),@(6,8),@(20,12),@(40,24))){
        $taskDiameter=$taskSize[0];$taskHeight=$taskSize[1];$taskLevel=.6*($taskHeight-1)
        & "$taskJdk/java.exe" -Xmx2g -cp $taskClassPath com.wormzjl.icheme.science.equipment.TankIndicatorRender TANK $taskDiameter $taskHeight 0 $taskLevel "$taskRenders/tank-$taskDiameter" front,back,top,low
        if($LASTEXITCODE -ne 0){throw 'Whole-tank render failed'}
    }
    & "$taskJdk/java.exe" -Xmx2g '-DgaugeCloseup=true' -cp $taskClassPath com.wormzjl.icheme.science.equipment.TankIndicatorRender TANK 20 12 0 6.6 "$taskRenders/gauge-current" front,low,side,under
    if($LASTEXITCODE -ne 0){throw 'Gauge render failed'}
    & "$taskJdk/java.exe" -Xmx2g '-DgaugeCloseup=true' '-DbaselineBands=true' -cp "$taskBaseline;$taskClassPath" com.wormzjl.icheme.science.equipment.TankIndicatorRender TANK 20 12 0 6.6 "$taskRenders/gauge-baseline" front,low,side,under
    if($LASTEXITCODE -ne 0){throw 'Control render failed'}
} finally {Pop-Location}
