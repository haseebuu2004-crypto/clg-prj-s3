$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (-not (Get-Command javac -ErrorAction SilentlyContinue) -or -not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error 'A Java Development Kit is required. Install Java 17 or newer and try again.'
    exit 1
}

$classes = Join-Path $PSScriptRoot 'build\classes'
$testClasses = Join-Path $PSScriptRoot 'build\test-classes'
New-Item -ItemType Directory -Force -Path $classes, $testClasses | Out-Null
$sources = @(Get-ChildItem -Path (Join-Path $PSScriptRoot 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& javac --release 17 -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$tests = @(Get-ChildItem -Path (Join-Path $PSScriptRoot 'src\test\java') -Filter '*.java' -Recurse | ForEach-Object { $_.FullName })
& javac --release 17 -encoding UTF-8 -cp $classes -d $testClasses @tests
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -ea -cp "$classes;$testClasses" skillswap.SkillSwapSelfTest
exit $LASTEXITCODE
