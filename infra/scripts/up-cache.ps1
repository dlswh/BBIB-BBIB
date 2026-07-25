$ErrorActionPreference = "Stop"

$infraRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$composeDir = Join-Path $infraRoot "compose"
$envFile = Join-Path $composeDir ".env"
$envExample = Join-Path $composeDir ".env.example"
$cacheCompose = Join-Path $composeDir "docker-compose.cache.yml"

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
}

$envContent = Get-Content -Raw $envFile
foreach ($name in @("REDIS_PASSWORD")) {
    if ($envContent -notmatch "(?m)^$name=.+") {
        throw "Missing or empty $name in infra/compose/.env. Add it from infra/compose/.env.example before starting Redis."
    }
}

docker network inspect s207-app *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create s207-app | Out-Null
}

docker compose -p s14p31s207-cache --env-file $envFile -f $cacheCompose up -d
docker compose -p s14p31s207-cache --env-file $envFile -f $cacheCompose ps
