# 같이가개 — 반려견과 차로 가는 모든 순간

> 차량 흔들림(주행 자극)과 이동 시간·배변 간격·운전자 피로로 **쉬는 순간과 장소**를 정해 주고, 잠깐 차에 남겨 둘 때부터 내릴 때까지 반려견의 이동 하루를 차가 함께 챙기는 Pleos Connect 차량용 앱.

현대자동차그룹 차량용 앱 개발 공모전 출품작 (대학(원)생 부문, 개인). 가칭이며 이전 주제 「쉼표」에서 전환했다(엔진·서버·차량 연동 재사용, [docs/plan/REUSE_MAP.md](docs/plan/REUSE_MAP.md)). 기능 범위 기준: [docs/plan/CONCEPT.md](docs/plan/CONCEPT.md).

---

## 1. 프로젝트 개요

### 문제
- 반려견은 아파도 말을 못 한다. 차멀미(침 흘림·헐떡임·구토)와 이동 불안이 흔한데, 운전자는 앞만 봐야 해서 알아채기 어렵다.
- 반려견 동반 여행 수요는 크지만 차 안에서 쓰는 전용 서비스는 없다. 2026 Pleos 아이디어 공모전 우수작 16개에도 반려동물 주제는 없다.
- 해외 참고: Tesla Dog Mode(정차 중 반려견 대기 화면), JLR의 주행 데이터 기반 멀미 예측(사람 대상), Apple Vehicle Motion Cues.
- 같이가개는 반려견 몸을 재지 않는다. 엔진 값은 **주행 자극**(차량 흔들림 누적, ISO 2631-1 단순화)이고, 반려견 상태는 보호자 음성 신고로만 남기는 **콩이 관찰 상태**다. 두 값을 섞지 않는다([docs/states.md](docs/states.md), [docs/wording.md](docs/wording.md)).

### 해결
| 단계 | 같이가개가 하는 일 |
| --- | --- |
| 출발 전 (P단) | 콩이 프로필(체급·나이·흔들림에 예민한 정도(보호자 응답)), 하네스/켄넬·물·배변봉투 체크 |
| 달릴 때 | **주행 자극 예보**: 속도·방위·앞 구간 경로 형상으로 차량 흔들림 누적을 계산하고 숫자 없이 한 줄 "3km 뒤 굽은 길 8분, 천천히 가면 덜 흔들려요." 길 안내는 Pleos 기본 내비, 같이가개는 옆 분할 창에서 다음 회전·남은 거리·도착(NaviHelper 실제 값) |
| 달릴 때 | **반려견 맞춤 쉼표**: 주행 자극 누적·이동 시간·배변 간격·운전자 피로를 합쳐 반려견 놀이터가 있는 휴게소 1곳, 음성 "여기서 쉴게요" → 경유지 |
| 잠깐 내릴 때 (P단) | **잠깐 대기 모드**: 차 화면이 바깥 사람용 안내로 바뀌고, 보호자 폰 웹페이지에 경과 시간·배터리 |
| 가는 길·목적지 | **함께 가는 장소**(한국관광공사 반려동물 동반여행), **동물병원 가는 길** |
| 달릴 때 | **"콩이가 이상해요"**: "콩이 토했어" 같은 음성 신고를 기록하고 1줄 안내, 심하면 동물병원(사전 접수는 시연용 제휴 동물병원만, 보내기 전 동의) |
| 도착 | **"콩이 내렸나요?"** → **도착 후 기록 리포트**: 시간순 타임라인, 주행 자극 그래프(차량 기준), 관찰 신고 |

### 화면 (v8 재정비, Pleos Connect 에뮬레이터 2026-11-01 · UI 동결 11/8)
주행 장면은 **실제 분할 창**(왼쪽 같이가개 1/3 + 오른쪽 Pleos 기본 내비)으로 찍었다. 기본 내비가 강릉씨티호텔로 안내 중이고, 다음 회전 카드("좌회전 80m · 기업지원허브 방면")는 NaviHelper `onTBTInfo` 실제 값이다(내비 화면과 같다). 주행·예보·쉼표·대기·리포트 숫자는 **시뮬레이터 실제 엔진 값**, 프로필·장소·병원 목록·온보딩·주행 자극 보기는 발표 모드 장면. 앱 영역만 잘랐다(왼쪽 차량 패널·아래 공조 바는 Pleos 시스템 UI). 기준 문서는 [docs/states.md](docs/states.md)·[docs/wording.md](docs/wording.md), 캡처 방법은 [docs/recording.md](docs/recording.md)·`python scripts/ui_capture.py` → `artifacts/ui/v8/`.

| 출발 전 (전체 창) | 주행 · 기본 내비 옆 1/3 | 굽은 길 예보 (숫자 없음) |
| --- | --- | --- |
| ![출발 전](docs/screens/v8/01-depart.png) | ![주행](docs/screens/v8/02-drive-nav.png) | ![예보](docs/screens/v8/03-forecast-nav.png) |
| **쉼표 추천 (1/3 창, 음성 실패 시 버튼)** | **콩이가 이상해요 (음성 신고)** | **병원 가는 길 (사전 접수 보냄)** |
| ![쉼표 추천](docs/screens/v8/04-suggest-third.png) | ![콩이가 이상해요](docs/screens/v8/07-distress-nav.png) | ![병원 가는 길](docs/screens/v8/15-going-vet-nav.png) |
| **쉼표 (정차)** | **잠깐 대기 · 보호자 폰 QR** | **동물병원 · 시연용 제휴 사전 접수** |
| ![쉼표](docs/screens/v8/05-rest.png) | ![잠깐 대기](docs/screens/v8/06-wait.png) | ![동물병원](docs/screens/v8/08-vets.png) |
| **실제 병원 (전화 · 도착 예정 링크 QR)** | **병원 접수함 (폰, 시연용)** | **도착 체크** |
| ![실제 병원](docs/screens/v8/08c-vets-call.png) | ![병원 접수함](docs/screens/v8/08b-clinic-inbox.png) | ![도착 체크](docs/screens/v8/09-arrival.png) |
| **도착 후 기록 리포트** | **리포트 (점프 뒤 실시간 신고 유지 · 보정 뒤에도 최고 55)** | **주행 자극 보기 (정차 중)** |
| ![리포트](docs/screens/v8/10-report.png) | ![리포트 보정 뒤](docs/screens/v8/10b-report-calibrated.png) | ![주행 자극 보기](docs/screens/v8/13-stimulus.png) |
| **반려견 프로필** | **온보딩** | **고갯길 비교 (발표 모드)** |
| ![프로필](docs/screens/v8/11-pet.png) | ![온보딩](docs/screens/v8/11-onboarding.png) | ![고갯길 비교](docs/screens/v8/12-compare.png) |

QR 주소의 `http://192.168.0.10:8000`은 캡처용 예시 주소다(`--es shareBase`로 노트북의 같은 Wi-Fi 주소를 넣는다). 이전 캡처는 `docs/screens/v6/`(재정비 전)·`docs/screens/v5/`(분할 창 첫 확인)에 이력으로 남겼다.

### 실차 확인 필요 (2026-10-30 기준)
에뮬레이터에서 확인한 것과 실차에서만 확인할 수 있는 것을 나눈다. 시연은 앱 시뮬레이터(시나리오)와 발표 모드로 한다.

| 항목 | 에뮬레이터 결과 | 실차에서 확인할 것 |
| --- | --- | --- |
| NaviHelper 회전 안내 `onTBTInfo` | **확인함** — 성남→강릉 안내 32건 수신. `distance`는 구간 거리라 누적해서 씀. 회전 안내만으로 예보하는 모드는 시뮬레이터 테스트로 허용 오차 통과(docs/plan/shape_compare.md) | 주행하며 남은 거리가 갱신되는지, 고갯길 굽이가 목록에 나오는지 |
| 실내 온도 측정값 | 에뮬레이터는 값 없음 → 온도 표시 숨김·여름 일반 문구 확인 | `getHvacTemperatureCurrent` 값과 단위, 대기 중 갱신 주기 |
| 위치 따라 안내 재생 | 안 됨 — `geo fix`가 내비 위치를 바꾸지 않음(Pleos 위치 서비스 별도) | 실제 주행으로 예보 시작 거리 |
| 차량 값(기어·속도·벨트)·Car data 패널 | 시도 중단(결정) — 에뮬레이터 user 빌드 | AAOS 속성·Pleos Vehicle SDK 값이 앱 "이 차에서 쓰는 신호"에 뜨는지 |
| 앱이 뒤에 있을 때 콜백 | 전체 화면에서 내비가 앞이면 앱 프로세스가 내려가 콜백 끊김. **런처 분할 창에서는 확인함(10/31)** — 기본 내비가 앞일 때 `onTBTInfo` 32건·`onDestinationInfo`·`onWayPointInfo` 수신 | 실제 주행 중 갱신 주기 |
| 분할 창 1/2·1/3 | **확인함** — `am task resize`로 1280·853dp, 앱 영역 오른쪽 끝 손잡이 끌기 `adb shell input swipe 2547 720 1700 720 800` | 사용자가 런처에서 직접 나누는 동작 |
| 음성 인식 | 에뮬레이터 마이크 없음 → 버튼 대체 확인 | 차 마이크로 "여기서 쉴게요" |

### 심사 항목과의 연결 (대응표)
| 심사 항목 | 근거 | 확인 방법 |
| --- | --- | --- |
| 차량 특화성 | 속도(가속도)·방위·기어·주차 브레이크·배터리·경로 남은 거리를 동시에 읽어야 가능한 판단. 대기 모드는 P단·배터리를 아는 차 화면에서만 가능 | 설정 "이 차에서 쓰는 신호", 발표 모드 신호 줄, [docs/plan/sdk_check.md](docs/plan/sdk_check.md) |
| 창의성 | "운전자 피로"가 아니라 **말 못 하는 동승자(반려견)를 흔드는 길을 미리 예보**(차량 기준, 반려견 측정 아님). 휴식 장소를 반려견 기준으로 고르고, 차 화면을 바깥 사람용 안내판으로 바꿈 | 시연 대본 [docs/demo_script.md](docs/demo_script.md) 장면 2~4 |
| 기술 활용성 | Vehicle SDK, NaviHelper SDK, AAOS 차량 속성, CarUxRestrictions, Android STT/TTS, ISO 2631-1 흔들림 누적량(MSDV) 단순화 모델(주행 자극), 로컬 LLM(Ollama), 공공데이터 3종 — **실제 적재(2026-10-30): 반려동물 동반 장소 327곳**(TourAPI 전국 17개 시·도, 사진 225곳, 시·도별 [docs/plan/public_data.md](docs/plan/public_data.md)) · **동물병원 5,063곳**(인허가 파일 영업 중, 농장 동물 병원 224곳 제외) · 휴게소 30곳(도로공사 키 없음 → 샘플) | [docs/plan/motion_model.md](docs/plan/motion_model.md), 고갯길 비교 [docs/plan/demo_compare.md](docs/plan/demo_compare.md)(OSM 실제 도로, 주행 자극 −45% (55 → 30)), 테스트 [test_plan.md](test_plan.md) |

---

## 2. 핵심 기능

| ID | 기능 | 쓰는 차량 신호 | 쉼표에서 가져온 것 |
| --- | --- | --- | --- |
| F1 | 주행 자극 예보 주행 (숫자 없이 한 줄) | 속도(가속도 추정), 방위, 경로 형상·남은 거리, 기어 | 신호 잡음 처리, AppMode/DriveState, 쿨다운 |
| F2 | 반려견 맞춤 쉼표 + 음성 경유지 추가 | 속도·주행 시간, 배터리, NaviHelper | 휴게소 추천 엔진·ScoringPolicy·니즈 타이머·피로 지수·음성 |
| F3 | 잠깐 대기 모드 + 보호자 폰 웹페이지("돌아가는 중", 실내 온도가 오르면 배너) | 기어 P, 배터리, 주차 브레이크, 실내 온도 **측정값**(`getHvacTemperatureCurrent`, 없으면 숨김) | 서버, 발표 모드 패널 |
| F4 | 함께 가는 장소 (TourAPI 반려동물 동반여행, 전국 327곳, 대형견·실내 필터는 명시된 곳만) | 남은 시간·목적지 | 서버 수집·캐시 |
| F5 | 동물병원 가는 길 + 증상 음성 메모 + **병원 사전 접수**(시연용 제휴 동물병원만. 전달 동의 — 주행 중 음성 "네", 정차 중 "동의하고 보내기" — 뒤 병원 접수함 `/clinic/…`에 증상·메모·오늘 이동, 도착 예정 자동 갱신, 병원 응답 "도착하면 바로 볼게요"가 차 화면·음성으로. 실제 공공데이터 병원은 전화·도착 예정 링크만, "접수 완료" 없음) + 보호자 도착 예정 공유(위치 없이, 2시간) | NaviHelper, 속도 | F1 엔진 |
| F6 | 타고·내릴 때 체크 ("콩이 내렸나요?") | 기어 P, 주행 종료 판단 | 출발 준비 화면 |
| F7 | 도착 후 기록 리포트 (시간순 타임라인·주행 자극 그래프·병원용 요약, 위치는 남기지 않고 거리만) + **개인 맞춤 보정**(음성 신고로 자동: 구토 = 예, 침·헐떡임 = 조금, 높음 구간 뒤 30분 조용함 = 아니요(가중치 0.4). 3회부터) | 주행 기록 | 여행 리포트, TripRecorder, TripResume, Ollama 문장+템플릿 |
| F8 | 온보딩 3단계 (반려견 프로필 → 차량 연동 신호 확인 → 흔들림에 예민한 정도 (보호자 응답)) | 쓰는 신호 목록 | — |
| F9 | **"콩이가 이상해요"** — 주행 중 "헥헥거려·토했어·낑낑대·축 처졌어"를 말하면 규칙(증상 + 실내 온도·계절·출발 뒤 시간·주행 자극)으로 1줄 안내 + 가장 가까운 정차 장소(응급은 동물병원, 제휴 병원이면 동의 후 사전 접수), 정차 후 체크리스트(검수 전) | 실내 온도 측정값, 속도·경로, NaviHelper | 음성, 휴게소·병원 데이터 |

**뺀 것(쉼표에서):** 아이 원정대, 도착 맞춤 라디오, "아직 멀었어?", 파워냅.

공통: 주행 안전(CarUxRestrictions가 DRIVING/RESTRICTED면 항상 주행 화면, 텍스트 2줄·버튼 2개 + 음성 힌트, 스크롤·자동 애니메이션 없음), 오프라인 우선(서버·LLM이 없어도 내장 데이터·템플릿), 주행 자극은 차량 흔들림 기준이며 반려견 측정·진단이 아니다. 주행 중에는 숫자·그래프 없이 예보 한 줄만, 숫자는 정차 중 "주행 자극 보기"·도착 후 리포트·발표 모드 비교에서만.

### 사용 공공데이터
| 데이터 | 출처 | 키 없을 때 |
| --- | --- | --- |
| 휴게소·편의시설 | 한국도로공사 휴게소 API (공공데이터포털) | `server/app/data/sample_reststops.json` |
| 반려동물 동반여행 | 한국관광공사 TourAPI `KorPetTourService2` (`areaBasedList2`·`detailPetTour2`): 전국 327곳, 7일 캐시·하루 900건 장부. 실내·대형견은 글에 명시된 경우만 필터(그 밖은 "정보 없음"), 동반 조건은 원문 그대로 | `server/app/data/sample_pet_places.json` ("샘플" 표시) |
| 동물병원 | 지방행정 인허가 데이터(동물병원) 파일 `server/data/raw/*.csv` (영업 중 반려동물 병원 5,063곳) | `server/app/data/sample_vets.json` |
| 고갯길 시연 경로 | OpenStreetMap 지방도 456호선(대관령 옛길) — `scenarios/geo/mountain_road.gpx` | 저장소에 포함 (`python scripts/gen_demo_data.py --fetch-osm`으로 다시 받기) |

**지도 데이터 출처:** 고갯길 시연 경로는 © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, [ODbL 1.0](https://opendatacommons.org/licenses/odbl/1-0/). 같은 표기가 GPX 메타데이터와 앱 설정 화면 "데이터 출처"에 있다. 이 GPX(파생 데이터베이스)는 ODbL을 따른다.

---

## 3. 기술 스택

| 영역 | 기술 | 비고 |
| --- | --- | --- |
| 차량 앱 | Kotlin 2.x, Jetpack Compose, Hilt, Coroutines/Flow, Room | Android Automotive OS (Pleos Connect), minSdk 28[^minsdk] |
| 차량 연동 | Pleos Connect Vehicle SDK, NaviHelper SDK, `CarUxRestrictionsManager` | 둘 다 신청 없이 사용 가능 |
| 음성 | **Android `SpeechRecognizer` / `TextToSpeech` (현재 1순위)**, Pleos STT/TTS가 열리면 교체 | 공급자 추상화 |
| 백엔드 | Python 3.11, FastAPI, SQLAlchemy 2, Alembic, Pydantic v2 | 휴게소·동반 장소·동물병원 데이터, 대기 공유, LLM 게이트웨이 |
| DB | PostgreSQL 16 (단위 테스트만 SQLite in-memory) | Docker Compose, 아래 DB 테스트 정책 참고 |
| LLM | Ollama `gemma4:12b` (집 PC RTX 5060 Ti 16GB) | 네이티브 `/api/chat`, `think=false`, `keep_alive=30m`, `num_predict=60`, `temperature=0.3`, 서버 시작 시 warm-up, 최대 2문장, 템플릿 문장 폴백 |
| 외부 데이터 | 한국도로공사 휴게소 API, 한국관광공사 TourAPI, 지방행정 인허가(동물병원) | 공공데이터포털 인증키 (없으면 샘플) |
| 테스트 | JUnit5, MockK, Turbine, Compose UI Test / pytest, httpx | |
| 빌드·배포 | Gradle 8, GitHub Actions(APK 빌드), GitHub Releases | 1차 접수 가산점: 소스 + APK |

**빌드 설정 (확정).**

| 항목 | 값 |
| --- | --- |
| Gradle JDK | Temurin 21 (`JAVA_HOME`). Android Studio Settings › Build Tools › Gradle › Gradle JDK에서 지정. `org.gradle.java.home` 같은 PC별 경로는 저장소에 넣지 않음 |
| Kotlin `jvmTarget` / Java `compileOptions` | 17 |
| compileSdk / targetSdk / minSdk | 35 / 34 / 28 |
| Vehicle SDK | 2.3.2 |
| NaviHelper SDK | 3.0.5 |
| Pleos Connect Emulator | 2.1.0 (AVD 시스템 이미지 API 34, Android 14) |

**SDK 호환성 확인 결과 (2026-10-27, 공식 릴리스 노트 기준).**
- [SDK 릴리스 노트](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-261015) (2026-10-19): NaviHelper **v3.0.5**와 Vehicle SDK **v2.3.1**이 실려 있다. Vehicle SDK 2.3.2는 릴리스 노트에 없고, [Vehicle SDK 샘플 앱 가이드](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk-sample-app)에 "baseline … Vehicle SDK 2.3.2 and Vehicle Service 2.3.22"로만 나온다.
- [Emulator 릴리스 노트](https://document.pleos.ai/en/docs/pleos-only/release-note/emulator-261015) (2.1.0, 2026-10-19): "에뮬레이터에 포함된 SDK 버전 업데이트로 API 지원 범위 확대"라고만 적혀 있고, 어떤 SDK 버전과 맞는지는 **명시되지 않았다**. 샘플 앱 가이드도 "버전 표시가 호환성을 보장하지 않는다"고 한다.
- 결론: 세 버전이 같은 날(10/19) 나온 묶음이라 함께 쓰는 것으로 보지만, 공식 호환표는 없다. M1에서 에뮬레이터에서 신호 수신·경유지 추가를 직접 확인해 확정한다.
- 주의: NaviHelper 3.0.5는 호환성이 깨지는 변경이 있다 (`onNavigationStatus` 인자 타입 변경, 좌표 인자 순서 위도·경도로 통일, `totalDistance/totalTime` → `remainDistance/remainTime`). M1에서 aar로 실제 시그니처를 확인한다 ([todo.md](todo.md) M1 항목).
- compileSdk: 공식 샘플 앱과 같이 **35**로 빌드한다. compileSdk는 빌드에 쓰는 API 버전일 뿐이라 targetSdk 34로 API 34 에뮬레이터에서 실행하는 데 문제없다.

**aar 실제 확인 결과 (M1, 2026-10-28).** Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`의 `ai.pleos.playground:Vehicle:2.3.2`, `NaviHelper:3.0.5`(+ `vehicle-data 1.1.11`, `navi-data 3.0.4`)를 javap로 확인하고, Pleos Connect Emulator 2.1.0(vehicle 서비스 2.3.22, navi 서비스 3.0.4)에서 실행했다.

| 항목 | 결과 |
| --- | --- |
| 기어 | `Vehicle.getGear().registerGearSelection(GearSelectionListener)` → `VehicleGear.GEAR_PARK/…`. 에뮬레이터에서 `PARK` 수신. 권한 `pleos.car.permission.CAR_DRIVE`. **SDK에서 deprecated** ("use the Google GEAR_SELECTION property") |
| 배터리 SoC | `getEvBattery().registerEvBatteryStateOfCharge` + `getEvBatteryStateOfCharge` → 100% 수신. 권한 `CAR_ENERGY` |
| 공조 | `getHVAC().register/getHvacTemperature·HvacAcState·HvacRecirculationState(SeatArea, …)`. 영역은 `checkHvac*Capabilities`가 주는 지원 영역을 써야 함(`ROW_1_LEFT` 가정 시 오류). 에뮬레이터: 내기순환 수신, A/C는 읽기 미지원, 온도는 `PropertyNotAvailableException`. 권한 `CAR_CLIMATE` |
| **속도** | **Vehicle SDK에 없음** (`VehicleSpeedListener` 제거). NaviHelper에도 없음 |
| **충전 중 여부** | `EvChargeState` 상수는 있으나 공개 API에 노출 안 됨 |
| 권한 오류 | 권한이 없으면 `onFailed`가 아니라 `SecurityException`을 바로 던진다 → 어댑터가 신호별로 잡아 "미지원" 처리 |
| NaviHelper | `NaviHelper(context).initialize(onSuccess, onError)`, 리스너 `NaviHelperEventListener`. 남은 거리·시간은 `DestinationInfo/WaypointInfo.remainDistance(m)·remainTime(초)` (3.0.5 변경 확인). `onNavigationStatus(NavigationStatus)` (INACTIVE·INITIALIZING·FREE_DRIVE·ROUTE_DRIVE). 경유지 추가 `addWaypoint(RequestWaypointInfo(위도, 경도, 이름, WaypointIndex, poiId, address, poiSubId, poiProvider))` — **위도가 먼저**. 권한 `pleos.car.permission.NAVI_ROUTE` |
| **경로 좌표열** | NaviHelper에 폴리라인 API 없음. 현재 위치(`CurrentLocationInfo`)와 목적지 좌표만 있음 |
| CarUxRestrictions | AAOS `android.car` (`Car.createCar`, `CAR_UX_RESTRICTION_SERVICE`, `registerListener`) 시그니처 확인. 에뮬레이터가 user 빌드라 adb로 주행 상태 주입 불가 → 상태 전환은 미확인 |

**Pleos Playground 현황 (2026-10-27).** 사용 가능: Vehicle State(11개 항목), NaviHelper. Coming Soon: TTS, STT, FusedLocation, Vehicle Control. LLM SDK는 Playground v2.6.0 릴리스 노트에서 중단. → **음성은 Android 기본 STT/TTS가 1순위**이고, Pleos STT/TTS가 열리면 교체한다. 문장 생성은 서버 경유 Ollama.

**주행 안전 규칙.** 화면 상태는 둘로 나눈다. `DriveState`(CarUxRestrictions에서만 나옴: PARKED / DRIVING / RESTRICTED)가 **어디까지 허용할지**를, `AppMode`(PREPARE / DRIVING / PAUSE / ARRIVED)가 **무엇을 보여줄지**를 정한다. DriveState가 DRIVING이나 RESTRICTED가 되면 AppMode와 상관없이 즉시 주행 UI로 전환한다. 주행 중에는 키보드·영상·자동 애니메이션을 금지하고 텍스트 2줄·버튼 1개만 쓴다. 벽난로 같은 애니메이션은 AppMode = PAUSE에서만 허용한다. 자세한 정의는 [architecture.md §5](architecture.md#5-상태-모델--drivestate와-appmode).

**DB 테스트 정책.** 테스트는 두 층으로 나눈다. 마이그레이션 검증과 통합 테스트는 PostgreSQL 16에서 돌린다(로컬은 `docker compose`, CI는 GitHub Actions service container). 단위 테스트는 SQLite in-memory에서 돌리며, 모델은 `JSON().with_variant(JSONB(), "postgresql")`, `BigInteger().with_variant(Integer, "sqlite")`처럼 방언별 타입을 쓴다. PostgreSQL 전용 테스트는 `@pytest.mark.pg` 마커로 구분하고, DB에 연결할 수 없으면 skip한다.

[^minsdk]: Pleos 공식 문서 [Setup Pleos Connect Emulator](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)는 "Android API level 28 or higher"(서명 키 로테이션·lineage 지원)를 요구한다. 예전 설정 가이드에는 "API 26 이상"이라고 적힌 페이지도 있었다. 두 문서가 서로 다르므로 더 엄격한 28을 따른다.

---

## 4. 저장소 구조

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
│  ├─ engine/pet/               # 반려견 프로필 · F3 대기 모드 · F6 체크 · F7 기록 패턴
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
├─ scenarios/                   # 시뮬레이터 장면 JSON · 시연 GPX
├─ scripts/                     # vhal.ps1 (에뮬레이터 차량 값), 비교 실험
├─ docs/plan/                   # CONCEPT · REUSE_MAP · sdk_check · motion_model · demo_compare
└─ docker-compose.yml
```

---

## 5. 실행 방법

### 사전 준비
- Android Studio (최신 안정판) + Temurin 21. `JAVA_HOME`을 Temurin 21로 두고 Settings › Build Tools › Gradle › Gradle JDK에서 지정 (PC별 경로는 커밋하지 않음)
- Android SDK Platform 35 (compileSdk)
- Pleos Playground 가입 → 프로젝트 생성 → **Pleos Connect Emulator 2.1.0** 시스템 이미지(API 34) 설치 및 AVD 설정
- Docker Desktop, Python 3.11
- Ollama 설치 + `ollama pull gemma4:12b`
- 공공데이터포털 한국도로공사 휴게소 API 인증키 (선택: 없으면 수집 잡이 `server/app/data/sample_reststops.json` 샘플 데이터로 동작)

### 1) 환경 변수
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

### 2) 백엔드
```bash
docker compose up -d db
cd server
python -m venv .venv
.venv\Scriptsctivate                              # Windows
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

### 3) 차량 앱
```bash
cd app-android
./gradlew -Pkotlin.compiler.execution.strategy=in-process assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
- 서버 주소는 빌드 종류로 정해진다: 디버그 `http://10.0.2.2:8000/api/v1/`(에뮬레이터 → 호스트 PC), 릴리스는 비어 있음 = 오프라인(내장 휴게소·장소·병원 샘플·템플릿).
- 보호자 폰 대기 페이지: 서버를 `--host 0.0.0.0`으로 띄우고, 같은 Wi-Fi의 폰이 `http://<노트북 IP>:8000/wait/<토큰>`을 연다. 차 화면에 보일 주소 앞부분은 `--es shareBase http://<노트북 IP>:8000`.
- 에뮬레이터: Pleos Connect Emulator AVD(`Pleos_Connect_API34`).
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
  adb shell am start -n com.emop.pause/.MainActivity --ez vehicle true                       # 실차 신호(Pleos SDK·AAOS 속성)
  ```
  릴리스에서도 로고를 5번 누르면 발표 모드(장면 점프·고갯길 비교) 패널이 열린다. 시연 대본: [docs/demo_script.md](docs/demo_script.md).
- 장면별 차량 값 + 앱 장면 맞추기: `powershell -ExecutionPolicy Bypass -File scripts/vhal.ps1 <depart|highway|rest_stop|wait|mountain_road|arrive>` (user 빌드 에뮬레이터는 값 주입이 막혀 있어 앱 시뮬레이터로 맞춘다, [docs/vhal_check.md](docs/vhal_check.md)).
- 시연 데이터 다시 만들기: `python scripts/gen_demo_data.py` (시나리오·샘플, `--fetch-osm`이면 OSM 고갯길 GPX도 다시 받기). 고갯길 비교: `./gradlew :app:testDebugUnitTest --tests "*DemoCompareTest*"` → [docs/plan/demo_compare.md](docs/plan/demo_compare.md), 형상 소스 비교(GPX vs 회전 안내·방위): [docs/plan/shape_compare.md](docs/plan/shape_compare.md).

### 4) 테스트
```bash
cd server
.venv\Scripts\python -m pytest                     # PostgreSQL 테스트는 DB가 없으면 건너뜀
cd ../app-android
./gradlew -Pkotlin.compiler.execution.strategy=in-process test
```
테스트 이름에는 [test_plan.md](test_plan.md) 항목 ID(F1-01, DS-02, SV-01 …)를 쓴다. 점검 결과는 [docs/qa_report.md](docs/qa_report.md).

### 5) 릴리스 APK
[docs/release.md](docs/release.md) — 서명 키는 저장소 밖에 두고, 비밀번호는 환경 변수로만 넘긴다.

---

## 6. 개인정보·제출 유의사항
- 앱은 계정 없이 **익명 기기 토큰**만 사용. 반려견 프로필은 기기에만 저장. 위치는 장소 조회에만 쓰고 저장하지 않으며, 대기 공유에는 위치를 보내지 않는다.
- 공모전 규정상 **공유 링크·첨부자료에 개발자 실명·계정정보가 노출되면 안 됨** → GitHub 계정명, 커밋 작성자명, APK 서명 정보, 영상 화면을 점검.
- 1차 접수 가산점: 시연 영상 · GitHub 저장소(소스 + APK 함께). APK만 따로 제출하지 않는다.

## 7. 참고
- 아키텍처: [architecture.md](architecture.md) · 흐름도: [flowchart.md](flowchart.md) · API: [api_spec.md](api_spec.md)
- DB: [database.md](database.md) · 테스트: [test_plan.md](test_plan.md) · 일정: [todo.md](todo.md)
