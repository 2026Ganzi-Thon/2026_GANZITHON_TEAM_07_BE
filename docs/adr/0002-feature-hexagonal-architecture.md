# 단일 모듈에서 기능별 헥사고날 구조 채택

현재 실행 기반만 있는 백엔드에 HTTP·DB·LLM 기술이 업무 로직으로 섞이지 않도록, 단일 Gradle 모듈에서 기능별 `domain`, `application`, `adapter` 패키지를 사용합니다. 초기 팀 개발의 빌드 복잡도를 줄이기 위해 멀티모듈 분리는 미루고, application·domain의 순수 Java 의존 방향은 아키텍처 테스트로 검증합니다.

별도 후속 health 기능은 `adapter.in.web → application.port.in ← application.service`로 연결하며 Spring bean은 바깥 `config`에서 조립합니다. health에는 업무 도메인 규칙이나 외부 연동이 없으므로 도메인 모델과 출력 포트는 만들지 않습니다. 저장·LLM 연동이 생기면 해당 기능의 `application.port.out`과 `adapter.out`을 추가합니다.
