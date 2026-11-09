# 02: 말랑이 만지기 세션 시작 API (POST /start)

**What to build:** 사용자가 메인 화면에서 대표 말랑이를 터치하거나 퀵 메뉴를 통해 만지기 화면으로 진입할 때 새 세션을 생성하고 `IN_PROGRESS` 상태의 고유 세션 ID를 발급합니다. 클라이언트는 이 세션 ID를 바탕으로 조작을 집계하고 종료 시점에 닫을 수 있습니다.

**Blocked by:** 01: 말랑이 고유 상태(customState) 영속화 및 갱신 API

**Status:** ready-for-agent

- [ ] `malangi_sessions` 테이블이 생성되어 세션 ID, 사용자 ID, 말랑이 종류, 상태(`IN_PROGRESS`, `COMPLETED`), 진입 경로(`MAIN`, `AFTER_COUNSELING`, `SLEEP_MODE`), 시작 일시, 종료 일시, 조작 횟수, 종료 사유 컬럼이 마련된다.
- [ ] `POST /api/v1/malangi/sessions/start` 호출 시 요청받은 말랑이 종류와 진입 경로로 `IN_PROGRESS` 세션이 생성된다.
- [ ] 응답으로 발급된 `sessionId`, `status`, `startedAt`, 그리고 현재 말랑이의 `customState`가 반환된다.
- [ ] 잠긴 말랑이에 대해 세션 시작을 요청할 경우 409 LOCKED 또는 유효성 오류(400)가 반환된다.
- [ ] 인증되지 않은 요청은 401 Unauthorized로 거부된다.
