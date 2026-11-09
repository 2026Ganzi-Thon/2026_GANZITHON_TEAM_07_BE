# 01: 말랑이 고유 상태(customState) 영속화 및 갱신 API

**What to build:** 사용자가 말랑이(특히 왁뿌볼)를 만지면서 변화시킨 고유 상태(예: 왁스 깨짐 단계 `crackedStage`)를 서버에 영속 저장하여 앱을 종료하거나 재시작해도 이전 조작 상태가 유지되도록 합니다. 보유 말랑이 목록 조회 시 현재 상태를 반환하고, 독립된 상태 갱신 API를 제공합니다.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `user_malangis` 테이블에 `custom_state` JSON 컬럼이 추가되고 기존/신규 행에 기본값(빈 JSON 또는 `{"crackedStage": 0}`)이 설정된다.
- [ ] `GET /api/v1/malangis` 호출 시 각 말랑이 항목에 `customState` 객체가 포함되어 반환된다.
- [ ] `PATCH /api/v1/malangis/{malangiType}/state` 호출 시 유효한 JSON 상태가 저장되고 갱신된 상태와 수정 일시가 응답된다.
- [ ] 잠겨있거나 보유하지 않은 말랑이의 상태를 수정하려고 할 경우 적절한 예외(404 또는 409)가 발생한다.
- [ ] 인증되지 않은 요청은 401 Unauthorized로 거부된다.
