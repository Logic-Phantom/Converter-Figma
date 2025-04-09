# Converter-Figma

Figma 디자인 파일을 `.clx` 확장자를 가진 XML 포맷으로 변환하는 도구입니다.  
이 도구는 Figma API를 통해 JSON 데이터를 가져와, eXBuilder6에서 사용하는 `.clx` 구조로 자동 변환해줍니다.

## 📌 목적

- **Figma → XML (`.clx`) 자동화**
- Figma 디자인을 기반으로 UI를 빠르게 eXBuilder6 프로젝트에 반영
- 반복적인 수작업을 줄이고 일관된 UI 구조 생성

## 🛠 주요 기능

- Figma API를 통해 디자인 요소(JSON) 가져오기
- Figma JSON → `.clx` XML 구조로 변환
- `.clx` 내 구성 요소: `<cl:button>`, `<cl:inputbox>`, `<cl:grid>`, `<cl:radiobutton>`, `<cl:group>` 등 지원
- 변환 시 위치, 크기, 스타일 속성 자동 적용
- `.js` 스크립트 파일도 함께 생성

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

1. **Figma API Token 발급**
   - https://www.figma.com/developers/api#access-tokens 참고
