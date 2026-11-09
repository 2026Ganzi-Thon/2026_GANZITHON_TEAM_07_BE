# 04: 최근 말랑이 세션 이력 조회 API (GET /sessions)

**What to build:** 사용자가 이전에 수행했던 말랑이 만지기 활동 이력과 조작 통계를 시계열로 조회할 수 있도록 최근 세션 목록 조회 엔드포인트를 제공합니다.

**Blocked by:** 03: 말랑이 만지기 세션 종료 및 조작 집계 API (POST /end)

**Status:** ready-for-agent

- [ ] `GET /api/v1/malangi/sessions` 호출 시 현재 로그인한 사용자의 완료/진행 세션 목록이 최신순(`started_at DESC`)으로 반환된다.
- [ ] `limit` 쿼리 파라미터(기본값 10)를 지원하여 조회 개수를 제어할 수 있다.
- [ ] 응답 객체에 `sessionId`, `malangiType`, `status`, `contextType`, `startedAt`, `endedAt`, `durationSeconds`, `totalInteractionCount`, `endReason`이 포함된다.
- [ ] 다른 사용자의 세션 기록은 노출되지 않는다.
- [ ] 인증되지 않은 요청은 401 Unauthorized로 거부된다.
