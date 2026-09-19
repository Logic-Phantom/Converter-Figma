# Figma to CLX Converter (eXCoverter-Figma)

Figma REST API가 돌려주는 파일 JSON(`GET /v1/files/:key`, `GET /v1/files/:key/nodes`)을 eXBuilder6 `.clx` + `.js` 쌍으로 변환한다.
**v2.0부터 좌표를 그대로 옮기지 않고, `templates/` 의 표준 화면 뼈대 중 Figma 화면과 가장 유사한 템플릿을 골라 그 위에 내용을 채운다.**

---

## 1. 동작 원리 (v2.0 템플릿 파이프라인)

```
Figma REST JSON ─▶ FigmaDocument ─▶ FigmaUiIrExtractor ─▶ UI-IR(JSON) ─▶ UiIrParser/Normalizer
 (document,          (화면 프레임 선택,    (원자 수집 → 영역 분할)                        │
  components,         컴포넌트셋 이름 해석)                                            ▼
  componentSets)                                               TemplateCatalog.selectFor (templates/ 78개 점수 비교)
                                                                                       │
                          clx-src/convertTest/{날짜}/{프레임명}.clx/.js ◀─ ClxValidator ◀─ ClxGenerator (뼈대 보존 + 내용 재구성)
```

| 단계 | 클래스 | 하는 일 |
|---|---|---|
| 1 | `figma/FigmaApiClient` | Figma URL/파일키 파싱, `?node-id=` 가 있으면 `/nodes?ids=` 로 해당 프레임만 요청 |
| 2 | `figma/FigmaDocument` | files/nodes 두 응답 형식 지원. INSTANCE → `components`/`componentSets` 로 **원본 컴포넌트명** 해석. 화면(프레임) 목록 결정 |
| 3 | `figma/FigmaComponentClassifier` | 노드 → eXBuilder 컨트롤 종류 (키워드는 `figma/component-keywords.properties` 로 확장) |
| 4 | `figma/FigmaUiIrExtractor` | 레이어 계층을 믿지 않고 텍스트/컨트롤/표를 절대좌표 원자로 모아 위→아래로 영역(title/search/form/grid/tabs/buttons…) 재구성 |
| 5 | `exconverter/*` | eXConverter-AI 의 템플릿 엔진(UI-IR → 템플릿 선택 → CLX 생성 → 검증) |
| 6 | `figma/FigmaConversionService` | 위를 묶는 서비스. 화면별 결과(선택 템플릿, 영역 요약, 경고, 저장 경로) 반환 |

AI 이미지 분석(eXConverter-AI)과 달리 Figma JSON 은 텍스트·컴포넌트·좌표가 정확하므로 **추출은 결정적**이다. AI/외부 모델 호출 없음.

### 1.1 Figma JSON 에서 읽는 것

| Figma 필드 | 용도 |
|---|---|
| `components[id].name`, `componentSets[id].name` | INSTANCE 의 정체. 레이어명이 `Frame 1000004302` 여도 `Base-input`/`Button`/`Radio-button` 으로 판별 |
| `componentProperties` (TEXT) | 버튼 캡션 (`Button name#67:81 = 조회`) |
| `characters`, `style.fontSize/fontWeight` | 라벨·제목·헤더 텍스트, 제목/섹션제목 판별, 폼테이블 판별 |
| `absoluteBoundingBox` | 행/열 분할, 라벨↔컨트롤 짝짓기, 그리드 컬럼 폭 |
| `visible:false` | 숨김 레이어 제외 (예: 숨긴 `hint`, 숨긴 콤보 화살표) |

### 1.2 추출 규칙 (`FigmaUiIrExtractor`)

| 대상 | 규칙 |
|---|---|
| 화면 | 페이지(CANVAS)의 겹치지 않는 최상위 FRAME 각각 = 화면 1개. 캔버스에 레이어가 흩어져 있으면 페이지 전체 = 화면 1개. node-id 지정 시 그 노드만 |
| 컨트롤 | 컴포넌트셋명 → 컴포넌트명 → 레이어명 순으로 키워드 매칭. `Base-input` 안에 **보이는** arrow-down 아이콘 → combobox, calendar → dateinput, search → searchinput |
| 화면 제목 | `title`/`AppHeader` 프레임의 가장 큰 텍스트, 없으면 상단의 18px 이상 최대 글자 → `udcComAppHeader.title` |
| 조회/폼 | 같은 줄의 `라벨 텍스트 → 컨트롤(들)` 을 필드로. 라벨 뒤 라디오/체크박스 여러 개 = 옵션, 날짜 2개 = daterange, `*` = 필수, 라벨 위쪽 배치도 인식. 줄당 필드 수 = `columnsPerRow` |
| 조회 vs 폼 | 첫 필드 묶음이 `조회/검색` 버튼을 갖거나, 페이지 제목 바로 아래 + 아래에 그리드가 있으면 search, 제목("프로젝트상세")이 있으면 form |
| 폼 테이블 | `table_register` 처럼 `라벨│값│라벨│값` 인 표는 그리드가 아니라 form. 라벨 뒤 일반 텍스트 = 읽기전용 `output` 필드. 원을 숨긴 Radio-button(값 표시용) = 텍스트 |
| 그리드 | 이름에 table/grid/테이블/그리드 또는 구조(3열 이상 정렬된 3행 이상). 헤더 = 텍스트가 가장 많은 첫 행(한 가지 글꼴 스타일), 헤더 왼쪽 체크박스 = 행선택 열, `NO/번호` = rowindex, 셀 안 버튼/입력 = 편집기, `FNM` 같은 대문자 식별자 = 바인딩 컬럼명 |
| 그리드 제목행 | 그리드 바로 위(64px 이내)의 텍스트 = 제목, `총 123건` = 건수(UDC가 그림), 버튼 = 제목행 버튼, 엑셀/다운로드 버튼 = `excel:true` |
| 페이징 | 그리드 안/바로 아래 Pagination 컴포넌트 → `paging:true` (pageindexer) |
| 탭 | `Base-Tab` 등 tab 컴포넌트 줄 → tabs, 그 아래 영역은 `inTab` |
| 좌우 분할 | 한 띠에 그리드가 좌우로 나란히 있으면 `side: left/right` |
| 버튼 줄 | 마지막 버튼 줄 = 푸터, 정렬(left/center/right)은 위치로 |

### 1.3 템플릿 선택과 생성 (`exconverter/*`, eXConverter-AI 에서 이식)

- `templates/**/*.clx` 전체를 프로파일링(조회영역 유무, 팝업, 푸터, 본문 블록열 `G`/`F`/`TAB{…}`/`DIV{…|…}` 등)하고 UI-IR 과 점수 비교 → 최고점 템플릿. 템플릿을 추가하면 재시작/재학습 없이 후보가 된다.
- 생성기는 템플릿 DOM 을 복제해 **뼈대(헤더 UDC, search-box, content-body, footer, 스타일 클래스, 간격)는 유지**하고 필드/컬럼/버튼/텍스트만 Figma 값으로 바꾼다.
- 상세 규칙(점수식, 블록열 편집거리, 생성 규칙)은 eXConverter-AI `README.md` §6~§9 참고.

### 1.4 UDC 반영 (`clx-src/udc/com`)

| UDC | 생성 규칙 (e6-compiler 로 컴파일 결과 확인) |
|---|---|
| `udcComAppHeader` | `title` = 화면 제목 |
| `udcComGridTitle` | `title` = 그리드 제목, **`ctrl` = 그리드**(건수/엑셀 연동), **`showExportExcel`** = 디자인에 엑셀 버튼이 있는지 |
| `udcComFormTitle` | `title` = 폼 제목 |
| `udcComGridCudBtns` | 그리드 제목행에 행추가/추가/신규 또는 행삭제/삭제가 있으면 개별 버튼 대신 이 UDC 생성. `grid` = 그리드, 라벨(`buttonNewLabel`/`buttonDelLabel`/`buttonRestoreLable`/`buttonSaveLabel`) = 디자인 캡션, 디자인에 없는 버튼은 `visible*Button=false`. 나머지 버튼은 일반 버튼 |

control 타입 UDC 속성 문법: `<cl:property name="grid" type="control" value="grd1"/>` → 컴파일 결과 `udc.grid = linker.grid_1`.
선택된 템플릿의 그리드 블록에 제목행 버튼 자리가 없으면(P1-4 등) 생성기가 `title-button-group` 을 추가한다.

---

## 2. API 엔드포인트

| URL | 입력 | 설명 |
|---|---|---|
| `GET /design/convertDirect.do` | `token`, `url` 또는 `fileKey`, `nodeId` (모두 선택) | 개인 액세스 토큰(PAT). 빠진 값은 `FIGMA_DIRECT_TOKEN` / `figma.direct.fileKey` |
| `/design/convert.do` | dmParam `token`(OAuth), 선택 `url`/`fileKey`/`nodeId` | 파일 지정이 없으면 `figma.team.id` 팀의 첫 프로젝트 첫 파일 |
| `/design/convertAll.do` | dmParam `token`(OAuth) | 팀의 모든 프로젝트 × 모든 파일 |
| `/design/jsonConvert.do` | 업로드 JSON | 저장해 둔 Figma 응답 JSON 변환 (convertJson.clx) |
| `/design/jsonConvertLegacy.do` | 업로드 JSON | v1.x 좌표 변환기(xylayout, 템플릿 미사용) — 비교용 |
| `/designForm/convertAdvanced.do` | dmParam `token`(PAT), `url`/`fileKey`, `nodeId` | 템플릿 변환 |
| `/designForm/convertFigmaToFormClx.do` | 없음 | 설정된 direct 파일 템플릿 변환 (convertForm.clx) |
| `/oauth/login.do`, `/oauth/callback.do` | | Figma OAuth → `converterStart.clx` 로 토큰 전달 |

`url` 에 Figma 링크(`https://www.figma.com/design/<KEY>/<이름>?node-id=12-34`)를 그대로 넣으면 해당 프레임만 `/nodes` API 로 받아 변환한다(응답이 작고 빠름).
응답 본문: 화면별 `✅ 프레임 → 템플릿 … / 영역: … / 저장: …` 또는 `❌ 사유`.

### 출력
```
clx-src/
├── convertTest/{yyyy-MM-dd}/{프레임명}.clx / .js   ← 변환 결과 (같은 프레임은 덮어씀)
├── json/{yyyy-MM-dd}/{파일명}_{id}.json             ← 받은 Figma 원본 JSON
└── udc/com/                                          ← 템플릿이 쓰는 공통 UDC
generated/ui-ir/{프레임명}.ui-ir.json                 ← 추출된 UI-IR (Tomcat 작업 디렉터리 기준, 디버깅용)
```

---

## 3. 설정

`src/main/resources/application.properties` (Figma) 와 `src/main/resources/exconverter/exconverter.properties` (템플릿 엔진).
우선순위: JVM `-Dkey=value` > 환경변수(`.`→`_`, 대문자. 예 `FIGMA_CLIENT_SECRET`) > properties 파일.

| 키 | 기본값 | 설명 |
|---|---|---|
| `figma.client.id` / `figma.client.secret` / `figma.redirect.uri` | | OAuth 앱 (secret 은 환경변수 권장) |
| `figma.oauth.scope` | `file_content:read projects:read` | `file_read` 는 2025-11-17 폐지 |
| `figma.team.id` | `1420657369280493518` | convert.do / convertAll.do 대상 팀 |
| `figma.direct.token` | (빈값) | PAT. **환경변수 `FIGMA_DIRECT_TOKEN` 사용** (최대 90일 만료) |
| `figma.direct.fileKey` | `x5gR79q0HUZ567W3CjCuCJ` | convertDirect.do 기본 파일 (URL 도 가능) |
| `figma.analysis.fileKey` | `rXU0zhKF2HjzFsND9njYbq` | 접근성/토큰/비교 분석 대상 파일 |
| `exconverter.template.root` | (프로젝트 `templates/`) | 템플릿 루트 강제 지정 |
| `exconverter.clx.result.folder` | `convertTest` | 결과 폴더(`clx-src/` 아래) |
| `exconverter.project.root` | (자동) | clx-src 를 가진 프로젝트 루트 |

컴포넌트 키워드 확장: `src/main/resources/figma/component-keywords.properties` (예: 사내 디자인시스템의 `SelectField` 를 콤보로 → `component.combobox=...,selectfield`).

---

## 4. 2025~2026 Figma API 변경과 반영 내역

| 변경 (Figma changelog) | 영향 | 반영 |
|---|---|---|
| 2025-11-17 `file_read`/`files:read` scope 폐지 | 기존 OAuth 로그인 실패 | `file_content:read projects:read` 로 교체 |
| OAuth 토큰 교환: client 자격증명은 Basic 헤더 | 본문 전송 방식 비권장 | Basic 헤더 + 랜덤 `state` 검증(CSRF) |
| 2025-04 PAT 최대 90일 | 하드코딩 토큰 만료 | 토큰은 요청 파라미터/환경변수 |
| `/v1/files/:key/nodes?ids=` | 파일 전체(수 MB) 대신 프레임만 | Figma URL 의 `node-id` 자동 사용 |
| Tier 1 rate limit (2025-11 조정) | 429 | 429 시 `Retry-After` 를 포함한 메시지 |
| 2026-08-10 `/v1/teams/:id/projects`, `/v1/projects/:id/files` deprecated | 중첩 폴더 팀은 v2 folders API | 현재는 v1 유지(주석 표기). 폴더 사용 팀이면 `GET /v2/teams/:team_id/folders` 로 교체 필요 |
| 2025-07 Grid auto-layout(`layoutMode: GRID`, `gridRowCount` 등) | 폼 레이아웃 힌트 | 미사용 (좌표 기반 추출로 충분). 향후 폼 열 수 판단에 활용 가능 |

Figma Dev Mode MCP 서버(`get_design_context`)는 LLM 이 코드를 생성하는 용도라 결정적 CLX 생성에는 쓰지 않았다. 같은 JSON 을 REST 로 받으므로 필요 시 입력원으로만 교체 가능.

---

## 5. 검증 방법

```powershell
# 1) 전체 컴파일 + 저장된 Figma JSON 전부 → UI-IR → 템플릿 → CLX → ClxValidator
$cp = ((Get-ChildItem src\main\webapp\WEB-INF\lib\*.jar).FullName) -join ';'
javac -encoding UTF-8 -d out -cp "$cp;ci-lib\clx\cleopatra_server.jar" (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
javac -encoding UTF-8 -d out -cp "out;$cp" tools\harness\FigmaHarness.java
java -cp "out;$cp;src\main\resources" FigmaHarness templates out\figma clx-src\json
# 2) eXBuilder6 헤드리스 컴파일: .project/.settings/clx-src(udc 포함) 를 가진 임시 폴더의 clx-src 에 out\figma 의 clx/js 복사 후
java -jar ci-lib\clx\e6-compiler.jar -s <임시프로젝트> -o <출력>
```

2026-09-19 기준: `clx-src/json` 의 47개 응답 중 46개 화면 생성·검증 통과(1개는 변환할 요소가 없는 조각 파일), e6-compiler **BUILD SUCCESS**.
대표 결과: `우리보건소 등록카드` → P1-4(조회+페이징 그리드), `프로젝트관리` → P5-2(상세폼 + 탭 안 그리드).

---

## 6. 폴더 구조

```
src/main/java/com/tomatosystem/
├── figma/                 ← v2.0 Figma → UI-IR (FigmaDocument, FigmaComponentClassifier, FigmaUiIrExtractor,
│                            FigmaApiClient, FigmaConversionService, FigmaSettings, FigmaPaths)
├── exconverter/           ← 템플릿 엔진 (eXConverter-AI 에서 복사, 아래 변경점 참고)
├── web/                   ← 컨트롤러 (Design, AdvancedDesign, OAuth, 접근성/토큰/비교 분석)
├── service/, type/, utill/← v1.x 좌표 변환기(FigmaToClxService, *NodeConverter) 와 분석 서비스
templates/                 ← 화면 패턴 뼈대 (P0~P8, *_P = 팝업) — 읽기 전용
tools/harness/FigmaHarness.java ← 서버 없이 변환/검증 (Eclipse 빌드 대상 아님)
```

`exconverter/` 는 eXConverter-AI 와 같은 코드를 유지하되 다음만 다르다(동기화 시 주의):
`ProjectRootResolver`(이 프로젝트 우선 탐색, `exconverter.clx.result.folder`), `UiIr`/`UiIrParser`/`ui-ir.schema.json`(grid `excel`),
`ClxGenerator`(`udcComGridCudBtns`, `udcComGridTitle.ctrl/showExportExcel`, 제목행 버튼 자리 추가, `setUdcProperty` 타입 지정).

---

## 7. 기타 기능 (v1.x)

- 웹 접근성 분석(WCAG 2.1, Excel 리포트): `/figma/accessibility/analyze.do`
- 디자인 토큰 추출(CSS/SCSS/Tailwind/JSON): `/figma/design-tokens/extract.do`
- JSON 버전 비교 리포트: `/figma/fetchAndAnalyzeFigmaData.do`, `/figma/analyzeRecentVersions.do`

## 8. 알려진 한계 / 다음 단계

- 좌우 분할은 그리드가 나란히 있을 때만 인식. 폼|그리드 분할은 위→아래로 나열된다.
- 이벤트/JS 는 템플릿 주석 헤더만 복사(eXConverter-AI 와 동일).
- 테마(`clx-src/theme`)는 이 프로젝트 것을 사용. 템플릿 클래스(`btn-primary-01`, `search-box` …)의 모양까지 맞추려면 eXConverter-AI 테마를 적용할 것.
- `directFileID.clx` 화면에는 입력칸이 없어 설정값(`FIGMA_DIRECT_TOKEN`, `figma.direct.fileKey`)으로 동작한다. 화면에 토큰/URL 입력칸을 추가하면 파라미터로 전달된다.
