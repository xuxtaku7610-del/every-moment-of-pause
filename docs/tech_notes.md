# 기술 메모 — 빌드 설정·Pleos SDK 확인 결과

README에서 옮긴 개발 기록(2026-11-03 README 개편). SDK 사용표는 README "Pleos SDK·차량 신호 사용표", SDK 점검 표는 [plan/sdk_check.md](plan/sdk_check.md).

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
- 주의: NaviHelper 3.0.5는 호환성이 깨지는 변경이 있다 (`onNavigationStatus` 인자 타입 변경, 좌표 인자 순서 위도·경도로 통일, `totalDistance/totalTime` → `remainDistance/remainTime`). M1에서 aar로 실제 시그니처를 확인한다 ([todo.md](../todo.md) M1 항목).
- compileSdk: 공식 샘플 앱과 같이 **35**로 빌드한다. compileSdk는 빌드에 쓰는 API 버전일 뿐이라 targetSdk 34로 API 34 에뮬레이터에서 실행하는 데 문제없다.

**aar 실제 확인 결과 (M1, 2026-10-28).** Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`의 `ai.pleos.playground:Vehicle:2.3.2`, `NaviHelper:3.0.5`(+ `vehicle-data 1.1.11`, `navi-data 3.0.4`)를 javap로 확인하고, Pleos Connect Emulator 2.1.0(vehicle 서비스 2.3.22, navi 서비스 3.0.4)에서 실행했다.

| 항목 | 결과 |
| --- | --- |
| 기어 | `Vehicle.getGear().registerGearSelection(GearSelectionListener)` → `VehicleGear.GEAR_PARK/…`. 에뮬레이터에서 `PARK` 수신. 권한 `pleos.car.permission.CAR_DRIVE`. **SDK에서 deprecated** ("use the Google GEAR_SELECTION property") |
| 배터리 SoC | `getEvBattery().registerEvBatteryStateOfCharge` + `getEvBatteryStateOfCharge` → 100% 수신. 권한 `CAR_ENERGY` |
| 공조 | `getHVAC().register/getHvacTemperature·HvacAcState·HvacRecirculationState(SeatArea, …)`. 영역은 `checkHvac*Capabilities`가 주는 지원 영역을 써야 함(`ROW_1_LEFT` 가정 시 오류). 에뮬레이터: 내기순환 수신, A/C는 읽기 미지원, 온도는 `PropertyNotAvailableException`. 권한 `CAR_CLIMATE` |
| 문 (11/02) | `Vehicle.getDoor(): Door` — `checkDoorPositionCapabilities((CapabilityConfig, Map<DoorArea, AreaBounds<Int>>) -> Unit, onFailed)`, `getCurrentDoorPosition(DoorArea, (Int?) -> Unit, onFailed)`, `registerDoorPosition/unregisterDoorPosition(DoorArea, DoorPositionListener)`, `DoorPositionListener.onPositionUpdated(Int?)`, `DoorArea(vararg DoorValue)`, `DoorValue.ROW_1_LEFT…ROW_3_RIGHT·HOOD·REAR`. 읽기·등록 권한 `pleos.car.permission.CAR_DOORS`(normal). 에뮬레이터 지원 영역 6곳. 위치 값의 뜻은 문서에 없음 → 0만 닫힘(실차 확인 예정) |
| **속도** | **Vehicle SDK에 없음** (`VehicleSpeedListener` 제거). NaviHelper에도 없음 |
| **충전 중 여부** | `EvChargeState` 상수는 있으나 공개 API에 노출 안 됨 |
| 권한 오류 | 권한이 없으면 `onFailed`가 아니라 `SecurityException`을 바로 던진다 → 어댑터가 신호별로 잡아 "미지원" 처리 |
| NaviHelper | `NaviHelper(context).initialize(onSuccess, onError)`, 리스너 `NaviHelperEventListener`. 남은 거리·시간은 `DestinationInfo/WaypointInfo.remainDistance(m)·remainTime(초)` (3.0.5 변경 확인). `onNavigationStatus(NavigationStatus)` (INACTIVE·INITIALIZING·FREE_DRIVE·ROUTE_DRIVE). 경유지 추가 `addWaypoint(RequestWaypointInfo(위도, 경도, 이름, WaypointIndex, poiId, address, poiSubId, poiProvider))` — **위도가 먼저**. 권한 `pleos.car.permission.NAVI_ROUTE` |
| **경로 좌표열** | NaviHelper에 폴리라인 API 없음. 현재 위치(`CurrentLocationInfo`)와 목적지 좌표만 있음 |
| CarUxRestrictions | AAOS `android.car` (`Car.createCar`, `CAR_UX_RESTRICTION_SERVICE`, `registerListener`) 시그니처 확인. 에뮬레이터가 user 빌드라 adb로 주행 상태 주입 불가 → 상태 전환은 미확인 |

**Pleos Playground 현황 (2026-10-27).** 사용 가능: Vehicle State(11개 항목), NaviHelper. Coming Soon: TTS, STT, FusedLocation, Vehicle Control. LLM SDK는 Playground v2.6.0 릴리스 노트에서 중단. → **음성은 Android 기본 STT/TTS가 1순위**이고, Pleos STT/TTS가 열리면 교체한다. 문장 생성은 서버 경유 Ollama.

**주행 안전 규칙.** 화면 상태는 둘로 나눈다. `DriveState`(CarUxRestrictions에서만 나옴: PARKED / DRIVING / RESTRICTED)가 **어디까지 허용할지**를, `AppMode`(PREPARE / DRIVING / PAUSE / ARRIVED)가 **무엇을 보여줄지**를 정한다. DriveState가 DRIVING이나 RESTRICTED가 되면 AppMode와 상관없이 즉시 주행 UI로 전환한다. 주행 중에는 키보드·영상·자동 애니메이션을 금지하고 텍스트 2줄·버튼 1개만 쓴다. 벽난로 같은 애니메이션은 AppMode = PAUSE에서만 허용한다. 자세한 정의는 [architecture.md §5](../architecture.md#5-상태-모델--drivestate와-appmode).

**DB 테스트 정책.** 테스트는 두 층으로 나눈다. 마이그레이션 검증과 통합 테스트는 PostgreSQL 16에서 돌린다(로컬은 `docker compose`, CI는 GitHub Actions service container). 단위 테스트는 SQLite in-memory에서 돌리며, 모델은 `JSON().with_variant(JSONB(), "postgresql")`, `BigInteger().with_variant(Integer, "sqlite")`처럼 방언별 타입을 쓴다. PostgreSQL 전용 테스트는 `@pytest.mark.pg` 마커로 구분하고, DB에 연결할 수 없으면 skip한다.

**minSdk 28 근거.** Pleos 공식 문서 [Setup Pleos Connect Emulator](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)는 "Android API level 28 or higher"(서명 키 로테이션·lineage 지원)를 요구한다. 예전 설정 가이드에는 "API 26 이상"이라고 적힌 페이지도 있었다. 두 문서가 서로 다르므로 더 엄격한 28을 따른다.
