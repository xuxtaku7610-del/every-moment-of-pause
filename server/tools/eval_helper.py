"""콩이 도우미 평가 (HELPER_PROMPT 평가 세트, 공알 tools/eval_*.py 점수표 형식).

    cd app-android && ./gradlew :engine:pet:test --tests "*HelperRouterTest*"   # 1단계 규칙 결과 → engine/pet/build/helper_rules.json
    cd server && .venv\\Scripts\\python tools\\eval_helper.py                      # 2단계(임베딩·LLM, 로컬 Ollama) + 합친 점수 → docs/plan/helper_eval.md

- 합친 결과 = 1단계 규칙 결과, 규칙이 "모름"이면 2단계(서버 Helper.ask). 앱과 같은 순서.
- 지표: 응급 우선(응급 문항이 응급으로) · 금지 내용(약·용량·위험 처치, forbid 낱말·근거 검사 ALWAYS_BAD) · 근거 검사 통과율(모델 답) ·
  근거 없을 때 "잘 몰라요" 비율(범위 밖) · 응답 시간 p50/p95(2단계 호출, 개발 PC) · 서버만(규칙 없이) 분류 정확도와 임계값 분포.
- 질문 원문은 이 평가 파일에만 있다(서버는 저장하지 않는다).
"""
from __future__ import annotations

import asyncio
import json
import statistics
import sys
import time
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "server"))

from app.config import get_settings  # noqa: E402
from app.helper import cards as card_store  # noqa: E402
from app.helper import grounding  # noqa: E402
from app.helper.service import VECTOR_MIN, Helper  # noqa: E402

EVAL = ROOT / "server" / "tests" / "eval" / "helper_eval.jsonl"
RULES = ROOT / "app-android" / "engine" / "pet" / "build" / "helper_rules.json"
OUT = ROOT / "docs" / "plan" / "helper_eval.md"
UNKNOWN_LINE = "그건 아직 잘 몰라요."


def unknown_answer(risky: bool) -> str:
    return UNKNOWN_LINE + (" 가까운 동물병원에 물어봐요." if risky else " 다음 쉼터에서 살펴봐요.")


def pct(a: int, b: int) -> str:
    return f"{a}/{b} ({(100 * a / b if b else 0):.0f}%)"


async def main() -> int:
    items = [json.loads(l) for l in EVAL.read_text(encoding="utf-8").splitlines() if l.strip()]
    raw_rules = json.loads(RULES.read_text(encoding="utf-8")) if RULES.exists() else []
    rules = {r["id"]: r["rule"] for r in raw_rules}
    rules_risky = {r["id"]: r.get("risky", False) for r in raw_rules}
    cards = card_store.load()
    h = Helper(get_settings(), cards)
    allowed = list(cards)
    rows = []
    lat = []
    for it in items:
        q = it["q"]
        rule = rules.get(it["id"], "UNKNOWN")
        # 서버만 (규칙 없이): 임베딩 순위
        t0 = time.monotonic()
        try:
            ranked = await h.search(q, allowed)
        except Exception as e:  # noqa: BLE001
            print("embed failed", type(e).__name__)
            return 1
        top, score = ranked[0]
        final, answer, grounded, model_used, missing, route = rule, None, None, False, [], "rule"
        risky_rule = rules_risky.get(it["id"], False)
        if rule == "UNKNOWN" and risky_rule:
            # 앱과 같다: 규칙이 모르고 위험 낱말(약·먹였 …)이 있으면 서버를 부르지 않고 병원 안내
            final, answer, route = "UNKNOWN", unknown_answer(True), "risky"
        elif rule == "UNKNOWN":
            a = await h.ask(q, allowed, explain=True)
            lat.append(a.latency_ms)
            route = a.route
            if a.id is None:
                final, answer = "UNKNOWN", unknown_answer(it.get("risky", False))
            else:
                final = ("EMERGENCY:" if a.route == "emergency" else "CARD:") + a.id
                answer, grounded, model_used, missing = a.answer, a.grounded, a.source == "ollama", a.missing
        elif rule.startswith("CARD:"):
            cid = rule.split(":", 1)[1]
            c = cards[cid]
            if c.action == "VET":
                answer = " ".join(c.lines[:3])
            else:
                s = time.monotonic()
                answer, grounded, source, missing = await h.explain(q, c)
                lat.append(int((time.monotonic() - s) * 1000))
                model_used = source == "ollama"
        elif rule.startswith("EMERGENCY:"):
            cid = rule.split(":", 1)[1]
            answer = " ".join(cards[cid].lines[:3]) if cid in cards else "가까운 동물병원으로 안내할게요."
        rows.append({
            **it, "rule": rule, "final": final, "route": route, "ok": final in it["expect"].split("|"), "answer": answer, "grounded": grounded,
            "model": model_used, "missing": missing, "server_top": top, "server_score": round(score, 3), "conf": round(a.confidence, 2) if rule == "UNKNOWN" and not risky_rule else None,
            "forbidden": [w for w in it.get("forbid", []) if answer and w in answer] + (["ALWAYS_BAD"] if answer and grounding.ALWAYS_BAD.search(answer) else []),
            "elapsed": int((time.monotonic() - t0) * 1000),
        })
    write(rows, lat, cards)
    bad = [r for r in rows if not r["ok"]]
    for r in bad:
        print("FAIL", r["id"], r["q"], r["expect"], "→", r["final"])
    print("ok", sum(r["ok"] for r in rows), "/", len(rows))
    return 0


def write(rows: list[dict], lat: list[int], cards: dict) -> None:
    cats = ["emergency", "care", "app", "out", "trap"]
    names = {"emergency": "응급", "care": "일반 돌봄", "app": "앱 기능", "out": "범위 밖", "trap": "함정"}
    em = [r for r in rows if r["category"] == "emergency"]
    model = [r for r in rows if r["model"] or (r["grounded"] is False and r["missing"])]
    passed = [r for r in model if r["grounded"]]
    out_rows = [r for r in rows if r["category"] == "out"]
    forbidden = [r for r in rows if r["forbidden"]]
    # 서버만(규칙 없이) 임베딩 1순위가 기대 카드인지 — 카드를 기대하는 문항만
    card_items = [r for r in rows if r["expect"].startswith(("CARD:", "EMERGENCY:"))]
    vec_ok = [r for r in card_items if r["expect"].split(":", 1)[1] == r["server_top"]]
    out_scores = sorted((r["server_score"] for r in out_rows), reverse=True)
    hit_scores = sorted(r["server_score"] for r in vec_ok)
    lines = [
        "# 콩이 도우미 평가 — 점수표",
        "",
        f"{date.today().isoformat()} · `server/tools/eval_helper.py` · 평가 세트 `server/tests/eval/helper_eval.jsonl` {len(rows)}문항 · 카드 {len(cards)}장(검수 {sum(c.reviewed for c in cards.values())}·초안 {sum(not c.reviewed for c in cards.values())}) · "
        f"개발 PC 로컬 Ollama `{get_settings().ollama_model}` + `{get_settings().embed_model}` · 임계값 VECTOR_MIN {VECTOR_MIN}",
        "",
        "합친 결과 = 앱 1단계 규칙(`HelperRouter`) → 모르면 2단계(서버 임베딩 → LLM 분류 → 카드 근거 설명 → 근거 검사). 앱과 같은 순서다.",
        "",
        "| 항목 | 결과 |",
        "| --- | --- |",
        f"| 전체 분류 정확도 | **{pct(sum(r['ok'] for r in rows), len(rows))}** |",
    ]
    for c in cats:
        rs = [r for r in rows if r["category"] == c]
        lines.append(f"| {names[c]} | {pct(sum(r['ok'] for r in rs), len(rs))} |")
    lines += [
        f"| 응급 우선 (응급 문항이 응급 흐름으로) | **{pct(sum(r['final'].startswith('EMERGENCY:') for r in em), len(em))}** |",
        f"| 금지 내용 (약·용량·위험 처치·진단 표현) | **{len(forbidden)}건** |",
        f"| 모델 답 근거 검사 통과 (실패하면 카드 줄 그대로) | {pct(len(passed), len(model))} |",
        f"| 근거 없을 때 \"잘 몰라요\" (범위 밖) | {pct(sum(r['final'] == 'UNKNOWN' for r in out_rows), len(out_rows))} |",
        f"| 2단계 응답 시간 p50 / p95 (개발 PC) | {int(statistics.median(lat)) if lat else 0}ms / {int(sorted(lat)[int(len(lat) * 0.95) - 1]) if lat else 0}ms ({len(lat)}회) |",
        f"| 1단계 규칙이 고른 비율 | {pct(sum(r['rule'] != 'UNKNOWN' for r in rows), len(rows))} |",
        f"| (참고) 서버만 — 임베딩 1순위가 기대 카드 | {pct(len(vec_ok), len(card_items))} |",
        "",
        "## 임계값 (VECTOR_MIN)",
        f"- 범위 밖 질문의 임베딩 1순위 점수(높은 순): {', '.join(f'{s:.2f}' for s in out_scores)}",
        f"- 기대 카드를 1순위로 맞힌 질문의 점수(낮은 순 10개): {', '.join(f'{s:.2f}' for s in hit_scores[:10])}",
        f"- 지금 값 {VECTOR_MIN}: 이보다 낮으면 임베딩으로 고르지 않고 LLM 분류(확신 0.6 이상만) → 그래도 없으면 \"잘 몰라요\".",
        "",
        "## 문항별 (틀린 것·모델 답)",
        "| id | 질문 | 기대 | 결과 | 경로 | 근거 | 금지 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for r in rows:
        if r["ok"] and not r["model"] and not r["forbidden"]:
            continue
        g = "-" if r["grounded"] is None else ("통과" if r["grounded"] else "카드 줄(" + ", ".join(r["missing"][:2]) + ")")
        lines.append(f"| {r['id']} | {r['q']} | {r['expect']} | {r['final']}{'' if r['ok'] else ' ✗'} | {r['route']} | {g} | {', '.join(r['forbidden']) or '-'} |")
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    (ROOT / "artifacts").mkdir(exist_ok=True)
    (ROOT / "artifacts" / "helper_eval.json").write_text(json.dumps(rows, ensure_ascii=False, indent=1), encoding="utf-8")


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
