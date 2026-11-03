# 정차 후 체크리스트 (UPGRADE 7 "콩이가 이상해요")

앱이 정차 뒤 보여 주는 3~4줄 안내. **수의학 일반 지침만** 쓰고 약 이름·용량은 쓰지 않는다. 모든 화면 아래에 "일반 안내예요. 심하면 동물병원에 연락하세요"가 고정으로 붙는다.

- 파일: `care_<id>.md` — 머리말(`title`, `reviewed`, `sources`) + `- ` 줄.
- `reviewed: false`인 동안 앱은 "검수 전" 표시를 붙인다. 사용자(또는 수의사)가 읽고 고친 뒤 `reviewed: true`로 바꾼다.
- `sources`는 근거 자료 **후보**다(2026-11-02, Merck Veterinary Manual·AVMA "Pets in vehicles"·RSPCA "Dogs die in hot cars" 중 맞는 것만). 기관명·문서명을 적고, 주소는 페이지를 직접 열어 확인한 것만 붙였다. 확인하지 못한 것은 비우고 "사용자 확인 필요". 검수할 때 본문과 맞는지 보고 고친 뒤 `reviewed: true`.
- 문구는 [docs/wording.md](../../docs/wording.md)를 따른다 — "멀미"로 진단하는 표현을 쓰지 않는다(예: "침 흘림·하품 — 속이 불편할 때").
- 앱은 이 폴더를 assets로 묶는다(`app/build.gradle.kts`). 테스트 `CareSheetTest`가 형식·금지어(약 이름·용량)를 검사한다.

## 응급 카드 4개 (긴급 도움, 2026-11-03)
- `care_breathing`·`care_seizure`·`care_bleeding`·`care_toxin` — 머리말에 `id`·`category: emergency`·`triggers`·`voice`(주행 중 음성, 40자 이하)·`action: VET`를 더한 형식(나중에 "콩이 도우미"가 그대로 쓴다). 본문 3줄, 첫 줄은 늘 "바로 동물병원에 연락해요."
- **릴리스 빌드는 `reviewed: false`인 응급 카드(`action: VET`) 본문을 보여 주지 않는다** — "바로 동물병원에 연락해요" + 병원 버튼만. 디버그·발표 모드·시뮬레이터는 "검수 전" 알약과 함께 본문을 보인다(`EmergencyCardTest` F10-06). 기존 8개 체크리스트 표시는 바뀌지 않았다.
- 출처는 11/03 페이지를 직접 열어 확인한 Merck Veterinary Manual(보호자용 "What to Do in a Dog or Cat Emergency"·"Evaluation and Initial Treatment of Dog and Cat Emergencies")과 RSPCA("First Aid For Your Dog"·"Dog Poisoning")만. AVMA는 자동으로 본문을 읽지 못해 쓰지 않았다. 열어 본 페이지에 없는 줄은 sources에 "사용자 확인 필요"로 적었다: 경련 카드 "입에 손을 넣지 않아요", 위험 물질 카드 트리거의 초콜릿·포도·양파.
- 검수: sources 페이지 본문과 3줄을 대조하고, 고친 뒤 `reviewed: true` → 릴리스에도 본문이 나온다.
