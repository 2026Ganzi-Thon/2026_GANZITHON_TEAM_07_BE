# 협업 및 작업 기록 스펙

기록일: 2026-11-01. 대화에서 확정한 최종 작업 방식입니다.

## 기본 작업 위치

코드는 기본 저장소 폴더의 작업 브랜치에서 수정합니다. 작업마다 worktree를 만들지 않습니다. 병렬 작업이 필요할 때만 선택적으로 사용합니다. 기본 폴더를 한 번 열면 브랜치 전환으로 작업할 수 있습니다.

## 작업 기록과 Git 추적

- `.scratch/<작업명>/spec.md`: 합의한 요구사항·범위·완료 조건
- `.scratch/<작업명>/issues/NN-설명.md`: 작업별 티켓, 개별 파일 사용
- `.scratch/<작업명>/verification.md`: 필요한 경우 실제 검증 결과
- `.scratch/history.md`: 프로젝트 작업 및 결정 변경 이력
- `.scratch/README.md`: 기록의 진입점

처음에는 로컬 전용·Git 제외로 정했지만, 사용자가 팀과 이력을 공유하기 위해 Git 추적을 요청하여 변경했습니다. 스펙·이슈·이력은 커밋과 PR로 공유합니다. 비밀값과 개인 설정은 기록하지 않습니다. 과거 미커밋 파일을 보존한 `preserved-before-bootstrap/`는 로컬 백업으로 제외합니다.

이슈는 GitHub Issues 대신 Markdown 파일로 관리합니다. 기존 다섯 triage 상태값은 유지합니다. GitHub 이슈 번호와 로컬 티켓 번호를 혼용하지 않습니다. 확정 용어는 루트 `GLOSSARY.md`, 장기 아키텍처 결정은 `docs/adr/`에도 유지합니다.

## 공유 경로

작업 브랜치 → 개인 fork `origin` → 팀 저장소 PR. `main`과 팀 저장소에 직접 push하지 않으며 pre-push 훅을 유지합니다. 이슈 저장 위치 변경은 GitHub PR 흐름을 없애는 결정이 아닙니다.

## Codex 명령 허용 설정

사용자 요청으로 개인 Codex 규칙에서 `gh` 명령을 영구 허용했습니다. 이후 upstream merge를 막아 달라는 요청에 따라 `gh pr merge` 전체를 금지했습니다. `--repo` 인자 위치나 현재 폴더에 따른 누락을 피하기 위한 범위이며 fork PR merge에도 적용됩니다.

조회·PR 생성은 허용, `gh pr merge`는 금지 판정을 확인했습니다. 이는 개인 Codex의 명령 규칙이고 GitHub 웹의 권한·브랜치 보호 설정은 아닙니다. Git push의 별도 승인 규칙과 저장소 pre-push 훅은 유지했습니다. 개인 Codex 설정 파일은 저장소에 복사하지 않습니다.

## 남아 있는 worktree

기존 두 worktree는 브랜치 점유를 해제한 detached 상태입니다. 폴더가 존재해도 현재 브랜치를 점유하지 않습니다. README worktree의 미커밋 `.gitignore` 수정은 보존했습니다. 백엔드 초기 구성용 임시 worktree는 기본 폴더로 옮긴 뒤 제거했습니다.

이 정보는 기록 당시 상태이며, 실제 작업 전에는 `git status`와 `git worktree list`로 다시 확인합니다.
