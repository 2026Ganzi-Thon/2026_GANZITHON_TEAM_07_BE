# 🚀 2026 GANZITHON · TEAM 07 Backend

> AI 말랑이와 함께 감정을 정리하고, 나만의 감정 패턴을 발견하여 더 나은 방향으로 나아갈 수 있도록 돕는 개인화 감정 케어 서비스

텍스트·음성으로 감정을 기록하고, AI 질문과 대화로 생각을 정리하며, 축적된 기록에서 감정 패턴을 발견하도록 돕습니다. 대화 이후에는 감정에 맞는 말랑이와 터치·드래그·ASMR 인터랙션으로 감각적인 감정 환기를 연결합니다.

서비스명은 미정이며, 현재는 Spring Boot 백엔드 초기 구성을 완료하고 업무 기능 구현을 준비하는 단계입니다. 아래 기능은 구현 예정입니다. 자세한 배경과 사용자 흐름은 [서비스 기획안](docs/service-overview.md)을 확인하세요.

<br />

## 👥 팀원 소개 (Backend)

| 이름 | 소속 | 연락처 | 담당 역할 |
| :--- | :--- | :--- | :--- |
| 이영현 | 성결대학교 | [ejjm0727@gmail.com](mailto:ejjm0727@gmail.com) | 미정 |
| 양한울 | 수원대학교 | [gierdanigi123@gmail.com](mailto:gierdanigi123@gmail.com) | 미정 |
| 강현우 | 가천대학교 | [al457616@gachon.ac.kr](mailto:al457616@gachon.ac.kr) | 미정 |

<br />

## 🛠️ 기술 스택 (Tech Stack)

| 영역 | 기술 | 상태 |
| :--- | :--- | :--- |
| Backend Framework | Spring Boot 4.1.2 / Spring MVC | 초기 구성 완료 |
| Language / JDK | Java 21 | Gradle toolchain |
| Build | Gradle 9.8.2 | Wrapper 제공 |
| Data | MySQL 8.4 LTS / Spring Data JPA | 연결 설정 및 로컬 Compose 제공 |
| API Documentation | springdoc 3.2.0 / Swagger UI | 로컬 활성화 |
| LLM | | 미정 |
| Security | 미정 | 인증·인가 요구사항에 따라 결정 |
| Infrastructure | 미정 | 배포 환경 선정 필요 |
| CI | GitHub Actions | PR 및 main push 시 훅·Java 21·MySQL 8.4 검사 |
| Deployment | 미정 | 배포 방식 선정 필요 |

<br />

## 🗺️ 시스템 아키텍처 (System Architecture)

아래는 초기 구성입니다. 기능별 헥사고날 구조와 health API를 제공하며 감정 탐색 업무 API·LLM 연동은 미구현입니다.

```mermaid
flowchart LR
    Client[클라이언트] --> Backend[Spring Boot 4.1 · Java 21]
    Backend --> DB[(MySQL 8.4)]
```

단일 Gradle 모듈에서 기능별 `domain / application / adapter` 구조를 사용하고 어댑터가 안쪽 application 포트에 의존하도록 구성합니다. 실제 도메인·출력 어댑터는 기능 구현 시 추가합니다.

구성 요소와 통신 방식은 [시스템 아키텍처](docs/architecture.md)에 정리합니다.

<br />

## 📊 ER Diagram (데이터베이스 구조)

도메인과 데이터베이스는 설계 전입니다. 엔티티와 관계가 확정되면 [데이터베이스 설계 및 ERD](docs/database.md)에 Mermaid ERD를 작성합니다.

<br />

## ✨ 핵심 기능 및 구현 상세 (Core Features)

- **AI 감정 대화 및 기록:** 텍스트·음성 입력과 질문·답변으로 감정의 원인과 사고 과정을 구체화
- **맞춤형 조언 및 말랑이 추천:** 대화 이후 감정 상태에 맞는 공감·조언과 말랑이 제공
- **말랑이 인터랙션:** 터치·드래그에 따른 형태 변형과 ASMR 사운드로 감정 환기
- **마음 보관함:** 감정 기록을 축적하고 다시 열람
- **감정 패턴 분석 및 개인화 인사이트:** 시간에 따른 감정 변화와 반복되는 스트레스 요인을 분석

전체 기획 기능과 결정할 사항은 [기능 명세서](docs/features.md)에 정리합니다. MVP 포함 범위는 아직 미정입니다.

<br />

## 📑 API 명세서 (API Documentation)

요청·응답 형식, 인증 조건, 오류 응답은 [API 명세서](docs/api.md)에서 관리합니다. 서버 확인용 `GET /api/v1/health`가 `{"status":"UP"}`을 반환합니다. 감정 탐색 업무 API는 아직 없으며, 로컬 실행 후 [Swagger UI](http://127.0.0.1:8080/swagger-ui/index.html)에서 추가되는 API를 확인할 수 있습니다.

<br />

## 🔥 기술적 의사결정 및 트러블슈팅

채택한 기술과 추후 기술 선택·문제 해결 기록은 [기술적 의사결정 및 트러블슈팅](docs/technical-decisions.md)에 정리합니다.

<br />

## 🌲 Git 브랜치 전략 (Git Branching Strategy)

`origin`은 팀 저장소를 가리킵니다. 최신 `origin/main`에서 작업 브랜치를 만들고, 팀 저장소에 해당 브랜치를 push한 뒤 `main`으로 PR을 보냅니다. 로컬 `main`에는 팀에서 merge된 변경만 pull로 반영합니다.

- `main`: 팀 저장소의 기준 브랜치
- `feat/<이슈번호>-<기능명>`: 기능 개발
- `fix/<이슈번호>-<버그명>`: 버그 수정
- `docs/<설명>`: 문서 작성 및 수정
- `chore/<설명>`: 저장소 설정 및 유지보수

이슈 번호가 없으면 생략할 수 있습니다. 커밋은 `feat(auth): 로그인 API 추가`처럼 `<type>(<scope>): <한국어 설명>` 형식을 사용합니다.

```text
팀 origin/main → 작업 브랜치 → 팀 origin에 브랜치 push → main으로 PR → 리뷰·CI → merge
```

`main`에 직접 push하지 않으며, pre-push 훅을 우회하지 않습니다. 자세한 절차는 [협업 워크플로](docs/agents/workflow.md)와 [PR 작성 규칙](docs/agents/pr.md)을 확인하세요.

<br />

## ⚙️ 시작하기 (Getting Started)

```bash
git clone https://github.com/2026Ganzi-Thon/2026_GANZITHON_TEAM_07_BE.git
cd 2026_GANZITHON_TEAM_07_BE
```

JDK 21과 Docker Compose v2를 준비한 뒤 실행합니다.

```bash
cp .env.example .env
# .env의 로컬 DB 비밀번호를 변경하세요.
docker compose up -d --wait mysql
./gradlew bootRun
```

빌드·테스트: `./gradlew clean test bootJar`. 기본 테스트는 H2를 사용하며, 실제 MySQL 연결 검증 방법은 아래 실행 가이드를 참고합니다.

JDK 버전, 환경변수, 로컬 실행·테스트 절차는 [개발 환경 및 실행 가이드](docs/getting-started.md)에 정리합니다. 기여 시에는 팀 저장소 쓰기 권한을 준비하고 [협업 워크플로](docs/agents/workflow.md)의 원격 저장소 설정을 따릅니다.

<br />

## 📚 문서 모음

| 문서 | 내용 |
| :--- | :--- |
| [서비스 기획안](docs/service-overview.md) | 서비스 배경, 목표, 타겟, 사용자 흐름 |
| [기능 명세서](docs/features.md) | 기능별 요구사항, 처리 규칙, 완료 조건 |
| [감정 대화 문서](docs/emotional-dialogue/README.md) | BE·AI 역할, 상태 추정, 질문 전략 조사와 데이터셋·사람 평가 설계 |
| [API 명세서](docs/api.md) | 엔드포인트, 요청·응답, 인증, 오류 |
| [시스템 아키텍처](docs/architecture.md) | 시스템 구성과 통신 흐름 |
| [데이터베이스 설계 및 ERD](docs/database.md) | 테이블, 관계, 제약조건 |
| [기술적 의사결정 및 트러블슈팅](docs/technical-decisions.md) | 기술 선택 근거와 문제 해결 기록 |
| [개발 환경 및 실행 가이드](docs/getting-started.md) | 로컬 설정, 실행, 테스트 |
| [에이전트 개발 절차](docs/agents/development.md) | 문서 읽기, 범위 결정, 명세 구체화, 완료 기준 |
| [협업 워크플로](docs/agents/workflow.md) | 브랜치, 커밋, 팀 저장소 PR 절차 |
| [공유 저장소 전환](docs/shared-repository-migration.md) | 기존 fork 클론의 원격·main 추적 정리 |
