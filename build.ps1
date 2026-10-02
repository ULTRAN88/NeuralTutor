$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Remove-Item "$Root\build\classes" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$Root\build\fat" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item "$Root\dist" -Recurse -Force -ErrorAction SilentlyContinue

New-Item "$Root\build\classes" -ItemType Directory -Force | Out-Null
New-Item "$Root\build\fat" -ItemType Directory -Force | Out-Null
New-Item "$Root\dist" -ItemType Directory -Force | Out-Null

$files = Get-ChildItem "$Root\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac --release 21 -cp "$Root\lib\sqlite-jdbc.jar" -d "$Root\build\classes" $files
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

@"
Manifest-Version: 1.0
Main-Class: com.neuraltutor.Main
"@ | Set-Content "$Root\build\manifest.mf"

Copy-Item "$Root\build\classes\*" "$Root\build\fat\" -Recurse -Force
$extract = "$Root\build\sqlite_extract"
Remove-Item $extract -Recurse -Force -ErrorAction SilentlyContinue
New-Item $extract -ItemType Directory -Force | Out-Null
Push-Location $extract
& jar xf "$Root\lib\sqlite-jdbc.jar"
Pop-Location

Copy-Item "$extract\org" "$Root\build\fat\org" -Recurse -Force
New-Item "$Root\build\fat\META-INF\services" -ItemType Directory -Force | Out-Null
Copy-Item "$extract\META-INF\services\java.sql.Driver" "$Root\build\fat\META-INF\services\java.sql.Driver" -Force

& jar --create --file "$Root\dist\NeuralTutor-Pro.jar" --manifest "$Root\build\manifest.mf" -C "$Root\build\fat" .
Write-Host "Built $Root\dist\NeuralTutor-Pro.jar"