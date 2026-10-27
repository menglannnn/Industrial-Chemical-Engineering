$ErrorActionPreference='Stop'
$root=(Get-Location).Path
$lane=Join-Path $root 'tmp/qualification'
for($attempt=0;$attempt -lt 180;$attempt++){
    if(Test-Path -LiteralPath (Join-Path $lane 'final-chain-100.log')){
        $results=Join-Path $lane 'final-results'
        New-Item -ItemType Directory -Path $results -Force | Out-Null
        Copy-Item -Path (Join-Path $root 'build/test-results/test/TEST-*.xml'),(Join-Path $root 'build/test-results/slowGates/TEST-*.xml') -Destination $results
        Write-Output 'Final gate XML captured before campaign overwrite'
        exit 0
    }
    if((Test-Path -LiteralPath (Join-Path $lane 'final-gates.log')) -and (Select-String -LiteralPath (Join-Path $lane 'final-gates.log') -Pattern 'BUILD FAILED' -Quiet)){throw 'Final gates failed'}
    Start-Sleep -Seconds 5
}
throw 'Final gate capture deadline exceeded'
