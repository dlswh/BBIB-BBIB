$ErrorActionPreference = "Stop"

$infraRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$composeDir = Join-Path $infraRoot "compose"
$envFile = Join-Path $composeDir ".env"
$envExample = Join-Path $composeDir ".env.example"
$storageCompose = Join-Path $composeDir "docker-compose.storage.yml"

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
}

$envContent = Get-Content -Raw $envFile
foreach ($name in @("MINIO_ROOT_USER", "MINIO_ROOT_PASSWORD")) {
    if ($envContent -notmatch "(?m)^$name=.+") {
        throw "Missing or empty $name in infra/compose/.env. Add it from infra/compose/.env.example before starting MinIO."
    }
}

docker network inspect s207-app *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create s207-app | Out-Null
}

docker compose -p s14p31s207-storage --env-file $envFile -f $storageCompose up -d
docker compose -p s14p31s207-storage --env-file $envFile -f $storageCompose ps
