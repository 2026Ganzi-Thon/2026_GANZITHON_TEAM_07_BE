# 프롬프트 시뮬레이션 (eval)

> **임의로 작성한 프롬프트 측정 도구입니다. 개발에는 참고하지 마세요. 개발은 팀 결정을 따릅니다.**
> 서버 흐름(단계 전환, 타임아웃, 위기 1차 필터, 폴백 문장 등)은 프롬프트를 돌려 보려고 임의로 흉내 낸 것이며 API·DB 구현 기준이 아닙니다.

F-02 상담 프롬프트를 고칠 때 대화를 자동으로 돌려 보고 채점하는 도구입니다. Python 3 표준 라이브러리만 씁니다.

| 파일 | 내용 |
| --- | --- |
| `engine.py` | 측정용 서버 흐름 흉내. 세션 시작(STEP_02) → 위기 1차 필터 → 분류기·상담 병렬 호출 → 슬롯 병합 → 단계 전환·되돌아가기 → 정리 |
| `simulate.py` | 가상 사용자(LLM)가 시나리오대로 답하며 처음부터 끝까지 대화하고 자동 채점 |
| `scenarios.json` | 시나리오 14개 (persona, 첫 상황 글, 대표 감정, 강도, 고정 대사 `script`, 기대 결과 `expect`) |
| `llm.py` | .env 읽기, Claude / OpenAI 호환 API 호출, 구조화 출력 변환 |
| `fallback.json` | 시뮬레이션용 호출 실패 문장 (임시) |

## 실행

루트 `.env`에 아래를 넣습니다.

```env
LLM_PROVIDER=anthropic
LLM_MODEL=claude-haiku-5-5
ANTHROPIC_API_KEY=...
```

```bash
python eval/simulate.py                       # 전체 (약 10분)
python eval/simulate.py --only basic,back_step
python eval/simulate.py --timeout 5           # 상담 호출 타임아웃 바꿔 보기 (측정용 기본값: 상담 8, 정리 15, 분류기 5초)
```

- 리포트: `eval/results/sim-*.md` (커밋하지 않음). 시나리오별 채점, 턴마다 AI 응답·서버 이벤트·재요청 사유, 감정 기록, 호출 시간 통계.
- 채점 항목: 위기 판단, 종료 요청, 정리까지 도달, 감정 기록 칸, 요약 한 줄·말랑이 사유, 질문 하나씩, 들어주기, 지시 무시 거절, 되돌아가기, 재요청·폴백 횟수
- `script` 키는 `"단계#순번"` (예: `"STEP_03#2": "모르겠어"` = STEP_03에서 사용자의 두 번째 발화)
