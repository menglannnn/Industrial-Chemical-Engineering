# Final lane of the process-control merge (AGENTS.md): test slowGates, chain-100 exact, fluid GameTests, the 1000-seed
# campaign. qualify.ps1 Final with output in this folder and native stderr let through (Windows PowerShell 5.1).
param([string]$Workspace)
$ErrorActionPreference='Stop'
$out=$PSScriptRoot
$env:TEMP=Join-Path $out 'temp';$env:TMP=$env:TEMP
New-Item -ItemType Directory -Path $env:TEMP -Force | Out-Null
function Assert-Idle {
    $busy=Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {$_.CommandLine -match 'forgeclientdev|neoforgeclientdev|Gradle Test Executor|GradleWrapperMain|gradle-wrapper.jar|net.neoforged.devlaunch.Main.*(?:client|mcpClient)RunProgramArgs'}
    if($busy){throw 'A dev client, test worker or other Gradle invocation is running; the lane must run alone.'}
}
function Invoke-Step([string]$Name,[string[]]$BuildArgs){
    Assert-Idle
    $log=Join-Path $out ($Name+'.log')
    Write-Host ("Starting "+$Name+" "+(Get-Date -Format HH:mm:ss))
    Push-Location $Workspace
    $ErrorActionPreference='Continue'
    try{& (Join-Path $Workspace 'gradlew.bat') @BuildArgs --max-workers=8 --offline --console=plain *> $log;$code=$LASTEXITCODE}finally{Pop-Location;$ErrorActionPreference='Stop'}
    Write-Host ("Finished "+$Name+" exit="+$code+" "+(Get-Date -Format HH:mm:ss))
    return $code
}
$gates=Invoke-Step 'final-gates' @('test','slowGates','--rerun-tasks')
$results=Join-Path $out 'final-results';New-Item -ItemType Directory -Path $results -Force | Out-Null
Copy-Item -Path (Join-Path $Workspace 'build/test-results/test/TEST-*.xml'),(Join-Path $Workspace 'build/test-results/slowGates/TEST-*.xml') -Destination $results
if($gates -ne 0){throw 'Final gates failed'}
$chain=Invoke-Step 'final-chain-100' @('--no-configuration-cache','fluidSolverRegression','-PfluidRegressionMode=exact')
$game=Invoke-Step 'final-gametests' @('--no-configuration-cache','runFluidGameTestServer','-PfluidGameTestRunId=process-control-merge-final')
Write-Output ("Starting campaign "+(Get-Date -Format HH:mm:ss))
& (Join-Path $out 'quick.ps1') -Project $Workspace -Prefix 'campaign' -Seeds 1000
Write-Output ("Finished campaign "+(Get-Date -Format HH:mm:ss)+" chain="+$chain+" gametests="+$game)
