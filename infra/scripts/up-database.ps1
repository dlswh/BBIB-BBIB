$ErrorActionPreference = "Stop"

$infraRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$composeDir = Join-Path $infraRoot "compose"
$envFile = Join-Path $composeDir ".env"
$envExample = Join-Path $composeDir ".env.example"
$databaseCompose = Join-Path $composeDir "docker-compose.database.yml"

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
}

$envContent = Get-Content -Raw $envFile
foreach ($name in @("POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD")) {
    if ($envContent -notmatch "(?m)^$name=.+") {
        throw "Missing or empty $name in infra/compose/.env. Add it from infra/compose/.env.example before starting PostgreSQL."
    }
}

docker network inspect s207-app *> $null
if ($LASTEXITCODE -ne 0) {
    docker network create s207-app | Out-Null
}

docker compose -p s14p31s207-database --env-file $envFile -f $databaseCompose up -d
docker compose -p s14p31s207-database --env-file $envFile -f $databaseCompose ps
