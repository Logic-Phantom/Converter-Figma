# Converter-Figma

Figma 디자인 파일을 `.clx` 확장자를 가진 XML 포맷으로 변환하는 도구입니다.  
이 도구는 Figma API를 통해 JSON 데이터를 가져와, eXBuilder6에서 사용하는 `.clx` 구조로 자동 변환해줍니다.

## 📌 목적

- **Figma → XML (`.clx`) 자동화**
- Figma 디자인을 기반으로 UI를 빠르게 eXBuilder6 프로젝트에 반영
- 반복적인 수작업을 줄이고 일관된 UI 구조 생성

## 🎨 Figma인 이유
  
| 도구         | 계층 구조 지원 | API 또는 추출 가능성       | 설명 |
|--------------|----------------|----------------------------|------|
| **Figma**    | ✅ 완전 지원     | ✅ Figma API (JSON 구조)     | 디자인 트리 전체를 JSON 형태로 제공하며, 자동화 및 계층 변환에 가장 적합 |
| **Sketch**   | ✅ 제한적 지원   | ✅ Sketch API 또는 JSON Export | macOS 환경 전용, 계층 정보 포함한 JSON 추출 가능 |
| **Adobe XD** | ⚠️ 제한적        | ⚠️ Plugin SDK 필요           | 공식 API는 미흡하나, Plugin 개발을 통해 JSON 형태의 구조 추출은 가능 |
| **Photoshop**| ⚠️ 제한적        | ⚠️ JavaScript 기반 추출 가능 | PSD 내부에 계층 정보는 존재하나, 외부 추출 구조가 복잡하고 자동화에 어려움 |
| **Zeplin**   | ❌ 불가능        | ✅ 일부 정보 추출 가능         | 계층 구조는 지원되지 않으며, 화면 기반 단일 요소 정보만 추출 가능 |

## 🛠 주요 기능

- ✅ **Figma API 연동**
  - Token 기반 또는 OAuth2 인증 방식 지원

- 🔁 **디자인 JSON → `.clx` 변환**
  - eXBuilder6 전용 XML 구조 생성

- 🧩 **지원 컴포넌트**
  - `<cl:button>`, `<cl:inputbox>`, `<cl:grid>`, `<cl:radiobutton>`, `<cl:group>` 등

- 🎨 **자동 스타일 적용**
  - 위치, 크기, 스타일 속성 자동 반영

- ✏️ **`.js` 스크립트 자동 생성**
  - 각 `.clx` 파일에 대응하는 자바스크립트 템플릿 포함

- 📁 **Figma 팀 → 프로젝트 → 파일 흐름 지원**


## 📂 프로젝트 구조

```
Converter-Figma/
├── src/ # 핵심 로직
│    ├── controller/ # 변환 요청을 처리하는 컨트롤러
│    ├── service/ # JSON → CLX 변환 로직
│    ├── web/ # JSON → CLX 변환 로직
├── resources/ # 예제
├── clx-src/# 클라이언트 소스(뷰)
├── .gitignore
└── README.md
```
## ▶️ 사용 방법

### 1. Figma API Token 수동 발급

- https://www.figma.com/developers/api#access-tokens 참고
- 발급된 Token은 컨트롤러에서 직접 사용하거나 `.properties`, `.env` 파일로 관리할 수 있습니다.

### 2. OAuth 인증 방식 (자동 토큰 발급 지원)

본 프로젝트는 **Figma OAuth2 인증**을 지원합니다.  
사용자는 로그인 후 자동으로 Access Token을 발급받고 `.clx` 변환 프로세스를 시작할 수 있습니다.

- https://www.figma.com/developers/apps 참고
- 직접 앱을 등록하여 clientID, clientSecret 설정

#### 🔑 OAuth 연동 흐름

```
사용자 → Figma 로그인 → Redirect (code) → Access Token 발급 → JSON 변환 → .clx 생성
```

#### 🔗 예시 URL 호출

```
https://www.figma.com/oauth?client_id=YOUR_CLIENT_ID &redirect_uri=YOUR_REDIRECT_URI &scope=file_read &state=STATE &response_type=code
```

#### ⚙️ 설정 예시 (.env 또는 config.properties 등)

```properties
figma.clientId=YOUR_CLIENT_ID
figma.clientSecret=YOUR_CLIENT_SECRET
figma.redirectUri=http://localhost:8080/oauth/callback.do

```
🔄 Callback 처리

```
/oauth/callback.do 엔드포인트에서 access_token을 발급받아 .clx 변환 흐름에 자동 연동됩니다.

변환된 결과는 clx-src/ 디렉토리 하위에 .clx 및 .js 파일로 저장됩니다.
```

## 📌 기능 분류

프로젝트는 아래 세 가지 방식의 변환 API 엔드포인트를 제공합니다:

1. **직접 수동 방식 (convertDirect.do)**  
   - 사용자가 직접 Figma URL과 토큰을 하드코딩하여 단일 파일을 변환합니다.  
   - 테스트나 간단한 작업에 유용합니다.
   
2. **OAuth 인증 후 단일 프로젝트/파일 변환 (convert.do)**  
   - OAuth 인증을 통해 발급받은 토큰을 사용하여,  
   - 지정된 팀 ID 내 첫 번째 프로젝트의 첫 번째 파일만 변환합니다.
   
3. **OAuth 인증 후 전체 프로젝트/파일 변환 (convertAll.do)**  
   - 동일하게 OAuth 인증을 통해 토큰을 사용하여,  
   - 지정된 팀 ID 내 **모든** 프로젝트의 **모든** 파일을 순회하며 변환합니다.
   

---
## ⚙️ 사용 예시

### 1. 직접 수동 방식 - `/design/convertDirect.do`

- **설명**:  
  미리 하드코딩된 Figma URL과 토큰("figd_...")을 사용하여 단일 파일만 변환합니다.
  
- **특징**:
  - 빠른 테스트 및 디버깅용.
  - 프로젝트나 파일 선택 없이 단순 변환.

- **호출 예시 (GET)**:

```
http://localhost:8080/design/convertDirect.do
```

- **응답 예시**:

```
CLX file saved successfully at: C:\Users\LCM\git\Converter-Figma\clx-src\2025-04-10\design12345.clx
```

---

### 2. OAuth 인증 후 단일 프로젝트/파일 변환 - `/design/convert.do`

- **설명**:  
클라이언트에서 전달받은 파라미터(`dmParam`) 내의 OAuth 토큰을 이용하여  
**하드코딩된 팀ID** 내 첫 번째 프로젝트의 첫 번째 파일을 변환합니다.

- **특징**:
- 팀 ID와 연동하여 Figma API를 호출 (단, 단일 파일만 처리).
- OAuth 인증 후 받은 토큰으로 요청.

- **호출 예시 (GET/POST)**:

```
http://localhost:8080/design/convert.do
```

요청 파라미터 예:
- dmParam.token : OAuth Access Token

- **응답 예시**:
```
CLX file saved successfully at: C:\Users\LCM\git\Converter-Figma\clx-src\2025-04-10\design67890.clx
```
---

### 3. OAuth 인증 후 전체 프로젝트/파일 변환 - `/design/convertAll.do`

- **설명**:  
클라이언트에서 전달받은 OAuth 토큰을 사용하여  
**하드코딩된 팀ID** 내 모든 프로젝트의 모든 파일을 순회하며 변환합니다.

- **특징**:
- 팀의 전체 프로젝트와 각 프로젝트의 모든 파일에 대해 CLX 파일 생성.
- 각 프로젝트와 파일 별로 처리 결과를 로그로 반환합니다.

- **호출 예시 (GET/POST)**:
```
http://localhost:8080/design/convertAll.do
```
요청 파라미터 예:
- dmParam.token : OAuth Access Token

- **응답 예시**:
```
Project ID: 1420657369280493518 File: Homepage (fileKey123) ✅ Saved: C:\Users\LCM\git\Converter-Figma\clx-src\2025-04-10\design00123.clx File: About (fileKey456) ✅ Saved: C:\Users\LCM\git\Converter-Figma\clx-src\2025-04-10\design00456.clx ...
```
