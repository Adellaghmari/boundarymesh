$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
  $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
  if (Test-Path $candidate) {
    $env:JAVA_HOME = $candidate
  }
}

if (-not $env:JAVA_HOME) {
  throw 'JAVA_HOME is not set. Phase 2 local backend requires JDK 25.'
}

$backend = Join-Path $PSScriptRoot '..\backend'
Set-Location $backend

$worker = Start-Process -FilePath '.\mvnw.cmd' -ArgumentList @('-pl', 'worker', '-am', 'spring-boot:run') -WorkingDirectory $backend -PassThru -WindowStyle Hidden

$ready = $false
for ($i = 0; $i -lt 90; $i++) {
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8081/api/v1/health' -TimeoutSec 2
    if ($response.StatusCode -eq 200) {
      $ready = $true
      break
    }
  } catch {
    Start-Sleep -Seconds 2
  }
}

if (-not $ready) {
  if ($worker -and -not $worker.HasExited) {
    Stop-Process -Id $worker.Id -Force -ErrorAction SilentlyContinue
  }
  throw 'Delivery worker did not become healthy on http://127.0.0.1:8081/api/v1/health.'
}

try {
  & .\mvnw.cmd -pl api -am spring-boot:run
} finally {
  if ($worker -and -not $worker.HasExited) {
    Stop-Process -Id $worker.Id -Force -ErrorAction SilentlyContinue
  }
}
