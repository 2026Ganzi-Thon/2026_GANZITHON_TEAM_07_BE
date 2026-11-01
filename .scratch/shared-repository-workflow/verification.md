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

CI 실행과 게시 결과는 PR 생성 후 아래에 기록합니다.
