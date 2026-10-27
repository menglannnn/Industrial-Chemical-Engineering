param(
    [ValidateSet('BaselineQuick','CandidateQuick','Final','FinalContinue')][string]$Mode,
    [string]$Workspace='D:/Minecraft/Modding/1.21/CreateChemE/.claude/worktrees/process-control'
)
$ErrorActionPreference='Stop'
$main='D:/Minecraft/Modding/1.21/CreateChemE'
$out=Join-Path $Workspace 'tmp/qualification'
$env:TEMP=Join-Path $out 'temp'
$env:TMP=$env:TEMP
New-Item -ItemType Directory -Path $env:TEMP -Force | Out-Null
function Assert-Idle {
    $processes=Get-CimInstance Win32_Process -Filter "name = 'java.exe'"
    if($processes | Where-Object {$_.CommandLine -match 'forgeclientdev|neoforgeclientdev|Gradle Test Executor|GradleWrapperMain|gradle-wrapper.jar|net.neoforged.devlaunch.Main.*(?:client|mcpClient)RunProgramArgs'}){
        throw 'A dev client, test worker or other Gradle invocation is running; measurement must run alone.'
    }
}
function Invoke-Build([string]$Project,[string]$Name,[string[]]$BuildArgs){
    Assert-Idle
    $log=Join-Path $out ($Name+'.log')
    Write-Output ("Starting "+$Name)
    Push-Location $Project
    try{
        & (Join-Path $Project 'gradlew.bat') @BuildArgs --max-workers=8 --offline --console=plain *> $log
        $buildExit=$LASTEXITCODE
    }finally{Pop-Location}
    if($buildExit -ne 0){Get-Content -LiteralPath $log -Tail 65;throw "$Name failed ($buildExit)"}
    Write-Output ("Passed "+$Name)
}
function Invoke-Fuzz([string]$Project,[string]$Prefix,[int]$Seeds){
    foreach($dist in @('cold-random','bundled-random','bundled-defaults')){
        $cold=$dist.StartsWith('cold')
        $harness=if($cold){'cold'}else{'bundled'}
        $report=Join-Path $out ($Prefix+'-'+$dist+'-report.txt')
        $args=@('--no-configuration-cache','--init-script',(Join-Path $out ($harness+'/hold-fuzz.init.gradle')),'test','--tests','*HoldFuzzTest',
            "-PholdFuzzSeeds=1-$Seeds",'-PholdFuzzThreads=8','-PholdFuzzCounters=true',
            ('-PholdFuzzDefaults='+$dist.EndsWith('defaults').ToString().ToLowerInvariant()),("-PholdFuzzOut="+$report))
        if($cold){$args+='-PholdFuzzCold=true'}
        Invoke-Build $Project ($Prefix+'-'+$dist) $args
        & 'C:/Program Files/Git/bin/bash.exe' ($main+'/tools/phase-ports-probes/hold-fuzz/classify.sh') $report
        if($LASTEXITCODE -ne 0){throw 'Classifier failed'}
        Get-Content -LiteralPath $report | Where-Object {$_ -match '^(SUMMARY|COUNTERS)'} | ForEach-Object {$_.Substring(0,[Math]::Min(600,$_.Length))}
    }
}
switch($Mode){
    BaselineQuick {Invoke-Fuzz (Join-Path $Workspace 'tmp/baseline') 'baseline-quick' 200}
    CandidateQuick {Invoke-Fuzz $Workspace 'candidate-quick' 200}
    FinalContinue {
        Invoke-Build $Workspace 'final-gametests-loadfix' @('--no-configuration-cache','runFluidGameTestServer','-PfluidGameTestRunId=process-control-loadfix')
        Invoke-Fuzz $Workspace 'campaign' 1000
    }
    Final {
        Invoke-Build $Workspace 'final-gates' @('test','slowGates','--rerun-tasks')
        Invoke-Build $Workspace 'final-chain-100' @('--no-configuration-cache','fluidSolverRegression','-PfluidRegressionMode=exact')
        Invoke-Build $Workspace 'final-gametests' @('--no-configuration-cache','runFluidGameTestServer','-PfluidGameTestRunId=process-control')
        Invoke-Fuzz $Workspace 'campaign' 1000
    }
}

