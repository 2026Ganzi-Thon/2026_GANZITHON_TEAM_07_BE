# 백엔드 실행 기반 구성

Status: ready-for-human

## 완료한 작업

Spring Boot, Java 21 toolchain, Gradle Wrapper, MySQL 연결과 Compose, JPA·Validation, Swagger UI, 실행 문서 및 검증 완료. LLM 설정은 빈 값으로 유지.

## 남은 확인

Docker 사용 가능한 로컬 환경에서 `docker compose up -d --wait mysql`과 실행 설정을 확인합니다. MySQL 8.4 JDBC 연결과 전체 빌드는 공유 저장소 전환 CI에서 통과했습니다.

## Comments

검증 환경에는 Docker가 없어 로컬 MySQL 9.4의 임시 DB로 연결을 확인했습니다.

후속 검증: [공유 저장소 CI 기록](../../shared-repository-workflow/verification.md).
