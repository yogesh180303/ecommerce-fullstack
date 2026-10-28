$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaCommand = if ($env:JAVA_HOME) {
    Join-Path $env:JAVA_HOME 'bin\java.exe'
} else {
    (Get-Command java -ErrorAction Stop).Source
}

if (-not (Test-Path $javaCommand)) {
    throw "Java executable not found: $javaCommand"
}

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $javaVersion = (& $javaCommand -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($javaVersion -notmatch 'version "21(?:\.|")|openjdk 21(?:\D|$)') {
    throw "Java 21 is required. Set JAVA_HOME to a JDK 21 installation. Detected: $($javaVersion.Trim())"
}

Push-Location (Join-Path $projectRoot 'backend')
try {
    & .\mvnw.cmd test
    if ($LASTEXITCODE -ne 0) {
        throw "Backend tests failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

Push-Location (Join-Path $projectRoot 'frontend')
try {
    npm test
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend tests failed with exit code $LASTEXITCODE"
    }

    npm run build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend production build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$collectionPath = Join-Path $projectRoot 'postman\atelier-store.postman_collection.json'
Get-Content $collectionPath -Raw | ConvertFrom-Json | Out-Null
Write-Output 'Postman collection JSON is valid.'
Write-Output 'All configured project verification steps passed.'
