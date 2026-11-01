# 분리 구현 검증

2026-11-02. 헥사고날 기반과 health API는 별도 브랜치·커밋으로 관리합니다.

- 기반 `refactor/hexagonal-foundation`: 아키텍처 규칙 4개 통과. API 없음. 잘못된 의존을 가진 fixture를 실제 공통 규칙이 탐지함을 확인했습니다.
- API `feat/health-api`: Java 21 `clean test bootJar` 성공. 15개 중 14개 통과, 실제 MySQL 검사 1개는 로컬에서 비활성화되어 스킵했습니다.
- HTTP 계약, 버전 접두사, POST 거부, DataSource 없는 MVC 처리, 앱 조립, Swagger 문서화·비활성화, 의존 방향을 확인했습니다.
- 기존 구현을 재사용했으며 최초 구현이 TDD였다고 주장하지 않습니다. 경로를 임시로 바꿨을 때 기존 HTTP 검사가 예상 200/실제 404 실패를 잡는 것을 확인한 뒤 원본을 복원했습니다.
- API PR은 기반 브랜치를 대상으로 만들어 차이를 분리하고, 기반 PR merge 후 main으로 재지정합니다. main 직접 push나 PR merge는 수행하지 않습니다.
- 코드 리뷰와 서버 CI 결과는 각 PR에서 확인합니다.
