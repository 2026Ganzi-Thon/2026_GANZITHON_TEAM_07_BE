#!/usr/bin/env python3
"""LLM 호출 공통 모듈 (표준 라이브러리만 사용, 프롬프트 측정용).

※ 임의로 작성한 프롬프트 측정 도구입니다. 개발에는 참고하지 마세요. 개발은 팀 결정을 따릅니다.

- .env 읽기, Claude Messages API / OpenAI 호환 API 호출, 구조화 출력(JSON Schema) 변환, 응답 JSON 파싱
- LLM_PROVIDER=anthropic 이면 Claude, 그 외는 OpenAI 호환(Upstage 등)
"""
import json
import os
import re
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROMPT_DIR = ROOT / "src/main/resources/prompts"
EVAL_DIR = ROOT / "eval"
DEFAULT_BASE_URL = "https://api.upstage.ai/v1"


def load_env():
    """루트 .env를 읽는다. 셸 환경변수가 있으면 그쪽이 우선."""
    env = {}
    path = ROOT / ".env"
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            env[key.strip()] = value.strip()
    env.update({k: v for k, v in os.environ.items() if k.startswith("LLM_") or k == "ANTHROPIC_API_KEY"})
    return env


def strip_comments(text):
    return re.sub(r"<!--.*?-->", "", text, flags=re.S).strip()


def call_llm(env, messages, temperature, schema=None, timeout=None):
    """LLM_PROVIDER에 따라 호출한다. anthropic이면 Claude Messages API, 아니면 OpenAI 호환(Upstage 등).
    schema: Claude 구조화 출력에 쓸 JSON Schema(dict). 없으면 v4 output.schema.json. timeout: 초."""
    if (env.get("LLM_PROVIDER") or "").lower() in ("anthropic", "claude"):
        return call_anthropic(env, messages, schema, timeout)
    return call_openai_compatible(env, messages, temperature)


def call_openai_compatible(env, messages, temperature):
    base = env.get("LLM_BASE_URL") or DEFAULT_BASE_URL
    key = env.get("LLM_API_KEY")
    if not key:
        raise RuntimeError("LLM_API_KEY가 비어 있습니다. .env를 확인하세요.")
    payload = {
        "model": env.get("LLM_MODEL") or "solar-pro4",
        "messages": messages,
        "temperature": temperature,
    }
    if env.get("LLM_REASONING_EFFORT"):
        payload["reasoning_effort"] = env["LLM_REASONING_EFFORT"]
    if env.get("LLM_JSON_MODE", "").lower() in ("1", "true", "yes"):
        payload["response_format"] = {"type": "json_object"}
    req = urllib.request.Request(
        base.rstrip("/") + "/chat/completions",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=60) as res:
        data = json.loads(res.read().decode("utf-8"))
    return data["choices"][0]["message"]["content"]


def claude_schema(schema=None):
    """output.schema.json을 Claude 구조화 출력(output_config.format)이 받는 모양으로 바꾼다.
    oneOf → anyOf, 길이·개수 제한 키워드 제거(API가 강제하지 않음), 모든 객체에 additionalProperties: false."""
    if schema is None:
        schema = json.loads((PROMPT_DIR / "emotion-conversation" / "output.schema.json").read_text(encoding="utf-8"))
    drop = {"$schema", "title", "minLength", "maxLength", "minItems", "maxItems", "minimum", "maximum"}

    def walk(node):
        if isinstance(node, dict):
            node = {k: walk(v) for k, v in node.items() if k not in drop}
            if "oneOf" in node:
                node["anyOf"] = node.pop("oneOf")
            if node.get("type") == "object":
                node["additionalProperties"] = False
            return node
        if isinstance(node, list):
            return [walk(v) for v in node]
        return node
    schema = walk(schema)
    # 구조화 출력은 required에 없는 칸을 빼먹을 수 있어서 모든 칸을 필수로 둔다 (프롬프트 v4의 question_stage 포함)
    schema["required"] = list(schema["properties"])
    return schema


def call_anthropic(env, messages, schema=None, timeout=None):
    # Upstage 키와 같이 둘 수 있게 ANTHROPIC_API_KEY를 먼저 본다
    key = env.get("ANTHROPIC_API_KEY") or env.get("LLM_API_KEY")
    if not key:
        raise RuntimeError("ANTHROPIC_API_KEY가 비어 있습니다. .env에 Claude API 키를 넣으세요.")
    system = "\n\n".join(m["content"] for m in messages if m["role"] == "system")
    convo = [{"role": m["role"], "content": m["content"]} for m in messages if m["role"] != "system"]
    payload = {
        "model": env.get("LLM_MODEL") or "claude-haiku-5-5",
        "max_tokens": int(env.get("LLM_MAX_TOKENS") or 4096),  # 생각(thinking) 토큰 포함
        # 프롬프트 캐싱: 시스템 프롬프트(공통+단계, 수천 토큰)를 캐시해 비용·지연을 줄인다
        "system": [{"type": "text", "text": system, "cache_control": {"type": "ephemeral"}}]
        if env.get("LLM_PROMPT_CACHE", "").lower() in ("1", "true", "yes") else system,
        "messages": convo,
        # temperature는 보내지 않는다 (최신 Claude 모델 권장 사항)
    }
    if env.get("LLM_JSON_MODE", "").lower() in ("1", "true", "yes"):
        payload["output_config"] = {"format": {"type": "json_schema", "schema": claude_schema(schema)}}
    req = urllib.request.Request(
        (env.get("LLM_BASE_URL") or "https://api.anthropic.com").rstrip("/") + "/v1/messages",
        data=json.dumps(payload).encode("utf-8"),
        headers={"x-api-key": key, "anthropic-version": "2023-06-01", "content-type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=timeout or 90) as res:
            data = json.loads(res.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        raise RuntimeError(f"Claude API 오류 {exc.code}: {exc.read().decode('utf-8', 'replace')[:500]}") from exc
    # 응답 블록 중 text만 모은다 (thinking 블록은 제외)
    return "".join(b.get("text", "") for b in data.get("content", []) if b.get("type") == "text")


def parse_reply(raw):
    try:
        return json.loads(raw)
    except (json.JSONDecodeError, TypeError):
        pass
    match = re.search(r"\{.*\}", raw or "", flags=re.S)
    if match:
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            return None
    return None
