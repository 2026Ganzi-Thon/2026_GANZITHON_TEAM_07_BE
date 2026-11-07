# 05. 정형 출력(Structured Output) 및 단계 전이 판정

Type: grilling
Status: resolved
Blocked by: 01, 04

## Question

LLM이 사용자에게 보여줄 발화 메시지 외에, 현재 단계의 데이터 추출 결과(`extractedData`)와 다음 단계 전이 가능 여부(`transitionReady`)를 백엔드가 어떻게 파싱하고 검증할 것인가?

## Answer

Spring AI의 Structured Output (JSON Schema 모드)을 활용하여 AI로부터 사용자용 발화 문장(`userMessage`), 현재 단계에서 추출된 속성 데이터(`extractedData`), 단계 완료 및 전이 추천 플래그(`transitionReady`)를 구조화된 JSON으로 수신한다. 백엔드는 이 결과를 검증한 뒤 DB에 저장하고 세션의 `currentStep`을 안전하게 전환한다.
