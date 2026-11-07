# 02. LLM 연동 기술 스택 및 모델 선정

Type: grilling
Status: resolved

## Question

백엔드에서 LLM을 연동할 때 어떤 기술 스택과 모델을 채택하고, 인증/비용 관리를 어떻게 구성할 것인가?

## Answer

Spring Boot 4 생태계와의 자연스러운 통합 및 향후 모델 교체 유연성을 위해 Spring AI 스타터를 채택한다. 가성비, 한국어 발화 품질 및 응답 속도를 고려하여 1차 타겟 모델을 GPT-4o-mini(또는 Claude 3.5 Haiku)로 설정하고, API Key는 환경변수(`LLM_API_KEY`)를 통해 안전하게 주입받도록 구성한다.
