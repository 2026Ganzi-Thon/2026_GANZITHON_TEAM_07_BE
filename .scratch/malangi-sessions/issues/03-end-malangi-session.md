# 03: 말랑이 만지기 세션 종료 및 조작 집계 API (POST /end)

**What to build:** 사용자가 만지기 화면에서 '사용 종료하기', '감정 정리하기', 또는 '수면 모드'를 눌러 이탈할 때, 클라이언트가 집계한 총 체류 시간, 조작 카운트(터치, 롱프레스, 드래그), 종료 사유(`EXIT`, `TO_COUNSELING`, `TO_SLEEP`) 및 최신 말랑이 상태를 일괄 전송하여 세션을 `COMPLETED`로 닫고 인벤토리 통계를 갱신합니다.

**Blocked by:** 02: 말랑이 만지기 세션 시작 API (POST /start)

**Status:** ready-for-agent

- [ ] `POST /api/v1/malangi/sessions/{sessionId}/end` 호출 시 전달된 `durationSeconds`, `touchCount`, `longPressCount`, `dragCount`, `endReason`이 해당 세션에 반영되고 `status`가 `COMPLETED`로 전환된다.
- [ ] 요청 바디에 `customState`가 포함된 경우 해당 사용자의 `user_malangis.custom_state`도 동일 트랜잭션 내에서 동기화된다.
- [ ] 해당 말랑이의 누적 플레이 시간(`total_play_time`)이 `durationSeconds`만큼 증가하고 `last_interacted_at`이 갱신된다.
- [ ] 본인 소유가 아닌 다른 사용자의 세션을 종료하려고 시도할 경우 403 Forbidden 또는 404가 반환된다.
- [ ] 이미 `COMPLETED` 상태로 닫힌 세션에 대해 중복 종료를 시도할 경우 적절한 클라이언트 오류(409 또는 400)가 반환된다.
- [ ] 응답으로 마감된 세션 통계(`totalInteractionCount`, `endReason`, `totalPlayTime` 등)가 정상 반환된다.
