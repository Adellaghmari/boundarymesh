$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

Write-Host 'Starting BoundaryMesh local full stack.'
Write-Host 'Backend: http://127.0.0.1:8080'
Write-Host 'Frontend: http://127.0.0.1:5173'
Write-Host 'VITE_* variables are public browser configuration. Do not put secrets there.'

Start-Process powershell -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy', 'Bypass',
  '-File', (Join-Path $PSScriptRoot 'start-backend.ps1')
)

Set-Location (Join-Path $root 'frontend')
npm run dev
