# 01: 기능별 헥사고날 기반 정의

**What to build:** 새 기능이 HTTP·DB 기술과 분리된 업무 코드로 구성되도록 단일 모듈의 의존 규칙과 자동 검증을 제공합니다. 이 티켓은 신규 기반 설정이며 health API는 포함하지 않습니다.

**Blocked by:** None (can start immediately).

Status: ready-for-agent

- [x] 기능별 domain/application/adapter와 외부 bean 조립 규칙을 문서화한다.
- [x] 의존 규칙과 잘못된 의존을 잡는 fixture 검사를 제공한다.
- [x] health API를 별도 후속 티켓으로 둔다.
- [x] 의존 PR에도 CI를 실행하도록 구성한다.
- [ ] 리뷰 후 전용 브랜치에 커밋·push하고 PR을 생성한다.
