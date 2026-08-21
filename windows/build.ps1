param(
    [string]$Version = "1.0.0",
    [switch]$SkipInstaller
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Build = Join-Path $Root "build"
$Classes = Join-Path $Build "classes"
$Input = Join-Path $Build "input"
$Dist = Join-Path $Build "dist"
$Source = Join-Path $Root "src\main\java"

Remove-Item $Build -Recurse -Force -ErrorAction SilentlyContinue
New-Item $Classes -ItemType Directory -Force | Out-Null
New-Item $Input -ItemType Directory -Force | Out-Null
New-Item $Dist -ItemType Directory -Force | Out-Null

$JavaFiles = Get-ChildItem $Source -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $JavaFiles) { throw "No Java sources found." }
$SharedCore = Join-Path $Root "..\app\src\main\java\com\ryusgua\app\HexagramEngine.java"
if (-not (Test-Path $SharedCore)) { throw "Shared HexagramEngine.java not found" }

Write-Host "Compiling Windows desktop sources + shared core..."
& javac -encoding UTF-8 -d $Classes $SharedCore $JavaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

$SharedZhouYi = Join-Path $Root "..\app\src\main\res\raw\zhouyi.json"
if (-not (Test-Path $SharedZhouYi)) { throw "Shared zhouyi.json not found" }
Copy-Item $SharedZhouYi (Join-Path $Classes "zhouyi.json") -Force

Write-Host "Running deterministic core self-test..."
& java -cp $Classes com.ryusgua.windows.WindowsApp --self-test
if ($LASTEXITCODE -ne 0) { throw "Self-test failed" }

$Jar = Join-Path $Input "RyusGua-Windows.jar"
& jar --create --file $Jar --main-class com.ryusgua.windows.WindowsApp -C $Classes .
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

$Icon = Join-Path $Build "icon.ico"
$Svg = Join-Path $Root "icon.svg"
if (Get-Command magick -ErrorAction SilentlyContinue) {
    & magick -background none -density 256 $Svg -define icon:auto-resize=256,128,64,48,32,16 $Icon
    if ($LASTEXITCODE -ne 0) { throw "ImageMagick icon conversion failed" }
} else {
    throw "ImageMagick (magick) is required to build the Windows icon"
}
$ImageDest = Join-Path $Build "app-image"

Write-Host "Creating portable Windows app image..."
& jpackage `
  --type app-image `
  --name RyusGua `
  --app-version $Version `
  --vendor Ryyus `
  --description "柳之卦 · Ryu's Gua — Windows Desktop" `
  --input $Input `
  --main-jar "RyusGua-Windows.jar" `
  --main-class "com.ryusgua.windows.WindowsApp" `
  --icon $Icon `
  --dest $ImageDest
if ($LASTEXITCODE -ne 0) { throw "jpackage app-image failed" }

$PortableDir = Join-Path $ImageDest "RyusGua"
$PortableZip = Join-Path $Dist "RyusGua-Windows-v$Version-portable.zip"
Compress-Archive -Path (Join-Path $PortableDir "*") -DestinationPath $PortableZip -CompressionLevel Optimal

$Launcher = Join-Path $PortableDir "RyusGua.exe"
if (-not (Test-Path $Launcher)) { throw "Portable launcher EXE missing" }
Copy-Item $Launcher (Join-Path $Dist "RyusGua-launcher.exe") -Force

if (-not $SkipInstaller) {
    Write-Host "Creating Windows installer EXE..."
    $InstallerDest = Join-Path $Build "installer"
    New-Item $InstallerDest -ItemType Directory -Force | Out-Null
    & jpackage `
      --type exe `
      --name RyusGua `
      --app-version $Version `
      --vendor Ryyus `
      --description "柳之卦 · Ryu's Gua — Windows Desktop" `
      --input $Input `
      --main-jar "RyusGua-Windows.jar" `
      --main-class "com.ryusgua.windows.WindowsApp" `
      --icon $Icon `
      --win-menu `
      --win-shortcut `
      --win-dir-chooser `
      --dest $InstallerDest

    if ($LASTEXITCODE -eq 0) {
        $Installer = Get-ChildItem $InstallerDest -Filter *.exe | Select-Object -First 1
        if ($Installer) {
            Copy-Item $Installer.FullName (Join-Path $Dist "RyusGua-Windows-v$Version-setup.exe") -Force
        }
    } else {
        Write-Warning "Installer EXE was not produced; portable ZIP remains valid."
    }
}

Get-FileHash (Join-Path $Dist "*") -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash.ToLower())  $([IO.Path]::GetFileName($_.Path))" } |
    Set-Content (Join-Path $Dist "sha256-windows.txt") -Encoding ascii

Write-Host "Windows build complete:"
Get-ChildItem $Dist | Format-Table Name, Length
