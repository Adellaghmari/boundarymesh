$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
  $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
  if (Test-Path $candidate) {
    $env:JAVA_HOME = $candidate
  }
}

if (-not $env:JAVA_HOME) {
  throw 'JAVA_HOME is not set. Phase 8 local worker requires JDK 25.'
}

$backend = Join-Path $PSScriptRoot '..\backend'
Set-Location $backend
& .\mvnw.cmd -pl worker -am spring-boot:run
