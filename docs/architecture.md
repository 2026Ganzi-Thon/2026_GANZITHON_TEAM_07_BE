# 시스템 아키텍처

[README로 돌아가기](../README.md)

단일 Gradle 모듈에서 기능별 헥사고날 구조를 사용합니다. 선택 이유는
[ADR-0002](adr/0002-feature-hexagonal-architecture.md)에 기록합니다.

## 기능별 패키지 규칙

각 기능 안에서 필요한 `domain`, `application.port.in`, `application.service`,
`application.port.out`, `adapter.in`, `adapter.out`을 구성합니다.

- domain: 업무 규칙. application과 외부 기술에 의존하지 않습니다.
- application: 유스케이스와 입출력 포트. 외부 어댑터·Spring·JPA·직접 JDBC에 의존하지 않습니다.
- adapter.in: 입력 변환. 서비스 구현체 대신 입력 포트를 호출합니다.
- adapter.out: DB·LLM 등 외부 연동으로 출력 포트를 구현합니다.
- config: Spring bean과 포트 구현을 바깥에서 조립합니다.

업무 규칙·외부 호출이 생길 때 필요한 패키지와 타입을 추가합니다. 아직 없는
기능을 위한 가상 도메인 객체나 출력 포트는 만들지 않습니다. 현재 업무 API는
없으며 health API는 별도 후속 작업입니다.

## 검증

ArchUnit으로 안쪽 코드의 외부 기술 의존과 입력 어댑터의 서비스 직접 의존을
검사합니다. 기능을 추가하기 전에는 빈 패키지를 허용하며 이후 자동 적용됩니다.
외부 기술에 의존하는 테스트 fixture를 탐지하는 검사도 제공합니다.

현재 Spring MVC·JPA/MySQL 8.4·Swagger 연결 설정을 유지합니다. LLM, 엔티티,
인증 및 배포는 미정입니다. 실행은 [실행 가이드](getting-started.md), API는
[API 명세](api.md)를 참고합니다.
