# 공유 팀 저장소 방식으로 전환

[협업 워크플로](agents/workflow.md) · [VS Code 가이드](vscode.md)

## 목표

팀 저장소를 `origin` 하나로 사용합니다. 팀원은 작업 브랜치에 push하고 같은
저장소의 `main`으로 PR을 생성합니다. 로컬 `main`은 팀 `origin/main`을 따라가며,
진행 중인 작업은 작업 브랜치에 유지합니다.

## 새 클론

[VS Code 최초 설정](vscode.md)을 따릅니다. 저장소의 Git 훅(`.githooks/pre-push`, `.githooks/pre-commit`) 설치는
클론마다 한 번 필요합니다. 로컬 Git 설정은 커밋으로 자동 적용되지 않습니다.

## 기존 클론

1. `git status`, `git remote -v`, `git worktree list`, `git branch -vv`를 확인합니다.
   미커밋 파일과 원격 URL·브랜치 추적 정보를 보존합니다. 미병합 커밋도 이름 있는
   브랜치에서 계속 접근할 수 있어야 합니다.
2. 팀 원격으로 바꾸고 최신 참조를 받습니다.

```bash
git remote set-url origin https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git
git fetch --prune origin
git config --local remote.pushDefault origin
git config --local push.default simple
git branch --set-upstream-to=origin/main main
```

3. `upstream`이 같은 팀 저장소를 가리키는 중복 원격이면, 그 원격을 추적하는
   활성 작업이 없는지 확인한 뒤 `git remote remove upstream`으로 정리합니다.
   fork에만 있던 커밋은 로컬 브랜치 또는 별도 백업으로 보존한 뒤 참조를 정리합니다.
4. 로컬 main에 전용 커밋이 있는지 `git log --oneline origin/main..main`으로 확인합니다.
   결과가 비었으면 main에서 `git pull --ff-only origin main`으로 갱신합니다.
   결과가 있으면 `git branch backup/local-main-before-migration main`처럼 보존하고,
   작업 브랜치로 전환한 다음 보존 여부를 확인한 후에만 로컬 main 참조를 팀 기준으로
   옮깁니다. `reset --hard`로 작업을 버리지 않습니다.
5. 훅을 설치하고 `python3 scripts/test_pre_push.py` 및 `python3 scripts/test_pre_commit.py`를 실행합니다. 이후에는
   [협업 워크플로](agents/workflow.md)의 작업 브랜치·PR 절차를 따릅니다.

원격 URL 변경과 fetch는 서버의 브랜치나 기존 PR을 삭제하지 않습니다. 다만
기존 fork 브랜치의 추적 참조는 없어질 수 있으므로 새 팀 작업 브랜치는 첫 push에서
`git push -u origin <작업브랜치>`로 추적을 설정합니다.

## main 보호

로컬 훅은 팀 작업 브랜치 push를 허용하고 main·태그·삭제 push를 차단합니다.
GitHub 웹과 다른 클론까지 보호하려면 관리자가 저장소 ruleset 또는 branch
protection으로 main PR·리뷰·CI 필수, force push·삭제 금지를 설정해야 합니다.
CI job은 `build-and-test`입니다.

2026-11-01 전환 시 조회한 main의 `protected` 값은 `false`였고, 현재 계정의
권한은 `WRITE`여서 서버 보호 설정을 변경하지 않았습니다. PR merge 자동 수행을
차단하는 개인 Codex 규칙도 유지했습니다.

## 이 클론에서 수행한 전환

기존 main과 최신 팀 main은 모두 `6d4e354`로 일치하여 커밋을 버리거나 강제로
재작성하지 않았습니다. 기존 작업 브랜치와 stash는 보존했습니다. 기본 폴더의
`chore/shared-repository-workflow`에서 전환 작업을 진행합니다. 이전 원격·브랜치
정보는 Git에서 제외되는 로컬 백업 폴더에 보관했습니다.
