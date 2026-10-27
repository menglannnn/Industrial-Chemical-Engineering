# 200-seed quick check, Invoke-Fuzz of tools/process-control/qualify.ps1 with output in this folder.
# quick.ps1 -Project <tree> -Prefix <label>
param([string]$Project,[string]$Prefix,[int]$Seeds=200,[string]$SeedSpec="",[string[]]$Dists=@("cold-random","bundled-random","bundled-defaults"))
$ErrorActionPreference='Stop'
$main='D:/Minecraft/Modding/1.21/CreateChemE'
$out=$PSScriptRoot
$env:TEMP=Join-Path $out 'temp'
$env:TMP=$env:TEMP
$busy=Get-CimInstance Win32_Process -Filter "name = 'java.exe'" | Where-Object {$_.CommandLine -match 'forgeclientdev|neoforgeclientdev|Gradle Test Executor|GradleWrapperMain|gradle-wrapper.jar|net.neoforged.devlaunch.Main.*(?:client|mcpClient)RunProgramArgs'}
if($busy){throw 'A dev client, test worker or other Gradle invocation is running; measurement must run alone.'}
foreach($dist in $Dists){
    $cold=$dist.StartsWith('cold')
    $harness=if($cold){'cold'}else{'bundled'}
    $report=Join-Path $out ($Prefix+'-'+$dist+'-report.txt')
    $log=Join-Path $out ($Prefix+'-'+$dist+'.log')
    $a=@('--no-configuration-cache','--init-script',(Join-Path $out ($harness+'/hold-fuzz.init.gradle')),'test','--tests','*HoldFuzzTest',
        ("-PholdFuzzSeeds="+$(if($SeedSpec){$SeedSpec}else{"1-$Seeds"})),'-PholdFuzzThreads=8','-PholdFuzzCounters=true',
        ('-PholdFuzzDefaults='+$dist.EndsWith('defaults').ToString().ToLowerInvariant()),("-PholdFuzzOut="+$report))
    if($cold){$a+='-PholdFuzzCold=true'}
    Write-Output ("Starting "+$Prefix+'-'+$dist)
    Push-Location $Project
    $ErrorActionPreference="Continue"
    try{& (Join-Path $Project "gradlew.bat") @a --max-workers=8 --offline --console=plain *> $log;$code=$LASTEXITCODE}finally{Pop-Location;$ErrorActionPreference="Stop"}
    if($code -ne 0){Get-Content -LiteralPath $log -Tail 40;throw "$Prefix-$dist failed ($code)"}
    & 'C:/Program Files/Git/bin/bash.exe' ($main+'/tools/phase-ports-probes/hold-fuzz/classify.sh') $report
    Get-Content -LiteralPath $report | Where-Object {$_ -match '^(SUMMARY|COUNTERS)'} | ForEach-Object {$_.Substring(0,[Math]::Min(600,$_.Length))}
}
