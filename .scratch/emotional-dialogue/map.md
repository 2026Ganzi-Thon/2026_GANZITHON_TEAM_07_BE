# Map: AI 감정 대화 시스템 백엔드 구현 스펙

## Destination

7단계 상담 라이프사이클 상태 머신, Spring AI 기반 LLM 프롬프트 파이프라인, 그리고 2중 위기 감지 가드레일을 백엔드 코드로 즉시 구현할 수 있는 완결된 기술·도메인 구현 스펙을 확정한다.

## Notes

- Domain: 감정 대화 7단계 라이프사이클 (CBT, 소크라테스식 대화, SFBT, NVC), 위기 안전 가드레일
- Skills: `grilling`, `domain-modeling`, `codebase-design`
- Architecture: Spring Boot 4 헥사고날 아키텍처 (`counseling` 도메인 패키지 대상)
- Tracker: Local Markdown tracker under `.scratch/emotional-dialogue/`

## Decisions so far

- [01-dialogue-turn-control](issues/01-dialogue-turn-control.md): 백엔드가 `currentStep` 상태 머신을 엄격히 관리하며 단계별 전용 시스템 프롬프트를 주입하여 제어함.
- [02-llm-integration-stack](issues/02-llm-integration-stack.md): Spring AI 스타터를 채택하고 환경변수(`LLM_API_KEY`) 기반으로 고속·저비용 모델(GPT-4o-mini 등) 연동.
- [03-safety-first-line-filter](issues/03-safety-first-line-filter.md): 외부 AI 호출 전 서버 내부 인메모리 정규식/키워드 필터로 1차 차단하고 일상 관용구 예외 처리.
- [04-context-window-and-history](issues/04-context-window-and-history.md): 최근 6~8턴 슬라이딩 윈도우와 DB 구조화 슬롯 요약을 결합한 효율적 프롬프트 문맥 구성.
- [05-structured-output-and-step-transition](issues/05-structured-output-and-step-transition.md): Spring AI 정형 출력(JSON)으로 사용자 발화, 추출 데이터, 전이 신호를 수신해 백엔드가 검증 및 단계 전환.
- [06-malangi-recommendation-logic](issues/06-malangi-recommendation-logic.md): 대표 감정 ➔ 캐릭터 매핑은 백엔드 도메인 룰로 확정하고 맞춤 사유/요약은 LLM이 생성하는 하이브리드 추천.
- [07-llm-failure-and-fallback](issues/07-llm-failure-and-fallback.md): 타임아웃 5초, 1회 재시도, 최종 실패 시 현재 단계에 맞춘 온화한 기본 폴백 메시지 반환.

## Not yet specified

<!-- 모든 핵심 기술·도메인 결정이 확정되어 안개(Fog)가 모두 걷힘. 즉시 코드 구현에 착수할 수 있는 상태에 도달함. -->
- 없음 (구현 스펙 확정 완료)

## Out of scope

- 멀티 모달 음성(STT/TTS) 실시간 스트리밍 처리 (MVP 이후 과제)
- 사용자 맞춤형 파인튜닝(Fine-tuning) 모델 구축 (프롬프트 엔지니어링으로 대체)
- 백엔드 외 FE 3D 말랑이 셰이더 및 ASMR 렌더링 엔진 구현
