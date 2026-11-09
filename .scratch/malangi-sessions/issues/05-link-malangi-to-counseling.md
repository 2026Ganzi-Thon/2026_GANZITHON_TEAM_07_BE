# 05: 말랑이 환기 세션 -> AI 상담 세션 연계 추적

**What to build:** 사용자가 말랑이 만지기 화면에서 '감정 정리하기'(`TO_COUNSELING`)를 통해 상담 대화로 전환할 때, 직전 만지기 세션 ID를 새 상담 세션에 연결하여 "감정 환기 후 상담 연계" 흐름을 온전히 추적하고 기록할 수 있도록 연동합니다.

**Blocked by:** 03: 말랑이 만지기 세션 종료 및 조작 집계 API (POST /end)

**Status:** ready-for-agent

- [ ] `counseling_sessions` 테이블에 `malangi_session_id` nullable 외래키 컬럼이 추가된다.
- [ ] `POST /api/v1/counseling/sessions` 요청 시 `malangiSessionId` 필드가 전달되면 유효한 말랑이 세션인지(존재 여부 및 본인 소유 여부) 검증 후 상담 세션에 저장된다.
- [ ] `malangiSessionId`가 전달되지 않는 일반 상담 시작 요청도 정상적으로 동작한다(선택적 바인딩).
- [ ] 상담 세션 상세 조회 또는 내부 연계 조회 시 연관된 `malangiSessionId`가 확인된다.
- [ ] 존재하지 않거나 다른 사용자의 `malangiSessionId`를 전달한 경우 적절한 클라이언트 오류(400 또는 404)를 반환한다.
