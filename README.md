# 쉼표 — 멈춤의 모든 순간 (Every Moment of Pause)

> 달릴 때는 **언제, 어디서 멈출지** 먼저 알려주고, 멈췄을 때는 **그 시간을 무엇으로 채울지** 골라주는 Pleos Connect 차량용 앱.

현대자동차그룹 차량용 앱 개발 공모전 출품작 (대학(원)생 부문, 개인)

---

## 1. 프로젝트 개요

### 문제
- 2026년 상반기 고속도로 사망자 중 **졸음·주시태만이 71.6%** (한국도로공사). 운전자는 피로를 늦게 자각하고, 도착 시간 압박으로 휴식을 미룬다.
- 가족 이동에서 "쉬어야 하는 이유"는 운전자 피로만이 아니다. 아이 화장실·수유, 반려동물 산책, 전기차 충전이 제각각 다른 시점에 필요하다. (국내 반려동물 양육 가구 29.2%, 2025 농식품부)
- 막상 멈추면(휴게소·충전·졸음쉼터) 그 시간은 **기다림**이 된다. 아이는 "아직 멀었어?"를 반복하고, 운전자는 제대로 쉬지 못한 채 다시 출발한다.

### 해결
쉼표는 차량 데이터(주행 시간·속도·기어·공조·배터리·충전 상태)와 내비 경로를 함께 읽어 **이동 중 '멈춤'의 모든 순간**을 설계한다.

| 단계 | 쉼표가 하는 일 |
| --- | --- |
| 출발 전 (정차) | 동승자 프리셋(아이·반려동물·교대 운전자), "어젯밤 몇 시간 주무셨어요?" 한마디로 휴식 계획 초안 |
| 달릴 때 (주행) | 피로 위험 지수 + 가족 니즈 타이머 → 경로상 최적 휴게소를 **이유 한 문장과 함께 음성 제안**, "응" 한마디로 경유지 추가 |
| 달릴 때 (뒷좌석) | 아이가 "아직 멀었어?" → "속담 라디오 2편 들으면 도착!" 도착 시간에 맞춘 라디오 편성 |
| 멈췄을 때 (P단) | 남은 정차·충전 시간에 맞춰 **파워냅(벽난로 모드) · 아이 원정대 미션 · 펫 산책 타이머 · 업무 집중** 중 하나를 추천 |
| 도착 후 | 여행 리포트: 휴식 횟수, 최장 연속 주행, 피로 피크, 다음 여행 팁 |

### 심사 항목과의 연결
| 심사 항목 | 쉼표의 근거 |
| --- | --- |
| 차량 특화성 | 연속 주행·공조·배터리·충전·기어·경로를 동시에 읽어야만 가능한 판단. 스마트폰 앱으로는 불가 |
| 창의성 | "경고하는 차"가 아니라 "멈춤을 먼저 계획하고, 멈춘 시간을 채워주는 차". 충전 시간 = 콘텐츠 길이 |
| 기술 활용성 | Vehicle SDK, NaviHelper SDK, CarUxRestrictions, Android 기본 STT/TTS, 로컬 LLM(Ollama), 공공데이터(한국도로공사 휴게소 API). Pleos STT/TTS가 Playground에 열리면 교체 |

---

## 2. 핵심 기능

### MVP (11/15 접수 전 완성 목표)
| ID | 기능 | 상태 |
| --- | --- | --- |
| F1 | 출발 준비: 동승자 프리셋 + 수면 한마디 | 필수 |
| F2 | 피로 위험 지수 v1 (규칙 기반, 기여 요인 공개) | 필수 |
| F3 | 가족 니즈 타이머 (아이·반려동물·충전) | 필수 |
| F4 | 휴게소 추천 + 음성 제안 + 경유지 추가 | 필수 (핵심) |
| F5 | "아직 멀었어?" 응답기 + 도착 맞춤 라디오 | 필수 (시그니처) |
| F6 | 멈춤 큐레이터: 충전 대기 · 파워냅(벽난로) · 원정대 미션 · 펫 산책 | 필수 (시그니처) |
| F7 | 여행 리포트 | 필수 |
| F8 | 시연용 주행 시뮬레이터 (서울→부산 5시간 → 3분 재생) | 필수 (개발·시연용) |

### 확장 (서류 통과 후, 12/3 PT 전)
정체 구간 호흡 코치 · 빗길 위험 가중 · 교대 운전 제안 · 하차 안심 루틴 · 피로 모델 v2(개인화) · 보호자 폰 알림

---

## 3. 기술 스택

| 영역 | 기술 | 비고 |
| --- | --- | --- |
| 차량 앱 | Kotlin 2.x, Jetpack Compose, Hilt, Coroutines/Flow, Room | Android Automotive OS (Pleos Connect), minSdk 28[^minsdk] |
| 차량 연동 | Pleos Connect Vehicle SDK, NaviHelper SDK, `CarUxRestrictionsManager` | 둘 다 신청 없이 사용 가능 |
| 음성 | **Android `SpeechRecognizer` / `TextToSpeech` (현재 1순위)**, Pleos STT/TTS가 열리면 교체 | 공급자 추상화 |
| 백엔드 | Python 3.11, FastAPI, SQLAlchemy 2, Alembic, Pydantic v2 | 휴게소 데이터, 라디오 콘텐츠, LLM 게이트웨이 |
| DB | PostgreSQL 16 (단위 테스트만 SQLite in-memory) | Docker Compose, 아래 DB 테스트 정책 참고 |
| LLM | Ollama `gemma4:12b` (집 PC RTX 5060 Ti 16GB) | 네이티브 `/api/chat`, `think=false`, `keep_alive=30m`, `num_predict=60`, `temperature=0.3`, 서버 시작 시 warm-up, 최대 2문장, 템플릿 문장 폴백 |
| 외부 데이터 | 한국도로공사 공공 API (휴게소 기준정보·편의시설) | 공공데이터포털 인증키 |
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
│  ├─ core/model/               # 도메인 모델
│  ├─ core/vehicle/             # Vehicle SDK 어댑터 (+ capability 확인)
│  ├─ core/navi/                # NaviHelper 어댑터
│  ├─ core/restriction/         # CarUxRestrictions → DriveState
│  ├─ engine/fatigue/           # 피로 위험 지수
│  ├─ engine/needs/             # 가족 니즈 타이머
│  ├─ engine/planner/           # 휴게소 점수화·추천
│  ├─ engine/pause/             # 멈춤 큐레이터
│  ├─ voice/                    # STT·TTS·LLM 공급자
│  ├─ data/                     # Room, Retrofit, 캐시
│  └─ sim/                      # 주행 시나리오 리플레이어
├─ server/                      # FastAPI 백엔드
│  ├─ app/ (api, models, services, jobs)
│  ├─ alembic/
│  └─ tests/
├─ content/                     # 라디오 에피소드·미션 원본 (검수된 JSON)
├─ scenarios/                   # 시뮬레이터 시나리오 JSON
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
python -m venv .venv && .venv\Scripts\activate      # Windows
pip install -r requirements.txt
alembic upgrade head
python -m app.jobs.ingest_reststops                  # 휴게소 데이터 적재 (EX_API_KEY 없으면 샘플)
python -m app.jobs.seed_content                      # 라디오·미션 콘텐츠 적재
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
확인: `http://localhost:8000/docs`

### 3) 차량 앱
1. Android Studio에서 `app-android/` 열기
2. `local.properties`에 `SERVER_BASE_URL=http://10.0.2.2:8000` (에뮬레이터 → 호스트 PC)
3. Pleos Connect Emulator AVD 선택 후 Run
4. 시연: 앱 설정 → **시뮬레이션 모드** → `scenarios/seoul-busan-family.json` 재생

### 4) 테스트
```bash
cd server && pytest
cd app-android && ./gradlew test connectedAndroidTest
```

---

## 6. 개인정보·제출 유의사항
- 앱은 계정 없이 **익명 기기 토큰**만 사용. 위치는 여행 중에만 쓰고 서버에는 휴게소 이벤트 단위로만 저장.
- 공모전 규정상 **공유 링크·첨부자료에 개발자 실명·계정정보가 노출되면 안 됨** → GitHub 계정명, 커밋 작성자명, APK 서명 정보, 영상 화면을 점검.
- 1차 접수 가산점: 시연 영상 · GitHub 저장소(소스 + APK 함께). APK만 따로 제출하지 않는다.

## 7. 참고
- 아키텍처: [architecture.md](architecture.md) · 흐름도: [flowchart.md](flowchart.md) · API: [api_spec.md](api_spec.md)
- DB: [database.md](database.md) · 테스트: [test_plan.md](test_plan.md) · 일정: [todo.md](todo.md)
