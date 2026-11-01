# VS Code에서 브랜치로 협업하기

[개발 환경 가이드](getting-started.md) · [협업 워크플로](agents/workflow.md)

## 공유 설정

저장소의 `.vscode/settings.json`은 다음 설정을 팀에 공유합니다.

- `git.detectWorktrees`: Git에 등록된 로컬 worktree 자동 탐지
- `git.autofetch: all`: 모든 원격 저장소의 변경 사항을 주기적으로 fetch
- `git.confirmSync`: 동기화 전 확인 창 표시

설정이 포함된 저장소 폴더를 VS Code에서 열면 적용됩니다. 이미 열려 있다면
명령 팔레트에서 `Developer: Reload Window`를 실행합니다. 자동 fetch는
원격 참조를 갱신하며, 작업 파일을 자동으로 pull하거나 merge하지 않습니다.

## 작업 폴더 선택

기본 저장소 폴더를 열고 작업 브랜치를 전환하며 개발합니다. 새 작업마다
폴더를 열 필요가 없습니다. `.scratch/<작업명>/`에는 로컬 이슈와 설계 기록을
두고, 애플리케이션 코드는 루트의 `src/`에서 수정합니다.

병렬 작업을 위해 선택적으로 worktree를 만들었을 때만 해당 폴더를 별도로
엽니다. 소스 제어의 Repositories 선택은 탐색기의 폴더를 바꾸지 않습니다.
다른 worktree에서 사용 중인 브랜치는 checkout 대신 그 폴더를 엽니다.
진행 중인 미커밋 변경을 보려는 목적으로 detached checkout을 사용하지 않습니다.

## 최초 push와 동기화

각 팀원은 개인 포크를 `origin`, 팀 저장소를 `upstream`으로 구성합니다.
새 작업 브랜치는 [협업 워크플로](agents/workflow.md)의 자동 추적을 끈 명령으로
만들고, 커밋 후 첫 push에서 개인 포크의 추적 브랜치를 설정합니다.

```bash
git push -u origin <작업브랜치>
git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}'
```

출력이 `origin/<작업브랜치>`인지 확인한 후 VS Code의 동기화를 사용합니다.
동기화 대상이 `upstream/main`이면 진행 전에 다음처럼 교정합니다.

```bash
git branch --set-upstream-to=origin/<작업브랜치> <작업브랜치>
```

이 명령은 해당 브랜치를 이미 포크에 push했을 때 사용합니다. 처음이라면
`git push -u origin <작업브랜치>`를 사용합니다. 기본 push 대상도 개인 포크로
정하려면 각 클론에서 한 번 설정합니다. 이 Git 설정은 커밋으로 공유되지 않습니다.

```bash
git config --local remote.pushDefault origin
git config --local push.default simple
```

팀 저장소의 `main`에 직접 push하려는 시도는 pre-push 훅이 차단합니다.
작업 브랜치 push 후 팀 저장소로 PR을 생성하는 흐름을 유지합니다.

## `.worktrees/`를 Git에서 제외하는 이유

`.scratch/`는 Git에서 제외되는 로컬 작업 기록이고, `.worktrees/`는 병렬 작업이 필요할 때만 만드는 개발자의 브랜치별 로컬 작업 사본입니다. 작업 내용은 해당
브랜치의 커밋과 PR로 공유합니다. 디렉터리 자체를 상위 저장소에 커밋하면 작업
사본을 중첩 저장하거나 로컬 Git 메타데이터를 잘못 포함할 수 있습니다.

따라서 `/.worktrees/`는 계속 제외합니다. VS Code의 worktree 탐지는 Git의
등록 정보를 사용하므로 이 ignore 규칙을 제거할 필요가 없습니다. 각 팀원은
자신의 클론에서 필요한 worktree를 생성합니다.

`.vscode/settings.json`은 공유하고 나머지 `.vscode/`의 로컬 파일은 제외합니다.
새 팀 설정 파일을 공유할 때는 `.gitignore`의 허용 목록을 함께 갱신합니다.

## 참고

- [VS Code 공식 worktree 안내](https://code.visualstudio.com/docs/sourcecontrol/branches-worktrees)
- [Git worktree 문서](https://git-scm.com/docs/git-worktree)
