# VS Code에서 브랜치로 협업하기

[개발 환경 가이드](getting-started.md) · [협업 워크플로](agents/workflow.md)

## 저장소와 폴더

팀 저장소를 클론한 기본 폴더를 계속 사용합니다. `origin`은 팀 저장소
`2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE`를 가리키며 개인 fork는 필요하지 않습니다.
애플리케이션 코드는 `src/`, Git으로 공유하는 이슈·스펙·이력은 `.scratch/`에 둡니다.

## 최초 설정

새 클론은 아래와 같이 준비합니다.

```bash
git clone https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git
cd 2026_GANZITHON_TEAM_07_BE
chmod +x .githooks/pre-push
git config --local core.hooksPath "$PWD/.githooks"
git config --local remote.pushDefault origin
git config --local push.default simple
```

팀 저장소 쓰기 권한이 있어야 작업 브랜치를 push할 수 있습니다. 기존 클론의
원격과 로컬 main을 정리하는 절차는 [공유 저장소 전환](shared-repository-migration.md)을 따릅니다.

## 작업 시작과 push

변경 사항을 보존한 뒤 최신 팀 main에서 작업 브랜치를 만듭니다.

```bash
git fetch origin
git switch main
git pull --ff-only origin main
git switch --no-track -c feat/login origin/main
# 구현하고 검사한 뒤 커밋합니다.
git push -u origin feat/login
git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}'
```

마지막 출력이 `origin/feat/login`인지 확인한 후 작업 브랜치에서 VS Code 동기화를
사용합니다. PR은 같은 팀 저장소의 `main`을 대상으로 생성하고 리뷰·CI를 확인합니다.

## main에서 동기화 버튼을 누른 경우

`main`은 팀에서 merge된 변경을 pull로 받는 기준 브랜치입니다. VS Code의 동기화는
push도 시도하므로 작업 브랜치에서 사용합니다. main 갱신에는 `Git: Pull` 또는
`git pull --ff-only origin main`을 사용합니다. main에 로컬 전용 커밋이 있으면
먼저 보존할 브랜치를 만들고 [전환 가이드](shared-repository-migration.md)를 따릅니다.
pre-push 훅이 main push를 차단하면 훅을 우회하지 않습니다.

## 공유 설정과 선택적 worktree

- `git.autofetch: true`: 원격 참조를 주기적으로 갱신하며 자동 pull·merge는 하지 않음
- `git.confirmSync: true`: 동기화 전 확인
- `git.detectWorktrees: true`: 선택적으로 만든 병렬 worktree 탐지

기본 폴더에서 브랜치를 전환하면 됩니다. 별도 worktree를 선택한 경우에만 해당
폴더를 엽니다. Repositories 선택은 탐색기의 폴더를 바꾸지 않습니다.

`.worktrees/`는 로컬 작업 사본이므로 Git에서 제외합니다. `.scratch/`의 협업
기록은 추적합니다. `.vscode/settings.json`은 공유하고 나머지 개인 편집기 설정은 제외합니다.

## 참고

- [VS Code worktree 안내](https://code.visualstudio.com/docs/sourcecontrol/branches-worktrees)
- [Git worktree 문서](https://git-scm.com/docs/git-worktree)
