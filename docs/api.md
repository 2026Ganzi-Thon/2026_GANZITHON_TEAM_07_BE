# API 명세서

[README로 돌아가기](../README.md) · [서비스 기획안](service-overview.md) · [기능 명세서](features.md) · [데이터베이스 설계](database.md) · [AI 감정 대화 시스템](emotional-dialogue/README.md)

## 1. 개요 및 공통 규약

테슬랑 서비스의 백엔드 API 계약을 정의한 문서입니다.
**AI 대화 세션 기반 아키텍처**와 **마음 기록함 타임라인(Timeline Array)** 모델에 맞추어 설계되었습니다.

- **Base URL:** `http://127.0.0.1:8080` (로컬) / 배포 URL 미정
- **API 기본 경로:** `/api/v1`
- **인증 방식:** `Authorization: Bearer <JWT_ACCESS_TOKEN>` (헤더 기반)
- **Content-Type:** `application/json; charset=UTF-8`
- **Swagger UI:** `http://127.0.0.1:8080/swagger-ui/index.html` (`.env`에서 `API_DOCS_ENABLED=true` 설정 시)

### 공통 응답 봉투 규약 (API Standard Response)

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

실패 시:
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

| 분류 | Method | URI | 설명 | 인증 | 관련 기능 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **시스템** | GET | `/api/v1/health` | 서버 헬스체크 | X | 기반 인프라 |
| **인증** | POST | `/api/v1/auth/signup` | 이메일 회원가입 | X | F-00 |
| **인증** | POST | `/api/v1/auth/login` | 로그인 및 JWT 토큰 발급 | X | F-00 |
| **인증** | GET | `/api/v1/auth/me` | 내 프로필 정보 조회 | O | F-00 |
| **AI 세션** | POST | `/api/v1/counseling/sessions` | 상담 세션 생성 및 시작 (STEP 01~02) | O | F-01, F-02, F-07 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/messages` | 메시지 전송 및 AI 답변 수신 (STEP 02~05) | O | F-02, F-03, F-09 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/complete` | 상담 종료, 사후 강도 입력, 결과/타임라인 확정 (STEP 06~07) | O | F-03, F-04, F-07, F-08 |
| **마음 기록함** | GET | `/api/v1/records` | 상담 결과 목록 조회 (페이징) | O | F-08 |
| **마음 기록함** | GET | `/api/v1/records/{resultId}` | 상담 결과 상세 조회 (타임라인 배열 포함) | O | F-08 |
| **마음 기록함** | DELETE| `/api/v1/records/{resultId}` | 상담 기록 및 연관 세션 삭제 | O | F-08 |
| **말랑이** | POST | `/api/v1/malangi/usages` | 말랑이 인터랙션 이력 저장 | O | F-05 |

---

## 3. 엔드포인트 상세 명세

### 3-1. 시스템 상태 확인 (Health API)

- **Method / URI:** `GET /api/v1/health`
- **인증:** 없음
- **응답 (HTTP 200):**
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
- **응답 (HTTP 201 Created):**
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
- **응답 (HTTP 200 OK):**
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

### 3-3. AI 감정 상담 세션 및 실시간 대화 (F-01, F-02, F-03, F-07, F-09)

#### [POST] `/api/v1/counseling/sessions` — 상담 세션 생성 및 시작 (STEP 01~02)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "situation": "팀 회의에서 내 의견이 가볍게 무시당한 것 같아 마음이 무겁고 답답해요.",
  "primaryEmotion": "속상함",
  "preIntensity": 8
}
```
- **응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "currentStep": "STEP_02",
    "aiMessage": "회의 중에 그런 소외감을 느끼셨군요. 정말 답답하고 서운하셨겠어요. 그때 상황에서 가장 먼저 머릿속에 떠오른 생각은 어떤 것이었나요?",
    "riskDetected": false
  },
  "error": null
}
```

#### [POST] `/api/v1/counseling/sessions/{sessionId}/messages` — 메시지 전송 및 대화 진행 (STEP 02~05)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId`
- **Request Body:**
```json
{
  "message": "‘팀원들이 나를 무능하다고 생각해서 내 의견을 듣지 않는구나’라는 생각이 가장 먼저 들었어요."
}
```
- **일반 성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "currentStep": "STEP_03",
    "aiMessage": "그런 생각이 드셨다면 마음이 정말 위축되고 속상하셨을 것 같아요. 혹시 팀원들이 나를 무시해서가 아니라, 마감 시간 때문에 회의를 서둘러서 그랬을 가능성도 있을까요? 그 순간 내가 사실 가장 바랐던 건 무엇이었나요?",
    "riskDetected": false
  },
  "error": null
}
```

#### 위기 신호 감지 시 특수 응답 규약 (F-09)
위험 신호 감지 시 세션 상태는 `ABORTED_RISK`로 전환되고 다음 응답이 반환됩니다.
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
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

#### [POST] `/api/v1/counseling/sessions/{sessionId}/complete` — 상담 종료 및 마음 기록 산출 (STEP 06~07)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId`
- **Request Body:**
```json
{
  "postIntensity": 4
}
```
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 101,
    "sessionId": 501,
    "preIntensity": 8,
    "postIntensity": 4,
    "intensityChange": -4,
    "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고, 다음 회의에서 메모로 먼저 소통하겠다는 작은 실천을 도출함.",
    "recommendedMalangi": {
      "type": "SQUISHY",
      "name": "스퀴시",
      "reason": "속상하고 답답했던 마음을 부드럽게 감싸 안으며 천천히 환기해 보세요."
    },
    "status": "COMPLETED"
  },
  "error": null
}
```

---

### 3-4. 마음 기록함 (F-08)

#### [GET] `/api/v1/records` — 상담 결과 목록 조회 (페이징)
- **인증:** 필요 (`Bearer <token>`)
- **Query Parameters:**
  - `page`: 페이지 번호 (기본값: 0)
  - `size`: 페이지 크기 (기본값: 10)
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "resultId": 101,
        "createdAt": "2026-11-07T20:30:00",
        "primaryEmotion": "속상함",
        "preIntensity": 8,
        "postIntensity": 4,
        "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고 실천을 도출함.",
        "recommendedMalangi": "SQUISHY"
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

#### [GET] `/api/v1/records/{resultId}` — 상담 결과 상세 조회 (타임라인 배열 포함)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `resultId`
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 101,
    "sessionId": 501,
    "createdAt": "2026-11-07T20:30:00",
    "situation": "팀 회의에서 내 의견이 가볍게 무시당한 것 같아 마음이 답답함.",
    "primaryEmotion": "속상함",
    "preIntensity": 8,
    "postIntensity": 4,
    "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고, 작은 실천을 도출함.",
    "recommendedMalangi": "SQUISHY",
    "timeline": [
      {
        "order": 1,
        "triggerMessageId": 3,
        "step": "STEP_02",
        "emotion": "속상함",
        "intensity": 8,
        "thought": "팀원들이 나를 무능하다고 생각하는구나",
        "timestamp": "2026-11-07T20:31:10"
      },
      {
        "order": 2,
        "triggerMessageId": 7,
        "step": "STEP_03",
        "alternativeThought": "마감 시간이 촉박해서 안건을 서둘러 넘겼을 수 있다",
        "underlyingNeed": "내 의견이 존중받고 팀에 기여하고 싶은 욕구",
        "timestamp": "2026-11-07T20:33:45"
      },
      {
        "order": 3,
        "triggerMessageId": 9,
        "step": "STEP_04",
        "copingStrategy": "과거에도 서운할 때 산책을 하거나 차분히 생각 정리를 했음",
        "timestamp": "2026-11-07T20:35:12"
      },
      {
        "order": 4,
        "triggerMessageId": 11,
        "step": "STEP_05",
        "actionPlan": "다음 회의 전에 메모로 안건을 먼저 정리해서 공유해보기",
        "timestamp": "2026-11-07T20:36:50"
      },
      {
        "order": 5,
        "triggerMessageId": 12,
        "step": "STEP_06",
        "intensity": 4,
        "intensityChange": -4,
        "recommendedMalangi": "SQUISHY",
        "timestamp": "2026-11-07T20:38:00"
      }
    ]
  },
  "error": null
}
```

#### [DELETE] `/api/v1/records/{resultId}` — 기록 삭제
- **인증:** 필요 (`Bearer <token>`)
- **응답 (HTTP 204 No Content):** (본문 없음)

---

### 3-5. 말랑이 인터랙션 기록 (F-05)

#### [POST] `/api/v1/malangi/usages` — 말랑이 이용 이력 저장
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "sessionId": 501,
  "malangiType": "SQUISHY",
  "durationSeconds": 120
}
```
- **응답 (HTTP 201 Created):**
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
