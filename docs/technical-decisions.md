# 기술적 의사결정 및 트러블슈팅

[README로 돌아가기](../README.md)

## 채택한 초기 구성 · 2026-11-01

- Spring Boot 4.1.2 + Java 21: 신규 프로젝트에서 지원 중인 프레임워크를 사용합니다. 초기 후보 3.x에서 변경한 배경은 [ADR-0001](adr/0001-spring-boot-4.md)에 기록했습니다.
- Gradle 9.8.2 Wrapper: 설치 없이 동일한 빌드 도구를 사용하며 공식 SHA-256으로 배포 파일을 검증합니다. Java 21 toolchain을 요구합니다.
- MySQL 8.4 LTS + Spring Data JPA: 사용자가 제안한 MySQL을 채택하고 로컬 Compose를 제공합니다. 엔티티와 스키마는 기능 구현 시 설계합니다.
- springdoc 3.2.0: [공식 호환성 표](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)의 Boot 4.1.x 조합을 사용합니다.
- LLM: 사용자가 감정을 표현하면 내면의 감정을 탐색할 수 있도록 후속 질문을 이어가는 역할입니다. 공통 ChatClient API 기반은 설치했으며 제공자·모델과 질문 전략은 미정이고 실제 호출은 없습니다.

## 개발 라이브러리 기반 · 2026-11-07

- Spring Boot DevTools는 `developmentOnly`로만 포함합니다. 컴파일된 클래스패스가 바뀌면 개발 중 재시작을 지원하고, 패키징 앱에는 포함하지 않습니다.
- Lombok은 `compileOnly`와 `annotationProcessor`로 설정했습니다. 기존 코드를 일괄 변환하지 않았습니다.
- Flyway는 Boot starter와 MySQL 모듈을 추가했습니다. Hibernate `ddl-auto=validate`를 유지하며 업무 DDL은 아직 없습니다.
- Spring Security와 OAuth2 Resource Server/JWT 지원을 설치했습니다. 인증 정책이 구현되기 전까지는 기존 공개 API 동작을 지키도록 모든 경로를 허용하는 최소 체인을 사용합니다. 토큰 발급·검증 및 계정 기능은 미구현입니다.
- Spring AI 2.0.2 BOM과 `spring-ai-client-chat` 공통 API를 추가했습니다. Spring AI 2.0.x의 Boot 4.1 지원을 확인했습니다. 모델 provider 선택 전이므로 provider starter, API 키, 모델 호출은 포함하지 않습니다.
- Testcontainers JUnit Jupiter, MySQL, Spring Boot service connection 테스트 모듈을 추가하고 MySQL 8.4 기동·연결 및 Flyway 무마이그레이션 상태를 확인하는 smoke test를 둡니다. Docker가 없는 환경에서는 테스트를 건너뜁니다.
- Actuator는 기본 health endpoint만 노출합니다. 상세 health 정보와 전체 endpoint는 공개하지 않습니다.

## F-00 인증 구현 · 2026-11-08

- 인증은 기능별 헥사고날 구조로 구현했습니다. OAuth 교환, JPA 저장, 비밀번호 해시, 토큰 발급·검증은 출력 어댑터에 두고 유스케이스는 포트에 의존합니다.
- Access Token(24시간)과 Refresh Token(14일)은 HS256 JWT입니다. `AUTH_JWT_SECRET`은 Base64 디코딩 후 32바이트 이상이어야 합니다. Access/Refresh 토큰에 서로 다른 `tokenType` 검증기를 두어 토큰 혼용을 막습니다.
- Refresh Token 원문은 저장하지 않고 SHA-256 해시를 저장합니다. 갱신·로그아웃은 DB 행 잠금으로 동시 회전을 직렬화합니다.
- 소셜 로그인은 백엔드에서 authorization code를 공급자 토큰으로 교환하고 검증된 이메일을 조회합니다. redirect URI는 서버 설정값과 완전히 일치해야 합니다.
- 동일 이메일의 계정 자동 병합은 하지 않으며 HTTP 409로 기존 공급자 로그인을 안내합니다. 실제 공급자 앱 등록 및 end-to-end OAuth 검증은 각 환경의 자격 증명이 준비된 뒤 수행해야 합니다.
- F-00의 핵심 경로는 H2 기반 API 테스트와 OAuth HTTP 모킹 테스트로 검증합니다. MySQL/Testcontainers 결과는 해당 검증 실행 후 기록합니다.

근거: [Spring AI 시작하기](https://docs.spring.io/spring-ai/reference/getting-started.html), [Spring Boot DevTools](https://docs.spring.io/spring-boot/reference/using/devtools.html), [Flyway 설정](https://docs.spring.io/spring-boot/how-to/data-initialization.html), [JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html), [Actuator endpoint](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html).

근거: [Spring Boot 요구사항](https://docs.spring.io/spring-boot/system-requirements.html), [Gradle Java 호환성](https://docs.gradle.org/current/userguide/compatibility.html), [MySQL LTS 안내](https://dev.mysql.com/doc/refman/8.4/en/mysql-releases.html).

## 기능별 헥사고날 구조 · 2026-11-02

단일 Gradle 모듈에서 기능별 domain/application/adapter를 사용합니다.
[ADR-0002](adr/0002-feature-hexagonal-architecture.md)에 의존 방향을 기록하며,
ArchUnit 1.5.1 core를 테스트 전용으로 사용해 규칙을 검사합니다.
API 구현은 별도 티켓과 브랜치로 진행합니다.

## 기술적 의사결정 작성 양식

- **제목 / 날짜:**
- **상태:** 제안 / 채택 / 대체됨
- **배경과 요구사항:**
- **검토한 대안:** 각 대안의 장점·제약
- **결정과 이유:** 선택한 방식이 요구사항에 맞는 근거
- **영향과 한계:** 운영 비용, 복잡도, 남은 제약
- **검증 자료:** 코드, 실험, 문서, 이슈 / PR 링크

장기적으로 유지할 아키텍처 결정은 도메인 문서 규칙에 따라 `docs/adr/`에 기록하고 여기에서 해당 문서를 링크합니다.

## 트러블슈팅 작성 양식

- **제목 / 발생 환경:**
- **문제 상황:** 증상, 발생 조건, 영향 범위
- **재현 방법:** 입력, 실행 절차, 관찰한 결과
- **원인 분석:** 로그·쿼리·측정값 등 확인한 근거
- **해결 방법:** 변경 내용과 선택 이유
- **검증 결과:** 같은 조건에서 수행한 변경 전후 비교
- **재발 방지 / 남은 작업:**
- **관련 이슈 / PR:**

성능 개선 수치는 측정 환경, 데이터 규모, 요청 조건과 함께 기록합니다.
