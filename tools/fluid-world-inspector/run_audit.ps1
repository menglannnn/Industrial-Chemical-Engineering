param(
    [string]$SourceRoot = 'C:\Users\wormz\.codex\worktrees\pipeline-tool\CreateChemE',
    [string]$JavaRoot = 'C:\Program Files\Java\jdk-21.0.11',
    [string]$GradleCache = 'C:\Users\wormz\.gradle\caches\modules-2\files-2.1',
    [string]$Python = 'C:\Users\wormz\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
)
$ErrorActionPreference = 'Stop'
$toolRoot = $PSScriptRoot
$gson = (Get-ChildItem -LiteralPath (Join-Path $GradleCache 'com.google.code.gson\gson\2.10.1') -Recurse -Filter '*.jar' | Select-Object -First 1).FullName
$ejml = (Get-ChildItem -LiteralPath (Join-Path $GradleCache 'org.ejml') -Recurse -Filter '*.jar' | Where-Object { $_.FullName -match '\\0\.44\.0\\' } | ForEach-Object { $_.FullName }) -join ';'
$classes = Join-Path $toolRoot 'classes'
$classpath = "$classes;$SourceRoot\build\classes\java\main;$SourceRoot\build\resources\main;$gson;$ejml"
$javac = Join-Path $JavaRoot 'bin\javac.exe'
$java = Join-Path $JavaRoot 'bin\java.exe'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& $javac -cp $classpath -d $classes (Join-Path $toolRoot 'PropertyAudit.java') (Join-Path $toolRoot 'StepAudit.java')
if ($LASTEXITCODE -ne 0) { throw 'Audit compilation failed' }
& $Python (Join-Path $toolRoot 'prepare_trace.py') $SourceRoot
if ($LASTEXITCODE -ne 0) { throw 'Trace preparation failed' }
$traceClasses = Join-Path $toolRoot 'trace-classes'
$traceSources = Get-ChildItem -LiteralPath (Join-Path $toolRoot 'trace-src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& $javac -cp $classpath -d $traceClasses (Join-Path $toolRoot 'AuditTrace.java') @traceSources
if ($LASTEXITCODE -ne 0) { throw 'Trace compilation failed' }
$runtime = "$traceClasses;$classpath"
$case = Join-Path $toolRoot 'world-inspection.json'
& $java -Xmx1g '-Dfluid.audit.trace=true' -cp $runtime com.wormzjl.icheme.science.fluid.network.StepAudit $case 4.5 interval *> (Join-Path $toolRoot 'baseline.log')
& $java -Xmx1g '-Dfluid.audit.carryValve=true' -cp $runtime com.wormzjl.icheme.science.fluid.network.StepAudit $case 4.5 interval 20 *> (Join-Path $toolRoot 'accepted-carry.log')
Get-Content -LiteralPath (Join-Path $toolRoot 'baseline.log') | Select-String '^CASE'
Get-Content -LiteralPath (Join-Path $toolRoot 'accepted-carry.log') -Tail 3
