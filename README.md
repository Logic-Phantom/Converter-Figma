# Converter-Figma

Figma 디자인 파일을 `.clx` 확장자를 가진 XML 포맷으로 변환하는 도구입니다.  
이 도구는 Figma API를 통해 JSON 데이터를 가져와, eXBuilder6에서 사용하는 `.clx` 구조로 자동 변환해줍니다.

## 📌 목적

- **Figma → XML (`.clx`) 자동화**
- Figma 디자인을 기반으로 UI를 빠르게 eXBuilder6 프로젝트에 반영
- 반복적인 수작업을 줄이고 일관된 UI 구조 생성

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
├── src/ # 핵심 로직 │
├── controller/ # 변환 요청을 처리하는 컨트롤러 │
├── service/ # JSON → CLX 변환 로직 │
├── util/ # 공통 유틸리티
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

/oauth/callback.do 엔드포인트에서 access_token을 발급받아 .clx 변환 흐름에 자동 연동됩니다.

변환된 결과는 clx-src/ 디렉토리 하위에 .clx 및 .js 파일로 저장됩니다.

```
