[CmdletBinding()]
param(
    [ValidateSet("app-image", "exe")]
    [string]$PackageType = "app-image",

    [string]$AppName = "CDIEM",

    [string]$AppVersion = "1.0.0",

    [string]$JdkHome = "",

    [string]$OutputDir = "dist"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $repoRoot "target"
$inputDir = Join-Path $targetDir "jpackage-input"
$outputPath = Join-Path $repoRoot $OutputDir
$mainClass = "com.project.CaseManagementLauncher"

if ($null -ne (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue)) {
    $PSNativeCommandUseErrorActionPreference = $false
}

function Fail([string]$Message) {
    throw $Message
}

function Get-CommandPath([string]$CommandName) {
    $command = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $command) {
        return $null
    }

    if ($command.Source) {
        return $command.Source
    }

    return $command.Path
}

function Resolve-JdkHome([string]$RequestedJdkHome) {
    if ($RequestedJdkHome) {
        return $RequestedJdkHome
    }

    if ($env:JAVA_HOME) {
        return $env:JAVA_HOME
    }

    $jpackageOnPath = Get-CommandPath "jpackage.exe"
    if ($jpackageOnPath) {
        return Split-Path -Parent (Split-Path -Parent $jpackageOnPath)
    }

    return $null
}

$JdkHome = Resolve-JdkHome $JdkHome

if (-not $JdkHome) {
    Fail "JAVA_HOME is not set and jpackage.exe was not found on PATH. Install JDK 21 or newer, add it to PATH, or rerun with -JdkHome '<path-to-jdk>'."
}

if (Test-Path $JdkHome) {
    $JdkHome = (Resolve-Path $JdkHome).Path
}

$javaExe = Join-Path $JdkHome "bin\java.exe"
$jpackageExe = Join-Path $JdkHome "bin\jpackage.exe"

if (-not (Test-Path $javaExe)) {
    Fail "java.exe was not found under '$JdkHome\bin'."
}

if (-not (Test-Path $jpackageExe)) {
    Fail "jpackage.exe was not found under '$JdkHome\bin'. Use JDK 21 or newer."
}

$javaVersionOutput = & $javaExe --version
if ($LASTEXITCODE -ne 0) {
    Fail "Failed to read the Java version from '$javaExe'."
}
$javaVersionLine = $javaVersionOutput | Select-Object -First 1

if ($javaVersionLine -match '"(?<major>\d+)') {
    $javaMajor = [int]$Matches.major
} elseif ($javaVersionLine -match '^(?:openjdk|java)\s+(?<major>\d+)\b') {
    $javaMajor = [int]$Matches.major
} else {
    Fail "Could not parse the Java version from: $javaVersionLine"
}

if ($javaMajor -lt 21) {
    Fail "JDK 21 or newer is required. Current Java version: $javaVersionLine"
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Fail "Maven was not found on PATH. Open a shell where 'mvn -version' works, then rerun this script."
}

if ($PackageType -eq "exe") {
    $hasCandle = $null -ne (Get-Command candle.exe -ErrorAction SilentlyContinue)
    $hasLight = $null -ne (Get-Command light.exe -ErrorAction SilentlyContinue)
    if (-not ($hasCandle -and $hasLight)) {
        Fail "Windows installer packaging requires WiX Toolset on PATH. Install WiX 3.x so that candle.exe and light.exe are available, or rerun with -PackageType app-image."
    }
}

Push-Location $repoRoot
try {
    Remove-Item $inputDir -Recurse -Force -ErrorAction SilentlyContinue
    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null

    & mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Fail "Maven package failed."
    }

    & mvn dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$inputDir"
    if ($LASTEXITCODE -ne 0) {
        Fail "Copying runtime dependencies failed."
    }

    $mainJar = Get-ChildItem $targetDir -Filter "*.jar" |
        Where-Object {
            $_.Name -notlike "original-*" -and
            $_.Name -notlike "*-sources.jar" -and
            $_.Name -notlike "*-javadoc.jar"
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if (-not $mainJar) {
        Fail "No application JAR was produced under '$targetDir'."
    }

    Copy-Item $mainJar.FullName $inputDir -Force

    $configSource = Join-Path $repoRoot "config"
    $configTarget = Join-Path $inputDir "config"
    if (Test-Path $configSource) {
        Copy-Item $configSource $configTarget -Recurse -Force
    } else {
        New-Item -ItemType Directory -Path $configTarget -Force | Out-Null
    }

    $storageTarget = Join-Path $inputDir "storage\evidence"
    New-Item -ItemType Directory -Path $storageTarget -Force | Out-Null

    if ($PackageType -eq "app-image") {
        $existingPackagePath = Join-Path $outputPath $AppName
        if (Test-Path $existingPackagePath) {
            Remove-Item $existingPackagePath -Recurse -Force
        }
    }

    $jpackageArgs = @(
        "--type", $PackageType,
        "--name", $AppName,
        "--app-version", $AppVersion,
        "--dest", $outputPath,
        "--input", $inputDir,
        "--main-jar", $mainJar.Name,
        "--main-class", $mainClass,
        "--java-options", "-Duser.dir=`$APPDIR"
    )

    if ($PackageType -eq "exe") {
        $jpackageArgs += @(
            "--win-per-user-install",
            "--win-shortcut",
            "--win-menu"
        )
    }

    & $jpackageExe @jpackageArgs
    if ($LASTEXITCODE -ne 0) {
        Fail "jpackage failed."
    }

    Write-Host ""
    Write-Host "Package created successfully in: $outputPath"
    Write-Host "Type: $PackageType"
    if ($PackageType -eq "app-image") {
        Write-Host "Launcher: $(Join-Path $outputPath "$AppName\$AppName.exe")"
    }
}
finally {
    Pop-Location
}
