param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$envExamplePath = Join-Path $repoRoot ".env.example"
$envPath = Join-Path $repoRoot ".env"

if ((Test-Path $envPath) -and (-not $Force)) {
    Write-Host ".env already exists: $envPath"
    exit 0
}

if (-not (Test-Path $envExamplePath)) {
    throw "Missing .env.example at: $envExamplePath"
}

Copy-Item -Path $envExamplePath -Destination $envPath -Force

$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$jwtSecret = [Convert]::ToBase64String($bytes)

$content = Get-Content -Path $envPath -Raw
if ($content -match '(?m)^JWT_SECRET=.*$') {
    $content = [regex]::Replace($content, '(?m)^JWT_SECRET=.*$', "JWT_SECRET=$jwtSecret")
} else {
    $content = $content.TrimEnd() + "`r`nJWT_SECRET=$jwtSecret`r`n"
}

Set-Content -Path $envPath -Value $content -Encoding utf8

Write-Host "Created .env with generated JWT_SECRET: $envPath"

