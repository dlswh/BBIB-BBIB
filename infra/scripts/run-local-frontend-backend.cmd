@echo off
setlocal

set "REPO_ROOT=%~dp0..\.."
pushd "%REPO_ROOT%" || exit /b 1

if not exist "infra\compose\.env" (
    copy "infra\compose\.env.example" "infra\compose\.env" >nul
    echo Created infra\compose\.env from .env.example. Check credentials before running Spring.
)

(
    echo VITE_USE_MOCK=false
    echo VITE_API_BASE_URL=
    echo API_PROXY_TARGET=http://localhost:8080
) > "web\.env"

echo Starting local Spring Boot on http://localhost:8080 ...
start "S207 Local Spring" cmd /k "cd /d ""%REPO_ROOT%"" && powershell -NoProfile -ExecutionPolicy Bypass -File ""infra\scripts\run-backend-local.ps1"""

echo Starting local Vite frontend on http://localhost:5173 ...
start "S207 Local Frontend" cmd /k "cd /d ""%REPO_ROOT%\web"" && npm.cmd install && npm.cmd run dev"

echo.
echo Open http://localhost:5173 after both servers finish starting.
echo Browser requests to /api/... will be proxied to http://localhost:8080/api/...

popd
