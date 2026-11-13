# 프롬프트

LLM에 보내는 프롬프트를 코드와 분리해 파일로 관리합니다. 관련 기능: F-02 AI 감정 대화, F-09 위기 대응.
근거 문서: [`docs/emotional-dialogue`](../../../../docs/emotional-dialogue/README.md), [`docs/api.md`](../../../../docs/api.md) 3-3, `.scratch/emotional-dialogue/issues` 01~07

## 구조

```text
prompts/
├── CHANGELOG.md                   # 변경 내용, 이유, 시뮬레이션 결과
├── emotion-conversation/          # F-02 상담 대화
│   ├── common.md                  # 공통 시스템 프롬프트 (역할·기법·운영 규칙·금지·출력 형식·현재 상태)
│   ├── steps/STEP_01~06.md        # 단계별 전용 프롬프트 + 예시 (01번 이슈)
│   └── output.schema.json         # 응답 JSON 형식 (05번 이슈)
└── safety-classifier/             # F-09 위기 신호 2차 판단
    ├── system.md
    └── output.schema.json
```

## 서버가 프롬프트를 쓰는 방법

프롬프트가 전제하는 입력과 출력입니다. 호출 시점, 단계 전환, 실패 처리 등 구현 방식은 팀 결정을 따릅니다.

화면 흐름은 와이어프레임(2026-11-14) 기준입니다. AI 캐릭터는 "테스형"(소크라테스), 말투는 하오체입니다.

| 화면 시점 | 프롬프트 | AI가 만드는 것 |
| --- | --- | --- |
| 대화 화면 첫 메시지 ~ 상황 파악 | `STEP_01` | 공감·질문, 상황(`situation`)과 감정 기록 카드의 "유발 상황"(`triggerFactor`) |
| 감정 기록 카드 (대표·부가 감정, 강도는 사용자가 고름) 이후 | `STEP_02` ~ `STEP_05` | 공감·질문, 생각·다른 해석·바란 것·버텨 온 방법·행동, 답 예시 칩(`replyOptions`) |
| 상담 후 강도 입력 이후 | `STEP_06` | 대화 정리, 기록함 제목(`summary`), "대화에서 남은 문장"(`keySentence`), 말랑이 추천 문장(`malangiReason`), 기록함용으로 다듬은 생각·관점·실천 |
| 사용자 메시지마다 | `safety-classifier` | 위기 신호 여부 |

- 단계 파일마다 `common.md`와 합쳐 하나의 시스템 프롬프트로 보냅니다.

- 시스템 프롬프트 = `common.md`의 "# 현재 상태" 앞부분 + 단계 파일 + "# 현재 상태" 블록. `{{...}}`는 서버가 채웁니다.
- 대화 기록은 최근 8턴(AI 발화는 JSON 원문)만 보내고, 그 이전 내용은 누적 슬롯(`session_state.slots`)으로 넘깁니다 (04번 이슈).
- 위기 판단: 1차 서버 필터를 통과한 사용자 메시지를 `safety-classifier`로 판단합니다(safety.md 2중 감지).
- 프롬프트 측정 도구: [`eval/`](../../../../eval/README.md) — 임의로 작성한 측정 도구이니 개발에는 참고하지 마세요. 개발은 팀 결정을 따릅니다.

## 응답 형식 (`emotion-conversation/output.schema.json`)

| 키 | 의미 |
| --- | --- |
| `userMessage` | 사용자에게 보여 줄 말 |
| `transitionReady` | 현재 단계 목표 충족 → 다음 단계 추천. 서버가 필수 슬롯을 확인해 최종 판단 |
| `extractedData` | 이번 메시지로 확인된 슬롯 (`situation`, `triggerFactor`, `initialThought`, `alternativeThought`, `underlyingNeed`, `copingStrategy`, `actionPlan`, STEP_06의 `summary`, `keySentence`, `malangiReason`). `null` = 변화 없음, `""` = 없다·모르겠다고 답함 |
| `questionTarget` (확장) | 질문이 묻는 항목. 이미 확인된 항목을 다시 묻는지 서버가 검증 |
| `replyOptions` (확장) | 질문 아래 답 예시 칩. STEP_02 생각 질문, STEP_05 행동 후보에서만 (와이어프레임) |
| `endRequested` (확장) | 사용자 종료 요청 → STEP_06 |
| `backToStep` (확장) | 새 핵심 사건 → 되돌아가기 제안 (process.md 운영 원칙 4) |

## 규칙

- 프롬프트 문자열을 Java 코드에 직접 쓰지 않고 이 폴더의 파일을 읽어 사용합니다.
- 내용은 기획안과 `docs/emotional-dialogue` 기준으로 씁니다. 문서에 없는 규칙은 "(보안 보강, 기획안 외)"처럼 표시합니다.
- 바꾸면 `CHANGELOG.md`에 바꾼 점·이유·`eval/` 시뮬레이션 결과를 기록합니다.
- API 키는 `.env`(`LLM_API_KEY`, `ANTHROPIC_API_KEY`)에 둡니다.
