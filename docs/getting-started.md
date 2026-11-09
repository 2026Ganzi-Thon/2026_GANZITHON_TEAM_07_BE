# 개발 환경 및 실행 가이드

[README로 돌아가기](../README.md)

## 필요한 도구

- JDK 21: `java -version`으로 확인합니다. Gradle은 설치하지 않고 Wrapper를 사용합니다.
- Docker Engine/Desktop + Docker Compose v2: 로컬 MySQL 실행에 사용합니다.
- Spring Boot 4.1.2, Gradle 9.8.2, MySQL 8.4 LTS, springdoc 3.2.0을 사용합니다.

팀 저장소 클론과 작업 브랜치 구성은 [협업 워크플로](agents/workflow.md), 편집기 설정은 [VS Code 협업 가이드](vscode.md)를 따릅니다.

## 로컬 실행

프로젝트 루트에서 실행합니다. Windows에서는 `./gradlew` 대신 `gradlew.bat`를 사용합니다.

```bash
cp .env.example .env
# .env의 DB_PASSWORD, MYSQL_ROOT_PASSWORD를 로컬 비밀번호로 변경합니다.
# AUTH_JWT_SECRET에는 `openssl rand -base64 32` 출력값을 넣습니다.
docker compose up -d --wait mysql
./gradlew bootRun
```

Spring Boot와 Compose 모두 루트의 `.env`를 읽습니다. `.env`는 `KEY=value` 형식으로 작성하고 값에 따옴표나 `export`를 붙이지 않습니다. 실제 비밀값은 커밋하지 않습니다. 셸 환경변수로 같은 키를 지정하면 `.env`보다 우선합니다.

기본 접속 주소:

- Health: http://127.0.0.1:8080/api/v1/health

- Swagger UI: http://127.0.0.1:8080/swagger-ui/index.html
- OpenAPI JSON: http://127.0.0.1:8080/v3/api-docs

서버 기동 후 `curl http://127.0.0.1:8080/api/v1/health`로 `{"status":"UP"}`을 확인합니다. health는 인증 없이 접근하고 DB·LLM 상태를 조회하지 않습니다. Actuator health는 `http://127.0.0.1:8080/actuator/health`에서 확인할 수 있으며, HTTP에는 health만 노출되고 상세 정보는 숨깁니다. 현재 앱 기동에는 DB가 필요합니다.

F-00 인증 API와 말랑이 인벤토리 조회·선택 API가 구현되어 있습니다. Flyway V1은 회원·토큰·인벤토리 테이블을 만들고 V2는 기존 계정의 기본 SLANGI를 보정합니다. 로컬·소셜 인증 모두 JWT 서명 키가 필요합니다. OAuth 로그인을 실제 사용하려면 공급자별 client ID/secret과 앱에 등록한 redirect URI를 `.env`에 설정해야 합니다. 설정이 비어 있어도 서버는 기동하지만 해당 공급자 로그인은 사용할 수 없습니다. Spring AI provider/model 설정은 아직 없어 외부 AI 호출은 발생하지 않습니다.

## 환경변수

| 이름 | 용도 | 기본값 / 조건 |
| :--- | :--- | :--- |
| SERVER_ADDRESS | 애플리케이션 바인딩 주소 | `127.0.0.1` |
| SERVER_PORT | 애플리케이션 포트 | `8080` |
| DB_HOST | MySQL 호스트 | `127.0.0.1` |
| DB_PORT | MySQL 호스트 포트 | `3307` |
| DB_NAME | 데이터베이스 이름 | `ganzithon` |
| DB_USERNAME | 애플리케이션 DB 사용자 | `ganzithon` |
| DB_PASSWORD | 애플리케이션 DB 비밀번호 | 필수, `.env`에 설정 |
| MYSQL_ROOT_PASSWORD | Compose MySQL 초기 root 비밀번호 | Compose 실행 시 필수 |
| API_DOCS_ENABLED | OpenAPI 및 Swagger UI 공개 여부 | 기본 `false`, 로컬 예시 `true` |
| AUTH_JWT_SECRET | JWT HS256 서명 키 (Base64) | 필수, 디코딩 후 32바이트 이상 |
| KAKAO_CLIENT_ID / KAKAO_CLIENT_SECRET / KAKAO_REDIRECT_URI | Kakao OAuth 앱 설정 | OAuth 사용 시 필수 |
| DISCORD_CLIENT_ID / DISCORD_CLIENT_SECRET / DISCORD_REDIRECT_URI | Discord OAuth 앱 설정 | OAuth 사용 시 필수 |
| GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET / GOOGLE_REDIRECT_URI | Google OAuth 앱 설정 | OAuth 사용 시 필수 |
| LLM_PROVIDER / LLM_MODEL / LLM_API_KEY | 추후 LLM 연동용 예약 항목 | 빈 값, provider 선택·호출 미구현 |

기존 MySQL을 사용하면 Compose 실행을 생략하고 DB 접속 정보를 설정합니다. 앱 시작 시 Flyway가 V1·V2 스키마를 적용하고, Hibernate `ddl-auto=validate`가 엔티티와 스키마를 검증합니다.

Compose는 MySQL을 호스트의 `127.0.0.1`에만 노출합니다. 운영 배포·TLS·인증은 별도 설계 대상이며, 외부 바인딩이 필요하면 `SERVER_ADDRESS`를 환경에 맞게 지정합니다.

## 빌드 및 테스트

```bash
./gradlew clean test bootJar
java -jar build/libs/ganzithon-0.0.1-SNAPSHOT.jar
```

일반 테스트는 MVC slice의 health 계약과 DB 의존성 없음, H2 기반 앱 기동·health·Swagger 접근, JDBC 연결, ArchUnit 의존 방향을 검사합니다. MySQL 호환성 검증을 대체하지 않습니다.

Testcontainers 기반 MySQL 8.4 연결과 Flyway 초기화를 확인하려면 Docker를 실행한 뒤 다음 명령을 사용합니다. Docker가 없으면 해당 테스트는 건너뜁니다.

```bash
./gradlew test --tests '*MySqlTestcontainersSmokeTests'
```

실제 MySQL 연결 테스트는 위 로컬 DB 준비 후 별도로 실행합니다:

```bash
RUN_MYSQL_TESTS=true ./gradlew test --tests '*MySqlConnectionTests' --rerun-tasks
```

`RUN_MYSQL_TESTS`가 없으면 MySQL 테스트는 건너뜁니다. 실행 결과는 `build/reports/tests/test/index.html`에서 확인합니다.

## PR 자동 검증

GitHub Actions의 `CI / build-and-test`가 main 대상 PR과 main 갱신 시 실행됩니다.
Java 21, MySQL 8.4 임시 서비스에서 전체 테스트·JAR 빌드와 push 훅 회귀 검사를
수행합니다. CI용 DB 값은 해당 일회성 서비스 전용이며 운영 비밀값은 사용하지 않습니다.
로컬 훅 검사는 Python 3으로 실행할 수 있습니다.

```bash
python3 scripts/test_pre_push.py
```

## 중지 및 문제 확인

```bash
docker compose logs mysql
docker compose down
```

DB 데이터는 `mysql-data` 볼륨에 유지됩니다. `.env`의 DB 이름·사용자·비밀번호 변경은 기존 볼륨의 계정을 바꾸지 않으므로, 이미 초기화한 DB는 기존 계정을 사용하거나 직접 계정을 변경해야 합니다. 병렬 worktree에서는 `SERVER_PORT`, `DB_PORT`를 각각 다르게 지정합니다.

## 초기 구성 검증 기록 · 2026-11-01

Temurin Java 21.0.13에서 `clean test bootJar`를 실행해 테스트 6개가 통과했습니다. MySQL 연결 테스트는 설치된 MySQL 9.4의 별도 임시 DB로 실행했고, 실행 JAR이 `.env`를 읽어 DB에 연결한 뒤 Swagger UI와 OpenAPI를 HTTP 200으로 반환하는 것도 확인했습니다. Docker가 없는 환경이라 Compose의 MySQL 8.4 기동은 검증하지 못했습니다.

후속 [공유 저장소 CI](https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE/actions/runs/41316312030)에서는 Java 21.0.13·MySQL 8.4.13 연결을 포함한 테스트와 JAR 빌드가 통과했습니다. 로컬 Compose 실행은 별도 확인 대상입니다.
