# Pleos SDK 재확인 — 같이가개 (CONCEPT 4장 입력)

확인일 2026-10-29 · 방법: Gradle 캐시의 aar → `classes.jar` 풀기 → `javap`(필요하면 `javap -v`로 권한 어노테이션).
대상: `ai.pleos.playground:Vehicle:2.3.2`(+ `vehicle-data 1.1.11`), `NaviHelper:3.0.5`(+ `navi-data 3.0.4`). 여기 없는 이름은 코드에 쓰지 않는다.

## 결론
- **경로 좌표열(폴리라인)은 여전히 없다.** 멀미 예보에 필요한 "앞 구간 곡률"은 SDK만으로 바로 얻을 수 없다.
- 대신 쓸 수 있는 것이 새로 보인다: ① `onTBTInfo`의 회전 종류·남은 거리 목록(앞쪽 커브 힌트), ② `RouteStateInfo.trafficStatus`의 정체 구간(시작 오프셋·길이·평균 속도 → 급제동 예보), ③ `CurrentLocationInfo.heading`(1초 간격 방위 변화 = 실제 요 레이트 → a_y = v·ψ̇), ④ Vehicle `SteeringWheel.registerSteeringWheelAngle`(조향각 → 횡가속 보조), ⑤ `onCautionPointInfo`(급커브는 없지만 낙석·내리막·좁은 길 등 주의 지점과 남은 거리).
- 그래서 F1은 **"현재 = 측정(속도 Δv + 방위 변화), 앞 구간 = 형상 소스"**로 나눈다. 형상 소스 우선순위: (1) 시연 GPX(경로 고정 시연) → (2) TBT 회전 목록으로 만든 근사 커브 → (3) 없음(현재 측정값만으로 누적, 예보는 "정보 없음"). 실차에서 (2)는 에뮬레이터로 값이 오는지 아직 확인 못 함 → `TODO(pleos-sdk)`. **구현(10/29 저녁):** `RouteShapeProvider` — (1) `RouteShape`(OSM GPX), (2) `TbtRouteShape`(회전 안내 원호 + 지나온 1.5km 방위 무늬 반복). 실차 모드에서 GPX가 없으면 (2)를 쓴다. 비교: [shape_compare.md](shape_compare.md).
- 속도는 Pleos SDK에 없고 AAOS `PERF_VEHICLE_SPEED`(이미 사용 중)로 받는다.

## 항목별

| 필요한 것 | SDK에 있나 | 확인한 시그니처 (javap) | 권한 | 같이가개에서 쓰는 곳 / 대체 |
| --- | --- | --- | --- | --- |
| 경로 좌표(형상) | **없음** | NaviHelper 공개 메서드에 폴리라인 없음. `requestTrip(TripRequest)` → `onTripInfo(TripInfo)`의 `TripSummary.getRoutePoints(): List<RoutePointInfo>`는 출발·경유지·목적지(`RoutePointType` ORIGIN/WAYPOINT/DESTINATION)와 `ratioOnRoute`만 | NAVI_ROUTE | F1 앞 구간 → **시연 GPX**(`scenarios/geo/*.gpx`) · 시뮬레이터 |
| 앞쪽 회전 | 있음(근사) — **에뮬레이터 확인 2026-10-30** | `getTBTInfo()` → `onTBTInfo(List<TBTInfo>)`, `TBTInfo(type: TurnType, distance: Double, description: String)`. 내비 앱(`ai.umos.maps.android.navigation.app`)에서 성남→강릉 안내를 걸면 경로 전체 32건이 한 번에 온다(앱이 앞에 있을 때, 약 10초마다 다시). **`distance`는 앞 안내 지점에서의 구간 거리**(32건 합 188.5km ≈ 경로 194km) → `RouteTurn.fromSegments`로 누적. 실제로 온 종류: LEFT·RIGHT·KEEP_L/R·ENTER_IC_R/C·EXIT_IC_R·ENTER_OVER_L·ENTER_UNDER_L·TOLL·ENTER_SERVICEAREA·SERVICE_AREA·RESTAREA·REST_AREA·ARRIVE(굽이 아닌 것은 누적에만) | NAVI_ROUTE | F1 형상 소스 (2): 회전 종류 → 가정 반경으로 곡률 근사. 고속도로·지방도 연속 커브는 목록에 없다(실제 목록에 "굽은 길" 종류 없음) → 방위 무늬 추정이 필요 |
| 정체(급제동) 구간 | 있음 | `getRouteStateInfo()` → `onRouteStateInfo(RouteStateInfo)`, `getTrafficStatus(): TrafficStatus` → `getCongestionItemList(): List<TrafficCongestionInfo>` (`offset`, `distance`, `avrSpeed`, `trafficCongestionType` FAST/MODERATE/SLOW/HEAVY, `roadName`) · `TripSummary.trafficConditions: List<TrafficInfo>(congestionType, startRatio, endRatio)` | NAVI_ROUTE | F1 종방향 예보(가다 서다 구간) |
| 주의 지점 | 있음 | `onCautionPointInfo(List<CautionPoint>)`, `CautionPoint(id, type: CautionPointType, remainDistance: Int)`. 타입에 급커브 없음(FALLING_ROCK_AREA, DOWNHILL_CAUTION_AREA, NARROW_ROAD, SPEED_BUMP …) | NAVI_ROUTE | F1 보조(과속방지턱·내리막 → 가중) |
| 현재 위치·방위 | 있음 | `getCurrentLocationInfo()` → `CurrentLocationInfo(latitude, longitude, heading: Double, address, routeId, currentRouteIndex)` | NAVI_ROUTE | F1 현재 횡가속 a_y = v·Δheading/Δt. 위치는 서버로 보내지 않음 |
| 남은 거리·시간 | 있음 (기존) | `DestinationInfo.remainDistance(m)·remainTime(초)`, `WaypointInfo` 동일, `onDrivingInfo(DrivingInfo(destination: DrivingState(name, duration, distance, …), waypoints))` | NAVI_ROUTE | F2 예상 도착, F4 "도착 10분 전" |
| 도착 | 있음 | `onDestinationArrived(DestinationArrivedInfo(duration, distance, name, address))`, `onWayPointArrived(WaypointArrivedInfo)` | NAVI_ROUTE | F6 도착 체크·F7 기록 |
| 경유지 추가 | 있음 (기존) | `addWaypoint(RequestWaypointInfo(lat, lng, name, WaypointIndex, poiId, address, poiSubId, poiProvider))` | NAVI_ROUTE | F2·F4·F5 |
| 목적지 안내 | 있음 | `requestRoute(RouteInfo(latitude, longitude, displayLat?, displayLng?, poiName, poiId, address, poiSubId, routeOption, poiProvider, isMobileRequest))` | NAVI_ROUTE | F5 동물병원 안내 (에뮬레이터 확인 전 `TODO(pleos-sdk)`) |
| 제한 속도 | 있음 | `onCurrentRoadSpeedLimit(int)` | NAVI_ROUTE | (선택) 고갯길 감속 안내 문구 근거 |
| 속도 | **Vehicle SDK·NaviHelper에 없음** | — | — | AAOS `PERF_VEHICLE_SPEED` (`AaosPropertySource`, CAR_SPEED dangerous) |
| 기어 | 있음 (deprecated) | `Vehicle.getGear().registerGearSelection(GearSelectionListener)` | CAR_DRIVE | F3 P단·F6 주행 종료. AAOS `GEAR_SELECTION`도 이미 읽음 |
| 배터리 | 있음 | `getEvBattery().registerEvBatteryStateOfCharge` | CAR_ENERGY | F3 대기 중 배터리 변화 |
| 연료 | 있음 | `getFuel().registerFuelPercent(FuelPercentListener)` → `onFuelPercentUpdated(Integer)` | CAR_ENERGY | F3 내연기관 차 대기 표시. 에뮬레이터 값 미확인 |
| 주차 브레이크 | **Vehicle SDK에 없음** (Brake는 페달만: `registerBrakePedalStatus`) | — | CAR_DRIVE | AAOS `PARKING_BRAKE_ON` (기존) |
| 실내 온도 | 있음 | **측정값**: `checkHvacTemperatureCurrentCapabilities((CapabilityConfig, List<SeatArea>))` → `registerHvacTemperatureCurrent(SeatArea, HvacTemperatureListener)`·`getHvacTemperatureCurrent(SeatArea, TemperatureUnit, …)` (javap Vehicle 2.3.2, 10/30 재확인). `getHvacTemperature`·`registerHvacTemperature`·`setHvacTemperature`는 **설정 온도** — 실내 온도로 쓰면 안 된다(10/30 이전 어댑터가 이것을 읽고 있었음, 고침) | CAR_CLIMATE | F3 "실내 확인 중" 표시. **에뮬레이터는 온도에서 `PropertyNotAvailableException`**(M1) → 없으면 "온도 정보 없음"으로 정직하게 |
| 도어 | 일부 | `getDoor()`: `registerDoorLockState(DoorArea, DoorLockStateListener)`, `registerDoorPosition(DoorArea, DoorPositionListener)` → `onPositionUpdated(Integer)` | CAR_DOORS | F6 "내렸나요?" 보조(뒷문 열림 여부). 기본은 사용 안 함 |
| 좌석 착석 | 있음 | `getSeat().registerSeatOccupancy(SeatArea, SeatOccupancyListener)` → `onSeatOccupancyUpdated(Boolean)` | CAR_SEATS | F6 보조. 반려견 체중으로는 감지 안 될 수 있어 **판단에 쓰지 않음**(표시만) |
| 안전벨트 | 있음 | `registerSeatBeltBuckledState(SeatArea, …)` | CAR_SEATS | 쓰지 않음 |
| 조향각 | 있음 | `getSteeringWheel().registerSteeringWheelAngle(SteeringWheelAngleListener)` → `onSteeringWheelAngleUpdated(Float)` | CAR_DRIVE | F1 현재 횡가속 보조. 단위(도, 핸들각)와 조향비 미확인 → 방위 변화를 우선 |
| 운전 시간 | 있음 | `getOdometer().registerTripCurrentDrivingTime(DrivingTimeListener)` → `onDrivingTimeUpdated(Integer)` | CAR_DRIVE | F2·F7 이동 시간 교차 확인 (앱 자체 계산이 기본) |

## AAOS (android.car, 이미 사용 중 — docs/vhal_check.md)
`PERF_VEHICLE_SPEED`, `GEAR_SELECTION`, `PARKING_BRAKE_ON`, `EV_BATTERY_LEVEL`, `EV_CHARGE_STATE`. 에뮬레이터가 user 빌드라 **값 주입은 막혀 있다**(BLOCKED 10/29). `PERF_STEERING_ANGLE`도 표준 속성이지만 이 에뮬레이터 VHAL 목록은 아직 확인하지 않음.

## 대체 방법 (시연)
1. **시뮬레이터 장면**(`scenarios/`): 속도·기어·주차 브레이크·배터리·헤딩을 1초 단위로 재생. F1은 장면 `mountain_road`에 시연 GPX를 붙여 곡률을 계산한다.
2. **시연 GPX**: 공개 도로 형상을 단순화해 직접 만든 좌표(지도 서비스 데이터를 복사하지 않음). 출처·가공 방법은 GPX 주석과 `docs/plan/motion_model.md`.
3. **에뮬레이터 VHAL**: `scripts/vhal.ps1`이 장면별 목표 값을 보여 주고, user 빌드에서는 사람이 확장 컨트롤로 넣는다.
