# 백엔드 초기 구성 검증 기록

실행일: 2026-11-01. 이 문서 정리 중 테스트를 새로 실행한 것은 아니며, 앞서 실제 실행한 결과와 보존된 JUnit XML을 대조한 기록입니다.

## 환경

- macOS arm64, 기본 설치 JDK는 Java 26
- 검증용 Temurin Java 21.0.13을 `/tmp`에 다운로드하여 `JAVA_HOME`으로 지정. 시스템 JDK를 교체하지 않음
- Gradle 캐시도 `/tmp`에 분리
- Docker 명령 없음
- 기존 MySQL 9.4 실행 파일로 별도 `/tmp` 데이터 디렉터리·소켓과 포트 33317을 사용. 기존 DB 데이터는 변경하지 않음

## 실행과 결과

Java 21 환경에서 DB 접속 정보를 환경변수로 주입하고 실행:

```bash
RUN_MYSQL_TESTS=true ./gradlew clean test bootJar
```

| 테스트 클래스 | 확인 내용 | 결과 |
| --- | --- | --- |
| GanzithonApplicationTests | OpenAPI 200, Swagger UI 200, H2 `SELECT 1` | 3개 통과 |
| ApiDocsDisabledTests | 문서 비활성 시 OpenAPI·Swagger UI 404 | 2개 통과 |
| MySqlConnectionTests | DB 제품명 MySQL 및 실제 `SELECT 1` | 1개 통과 |

합계 6개 통과, 실패·오류·스킵 0개. 보존된 결과는 저장소 루트 `build/test-results/test/` 및 `build/reports/tests/test/`에 있습니다. 빌드 산출물은 Git 추적 대상이 아닙니다.

`RUN_MYSQL_TESTS`를 주지 않으면 실제 MySQL 테스트는 건너뜁니다. 일반 테스트용 H2를 MySQL 호환성 검증으로 간주하지 않습니다.

## 실행 JAR 확인

생성한 `ganzithon-0.0.1-SNAPSHOT.jar`를 Java 21로 실행했습니다. 별도 임시 작업 폴더의 `.env`에서 DB·포트·Swagger 설정을 읽도록 하여 MySQL 9.4 연결 및 포트 18087의 OpenAPI·Swagger UI HTTP 200을 확인했습니다. 확인 후 앱과 임시 DB 서버를 종료했습니다.

## 나머지 검사와 제한

- 문서의 로컬 링크와 `git diff --check` 확인
- Gradle 배포 파일의 공식 SHA-256 적용
- Docker가 없어 Compose 실행과 MySQL 8.4 연결은 미검증
- LLM, 업무 API, 배포, 인증은 구현하지 않아 해당 검증 없음

DB 비밀번호, API 키, 원본 환경 파일은 이 기록에 포함하지 않습니다.
