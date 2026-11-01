# 공유 저장소 전환 검증 기록

기록일: 2026-11-01.

## 로컬에서 확인한 항목

- main과 팀 main은 `6d4e354`로 일치하며 전환 전 미커밋 변경이 없었습니다.
- origin URL은 팀 저장소이고 중복 upstream은 제거했습니다.
- main 추적은 origin/main, remote.pushDefault는 origin, push.default는 simple입니다.
- `sh -n .githooks/pre-push`: 통과.
- `python3 scripts/test_pre_push.py`: 실제 임시 Git 저장소에서 테스트 9개 통과. HTTPS·SSH 작업 브랜치 허용, 다른 저장소·다른 원격·여러 push URL 차단, main 소스·목적지 차단, 태그·삭제·detached HEAD ref 차단, 여러 ref 중 하나가 main인 push 차단을 확인했습니다.
- 앱 소스나 의존성은 변경하지 않았으며, 새 GitHub Actions CI가 Java 21·MySQL 8.4 전체 테스트를 담당합니다.

## 서버 설정의 범위

2026-11-01 조회한 사용자의 권한은 WRITE, main protected는 false입니다. 관리자 권한이 필요한 branch protection은 변경하지 않았습니다. 개인 Codex의 PR merge 차단도 유지했습니다.

## 팀 저장소 게시 및 CI 결과

- 팀 origin의 `chore/shared-repository-workflow` push가 새 훅을 정상 통과했습니다. 현재 추적은 `origin/chore/shared-repository-workflow`입니다.
- 같은 팀 저장소의 [PR #5](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/5)를 생성했습니다. main 직접 push나 PR merge는 하지 않았습니다.
- [CI 실행 41316312030](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/actions/runs/41316312030)은 구현 커밋 `b26e278`에서 SUCCESS입니다.
- Temurin Java 21.0.13, MySQL 8.4.13 서비스에서 `RUN_MYSQL_TESTS=true ./gradlew --no-daemon clean test bootJar`가 성공했습니다.
- 실제 runner에서 훅 테스트 9개 통과, `BUILD SUCCESSFUL in 42s`를 확인했습니다.
- 문서 로컬 링크, CI YAML 구문, `git diff --check`도 통과했습니다.

로컬 Compose 자체의 실행은 이 CI 서비스 실행과 다르므로 아직 미검증입니다. main 서버 보호는 [관리자 작업](issues/01-protect-main.md)으로 남깁니다.
