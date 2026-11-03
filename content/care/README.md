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
