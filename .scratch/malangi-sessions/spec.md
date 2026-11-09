# Status: ready-for-agent

# 말랑이 만지기 세션 라이프사이클, 상태 영속화 및 상담 연계 스펙

## Problem Statement

사용자는 감정이 복잡하거나 답답할 때 메인 화면에서 대표 말랑이를 터치하여 감각적 상호작용(만지기, ASMR)을 통해 즉각적인 감정 환기를 경험하고자 합니다.
그러나 기존 시스템은 단순 일회성 조작 로그만을 임의로 기록하도록 되어 있어, 사용자가 언제 만지기를 시작해서 얼마나 머물렀는지(체류 시간), 얼마나 인터랙션을 수행했는지 등의 생명주기가 관리되지 않았습니다.
또한 왁뿌볼과 같이 왁스를 단계적으로 깨뜨리는 고유 인터랙션 상태가 앱 종료나 화면 이탈 시 유지되지 못하고 초기화되는 문제가 있었으며, 말랑이를 만진 후 "감정 정리하기" 버튼을 눌러 AI 상담으로 진입하더라도 이전의 환기 활동이 상담 세션과 연결되지 않아 사용자 감정 여정의 맥락이 단절되었습니다.

## Solution

1. **말랑이 만지기 세션 라이프사이클 관리:**
   - 사용자가 말랑이 만지기 화면에 진입할 때 전용 세션을 시작하여 고유 세션 ID를 발급합니다.
   - 터치, 롱프레스, 드래그 등의 잦은 조작은 클라이언트가 집계하고, 사용자가 "사용 종료하기"를 누를 때 총 체류 시간, 조작 카운트, 세션 종료 사유를 한 번에 서버에 전송하여 안전하게 마감합니다.
2. **말랑이 고유 상태 서버 영속화:**
   - 왁뿌볼의 왁스 깨짐 단계(`crackedStage`) 등 말랑이별 특수 상태를 사용자 인벤토리 엔티티에 영속화하여, 앱을 다시 켜도 이전 조작 상태가 보존되도록 합니다.
3. **감정 환기 ➔ AI 상담 여정 연계 추적:**
   - 만지기 화면에서 "감정 정리하기"를 통해 상담 대화로 전환할 때, 직전 말랑이 세션 ID를 상담 세션에 연결하여 "환기 후 상담 연계" 흐름을 온전히 기록하고 분석할 수 있도록 지원합니다.

## User Stories

1. As a service user, I want to start a malangi touch session when I tap a malangi on the main screen, so that my sensory grounding activity is tracked with an active session ID.
2. As a service user, I want my client app to aggregate multiple touches, long presses, and drag interactions locally without lagging, so that I experience seamless, responsive haptic and visual feedback.
3. As a service user, I want to see my cracked wax progress on Wakppuball safely saved, so that I can continue breaking it even after closing and reopening the application.
4. As a service user, I want to explicitly tap "End Usage" to end my touch session, so that my total play duration and interaction statistics are accurately stored.
5. As a service user, I want to tap "Organize Emotions" from the malangi touch screen to smoothly transition into an AI counseling session, so that the emotional relief I gained connects directly to deeper cognitive reflection.
6. As a service user, I want to navigate to Sleep Mode from the malangi quick menu, so that my exit reason is cleanly recorded as a transition to sleep.
7. As a service user, I want my cumulative play time on each malangi to increase whenever I complete a touch session, so that my inventory accurately reflects how much care and time I spent with each companion.
8. As a service user, I want to view my past malangi touch sessions in my history, so that I can see when and how long I engaged in sensory grounding.
9. As an unauthenticated guest, I want unauthenticated access to be rejected when starting or ending malangi sessions, so that my personal usage logs and malangi states are securely protected.
10. As a mobile client developer, I want to pass optional custom state updates together with the session end payload, so that state persistence and session closure can be accomplished in a single network roundtrip.
11. As a service analyst, I want counseling sessions to reference their preceding malangi touch session ID, so that I can evaluate how sensory grounding before conversation impacts emotional intensity changes.

## Implementation Decisions

- **Domain Model & Architecture:**
  - Create a new `MalangiSession` domain entity representing the lifecycle (`IN_PROGRESS`, `COMPLETED`) with start time, end time, duration seconds, interaction counts (touch, long press, drag), context type (`MAIN`, `AFTER_COUNSELING`, `SLEEP_MODE`), and end reason (`EXIT`, `TO_COUNSELING`, `TO_SLEEP`).
  - Extend the existing `UserMalangi` entity with a JSON/Map state container (`customState`) to persist companion-specific attributes such as `crackedStage`.
  - Maintain hex-architecture boundary: domain logic lives in `malangi/domain/`, exposed through ports in `malangi/application/port/in/` and implemented via `MalangiSessionService`.
  - Update `CounselingSession` domain entity to include an optional reference to `malangiSessionId`.

- **API Contracts:**
  - `POST /api/v1/malangi/sessions/start`: starts a new session with `malangiType` and `contextType`; returns `sessionId`, `status: IN_PROGRESS`, and current `customState`.
  - `POST /api/v1/malangi/sessions/{sessionId}/end`: finishes the session with `durationSeconds`, `touchCount`, `longPressCount`, `dragCount`, `endReason`, and optional `customState`; marks session `COMPLETED` and increments cumulative play time.
  - `PATCH /api/v1/malangis/{malangiType}/state`: independently updates companion state (e.g., crackedStage).
  - `GET /api/v1/malangi/sessions`: returns recent sessions with limit pagination.
  - `POST /api/v1/counseling/sessions`: accepts optional `malangiSessionId` to associate preceding sensory grounding with the counseling session.

- **Persistence & Migration:**
  - Introduce `malangi_sessions` table mapping to the `MalangiSessionEntity`.
  - Add `custom_state` JSON column to `user_malangis` table (defaulting to empty JSON or `{"crackedStage": 0}`).
  - Add `malangi_session_id` nullable foreign key to `counseling_sessions` table.

## Testing Decisions

- **Testing Philosophy & Seam Selection:**
  - A good test exercises observable system behavior from the highest possible boundary, avoiding coupling to private helper methods, specific query structures, or transient internal state.
  - The primary test seam for this feature is the top-level HTTP REST API via `@SpringBootTest` with `@AutoConfigureMockMvc`, mirroring real client calls with JWT authentication and testing against actual persistence (H2 in test, Flyway migration parity with MySQL).
  - Existing prior art: `MalangiApiTests` and `AuthApiTests` demonstrate end-to-end user registration, token generation, and JSON response verification against standard envelopes.
  - Tests will cover:
    1. Starting a session and verifying `sessionId`, `IN_PROGRESS` status, and initial `customState`.
    2. Ending a session with interaction counts and verifying `COMPLETED` status, updated cumulative `totalPlayTime`, and `lastInteractedAt`.
    3. Persisting `customState` changes across sessions and inspecting `GET /api/v1/malangis`.
    4. Transitioning to counseling (`endReason: TO_COUNSELING`) and verifying counseling session links the `malangiSessionId`.
    5. Handling error cases: ending an already completed session, ending someone else's session (forbidden), and invalid input validation.

## Out of Scope

- Real-time WebSocket streaming of individual touch coordinates (client manages touch gestures locally).
- Subjective post-touch questionnaire / relief rating modals (user explicitly chose pure usage logging without survey friction).
- Audio streaming backend (ASMR sounds are bundled as local assets within the frontend client).
- Automatic lock resets for cracked wax ball timers (wax reset is manually triggered or driven by client logic).

## Further Notes

- The design guarantees backward compatibility with the existing inventory selection APIs (`GET /api/v1/malangis`, `PATCH /api/v1/malangis/{malangiType}/select`).
- If a user loses connection or force-closes the app, the session remains `IN_PROGRESS` and client can send the end payload upon reconnect, or background cleanup can periodically handle stale sessions.

## Implementation Progress

- Implemented: custom state persistence, session start/end, interaction counts, cumulative play time, per-user recent session history, authenticated API integration tests (items 01–04).
- Pending integration: item 05 requires the counseling session creation endpoint and `counseling_sessions` table, which are not present in this repository yet. Add the nullable session foreign key and validate ownership when the counseling module is implemented.
