# 헥사고날 기반과 health API 스펙

기록일: 2026-11-02. F-01~F-10 구현 전 기반 작업.

## 확정한 결정

사용자가 grilling의 세 추천안을 모두 승인했습니다.

1. 단일 Gradle 모듈, 기능별 헥사고날 패키지. domain·application·adapter의 의존 방향을 지키고 DB·LLM 어댑터는 필요할 때 추가합니다.
2. `GET /api/v1/health`, 인증 없음, HTTP 200과 `application/json`의 `{"status":"UP"}` 응답. 앱 API는 `/api/v1` 접두사를 사용하며 Swagger와 OpenAPI 경로는 유지합니다.
3. 기동된 서버의 HTTP 응답 확인만 수행합니다. 요청 시 DB·LLM 상태를 조회하지 않습니다. JPA 기본 설정상 앱 기동에는 여전히 DB가 필요합니다.

## 구현 범위

- health 입력 포트·순수 Java 서비스·HTTP 어댑터와 외부 Spring bean 조립
- 업무 도메인 규칙이 없는 health에는 가상 도메인 모델이나 출력 포트를 만들지 않음
- 응답 계약·버전 경로·Swagger 노출 및 의존 방향 검증
- 아키텍처·API·실행 안내·ADR·작업 기록 갱신

전체 업무 API, 공통 응답 포맷, 인증, DB readiness, LLM, 배포 설계는 포함하지 않습니다. `/api/v1`은 컨트롤러 매핑이며 servlet context path를 전역 변경하지 않습니다.

## 완료 조건

- GET `/api/v1/health`가 자격증명 없이 합의한 JSON을 반환
- prefix 없는 `/health`는 노출하지 않고 POST health는 허용하지 않음
- 요청 처리에 DataSource 없이 동작하는 HTTP slice 검사
- Swagger가 health를 문서화하며 문서 비활성화 시에도 health는 유지
- application·domain은 외부 어댑터와 프레임워크에 의존하지 않음
- 팀 작업 브랜치 push, PR 생성 및 CI 확인. main merge는 하지 않음

## 검증 경계와 진행 순서

사용자가 확정한 응답 계약을 HTTP 경계에서 검사하고, 실제 Spring 조립으로 입력 포트 구현을 함께 실행합니다. 구조 자체의 계약은 ArchUnit 검사로 확인합니다. 추가 구현 전에 티켓을 정리하며, 현재 남아 있는 기존 미커밋 구현을 재사용합니다. 이미 작성한 코드를 테스트 먼저 작성한 결과로 표현하지 않습니다.

리뷰 기준은 이번 작업 브랜치의 분기점 `1c37140`이고 스펙은 이 문서와 티켓 01입니다.

## 승인된 티켓·브랜치 분리

사용자가 헥사고날 작업과 API 작업을 별도 브랜치로 나누고 커밋·push하도록 지시했습니다. 구조 기반은 `refactor/hexagonal-foundation`, health API는 그 커밋을 기반으로 한 `feat/health-api`에서 구현합니다. 구조 PR을 먼저 반영하고 API PR을 main으로 재지정합니다.
