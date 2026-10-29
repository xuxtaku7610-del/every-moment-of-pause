# 같이가개 — 반려견과 차로 가는 모든 순간

> 주행 데이터로 반려견의 멀미·피로를 미리 예측해 **쉬는 순간과 장소**를 정해 주고, 잠깐 차에 남겨 둘 때부터 내릴 때까지 반려견의 이동 하루를 차가 함께 챙기는 Pleos Connect 차량용 앱.

현대자동차그룹 차량용 앱 개발 공모전 출품작 (대학(원)생 부문, 개인). 가칭이며 이전 주제 「쉼표」에서 전환했다(엔진·서버·차량 연동 재사용, [docs/plan/REUSE_MAP.md](docs/plan/REUSE_MAP.md)). 기능 범위 기준: [docs/plan/CONCEPT.md](docs/plan/CONCEPT.md).

---

## 1. 프로젝트 개요

### 문제
- 반려견은 아파도 말을 못 한다. 차멀미(침 흘림·헐떡임·구토)와 이동 불안이 흔한데, 운전자는 앞만 봐야 해서 알아채기 어렵다.
- 반려견 동반 여행 수요는 크지만 차 안에서 쓰는 전용 서비스는 없다. 2026 Pleos 아이디어 공모전 우수작 16개에도 반려동물 주제는 없다.
- 해외 참고: Tesla Dog Mode(정차 중 반려견 대기 화면), JLR의 주행 데이터 기반 멀미 예측, Apple Vehicle Motion Cues.

### 해결
| 단계 | 같이가개가 하는 일 |
| --- | --- |
| 출발 전 (P단) | 콩이 프로필(체급·나이·멀미 성향), 하네스/켄넬·물·배변봉투 체크 |
| 달릴 때 | **멀미 예보**: 속도·방위·앞 구간 경로 형상으로 반려견 멀미 누적량을 계산하고 "3km 뒤 굽은 길 8분, 조금 일찍 감속" |
| 달릴 때 | **반려견 맞춤 쉼표**: 멀미 누적·이동 시간·배변 간격·운전자 피로를 합쳐 반려견 놀이터가 있는 휴게소 1곳, 음성 "여기서 쉴게요" → 경유지 |
| 잠깐 내릴 때 (P단) | **잠깐 대기 모드**: 차 화면이 바깥 사람용 안내로 바뀌고, 보호자 폰 웹페이지에 경과 시간·배터리 |
| 가는 길·목적지 | **함께 가는 장소**(한국관광공사 반려동물 동반여행), **동물병원 가는 길** |
| 도착 | **"콩이 내렸나요?"** → **이동 기록**: "콩이는 약 1시간 10분이 한계" |

### 심사 항목과의 연결 (대응표)
| 심사 항목 | 근거 | 확인 방법 |
| --- | --- | --- |
| 차량 특화성 | 속도(가속도)·방위·기어·주차 브레이크·배터리·경로 남은 거리를 동시에 읽어야 가능한 판단. 대기 모드는 P단·배터리를 아는 차 화면에서만 가능 | 설정 "이 차에서 쓰는 신호", 발표 모드 신호 줄, [docs/plan/sdk_check.md](docs/plan/sdk_check.md) |
| 창의성 | "운전자 피로"가 아니라 **말 못 하는 동승자(반려견)의 멀미를 미리 예보**. 휴식 장소를 반려견 기준으로 고르고, 차 화면을 바깥 사람용 안내판으로 바꿈 | 시연 대본 [docs/demo_script.md](docs/demo_script.md) 장면 2~4 |
| 기술 활용성 | Vehicle SDK, NaviHelper SDK, AAOS 차량 속성, CarUxRestrictions, Android STT/TTS, ISO 2631-1 멀미 누적량(MSDV) 단순화 모델, 로컬 LLM(Ollama), 공공데이터 3종 | [docs/plan/motion_model.md](docs/plan/motion_model.md), 고갯길 비교 [docs/plan/demo_compare.md](docs/plan/demo_compare.md), 테스트 [test_plan.md](test_plan.md) |

---

## 2. 핵심 기능

| ID | 기능 | 쓰는 차량 신호 | 쉼표에서 가져온 것 |
| --- | --- | --- | --- |
| F1 | 멀미 예보 주행 | 속도(가속도 추정), 방위, 경로 형상·남은 거리, 기어 | 신호 잡음 처리, AppMode/DriveState, 쿨다운 |
| F2 | 반려견 맞춤 쉼표 + 음성 경유지 추가 | 속도·주행 시간, 배터리, NaviHelper | 휴게소 추천 엔진·ScoringPolicy·니즈 타이머·피로 지수·음성 |
| F3 | 잠깐 대기 모드 + 보호자 폰 웹페이지 | 기어 P, 배터리, 주차 브레이크, (가능하면) 실내 온도 | 서버, 발표 모드 패널 |
| F4 | 함께 가는 장소 (TourAPI 반려동물 동반여행) | 남은 시간·목적지 | 서버 수집·캐시 |
| F5 | 동물병원 가는 길 | NaviHelper, 속도 | F1 엔진 |
| F6 | 타고·내릴 때 체크 ("콩이 내렸나요?") | 기어 P, 주행 종료 판단 | 출발 준비 화면 |
| F7 | 콩이 이동 기록 (한계 시간·병원용 요약) | 주행 기록 | 여행 리포트, TripRecorder, TripResume, Ollama 문장+템플릿 |

**뺀 것(쉼표에서):** 아이 원정대, 도착 맞춤 라디오, "아직 멀었어?", 파워냅.

공통: 주행 안전(CarUxRestrictions가 DRIVING/RESTRICTED면 항상 주행 화면, 텍스트 2줄·버튼 2개 + 음성 힌트, 스크롤·자동 애니메이션 없음), 오프라인 우선(서버·LLM이 없어도 내장 데이터·템플릿), 멀미 지수는 의학적 진단이 아닌 "주의 예보".

### 사용 공공데이터
| 데이터 | 출처 | 키 없을 때 |
| --- | --- | --- |
| 휴게소·편의시설 | 한국도로공사 휴게소 API (공공데이터포털) | `server/app/data/sample_reststops.json` |
| 반려동물 동반여행 | 한국관광공사 TourAPI (공공데이터포털) | `server/app/data/sample_pet_places.json` ("샘플" 표시) |
| 동물병원 | 지방행정 인허가 데이터(동물병원) 파일 | `server/app/data/sample_vets.json` |

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
│  ├─ engine/motion/            # F1 멀미 예보 (MotionComfortEngine)
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
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 8748 --es action play         # 고갯길 3km 앞 멀미 예보
  adb shell am start -n com.emop.pause/.MainActivity --ei seek 10820 --es action play        # 도착 → 60초 뒤 "콩이 내렸나요?"
  adb shell am start -n com.emop.pause/.MainActivity --ez vehicle true                       # 실차 신호(Pleos SDK·AAOS 속성)
  ```
  릴리스에서도 로고를 5번 누르면 발표 모드(장면 점프·고갯길 비교) 패널이 열린다. 시연 대본: [docs/demo_script.md](docs/demo_script.md).
- 장면별 차량 값 + 앱 장면 맞추기: `powershell -ExecutionPolicy Bypass -File scripts/vhal.ps1 <depart|highway|rest_stop|wait|mountain_road|arrive>` (user 빌드 에뮬레이터는 값 주입이 막혀 있어 앱 시뮬레이터로 맞춘다, [docs/vhal_check.md](docs/vhal_check.md)).
- 시연 데이터 다시 만들기: `python scripts/gen_demo_data.py` (합성 고갯길 GPX·시나리오·샘플). 고갯길 비교: `./gradlew :app:testDebugUnitTest --tests "*DemoCompareTest*"` → [docs/plan/demo_compare.md](docs/plan/demo_compare.md).

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
