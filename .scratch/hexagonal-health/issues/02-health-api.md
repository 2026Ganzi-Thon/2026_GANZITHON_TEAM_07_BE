# 02: 최초 health API 제공

**What to build:** 기동된 백엔드의 `GET /api/v1/health`가 인증 없이 HTTP 200·application/json·`{"status":"UP"}`을 반환하도록 입력 포트부터 HTTP 응답까지 연결합니다.

**Blocked by:** 01: 기능별 헥사고날 기반 정의. 기반 커밋으로 구현하고 main에는 01 PR 이후 반영합니다.

Status: ready-for-human

- [x] 요청 처리에 DB·LLM 조회 없이 health를 반환한다. 기존 DB 기동 조건은 유지한다.
- [x] 버전 경로를 사용하고 POST는 405, /health는 404를 반환한다.
- [x] Swagger가 health를 문서화하며 문서 비활성화 시에도 응답한다.
- [ ] HTTP·조립·의존 방향 검사와 문서·CI를 확인한다.
- [ ] API 전용 브랜치에 커밋·push하고 선행 PR을 명시한다.

## Comments

로컬 구현과 전체 검사 완료. 실제 MySQL 및 최종 CI는 PR에서 확인합니다.
