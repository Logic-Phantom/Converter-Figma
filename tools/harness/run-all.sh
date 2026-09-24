#!/usr/bin/env bash
# Offline verification of the whole converter (README §7). Needs JDK 11+; the visual QA step also needs
# Chrome/Edge/Chromium (or Node + Playwright) and skips itself otherwise.
#
#   tools/harness/run-all.sh            # everything (~5 min)
#   tools/harness/run-all.sh quick      # compile + rule path + AI path + OpenAPI only
set -u
cd "$(dirname "$0")/../.."
ROOT=$(pwd)
OUT=${HARNESS_OUT:-"$ROOT/target/harness"}
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/logs"
CP=$(ls "$ROOT"/src/main/webapp/WEB-INF/lib/*.jar | tr '\n' ':')"$ROOT/ci-lib/clx/cleopatra_server.jar"
RUN_CP="$OUT/classes:$CP:$ROOT/src/main/resources"
status=0
step() { echo; echo "=================== $1 ==================="; }
# run <log name> <command...>: runs once, keeps the full log, prints the interesting lines, records failure.
run() { local log="$OUT/logs/$1.log"; shift; "$@" > "$log" 2>&1; local rc=$?; grep -vE "^\[eXConverter|^\s*$" "$log" | grep -E "^(OK|FAIL|ERR|SKIP|ALL|[0-9]+ FAILED|screens|   )" ; [ $rc -ne 0 ] && { echo "!! 실패 (exit $rc, 로그: $log)"; status=1; }; return $rc; }

step "1. javac (src/main/java + tools/harness)"
javac -encoding UTF-8 -nowarn -d "$OUT/classes" -cp "$CP" $(find src/main/java -name '*.java') || exit 1
javac -encoding UTF-8 -nowarn -d "$OUT/classes" -cp "$OUT/classes:$CP" tools/harness/*.java || exit 1
echo "OK   컴파일"

step "2. FigmaHarness: clx-src/json 전체 → CLX (기대: 46 OK, 1 ERR=요소 없는 페이지)"
java -cp "$RUN_CP" FigmaHarness templates "$OUT/figma" clx-src/json > "$OUT/logs/figma.log" 2>&1
grep -E "^(FAIL|ERR|screens)" "$OUT/logs/figma.log"
grep -q "screens ok=46 failures=1" "$OUT/logs/figma.log" || { echo "!! FigmaHarness 결과가 46 OK / 1 ERR 가 아닙니다 (로그: $OUT/logs/figma.log)"; status=1; }

step "3. FigmaAiHarness: 가짜 AI 서버 7 시나리오"
run ai java -cp "$RUN_CP" FigmaAiHarness templates "$OUT/ai" clx-src/json/2025-08-04/2025-08-04_e69f5327.json

step "4. OpenApiHarness: Swagger/OpenAPI → DataSet/DataMap/Submission + JS"
run api java -cp "$RUN_CP" OpenApiHarness templates "$OUT/api" clx-src/json/2025-08-04/2025-08-04_e69f5327.json \
  tools/harness/samples/health-card.openapi.json tools/harness/samples/health-card.swagger2.json

if [ "${1:-}" = "quick" ]; then echo; echo "quick 모드 종료 (status=$status)"; exit $status; fi

step "5. e6-compiler: 바인딩된 CLX + 규칙 CLX 헤드리스 컴파일"
E6="$OUT/e6"; mkdir -p "$E6/project/clx-src/gen"
cp -R .project .settings "$E6/project/" 2>/dev/null
cp clx-src/env.json clx-src/language.json "$E6/project/clx-src/"; cp -R clx-src/udc clx-src/theme "$E6/project/clx-src/"
cp "$OUT/api/api.clx" "$OUT/api/api.js" "$E6/project/clx-src/gen/"
cp "$OUT"/figma/*.clx "$OUT"/figma/*.js "$E6/project/clx-src/gen/"
java -jar ci-lib/clx/e6-compiler.jar -s "$E6/project" -o "$E6/out" > "$OUT/logs/e6.log" 2>&1
grep -E "ERROR|BUILD" "$OUT/logs/e6.log" | sed 's#(file:[^)]*)##' | tail -5
grep -q "BUILD SUCCESS" "$OUT/logs/e6.log" || { echo "!! e6-compiler 실패 (로그: $OUT/logs/e6.log)"; status=1; }
grep -q 'toDataMap(app.lookup("dmSearch")' "$E6/out/gen/api.clx.js" && echo "OK   api.clx.js: datamapbind → toDataMap, submission → addRequestData/addResponseData" || { echo "FAIL api.clx.js 바인딩 코드 없음"; status=1; }

step "6. ThemeSyncHarness: Figma styles/variables → theme LESS (+ e6-compiler 로 LESS 컴파일)"
run theme java -cp "$RUN_CP" ThemeSyncHarness "$ROOT" "$OUT/theme" clx-src/json/2025-08-04/2025-08-04_e69f5327.json tools/harness/samples/variables-local.sample.json
java -jar ci-lib/clx/e6-compiler.jar -s "$OUT/theme/project" -o "$OUT/theme/out" > "$OUT/logs/theme-e6.log" 2>&1
grep -E "ERROR|BUILD" "$OUT/logs/theme-e6.log" | tail -3
# The sample variables set hover-background to #e8f3fd (settings default is #DAEFFC): the compiled CSS must carry it.
grep -q "BUILD SUCCESS" "$OUT/logs/theme-e6.log" && grep -qi "e8f3fd" "$OUT/theme/out/theme/cleopatra-theme.css" \
  && echo "OK   figma 토큰이 들어간 테마 LESS → CSS 컴파일 (hover-background #e8f3fd 반영)" || { echo "FAIL 테마 LESS 컴파일 또는 토큰 미반영 (로그: $OUT/logs/theme-e6.log)"; status=1; }

step "7. VisualQaHarness: pixelmatch + 헤드리스 렌더 + 리포트"
run qa java -cp "$RUN_CP" VisualQaHarness "$ROOT" "$OUT/api/api.clx" "$OUT/api/api.js" "$OUT/qa"

echo; echo "=================== 결과: $([ $status = 0 ] && echo ALL OK || echo FAILED) (출력: $OUT, 로그: $OUT/logs) ==================="
exit $status
