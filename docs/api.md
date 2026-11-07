# API 명세서

[README로 돌아가기](../README.md) · [서비스 기획안](service-overview.md) · [기능 명세서](features.md) · [데이터베이스 설계](database.md)

## 1. 개요 및 공통 규약

테슬랑 서비스의 백엔드 API 계약을 정의한 문서입니다.
Spring Boot 4 및 springdoc OpenAPI 3.0을 기반으로 관리되며, 해커톤 MVP 필수 기능(인증, AI 상담 세션, 마음 기록함, 말랑이 추천/이용, 위기 대응)을 포괄합니다.

- **Base URL:** `http://127.0.0.1:8080` (로컬) / 배포 URL 미정
- **API 기본 경로:** `/api/v1`
- **인증 방식:** `Authorization: Bearer <JWT_ACCESS_TOKEN>` (헤더 기반)
- **Content-Type:** `application/json; charset=UTF-8`
- **Swagger UI:** `http://127.0.0.1:8080/swagger-ui/index.html` (`.env`에서 `API_DOCS_ENABLED=true` 설정 시)

### 공통 응답 형식 (API Standard Response)

모든 API 응답은 일관된 봉투(Envelope) 포맷을 따릅니다.

**성공 응답 예시:**
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**실패 응답 예시:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_INPUT",
    "message": "감정 강도는 0에서 10 사이의 숫자여야 합니다."
  }
}
```

---

## 2. 엔드포인트 목록 요약

| 분류 | Method | URI | 설명 | 인증 필요 | 관련 기능 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **시스템** | GET | `/api/v1/health` | 서버 헬스체크 | X | 기반 인프라 |
| **인증** | POST | `/api/v1/auth/signup` | 이메일 회원가입 | X | F-00 |
| **인증** | POST | `/api/v1/auth/login` | 로그인 및 토큰 발급 | X | F-00 |
| **인증** | GET | `/api/v1/auth/me` | 내 프로필 정보 조회 | O | F-00 |
| **AI 상담** | POST | `/api/v1/counseling/start` | 감정 상담 세션 시작 (STEP 01~02) | O | F-01, F-02, F-07 |
| **AI 상담** | POST | `/api/v1/counseling/{recordId}/messages` | 답변 전송 및 AI 응답 수신 (STEP 02~05) | O | F-02, F-03, F-09 |
| **AI 상담** | POST | `/api/v1/counseling/{recordId}/complete` | 상담 종료, 사후 강도 입력, 말랑이 추천 (STEP 06~07) | O | F-03, F-04, F-07, F-08 |
| **마음 기록함** | GET | `/api/v1/records` | 날짜별 상담 기록 목록 조회 (페이징) | O | F-08 |
| **마음 기록함** | GET | `/api/v1/records/{recordId}` | 상담 기록 상세 조회 | O | F-08 |
| **마음 기록함** | DELETE| `/api/v1/records/{recordId}` | 상담 기록 삭제 | O | F-08 |
| **말랑이** | POST | `/api/v1/malangi/usages` | 말랑이 인터랙션 이력 저장 | O | F-05 |

---

## 3. 엔드포인트 상세 명세

### 3-1. 시스템 상태 확인 (Health API)

- **Method / URI:** `GET /api/v1/health`
- **인증:** 없음
- **성공 응답 (HTTP 200):**
```json
{
  "status": "UP"
}
```

---

### 3-2. 사용자 인증 (F-00)

#### [POST] `/api/v1/auth/signup` — 회원가입
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!",
  "nickname": "슬랑이친구"
}
```
- **성공 응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "슬랑이친구"
  },
  "error": null
}
```

#### [POST] `/api/v1/auth/login` — 로그인
- **Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```
- **성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "nickname": "슬랑이친구"
  },
  "error": null
}
```

---

### 3-3. AI 감정 상담 및 위기 대응 (F-01, F-02, F-03, F-07, F-09)

#### [POST] `/api/v1/counseling/start` — 상담 세션 시작 (STEP 01~02)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "situation": "팀 프로젝트 회의에서 내 의견이 무시당한 것 같아 마음이 계속 답답하고 힘들어요.",
  "primaryEmotion": "속상함",
  "secondaryEmotion": "답답함",
  "preIntensity": 8
}
```
- **성공 응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "recordId": 101,
    "currentStep": "STEP_02",
    "aiMessage": "회의 중에 그런 경험을 하셔서 정말 마음이 무겁고 답답하셨겠어요. 그때 상황에서 가장 먼저 머릿속에 스친 생각은 어떤 것이었나요?",
    "riskDetected": false
  },
  "error": null
}
```

#### [POST] `/api/v1/counseling/{recordId}/messages` — 대화 지속 (STEP 02~05)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `recordId` (상담 세션 ID)
- **Request Body:**
```json
{
  "message": "‘팀원들이 나를 무능하다고 생각해서 내 말을 안 듣는구나’라는 생각이 제일 먼저 들었어요."
}
```
- **성공 응답 (일반 진행 시 - HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "recordId": 101,
    "currentStep": "STEP_03",
    "aiMessage": "그렇게 생각하셨다면 정말 서운하고 위축되셨을 것 같아요. 혹시 팀원들이 내 의견을 무시한 것이 아니라, 단순히 시간 부족이나 다른 맥락 때문이었을 가능성도 있을까요? 그 순간 내가 정말로 원했던 건 무엇이었을까요?",
    "riskDetected": false
  },
  "error": null
}
```

#### 위기 신호 감지 시 특수 응답 규약 (F-09)
사용자 메시지에 자해, 자살, 타해, 폭력 등 위험 신호가 감지되면 대화는 즉시 중단되며 아래와 같은 안전 대응 응답이 반환됩니다.
```json
{
  "success": true,
  "data": {
    "recordId": 101,
    "currentStep": "ABORTED_RISK",
    "aiMessage": "혼자 감당하지 않아도 괜찮아요. 지금은 마음의 문제를 해결하기보다 안전하게 도움을 받는 것이 중요해요.",
    "riskDetected": true,
    "emergencyHotlines": [
      { "name": "자살예방상담전화", "tel": "109", "available": "24시간" },
      { "name": "정신건강위기상담전화", "tel": "1577-0199", "available": "24시간" },
      { "name": "청소년상담", "tel": "1388", "available": "24시간" },
      { "name": "긴급신고", "tel": "112 / 119", "available": "24시간" }
    ]
  },
  "error": null
}
```

#### [POST] `/api/v1/counseling/{recordId}/complete` — 상담 종료 및 말랑이 추천 (STEP 06~07)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `recordId`
- **Request Body:**
```json
{
  "postIntensity": 4,
  "alternativeThought": "팀원들이 나를 싫어해서가 아니라 마감 때문에 급해서 그랬을 수도 있다.",
  "actionPlan": "다음 회의 전에 안건을 메모로 먼저 공유해보기"
}
```
- **성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "recordId": 101,
    "preIntensity": 8,
    "postIntensity": 4,
    "intensityChange": -4,
    "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고, 다음 회의에서 메모로 먼저 소통하겠다는 작은 실천을 정리함.",
    "recommendedMalangi": {
      "type": "SQUISHY",
      "name": "스퀴시",
      "reason": "속상하고 답답했던 마음을 부드럽게 감싸 안으며 천천히 해소해보세요."
    },
    "status": "COMPLETED"
  },
  "error": null
}
```

---

### 3-4. 마음 기록함 (F-08)

#### [GET] `/api/v1/records` — 상담 기록 목록 조회 (페이징)
- **인증:** 필요 (`Bearer <token>`)
- **Query Parameters:**
  - `page`: 페이지 번호 (기본값: 0)
  - `size`: 페이지 크기 (기본값: 10)
- **성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "recordId": 101,
        "createdAt": "2026-11-07T20:30:00",
        "primaryEmotion": "속상함",
        "preIntensity": 8,
        "postIntensity": 4,
        "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라봄.",
        "recommendedMalangi": "SQUISHY",
        "status": "COMPLETED"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "error": null
}
```

#### [GET] `/api/v1/records/{recordId}` — 상담 기록 상세 조회
- **인증:** 필요 (`Bearer <token>`)
- **성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "recordId": 101,
    "createdAt": "2026-11-07T20:30:00",
    "situation": "팀 프로젝트 회의에서 내 의견이 무시당한 것 같아 마음이 답답함.",
    "primaryEmotion": "속상함",
    "secondaryEmotion": "답답함",
    "preIntensity": 8,
    "postIntensity": 4,
    "initialThought": "팀원들이 나를 무능하다고 생각해서 안 듣는구나.",
    "alternativeThought": "팀원들이 나를 싫어해서가 아니라 마감 때문에 급했을 수 있다.",
    "underlyingNeed": "내 의견이 존중받고 팀에 기여하고 싶은 마음",
    "copingStrategy": "과거에는 혼자 삭히거나 산책을 했었음",
    "actionPlan": "다음 회의 전 안건을 메모로 먼저 공유하기",
    "recommendedMalangi": "SQUISHY",
    "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고, 작은 실천을 도출함.",
    "messages": [
      { "sender": "USER", "step": "STEP_01", "content": "팀 프로젝트 회의에서..." },
      { "sender": "ASSISTANT", "step": "STEP_02", "content": "회의 중에 그런 경험을..." }
    ]
  },
  "error": null
}
```

#### [DELETE] `/api/v1/records/{recordId}` — 기록 삭제
- **인증:** 필요 (`Bearer <token>`)
- **성공 응답 (HTTP 204 No Content):** (본문 없음)

---

### 3-5. 말랑이 인터랙션 기록 (F-05)

#### [POST] `/api/v1/malangi/usages` — 말랑이 이용 이력 저장
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "recordId": 101,
  "malangiType": "SQUISHY",
  "durationSeconds": 120
}
```
- **성공 응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "usageId": 45,
    "malangiType": "SQUISHY",
    "durationSeconds": 120,
    "savedAt": "2026-11-07T20:45:00"
  },
  "error": null
}
```
