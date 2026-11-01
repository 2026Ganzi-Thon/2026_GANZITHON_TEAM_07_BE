# 작업 및 결정 이력

기록일: 2026-11-01. 대화 내용, 커밋, PR 상태 및 실제 검증 결과에 근거합니다.

## 저장소에 먼저 반영된 작업

아래 PR은 2026-11-01 GitHub 조회에서 MERGED를 확인했습니다. 이 표는 저장소 이력이며 이번 대화에서 agent가 merge했다는 뜻은 아닙니다.

| PR | 반영 내용 |
| --- | --- |
| [#1](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/1) | AI 협업 워크플로, 커밋 및 PR 템플릿 |
| [#2](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/2) | 서비스 기획·기능 요구사항·README·문서 기반 개발 지침 |
| [#3](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/3) | 개발 산출물 제외, VS Code worktree 탐지와 fork 동기화 설정 |

## 이번 대화에서 진행한 작업

1. 사용자 후보인 Spring Boot 3.x·Java 21·Gradle·MySQL·Swagger를 검토했습니다. 지원 종료를 확인한 뒤 사용자가 Boot 4.x 추천안을 선택했습니다.
2. 사용자가 서비스의 핵심을 감정 표현에 기반한 연속 후속 질문으로 설명했습니다. LLM 제공자·모델·키는 비워 달라는 요청을 유지했습니다.
3. 백엔드 실행 기반, DB·Swagger 설정, 테스트, 문서 및 ADR·용어집을 추가했습니다. 구현 커밋은 `30df747`입니다.
4. 개인 Codex의 `gh` 명령을 영구 허용했습니다. 이후 사용자 요청으로 `gh pr merge`는 금지로 변경했습니다. 실제 merge를 수행하지 않았습니다.
5. Java 21 빌드, 테스트 6개, MySQL 9.4의 임시 DB 연결, 실행 JAR의 Swagger/OpenAPI HTTP 200을 확인했습니다. Docker가 없어 MySQL 8.4 Compose 검증은 남겼습니다.
6. worktree 필수 방식에서 기본 저장소 폴더의 브랜치 작업 + `.scratch` Markdown 기록 + 선택적 worktree로 변경했습니다. 해당 커밋은 `1013b95`입니다.
7. 기본 폴더의 기존 README 공백과 VS Code 설정을 stash와 로컬 백업으로 보존하고 백엔드 브랜치를 기본 폴더로 옮겼습니다. 테스트 자료와 JAR도 기본 폴더로 옮기고 해당 임시 worktree를 제거했습니다.
8. 개인 fork의 `build/backend-bootstrap`에 push하고 팀 [PR #4](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/4)를 생성했습니다. 2026-11-01 조회 시 OPEN입니다.
9. VS Code checkout 오류를 확인했습니다. 같은 브랜치는 여러 worktree에서 동시에 checkout할 수 없으며, 작업 종료나 편집기 종료만으로 점유가 해제되지 않는 점을 설명했습니다.
10. 기존 worktree들의 브랜치 점유를 해제했습니다. README worktree의 미커밋 `.gitignore` 변경은 그대로 보존했습니다. 기본 폴더에서 README 브랜치 전환이 되는 것을 확인했습니다.
11. 사용자가 지금까지의 작업과 spec을 `.scratch`에 정리해 달라고 요청했습니다. 이어서 협업 이력 추적을 위해 `.scratch`를 Git 추적 대상으로 바꾸도록 요청하여 기록과 지침을 갱신했습니다.

## 남아 있는 작업

- MySQL 8.4 Compose 기동 및 JDBC 연결 검증: [기반 작업 이슈](backend-bootstrap/issues/01-bootstrap.md)
- LLM 제공자·모델·비용·연동 방식, 질문 전략과 대화 정책 결정: [서비스 스펙](service/spec.md)
- 업무 API·인증·저장 모델·MVP 범위 결정과 기능 구현
- 기존 README worktree에 보존한 `.gitignore` 수정의 처리 여부 결정
- PR #4 리뷰 및 팀 절차에 따른 merge: agent는 merge하지 않음

보존한 원본 환경 파일이나 비밀값은 기록에 복사하지 않습니다. 일회성 `/tmp` 파일과 빌드 결과는 재생성 가능한 자료이며 Git 이력에 포함하지 않습니다.
