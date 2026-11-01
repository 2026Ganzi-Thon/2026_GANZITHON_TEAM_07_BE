# 백엔드 초기 구성 스펙

관련 범위: F-01~F-10 구현 전 기반 작업. 기록일: 2026-11-01.

## 결정과 배경

초기 제안은 Spring Boot 3.x·Java 21·Gradle·MySQL·Swagger였습니다. Spring Boot 3.5.x의 OSS 지원 종료를 확인한 뒤 사용자가 추천안을 선택하여 Boot 4.x로 변경했습니다. 근거는 [ADR-0001](../../docs/adr/0001-spring-boot-4.md)에 기록했습니다.

| 항목 | 채택한 구성 |
| --- | --- |
| 프레임워크 | Spring Boot 4.1.2 / Spring MVC |
| 언어 | Java 21 toolchain |
| 빌드 | Gradle 9.8.2 Wrapper, 공식 SHA-256 검증 |
| DB | MySQL 8.4 LTS, 로컬 Docker Compose |
| 데이터 접근 | Spring Data JPA |
| 입력 검증 기반 | Spring Validation |
| API 문서 | springdoc 3.2.0 / Swagger UI |
| 테스트 | JUnit·Spring Boot 통합 테스트, H2와 선택적 실제 MySQL 검사 |
| LLM | 제공자·모델·키 빈 값, 연동 미구현 |

## 구현한 범위

- `com.ganzithon.backend.GanzithonApplication` 애플리케이션과 실행 JAR 빌드
- 환경변수 및 루트 `.env` 기반 설정과 `.env.example`
- MySQL 연결, 로컬 Compose·볼륨·DB 질의 healthcheck
- JPA `ddl-auto=validate`, `open-in-view=false`
- OpenAPI 제목·설명, Swagger 활성/비활성 설정
- 테스트, README·실행 가이드·아키텍처·DB·API 문서 동기화
- 감정 탐색 대화 용어집과 Boot 4 채택 ADR

업무 API, 엔티티·테이블, 마이그레이션, 인증·인가, 배포·CI, LLM 호출은 이번 구현에 포함하지 않았습니다.

## 실행 계약

JDK 21과 Docker Compose v2를 준비하고 저장소 루트에서 실행합니다.

```bash
cp .env.example .env
# .env의 예시 DB 비밀번호를 변경합니다.
docker compose up -d --wait mysql
./gradlew bootRun
```

- 앱 기본 주소: `127.0.0.1:8080`
- MySQL 호스트 기본 주소: `127.0.0.1:3307`
- Swagger UI: `http://127.0.0.1:8080/swagger-ui/index.html`
- OpenAPI: `http://127.0.0.1:8080/v3/api-docs`
- Swagger는 기본 비활성화이며 로컬 `.env.example`에서는 활성화합니다. 업무 API가 없어 목록은 비어 있습니다.
- `DB_PASSWORD`는 앱 실행 시 필수, `MYSQL_ROOT_PASSWORD`는 Compose 초기화 시 필수입니다.
- `.env`는 `KEY=value` 형식으로 작성합니다. 실제 비밀번호·키는 Git과 작업 기록에 남기지 않습니다.
- 기존 볼륨은 `.env`의 계정·비밀번호 변경만으로 재초기화되지 않습니다.

전체 환경변수와 중지 방법은 [실행 가이드](../../docs/getting-started.md)에 있습니다.

## 완료 조건과 확인 상태

- [x] Java 21 컴파일 및 실행 JAR 생성
- [x] 애플리케이션 기동과 테스트 DB 연결
- [x] Swagger/OpenAPI 활성 시 200, 비활성 시 404
- [x] 실제 MySQL JDBC 연결과 `SELECT 1` — 임시 MySQL 9.4 사용
- [x] 실행 JAR의 `.env` 로딩과 실제 HTTP 응답
- [x] 관련 문서 동기화 및 형식·링크 확인
- [ ] Compose MySQL 8.4 기동 및 같은 버전의 연결 검증 — Docker 미설치로 미검증

검증 상세: [verification.md](verification.md). 남은 작업: [이슈 01](issues/01-bootstrap.md).

구현 커밋: `30df747`. 관련 [PR #4](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/pull/4)는 2026-11-01 조회 시 OPEN이며 merge하지 않았습니다.
