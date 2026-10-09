# OpenBimmer Local Build Pipeline Script (PowerShell)
# Builds the OpenBimmer Offline APK

param(
    [string]$Mode = "release" # "release" or "debug"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       OpenBimmer Offline - Local APK Build Pipeline      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check Java
Write-Host "[1/4] Checking Java JDK..." -ForegroundColor Yellow
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Host "Error: Java is not found on PATH. Please install JDK 17 or higher." -ForegroundColor Red
    exit 1
}
$javaVersion = & java -version 2>&1 | Select-Object -First 1
Write-Host "  Found Java: $javaVersion" -ForegroundColor Green

# 2. Check Flutter
Write-Host "[2/4] Checking Flutter SDK..." -ForegroundColor Yellow
$flutterCmd = Get-Command flutter -ErrorAction SilentlyContinue
if (-not $flutterCmd) {
    Write-Host "Flutter CLI not found on PATH." -ForegroundColor Yellow
    Write-Host "To build locally, install Flutter from https://docs.flutter.dev/get-started/install" -ForegroundColor Yellow
    Write-Host "Or push code to GitHub to trigger the automated CI/CD pipeline (.github/workflows/build_apk.yml)!" -ForegroundColor Green
    exit 1
}
$flutterVersion = & flutter --version | Select-Object -First 1
Write-Host "  Found Flutter: $flutterVersion" -ForegroundColor Green

# 3. Flutter Pub Get
Write-Host "[3/4] Resolving dependencies..." -ForegroundColor Yellow
Set-Location -Path $PSScriptRoot
& flutter pub get
if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: flutter pub get failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 4. Build APK
Write-Host "[4/4] Building APK ($Mode mode)..." -ForegroundColor Yellow
if ($Mode -eq "debug") {
    & flutter build apk --debug
} else {
    & flutter build apk --release
}

if ($LASTEXITCODE -eq 0) {
    $apkPath = "$PSScriptRoot\build\app\outputs\flutter-apk\app-$Mode.apk"
    Write-Host ""
    Write-Host "SUCCESS! APK built successfully at:" -ForegroundColor Green
    Write-Host "  $apkPath" -ForegroundColor Cyan
} else {
    Write-Host "Build failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}
