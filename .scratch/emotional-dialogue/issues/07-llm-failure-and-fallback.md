# 07. LLM 호출 실패, 타임아웃 및 폴백(Fallback) 안전망

Type: grilling
Status: resolved
Blocked by: 02

## Question

외부 AI 모델 API가 타임아웃(지연)되거나 오류(500, Rate Limit 등)를 반환할 때, 사용자 대화 흐름이 끊기지 않도록 백엔드에서 어떤 폴백 메시지 및 재시도 정책을 가동할 것인가?

## Answer

타임아웃은 5초로 설정하고 일시적 오류 시 지수 백오프 기반으로 1회 즉시 재시도한다. 최종 실패 시 서버 500 에러를 사용자에게 노출하지 않고, 현재 세션의 `currentStep`에 맞추어 미리 준비된 온화한 시스템 기본 안내 문장(Fallback Message)을 제공하여 대화가 매끄럽게 유지되도록 방어한다.
