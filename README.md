# Figma to CLX Converter (eXCoverter-Figma)

Figma REST API가 돌려주는 파일 JSON(`GET /v1/files/:key`, `GET /v1/files/:key/nodes`)을 eXBuilder6 `.clx` + `.js` 쌍으로 변환한다.
**v2.0부터 좌표를 그대로 옮기지 않고, `templates/` 의 표준 화면 뼈대 중 Figma 화면과 가장 유사한 템플릿을 골라 그 위에 내용을 채운다.**

> 이 문서 하나로 목적, 동작 원리, 설정, 사용법, 검증, 변경 이력을 파악할 수 있도록 작성했다.
> 코드를 고치기 전에 **§11 작업 규칙**을 먼저 확인할 것.

## 목차
0. [한눈에 보기](#0-한눈에-보기)
1. [동작 원리](#1-동작-원리-v20-템플릿-파이프라인)
2. [변환 예시](#2-변환-예시)
3. [API 엔드포인트와 출력](#3-api-엔드포인트와-출력)
4. [설정](#4-설정)
5. [2025~2026 Figma API 변경과 반영 내역](#5-20252026-figma-api-변경과-반영-내역)
6. [검증 방법](#6-검증-방법)
7. [폴더 구조](#7-폴더-구조)
8. [v2.0 작업 내역 (2026-09-19)](#8-v20-작업-내역-2026-09-19)
9. [문제 해결](#9-문제-해결)
10. [알려진 한계 / 다음 단계](#10-알려진-한계--다음-단계)
10.5 [AI 보조 변환 (v2.1, 선택)](#105-ai-보조-변환-v21-선택)
11. [작업 규칙](#11-작업-규칙)
12. [기타 기능 (v1.x)](#12-기타-기능-v1x)

---

## 0. 한눈에 보기

### 무엇을 하나
Figma 디자인(프레임) → eXBuilder6 화면(`.clx` + `.js`). 회사 표준 화면 패턴(헤더 UDC, 조회영역, 본문, 푸터, 스타일 클래스, 간격 규칙)을 가진 **템플릿을 자동으로 골라**, 필드·그리드 컬럼·버튼·텍스트만 Figma 내용으로 채운다.

### v1.x 와 v2.0 비교

| 항목 | v1.x (좌표 복사) | v2.0 (템플릿 기반) |
|---|---|---|
| 레이아웃 | 모든 요소를 `cl:xylayout` 절대좌표로 배치 | 가장 유사한 템플릿(P1~P8)의 formlayout/verticallayout 뼈대 사용 |
| 컨트롤 판별 | 레이어 이름에 "input"/"radio" 등이 들어있는지 | INSTANCE 의 **원본 컴포넌트셋/컴포넌트명** + 보이는 내부 아이콘 |
| 버튼 캡션 | 레이어명 또는 직계 TEXT (대부분 "Button") | `componentProperties` TEXT 값 → 보이는 텍스트 |
| 표(그리드) | 이름이 정확히 `table` 일 때 5열 빈 그리드 | 헤더/컬럼 폭/행선택 체크박스/rowindex/셀 편집기/페이징/엑셀 여부 추출 |
| 숨김 레이어 | 그대로 출력 | `visible:false` 제외 |
| UDC | `udcComAppHeader` 만 | AppHeader, GridTitle(ctrl·엑셀), FormTitle, **GridCudBtns** |
| 결과 검증 | 없음 | `ClxValidator` (중복 id/sid, 그리드·formlayout 범위) + e6-compiler |
| 저장 경로 | `C:\Users\LCM\...` 하드코딩 | 워크스페이스에서 프로젝트 루트 자동 탐색 |
| OAuth | `file_read` scope (2025-11-17 폐지 → 로그인 실패) | `file_content:read projects:read`, Basic 인증, state 검증 |

### 빠른 시작
1. 토큰 준비: Figma 개인 액세스 토큰(PAT, scope `file_content:read`)을 환경변수 `FIGMA_DIRECT_TOKEN` 에 넣고 Eclipse Tomcat 재시작.
2. 브라우저에서 `GET /design/convertDirect.do?url=<Figma 프레임 링크>` 호출 (또는 `directFileID.clx` 의 확인 버튼 → 설정된 기본 파일).
3. 결과 확인: `clx-src/convertTest/<오늘날짜>/<프레임명>.clx` 를 eXBuilder6 에서 연다. 응답 본문에 선택된 템플릿과 영역 요약이 나온다.
4. 저장해 둔 JSON 이 있으면 `convertJson.clx` 화면에서 업로드(`/design/jsonConvert.do`).

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
| 1 | `figma/FigmaApiClient` | Figma URL/파일키 파싱, `?node-id=` 가 있으면 `/nodes?ids=` 로 해당 프레임만 요청. PAT=`X-Figma-Token`, OAuth=`Bearer` |
| 2 | `figma/FigmaDocument` | files/nodes 두 응답 형식 지원. INSTANCE → `components`/`componentSets` 로 **원본 컴포넌트명** 해석. 화면(프레임) 목록 결정 |
| 3 | `figma/FigmaComponentClassifier` | 노드 → eXBuilder 컨트롤 종류 (키워드는 `figma/component-keywords.properties` 로 확장) |
| 4 | `figma/FigmaUiIrExtractor` | 레이어 계층을 믿지 않고 텍스트/컨트롤/표를 절대좌표 원자로 모아 위→아래로 영역(title/search/form/grid/tabs/buttons…) 재구성 |
| 5 | `exconverter/*` | eXConverter-AI 의 템플릿 엔진(UI-IR 파싱·정규화 → 템플릿 선택 → CLX 생성 → 검증 → 저장) |
| 6 | `figma/FigmaConversionService` | 위를 묶는 서비스. 화면별 결과(선택 템플릿, 영역 요약, 경고, 저장 경로) 반환 |

왜 UI-IR 을 거치나: eXConverter-AI(설계서 이미지 → AI → UI-IR → CLX)와 **같은 중간 표현과 같은 생성기**를 쓰므로 템플릿·생성 규칙 개선이 두 프로젝트에 그대로 통한다. 입력만 "이미지+AI" 대신 "Figma JSON"이다.
Figma JSON 은 텍스트·컴포넌트·좌표가 정확하므로 **추출은 결정적**이다(같은 입력 → 같은 결과). AI/외부 모델 호출 없음.

### 1.1 Figma JSON 에서 읽는 것

| Figma 필드 | 용도 |
|---|---|
| `components[id].name`, `componentSets[id].name` | INSTANCE 의 정체. 레이어명이 `Frame 1000004302` 여도 `Base-input`/`Button`/`Radio-button` 으로 판별 |
| `componentProperties` (TEXT) | 버튼 캡션 (`Button name#67:81 = 조회`). 레이어명 `버튼` 이 아니라 실제 글자 |
| `characters`, `style.fontSize/fontWeight` | 라벨·제목·헤더 텍스트, 제목/섹션제목 판별, 폼테이블 판별. `U+2028` 줄 구분자는 줄바꿈으로 |
| `absoluteBoundingBox` | 행/열 분할, 라벨↔컨트롤 짝짓기, 그리드 컬럼 폭(`sourceWidth` 대비 1408px 로 환산) |
| `visible:false`, `opacity:0` | 숨김 레이어 제외 (예: 숨긴 `hint`, 숨긴 콤보 화살표) |

### 1.2 추출 규칙 (`FigmaUiIrExtractor`)

| 대상 | 규칙 |
|---|---|
| 화면 | 페이지(CANVAS)의 겹치지 않는 최상위 FRAME(320×200 이상) 각각 = 화면 1개. 캔버스에 레이어가 흩어져 있거나 프레임이 겹치면 페이지 전체 = 화면 1개. SECTION 은 내부 프레임을 화면으로. node-id 지정 시 그 노드만 |
| 컨트롤 | 컴포넌트셋명 → 컴포넌트명(`Size=Medium` 같은 variant 이름 제외) → 레이어명 순으로 키워드 매칭. `Base-input` 안에 **보이는** arrow-down 아이콘 → combobox, calendar → dateinput, search → searchinput |
| 프레임형 컨트롤 | 컴포넌트가 아닌 FRAME/GROUP 도 이름이 `btn_login`, `input_id` 처럼 컨트롤이고 내부에 다른 컨트롤이 없으며 텍스트가 1개 이하이면 컨트롤. 와이어프레임의 `RECTANGLE 'InputBox-8'` 도 컨트롤로 |
| 무시 | icon, breadcrumb, logo, divider, 벡터/도형, 입력칸 위에 겹친 placeholder 텍스트 |
| 화면 제목 | `title`/`AppHeader` 프레임의 가장 큰 텍스트, 없으면 상단의 18px 이상 최대 글자 → `udcComAppHeader.title` |
| 조회/폼 | 같은 줄의 `라벨 텍스트 → 컨트롤(들)` 을 필드로. 라벨 뒤 라디오/체크박스 여러 개 = 옵션, 날짜 2개 = daterange, `*` = 필수, `-`/`~` 구분자, 라벨 위쪽 배치도 인식. 줄당 필드 수 = `columnsPerRow` |
| 폼 제목행 | 필드 묶음 바로 위(60px 이내)의 제목 텍스트·버튼 줄 → 폼 `title`/`buttons` |
| 조회 vs 폼 | 첫 필드 묶음이 캡션이 정확히 `조회/검색/찾기` 인 버튼을 갖거나, 페이지 제목 바로 아래 + 아래에 그리드가 있고 자체 제목이 없으면 search. 제목("프로젝트상세")이 있으면 form. `평가의뢰 조회` 같은 업무 버튼은 조회 버튼으로 보지 않음 |
| 폼 테이블 | `table_register` 처럼 `라벨│값│라벨│값` 인 표는 그리드가 아니라 form (이름에 register/form/detail/등록/상세, 헤더 행에 입력 컨트롤, 헤더 행 글꼴 스타일이 섞임 중 하나). 라벨 뒤 일반 텍스트 = 읽기전용 `output` 필드. 원을 숨긴 Radio-button(값 표시용) = 텍스트 |
| 그리드 | 이름에 table/grid/테이블/그리드 또는 구조(3열 이상 정렬된 3행 이상, 입력 컨트롤 없음, 표 아래 다른 요소 없음). 헤더 = 텍스트가 가장 많은 첫 행(한 가지 글꼴 스타일), 헤더 왼쪽 체크박스 = 행선택 열, `NO/번호` = rowindex, 셀 안 버튼/입력 = 편집기(5행 다수결), `FNM`·`USER_ID` 같은 대문자 식별자(2글자 이상) = 바인딩 컬럼명 |
| 그리드 제목행 | 그리드 바로 위(64px 이내)의 텍스트 = 제목, `총 123건` = 건수(UDC가 그림 → 버림), 버튼 = 제목행 버튼, 엑셀/다운로드 버튼 = `excel:true` |
| 페이징 | 그리드 안/바로 아래(80px) Pagination 컴포넌트 → `paging:true` (pageindexer) |
| 탭 | `Base-Tab` 등 tab 컴포넌트 줄(`table` 은 제외) → tabs, 그 아래 영역은 `inTab`(마지막 푸터 버튼 제외) |
| 좌우 분할 | 한 띠에 그리드가 좌우로 나란히 있으면 가장 넓은 가로 간격에서 나눠 `side: left/right` |
| 버튼 줄 | 마지막 버튼 줄 = 푸터, 정렬(left/center/right)은 위치로 |
| 팝업 | 프레임명에 popup/팝업/dialog/modal/레이어, 또는 폭 1000px 미만 → `screen.type=POPUP` (`*_P.clx` 템플릿 우선) |

### 1.3 템플릿 선택과 생성 (`exconverter/*`, eXConverter-AI 에서 이식)

- `templates/**/*.clx` 전체를 프로파일링(조회영역 유무, 팝업, 푸터, 본문 블록열 `G`/`F`/`TAB{…}`/`DIV{…|…}` 등)하고 UI-IR 과 점수 비교 → 최고점 템플릿.
  점수 = 100 + 조회영역 일치 ±40 − 팝업 불일치 80 + 푸터 일치 5 − 블록열 편집거리 − 탭/트리 불일치 60 − 셔틀 열 불일치 40. 동점이면 작은(단순한) 템플릿.
- 템플릿을 `templates/` 에 추가하면 재시작/재학습 없이 후보가 된다(표준 구조 `grpHeader/grpSearch/grpData/grpFooter`, 클래스 `content-header/search-box/content-body/content/content-footer`).
- 생성기는 템플릿 DOM 을 복제해 **뼈대(헤더 UDC, search-box, content-body, footer, 스타일 클래스, 간격)는 유지**하고 필드/컬럼/버튼/텍스트만 Figma 값으로 바꾼다. 새 노드의 id/sid 는 중복 없이 생성.
- 상세 규칙은 eXConverter-AI `README.md` §6~§9 참고.

템플릿 목록(`templates/`, 78개): P0 Inner, P1 Single(버티컬=조회+그리드 / 폼=조회+폼), P2 Multi(그리드 여러 개), P3 List, P4 Master-Detail, P5 Tab, P6 Tree, P7 Shuttle, P8 Thirdparty, `*_P` = 팝업, `Form.clx`(기존).

### 1.4 UDC 반영 (`clx-src/udc/com`)

| UDC | 생성 규칙 (e6-compiler 로 컴파일 결과 확인) |
|---|---|
| `udcComAppHeader` | `title` = 화면 제목 |
| `udcComGridTitle` | `title` = 그리드 제목(없으면 첫 그리드는 화면명, 이후 `목록 N`), **`ctrl` = 그리드**(건수/엑셀 연동), **`showExportExcel`** = 디자인에 엑셀 버튼이 있는지 |
| `udcComFormTitle` | `title` = 폼 제목 |
| `udcComGridCudBtns` | 그리드 제목행에 행추가/추가/신규 또는 행삭제/삭제가 있으면 개별 버튼 대신 이 UDC 생성. `grid` = 그리드, 라벨(`buttonNewLabel`/`buttonDelLabel`/`buttonRestoreLable`/`buttonSaveLabel`) = 디자인 캡션, 디자인에 없는 버튼은 `visible*Button=false`. 나머지 버튼(예: 퇴록)은 일반 버튼. 저장 버튼만 있으면 UDC 를 쓰지 않음 |

control 타입 UDC 속성 문법: `<cl:property name="grid" type="control" value="grd1"/>` → 컴파일 결과 `udc.grid = linker.grid_1`.
선택된 템플릿의 그리드 블록에 제목행 버튼 자리가 없으면(P1-4 등) 생성기가 `title-button-group`(flowlayout, 오른쪽 정렬)을 추가한다. 자리를 만들 수 없으면 경고에 `제목행 버튼을 둘 자리가 없어 생략` 이 남는다.

---

## 2. 변환 예시

`clx-src/json/2025-08-04/2025-08-04_e69f5327.json` (프레임 `main_content_area`, 1654×940).

**Figma 원본 (일부)**
```
FRAME 'title'      → TEXT '우리보건소 등록카드' (24px, 700)          + Breadcrumb(무시)
FRAME '검색어'      → 'Selectbox' 프레임들: TEXT '예산연도' + INSTANCE Base-input(보이는 arrow-down 아이콘)
                                            TEXT '대상자구분' + INSTANCE Radio-button ×5
                   → INSTANCE Button(Button name=초기화), Button(Button name=조회)
TEXT '총 123회' + Button(삭제) + Button(퇴록) + btn_excel
FRAME 'table_40'   → 행 FRAME 'table1_00'(헤더) … + INSTANCE Pagination
GROUP 'edit1..4'   → Button ×4 (성인건강보험카드 등록 …)
```

**추출된 UI-IR (요약)**
```json
{ "screen": { "name": "우리보건소 등록카드", "sourceWidth": 1654 },
  "regions": [
    { "type": "title", "text": "우리보건소 등록카드" },
    { "type": "search", "columnsPerRow": 2, "buttons": ["초기화", "조회"], "fields": [
      { "label": "예산연도", "component": "combobox", "value": "전체" },
      { "label": "대상자구분", "component": "radiobutton", "options": ["전체", "성인 건강보험", "…"] },
      { "label": "성명", "component": "inputbox" }, { "label": "주민등록번호", "component": "inputbox" }, … ] },
    { "type": "grid", "paging": true, "excel": true, "buttons": ["삭제", "퇴록"], "columns": [
      { "header": "", "editor": "checkbox", "width": 42 }, { "header": "NO", "editor": "rowindex" },
      { "header": "예산연도", "editor": "output", "width": 100 }, …,
      { "header": "다른연도 등록", "editor": "button", "cellText": "등록" } ] },
    { "type": "buttons", "align": "right", "buttons": ["성인건강보험카드 등록", "…"] } ] }
```

**선택된 템플릿**: `P1_Single Pattern/폼/Single Pattern P1-4.clx` (점수 140, 조회영역 + 페이징 그리드 + 푸터)

**생성된 CLX (그리드 제목행 일부)**
```xml
<cl:udc id="udccomgridtitle1" type="udc.com.udcComGridTitle">
  <cl:property name="title" type="string" value="우리보건소 등록카드"/>
  <cl:property name="ctrl" type="control" value="grd1"/>
  <cl:property name="showExportExcel" type="boolean" value="true"/>
</cl:udc>
<cl:group class="title-button-group">
  <cl:udc id="udccomgridcudbtns1" type="udc.com.udcComGridCudBtns">
    <cl:property name="grid" type="control" value="grd1"/>
    <cl:property name="visibleNewButton" type="boolean" value="false"/>
    <cl:property name="buttonDelLabel" type="string" value="삭제"/> …
  </cl:udc>
  <cl:button class="btn-secondary-03 btn-md" value="퇴록"/>
</cl:group>
```

두 번째 예: `2025-04-14_08acdcca.json` (`프로젝트관리`) → `title → form(프로젝트상세, 필드 9: 읽기전용 값 5 + 기간 + 콤보 + 입력 + 날짜) → tabs(6) → grid(inTab, 페이징)` → **P5-2 탭 패턴**.

---

## 3. API 엔드포인트와 출력

| URL | 입력 | 설명 |
|---|---|---|
| `GET /design/convertDirect.do` | `token`, `url` 또는 `fileKey`, `nodeId` (모두 선택) | 개인 액세스 토큰(PAT). 빠진 값은 `FIGMA_DIRECT_TOKEN` / `figma.direct.fileKey` (directFileID.clx) |
| `/design/convert.do` | dmParam `token`(OAuth), 선택 `url`/`fileKey`/`nodeId` | 파일 지정이 없으면 `figma.team.id` 팀의 첫 프로젝트 첫 파일 (converterStart.clx) |
| `/design/convertAll.do` | dmParam `token`(OAuth) | 팀의 모든 프로젝트 × 모든 파일 |
| `/design/jsonConvert.do` | 업로드 JSON | 저장해 둔 Figma 응답 JSON 변환 (convertJson.clx) |
| `/design/jsonConvertLegacy.do` | 업로드 JSON | v1.x 좌표 변환기(xylayout, 템플릿 미사용) — 비교용 |
| `/designForm/convertAdvanced.do` | dmParam `token`(PAT), `url`/`fileKey`, `nodeId` | 템플릿 변환 |
| `/designForm/convertFigmaToFormClx.do` | 없음 | 설정된 direct 파일 템플릿 변환 (convertForm.clx) |
| `/oauth/login.do`, `/oauth/callback.do` | | Figma OAuth → `converterStart.clx` 로 토큰 전달 |

`url` 에 Figma 링크(`https://www.figma.com/design/<KEY>/<이름>?node-id=12-34`)를 그대로 넣으면 해당 프레임만 `/nodes` API 로 받아 변환한다(응답이 작고 빠름). `file/design/proto/board` 링크 모두 인식.

응답 본문(text/plain, UTF-8) 예:
```
✅ main_content_area → 템플릿 P1_Single Pattern/폼/Single Pattern P1-4.clx
    영역: title → search(필드 6) → grid(컬럼 15) → buttons
    저장: C:\eclipse_AI\Converter-Figma\clx-src\convertTest\2026-09-19\main_content_area.clx
```
실패한 화면은 `❌ 화면명: 사유`. 한 화면이라도 성공하면 200, 전부 실패면 500, 입력 오류는 400.

Eclipse 콘솔에는 `[eXConverter HH:mm:ss]` 로 진행 로그(영역 요약 → 템플릿 선택/점수 → 생성·검증 → 저장)가 찍힌다.

### 출력
```
clx-src/
├── convertTest/{yyyy-MM-dd}/{프레임명}.clx / .js   ← 변환 결과 (같은 프레임은 덮어씀, 이름 중복 시 _2)
├── json/{yyyy-MM-dd}/{파일명}_{id}.json             ← 받은 Figma 원본 JSON
└── udc/com/                                          ← 템플릿이 쓰는 공통 UDC
generated/ui-ir/{프레임명}.ui-ir.json                 ← 추출된 UI-IR (Tomcat 작업 디렉터리 기준, 디버깅용)
```
`.js` 는 선택된 템플릿의 짝 JS(주석 헤더)를 파일명/날짜만 바꿔 복사한다.

---

## 4. 설정

`src/main/resources/application.properties` (Figma) 와 `src/main/resources/exconverter/exconverter.properties` (템플릿 엔진).
우선순위: JVM `-Dkey=value` > 환경변수(`.`→`_`, 대문자. 예 `FIGMA_CLIENT_SECRET`) > properties 파일.
Eclipse Tomcat 에 환경변수/`-D` 를 줄 때는 Servers 뷰 → 서버 더블클릭 → Open launch configuration → Arguments/Environment.

| 키 | 기본값 | 설명 |
|---|---|---|
| `figma.client.id` / `figma.client.secret` / `figma.redirect.uri` | | OAuth 앱 (secret 은 환경변수 권장) |
| `figma.oauth.scope` | `file_content:read projects:read` | `file_read` 는 2025-11-17 폐지. OAuth 앱 설정에도 같은 scope 를 켜야 함 |
| `figma.team.id` | `1420657369280493518` | convert.do / convertAll.do 대상 팀 (Figma 에 "내 팀 목록" API 가 없음) |
| `figma.direct.token` | (빈값) | PAT. **환경변수 `FIGMA_DIRECT_TOKEN` 사용** (최대 90일 만료) |
| `figma.direct.fileKey` | `x5gR79q0HUZ567W3CjCuCJ` | convertDirect.do 기본 파일 (URL 도 가능) |
| `figma.analysis.fileKey` | `rXU0zhKF2HjzFsND9njYbq` | 접근성/토큰/비교 분석 대상 파일 |
| `exconverter.template.root` | (프로젝트 `templates/`) | 템플릿 루트 강제 지정 |
| `exconverter.template.useProjectFolder` | `true` | 소스의 `templates/` 를 직접 읽음(추가 즉시 반영). false 면 배포본 `WEB-INF/classes/exconverter/templates` |
| `exconverter.clx.result.folder` | `convertTest` | 결과 폴더(`clx-src/` 아래) |
| `exconverter.clx.result.root` | (빈값) | 결과 루트 절대경로 (위 폴더 설정보다 우선) |
| `exconverter.project.root` | (자동) | clx-src 를 가진 프로젝트 루트. 자동 탐색은 `Converter-Figma`/`eXCoverter-Figma` 우선 |
| `exconverter.generated.root` | `generated` | UI-IR 디버그 파일 루트 |

컴포넌트 키워드 확장: `src/main/resources/figma/component-keywords.properties` (예: 사내 디자인시스템의 `SelectField` 를 콤보로 → `component.combobox=combobox,combo,select,dropdown,selectfield`). 줄을 주석 해제하면 그 종류의 기본 키워드를 **대체**한다.

---

## 5. 2025~2026 Figma API 변경과 반영 내역

| 변경 (Figma changelog) | 영향 | 반영 |
|---|---|---|
| 2025-11-17 `file_read`/`files:read` scope 폐지 | 기존 OAuth 로그인 실패 | `file_content:read projects:read` 로 교체 |
| OAuth 토큰 교환: client 자격증명은 Basic 헤더 | 본문 전송 방식 비권장 | Basic 헤더 + 랜덤 `state` 세션 검증(CSRF) |
| 2025-04 PAT 최대 90일 | 하드코딩 토큰 만료 | 토큰은 요청 파라미터/환경변수 |
| `/v1/files/:key/nodes?ids=` | 파일 전체(수 MB) 대신 프레임만 | Figma URL 의 `node-id` 자동 사용 |
| Tier 1 rate limit (2025-11 조정) | 429 | 429 시 `Retry-After` 를 포함한 메시지, 403 은 만료/scope 안내 |
| 2026-08-10 `/v1/teams/:id/projects`, `/v1/projects/:id/files` deprecated | 중첩 폴더 팀은 v2 folders API | 현재는 v1 유지(주석 표기). 폴더 사용 팀이면 `GET /v2/teams/:team_id/folders` 로 교체 필요 |
| 2025-07 Grid auto-layout(`layoutMode: GRID`, `gridRowCount` 등) | 폼 레이아웃 힌트 | 미사용 (좌표 기반 추출로 충분). 향후 폼 열 수 판단에 활용 가능 |

검토했지만 채택하지 않은 것:
- **Figma Dev Mode MCP 서버(`get_design_context`)**: LLM 이 React/HTML 코드를 만드는 용도라, "항상 같은 결과 + eXBuilder 에서 열리는 CLX" 가 목표인 이 변환에는 맞지 않는다. REST 와 같은 노드 데이터를 쓰므로 필요하면 입력원으로만 바꿀 수 있다.
- **AI(LLM) 로 CLX 직접 생성**: eXConverter-AI 원칙과 같이 CLX 는 생성기가 결정적으로 만든다. Figma 는 구조가 정확해 AI 가 필요 없다.
- **auto-layout → flowlayout 직접 매핑**: 디자이너의 auto-layout(`SPACE_BETWEEN`, 절대배치 자식 등)이 eXBuilder 레이아웃과 1:1 이 아니고, 템플릿 뼈대를 쓰는 편이 표준 화면에 더 가깝다.

---

## 6. 검증 방법

```powershell
# 1) 전체 컴파일 + 저장된 Figma JSON 전부 → UI-IR → 템플릿 → CLX → ClxValidator
$cp = ((Get-ChildItem src\main\webapp\WEB-INF\lib\*.jar).FullName) -join ';'
javac -encoding UTF-8 -d out -cp "$cp;ci-lib\clx\cleopatra_server.jar" (Get-ChildItem -Recurse src\main\java -Filter *.java).FullName
javac -encoding UTF-8 -d out -cp "out;$cp" tools\harness\FigmaHarness.java
java -cp "out;$cp;src\main\resources" FigmaHarness templates out\figma clx-src\json
# 2) eXBuilder6 헤드리스 컴파일: .project/.settings/clx-src(*.json, udc 포함) 를 가진 임시 폴더의 clx-src 에 out\figma 의 clx/js 복사 후
java -jar ci-lib\clx\e6-compiler.jar -s <임시프로젝트> -o <출력>
```

`FigmaHarness` 출력: 화면마다 `OK/FAIL 파일__번호 [프레임] → 템플릿 (점수)` 와 영역 요약, `out\figma` 에 `.clx`/`.js`/`.ui-ir.json`. 실패가 있으면 종료코드 1.

2026-09-19 결과:
- `clx-src/json` 의 47개 응답 중 **46개 화면 생성·ClxValidator 통과**. 1개(`2025-05-12`)는 `10건` 텍스트 조각뿐이라 "변환할 요소를 찾지 못했습니다" 로 끝나는 것이 정상.
- 46개 CLX 를 e6-compiler 로 컴파일 → **BUILD SUCCESS**, 46개 모두 `.clx.js` 생성. UDC 속성(`udc.grid = linker.grid_1`, `ctrl`, `showExportExcel`, `visible*Button`) 확인.
- 대표 결과: `우리보건소 등록카드` → P1-4, `프로젝트관리` → P5-2.
- 서비스 계층(`FigmaConversionService` → `GenerationService` → `ProjectRootResolver`)도 서버 없이 실행해 프로젝트 루트 탐색·저장 확인.
- 미확인: 실제 Tomcat 기동 후 화면 호출, 실제 Figma API/OAuth 호출(토큰 필요).

---

## 7. 폴더 구조

```
src/main/java/com/tomatosystem/
├── figma/                 ← v2.0 Figma → UI-IR
│   ├── FigmaApiClient          URL 파싱, files/nodes 요청, 인증 헤더, 429/403 메시지
│   ├── FigmaDocument           응답 래퍼, 컴포넌트셋 이름 해석, 화면 목록
│   ├── FigmaComponentClassifier 노드 → 컨트롤 종류, 아이콘으로 콤보/날짜 판별
│   ├── FigmaUiIrExtractor      원자 수집 → 그리드/폼/탭/버튼 영역 → UI-IR JSON
│   ├── FigmaConversionService  화면별 변환·저장, 원본 JSON 보관 (Spring @Service)
│   ├── FigmaSettings           -D > 환경변수 > application.properties
│   └── FigmaPaths              clx-src 하위 경로 (하드코딩 경로 대체)
├── exconverter/           ← 템플릿 엔진 (eXConverter-AI 에서 복사, §7.1 변경점)
│   ├── model/UiIr               UI-IR 모델
│   └── service/                 UiIrParser, UiIrNormalizer, ColumnNames, LayoutShape, TemplateReverse,
│                                TemplateCatalog, ClxGenerator, ClxValidator, CompanionJsGenerator,
│                                GenerationService, ProjectRootResolver, ExConverterConfig, ProgressLog
├── web/                   ← 컨트롤러 (Design, AdvancedDesign, OAuth, 접근성/토큰/비교 분석)
├── service/, type/, utill/← v1.x 좌표 변환기(FigmaToClxService, *NodeConverter) 와 분석 서비스
src/main/resources/
├── application.properties            Figma 설정
├── exconverter/exconverter.properties 템플릿 엔진 설정, ui-ir.schema.json (UI-IR 계약)
└── figma/component-keywords.properties 컴포넌트 키워드 확장
templates/                 ← 화면 패턴 뼈대 78개 (P0~P8, *_P = 팝업) — 읽기 전용, WEB-INF/classes/exconverter/templates 로도 배포
clx-src/udc/com/           ← udcComAppHeader, udcComFormTitle, udcComGridTitle, udcComGridCudBtns
tools/harness/FigmaHarness.java ← 서버 없이 변환/검증 (Eclipse 빌드 대상 아님)
```

### 7.1 eXConverter-AI 와의 차이 (동기화 시 주의)
`exconverter/` 는 eXConverter-AI 와 같은 코드를 유지하되 다음만 다르다.
- `ProjectRootResolver`: 이 프로젝트(`Converter-Figma`/`eXCoverter-Figma`) 우선 탐색, 설정을 `ExConverterConfig` 로 읽음, `exconverter.clx.result.folder`.
- `UiIr` / `UiIrParser` / `ui-ir.schema.json`: grid `excel`(Boolean, 모르면 null).
- `ClxGenerator`: `udcComGridCudBtns` 생성, `udcComGridTitle.ctrl/showExportExcel`, 제목행 버튼 자리(`title-button-group`) 추가, `setUdcProperty` 타입(string/boolean/control) 지정.
- 가져오지 않은 것: AI 분석기(Ollama/Gemini), TemplateWatcher/Inspector, 이미지 업로드 컨트롤러.

---

## 8. v2.0 작업 내역 (2026-09-19)

### 8.1 분석 결과 (v1.x 의 문제)
- 좌표 복사 방식이라 표준 화면 구조가 나오지 않음. 컨트롤 판별이 레이어 이름 문자열에 의존("Button" 캡션이 대부분 `Button`).
- Figma 응답의 `components`/`componentSets`, `componentProperties`, `visible` 을 쓰지 않음.
- OAuth `file_read` scope 폐지로 로그인 불가. 토큰/파일키/팀ID/저장경로(`C:\Users\LCM\...`) 하드코딩.
- 버그: 좌표가 정수로 오거나 키가 없으면 `(double)` 캐스팅 `ClassCastException`, `escapeXml` 이 `"` 를 이스케이프하지 않아 XML 깨짐, 그리드는 이름이 정확히 `table` 일 때만 5열 빈 그리드.
- `/designForm/convertJsonToFormClx.do` 가 요청 파라미터로 받은 **서버 임의 경로를 읽고 씀**(보안 문제).

### 8.2 추가
| 파일 | 내용 |
|---|---|
| `figma/*` (7개) | Figma → UI-IR 추출기와 API 클라이언트·서비스·설정 (§1) |
| `exconverter/*` (14개) | eXConverter-AI 템플릿 엔진 이식 + §7.1 변경 |
| `templates/**` (77개 CLX + JS) | eXConverter-AI 템플릿 (기존 `Form.clx` 유지) |
| `clx-src/udc/com/*` | 템플릿이 쓰는 UDC 4종 |
| `src/main/resources/exconverter/*` | 엔진 설정, UI-IR 스키마 |
| `src/main/resources/figma/component-keywords.properties` | 키워드 확장 파일 |
| `tools/harness/FigmaHarness.java` | 오프라인 변환/검증 도구 |

### 8.3 변경
| 파일 | 내용 |
|---|---|
| `web/DesignController` | 모든 변환을 템플릿 파이프라인으로. Figma URL/node-id 지원, 토큰·파일키·팀ID 설정화, UTF-8 응답, 레거시 변환은 `jsonConvertLegacy.do` |
| `web/AdvancedDesignController` | 폼 레이아웃 실험을 템플릿 파이프라인으로 대체, 임의 경로 엔드포인트 제거 |
| `web/OAuthController` | scope 교체, Basic 인증 토큰 교환, state 검증, 설정은 `FigmaSettings` |
| `application.properties` | scope, 팀ID, direct 토큰/파일키 키 추가 |
| 분석 컨트롤러/서비스 6개 | 하드코딩 경로 → `FigmaPaths`, `"사용자 토큰"` → `FIGMA_DIRECT_TOKEN`, 분석 파일키 설정화 |
| `service/FigmaToClxService`, `type/InstanceNodeConverter` | 숫자 캐스팅 안전화(`Number`), 경로 설정화 |
| `utill/NodeConverterUtils` | `escapeXml` 에 `"` 추가 |
| `.settings/org.eclipse.wst.common.component` | `templates/` → `WEB-INF/classes/exconverter/templates` 배포 |

### 8.4 삭제
- `service/AdvancedFigmaToClxService`, `util/ClxLayoutUtil`: 폼 레이아웃 실험 코드. 템플릿 파이프라인이 대체(git 이력에 남아 있음).

### 8.5 샘플로 찾아 고친 추출 규칙
실제 저장된 Figma 응답으로 반복 검증하며 추가한 규칙:
- 숨긴 `right` 그룹의 arrow-down → inputbox, 보이면 combobox.
- `table_register`(등록 테이블)를 그리드로 오인 → 이름/헤더 행 컨트롤/헤더 글꼴 스타일로 폼 판별.
- 원을 숨긴 `Radio-button` 으로 값 표시 → 텍스트로 처리해 읽기전용 `output` 필드.
- `Base-Tab` 탭 인식, `table` 과 구분.
- 입력칸 위 placeholder 텍스트 제거, 헤더의 `U+2028` 줄 구분자 처리, 데이터값 `C34` 를 바인딩 컬럼명으로 오인하지 않도록 규칙 강화.
- `평가의뢰 조회` 같은 업무 버튼 때문에 폼이 조회영역이 되던 문제 → 정확한 조회 캡션만 인정.
- 선택된 템플릿(P1-4)에 그리드 제목행 버튼 자리가 없어 버튼이 사라지던 엔진 문제 → 자리 생성.

### 8.6 확인이 필요한 사항
- **보안**: `application.properties` 의 `figma.client.secret` 이 git 에 커밋되어 있다. Figma 개발자 설정에서 재발급하고 환경변수 `FIGMA_CLIENT_SECRET` 로 옮길 것.
- OAuth 앱 설정 화면에서 `file_content:read`, `projects:read` scope 활성화(projects 엔드포인트는 Figma 승인 필요할 수 있음).
- 팀이 중첩 폴더를 쓰면 v2 folders API 로 교체.

---

## 9. 문제 해결

| 증상 | 확인 |
|---|---|
| `Figma token is required` | `token` 파라미터 또는 환경변수 `FIGMA_DIRECT_TOKEN` 설정 후 Tomcat 재시작 |
| `Figma API 403` | PAT 만료(최대 90일) 또는 scope `file_content:read` 없음 |
| `Figma API rate limit (429)` | 메시지의 `Retry-After` 초만큼 대기. 파일 전체 대신 `node-id` 링크 사용 |
| OAuth `state 불일치` | 로그인부터 다시(세션 만료/다른 탭). `figma.redirect.uri` 가 앱 설정과 같은지 |
| `Node not found` | node-id 가 그 파일에 없음. 링크를 Figma 에서 다시 복사 |
| `화면에서 변환할 요소(…)를 찾지 못했습니다` | 선택한 노드에 텍스트/컨트롤/표가 없음 (아이콘·조각 등) |
| 결과가 다른 프로젝트에 저장됨 | `-Dexconverter.project.root=C:\eclipse_AI\Converter-Figma` |
| 콤보가 입력칸으로 나옴 | 화살표 아이콘이 숨김이거나 이름에 arrow-down/chevron-down 이 없음. `component-keywords.properties` 또는 디자인 컴포넌트명 확인 |
| 컨트롤이 인식 안 됨 | 컴포넌트셋/레이어 이름이 키워드에 없음 → `component-keywords.properties` 에 추가 |
| 원하는 템플릿이 안 골라짐 | `generated/ui-ir/<프레임>.ui-ir.json` 의 영역 순서 확인, 콘솔의 `템플릿 선택 … (점수, body=…)` 비교 |
| `Generated CLX is invalid: [...]` | ClxValidator 메시지 확인. 템플릿이 표준 구조를 벗어났는지 |
| 한글이 깨진 응답 | v2.0 응답은 `text/plain;charset=UTF-8`. 구버전 배포본인지 확인(Publish/재시작) |

---

## 10. 알려진 한계 / 다음 단계

- 좌우 분할은 그리드가 나란히 있을 때만 인식. 폼|그리드 분할은 위→아래로 나열된다.
- 이벤트/JS 는 템플릿 주석 헤더만 복사(eXConverter-AI 와 동일).
- 테마(`clx-src/theme`)는 이 프로젝트 것을 사용. 템플릿 클래스(`btn-primary-01`, `search-box` …)의 모양까지 맞추려면 eXConverter-AI 테마를 적용할 것.
- `directFileID.clx` 화면에는 입력칸이 없어 설정값(`FIGMA_DIRECT_TOKEN`, `figma.direct.fileKey`)으로 동작한다. 화면에 토큰/URL 입력칸을 추가하면 파라미터로 전달된다.
- 이미지/아이콘은 CLX 에 넣지 않는다(v1.x 도 비활성). 필요 시 `GET /v1/images/:key?ids=a,b,c` 한 번에 묶어 받아 저장하는 방식 권장(노드마다 호출하면 rate limit).
- Figma Variables(디자인 토큰)·Grid auto-layout 속성은 아직 미사용.

## 10.5 AI 보조 변환 (v2.1, 선택)

**설계 문서: [docs/ai-architecture.md](docs/ai-architecture.md)** — 후보 구성 비교, 안전장치, 비용/한도, 검증까지 정리.

§1 의 결정적 파이프라인은 그대로 두고, 애매한 판정과 이름 짓기만 AI 에게 물어보는 경로를 **별도 패키지·엔드포인트**로 추가했다.
`figma.ai.provider=none`(기본)이면 AI 호출이 전혀 없고 결과도 v2.0 과 동일하다.

```
Figma JSON → (규칙) UI-IR ─┬─ 컬럼 코드: 사전 → 캐시 → AI
                           ├─ 구조 검토: AI 는 화이트리스트 patch 만 제안 (+선택: 화면 렌더 PNG 동봉)
                           └─ 수리: 검증 실패 시 최소 수정 요청 (최대 2회) → 실패하면 규칙 결과로 복귀
                                        ↓
                        기존 UiIrParser → 템플릿 선택 → ClxGenerator → ClxValidator
```

| 제공자 | 비용 | 비고 |
|---|---|---|
| `none` (기본) | 0 | AI 사용 안 함 |
| `gemini` | 무료 등급 | `gemini-3.5-flash-lite`. **무료 등급은 입력이 Google 제품 개선에 사용** → 기밀 디자인 금지 |
| `ollama` | 0 | 사내 로컬 모델, 외부 전송 없음 |

안전장치 — AI 는 **CLX 를 만들지 않고 UI-IR 만, 그것도 허용된 op 만** 고친다:
`screen.name`, `region.type`(search↔form, description↔sectionTitle 만), `region.title`, `region.drop`(장식 영역만),
`field.label/component/required`, `column.header/editor/name`. 영역 추가·순서 변경·좌표·CLX 편집은 불가.
적용 결과가 UI-IR 규격을 벗어나거나 CLX 검증에 실패하면 **AI 보정을 버리고 규칙 결과로 되돌린다**.
적용/거부 내역은 응답과 `generated/ui-ir/<프레임>.ui-ir.json` 의 `ai` 에 남는다.

| URL | 설명 |
|---|---|
| `GET /designAi/convert.do` | `/design/convertDirect.do` 와 같은 파라미터(token, url\|fileKey, nodeId) + AI 보정 |
| `GET /designAi/preview.do` | 파일을 쓰지 않고 UI-IR before/after, patch, 선택될 템플릿을 JSON 으로 (프롬프트 튜닝용) |
| `/designAi/jsonConvert.do` | 업로드 JSON + AI 보정 |
| `GET /designAi/status.do` | provider/모델/키 설정 여부, 컬럼코드 캐시 크기 |

설정은 `application.properties` 의 `figma.ai.*` (§4 아래쪽). 키는 환경변수 `FIGMA_AI_GEMINI_APIKEY` 권장.
컬럼 코드 캐시는 `generated/ai-cache/label-codes.json` 에 쌓여 같은 라벨은 다시 묻지 않는다(반복 변환 시 API 0회).

검증(키 없이): `tools/harness/FigmaAiHarness.java` 가 로컬에 가짜 AI 서버를 띄워 7가지 시나리오를 검사한다.
```powershell
javac -encoding UTF-8 -d out -cp "out;$cp" tools\harness\FigmaAiHarness.java
java -cp "out;$cp;src\main\resources" FigmaAiHarness templates out\ai clx-src\json\2025-08-04\2025-08-04_e69f5327.json
```
2026-09-20 기준 7/7 통과(AI 끔·허용 patch 적용·금지 op 거부·깨진 응답 폴백·컬럼코드 캐시·보정 후 CLX 검증·CLX 직접수정 거부),
보정된 CLX 는 e6-compiler **BUILD SUCCESS**, 규칙 경로 회귀 46 OK 유지.

## 11. 작업 규칙

1. CLX XML 을 문자열로 직접 쓰지 말 것. 새 변환 규칙은 **UI-IR 추출(`FigmaUiIrExtractor`)** 에, 화면 구조 규칙은 **생성기/템플릿** 에 넣는다.
   AI 에게 CLX 를 만들게 하지 말 것(§10.5). AI 출력은 허용된 UI-IR patch 뿐이며, 새 op 를 추가하면 `UiIrPatch` 검증과 `FigmaAiHarness` 시나리오도 함께 추가한다.
2. `templates/` 는 읽기 전용 원본. 생성 결과를 쓰지 말 것.
3. 새 CLX 문법(컨트롤/속성/UDC 속성)은 **반드시 e6-compiler 로 컴파일해 생성 JS 를 확인**할 것. 추측 금지.
4. 추출 규칙을 바꾸면 `FigmaHarness` 로 `clx-src/json` 전체를 돌려 기존 결과(46개 OK, 대표 화면 템플릿)가 나빠지지 않았는지 확인하고, 새 실패 사례 JSON 은 `clx-src/json` 에 보관한다.
5. `exconverter/` 를 고치면 §7.1 에 기록하고 eXConverter-AI 와 동기화 여부를 판단한다.
6. Java 11 문법만 사용(Tomcat 은 21 로 실행). 새 jar 는 `WEB-INF/lib` 에 추가.
7. 토큰/시크릿은 코드·properties 에 넣지 말고 환경변수로.

## 12. 기타 기능 (v1.x)

- 웹 접근성 분석(WCAG 2.1, Excel 리포트): `/figma/accessibility/analyze.do` → `clx-src/result/webAccess`
- 디자인 토큰 추출(CSS/SCSS/Tailwind/JSON): `/figma/design-tokens/extract.do` → `clx-src/result/design-tokens`
- JSON 버전 비교 리포트: `/figma/fetchAndAnalyzeFigmaData.do`, `/figma/analyzeRecentVersions.do` → `clx-src/result/txt`, `excel`
