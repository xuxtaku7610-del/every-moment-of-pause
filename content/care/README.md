# 정차 후 체크리스트 (UPGRADE 7 "콩이가 이상해요")

앱이 정차 뒤 보여 주는 3~4줄 안내. **수의학 일반 지침만** 쓰고 약 이름·용량은 쓰지 않는다. 모든 화면 아래에 "일반 안내예요. 심하면 동물병원에 연락하세요"가 고정으로 붙는다.

- 파일: `care_<id>.md` — 머리말(`title`, `reviewed`, `sources`) + `- ` 줄.
- `reviewed: false`인 동안 앱은 "검수 전" 표시를 붙인다. 사용자(또는 수의사)가 읽고 고친 뒤 `reviewed: true`로 바꾼다. **2026-11-03: 12장 모두 사용자 검수 끝(`reviewed: true`)** — 출처 페이지와 문장별 대조 후 문구를 고쳤다(시원한 물·담그지 않음, 경련 "주변을 조용히", 불안 헐떡임은 쓰다듬기 대신 장난감으로 관심 돌리기 등). 수의사 검수는 아니다.
- `sources`는 근거 페이지다(11/03 검수 때 바꿈, Merck Veterinary Manual·RSPCA만 — AVMA는 본문을 읽지 못해 뺌). 기관명·문서명과 직접 열어 확인한 주소만 적고 본문을 옮기지 않는다. 카드를 고치면 다시 대조하고, 대조 전이면 `reviewed: false`로 되돌린다.
- 문구는 [docs/wording.md](../../docs/wording.md)를 따른다 — "멀미"로 진단하는 표현을 쓰지 않는다(예: "침 흘림·하품 — 속이 불편할 때").
- 앱은 이 폴더를 assets로 묶는다(`app/build.gradle.kts`). 테스트 `CareSheetTest`가 형식·금지어(약 이름·용량)를 검사한다.

## 응급 카드 4개 (긴급 도움, 2026-11-03)
- `care_breathing`·`care_seizure`·`care_bleeding`·`care_toxin` — 머리말에 `id`·`category: emergency`·`triggers`·`voice`(주행 중 음성, 40자 이하)·`action: VET`를 더한 형식(나중에 "콩이 도우미"가 그대로 쓴다). 본문 3줄, 첫 줄은 늘 "바로 동물병원에 연락해요."
- **릴리스 빌드는 `reviewed: false`인 응급 카드(`action: VET`) 본문을 보여 주지 않는다** — "바로 동물병원에 연락해요" + 병원 버튼만. 디버그·발표 모드·시뮬레이터는 "검수 전" 알약과 함께 본문을 보인다(`EmergencyCardTest` F10-06). 기존 8개 체크리스트 표시는 바뀌지 않았다.
- 출처는 11/03 페이지를 직접 열어 확인한 Merck Veterinary Manual(보호자용 "What to Do in a Dog or Cat Emergency"·"Evaluation and Initial Treatment of Dog and Cat Emergencies")과 RSPCA("First Aid For Your Dog"·"Dog Poisoning")만. AVMA는 자동으로 본문을 읽지 못해 쓰지 않았다. 11/03 검수에서 열어 본 페이지에 없던 경련 카드 "입에 손을 넣지 않아요"는 뺐다. 위험 물질 카드의 트리거(초콜릿·포도·양파 등)는 음성 분류용 말이고 카드 본문에는 없다.
- 검수: sources 페이지 본문과 3줄을 대조하고, 고친 뒤 `reviewed: true` → 릴리스에도 본문이 나온다. 11/03 4장 모두 `reviewed: true`.

## 콩이 도우미 카드 (2026-11-04, `_private/HELPER_PROMPT.md`)

### 형식 (모든 카드 공통)
```
---
id: water                         # 파일 이름 care_<id>.md와 같게
title: 이동 중 물 주기
category: care | emergency        # emergency = 도우미가 맨 먼저 보고, 응급 흐름(긴급 도움)으로 보낸다
triggers: [물 얼마나, 물은 언제]  # 규칙 분류 말(띄어쓰기 무시). 본문에 없는 말이어도 된다
voice: 쉬는 곳에서 물을 조금씩 줘요.   # 주행 중 음성 한 줄, 40자 이하, 본문 내용만
action: VET | NEAREST_STOP | POTTY_SPOT | WAIT_MODE | NEXT_REST | NONE
reviewed: false
sources: 기관 · 문서명 · 직접 연 주소 (날짜, 확인 상태)
---
- 본문 2~4줄 (약 이름·용량·사람 약 없음, 진단 표현 없음)
```
- 폴더: 검수 끝난 카드 = `content/care/`(릴리스·디버그), **초안 = `content/care/_drafts/`(디버그 assets에만)**. 자동 수집 후보 = `_drafts/_candidates/`(앱에 안 들어감).
- 릴리스 빌드는 `reviewed: false` 카드를 **도우미 목록에서 아예 뺀다**(`HelperAnswers.catalog`) — 분류도 서버 허용 목록도 못 고른다. 설정 실수로 초안이 들어가도 마찬가지(`HelperAnswersTest` F9-30·31·37).
- 디버그·발표 모드·시뮬레이터는 초안 답에 "검수 전" 칩을 붙인다.

### 초안 14장 (`_drafts/`, 모두 `reviewed: false`)
| id | 제목 성격 | 출처(11/04 페이지 열어 확인, 사용자 대조 전) | 빈틈(대조할 때 볼 것) |
| --- | --- | --- | --- |
| heatstroke | 응급 · VET | RSPCA heatstroke · dogs in hot cars | 11/05 검수 카드와 같은 문구 "시원한 물로 몸을 적셔요(물에 담그지는 않아요)"로 통일 |
| foreign_object | 응급 · VET | Merck emergency · RSPCA first aid | 11/05 양말·뼈 예시 삭제, "삼킨 것"으로 |
| sting | 돌봄 | Merck minor injuries | |
| wait_heat | 대기 · WAIT_MODE | RSPCA dogs in hot cars | 에어컨 켠 경우는 원문에 없음 |
| water | 돌봄 | RSPCA holiday · Merck travel kit | |
| potty_signal | 쉬할 곳 · POTTY_SPOT | RSPCA toilet training | |
| departure_prep | 출발 전 | RSPCA holiday · Merck motion sickness | |
| harness | 출발 전 | RSPCA holiday | |
| reststop_walk | 휴게소 산책 | RSPCA walking · hot walks | |
| charging_walk | 충전 중 산책 | RSPCA hot walks | |
| senior | 노견 | RSPCA senior dogs | |
| brachy | 단두종 | RSPCA genetics/caring | |
| barking | 짖음 | RSPCA barking | 차 안 짖음 자료는 없음(일반 짖음만) — 11/05 이번에는 내보내지 않음(보류) |
| arrival_rest | 도착 뒤 | RSPCA environment | |

### 검수 방법 (사용자)
1. 카드의 `sources` 주소를 직접 열고 본문 줄마다 근거 문장을 찾는다(없으면 그 줄을 지운다). 빈틈 열을 먼저 본다.
2. 약 이름·용량·사람 약·진단 표현이 없는지, `voice`가 본문 내용만인지(40자 이하) 본다.
3. 고친 뒤 `sources` 끝의 "(사용자 대조 전)"을 "(YYYY-MM-DD 사용자 대조)"로, `reviewed: true`로 바꾸고 파일을 `_drafts/`에서 `content/care/`로 옮긴다.
4. `./gradlew test`(CareSheetTest·HelperRouterTest·HelperAnswersTest)와 `pytest tests/test_helper.py`를 돌리고, `tools/eval_helper.py`로 점수표를 다시 만든다.

### 자료 후보 모으기 (`server/tools/collect_care_sources.py`)
- 개발 PC에서만(앱 실행 중·차 안에서는 크롤링하지 않는다). 목록에 적은 Merck·RSPCA 주소만, robots.txt 확인, 요청 사이 5초 이상.
- 결과 = `_drafts/_candidates/<slug>.md`: 요약 후보 2~4줄(`--summarize`면 로컬 Ollama가 인용만 근거로, 아니면 빈 칸) + 원문 인용 1~2개 + 주소 + 날짜. **원문 전체는 저장·업로드하지 않는다.** 후보는 카드가 아니다 — 위 검수 방법으로 사람이 옮긴다.
