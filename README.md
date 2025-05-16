# Figma to CLX Converter

## 🚀 주요 기능

### 1. Figma 디자인 변환
- Figma 디자인을 CLX 컴포넌트로 자동 변환
- 컴포넌트별 속성 및 스타일 자동 매핑
- 레이아웃 및 위치 정보 보존
- 이미지 및 벡터 자동 변환 지원

### 2. 인증 및 접근 방식
#### OAuth 2.0 인증
- Figma OAuth를 통한 자동 로그인
- Access Token 자동 발급 및 관리
- 팀/프로젝트/파일 접근 권한 관리

#### 직접 토큰 방식
- 수동 API 토큰 입력 지원
- 단일 파일 빠른 변환 가능

### 3. 프로젝트 관리 기능
#### 팀 단위 관리
- 팀 ID 기반 프로젝트 목록 조회
- 전체 프로젝트 일괄 변환
- 프로젝트별 파일 관리

#### 파일 관리
- 날짜별 자동 디렉토리 생성
- UUID 기반 중복 없는 파일명 생성
- JSON 원본 데이터 백업

### 4. 컴포넌트 변환
#### 기본 컴포넌트
- Button (`<cl:button>`)
- Input (`<cl:inputbox>`)
- Output (`<cl:output>`)
- Group (`<cl:group>`)
- Rectangle
- Vector/Image

#### 복합 컴포넌트
- Radio Button Groups
  - 자동 그룹화
  - 아이템 자동 생성
- Tables
  - 테이블 구조 자동 인식
  - 셀 병합 지원
- Frames
  - 중첩 프레임 지원
  - 상대 위치 계산
- Instances
  - 컴포넌트 인스턴스 처리
  - 속성 상속

### 5. 웹 접근성 분석
- WCAG 2.1 기준 준수 검사
- 접근성 문제 자동 감지
- 상세 리포트 생성
  - 요약 정보
  - 상세 분석
  - WCAG 기준별 분석
  - 컴포넌트별 분석
- Excel 리포트 출력
  - 한글 지원
  - 멀티 시트 구성
  - 스타일 자동 적용

### 6. 디자인 토큰 추출
- 색상 토큰
  - 브랜드 컬러
  - 시스템 컬러
  - 의미적 컬러
- 타이포그래피
  - 폰트 패밀리
  - 폰트 크기
  - 라인 높이
- 스페이싱
  - 여백
  - 간격
- 테두리
  - 반경
  - 스타일

### 7. 버전별 기능
#### v1.0
- 기본 컴포넌트 변환
- 단일 파일 처리
- 수동 토큰 방식

#### v1.1
- OAuth 인증 추가
- 팀 단위 관리
- 다중 파일 처리

#### v1.2
- 웹 접근성 분석
- Excel 리포트
- 디자인 토큰 추출

#### v1.3 (현재)
- 이미지/벡터 처리 개선
- 복합 컴포넌트 지원 강화
- 스타일 매핑 개선

## 🔧 시스템 요구사항
- Java 8 이상
- Spring Framework
- Apache POI (Excel 리포트용)
- Figma API 토큰 또는 OAuth 인증 정보

## 📝 사용 방법

### 1. OAuth 인증 방식
```properties
# application.properties 설정
figma.client.id=YOUR_CLIENT_ID
figma.client.secret=YOUR_CLIENT_SECRET
figma.redirect.uri=http://localhost:8080/oauth/callback.do
```

### 2. API 엔드포인트
#### 파일 변환
- `/design/convertDirect.do`: 단일 파일 직접 변환
- `/design/convert.do`: OAuth 토큰 기반 변환
- `/design/convertAll.do`: 전체 프로젝트 변환

#### 웹 접근성 분석
- `/accessibility/analyze.do`: 접근성 분석 실행
- `/accessibility/report.do`: Excel 리포트 생성

#### 디자인 토큰
- `/design/tokens/extract.do`: 디자인 토큰 추출
- `/design/tokens/export.do`: 토큰 내보내기

### 3. 출력 파일 구조
```
clx-src/
├── convertTest/    # CLX 변환 파일
│   └── YYYY-MM-DD/
│       ├── design12345.clx
│       └── design12345.js
├── json/          # JSON 백업
│   └── YYYY-MM-DD/
│       └── design12345.json
└── reports/       # 분석 리포트
    └── YYYY-MM-DD/
        └── accessibility_report_12345.xlsx
```

## 🎯 향후 계획
- [ ] AI 기반 컴포넌트 자동 인식
- [ ] 실시간 변환 모니터링
- [ ] 다국어 지원 확대
- [ ] 변환 성능 최적화
- [ ] 사용자 인터페이스 개선
