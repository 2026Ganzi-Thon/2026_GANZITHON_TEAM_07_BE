# API 명세서

[README로 돌아가기](../README.md) · [서비스 기획안](service-overview.md) · [기능 명세서](features.md) · [데이터베이스 설계](database.md) · [AI 감정 대화 시스템](emotional-dialogue/README.md)

## 1. 개요 및 공통 규약

테슬랑 서비스의 백엔드 API 계약을 정의한 문서입니다.
**AI 대화 세션 생명주기 관리**, **마음 기록함 타임라인(Timeline Array)**, **JWT 인증(Refresh Token 포함)** 모델에 맞추어 설계되었습니다.

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
| **인증** | POST | `/api/v1/auth/login` | 로그인 및 Access/Refresh 토큰 발급 | X | F-00 |
| **인증** | POST | `/api/v1/auth/oauth/{provider}` | 소셜 로그인 (카카오, 디스코드, 구글) | X | F-00 |
| **인증** | POST | `/api/v1/auth/refresh` | Refresh Token 기반 Access/Refresh 토큰 갱신 | X | F-00 |
| **인증** | POST | `/api/v1/auth/logout` | 로그아웃 및 Refresh Token 폐기 | O | F-00 |
| **인증** | GET | `/api/v1/auth/me` | 내 프로필 정보 조회 | O | F-00 |
| **AI 세션** | POST | `/api/v1/counseling/sessions` | 상담 세션 생성 및 시작 (STEP 01~02) | O | F-01, F-02, F-07 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/messages` | 메시지 전송 및 AI 답변 수신 (STEP 02~05) | O | F-02, F-03, F-09 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/abort` | 사용자의 명시적 상담 중단 (조기 종료) | O | F-02 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/complete` | 상담 종료, 사후 강도 입력, 결과/타임라인 확정 및 말랑이 해금 | O | F-03, F-04, F-07, F-08 |
| **AI 세션** | DELETE| `/api/v1/counseling/sessions/{sessionId}` | 세션 영구 삭제 (위기 세션, 진행 중 세션, 완료 세션 일괄 삭제) | O | F-08, F-09 |
| **마음 기록함** | GET | `/api/v1/records` | 상담 결과 목록 조회 (페이징) | O | F-08 |
| **마음 기록함** | GET | `/api/v1/records/{resultId}` | 상담 결과 상세 조회 (타임라인 배열 포함) | O | F-08 |
| **마음 기록함** | DELETE| `/api/v1/records/{resultId}` | 상담 기록 및 연관 세션 삭제 | O | F-08 |
| **말랑이** | GET | `/api/v1/malangis` | 보유 말랑이 목록 및 현재 선택된 말랑이 조회 | O | F-04, F-05 |
| **말랑이** | PATCH | `/api/v1/malangis/{malangiType}/select` | 메인 화면 대표 말랑이 변경 | O | F-05 |
| **말랑이** | POST | `/api/v1/malangi/interactions` | 말랑이 조작 시계열 로그 저장 (언제, 얼마 동안, 몇 번 만졌는지) | O | F-05, F-06 |
| **말랑이** | GET | `/api/v1/malangi/interactions` | 최근 말랑이 조작 이력 조회 | O | F-05 |

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

### 3-2. 사용자 인증 및 토큰 관리 (F-00)

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
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 1,
    "nickname": "슬랑이친구"
  },
  "error": null
}
```

#### [POST] `/api/v1/auth/oauth/{provider}` — 소셜 로그인 (카카오, 디스코드, 구글)
- **인증:** 없음
- **Path Parameter:** `provider` (`kakao`, `discord`, `google`)
- **Request Body:**
```json
{
  "authorizationCode": "4/0AY0e-g7...",
  "redirectUri": "http://localhost:3000/oauth/callback/kakao"
}
```
- **처리 규칙:**
  1. 백엔드가 해당 공급자(OAuth2 Provider)의 토큰/프로필 엔드포인트와 통신하여 `providerId`, `email`, `nickname`을 획득합니다.
  2. **동일 이메일 가입 거부 정책:** DB에 이미 동일한 이메일을 가진 계정이 존재하고 공급자가 일치하지 않는 경우, 가입을 거부하고 기존 가입된 공급자(`existingProvider`)로 로그인하도록 안내합니다 (HTTP 409 Conflict).
  3. **신규 가입 (JIT):** 동일 이메일이 없는 경우 즉시 회원 레코드를 생성하고, 기본 말랑이(`SLANGI`, 슬랑이)를 자동 해금(`is_unlocked = true`, `is_selected = true`) 등록한 뒤 서비스 JWT 쌍을 발급합니다 (`isNewUser: true`).
  4. **기존 가입:** 이미 해당 소셜로 가입된 회원이면 즉시 로그인 처리 후 JWT 쌍을 발급합니다 (`isNewUser: false`).

- **성공 응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.access...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": 2,
    "email": "user@kakao.com",
    "nickname": "카카오친구",
    "provider": "KAKAO",
    "isNewUser": true
  },
  "error": null
}
```

- **실패 응답 — 동일 이메일이 이미 존재하는 경우 (HTTP 409 Conflict):**
```json
{
  "success": false,
  "data": {
    "existingProvider": "LOCAL",
    "registeredEmail": "user@example.com"
  },
  "error": {
    "code": "EMAIL_ALREADY_REGISTERED",
    "message": "이미 해당 이메일로 가입된 계정이 있습니다. LOCAL 계정으로 로그인해 주세요."
  }
}
```

#### [POST] `/api/v1/auth/refresh` — 토큰 갱신 (Refresh Token Rotation)
- **인증:** 없음
- **Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh..."
}
```
- **처리 규칙:** 기존 Refresh Token을 DB에서 검증 후 폐기(`is_revoked = true`) 처리하고, 새로운 Access Token과 새로운 Refresh Token을 한 쌍으로 발급합니다.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new_access...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.new_refresh...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "error": null
}
```

#### [POST] `/api/v1/auth/logout` — 로그아웃 (토큰 폐기)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refresh..."
}
```
- **응답 (HTTP 204 No Content):** (본문 없음)

---

### 3-3. AI 감정 상담 세션 및 실시간 대화 (F-01, F-02, F-03, F-07, F-09)

#### [POST] `/api/v1/counseling/sessions` — 세션 생성 및 시작 (STEP 01~02)
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
    "status": "IN_PROGRESS",
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
    "status": "IN_PROGRESS",
    "currentStep": "STEP_03",
    "aiMessage": "그런 생각이 드셨다면 마음이 정말 위축되고 속상하셨을 것 같아요. 혹시 팀원들이 나를 무시해서가 아니라, 마감 시간 때문에 회의를 서둘러서 그랬을 가능성도 있을까요? 그 순간 내가 사실 가장 바랐던 건 무엇이었나요?",
    "riskDetected": false
  },
  "error": null
}
```

#### 위기 신호 감지 시 응답 규약 (F-09)
위기 신호 감지 시 `status`는 `ABORTED_RISK`로 전환되고 `currentStep`은 감지 당시 단계가 유지됩니다.
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "status": "ABORTED_RISK",
    "currentStep": "STEP_02",
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

#### [POST] `/api/v1/counseling/sessions/{sessionId}/abort` — 조기 종료
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId`
- **처리 내용:** 사용자가 중단 의사를 표현했을 때 세션 상태를 `ABORTED_USER`로 전환하고 당시까지의 슬롯을 보존합니다.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "status": "ABORTED_USER",
    "currentStep": "STEP_03",
    "message": "상담이 중단되었습니다. 언제든 다시 대화를 시작하실 수 있어요."
  },
  "error": null
}
```

#### [POST] `/api/v1/counseling/sessions/{sessionId}/complete` — 상담 완료, 결과 확정 및 말랑이 해금
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId`
- **Request Body:**
```json
{
  "postIntensity": 4
}
```
- **처리 내용:**
  - 세션 `status`를 `COMPLETED`로 전환하고 `counseling_results` 레코드 및 `timeline` JSON을 확정 저장합니다.
  - 추천된 말랑이가 잠겨있던 상태라면 `user_malangis`에서 자동으로 해금(`is_unlocked = true`)하고 `isNewlyUnlocked: true` 플래그를 반환합니다.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 101,
    "sessionId": 501,
    "status": "COMPLETED",
    "preIntensity": 8,
    "postIntensity": 4,
    "intensityChange": -4,
    "summary": "회의 중 느낀 소외감 속에서 팀원들의 상황을 새롭게 바라보고, 다음 회의에서 메모로 먼저 소통하겠다는 작은 실천을 도출함.",
    "recommendedMalangi": {
      "type": "SQUISHY",
      "name": "스퀴시",
      "isNewlyUnlocked": true,
      "reason": "속상하고 답답했던 마음을 부드럽게 감싸 안으며 천천히 환기해 보세요."
    }
  },
  "error": null
}
```

#### [DELETE] `/api/v1/counseling/sessions/{sessionId}` — 세션 영구 삭제 (일원화)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId`
- **처리 내용:** 세션의 상태(`IN_PROGRESS`, `ABORTED_RISK`, `ABORTED_USER`, `COMPLETED`)와 무관하게 해당 세션과 연관된 메시지 및 상담 결과를 DB에서 완전히 영구 삭제합니다.
- **응답 (HTTP 204 No Content):** (본문 없음)

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
        "sessionId": 501,
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

#### [GET] `/api/v1/records/{resultId}` — 상담 결과 상세 조회 (타임라인 포함)
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

### 3-5. 말랑이 인벤토리 및 인터랙션 시계열 로깅 (F-04, F-05, F-06)

#### [GET] `/api/v1/malangis` — 보유 말랑이 목록 및 선택 상태 조회
- **인증:** 필요 (`Bearer <token>`)
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "malangis": [
      {
        "malangiType": "SLANGI",
        "name": "슬랑이",
        "isUnlocked": true,
        "isSelected": true,
        "totalPlayTime": 450,
        "lastInteractedAt": "2026-11-07T22:15:30"
      },
      {
        "malangiType": "SQUISHY",
        "name": "스퀴시",
        "isUnlocked": true,
        "isSelected": false,
        "totalPlayTime": 180,
        "lastInteractedAt": "2026-11-07T20:45:00"
      },
      {
        "malangiType": "WAKPPU",
        "name": "왁뿌볼",
        "isUnlocked": false,
        "isSelected": false,
        "totalPlayTime": 0,
        "lastInteractedAt": null
      }
    ]
  },
  "error": null
}
```

#### [PATCH] `/api/v1/malangis/{malangiType}/select` — 대표 말랑이 변경
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `malangiType` (`SLANGI`, `SQUISHY`, `WAKPPU`)
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "selectedMalangiType": "SQUISHY",
    "updatedAt": "2026-11-07T22:20:00"
  },
  "error": null
}
```

#### [POST] `/api/v1/malangi/interactions` — 말랑이 조작 시계열 로그 저장
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "sessionId": 501,
  "malangiType": "SQUISHY",
  "contextType": "AFTER_COUNSELING",
  "startedAt": "2026-11-07T20:38:10",
  "endedAt": "2026-11-07T20:40:10",
  "durationSeconds": 120,
  "interactionCount": 35
}
```
- **응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "logId": 1024,
    "malangiType": "SQUISHY",
    "startedAt": "2026-11-07T20:38:10",
    "endedAt": "2026-11-07T20:40:10",
    "durationSeconds": 120,
    "interactionCount": 35,
    "totalPlayTime": 300
  },
  "error": null
}
```

#### [GET] `/api/v1/malangi/interactions` — 최근 말랑이 조작 이력 조회
- **인증:** 필요 (`Bearer <token>`)
- **Query Parameters:**
  - `limit`: 조회할 로그 개수 (기본값: 10)
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "logs": [
      {
        "logId": 1024,
        "malangiType": "SQUISHY",
        "contextType": "AFTER_COUNSELING",
        "startedAt": "2026-11-07T20:38:10",
        "endedAt": "2026-11-07T20:40:10",
        "durationSeconds": 120,
        "interactionCount": 35
      }
    ]
  },
  "error": null
}
```
