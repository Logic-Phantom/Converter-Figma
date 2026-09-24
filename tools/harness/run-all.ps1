# Offline verification of the whole converter on Windows (README §7). Needs JDK 11+; the visual QA step also needs
# Chrome/Edge (or Node + Playwright) and skips itself otherwise.
#
#   powershell -ExecutionPolicy Bypass -File tools\harness\run-all.ps1          # everything
#   powershell -ExecutionPolicy Bypass -File tools\harness\run-all.ps1 quick    # compile + rule path + AI path only
param([string]$Mode = "")
$ErrorActionPreference = "Continue"
Set-Location (Join-Path $PSScriptRoot "..\..")
$Root = (Get-Location).Path
$Out = if ($env:HARNESS_OUT) { $env:HARNESS_OUT } else { Join-Path $Root "target\harness" }
if (Test-Path $Out) { Remove-Item -Recurse -Force $Out }
New-Item -ItemType Directory -Force (Join-Path $Out "classes") | Out-Null
$Cp = ((Get-ChildItem "$Root\src\main\webapp\WEB-INF\lib\*.jar").FullName -join ';') + ";$Root\ci-lib\clx\cleopatra_server.jar"
$RunCp = "$Out\classes;$Cp;$Root\src\main\resources"
$status = 0
function Step($t) { Write-Host ""; Write-Host "=================== $t ===================" }

Step "1. javac (src/main/java + tools/harness)"
$sources = (Get-ChildItem -Recurse "$Root\src\main\java" -Filter *.java).FullName
javac -encoding UTF-8 -nowarn -d "$Out\classes" -cp $Cp $sources; if ($LASTEXITCODE -ne 0) { exit 1 }
javac -encoding UTF-8 -nowarn -d "$Out\classes" -cp "$Out\classes;$Cp" (Get-ChildItem "$Root\tools\harness\*.java").FullName; if ($LASTEXITCODE -ne 0) { exit 1 }

Step "2. FigmaHarness: clx-src/json 전체 → CLX (기대: 46 OK, 1 ERR=요소 없는 페이지)"
$figma = java -cp $RunCp FigmaHarness templates "$Out\figma" clx-src\json 2>&1
$figma | Select-String -Pattern "^(FAIL|ERR|screens)"
if (-not ($figma -match "screens ok=46 failures=1")) { Write-Host "!! FigmaHarness 결과가 46 OK / 1 ERR 가 아닙니다"; $status = 1 }

Step "3. FigmaAiHarness: 가짜 AI 서버 7 시나리오"
java -cp $RunCp FigmaAiHarness templates "$Out\ai" clx-src\json\2025-08-04\2025-08-04_e69f5327.json 2>&1 | Select-String -Pattern "^(OK|FAIL|ALL|[0-9]+ FAILED)"
if ($LASTEXITCODE -ne 0) { $status = 1 }

Step "4. OpenApiHarness: Swagger/OpenAPI → DataSet/DataMap/Submission + JS"
java -cp $RunCp OpenApiHarness templates "$Out\api" clx-src\json\2025-08-04\2025-08-04_e69f5327.json tools\harness\samples\health-card.openapi.json tools\harness\samples\health-card.swagger2.json 2>&1 | Select-String -NotMatch "^\[eXConverter"
if ($LASTEXITCODE -ne 0) { $status = 1 }

if ($Mode -eq "quick") { Write-Host ""; Write-Host "quick 모드 종료 (status=$status)"; exit $status }

Step "5. e6-compiler: 바인딩된 CLX + 규칙 CLX 헤드리스 컴파일"
$E6 = Join-Path $Out "e6"
New-Item -ItemType Directory -Force "$E6\project\clx-src\gen" | Out-Null
Copy-Item -Recurse -Force "$Root\.project", "$Root\.settings" "$E6\project\" -ErrorAction SilentlyContinue
Copy-Item "$Root\clx-src\env.json", "$Root\clx-src\language.json" "$E6\project\clx-src\"
Copy-Item -Recurse "$Root\clx-src\udc", "$Root\clx-src\theme" "$E6\project\clx-src\"
Copy-Item "$Out\api\api.clx", "$Out\api\api.js" "$E6\project\clx-src\gen\"
Copy-Item "$Out\figma\*.clx", "$Out\figma\*.js" "$E6\project\clx-src\gen\"
$e6log = java -jar ci-lib\clx\e6-compiler.jar -s "$E6\project" -o "$E6\out" 2>&1
$e6log | Select-String -Pattern "ERROR|BUILD" | Select-Object -Last 5
if (-not ($e6log -match "BUILD SUCCESS")) { Write-Host "!! e6-compiler 실패"; $status = 1 }
if (Select-String -Quiet -Path "$E6\out\gen\api.clx.js" -Pattern 'toDataMap\(app.lookup\("dmSearch"\)') { Write-Host "OK   api.clx.js: datamapbind → toDataMap" } else { Write-Host "FAIL api.clx.js 바인딩 코드 없음"; $status = 1 }

Step "6. ThemeSyncHarness: Figma styles/variables → theme LESS (+ e6-compiler 로 LESS 컴파일)"
java -cp $RunCp ThemeSyncHarness $Root "$Out\theme" clx-src\json\2025-08-04\2025-08-04_e69f5327.json tools\harness\samples\variables-local.sample.json 2>&1 | Select-String -NotMatch "^\[eXConverter"
if ($LASTEXITCODE -ne 0) { $status = 1 }
$themeLog = java -jar ci-lib\clx\e6-compiler.jar -s "$Out\theme\project" -o "$Out\theme\out" 2>&1
$themeLog | Select-String -Pattern "ERROR|BUILD" | Select-Object -Last 3
if (($themeLog -match "BUILD SUCCESS") -and (Select-String -Quiet -Path "$Out\theme\out\theme\cleopatra-theme.css" -Pattern "e8f3fd")) { Write-Host "OK   figma 토큰이 들어간 테마 LESS → CSS 컴파일 (hover-background #e8f3fd 반영)" } else { Write-Host "FAIL 테마 LESS 컴파일 또는 토큰 미반영"; $status = 1 }

Step "7. VisualQaHarness: pixelmatch + 헤드리스 렌더 + 리포트"
java -cp $RunCp VisualQaHarness $Root "$Out\api\api.clx" "$Out\api\api.js" "$Out\qa" 2>&1 | Select-String -NotMatch "^\[eXConverter"
if ($LASTEXITCODE -ne 0) { $status = 1 }

Write-Host ""; Write-Host ("=================== 결과: " + $(if ($status -eq 0) { "ALL OK" } else { "FAILED" }) + " (출력: $Out) ===================")
exit $status
