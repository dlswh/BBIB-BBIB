$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$composeDir = Join-Path $repoRoot "infra\compose"
$envFile = Join-Path $composeDir ".env"
$envExample = Join-Path $composeDir ".env.example"
$backendDir = Join-Path $repoRoot "backend"
$preferredKafkaBootstrapServers = $env:BACKEND_KAFKA_BOOTSTRAP_SERVERS

if (-not (Test-Path $envFile)) {
    Copy-Item $envExample $envFile
    Write-Host "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith("#")) {
        return
    }

    $parts = $line.Split("=", 2)
    if ($parts.Count -ne 2) {
        return
    }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"').Trim("'")
    [Environment]::SetEnvironmentVariable($name, $value, "Process")
}

if ($preferredKafkaBootstrapServers) {
    $env:BACKEND_KAFKA_BOOTSTRAP_SERVERS = $preferredKafkaBootstrapServers
}

foreach ($name in @("JWT_SECRET", "POSTGRES_DB", "POSTGRES_USER", "POSTGRES_PASSWORD", "REDIS_PASSWORD")) {
    if (-not [Environment]::GetEnvironmentVariable($name, "Process")) {
        throw "Missing or empty $name in infra/compose/.env. Add it before starting Backend locally."
    }
}

$postgresPort = if ($env:POSTGRES_PORT) { $env:POSTGRES_PORT } else { "5432" }
$redisPort = if ($env:REDIS_PORT) { $env:REDIS_PORT } else { "6379" }
$kafkaPort = if ($env:KAFKA_PORT) { $env:KAFKA_PORT } else { "9092" }
$kafkaBootstrapServers = if ($env:BACKEND_KAFKA_BOOTSTRAP_SERVERS) {
    $env:BACKEND_KAFKA_BOOTSTRAP_SERVERS
} else {
    "localhost:$kafkaPort"
}

$env:SPRING_PROFILES_ACTIVE = if ($env:BACKEND_SPRING_PROFILES_ACTIVE) { $env:BACKEND_SPRING_PROFILES_ACTIVE } else { "dev" }
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:$postgresPort/$env:POSTGRES_DB"
$env:SPRING_DATASOURCE_USERNAME = $env:POSTGRES_USER
$env:SPRING_DATASOURCE_PASSWORD = $env:POSTGRES_PASSWORD
$env:SPRING_DATA_REDIS_HOST = "localhost"
$env:SPRING_DATA_REDIS_PORT = $redisPort
$env:SPRING_DATA_REDIS_PASSWORD = $env:REDIS_PASSWORD
$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = $kafkaBootstrapServers

Push-Location $backendDir
try {
    .\gradlew.bat bootRun --no-daemon
}
finally {
    Pop-Location
}
