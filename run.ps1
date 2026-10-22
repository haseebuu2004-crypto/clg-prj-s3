$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$classes = Join-Path $PSScriptRoot 'build\classes'
& java -cp $classes skillswap.App
exit $LASTEXITCODE
