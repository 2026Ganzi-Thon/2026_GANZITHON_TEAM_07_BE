# 기술적 의사결정 및 트러블슈팅

[README로 돌아가기](../README.md)

## 채택한 초기 구성 · 2026-11-01

- Spring Boot 4.1.2 + Java 21: 신규 프로젝트에서 지원 중인 프레임워크를 사용합니다. 초기 후보 3.x에서 변경한 배경은 [ADR-0001](adr/0001-spring-boot-4.md)에 기록했습니다.
- Gradle 9.8.2 Wrapper: 설치 없이 동일한 빌드 도구를 사용하며 공식 SHA-256으로 배포 파일을 검증합니다. Java 21 toolchain을 요구합니다.
- MySQL 8.4 LTS + Spring Data JPA: 사용자가 제안한 MySQL을 채택하고 로컬 Compose를 제공합니다. 엔티티와 스키마는 기능 구현 시 설계합니다.
- springdoc 3.2.0: [공식 호환성 표](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)의 Boot 4.1.x 조합을 사용합니다.
- LLM: 사용자가 감정을 표현하면 내면의 감정을 탐색할 수 있도록 후속 질문을 이어가는 역할입니다. 제공자·모델·SDK·질문 전략은 미정이며 이번에는 빈 설정만 둡니다.

근거: [Spring Boot 요구사항](https://docs.spring.io/spring-boot/system-requirements.html), [Gradle Java 호환성](https://docs.gradle.org/current/userguide/compatibility.html), [MySQL LTS 안내](https://dev.mysql.com/doc/refman/8.4/en/mysql-releases.html).

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
