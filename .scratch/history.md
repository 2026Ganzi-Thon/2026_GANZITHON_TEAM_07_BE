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

- 로컬 MySQL 8.4 Compose 기동 확인 (JDBC 연결은 후속 CI에서 통과): [기반 작업 이슈](backend-bootstrap/issues/01-bootstrap.md)
- LLM 제공자·모델·비용·연동 방식, 질문 전략과 대화 정책 결정: [서비스 스펙](service/spec.md)
- 업무 API·인증·저장 모델·MVP 범위 결정과 기능 구현
- 팀 main 보호 설정은 관리자 작업 필요: [공유 저장소 전환](shared-repository-workflow/spec.md)

보존한 원본 환경 파일이나 비밀값은 기록에 복사하지 않습니다. 일회성 `/tmp` 파일과 빌드 결과는 재생성 가능한 자료이며 Git 이력에 포함하지 않습니다.

## 추가 작업: 보존된 ignore 규칙 반영

사용자가 이전 README worktree에 남아 있던 Windows·macOS·Java ignore 규칙의 확인과 커밋을 요청했습니다. 현재 브랜치의 환경변수·IDE·개발 산출물 규칙과 합치고, `*.jar`에 `gradle/wrapper/gradle-wrapper.jar` 예외를 두었습니다. `.scratch` 작업 기록 추적은 유지합니다. 원본 파일은 기존 로컬 백업 폴더에 보존하며, 반영 후 이전 worktree의 미커밋 변경은 정리합니다.

Git ignore 판정으로 OS 파일·Java 아카이브 등 27개 제외 대상과 Wrapper·`.scratch` 기록 등 6개 추적 대상이 의도대로 처리되는 것을 확인했습니다. macOS CR 문자 패턴과 `git diff --check`도 통과했습니다. 애플리케이션 코드는 변경하지 않아 빌드 테스트는 재실행하지 않습니다.

## 추가 작업: 공유 팀 저장소 방식 전환

사용자가 fork 없이 팀 저장소의 작업 브랜치와 PR로 개발하도록 전체 협업 구성을 바꾸도록 요청했습니다. 최신 팀 main과 로컬 main은 `6d4e354`로 일치했고, 이 커밋에 PR #4 merge가 포함된 것을 확인했습니다. 이전 OPEN 기록은 당시 상태이며 현재 PR #4 구현은 팀 기준에 반영된 상태입니다.

팀 저장소를 origin으로 변경하고 같은 주소의 중복 upstream은 제거했습니다. main은 origin/main을 추적하며 push 기본값은 origin/simple입니다. 기존 커밋·브랜치·stash는 보존했습니다. 기본 폴더의 새 작업 브랜치 `chore/shared-repository-workflow`에서 훅·문서·CI를 변경하며 main 직접 push나 PR merge는 수행하지 않습니다.

훅은 팀 작업 브랜치만 허용하는 규칙으로 바꾸고 실제 Git 환경에서 회귀 검사를 추가했습니다. CI는 Java 21·MySQL 8.4와 훅 검사를 실행합니다. 현재 계정은 WRITE 권한이며 main 서버 보호 설정은 꺼져 있어 관리자 설정 절차를 별도로 문서화했습니다. 검증과 최종 결과는 [전환 검증 기록](shared-repository-workflow/verification.md)에 남깁니다.

공유 저장소 작업 브랜치 push와 PR #5 생성이 성공했고, 구현 커밋의 GitHub Actions에서 훅 9개·Java 21·MySQL 8.4 검증을 통과했습니다. 원격 main은 변경하지 않았습니다.
