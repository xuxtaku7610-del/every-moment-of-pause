# 에뮬레이터 차량 상태(VHAL) 점검 — 쉼표

점검일 2026-10-29 · AVD `Pleos_Connect_API34` (API 34) · 스크립트 `scripts/vhal.ps1` · 원자료 `artifacts/vhal/`

## 결론
- **이 에뮬레이터는 user 빌드라 스크립트로 차량 값을 넣을 수 없다.** `cmd car_service`의 모든 하위 명령이 막히고, `adb root`도 거부된다.
- 앱 쪽 연동은 동작한다. 앱이 Pleos SDK와 AAOS 속성에서 신호를 신호 단위로 골라 받고, 출처를 기록한다(아래 "앱이 받은 신호").
- 장면별 값 바꾸기는 사람이 에뮬레이터 UI에서 하거나 userdebug 이미지가 필요하다 → `docs/BLOCKED.md`. 그동안 시연은 시뮬레이터 시나리오로 한다(`--es sim seoul-busan-family`).

## 확인한 것

| 확인 | 명령 | 결과 |
| --- | --- | --- |
| 빌드 종류 | `adb shell getprop ro.build.type` | `user` |
| car_service 도움말 | `adb shell cmd car_service -h` | `SecurityException: The command '-h' requires non-user build` |
| 값 읽기 | `cmd car_service get-property-value 291504647` | 같은 SecurityException |
| 값 넣기 | `cmd car_service inject-vhal-event 291504647 10` | 같은 SecurityException |
| root | `adb root` | `adbd cannot run as root in production builds` |
| 에뮬레이터 콘솔 | `adb emu help` | 차량 관련 명령 없음 (`sensor`, `geo`, `power` 등만) |
| 에뮬레이터 gRPC | `emulator/lib/*.proto` | 차량 속성 API 없음 |
| 속성 설정 | `adb shell dumpsys car_service --services VehicleHal` | 아래 표 (`artifacts/vhal/dumpsys_vhal.txt`) |

## 속성 (ID는 `android.car.jar` `VehiclePropertyIds` 상수, 설정은 dumpsys)

| 속성 | ID | access | 비고 | 앱 권한 |
| --- | --- | --- | --- | --- |
| PERF_VEHICLE_SPEED | 291504647 | READ | float m/s, 샘플링 10~20Hz | `CAR_SPEED` (dangerous) |
| GEAR_SELECTION | 289408000 | READ | 지원 값 [4 P, 1 N, 2 R, 8 D, 16~256 1~5단] | `CAR_POWERTRAIN` |
| CURRENT_GEAR | 289408001 | READ | 지원 값 [4, 1, 2, 16~256] | `CAR_POWERTRAIN` |
| PARKING_BRAKE_ON | 287310850 | READ | boolean | `CAR_POWERTRAIN` |
| SEAT_BELT_BUCKLED | 354421634 | READ_WRITE | 좌석 영역 1(ROW_1_LEFT), 4 … | (앱은 읽지 않음) |
| EV_BATTERY_LEVEL | 291504905 | READ | float Wh | `CAR_ENERGY` (dangerous) |
| INFO_EV_BATTERY_CAPACITY | 291504390 | READ | float Wh, 고정값 | `CAR_INFO` |
| EV_CHARGE_STATE | 289410881 | READ | int, EvChargeState 1 충전 중·2 완충·3 충전 안 함 | `CAR_ENERGY` (dangerous) |

권한 보호 수준은 `adb shell dumpsys package permission …`로 확인했다. **CAR_ENERGY도 dangerous**라서 앱이 CAR_SPEED와 함께 런타임에 묻는다.
AAOS는 다중 사용자라 앱이 사용자 10으로 돈다. `pm grant`는 `--user 10`(= `am get-current-user`)으로 줘야 한다.

## 앱이 받은 신호 (실차 모드 `--ez vehicle true`, logcat `PauseSignals`)

```
speed=0.0(AAOS_PROPERTY) gear=PARK(PLEOS_SDK) brake=false(AAOS_PROPERTY) soc=100(PLEOS_SDK) charging=false(AAOS_PROPERTY) drive=PARKED mode=PREPARE
```

- AaosPropertySource 지원: `[SPEED, GEAR, PARKING_BRAKE, CHARGING]`. 배터리 %는 INFO_EV_BATTERY_CAPACITY를 읽지 못해 AAOS 쪽에서는 빠지고 Pleos SDK 값을 쓴다.
- 합친 결과: 기어·배터리·공조는 Pleos SDK가 우선, SDK에 없는 속도·주차 브레이크·충전 상태는 AAOS 속성. 런처 상단 표시(0 km/h, P, 100%)와 같다.
- 캡처: `artifacts/vhal/vehicle_signals_demo_panel.png` (발표 모드 패널의 신호 줄에 출처 표시).

## 장면별 목표 값 (`scripts/vhal.ps1 <장면>`)

| 장면 | 기어 | 속도 | 주차 브레이크 | 안전벨트(운전석) | 배터리 | 충전 상태 | 기대 AppMode |
| --- | --- | --- | --- | --- | --- | --- | --- |
| drive | D (8) | 27.8 m/s (100 km/h) | 0 | 1 | 62% | 3 충전 안 함 | DRIVING (여행 시작 후) |
| pause | P (4) | 0 | 1 | 0 | 55% | 3 | PAUSE (rest) |
| charge | P (4) | 0 | 1 | 0 | 34% | 1 충전 중 | PAUSE (charging) |
| arrive | P (4) | 0 | 1 | 0 | 21% | 3 | (도착은 내비 목적지 도착으로 판단) |

| 장면 | 캡처 | 앱이 받은 신호 | 판정 |
| --- | --- | --- | --- |
| drive | `artifacts/vhal/drive.png` | 값 변화 없음 (위 로그와 같음) | 값 주입 불가 → 보류 |
| pause | `artifacts/vhal/pause.png` | 같음 | 보류 |
| charge | `artifacts/vhal/charge.png` | 같음 | 보류 |
| arrive | `artifacts/vhal/arrive.png` | 같음 | 보류 |

캡처는 VHAL 값을 바꾸지 못한 상태(P, 0 km/h, 100%)라 네 장 모두 같은 출발 준비 화면이다.

## 다음
1. (사용자) 에뮬레이터 창의 확장 컨트롤에 차량 데이터 패널이 있는지 확인하고, 있으면 `vhal.ps1 <장면>`을 `-NoWait` 없이 실행한 뒤 안내대로 값을 바꾼다. 결과 로그를 이 표에 붙인다.
2. userdebug AAOS 이미지가 있으면 `adb shell cmd car_service -h`로 `inject-vhal-event` 형식을 먼저 확인한다. 스크립트의 주입 경로는 AOSP 형식을 가정했고 이 에뮬레이터에서는 확인하지 못했다.
