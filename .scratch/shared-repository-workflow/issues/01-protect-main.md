# 팀 main 서버 보호 설정

Status: ready-for-human

## 확인한 상태

2026-11-01 팀 main은 protected=false이고 현재 사용자의 권한은 WRITE입니다. 로컬 pre-push 훅과 CI는 구성했지만 GitHub 웹과 다른 클론에 적용되는 서버 보호는 관리자 설정이 필요합니다.

## 완료 조건

- main 변경에 PR과 코드 리뷰를 요구
- PR의 `build-and-test` CI 검사 통과를 merge 조건으로 설정
- main force push와 삭제 차단
- 설정 후 main 보호가 활성화됐는지 조회하여 확인

## Comments

이번 전환에서는 서버 보호를 변경하거나 관리자 권한을 우회하지 않았습니다. CI job은 [PR #5](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/5)에 추가했습니다.
