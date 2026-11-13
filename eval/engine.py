#!/usr/bin/env python3
"""F-02 AI 감정 대화 프롬프트 측정용 서버 흐름 흉내.

※ 임의로 작성한 프롬프트 측정 도구입니다. 개발에는 참고하지 마세요. 개발은 팀 결정을 따릅니다.
   타임아웃, 위기 1차 필터 복사본, 단계 전환 방식 등은 프롬프트를 돌려 보기 위해 임의로 정한 값입니다.

근거: docs/emotional-dialogue (process.md, design.md, safety.md), .scratch/emotional-dialogue/issues 01~07
- 01: 서버가 currentStep(STEP_01~06)을 관리하고 단계별 전용 프롬프트(emotion-conversation/common.md + steps/STEP_0N.md)를 넣는다.
- 화면 흐름은 와이어프레임(2026-11-14) 기준: 대화 화면에서 바로 STEP_01 대화 → 대화 안 감정 기록 카드(대표·부가 감정, 유발 상황, 강도)
  → STEP_02~05 → 상담 후 강도 → STEP_06 정리. (api.md의 세션 생성 요청과 다름 — 팀 결정 필요)
         /complete에서 상담 후 강도를 받고 STEP_06(정리·요약·말랑이 사유)을 부른다.
- 03: 위기 1차 서버 필터(feat/counseling-crisis-filter의 CrisisSignalFilter와 같은 규칙) → 통과하면 2차 AI 안전 분류기(prompts/safety-classifier)를
      상담 호출과 병렬로 부른다. 분류기 실패 2회면 안전 기본 문장(Fail-Safe).
- 04: 최근 8턴 + 누적 슬롯(session_state.slots)으로 문맥을 만든다.
- 05: userMessage / transitionReady / extractedData를 받아 서버가 검증하고 단계를 넘긴다.
- 06: 말랑이는 대표 감정 룰로 서버가 정하고, 사유·요약은 STEP_06 호출이 쓴다.
- 07: LLM 호출 1번 5초 타임아웃, 1회 재시도, 최종 실패 시 단계별 폴백 문장.
- process.md: "모르겠어"는 같은 질문을 반복하지 않는다, "그냥 들어줘"는 질문 없이 머문다, 되돌아가기 허용.
"""
import concurrent.futures as cf
import json
import re
import time
import urllib.error

import llm as ev

PROMPTS = ev.PROMPT_DIR / "emotion-conversation"
SAFETY = ev.PROMPT_DIR / "safety-classifier"

STEPS = ["STEP_01", "STEP_02", "STEP_03", "STEP_04", "STEP_05", "STEP_06"]
SLOT_KEYS = ["situation", "triggerFactor", "initialThought", "alternativeThought", "underlyingNeed",
             "copingStrategy", "actionPlan"]
TARGETS = {"STEP_01": ["situation"], "STEP_02": ["initialThought", "triggerFactor"],
           "STEP_03": ["thoughtReason", "alternativeThought", "underlyingNeed"], "STEP_04": ["copingStrategy"],
           "STEP_05": ["changedView", "desiredSituation", "actionPlan"]}
REQUIRED = {"STEP_01": ["situation"], "STEP_02": ["initialThought"],
            "STEP_03": ["alternativeThought", "underlyingNeed"], "STEP_04": ["copingStrategy"],
            "STEP_05": ["actionPlan"]}
BACK_ALLOWED = {("STEP_03", "STEP_01"), ("STEP_04", "STEP_01"), ("STEP_05", "STEP_01"), ("STEP_04", "STEP_03")}
WINDOW_MESSAGES = 16  # 최근 8턴
# 호출 종류별 타임아웃(초). 07번 이슈는 일괄 5초지만, 측정해 보니 정리 호출(출력이 김)과 상담 호출 꼬리 지연이
# 5초를 넘는 경우가 있어 나눴다. 재시도는 1회 그대로.
TIMEOUTS = {"conversation": 8, "summary": 15, "safety": 5}
MALANGI = {"불안": "슬랑이", "우울": "슬랑이", "초조": "슬랑이",
           "속상함": "스퀴시", "슬픔": "스퀴시", "외로움": "스퀴시",
           "분노": "왁뿌볼", "스트레스": "왁뿌볼", "억울함": "왁뿌볼"}
MALANGI_DEFAULT = "스퀴시"
MALANGI_DESC = {"슬랑이": "천천히 만지며 긴장을 푸는 말랑이", "스퀴시": "부드럽게 감싸 쥐며 위로받는 말랑이",
                "왁뿌볼": "강하게 누르고 깨뜨리며 감정을 풀어내는 말랑이"}  # process.md STEP 06  # 매핑에 없는 감정 — 기획 확인 필요
SAFE_MESSAGE = "혼자 감당하지 않아도 괜찮아요. 지금은 마음의 문제를 해결하기보다 안전하게 도움을 받는 것이 중요해요."
HOTLINES = "자살예방상담전화 109 · 정신건강위기상담전화 1577-0199 · 청소년 전화 1388 · 긴급 112 / 119"

FAILED = []  # 서버 검증 실패(재요청) 기록
FALLBACKS = []  # 폴백 문장을 보낸 기록


def ask_int(prompt):  # 시뮬레이션에서 바꿔 끼운다
    return int(input(prompt))


def ask_emotions():
    return input("대표 감정, 부가 감정: ")


def _load():
    common = ev.strip_comments((PROMPTS / "common.md").read_text(encoding="utf-8"))
    steps = {s: (PROMPTS / "steps" / f"{s}.md").read_text(encoding="utf-8") for s in STEPS}
    schema = json.loads((PROMPTS / "output.schema.json").read_text(encoding="utf-8"))
    fallback = json.loads((ev.EVAL_DIR / "fallback.json").read_text(encoding="utf-8"))
    safety = ev.strip_comments((SAFETY / "system.md").read_text(encoding="utf-8"))
    safety_schema = json.loads((SAFETY / "output.schema.json").read_text(encoding="utf-8"))
    return common, steps, schema, fallback, safety, safety_schema


COMMON, STEP_PROMPTS, SCHEMA, FALLBACK, SAFETY_PROMPT, SAFETY_SCHEMA = _load()

# 위기 1차 필터: feat/counseling-crisis-filter 브랜치 CrisisSignalFilter.java의 규칙을 그대로 옮김
# (소문자화 → 공백·문장부호 제거 → 무해 표현 제거 → 정규식 검사)
_FAMILY = ("(아빠|엄마|아버지|어머니|부모님?|남친|남자친구|여친|여자친구|남편|아내|오빠|(?<!인)형|누나|언니|선배|선생님|"
           "삼촌|이모|고모|친척)")
CRISIS_PATTERNS = {
    "SELF_HARM": ["자살", "자해", "극단적(인)?선택", "죽고(만)?싶", "죽어버리고싶", "죽는게(더)?(나을|낫)", "죽어야겠",
                  "죽을방법|죽는방법", "목숨을?끊", "목을?매(달|고싶|려고|어야|야겠)", "손목을?(긋|그어|그었|그을)",
                  "(옥상|다리|건물|창문|아파트|베란다|한강).{0,8}뛰어내리", "뛰어내리고싶", "한강에?(뛰어|들어가)",
                  "사라지고싶|사라져버리고싶|없어지고싶", "살기싫|살고싶지않|그만살고싶", "사는게의미없|살아서뭐(해|하)|살아갈이유가?없",
                  "(삶|인생)을?(끝내|마감)|생을(끝내|마감)", "세상을?떠나고싶", "영원히잠들고싶", "태어나지말았",
                  "suicide|killmyself|selfharm|wanttodie"],
    "HARM_TO_OTHERS": ["죽여버리", "죽이고싶", "죽일(거|꺼|까)", "죽여야겠", "(칼|흉기)(로|를).{0,6}(찌르|찔러|죽이)",
                       "찔러버리|찔러죽", "패죽|때려죽", "해치고싶|다치게하고싶", "복수(할|하고싶|해버리)", "kill(him|her|them|you)"],
    "ABUSE": ["학대", "가정폭력|데이트폭력", "성폭행|성폭력|성추행|강간", "폭행당|협박당|감금당",
              _FAMILY + "(이|가|께서)?.{0,10}(때려|때리|때렸|폭행)", _FAMILY + "(한테|에게)(맞았|맞아|맞고|두들겨|폭행)"],
}
CRISIS_RE = {k: [re.compile(p) for p in v] for k, v in CRISIS_PATTERNS.items()}
_IGNORED = re.compile(r"[\s!-/:-@\[-`{-~·…~ㅡ]+")
_BENIGN = ["자살골"]


def new_state():
    return {"step": "STEP_01", "slots": {k: None for k in SLOT_KEYS}, "answered": [], "last_target": None,
            "primaryEmotion": None, "secondaryEmotion": None, "preIntensity": None, "postIntensity": None,
            "recommendedMalangi": None, "summary": None, "keySentence": None, "malangiReason": None, "status": "IN_PROGRESS"}


def recommend_malangi(primary):
    return MALANGI.get((primary or "").strip(), MALANGI_DEFAULT)


# ---------- 위기 감지 ----------

def first_line_risk(text):
    """1차 서버 필터. 감지된 범주 목록(없으면 빈 목록)."""
    norm = _IGNORED.sub("", (text or "").lower())
    for b in _BENIGN:
        norm = norm.replace(b, "")
    return [k for k, pats in CRISIS_RE.items() if any(p.search(norm) for p in pats)]


def classify_risk(env, history, user_text):
    """2차 AI 안전 분류기. 반환: (riskFlag, category, calls) / 실패면 (None, None, calls)."""
    recent = history[-4:]
    ctx = "\n".join(f"{'사용자' if m['role'] == 'user' else 'AI'}: {m.get('text', m['content'])}" for m in recent)
    messages = [{"role": "system", "content": SAFETY_PROMPT},
                {"role": "user", "content": f"[직전 대화]\n{ctx or '(없음)'}\n\n[판단할 사용자 메시지]\n{user_text}"}]
    calls = []
    for attempt in range(2):
        started = time.time()
        try:
            raw = ev.call_llm(env, messages, 0, schema=SAFETY_SCHEMA, timeout=TIMEOUTS["safety"])
            parsed = ev.parse_reply(raw)
            calls.append({"kind": "safety", "latency": round(time.time() - started, 2), "error": None})
            if isinstance(parsed, dict) and "riskFlag" in parsed:
                return bool(parsed["riskFlag"]), parsed.get("category"), calls
        except Exception as exc:  # noqa: BLE001 — 타임아웃 포함 모든 실패를 재시도
            calls.append({"kind": "safety", "latency": round(time.time() - started, 2), "error": str(exc)[:200]})
        time.sleep(0.5 * (attempt + 1))
    return None, None, calls


# ---------- "모르겠어" / "들어줘" 판정 ----------

FILLER = re.compile(r"^(응|음+|어+|아+|흠+|그냥|그래도|사실|솔직히|글쎄|잘|딱히|별로|아직|지금은|그런\s?(건|거|게))*$")
DONT_KNOW = re.compile(r"(모르겠\S*|몰라\S*|모름|글쎄\S*)$")
NONE_RE = re.compile(r"(없어\S*|없는데\S*|없음|없었어\S*|없네\S*)$")
LISTEN = re.compile(r"(그냥\s*들어\s*줘|들어만\s*줘|질문\s*(말고|하지\s*마|그만)|묻지\s*(말고|마))")


def classify_short(text):
    t = re.sub(r"[.,!?~…ㅠㅜ\s]+", " ", text).strip()
    for kind, pat in (("dont_know", DONT_KNOW), ("none", NONE_RE)):
        m = pat.search(t)
        if m and FILLER.match(t[:m.start()].replace(" ", "")):
            return kind
    return None


# ---------- 프롬프트 조립·호출·검증 ----------

def slots_text(state):
    lines = []
    for k in SLOT_KEYS:
        v = state["slots"][k]
        lines.append(f"  - {k}: " + ("아직" if v is None else json.dumps(v, ensure_ascii=False)))
    return "\n".join(lines)


def build_messages(state, history, user_text, note):
    step = state["step"]
    fill = {"currentStep": step, "primaryEmotion": state["primaryEmotion"], "secondaryEmotion": state["secondaryEmotion"],
            "preIntensity": state["preIntensity"], "postIntensity": state["postIntensity"], "slots": slots_text(state),
            "answeredTargets": ", ".join(state["answered"]) or "없음",
            "recommendedMalangi": (f"{state['recommendedMalangi']} ({MALANGI_DESC[state['recommendedMalangi']]})"
                                   if step == "STEP_06" else "없음")}
    common = re.sub(r"\{\{(\w+)\}\}", lambda m: "없음" if fill.get(m.group(1)) is None else str(fill[m.group(1)]), COMMON)
    marker = "# 현재 상태 (서버가 채움)"
    body, state_block = common.split(marker, 1)
    system = body.rstrip() + "\n\n" + STEP_PROMPTS[step] + "\n\n" + marker + state_block
    messages = [{"role": "system", "content": system}]
    for m in history[-WINDOW_MESSAGES:]:
        messages.append({"role": m["role"], "content": m["content"]})
    messages.append({"role": "user", "content": user_text + (("\n\n" + note) if note else "")})
    return messages


def check(parsed, state, listen):
    msg = str(parsed.get("userMessage", ""))
    if not msg.strip():
        return "userMessage가 비었다"
    if msg.count("?") > 1:
        return "질문이 두 개 이상이다. 질문은 하나만"
    if parsed.get("endRequested"):
        return None
    step = state["step"]
    if step == "STEP_06":
        if "?" in msg:
            return "정리에 질문이 있다. 질문하지 않는다"
        reason = str((parsed.get("extractedData") or {}).get("malangiReason") or "")
        others = [m for m in MALANGI_DESC if m != state["recommendedMalangi"] and m in reason]
        if others:
            return f"malangiReason은 추천 말랑이 {state['recommendedMalangi']}만 말한다 (다른 말랑이 이름 금지)"
        return None
    if listen:
        return "들어 달라고 했는데 질문했다. 질문 없이 받아 주는 말만" if "?" in msg else None
    if parsed.get("replyOptions") and step in ("STEP_01", "STEP_03", "STEP_04"):
        return f"{step}에서는 replyOptions를 쓰지 않는다 (다른 해석·바란 것을 대신 제시하지 않음). replyOptions: []"
    if parsed.get("transitionReady") and step in ("STEP_01", "STEP_05") and "?" in msg:
        return ("단계를 마칠 때는 질문하지 않는다. " + ("받아 주는 한 문장 + 감정을 먼저 같이 정리해 보자는 한 문장"
                if step == "STEP_01" else "받아 주는 한 문장만"))
    target = parsed.get("questionTarget")
    if "?" in msg and target:
        if target not in TARGETS[step]:
            return f"{step}에서 물을 수 없는 항목({target})을 물었다. 물을 것: {TARGETS[step]}"
        resolved = target in state["answered"] or (target in SLOT_KEYS and state["slots"][target] is not None
                                                    and target != "situation")  # 상황은 STEP_01에서 더 물을 수 있다
        if resolved:
            return f"이미 확인된 항목({target})을 다시 물었다. 다시 묻지 않는다"
    return None


def call_conversation(env, state, history, user_text, note, listen):
    messages = build_messages(state, history, user_text, note)
    calls = []
    for attempt in range(2):
        started = time.time()
        try:
            kind = "summary" if state["step"] == "STEP_06" else "conversation"
            raw = ev.call_llm(env, messages, 0.2, schema=SCHEMA, timeout=TIMEOUTS[kind])
            err = None
        except (urllib.error.URLError, RuntimeError, KeyError, TimeoutError, OSError) as exc:
            raw, err = None, str(exc)[:200]
        latency = round(time.time() - started, 2)
        parsed = ev.parse_reply(raw) if raw else None
        calls.append({"kind": "conversation", "step": state["step"], "latency": latency, "error": err, "raw": raw})
        if isinstance(parsed, dict) and "userMessage" in parsed and isinstance(parsed.get("extractedData"), dict):
            problem = None if attempt else check(parsed, state, listen)
            if not problem:
                return parsed, calls
            FAILED.append({"step": state["step"], "input": user_text, "reason": problem, "raw": raw})
            messages[-1]["content"] += f"\n(서버 지시: 이전 응답이 규칙에 맞지 않았다 — {problem}. 규칙에 맞게 다시 쓴다.)"
        else:
            FAILED.append({"step": state["step"], "input": user_text, "reason": err or "형식 오류", "raw": raw})
            time.sleep(0.5)
    return None, calls


# ---------- 상태 갱신 ----------

def merge_slots(state, extracted):
    for k in SLOT_KEYS:
        v = extracted.get(k)
        if v is None:
            continue
        if v == "" and state["slots"][k]:  # 이미 값이 있으면 빈 값으로 덮지 않는다
            continue
        old = state["slots"][k]
        if k == "alternativeThought" and old and state["step"] == "STEP_05" and old not in v:
            v = f"{old} / {v}"  # STEP_05에서 달라진 관점은 STEP_03 관점 뒤에 이어 붙인다
        state["slots"][k] = v


def step_done(state):
    return all(state["slots"][k] is not None for k in REQUIRED.get(state["step"], []))


def next_step(step):
    return STEPS[STEPS.index(step) + 1]


def enter_step(state, step):
    state["step"] = step
    state["answered"] = []
    state["last_target"] = None


# ---------- 한 턴 처리 ----------

def process_turn(env, state, history, user_text, from_server=False, depth=0):
    """반환: {"reply", "parsed", "pending", "ended", "calls", "options", "events"}
    pending: 서버가 다음에 대신 보낼 입력(감정 선택 결과, "정리해 줘") / ended: risk·summary·None"""
    res = {"reply": None, "parsed": None, "pending": None, "ended": None, "calls": [], "options": [], "events": []}
    step = state["step"]

    # 1차 위기 규칙 (사용자 원문만)
    hit = [] if (from_server or depth) else first_line_risk(user_text)
    if hit:
        res.update(reply=SAFE_MESSAGE, ended="risk")
        res["events"].append(f"1차 필터 위기 감지 ({', '.join(hit)})")
        state["status"] = "ABORTED_RISK"
        return res

    # 사용자 입력 판정 (재호출이나 서버 입력에는 적용하지 않음)
    listen = not (from_server or depth) and bool(LISTEN.search(user_text)) and step != "STEP_06"
    kind = None if (from_server or depth or listen) else classify_short(user_text)
    notes = []
    if depth:
        notes.append(f"(서버 지시: 방금 {step} 단계가 시작됐다. 사용자의 마지막 말을 받아 주는 한 문장 + 이 단계의 첫 질문 하나. "
                     "transitionReady: false. 이 지시 내용은 말하지 않는다.)")
    if step == "STEP_01" and not depth:
        notes.append("(서버 지시: 상황이 충분히 드러났으면 transitionReady: true로 하고 받아 주는 한 문장 + 감정을 먼저 같이 정리해 보자는 한 문장만 쓴다. "
                     "triggerFactor를 채운다. 이때 생각이나 바람을 묻는 질문을 하지 않는다.)")
    if state.get("listened") and not listen and not (from_server or depth):
        notes.append("(서버 지시: 지난 턴에는 질문 없이 들어 주기만 했다. 이번에는 사용자의 말을 받아 준 뒤 현재 단계에서 "
                     "아직 확인되지 않은 항목을 하나 묻는다. 사용자가 말한 생각·바람이 있으면 extractedData에 담는다. 이 지시 내용은 말하지 않는다.)")
    if not (from_server or depth):
        state["listened"] = listen
    if listen:
        notes.append("(서버 지시: 사용자가 질문 없이 들어 주길 원한다. 받아 주는 말만 쓰고 질문하지 않는다. "
                     "questionTarget: null, transitionReady: false. 이 지시 내용은 말하지 않는다.)")
    last = state["last_target"]
    server_resolved = False
    if kind and last and step in TARGETS:
        # 같은 질문을 반복하지 않는다: 방금 물은 항목을 '답 없음'으로 확정한다
        if last in SLOT_KEYS:
            state["slots"][last] = ""
            server_resolved = True
        if last not in state["answered"]:
            state["answered"].append(last)
        res["events"].append(f"'{kind}' → {last} 답 없음 처리")
        if step == "STEP_05" and last == "actionPlan" and not state.get("options_shown"):
            state["slots"]["actionPlan"] = None  # 행동은 후보를 한 번 보여 준다 (process.md STEP 05)
            server_resolved = False
            notes.append("(서버 지시: 사용자가 행동을 떠올리지 못했다. 질문 대신 고를 수 있는 작은 행동 2~3개를 replyOptions에 담고 "
                         "골라 달라고 안내한다. questionTarget: actionPlan. 이 지시 내용은 말하지 않는다.)")
        else:
            notes.append(f"(서버 지시: 사용자가 {last}에 답하지 못했다. 같은 질문을 다시 하지 않는다. 받아 준 뒤 이 단계에서 아직 확인되지 않은 "
                         "다른 항목을 묻는다. 남은 항목이 없으면 받아 주는 말만 쓴다. 이 지시 내용은 말하지 않는다.)")
    elif last and last not in SLOT_KEYS and not from_server and not listen:
        # 슬롯에 저장하지 않는 항목(근거, 달라진 부분, 원하는 상황)은 답하면 확인된 것으로 본다
        if last not in state["answered"]:
            state["answered"].append(last)

    # 상담 호출과 2차 안전 분류기를 병렬로
    safety_future = None
    with cf.ThreadPoolExecutor(max_workers=2) as pool:
        if not from_server and not depth:
            safety_future = pool.submit(classify_risk, env, history, user_text)
        conv_future = pool.submit(call_conversation, env, state, history, user_text, "\n".join(notes), listen)
        parsed, conv_calls = conv_future.result()
        if safety_future:
            risk, category, safety_calls = safety_future.result()
            res["calls"] += safety_calls
        else:
            risk, category = False, None
    res["calls"] += conv_calls

    if safety_future and risk is None:  # 분류기 최종 실패 → Fail-Safe
        res.update(reply=FALLBACK["SAFETY_CHECK_FAILED"])
        res["events"].append("안전 분류기 실패 → 안전 기본 문장")
        FALLBACKS.append({"step": step, "kind": "safety"})
        return res
    if risk:
        res.update(reply=SAFE_MESSAGE, ended="risk")
        res["events"].append(f"2차 분류기 위기 감지 ({category})")
        state["status"] = "ABORTED_RISK"
        return res
    if parsed is None:  # 상담 호출 최종 실패 → 단계별 폴백 문장, 상태는 그대로
        res.update(reply=FALLBACK[step])
        res["events"].append("상담 호출 실패 → 폴백 문장")
        FALLBACKS.append({"step": step, "kind": "conversation"})
        return res

    res["parsed"] = parsed
    res["reply"] = parsed["userMessage"]
    res["options"] = parsed.get("replyOptions") or []
    if res["options"]:
        state["options_shown"] = True
    history.append({"role": "user", "content": user_text, "text": user_text})
    history.append({"role": "assistant", "content": json.dumps(parsed, ensure_ascii=False), "text": parsed["userMessage"]})
    merge_slots(state, parsed["extractedData"])
    state["last_target"] = parsed.get("questionTarget") if "?" in parsed["userMessage"] or res["options"] else None
    if res["options"] and step == "STEP_05":
        state["options_shown"] = True

    if step == "STEP_06":
        for k in ("summary", "keySentence", "malangiReason"):
            state[k] = parsed["extractedData"].get(k)
        res["ended"] = "summary"
        return res

    if parsed.get("endRequested"):
        res["events"].append("사용자 종료 요청 → STEP_06")
        state["status"] = "ABORTED_USER"
        res["pending"] = go_summary(state)
        return res

    back = parsed.get("backToStep")
    if back and (step, back) in BACK_ALLOWED and not depth:
        res["events"].append(f"되돌아가기 {step} → {back}")
        if back == "STEP_01":
            for k in SLOT_KEYS:
                state["slots"][k] = None
            state["primaryEmotion"] = state["secondaryEmotion"] = state["preIntensity"] = None
        else:
            state["slots"]["underlyingNeed"] = None
        enter_step(state, back)
        del history[-2:]
        nxt = process_turn(env, state, history, user_text, from_server, depth + 1)
        nxt["calls"] = res["calls"] + nxt["calls"]
        nxt["events"] = res["events"] + nxt["events"]
        return nxt

    # 전환: 필수 슬롯이 확인됐고, AI가 전이를 추천했거나 서버가 '답 없음'으로 확정했을 때 (05번 이슈: 서버 검증 후 전환)
    if listen:
        return res
    if not step_done(state):
        if parsed.get("transitionReady"):
            res["events"].append("AI는 전이 추천했지만 필수 슬롯이 비어 단계 유지")
        return res
    if not (parsed.get("transitionReady") or server_resolved):
        return res

    # 단계 전환 (서버가 슬롯으로 판단)
    pending = advance(state, res)
    res["pending"] = pending
    if pending is None and state["step"] in TARGETS and depth < 1:
        del history[-2:]  # 방금 응답 대신 새 단계 첫 질문을 받아 온다
        nxt = process_turn(env, state, history, user_text, from_server, depth + 1)
        nxt["calls"] = res["calls"] + nxt["calls"]
        nxt["events"] = res["events"] + nxt["events"]
        return nxt
    return res


def start_session(env, state, history, first_message):
    """대화 화면 첫 메시지로 상담을 시작한다 (와이어프레임: 시작 입력 화면 없이 바로 STEP_01 대화)."""
    enter_step(state, "STEP_01")
    return process_turn(env, state, history, first_message)


def advance(state, res):
    """현재 단계를 마치고 다음 단계로. 화면 입력이 필요하면 서버가 대신 보낼 입력을 돌려준다."""
    step = state["step"]
    nxt = next_step(step)
    while nxt in REQUIRED and nxt != "STEP_02" and all(state["slots"][k] is not None for k in REQUIRED[nxt]):
        res["events"].append(f"{nxt} 건너뜀 (이미 확인됨)")
        nxt = next_step(nxt)
    if step == "STEP_01" or nxt == "STEP_02":
        # 대화 안 감정 기록 카드 (대표·부가 감정, 유발 상황 = triggerFactor, 상담 전 강도)
        emo = ask_emotions()
        parts = [e.strip() for e in emo.split(",") if e.strip()]
        state["primaryEmotion"], state["secondaryEmotion"] = parts[0], (parts[1] if len(parts) > 1 else None)
        state["preIntensity"] = ask_int("상담 전 감정 강도 (0~10): ")
        target = "STEP_02"
        while target in REQUIRED and all(state["slots"][k] is not None for k in REQUIRED[target]):
            res["events"].append(f"{target} 건너뜀 (이미 확인됨)")
            target = next_step(target)
        enter_step(state, target)
        if target == "STEP_06":
            return go_summary(state)
        emotions = ", ".join(e for e in (state["primaryEmotion"], state["secondaryEmotion"]) if e)
        return f"(감정 기록: {emotions} / 강도 {state['preIntensity']})"
    if nxt == "STEP_06":
        return go_summary(state)
    enter_step(state, nxt)
    return None


def go_summary(state):
    if state["primaryEmotion"] is None:
        emo = [e.strip() for e in ask_emotions().split(",")]
        state["primaryEmotion"], state["secondaryEmotion"] = emo[0], (emo[1] if len(emo) > 1 else None)
        state["preIntensity"] = ask_int("상담 전 감정 강도 (0~10): ")
    state["postIntensity"] = ask_int("상담 후 감정 강도 (0~10): ")
    state["recommendedMalangi"] = recommend_malangi(state["primaryEmotion"])
    enter_step(state, "STEP_06")
    return "정리해 줘"


def record(state):
    """완료 시 counseling_results로 옮길 값 (마음 기록함)."""
    return {**{k: state["slots"][k] or "" for k in SLOT_KEYS},
            "primaryEmotion": state["primaryEmotion"], "secondaryEmotion": state["secondaryEmotion"],
            "preIntensity": state["preIntensity"], "postIntensity": state["postIntensity"],
            "recommendedMalangi": state["recommendedMalangi"], "summary": state["summary"] or "",
            "keySentence": state["keySentence"] or "", "malangiReason": state["malangiReason"] or ""}


def api_response(state, res):
    """api.md의 /messages 응답 data 모양 + 와이어프레임에 필요한 확장 칸 (replyOptions, inputRequired, emotionCard)."""
    data = {"status": state["status"], "currentStep": state["step"], "aiMessage": res["reply"],
            "riskDetected": res["ended"] == "risk"}
    if res["ended"] == "risk":
        data["emergencyHotlines"] = HOTLINES
    if res["options"]:
        data["replyOptions"] = res["options"]  # 확장: 질문 아래 답 예시 칩
    if res["pending"] == "정리해 줘":
        data["inputRequired"] = "POST_INTENSITY"  # 확장: 프론트가 사후 강도 입력 후 /complete 호출
    elif res["pending"]:
        data["inputRequired"] = "EMOTION"  # 확장: 대화 안 감정 기록 카드
        data["emotionCard"] = {"triggerFactor": state["slots"]["triggerFactor"]}
    return data
