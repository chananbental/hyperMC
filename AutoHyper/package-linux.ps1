$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$publishDir = Join-Path $repoRoot 'build/autohyper-portable'
$archive = Join-Path $repoRoot 'benchmark-bundle/autohyper.zip'

# Framework-dependent managed output can run on Linux using its .NET 8 runtime.
# Do not package a Windows apphost or machine-specific paths.json.
& dotnet publish (Join-Path $PSScriptRoot 'src/AutoHyper/AutoHyper.fsproj') `
    -c Release -p:UseAppHost=false -o $publishDir
if ($LASTEXITCODE -ne 0) { throw 'AutoHyper publish failed' }

$required = @('AutoHyper.dll', 'AutoHyper.deps.json', 'AutoHyper.runtimeconfig.json',
    'FsOmegaLib.dll', 'TransitionSystemLib.dll', 'FSharp.Core.dll', 'FParsec.dll', 'FParsecCS.dll')
$files = foreach ($name in $required) {
    $file = Join-Path $publishDir $name
    if (-not (Test-Path -LiteralPath $file)) { throw "Missing published dependency: $name" }
    $file
}
New-Item -ItemType Directory -Path (Split-Path -Parent $archive) -Force | Out-Null
Compress-Archive -LiteralPath $files -DestinationPath $archive -Force
Write-Host "Ready to commit: $archive"
