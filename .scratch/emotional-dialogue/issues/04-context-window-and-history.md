# 04. 대화 문맥 구성 및 히스토리 관리

Type: grilling
Status: resolved
Blocked by: 01, 02

## Question

LLM 호출 시 프롬프트에 제공할 대화 문맥(Context)을 어떻게 구성할 것인가? 이전 발화 전체를 보낼 것인가, 슬라이딩 윈도우(예: 최근 6~10턴)와 현재 세션의 구조화된 요약(Situation, Emotion 등)을 결합할 것인가?

## Answer

토큰 낭비와 응답 지연을 방지하고 문맥 집중도를 높이기 위해, 최근 6~8턴의 발화 내역(슬라이딩 윈도우)과 함께 지금까지 백엔드 DB에 누적된 구조화된 슬롯 데이터(`Situation`, `primary_emotion`, `initial_thought` 등)를 프롬프트 상단 시스템 컨텍스트로 결합하여 주입한다.
