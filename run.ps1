$ErrorActionPreference = "Stop"

$mavenVersion = "3.9.6"
$toolsDir = Join-Path $PSScriptRoot ".tools"
$jdkDir = Join-Path $toolsDir "jdk17"
$javaBin = Join-Path $jdkDir "bin\java.exe"
$mavenDir = Join-Path $toolsDir "apache-maven-$mavenVersion"
$mavenBin = Join-Path $mavenDir "bin\mvn.cmd"

if (-not (Test-Path $javaBin)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $jdkZip = Join-Path $toolsDir "jdk17.zip"
    if (-not (Test-Path $jdkZip)) {
        Invoke-WebRequest "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk" -OutFile $jdkZip
    }
    $extractDir = Join-Path $toolsDir "jdk-extract"
    if (Test-Path $extractDir) {
        Remove-Item -Recurse -Force $extractDir
    }
    Expand-Archive -Force $jdkZip $extractDir
    $expandedJdk = Get-ChildItem $extractDir | Where-Object { $_.PSIsContainer } | Select-Object -First 1
    Move-Item $expandedJdk.FullName $jdkDir
    Remove-Item -Recurse -Force $extractDir
}

if (-not (Test-Path $mavenBin)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $zip = Join-Path $toolsDir "apache-maven-$mavenVersion-bin.zip"
    if (-not (Test-Path $zip)) {
        Invoke-WebRequest "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip" -OutFile $zip
    }
    Expand-Archive -Force $zip $toolsDir
}

$env:JAVA_HOME = $jdkDir
$env:Path = (Join-Path $jdkDir "bin") + ";" + $env:Path
& $mavenBin javafx:run
