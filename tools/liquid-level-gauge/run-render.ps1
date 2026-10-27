param([string]$SourceRoot='D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/liquid-gauge-accessory')
$ErrorActionPreference='Stop'
$taskOutput=Join-Path $SourceRoot 'tmp/level-gauge'
$taskJdk='C:/Program Files/Java/jdk-21.0.11/bin'
$taskGson=(Get-ChildItem -LiteralPath 'C:/Users/wormz/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.10.1' -Recurse -Filter 'gson-2.10.1.jar' | Select-Object -First 1).FullName
New-Item -ItemType Directory -Force (Join-Path $taskOutput 'render-classes'),(Join-Path $taskOutput 'render') | Out-Null
& "$taskJdk/javac.exe" -encoding UTF-8 -cp "$taskGson;$SourceRoot/build/classes/java/main" -d "$taskOutput/render-classes" "$PSScriptRoot/LevelGaugeRender.java" "$PSScriptRoot/GaugePalette.java"
if($LASTEXITCODE -ne 0){throw 'Render compilation failed'}
$taskClassPath="$taskGson;$taskOutput/render-classes;$SourceRoot/build/classes/java/main;$SourceRoot/build/resources/main"
Push-Location $SourceRoot
try {
    foreach($taskCase in @(@('bare','bare',0),@('ghost','ghost',0),@('filled','installed',1.8),@('empty','installed',0))){
        & "$taskJdk/java.exe" '-Djava.awt.headless=true' "-DlevelGaugeState=$($taskCase[1])" -cp $taskClassPath com.wormzjl.icheme.science.equipment.LevelGaugeRender TANK 4 4 0 $taskCase[2] "$taskOutput/render/$($taskCase[0])" low,side
        if($LASTEXITCODE -ne 0){throw 'Native preview failed'}
    }
}finally{Pop-Location}
& 'C:/Users/wormz/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' "$PSScriptRoot/assemble.py" $taskOutput
