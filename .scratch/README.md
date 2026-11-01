# 프로젝트 작업 기록

최종 갱신: 2026-11-01. 대화에서 합의한 내용, 실제 코드·검증 결과, 확인한 GitHub 상태를 정리합니다.

| 기록 | 내용 |
| --- | --- |
| [서비스 스펙](service/spec.md) | 감정 탐색 대화의 목표, 기획 기능, 아직 정하지 않은 제품 정책 |
| [백엔드 초기 구성 스펙](backend-bootstrap/spec.md) | 채택 스택, 구현 범위, 설정 및 완료 조건 |
| [백엔드 검증 결과](backend-bootstrap/verification.md) | 실제 실행한 검사와 환경, 미검증 항목 |
| [백엔드 작업 이슈](backend-bootstrap/issues/01-bootstrap.md) | 완료한 작업과 남은 MySQL 8.4 검증 |
| [협업 방식 스펙](collaboration/spec.md) | 기본 폴더·브랜치 사용, Markdown 이슈, Git 추적 및 PR 규칙 |
| [진행 이력](history.md) | 기존 PR, 대화 중 결정 변경, 구현·환경 정리 이력 |

`.scratch/`의 스펙·이슈·작업 이력은 Git으로 추적하고 PR로 공유합니다. 비밀값과 개인 설정은 기록하지 않습니다. 기존 파일을 보존한 `preserved-before-bootstrap/`만 로컬 백업으로 제외합니다.

제품 요구사항의 상세 원문은 [서비스 기획안](../docs/service-overview.md)과 [기능 명세서](../docs/features.md), 확정한 용어는 [용어집](../GLOSSARY.md), 장기 결정은 [ADR](../docs/adr/0001-spring-boot-4.md)을 함께 확인합니다. 이 기록은 새 기능의 구현 승인이나 전체 MVP 확정을 의미하지 않습니다.
