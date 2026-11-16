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
| **AI 세션** | POST | `/api/v1/counseling/sessions` | 대화 첫 메시지로 상담 세션 시작 (STEP 01) | O | F-01, F-02, F-09 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/emotion` | 대화 안 감정 기록 카드 제출 (대표·부가 감정, 유발 상황, 상담 전 강도) | O | F-01, F-07 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/messages` | 메시지 전송 및 AI 답변 수신 (STEP 01~05) | O | F-02, F-03, F-09 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/abort` | 사용자의 명시적 상담 중단 (조기 종료) | O | F-02 |
| **AI 세션** | POST | `/api/v1/counseling/sessions/{sessionId}/complete` | 사후 강도 입력, STEP 06 정리, 결과/타임라인 확정 및 말랑이 해금 | O | F-03, F-04, F-07, F-08 |
| **AI 세션** | DELETE| `/api/v1/counseling/sessions/{sessionId}` | 세션 영구 삭제 (위기 세션, 진행 중 세션, 완료 세션 일괄 삭제) | O | F-08, F-09 |
| **마음 기록함** | GET | `/api/v1/records` | 상담 결과 목록 조회 (페이징) | O | F-08 |
| **마음 기록함** | GET | `/api/v1/records/{resultId}` | 상담 결과 상세 조회 (타임라인 배열 포함) | O | F-08 |
| **마음 기록함** | DELETE| `/api/v1/records/{resultId}` | 상담 기록 및 연관 세션 삭제 | O | F-08 |
| **말랑이** | GET | `/api/v1/malangis` | 보유 말랑이 목록, 고유 상태 및 현재 선택된 말랑이 조회 | O | F-04, F-05 |
| **말랑이** | PATCH | `/api/v1/malangis/{malangiType}/select` | 메인 화면 대표 말랑이 변경 | O | F-05 |
| **말랑이** | PATCH | `/api/v1/malangis/{malangiType}/state` | 말랑이 고유 상태 갱신 (왁뿌볼 왁스 깨짐 단계 등) | O | F-05 |
| **말랑이** | POST | `/api/v1/malangi/sessions/start` | 말랑이 만지기 세션 시작 (세션 ID 발급) | O | F-05, F-06 |
| **말랑이** | POST | `/api/v1/malangi/sessions/{sessionId}/end` | 말랑이 만지기 세션 종료 및 조작 집계 (사용 종료하기) | O | F-05, F-06 |
| **말랑이** | GET | `/api/v1/malangi/sessions` | 최근 말랑이 만지기 세션 이력 조회 | O | F-05, F-06 |

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
- **검증:** email 형식·최대 255자, password 8~72자(UTF-8 기준 최대 72바이트), nickname 최대 50자. 이메일은 앞뒤 공백 제거 후 소문자로 정규화합니다.
- **중복 이메일:** HTTP 409 `EMAIL_ALREADY_REGISTERED`; 응답의 `data`에 기존 `existingProvider`와 정규화된 `registeredEmail`을 반환합니다.
- 가입 완료 시 기본 말랑이 `SLANGI`를 해금 및 선택 상태로 지급합니다.
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
- `redirectUri`는 서버 환경변수에 등록된 해당 공급자의 URI와 완전히 일치해야 합니다. 사용자 이메일을 제공하고 검증 완료 상태임을 공급자가 확인해 주는 계정만 허용합니다. 미검증 이메일은 로그인할 수 없습니다.
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

#### [GET] `/api/v1/auth/me` — 내 프로필 조회
- **인증:** 필요 (`Bearer <Access Token>`)
- **응답 (HTTP 200 OK):** `userId`, `email`, `nickname`, `provider`를 포함한 공통 응답 envelope
- Refresh Token은 인증 토큰으로 사용할 수 없습니다. 로그아웃은 요청한 사용자 소유의 활성 Refresh Token만 폐기합니다.

인증 실패는 HTTP 401, 유효하지 않은 입력은 400, 동일 이메일 충돌은 409, OAuth 공급자 장애는 502로 반환합니다. 보호 API는 Access Token이 없거나 만료·위변조된 경우 401을 반환합니다.

---

### 3-3. AI 감정 상담 세션 및 실시간 대화 (F-01, F-02, F-03, F-07, F-09)

와이어프레임(2026-11-14) 흐름을 따른다. 응답의 `nextInput`이 다음에 보여 줄 입력을 알려 준다.

```text
POST /sessions (첫 메시지, STEP_01)
  → POST /messages … (nextInput=MESSAGE)
  → nextInput=EMOTION_CARD → POST /emotion (대표·부가 감정, 유발 상황, 상담 전 강도) → STEP_02
  → POST /messages … STEP_02~05 (생각 질문 아래 replyOptions 칩)
  → nextInput=POST_INTENSITY → POST /complete (상담 후 강도) → STEP_06 정리·기록·말랑이 추천
위기 신호(1차 서버 필터 → 2차 AI 분류기) → status=ABORTED_RISK, riskDetected=true, nextInput=NONE
```

| `nextInput` | 클라이언트 동작 |
| :--- | :--- |
| `MESSAGE` | 채팅 입력. `replyOptions`가 있으면 눌러서 고를 수 있는 답 예시 칩으로 보여 준다(고른 문장을 그대로 `message`로 보낸다) |
| `EMOTION_CARD` | 대화 안 감정 기록 카드. `emotionCard.triggerFactor`를 "유발 상황"에 미리 채우고, `primaryEmotions`·`secondaryEmotions`를 칩으로 보여 준다 |
| `POST_INTENSITY` | 상담 후 감정 강도(0~10) 입력 → `/complete` |
| `NONE` | 입력 없음 (위기 안내, 완료, 중단) |

- `aiMessage`의 줄바꿈(`\n`)은 공감 말풍선과 질문 말풍선을 나누는 자리다.
- 공통 대화 응답(`TurnData`): `sessionId`, `status`, `currentStep`, `aiMessage`, `riskDetected`, `emergencyHotlines`(위기일 때만 값), `replyOptions`, `nextInput`, `emotionCard`(`EMOTION_CARD`일 때만 값)

#### [POST] `/api/v1/counseling/sessions` — 대화 첫 메시지로 세션 시작 (STEP 01)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "message": "알바하다가 주문 두 개를 바꿔 냈는데 점장님이 손님 앞에서 뭐라 했어",
  "malangiSessionId": 302
}
```
- `malangiSessionId`는 선택. 말랑이 만지기 화면에서 '감정 정리하기'로 들어온 경우 직전 말랑이 세션 ID(본인 세션만).
- **응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "status": "IN_PROGRESS",
    "currentStep": "STEP_01",
    "aiMessage": "손님 앞에서 지적을 받아 많이 창피했을 것 같소.\n그때의 감정을 먼저 같이 정리해 보면 좋을 것 같소.",
    "riskDetected": false,
    "emergencyHotlines": [],
    "replyOptions": [],
    "nextInput": "EMOTION_CARD",
    "emotionCard": {
      "triggerFactor": "손님 앞에서 점장님에게 지적받음",
      "primaryEmotions": ["속상함", "불안함", "분노", "억울함", "슬픔", "서운함"],
      "secondaryEmotions": ["답답함", "외로움", "허무함", "서운함", "억울함"]
    }
  },
  "error": null
}
```
- 상황이 부족하면 `nextInput: "MESSAGE"`로 한 번 더 묻는다.

#### [POST] `/api/v1/counseling/sessions/{sessionId}/messages` — 메시지 전송 (STEP 01~05)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:** `{ "message": "나는 진짜 일 못하는 사람인가 봐 싶었어" }` (최대 2000자)
- **응답 (HTTP 200 OK):** `TurnData`
```json
{
  "success": true,
  "data": {
    "sessionId": 501,
    "status": "IN_PROGRESS",
    "currentStep": "STEP_03",
    "aiMessage": "손님 앞에서 지적을 받고 스스로를 그렇게 여길 만큼 마음이 많이 상했구려.\n'나는 진짜 일 못하는 사람인가 봐'라는 생각이 든 이유는 무엇이오?",
    "riskDetected": false,
    "emergencyHotlines": [],
    "replyOptions": [],
    "nextInput": "MESSAGE",
    "emotionCard": null
  },
  "error": null
}
```
- 단계 전환은 서버가 정한다. 단계가 바뀌면 같은 응답에 새 단계의 첫 질문이 담긴다.
- `nextInput`이 `MESSAGE`가 아닐 때 보내면 `409 INPUT_NOT_EXPECTED`.

#### [POST] `/api/v1/counseling/sessions/{sessionId}/emotion` — 감정 기록 카드 제출 (STEP 01 → 02)
- **인증:** 필요 (`Bearer <token>`)
- **Request Body:**
```json
{
  "primaryEmotion": "속상함",
  "secondaryEmotion": "허무함",
  "preIntensity": 7,
  "triggerFactor": "손님 앞에서 점장님에게 지적받음"
}
```
- `secondaryEmotion`, `triggerFactor`(카드에서 고친 유발 상황)는 선택. `preIntensity`는 0~10.
- **응답 (HTTP 200 OK):** `TurnData` (`currentStep: "STEP_02"`, 생각 질문과 `replyOptions` 칩 2개)

#### 위기 신호 감지 시 응답 규약 (F-09)
1차 서버 필터(정규식)에서 걸리면 AI를 부르지 않는다. 통과하면 2차 AI 안전 분류기를 상담 호출과 병렬로 부르고, 위기면 상담 응답을 기다리지 않는다. `status`는 `ABORTED_RISK`, `currentStep`은 감지 당시 단계를 유지한다. 결과(`counseling_results`)는 만들지 않는다.
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
      { "name": "자살예방상담전화", "tel": "109", "available": "24시간 연중무휴" },
      { "name": "정신건강위기상담전화", "tel": "1577-0199", "available": "24시간 연중무휴" },
      { "name": "청소년 전화", "tel": "1388", "available": "24시간 연중무휴" },
      { "name": "긴급 범죄 신고", "tel": "112", "available": "24시간 연중무휴" },
      { "name": "긴급 구조 신고", "tel": "119", "available": "24시간 연중무휴" }
    ],
    "replyOptions": [],
    "nextInput": "NONE",
    "emotionCard": null
  },
  "error": null
}
```
- 안전 분류기가 두 번 다 실패하면 안전 기본 문장을 보내고 대화는 이어 간다(Fail-Safe). 상담 호출이 실패하면 단계별 기본 문장을 보낸다(07번 이슈).

#### [POST] `/api/v1/counseling/sessions/{sessionId}/abort` — 조기 종료
- **인증:** 필요 (`Bearer <token>`)
- **처리 내용:** 세션 상태를 `ABORTED_USER`로 바꾸고 그때까지의 슬롯을 보존한다. 결과는 만들지 않는다.
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
- 대화 중 사용자가 "그만할래"라고 하면 AI가 받아 주고 `nextInput: "POST_INTENSITY"`가 온다. 이때 `/complete`를 부르면 그때까지 이야기로 정리·기록한다.

#### [POST] `/api/v1/counseling/sessions/{sessionId}/complete` — 상담 후 강도 입력, 정리(STEP 06), 결과 확정 및 말랑이 해금
- **인증:** 필요 (`Bearer <token>`)
- **호출 시점:** `nextInput`이 `POST_INTENSITY`일 때
- **Request Body:**
```json
{
  "postIntensity": 4
}
```
- 감정 기록 카드 전에 상담을 마쳤다면 `primaryEmotion`, `secondaryEmotion`(선택), `preIntensity`를 같이 보낸다. 없으면 `400 EMOTION_REQUIRED`.
- **처리 내용:**
  - STEP_06 정리 호출로 요약·남은 문장·기록함 문장·말랑이 추천 사유를 만든다. 추천 말랑이는 대표 감정 룰로 서버가 정한다(06번 이슈).
  - 세션 `status`를 `COMPLETED`로 바꾸고 `counseling_results`와 `timeline`을 저장한다.
  - 추천 말랑이가 잠겨 있으면 해금하고 `isNewlyUnlocked: true`.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 101,
    "sessionId": 501,
    "status": "COMPLETED",
    "aiMessage": "오늘 이야기를 정리해 보았소.\n처음엔 일 못하는 사람인가 싶었지만, 그날 손님이 몰리고 피곤했던 사정도 있었다는 걸 직접 찾았소.",
    "primaryEmotion": "속상함",
    "secondaryEmotion": "허무함",
    "preIntensity": 7,
    "postIntensity": 4,
    "intensityChange": -3,
    "summary": "손님 앞에서 지적을 받아 스스로가 일을 못한다고 느꼈어요",
    "keySentence": "“나는 일 못하는 사람인가 봐”보다 “실수 하나로 일 못하는 사람은 아닌 것 같아”가 지금은 더 가까운 말 같아요",
    "record": {
      "initialThought": "나는 진짜 일 못하는 사람인가 봐요.",
      "alternativeThought": "그날 손님이 몰리고 피곤했던 사정도 있었어요.",
      "actionPlan": "다음 근무 때 주문이 나갈 때 영수증과 한 번 더 대조해 보기"
    },
    "recommendedMalangi": {
      "type": "SQUISHY",
      "name": "스퀴시",
      "isNewlyUnlocked": true,
      "reason": "속상했던 마음을 부드럽게 감싸 쥐며 달래 봐요."
    }
  },
  "error": null
}
```
- `summary`는 기록함 카드 제목, `keySentence`는 추천 화면 "대화에서 남은 문장", `record`는 기록함 상세("그때의 생각", "새로 본 관점", "고른 실천")에 쓴다(사용자 1인칭 해요체).

#### [DELETE] `/api/v1/counseling/sessions/{sessionId}` — 세션 영구 삭제 (일원화)
- **인증:** 필요 (`Bearer <token>`)
- **처리 내용:** 세션 상태와 무관하게 세션, 메시지, 상담 결과를 영구 삭제한다. 본인 세션이 아니거나 없으면 `404`.
- **응답 (HTTP 204 No Content):** (본문 없음)

#### 오류 코드
| HTTP | code | 상황 |
| :--- | :--- | :--- |
| 400 | `INVALID_INPUT` | 빈 메시지, 2000자 초과, 강도 범위 밖, 남의 말랑이 세션 ID |
| 400 | `EMOTION_REQUIRED` | 감정 없이 완료 요청 |
| 404 | `SESSION_NOT_FOUND` | 세션이 없거나 본인 세션이 아님 |
| 404 | `RECORD_NOT_FOUND` | 기록이 없거나 본인 기록이 아님 (기록함 API) |
| 409 | `SESSION_CLOSED` | 이미 끝난 세션(완료, 중단, 위기) |
| 409 | `INPUT_NOT_EXPECTED` | `nextInput`과 다른 입력 |

---


### 3-4. 마음 기록함 (F-08)

상담을 완료(`/complete`)하면 `counseling_results`에 저장되고 기록함에 나온다. 위기로 멈춘 세션과 중단한 세션은 결과가 없어 나오지 않는다. 문장 칸은 정리(STEP 06)에서 만든 사용자 1인칭 해요체 문장이다(와이어프레임).

#### [GET] `/api/v1/records` — 상담 결과 목록 조회 (페이징)
- **인증:** 필요 (`Bearer <token>`)
- **Query Parameters:**
  - `page`: 페이지 번호 (기본값: 0)
  - `size`: 페이지 크기 (기본값: 10, 1~50)
- 최근 기록부터 정렬한다. 범위를 벗어나면 `400 INVALID_INPUT`.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "resultId": 101,
        "sessionId": 501,
        "createdAt": "2026-11-16T12:30:00",
        "primaryEmotion": "속상함",
        "secondaryEmotion": "허무함",
        "preIntensity": 7,
        "postIntensity": 4,
        "intensityChange": -3,
        "summary": "손님 앞에서 지적을 받아 스스로가 일을 못한다고 느꼈어요",
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
- `summary`는 기록함 카드 제목이다.

#### [GET] `/api/v1/records/{resultId}` — 상담 결과 상세 조회 (타임라인 포함)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `resultId`
- 본인 기록이 아니거나 없으면 `404 RECORD_NOT_FOUND`.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 101,
    "sessionId": 501,
    "createdAt": "2026-11-16T12:30:00",
    "situation": "알바 중 주문을 바꿔 내서 점장님이 손님 앞에서 지적함",
    "triggerFactor": "손님 앞에서 점장님에게 지적받음",
    "primaryEmotion": "속상함",
    "secondaryEmotion": "허무함",
    "preIntensity": 7,
    "postIntensity": 4,
    "intensityChange": -3,
    "summary": "손님 앞에서 지적을 받아 스스로가 일을 못한다고 느꼈어요",
    "keySentence": "“나는 일 못하는 사람인가 봐”보다 “실수 하나로 일 못하는 사람은 아닌 것 같아”가 지금은 더 가까운 말 같아요",
    "initialThought": "나는 진짜 일 못하는 사람인가 봐요.",
    "alternativeThought": "그날 손님이 몰리고 피곤했던 사정도 있었어요.",
    "underlyingNeed": "손님 앞 말고 따로 말해주길 바랐다",
    "copingStrategy": "친구랑 편의점에서 이야기하기",
    "actionPlan": "다음 근무 때 주문이 나갈 때 영수증과 한 번 더 대조해 보기",
    "recommendedMalangi": "SQUISHY",
    "malangiReason": "속상했던 마음을 부드럽게 감싸 쥐며 달래 봐요.",
    "timeline": [
      { "order": 1, "triggerMessageId": 2, "step": "STEP_01", "situation": "...", "triggerFactor": "...", "timestamp": "2026-11-16T12:20:10" },
      { "order": 2, "triggerMessageId": 5, "step": "STEP_02", "emotion": "속상함", "secondaryEmotion": "허무함", "intensity": 7, "thought": "나는 진짜 일 못하는 사람인가 봐", "timestamp": "..." },
      { "order": 3, "triggerMessageId": 11, "step": "STEP_03", "alternativeThought": "...", "underlyingNeed": "...", "timestamp": "..." },
      { "order": 4, "triggerMessageId": 13, "step": "STEP_04", "copingStrategy": "...", "timestamp": "..." },
      { "order": 5, "triggerMessageId": 19, "step": "STEP_05", "alternativeThought": "...", "actionPlan": "...", "timestamp": "..." },
      { "order": 6, "triggerMessageId": 21, "step": "STEP_06", "intensity": 4, "intensityChange": -3, "recommendedMalangi": "SQUISHY", "summary": "...", "keySentence": "...", "malangiReason": "...", "record": { "initialThought": "...", "alternativeThought": "...", "actionPlan": "..." }, "timestamp": "..." }
    ]
  },
  "error": null
}
```
- 와이어프레임 상세 화면: `initialThought` = "그때의 생각", `alternativeThought` = "새로 본 관점", `actionPlan` = "고른 실천". 대화에서 확인하지 못한 칸은 `null`.
- 건너뛴 단계는 타임라인 항목이 없을 수 있다. 상세 칸은 타임라인 마지막 STEP_06 항목을 기준으로 채운다.

#### [DELETE] `/api/v1/records/{resultId}` — 기록 삭제
- **인증:** 필요 (`Bearer <token>`)
- **처리 내용:** 기록과 연결된 상담 세션·메시지까지 영구 삭제한다. 본인 기록이 아니거나 없으면 `404 RECORD_NOT_FOUND`.
- **응답 (HTTP 204 No Content):** (본문 없음)

---


### 3-5. 말랑이 인벤토리, 상태 관리 및 만지기 세션 라이프사이클 (F-04, F-05, F-06)

#### [GET] `/api/v1/malangis` — 보유 말랑이 목록, 상태 및 선택 정보 조회
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
        "customState": {},
        "totalPlayTime": 450,
        "lastInteractedAt": "2026-11-07T22:15:30"
      },
      {
        "malangiType": "SQUISHY",
        "name": "스퀴시",
        "isUnlocked": true,
        "isSelected": false,
        "customState": {},
        "totalPlayTime": 180,
        "lastInteractedAt": "2026-11-07T20:45:00"
      },
      {
        "malangiType": "WAKPPU",
        "name": "왁뿌볼",
        "isUnlocked": false,
        "isSelected": false,
        "customState": {
          "crackedStage": 0
        },
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
- 잠긴 말랑이는 HTTP 409 `LOCKED`, 지원하지 않는 종류는 HTTP 400 `INVALID_TYPE`으로 응답합니다. 선택은 요청 사용자 인벤토리에만 적용합니다.
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

#### [PATCH] `/api/v1/malangis/{malangiType}/state` — 말랑이 고유 상태 갱신 (왁스 깨짐 등)
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `malangiType` (`SLANGI`, `SQUISHY`, `WAKPPU`)
- **Request Body:**
```json
{
  "customState": {
    "crackedStage": 2
  }
}
```
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "malangiType": "WAKPPU",
    "customState": {
      "crackedStage": 2
    },
    "updatedAt": "2026-11-07T22:25:00"
  },
  "error": null
}
```

#### [POST] `/api/v1/malangi/sessions/start` — 말랑이 만지기 세션 시작
- **인증:** 필요 (`Bearer <token>`)
- **설명:** 메인 화면 또는 퀵 메뉴에서 말랑이를 터치해 만지기 화면으로 진입할 때 호출하여 만지기 세션을 오픈합니다.
- **Request Body:**
```json
{
  "malangiType": "WAKPPU",
  "contextType": "MAIN"
}
```
- **응답 (HTTP 201 Created):**
```json
{
  "success": true,
  "data": {
    "sessionId": 302,
    "malangiType": "WAKPPU",
    "status": "IN_PROGRESS",
    "contextType": "MAIN",
    "startedAt": "2026-11-07T22:20:10",
    "customState": {
      "crackedStage": 0
    }
  },
  "error": null
}
```

#### [POST] `/api/v1/malangi/sessions/{sessionId}/end` — 말랑이 만지기 세션 종료 및 조작 집계
- **인증:** 필요 (`Bearer <token>`)
- **Path Parameter:** `sessionId` (말랑이 세션 ID)
- **설명:** 만지기 화면에서 '사용 종료하기', '감정 정리하기(상담 이동)', '수면 모드' 등을 눌러 이탈할 때 호출합니다. 클라이언트에서 집계한 인터랙션 카운트와 체류 시간을 일괄 반영하며, 필요 시 변경된 말랑이 상태(`customState`)도 함께 동기화합니다.
- **Request Body:**
```json
{
  "durationSeconds": 210,
  "touchCount": 45,
  "longPressCount": 12,
  "dragCount": 20,
  "endReason": "TO_COUNSELING",
  "customState": {
    "crackedStage": 3
  }
}
```
- `endReason`: `EXIT` (단순 종료/메인 복귀), `TO_COUNSELING` (감정 정리하기 상담 연계), `TO_SLEEP` (수면 모드 전환)
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "sessionId": 302,
    "malangiType": "WAKPPU",
    "status": "COMPLETED",
    "durationSeconds": 210,
    "touchCount": 45,
    "longPressCount": 12,
    "dragCount": 20,
    "totalInteractionCount": 77,
    "endReason": "TO_COUNSELING",
    "totalPlayTime": 210
  },
  "error": null
}
```

#### [GET] `/api/v1/malangi/sessions` — 최근 말랑이 만지기 세션 이력 조회
- **인증:** 필요 (`Bearer <token>`)
- **Query Parameters:**
  - `limit`: 조회할 세션 개수 (기본값: 10)
  - `limit`은 1~100 사이여야 합니다.
- **응답 (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "sessions": [
      {
        "sessionId": 302,
        "malangiType": "WAKPPU",
        "status": "COMPLETED",
        "contextType": "MAIN",
        "startedAt": "2026-11-07T22:20:10",
        "endedAt": "2026-11-07T22:23:40",
        "durationSeconds": 210,
        "totalInteractionCount": 77,
        "endReason": "TO_COUNSELING"
      }
    ]
  },
  "error": null
}
```
