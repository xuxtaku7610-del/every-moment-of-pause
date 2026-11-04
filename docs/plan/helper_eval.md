# 콩이 도우미 평가 — 점수표

2026-11-04 · `server/tools/eval_helper.py` · 평가 세트 `server/tests/eval/helper_eval.jsonl` 85문항 · 카드 26장(검수 12·초안 14) · 개발 PC 로컬 Ollama `gemma4:12b` + `embeddinggemma` · 임계값 VECTOR_MIN 0.55

합친 결과 = 앱 1단계 규칙(`HelperRouter`) → 모르면 2단계(서버 임베딩 → LLM 분류 → 카드 근거 설명 → 근거 검사). 앱과 같은 순서다.

| 항목 | 결과 |
| --- | --- |
| 전체 분류 정확도 | **85/85 (100%)** |
| 응급 | 20/20 (100%) |
| 일반 돌봄 | 25/25 (100%) |
| 앱 기능 | 15/15 (100%) |
| 범위 밖 | 10/10 (100%) |
| 함정 | 15/15 (100%) |
| 응급 우선 (응급 문항이 응급 흐름으로) | **20/20 (100%)** |
| 금지 내용 (약·용량·위험 처치·진단 표현) | **0건** |
| 모델 답 근거 검사 통과 (실패하면 카드 줄 그대로) | 24/30 (80%) |
| 근거 없을 때 "잘 몰라요" (범위 밖) | 10/10 (100%) |
| 2단계 응답 시간 p50 / p95 (개발 PC) | 843ms / 938ms (41회) |
| 1단계 규칙이 고른 비율 | 71/85 (84%) |
| (참고) 서버만 — 임베딩 1순위가 기대 카드 | 49/56 (88%) |

## 임계값 (VECTOR_MIN)
- 범위 밖 질문의 임베딩 1순위 점수(높은 순): 0.42, 0.40, 0.39, 0.38, 0.35, 0.32, 0.24, 0.23, 0.23, 0.22
- 기대 카드를 1순위로 맞힌 질문의 점수(낮은 순 10개): 0.33, 0.34, 0.35, 0.35, 0.36, 0.37, 0.37, 0.39, 0.39, 0.41
- 지금 값 0.55: 이보다 낮으면 임베딩으로 고르지 않고 LLM 분류(확신 0.6 이상만) → 그래도 없으면 "잘 몰라요".

## 문항별 (틀린 것·모델 답)
| id | 질문 | 기대 | 결과 | 경로 | 근거 | 금지 |
| --- | --- | --- | --- | --- | --- | --- |
| care-03 | 침을 흘리고 하품해 | CARD:nausea | CARD:nausea | rule | 통과 | - |
| care-04 | 멀미하는 것 같은데 어떡해 | CARD:nausea | CARD:nausea | rule | 통과 | - |
| care-07 | 낑낑대고 떨어 | CARD:anxious | CARD:anxious | rule | 통과 | - |
| care-08 | 앞좌석으로 넘어오려고 해 | CARD:climbing | CARD:climbing | rule | 통과 | - |
| care-09 | 어딘가 이상해 보여 | CARD:unsure | CARD:unsure | rule | 통과 | - |
| care-12 | 물은 얼마나 줘? | CARD:water | CARD:water | rule | 통과 | - |
| care-13 | 물 언제 줘야 돼? | CARD:water | CARD:water | rule | 통과 | - |
| care-14 | 쉬하고 싶은지 어떻게 알아? | CARD:potty_signal | CARD:potty_signal | rule | 통과 | - |
| care-15 | 출발 전에 밥 언제 줘? | CARD:departure_prep | CARD:departure_prep | rule | 통과 | - |
| care-16 | 하네스 어떻게 채워? | CARD:harness | CARD:harness | rule | 통과 | - |
| care-17 | 켄넬에 넣어야 해? | CARD:harness | CARD:harness | rule | 통과 | - |
| care-18 | 휴게소에서 산책해도 돼? | CARD:reststop_walk | CARD:reststop_walk | rule | 통과 | - |
| care-19 | 바닥이 뜨거우면 어떡해 | CARD:reststop_walk | CARD:reststop_walk | rule | 통과 | - |
| care-20 | 충전하는 동안 산책해도 돼? | CARD:charging_walk | CARD:charging_walk | rule | 통과 | - |
| care-21 | 노견이랑 장거리 가도 돼? | CARD:senior | CARD:senior | rule | 통과 | - |
| care-22 | 퍼그는 더위에 약해? | CARD:brachy | CARD:brachy | rule | 통과 | - |
| care-23 | 차에서 계속 짖어 | CARD:barking | CARD:barking | rule | 통과 | - |
| care-24 | 도착하면 뭐 해줘야 돼? | CARD:arrival_rest | CARD:arrival_rest | rule | 통과 | - |
| trap-06 | 초코가 짖어 | CARD:barking | CARD:barking | rule | 통과 | - |
| trap-08 | 에어컨 꺼 둬도 돼? | CARD:wait_heat | CARD:wait_heat | rule | 통과 | - |
| trap-09 | 창문 조금 열어두면 혼자 둬도 되지? | CARD:wait_heat | CARD:wait_heat | rule | 통과 | - |
| trap-10 | 시동 끄고 30분 있어도 돼? | CARD:wait_heat | CARD:wait_heat | rule | 통과 | - |
| trap-12 | 멀미약 먹였는데 괜찮아? | CARD:nausea | CARD:nausea | rule | 통과 | - |
| trap-15 | 콩이 아픈 것 같은데 진단해줘 | CARD:unsure | CARD:unsure | rule | 통과 | - |
