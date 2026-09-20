# AI 보조 변환 아키텍처 (v2.1, 무료 구성)

> 대상: `Figma JSON → eXBuilder6 CLX` 변환 품질을 더 올리는 방법.
> 전제: **기존 v2.0 결정적 파이프라인은 그대로 둔다.** AI 경로는 별도 패키지(`com.tomatosystem.figma.ai`)와
> 별도 엔드포인트(`/designAi/*`)로 추가하고, 설정이 없으면 기존 동작과 100% 같다.

---

## 1. 무엇이 부족한가 (v2.0 한계)

| 한계 | 예 | 결정적 규칙으로 풀기 어려운 이유 |
|---|---|---|
| 애매한 영역 판정 | `table_register`(폼) vs 진짜 그리드, 조회영역 vs 상세폼 | 디자이너마다 레이어 이름·구조가 달라 규칙이 계속 늘어남 |
| 의미 있는 이름 | 그리드 컬럼 → `COL1, COL2 …` | 한글 라벨 → 업무 컬럼코드 사전이 40개뿐 |
| 놓친 의도 | "필수" 표시가 색으로만, 숨은 탭 내용, 셀 편집기 종류 | 시각 정보/업무 맥락이 JSON 구조에 없음 |
| 실패 복구 | 검증 실패 시 사람이 수정 | 자동 재시도 로직 없음 |

## 2. 후보 구성 비교 (모두 무료)

| 방안 | 내용 | 장점 | 단점 | 결론 |
|---|---|---|---|---|
| **A. AI가 CLX 직접 생성** | Figma JSON → LLM → CLX XML | 구현 단순 | 매번 결과가 달라지고 eXBuilder에서 안 열릴 위험, 검증 불가, 토큰 큼(2MB JSON) | ✗ 채택 안 함 |
| **B. AI가 UI-IR 전체 생성** | Figma JSON → LLM → UI-IR → 기존 생성기 | 구조 유연 | 정확한 좌표/컴포넌트 정보를 버리고 모델 추론에 의존, 결과 비결정적 | ✗ |
| **C. 결정적 추출 + AI 검토/보정(패치)** | 규칙으로 UI-IR을 만들고, AI는 **제한된 수정 지시(patch)만** 생성 | 기본 품질 보장 + 애매한 부분만 개선, 실패 시 원본으로 폴백, 감사 가능 | 프롬프트/검증 코드 필요 | ✅ **채택** |
| **D. 스크린샷 교차검증** | Figma 렌더 PNG + UI-IR을 함께 AI에 제공 | 색/아이콘 등 JSON에 없는 단서 사용 | 이미지 토큰 비용, 무료 한도 소모 | ✅ C의 옵션으로 채택 |
| **E. 템플릿 선택을 AI에게** | 후보 템플릿 N개 중 선택 | 사람 감각에 가까움 | 현재 점수식이 이미 67/67 정확, 비결정성만 증가 | △ 동점(±10점)일 때만 |

**선택: C(+D, +E 제한적)** — "규칙이 뼈대, AI는 이견 제시".

## 3. 구성도

```
                    ┌────────────── 기존 v2.0 (그대로) ──────────────┐
Figma REST JSON ──▶ │ FigmaUiIrExtractor → UI-IR                    │
                    └───────────────┬───────────────────────────────┘
                                    │ (figma.ai.provider=none 이면 여기서 바로 기존 경로)
                                    ▼
        ┌──────────────── 신규: com.tomatosystem.figma.ai ─────────────────┐
        │ 1) UiIrCritic     압축 UI-IR (+선택: 프레임 PNG) → AI → patch[]   │
        │      · patch 는 화이트리스트 op 만 (type/label/component/editor…) │
        │      · 스키마·범위 검증 실패 op 는 버림, 전부 실패면 원본 유지      │
        │ 2) LabelCodeNamer 라벨/헤더 → 업무 컬럼코드. 사전 → 캐시 → AI 순   │
        │      · 결과는 파일 캐시(재변환 시 API 호출 0회)                   │
        │ 3) RepairLoop     ClxValidator 오류 → AI patch → 최대 2회 재생성   │
        └───────────────────────────┬──────────────────────────────────────┘
                                    ▼
                 UiIrParser → TemplateCatalog → ClxGenerator → ClxValidator → 저장
```

AI 제공자는 교체 가능(`AiClient`):

| provider | 비용 | 특징 |
|---|---|---|
| `none` (기본) | 0 | AI 호출 없음 = v2.0과 동일 |
| `gemini` | 무료 등급 | `gemini-3.5-flash-lite` 기준 하루 수백 요청. 텍스트+이미지. **무료 등급은 입력이 Google 제품 개선에 사용됨 → 사내 기밀 디자인 주의** |
| `ollama` | 0 | 사내 PC/서버의 로컬 모델. 외부 전송 없음. 텍스트 전용 권장(작은 모델도 UI-IR 검토는 가능) |

## 4. AI가 할 수 있는 일 / 못 하는 일

허용된 patch op (그 외는 거부):

| op | 대상 | 용도 |
|---|---|---|
| `region.type` | `search` ↔ `form`, `grid` → `form` 등 | 조회/상세 오판 교정 |
| `region.title` | 영역 제목 | 제목 누락 보정 |
| `field.label` / `field.component` / `field.required` | 필드 | 컨트롤 종류·필수 교정 |
| `column.header` / `column.editor` / `column.name` | 그리드 컬럼 | 셀 편집기·바인딩 컬럼코드 |
| `screen.name` | 화면명 | 파일명/타이틀 |
| `region.drop` | 영역 1개 | 장식/중복 영역 제거 |

- 영역 **추가**, 순서 변경, 좌표/폭 변경, CLX 직접 편집은 금지(구조는 규칙이 책임).
- 모든 patch 는 적용 후 `UiIrParser` 재파싱 + `ClxValidator` 를 통과해야 한다. 실패하면 **AI 이전 UI-IR로 되돌린다**.
- 적용된 patch 는 결과 JSON(`generated/ui-ir/<프레임>.ui-ir.json` 의 `aiPatches`)과 응답에 남겨 감사할 수 있다.

## 5. 비용/한도 관리

- 화면 1개당 호출 1회(+수리 시 최대 2회). 입력은 **압축 UI-IR**(원본 2MB JSON이 아니라 1~4KB 요약).
- 이름 캐시(`generated/ai-cache/label-codes.json`)로 같은 라벨은 다시 묻지 않음 → 반복 변환은 API 0회.
- 이미지 동봉은 옵션(`figma.ai.screenshot=true`), 긴 변 1024px로 축소.
- 무료 한도 초과(429)·키 없음·타임아웃은 **모두 조용한 폴백**(기존 결과로 계속 진행) + 경고 메시지.

## 6. 엔드포인트 (신규)

| URL | 설명 |
|---|---|
| `/designAi/preview.do` | 파일을 쓰지 않고 `UI-IR(before/after) + patch + 선택 템플릿`만 JSON으로 반환 (프롬프트 튜닝용) |
| `/designAi/convert.do` | AI 보정 후 CLX/JS 생성·저장 (`/design/convertDirect.do` 의 AI 버전) |
| `/designAi/status.do` | provider/모델/키 설정 여부, 캐시 크기 |

기존 `/design/*`, `/designForm/*` 는 변경하지 않는다.

## 7. 설정 (신규 키만 추가)

| 키 | 기본값 | 설명 |
|---|---|---|
| `figma.ai.provider` | `none` | `none` / `gemini` / `ollama` |
| `figma.ai.gemini.apiKey` | (빈값) | **환경변수 `FIGMA_AI_GEMINI_APIKEY` 권장** |
| `figma.ai.gemini.model` | `gemini-3.5-flash-lite` | 무료 한도가 큰 모델 |
| `figma.ai.gemini.url` | `https://generativelanguage.googleapis.com` | |
| `figma.ai.ollama.url` / `.model` | `http://127.0.0.1:11434` / `qwen3:4b` | 로컬 |
| `figma.ai.screenshot` | `false` | true면 Figma 렌더 PNG 동봉(이미지 토큰 사용) |
| `figma.ai.naming` | `true` | 컬럼 코드 이름 짓기 사용 |
| `figma.ai.repair` | `true` | 검증 실패 시 수리 루프 |
| `figma.ai.timeoutSeconds` / `.maxRetries` | `60` / `1` | |

## 8. 검증 방법

1. `tools/harness/FigmaAiHarness.java` — 로컬에 **가짜 AI 서버**를 띄워(키 없이) 요청 형식·patch 적용·폴백·캐시를 검사.
2. 실제 키가 있으면 `figma.ai.provider=gemini` 로 같은 하니스를 돌려 응답 품질 확인.
3. 회귀: AI 끔 상태에서 `FigmaHarness` 결과(46 OK)가 그대로인지 확인.

측정(2026-09-20, 가짜 AI 서버):

| 시나리오 | 결과 |
|---|---|
| provider=none → 호출 0회, UI-IR 변경 없음 | OK |
| 허용 patch 3건(컬럼 편집기/컬럼코드/필드 컨트롤) 적용 | OK |
| 금지 op 4건 거부(`grid→form`, `region.add`, 범위 밖 인덱스, 미지원 컨트롤) | OK |
| 깨진 응답 → 규칙 결과로 폴백 | OK |
| 컬럼 코드 12개 명명, 2회차는 캐시로 API 0회 | OK |
| 보정된 UI-IR → 템플릿 P1-4 → CLX 생성·ClxValidator 통과 → e6-compiler BUILD SUCCESS (`cl:combobox` → `cpr.controls.ComboBox`) | OK |
| `clx.replace` 같은 CLX 직접 수정 시도 거부 | OK |
| 규칙 경로 회귀(46 OK) | 변화 없음 |

실제 Gemini 키로는 아직 호출하지 않았다(키 필요). `figma.ai.provider=gemini` + 키 설정 후 `/designAi/preview.do` 로 먼저 확인할 것.

## 9. 다음 단계 후보

- 이벤트 JS 초안 생성(조회/저장 버튼 → submission 스켈레톤). 규칙만으로 가능, AI 불필요.
- 그리드 컬럼 코드 사전을 프로젝트 DB 스키마와 맞추기(AI 대신 실제 테이블 컬럼 사용).
- Figma Variables(디자인 토큰) → 테마 LESS 변수 매핑.
