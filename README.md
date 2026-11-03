# 같이가개 — 반려견과 차로 가는 모든 순간

반려견은 말을 못 하고, 운전자는 뒤를 못 봅니다. 같이가개는 차가 아는 정보(흔들림·기어·문·경로)로 **쉬는 순간과 장소, 잠깐 남겨 둘 때, 내릴 때**를 대신 챙기는 Pleos Connect 차량용 앱입니다.

[시연 영상 (11/13 공개 예정)](#) <!-- TODO 11/13: 영상 주소 --> · [APK 다운로드 v1.0.0 (Releases)](#) <!-- TODO 11/13: Releases 주소 --> · [Pleos 마켓 제출 패키지](docs/store/listing.md)

![왼쪽은 같이가개 분할 창(다음 회전·다음 쉼표·콩이 관찰 상태), 오른쪽은 Pleos 기본 내비 안내 화면](docs/store/screenshots/02-drive-split.png)

*주행 중: Pleos 기본 내비 옆 분할 창. 다음 회전 카드는 기본 내비의 실제 안내 값(NaviHelper), 나머지는 시뮬레이터 엔진 값.*

| 문제 | 같이가개 |
| --- | --- |
| 반려견은 아파도 말을 못 한다 | 보호자가 "콩이 토했어"라고 말하면 기록하고 한 줄 안내, 심하면 가장 가까운 동물병원 |
| 운전자는 달리는 동안 뒤를 볼 수 없다 | 굽은 길·이동 시간·배변 간격을 차가 계산해 쉬는 곳을 먼저 제안 |
| 차가 아는 정보는 많은데 반려견에 쓰이지 않는다 | 흔들림·기어 P·문·실내 온도·경로로 쉼표·잠깐 대기·"콩이 내렸나요?"를 대신 챙긴다 |

## 심사 기준 한눈에

| 기준 | 같이가개는 이렇게 | 근거 |
| --- | --- | --- |
| 차량특화성 | 속도·방위·기어·주차 브레이크·배터리·문·실내 온도·남은 경로를 함께 읽어야 되는 판단. 주행 중엔 2줄·버튼 2개, 길 안내는 기본 내비에 맡기고 옆 분할 창 | [신호 사용표](#pleos-sdk차량-신호-사용표), 앱 설정 "이 차에서 쓰는 신호", [docs/plan/sdk_check.md](docs/plan/sdk_check.md), [docs/store/distraction_check.md](docs/store/distraction_check.md), test_plan DS-01~ |
| 창의성 | "운전자 피로"가 아니라 말 못 하는 동승자를 흔드는 길을 미리 예보(차량 흔들림 기준). 휴게소를 반려견 기준으로 고르고, 차 화면을 창문 너머 안내판으로 | [docs/demo_script.md](docs/demo_script.md), [docs/plan/demo_compare.md](docs/plan/demo_compare.md) |
| 기술 활용성 | Vehicle SDK·NaviHelper·AAOS 차량 속성·CarUxRestrictions, ISO 2631-1 흔들림 누적 단순화 모델, 공공데이터 3종(동반 장소 327곳·동물병원 5,063곳). 서버·LLM 없이도 동작 | [docs/plan/motion_model.md](docs/plan/motion_model.md), [docs/plan/public_data.md](docs/plan/public_data.md), [docs/qa_report.md](docs/qa_report.md) |
| 사업성 | 반려견 양육 가구 대상, 동물병원 사전 접수 구독·동반 장소 제휴·펫보험 연계(가정 기반 추정) | [사업성 요약](#사업성-요약) |

## 핵심 기능

<table>
<tr>
<td width="50%"><img src="docs/store/screenshots/03-forecast.png" width="420" alt="굽은 길 예보 화면: 숫자 없이 한 줄 안내"><br><b>주행 자극 예보</b> — 앞길 굽은 구간을 숫자 없이 한 줄로. "3km 뒤 굽은 길, 천천히 가면 덜 흔들려요."</td>
<td width="50%"><img src="docs/store/screenshots/04-suggest.png" width="420" alt="쉼표 추천 화면: 횡성휴게소, 버튼 2개"><br><b>반려견 맞춤 쉼표</b> — 흔들림·이동 시간·배변 간격으로 반려견 놀이터 있는 휴게소 한 곳. "여기서 쉴게요" → 기본 내비 경유지(음성 8초 뒤 버튼).</td>
</tr>
<tr>
<td><img src="docs/store/screenshots/05-emergency-vet.png" width="420" alt="긴급 도움 화면: 위험 물질 섭취 카드와 가장 가까운 동물병원"><br><b>긴급 도움·병원 사전 접수</b> — 정차 중 상황 4가지 → 응급 카드(일반 안내) + 가장 가까운 병원 [경로 안내][전화], 제휴 병원이면 [사전 접수]. 자동 전화 없음.</td>
<td><img src="docs/store/screenshots/06-wait.png" width="420" alt="잠깐 대기 화면: 주인이 곧 돌아와요, 실내 온도, 경과 시간"><br><b>잠깐 대기</b> — "잠깐 다녀올게" → 창문 너머 2m에서 읽히는 큰 안내, 실내 온도는 차가 준 값이 있을 때만. 보호자 폰 페이지는 서버가 있을 때.</td>
</tr>
<tr>
<td><img src="docs/store/screenshots/07-arrival-door.png" width="420" alt="도착 후 콩이 내렸나요 화면"><br><b>문 신호</b> — 도착 후 뒷문이 열리면 바로 "콩이 내렸나요?", 휴게소에서 운전석 문이 열리면 대기 모드 제안, 답이 없으면 안전 안내 한 번.</td>
<td><img src="docs/store/screenshots/08-report.png" width="420" alt="도착 후 기록 리포트: 이동 시간, 거리, 휴식, 관찰 신고, 타임라인"><br><b>도착 리포트</b> — 시간순 타임라인과 주행 자극 그래프, 병원에 보여 줄 요약. 위치는 남기지 않는다.</td>
</tr>
</table>

전체 기능(F1~F10)·단계별 설명은 [docs/features.md](docs/features.md), 화면 전체(21장)는 [docs/screens.md](docs/screens.md).

## 심사위원용 실행 방법 (5분)

1. **에뮬레이터 준비** — Pleos Playground에서 Pleos Connect Emulator 2.1.0(API 34) AVD를 만들고 켠다(아래 "테스트 환경").
2. **APK 받기** — [Releases](#) <!-- TODO 11/13: Releases 주소 --> 에서 `gatigage-v1.0.0.apk`.
3. **설치·실행** — 이전 버전이 있으면 먼저 지운다(서명이 달라 덮어쓰기가 안 된다).
   ```powershell
   adb uninstall com.emop.pause
   adb install gatigage-v1.0.0.apk
   adb shell am start -n com.emop.pause/.MainActivity
   ```
4. **둘러보기** — 첫 실행은 온보딩 3단계("나중에"로 건너뛰어도 된다). 출발 전 화면에서 10초쯤 지나면 아래에 **"차량 신호가 없어요. 시연 주행으로 둘러볼까요?"** 띠가 뜬다.
   - **[시연 주행 시작]**을 누르면 `pet-trip`(서울→강릉) 시연 주행이 준비된다. 체크 3개를 누르고 [이대로 출발].
   - **장면 이동:** 앱 바 왼쪽 로고를 3초 안에 **5번 탭** → 발표 모드 패널. "장면으로 이동"에서 출발 체크·고속도로·쉼표 추천·휴게소 쉼표·잠깐 대기·고갯길 예보·도착 체크 등을 고르면 그 장면부터 달린다. 음성 대신 "운전자 응답 넣기"("여기서 쉴게요"·"콩이 토했어" 등)를 누른다.
   - "고갯길 비교"는 같은 길을 안내 없이 / 안내대로 달린 주행 자극을 나란히 보여 준다(55 → 30).

릴리스 APK는 서버 주소가 비어 있어 서버에 연결하지 않는다(오프라인 제출본). 시연·서버 연동은 디버그 빌드.

| 릴리스 APK만으로 되는 것 | 서버가 있어야 되는 것 |
| --- | --- |
| 주행 자극 예보, 쉼표 추천(내장 휴게소 샘플)·경유지 추가, 음성 응답(8초 → 버튼) | 휴게소 실제 데이터 갱신 (`/reststops`) |
| "콩이가 이상해요" 안내·정차 후 체크리스트(11/03 검수 끝) | 쉼표·요약 문장 다듬기(Ollama `/llm/phrases`, 없으면 템플릿 문장) |
| 긴급 도움(상황 4가지·병원 안내·응급 카드 본문) | 동물병원 실제 목록·전화번호 (`/vets`, 없으면 내장 샘플) |
| 잠깐 대기 모드(차 화면), 도착 체크·문 이벤트 | 보호자 폰 대기 페이지·"돌아가는 중"(`/wait`) |
| 도착 후 기록 리포트·개인 보정(기기 저장) | 도착 예정 공유 링크(`/eta`), 병원 사전 접수·병원 접수함(`/clinic`) |
| 함께 가는 장소·동물병원 목록(내장 샘플, 전화번호 없음) | 함께 가는 장소 전국 327곳(TourAPI)·동물병원 5,063곳 |
| 발표 모드(로고 5번 탭)·장면 점프·고갯길 비교 | — |

## 테스트 환경·필요한 설정

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect Emulator **2.1.0**, AVD 시스템 이미지 API 34(Android 14), 화면 2560×1440 (density 160) |
| 앱 SDK | minSdk 28 · targetSdk 34 · compileSdk 35 |
| 메모리 | AVD RAM **4096MB**(에뮬레이터 최소값). 호스트 메모리를 5~6GB 쓰므로 PC 남은 메모리 6GB 이상 권장 |
| 권한 | 차량 신호: `pleos.car.permission.CAR_DRIVE`·`CAR_ENERGY`·`CAR_CLIMATE`·`CAR_DOORS`·`NAVI_ROUTE`(normal, 자동 허용), AAOS 속도·충전(`CAR_SPEED`·`CAR_ENERGY`, 첫 실행에 허용). 마이크(`RECORD_AUDIO`)는 출발 버튼에서 묻는다 — 거절해도 버튼으로 응답 |
| 오디오 | 에뮬레이터는 마이크·TTS 엔진이 없어 음성 대신 버튼(8초 뒤 자동 전환). 음성 신고 시연은 발표 모드 |
| 서버(선택) | Docker Desktop(PostgreSQL 16), Python 3.11, Ollama `gemma4:12b` — [개발자용 상세 실행](#개발자용-상세-실행) |

개발 PC: Windows 11 Pro, Ryzen 5 9600X, RAM 32GB, RTX 5060 Ti 16GB(Ollama).

## Pleos SDK·차량 신호 사용표

시그니처는 aar·jar를 javap로 확인한 것만 쓴다([docs/tech_notes.md](docs/tech_notes.md)). 신호마다 Pleos SDK → AAOS 속성 → (시연) 시뮬레이터 순으로 받고, 없는 신호는 그 기능만 끈다.

| 기능 | 쓰는 신호 | API | 신호가 없을 때 |
| --- | --- | --- | --- |
| 주행 화면 선택 (주행 안전) | 주행 제한 상태 | AAOS `CarUxRestrictionsManager.registerListener` | 항상 있음 (주행 중이면 2줄·버튼 2개) |
| 주행 자극 예보 | 속도, 진행 방위, 앞 회전 안내 | AAOS `PERF_VEHICLE_SPEED` / NaviHelper `getCurrentLocationInfo`(heading)·`onTBTInfo`·`getTBTInfo` | 방위·회전 안내가 없으면 종방향만 계산하고 예보 없음(F1-08), 속도가 없으면 계산 안 함 |
| 다음 회전·남은 거리·도착 카드 | 회전 안내, 남은 거리·시간, 목적지 | NaviHelper `onTBTInfo`·`onDestinationInfo`·`onWayPointInfo` | 회전 안내가 없으면 목적지·남은 거리·도착 한 줄, 그것도 없으면 숨김 |
| 쉼표 추천 → 경유지 | 주행 시간·속도, 배터리, 남은 거리, 경유지 추가 | AAOS `PERF_VEHICLE_SPEED` / Pleos `EvBattery.registerEvBatteryStateOfCharge`(없으면 AAOS `EV_BATTERY_LEVEL`) / NaviHelper `addWaypoint` | 배터리가 없으면 충전 니즈 빼고 추천, 경유지 추가가 실패하면 "내비에서 골라 주세요" 한 줄 |
| 운전자 피로(쉼표 가중) | 공조 에어컨·내기 순환 | Pleos `HVAC.registerHvacAcState`·`registerHvacRecirculationState` | 그 요인을 빼고 재정규화 |
| 잠깐 대기 모드 | 기어 P, 주차 브레이크, 배터리, 실내 온도 | Pleos `Gear.registerGearSelection`(없으면 AAOS `GEAR_SELECTION`), AAOS `PARKING_BRAKE_ON`, Pleos `HVAC.getHvacTemperatureCurrent`·`registerHvacTemperatureCurrent` | 기어가 없으면 주차 브레이크로, 온도가 없으면 온도 표시 숨김 |
| 도착 체크 "콩이 내렸나요?" | 기어 P(60초), 실내 온도, 뒷문 열림 | 위 기어·온도 + Pleos `Door.registerDoorPosition`·`getCurrentDoorPosition`(ROW_2_LEFT·ROW_2_RIGHT) | 문 신호가 없으면 P단 60초 뒤에 묻는다 |
| 문 이벤트 (휴게소 대기 모드 제안, 도착 후 안전 안내 한 번) | 운전석·뒷문 열림 | Pleos `Door.checkDoorPositionCapabilities`·`registerDoorPosition`(ROW_1_LEFT 등), 권한 `pleos.car.permission.CAR_DOORS` | 이 기능만 꺼짐, "잠깐 다녀올게" 음성 진입은 그대로 |
| 충전 중 정차 | 충전 상태 | AAOS `EV_CHARGE_STATE` | 충전 정차를 휴식 정차로 본다 |
| 긴급 도움 (정차 중만) | 주행 제한 상태, 지금 위치·목적지, 경유지 추가 | AAOS `CarUxRestrictionsManager`(주행이면 열리지 않음) / NaviHelper `getCurrentLocationInfo`(병원 거리)·`addWaypoint`(누를 때만) | 위치가 없으면 목적지 근처 병원, 병원 데이터가 없으면 "근처에 찾은 곳이 없어요" |

```mermaid
flowchart LR
  V["차량 신호<br/>Pleos Vehicle SDK · AAOS 속성"] --> A
  U["주행 제한 상태<br/>CarUxRestrictions"] --> A
  N["Pleos 기본 내비<br/>NaviHelper"] <--> A
  A["같이가개 차량 앱<br/>엔진·화면, 오프라인 우선"] <-->|"선택: 서버가 있을 때"| S["서버<br/>FastAPI"]
  S --> D[("PostgreSQL")]
  S --> L["Ollama<br/>문장 다듬기"]
  P["공공데이터 3종<br/>휴게소·동반 장소·동물병원"] --> S
  S --> W["보호자 폰·병원 접수함<br/>웹 페이지"]
```

**주행 자극 모델 (요약).**
- 속도·진행 방위·앞 구간 경로 형상(회전 안내 또는 시연 GPX)으로 앞뒤·좌우 흔들림을 계산하고, ISO 2631-1 부록 D의 흔들림 누적량(MSDV) 개념을 단순화해 0~100으로 쌓는다.
- 체급·나이·흔들림에 예민한 정도(보호자 응답)로 민감도를 곱하고, 정차하면 줄어든다. 계수는 모두 문헌을 참고한 가정값이다.
- 같은 고갯길(OSM 대관령 옛길)을 안내대로 달리면 주행 자극 55 → 30(−45%). 자세한 내용: [docs/plan/motion_model.md](docs/plan/motion_model.md).

> 주행 자극은 **차량 흔들림 기준 · 반려견 측정·진단 아님**. 반려견 상태는 보호자 음성 신고(콩이 관찰 상태)로만 따로 남긴다.

## 사업성 요약

> 아래 숫자는 모두 **가정 기반 추정**이다(기획서 6번).

- **타깃:** 반려동물 양육 가구 29.2%, 그중 반려견 80.5% (2025 농림축산식품부).
- **수익 3가지:** ① 동물병원 사전 접수 구독 ② 반려동물 동반 장소 제휴 ③ 펫보험 연계.
- **지표:** 1년 차 국내 30만 대를 가정 → 설치 약 1만·MAU 약 5천, 3개월 리텐션 목표 40%.
- **확대 3단계:**
  1. **국내:** 실차 검증, 반려견별 패턴 인사이트, 보호자 2명 공유, 여러 마리, 응급 동물병원 중심 사전 접수 제휴 시범.
  2. **연동:** Vehicle Control이 열리면 대기 모드 공조 유지, 동물병원 예약관리 시스템 연동, 펫보험 제휴.
  3. **해외·확장:** 플레오스 커넥트 해외 출시에 맞춰 지역별 병원·장소 데이터 교체, 같은 주행 자극 엔진으로 뒷좌석 아이 멀미 케어·초보 운전 코치로 확장, 실내 카메라·웨어러블로 관찰 자동화(보호자 동의 기반).
- **2차(PT) 계획:** 콩이 도우미 — 검수된 돌봄 카드만 근거로 답하는 대화형 도우미(질문 분류 → 카드 검색 → 설명 → 근거 검사, 함정 문항 평가 세트로 검증).

## 검증

- **테스트(고유 수):** 앱 339개 · 서버 81개, 모두 통과. 앱은 모듈별로 한 빌드 변형만 센다([docs/qa_report.md](docs/qa_report.md), 테스트 목록 [test_plan.md](test_plan.md)).
- **PT 리허설:** 연속 3회 통과, 재시작 없이 77·68·66초(11/02, 문 이벤트 빌드). 예비 장면까지 9장면 `--all` 통과(141초, 11/03).
- **장면 점프 시험:** 20회 연속 점프 뒤에도 쉼표 추천이 음성 → 버튼으로 넘어감(듣는 중 최장 8초).
- **숫자 대조:** 발표 화면 숫자와 엔진 값 [docs/demo_numbers.md](docs/demo_numbers.md).

**실차 확인 예정 (2026-11-02 기준).** 시연은 앱 시뮬레이터(시나리오)와 발표 모드로 하고, 회전 안내만 기본 내비 실제 값(NaviHelper)을 받는다. 막힌 항목: [docs/BLOCKED.md](docs/BLOCKED.md).

| 항목 | 에뮬레이터 결과 | 실차에서 확인할 것 |
| --- | --- | --- |
| 고갯길 회전 안내 (NaviHelper `onTBTInfo`) | **확인함** — 성남→강릉 안내 32건 수신. `distance`는 구간 거리라 누적해서 씀. 회전 안내만으로 예보하는 모드는 시뮬레이터 테스트로 허용 오차 통과([docs/plan/shape_compare.md](docs/plan/shape_compare.md)) | 고갯길 굽이가 회전 안내 목록에 나오는지(안 나오면 방위 무늬 추정이 주력, 들어선 뒤 1~2km), 주행하며 남은 거리가 갱신되는지 |
| 실내 온도 값·단위 | 에뮬레이터는 값 없음 → 온도 표시 숨김·여름 일반 문구 확인 | `getHvacTemperatureCurrent` 값이 오는지, 단위(℃ 가정), 대기 중 갱신 주기 |
| 위치 따라 안내 재생 | 안 됨 — `geo fix`가 내비 위치를 바꾸지 않음(Pleos 위치 서비스 별도) | 실제 주행으로 예보 시작 거리 |
| 차량 값(기어·속도·벨트)·Car data 패널 | 시도 중단(결정) — 에뮬레이터 user 빌드 | AAOS 속성·Pleos Vehicle SDK 값이 앱 "이 차에서 쓰는 신호"에 뜨는지 |
| 앱이 뒤에 있을 때 콜백 | 전체 화면에서 내비가 앞이면 앱 프로세스가 내려가 콜백 끊김. **런처 분할 창에서는 확인함(10/31)** — 기본 내비가 앞일 때 `onTBTInfo` 32건·`onDestinationInfo`·`onWayPointInfo` 수신 | 실제 주행 중 갱신 주기 |
| 실제 분할 창 1/2·1/3 | **확인함** — `am task resize`로 1280·853dp, 앱 영역 오른쪽 끝 손잡이 끌기 `adb shell input swipe 2547 720 1700 720 800` | 사용자가 실차 런처에서 직접 나누는 동작과 실제 창 폭 |
| 1920×1080 배치 | 이 AVD 런처는 `wm size`를 바꿔도 앱 영역이 1067×850 → 정차 화면은 1/2 창 배치(쌓기+스크롤)로 보임 | 실제 1920×1080 Pleos 런처의 앱 영역과 정차 화면 전체 창 배치 |
| 음성 인식·말하기 | 에뮬레이터는 마이크·TTS 엔진 없음 → 버튼 대체 확인. 음성 응답은 8초 한 번, 없으면 무조건 버튼 | 차 마이크로 "여기서 쉴게요", 차 스피커 TTS와 내비 안내 음성이 겹칠 때 |
| 문자열 길이 (주행 중) | CarUxRestrictions 문자열 길이 제한은 대체 문구(14자)로 처리 | 실차가 주는 `maxStringLength` 값 |
| 문 신호 (Pleos `Door`) | **확인함(11/02)** — 실차 모드에서 문 위치 지원 영역 6곳, 운전석·뒷좌석 왼쪽·오른쪽 등록, 권한 `CAR_DOORS`(normal) 자동 허용. 값 변화는 에뮬레이터에서 못 바꿔 시나리오·`--es door`로 확인 | 위치 값의 뜻(0만 닫힘으로 봄), 운전석 = 1열 왼쪽(좌핸들 가정), 열고 닫을 때 알림이 오는지 |
| 개인 보정 | 시연 시드(`--es calib`)로 확인, 화면 숫자는 보정 전 값(DM-08) | 실제 여러 번의 여행에서 신고가 쌓이는지 |

## 상용화 준비

Pleos 앱 심사 가이드·제출 가이드 기준으로 `docs/store/`에 제출 패키지를 만들었다.

| 문서 | 내용 |
| --- | --- |
| [data_inventory.md](docs/store/data_inventory.md) | 데이터 처리 표(코드 기준), 문서와 코드의 불일치 |
| [privacy.md](docs/store/privacy.md) · [privacy.html](docs/privacy.html) | 개인정보 처리방침 (GitHub Pages로 공개 예정) |
| [distraction_check.md](docs/store/distraction_check.md) | 운전자 주의분산 자체 점검표(지킴/일부/확인 필요) |
| [listing.md](docs/store/listing.md) | 이름·설명·업데이트 노트·카테고리·태그·연령 등급 메모·권한 이유·심사 노트 |
| [screenshots/](docs/store/screenshots/) · [icon-512.png](docs/store/icon-512.png) | 스크린샷 8장(1700×1212, "시연 데이터" 표시 없이), 아이콘 512×512 |
| [release_notes_v1.0.0.md](docs/release_notes_v1.0.0.md) · [release.md](docs/release.md) | 릴리스 노트 초안, 서명·릴리스 빌드·About·topics·GitHub Pages 절차 |

**실제 출시 때 남은 것**
- 클라우드 서버: 지금 제출본은 서버 없이 동작한다. 보호자 페이지·병원 접수·실데이터 갱신을 쓰려면 서버를 운영해야 한다.
- 개발자 인증과 차량 SDK 권한 사전 승인.
- IARC 연령 등급 발급.
- 사진 업로드 방식: 차 화면 QR → 보호자 폰 브라우저에서 사진 선택 → 서버 → 차. 출시 때는 클라우드 서버가 필요하다(지금은 앱 내장 일러스트·시연 사진).
- 고객 지원: GitHub Issues(저장소 Issues 탭). 개인정보 처리방침 시행일 2026-11-13.

---

<details>
<summary><b>개발자용 상세 실행</b> (서버·DB·Ollama·환경 변수·시연 명령·테스트·릴리스 빌드)</summary>

### 개발자용 상세 실행

**사전 준비**
- Android Studio (최신 안정판) + Temurin 21. `JAVA_HOME`을 Temurin 21로 두고 Settings › Build Tools › Gradle › Gradle JDK에서 지정 (PC별 경로는 커밋하지 않음)
- Android SDK Platform 35 (compileSdk)
- Pleos Playground 가입 → 프로젝트 생성 → **Pleos Connect Emulator 2.1.0** 시스템 이미지(API 34) 설치 및 AVD 설정(`Pleos_Connect_API34`)
- Docker Desktop, Python 3.11
- Ollama 설치 + `ollama pull gemma4:12b`
- 공공데이터포털 한국도로공사 휴게소 API 인증키 (선택: 없으면 수집 잡이 `server/app/data/sample_reststops.json` 샘플 데이터로 동작)

**1) 환경 변수**
```bash
cp server/.env.example server/.env
# EX_API_KEY=            # 비워 두면 샘플 휴게소 데이터 사용
# TOUR_API_KEY=          # 비워 두면 샘플 반려동물 동반 장소 사용
# OLLAMA_BASE_URL=http://127.0.0.1:11434
# OLLAMA_MODEL=gemma4:12b
# OLLAMA_KEEP_ALIVE=30m
# OLLAMA_NUM_PREDICT=60
# DATABASE_URL=postgresql+psycopg://pause:pause@127.0.0.1:5432/pause
```

**2) 백엔드**
```bash
docker compose up -d db
cd server
python -m venv .venv
.venv\Scripts\activate                              # Windows
pip install -r requirements.txt
python -m alembic upgrade head
python -m app.jobs.ingest_reststops                  # 휴게소 데이터 적재 (EX_API_KEY 없으면 샘플)
python -m app.jobs.ingest_pet_places                # 반려동물 동반 장소 (TOUR_API_KEY 없으면 샘플)
python -m app.jobs.load_vets                         # 동물병원 (data/raw/ 파일 없으면 샘플)
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000
```
확인: `http://127.0.0.1:8000/api/v1/health` → `{"status":"ok", ...}`, API 문서는 `/docs`.
Docker로 한 번에: `docker compose up -d --build` (db + api).
보관 기간 정리(하루 1회): `python -m app.jobs.cleanup`.

**3) 차량 앱**
```bash
cd app-android
./gradlew -Pkotlin.compiler.execution.strategy=in-process assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
- 서버 주소는 빌드 종류로 정해진다: 디버그 `http://10.0.2.2:8000/api/v1/`(에뮬레이터 → 호스트 PC), 릴리스는 비어 있음 = 오프라인(내장 휴게소·장소·병원 샘플·템플릿).
- 보호자 폰 대기 페이지: 서버를 `--host 0.0.0.0`으로 띄우고, 같은 Wi-Fi의 폰이 `http://<노트북 IP>:8000/wait/<토큰>`을 연다. 차 화면에 보일 주소 앞부분은 `--es shareBase http://<노트북 IP>:8000` (`scripts/demo_check.ps1 -DetectIp`가 주소를 찾아 준다).
- 시연(디버그 빌드):
  ```bash
  adb shell am start -n com.emop.pause/.MainActivity --es sim pet-trip --es action play      # 서울→강릉 시연 시나리오
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 3420 --es action play         # 쉼표 추천 직전
  adb shell "am start -n com.emop.pause/.MainActivity --es say '여기서 쉴게요'"               # 음성 응답 주입 (띄어쓰기는 따옴표로)
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 5240 --es action play         # 휴게소 쉼표
  adb shell "am start -n com.emop.pause/.MainActivity --es say '잠깐 다녀올게'"               # 대기 모드
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 8748 --es action play         # 고갯길 3km 앞 주행 자극 예보
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 9660 --es action play          # 도착 10분 전 → 약 12초 뒤 산책 제안
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 10843 --es action play        # 도착 → 60초 뒤 "콩이 내렸나요?"
  adb shell am start -n com.emop.pause/.MainActivity --es emergency toxin                    # 긴급 도움 카드 (정차 중)
  adb shell am start -n com.emop.pause/.MainActivity --ez vehicle true                       # 실차 신호(Pleos SDK·AAOS 속성)
  ```
  시연 대본: [docs/demo_script.md](docs/demo_script.md).
- 장면별 차량 값 + 앱 장면 맞추기: `powershell -ExecutionPolicy Bypass -File scripts/vhal.ps1 <depart|highway|rest_stop|wait|mountain_road|arrive>` (user 빌드 에뮬레이터는 값 주입이 막혀 있어 앱 시뮬레이터로 맞춘다, [docs/vhal_check.md](docs/vhal_check.md)).
- 시연 데이터 다시 만들기: `python scripts/gen_demo_data.py` (시나리오·샘플, `--fetch-osm`이면 OSM 고갯길 GPX도 다시 받기). 고갯길 비교: `./gradlew :app:testDebugUnitTest --tests "*DemoCompareTest*"` → [docs/plan/demo_compare.md](docs/plan/demo_compare.md), 형상 소스 비교(GPX vs 회전 안내·방위): [docs/plan/shape_compare.md](docs/plan/shape_compare.md).

**시연 점검 (한 번에)**
```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1             # 점검만: 줄마다 OK/WARN/FAIL
powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Prepare -ShareBase http://<노트북 IP>:8000 -Rehearsal
powershell -ExecutionPolicy Bypass -File scripts\demo_check.ps1 -Stop       # 쉴 때: Gradle 데몬·서버·에뮬레이터·DB 정리
```
- 점검 항목: Gradle 데몬 종료 → adb 기기·부팅 완료 → 앱 설치·버전 → PostgreSQL 컨테이너 → 서버 상태 → Ollama·`gemma4:12b`(없으면 WARN, 템플릿으로 동작) → PC 남은 메모리. 비밀값은 출력하지 않는다.
- `-Prepare`는 꺼진 DB·서버(0.0.0.0:8000)·에뮬레이터·앱을 켜고, `-Rehearsal`은 기본 내비 안내 걸기 안내 뒤 리허설 1회를 돈다. 에뮬레이터는 호스트 메모리를 5~6GB 쓰므로 남은 메모리가 6GB보다 적으면 켜지 않는다.
- **분할 창(주행 장면):** 장면을 띄운 뒤 앱 영역 오른쪽 끝 손잡이를 왼쪽으로 끈다 → 같이가개(왼쪽) + Pleos 기본 내비(오른쪽). 에뮬레이터에서는 `adb shell input swipe 2547 720 1700 720 800`. 정차 장면(비교·리포트)은 전체 창으로 되돌린다.
- 발표 순서·말할 것: [docs/demo_script.md](docs/demo_script.md), 녹화·기본 내비 안내 걸기: [docs/recording.md](docs/recording.md)(`scripts/record_scenes.py`), 숫자 대조: [docs/demo_numbers.md](docs/demo_numbers.md).
- 리허설만: `python scripts/rehearsal.py` (PT 3장면 + 여는·닫는 장면, 약 70초), `--all`(예비 장면 포함), `--jumps 20`(장면 점프 연속 뒤 쉼표 추천 버튼 확인). 결과 표: [docs/qa_report.md](docs/qa_report.md).
- 발표 직전에는 앱을 새로 띄운다: `adb shell am force-stop com.emop.pause`.

**4) 테스트**
```bash
cd server
.venv\Scripts\python -m pytest                     # PostgreSQL 테스트는 DB가 없으면 건너뜀
cd ../app-android
./gradlew -Pkotlin.compiler.execution.strategy=in-process test
```
테스트 이름에는 [test_plan.md](test_plan.md) 항목 ID(F1-01, DS-02, SV-01 …)를 쓴다. 점검 결과는 [docs/qa_report.md](docs/qa_report.md).

**5) 릴리스 APK**
[docs/release.md](docs/release.md) — 서명 키는 저장소 밖에 두고, 서명 값은 `app-android/local.properties`(gitignore) 또는 환경 변수. 결과는 `app/build/outputs/apk/release/gatigage-v1.0.0.apk`(R8 축소 끔).

</details>

<details>
<summary><b>기술 스택</b></summary>

| 영역 | 기술 | 비고 |
| --- | --- | --- |
| 차량 앱 | Kotlin 2.x, Jetpack Compose, Hilt, Coroutines/Flow, Room | Android Automotive OS (Pleos Connect), minSdk 28(근거: [docs/tech_notes.md](docs/tech_notes.md)) |
| 차량 연동 | Pleos Connect Vehicle SDK 2.3.2, NaviHelper SDK 3.0.5, `CarUxRestrictionsManager` | 둘 다 신청 없이 사용 가능 |
| 음성 | Android `SpeechRecognizer` / `TextToSpeech` (현재 1순위), Pleos STT/TTS가 열리면 교체 | 공급자 추상화 |
| 백엔드 | Python 3.11, FastAPI, SQLAlchemy 2, Alembic, Pydantic v2 | 휴게소·동반 장소·동물병원 데이터, 대기 공유, LLM 게이트웨이 |
| DB | PostgreSQL 16 (단위 테스트만 SQLite in-memory) | Docker Compose |
| LLM | Ollama `gemma4:12b` | 네이티브 `/api/chat`, `think=false`, `keep_alive=30m`, `num_predict=60`, `temperature=0.3`, 서버 시작 시 warm-up, 최대 2문장, 템플릿 문장 폴백 |
| 외부 데이터 | 한국도로공사 휴게소 API, 한국관광공사 TourAPI, 지방행정 인허가(동물병원) | 공공데이터포털 인증키 (없으면 샘플) |
| 테스트 | JUnit5, MockK, Turbine, Compose UI Test / pytest, httpx | |
| 빌드·배포 | Gradle 8, GitHub Actions(APK 빌드), GitHub Releases | 1차 접수 가산점: 소스 + APK |

빌드 설정(Gradle JDK·jvmTarget), SDK 호환성 확인 결과, aar 실제 확인 결과, Pleos Playground 현황, 주행 안전 규칙, DB 테스트 정책: [docs/tech_notes.md](docs/tech_notes.md). 구조: [architecture.md](architecture.md).

</details>

<details>
<summary><b>저장소 구조</b></summary>

```
every-moment-of-pause/
├─ README.md · architecture.md · flowchart.md · api_spec.md
├─ database.md · test_plan.md · todo.md
├─ app-android/                 # Pleos Connect 앱 (Gradle 멀티모듈)
│  ├─ app/                      # Compose 화면, ViewModel, 내비게이션, AppModeReducer
│  ├─ core/model/               # 신호·상태 모델, 잡음 처리
│  ├─ core/uistate/             # 화면 계약 (UiState·UiEvent·Fixtures)
│  ├─ core/vehicle/             # Pleos Vehicle SDK · AAOS 차량 속성 어댑터
│  ├─ core/navi/                # NaviHelper 어댑터
│  ├─ core/restriction/         # CarUxRestrictions → DriveState
│  ├─ engine/motion/            # F1 주행 자극 예보 (MotionComfortEngine)
│  ├─ engine/pet/               # 반려견 프로필 · F3 대기 모드 · F6 체크 · F7 기록 패턴 · 긴급 도움 분류
│  ├─ engine/fatigue/           # 운전자 피로 (F2 요인)
│  ├─ engine/needs/             # 배변·산책 간격, 충전
│  ├─ engine/planner/           # F2 반려견 맞춤 쉼표
│  ├─ engine/pause/             # 쉼표에서 할 일 (산책·물·배변)
│  ├─ voice/                    # STT·TTS·의도 인식
│  ├─ data/                     # Room, Retrofit, 캐시, 내장 데이터
│  └─ sim/                      # 장면 시나리오 리플레이어
├─ server/                      # FastAPI 백엔드
│  ├─ app/ (api, models, jobs, data)
│  ├─ alembic/
│  └─ tests/
├─ content/care/                # 정차 후 체크리스트·응급 카드
├─ scenarios/                   # 시뮬레이터 장면 JSON · 시연 GPX
├─ scripts/                     # 시연 점검·리허설·캡처·녹화, 시연 데이터 생성
├─ docs/                        # 상태·문구 기준, 스토어 패키지, 화면, QA
├─ docs/plan/                   # CONCEPT · REUSE_MAP · sdk_check · motion_model · demo_compare
└─ docker-compose.yml
```

</details>

<details>
<summary><b>8개 상태</b></summary>

기준 문서: [docs/states.md](docs/states.md).

| # | 상태 | 화면 | 들어오는 조건 | 주행 중 제한 |
| --- | --- | --- | --- | --- |
| 1 | 출발 전 | 출발 준비 (첫 실행은 온보딩) | P단, 여행 시작 전 | 정차 중만 |
| 2 | 일반 주행 | 기본 내비 옆 분할 창: 다음 회전·남은 거리·도착, 다음 쉼표, 콩이 관찰 상태 | 주행(CarUxRestrictions) | 2줄·버튼 0 |
| 3 | 주행 자극 예보 | 굽은 길 예보 한 줄 + 음성 | 앞 5km 안 굽은 길 | 숫자 없음 |
| 4 | 쉼표 추천 | 장소·도착까지·이유 2개, "여기서 쉴게요" | 플래너 제안 | 버튼 2개 + 음성(8초 뒤 버튼) |
| 5 | 콩이가 이상해요 | 신고 기록 → 1줄 안내 → 심하면 병원 제안 | 음성 신고("콩이 토했어") | 음성 중심, 버튼 2개 |
| 6 | 대기 모드 | 큰 안내·경과 시간, 보호자 폰 페이지(QR, 설정 안), 온도는 차가 준 값이 있을 때만 | "잠깐 다녀올게" + P단 | P단만 |
| 7 | 병원 이동 | 병원 선택 → 경유지 → 전달 동의 → 도착 예정 갱신 (사전 접수는 시연용 제휴 병원만) | 상태 5에서 "동물병원으로" | 입력은 정차·음성 |
| 8 | 도착 후 기록 리포트 | 첫 화면은 요약 한 줄 + 시간순 타임라인, "주행 자극 자세히 보기"에서 그래프("차량 흔들림 기준 · 반려견 측정 아님")·병원에 보여 줄 내용 | 도착(+ "내렸어요") | 정차 중만 |
| – | 긴급 도움 (겹침) | 정차 화면 위: 상황 4가지 → 응급 카드 + 가장 가까운 병원 | 정차 화면의 "긴급 도움" 또는 정차 중 응급 음성("콩이 초콜릿 먹었어") | 주행 중 열리지 않음 (음성만) |

</details>

<details>
<summary><b>개인정보·제출 유의사항</b></summary>

- 앱은 계정 없이 **익명 기기 토큰**만 사용. 반려견 프로필은 기기에만 저장. 위치는 장소 조회에만 쓰고 저장하지 않으며, 대기 공유에는 위치를 보내지 않는다.
- 공모전 규정상 **공유 링크·첨부자료에 개발자 실명·계정정보가 노출되면 안 됨** → GitHub 계정명, 커밋 작성자명, APK 서명 정보, 영상 화면을 점검.
- 1차 접수 가산점: 시연 영상 · GitHub 저장소(소스 + APK 함께). APK만 따로 제출하지 않는다.
- 개인정보 처리방침: [docs/privacy.html](docs/privacy.html)(단독 정적 페이지). 저장소 공개 뒤 GitHub Pages URL로 스토어에 등록할 예정이다.

</details>

<details>
<summary><b>데이터 출처·라이선스</b></summary>

저장소 자체는 [LICENSE](LICENSE)(All rights reserved, 공모전 심사·열람 목적만 허용). 아래 자료는 LICENSE의 예외로 각자의 조건을 따른다.

| 대상 | 출처 · 라이선스 |
| --- | --- |
| 반려동물 동반 장소·사진 | 한국관광공사 TourAPI `KorPetTourService2` — 공공누리(사진 `cpyrhtDivCd` Type1·Type3, Type3는 변경 금지라 자르기만), 앱에 "사진 한국관광공사" 표기 |
| 동물병원 | 지방행정 인허가 데이터(동물병원), 공공데이터포털 — 이용 조건(이용허락범위) 확인 필요 |
| 휴게소 | 한국도로공사 휴게소 API(공공데이터포털) — 키가 없어 지금은 샘플 30곳(경부선 실제 휴게소 이름, 시설 정보는 확인 필요) |
| 고갯길 시연 경로 | © OpenStreetMap contributors, ODbL 1.0 |
| 글꼴 | Pretendard, SIL OFL 1.1 ([licenses/Pretendard-OFL.txt](licenses/Pretendard-OFL.txt)) |
| 아이콘 | Microsoft Fluent UI Emoji(MIT, [licenses/FluentUI-Emoji-MIT.txt](licenses/FluentUI-Emoji-MIT.txt)), Material Symbols(Apache-2.0) |
| 그림 | 사용자가 생성형 AI 디자인 도구로 직접 제작 ([licenses/ASSETS.md](licenses/ASSETS.md)) |
| 라이브러리 | AndroidX·Jetpack Compose·Hilt·Room·Retrofit·OkHttp·Coil·kotlinx.coroutines·ZXing core 3.5.3(QR 인코더) — Apache-2.0. 서버 FastAPI(MIT)·SQLAlchemy(MIT)·Pydantic(MIT)·Alembic(MIT)·httpx(BSD-3) |
| 정차 후 체크리스트 | `content/care` — 일반 안내 12장(11/03 사용자 검수 끝, `reviewed: true`). 근거 Merck Veterinary Manual·RSPCA 페이지는 이름과 직접 열어 확인한 주소만 적고 본문을 옮기지 않음(AVMA는 본문을 읽지 못해 뺌) |
| LLM | Ollama 로컬 모델(요약·제안 문장 다듬기만, 실패하면 템플릿) |

**사용 공공데이터**

| 데이터 | 출처 | 키 없을 때 |
| --- | --- | --- |
| 휴게소·편의시설 | 한국도로공사 휴게소 API (공공데이터포털) | `server/app/data/sample_reststops.json` |
| 반려동물 동반여행 | 한국관광공사 TourAPI `KorPetTourService2` (`areaBasedList2`·`detailPetTour2`): 전국 327곳(사진 225곳, 2026-10-30 적재), 7일 캐시·하루 900건 장부. 실내·대형견은 글에 명시된 경우만 필터(그 밖은 "정보 없음"), 동반 조건은 원문 그대로. 시·도별 [docs/plan/public_data.md](docs/plan/public_data.md) | `server/app/data/sample_pet_places.json` ("샘플" 표시) |
| 동물병원 | 지방행정 인허가 데이터(동물병원) 파일 `server/data/raw/*.csv` (영업 중 반려동물 병원 5,063곳, 농장 동물 병원 224곳 제외) | `server/app/data/sample_vets.json` |
| 고갯길 시연 경로 | OpenStreetMap 지방도 456호선(대관령 옛길) — `scenarios/geo/mountain_road.gpx` | 저장소에 포함 (`python scripts/gen_demo_data.py --fetch-osm`으로 다시 받기) |

**지도 데이터 출처:** 고갯길 시연 경로는 © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/). 같은 표기가 GPX 메타데이터와 앱 설정 화면 "데이터 출처"에 있다. 이 GPX(파생 데이터베이스)는 ODbL을 따른다.

</details>

<details>
<summary><b>참고 자료</b></summary>

- 아키텍처: [architecture.md](architecture.md) · 흐름도: [flowchart.md](flowchart.md) · API: [api_spec.md](api_spec.md)
- DB: [database.md](database.md) · 테스트: [test_plan.md](test_plan.md) · 일정: [todo.md](todo.md)
- 기능 상세: [docs/features.md](docs/features.md) · 화면 전체: [docs/screens.md](docs/screens.md) · 기술 메모: [docs/tech_notes.md](docs/tech_notes.md)
- 상태·문구 기준: [docs/states.md](docs/states.md) · [docs/wording.md](docs/wording.md) · 기능 범위: [docs/plan/CONCEPT.md](docs/plan/CONCEPT.md) · 쉼표에서 다시 쓴 것: [docs/plan/REUSE_MAP.md](docs/plan/REUSE_MAP.md)

</details>
