param(
    [string]$SourceRoot = 'C:\Users\wormz\.codex\worktrees\pipeline-tool\CreateChemE',
    [string]$JavaRoot = 'C:\Program Files\Java\jdk-21.0.11',
    [string]$GradleCache = 'C:\Users\wormz\.gradle\caches'
)
$ErrorActionPreference = 'Stop'
$gson = (Get-ChildItem -LiteralPath "$GradleCache\modules-2\files-2.1\com.google.code.gson\gson\2.10.1" -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
$ejml = (Get-ChildItem -LiteralPath "$GradleCache\modules-2\files-2.1\org.ejml" -Recurse -Filter '*.jar' | Where-Object { $_.FullName -match '\\0\.44\.0\\' } | ForEach-Object { $_.FullName }) -join ';'
$taskClasspath = "$PSScriptRoot\classes;$SourceRoot\build\classes\java\main;$SourceRoot\build\resources\main;$gson;$ejml"
$renderRoot = 'D:\Minecraft\Modding\1.21\CreateChemE\documentation\2026-10-23-fluid-display-followups\renders'
& "$JavaRoot\bin\javac.exe" -cp $taskClasspath -d "$PSScriptRoot\classes" "$PSScriptRoot\GeneratorPhaseAudit.java" "$PSScriptRoot\AccessoryIconRender.java"
if ($LASTEXITCODE -ne 0) { throw 'Offline tool compilation failed' }
& "$JavaRoot\bin\java.exe" -Xmx1g -cp $taskClasspath GeneratorPhaseAudit
if ($LASTEXITCODE -ne 0) { throw 'Fresh-feed audit failed' }
& "$JavaRoot\bin\java.exe" '-Djava.awt.headless=true' -cp $taskClasspath AccessoryIconRender $SourceRoot $renderRoot "$GradleCache\neoformruntime\artifacts\minecraft_1.21.1_client.jar"
if ($LASTEXITCODE -ne 0) { throw 'Icon rendering failed' }
