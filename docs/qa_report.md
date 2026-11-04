# QA 점검표 — 같이가개 test_plan P0 (2026-10-29, 저녁 갱신)

## 내비 전체 화면에서 계속 동작 (v10-1, 2026-11-04 05:36~05:41, 에뮬레이터 Pleos_Connect_API34, 디버그 빌드, pet-trip)
| 확인 | 결과 | 근거 |
| --- | --- | --- |
| 여행 중 포그라운드 서비스 | **동작** — 출발(시뮬레이터 ACTIVE) 직후 `TripService` isForeground=true, types=0x40000080(specialUse \| microphone, 앱이 보일 때 시작) | `dumpsys activity services com.emop.pause`, logcat `PauseService: foreground type=1073741952 mic=true` |
| 내비 전체 화면(맨 위 활동 = `ai.umos.maps…navigation.app`)에서 예보 음성 | **나옴** — 05:40:13 `requestAudioFocus … callingPack=com.emop.pause USAGE_ASSISTANT` (TTS 시작) | logcat MediaFocusControl |
| 상주 알림 | 제목 "같이가개가 콩이와의 이동을 챙기고 있어요", 본문이 뒤에서 말한 예보로 바뀜 "5km 뒤 굽은 길 15분, 천천히 가면 덜 흔들려요." | `dumpsys notification --noredact` |
| heads-up(위쪽 잠깐 뜨는 알림) | **쓰지 않음** — AAOS는 일반 앱 heads-up을 통화·메시지·내비 범주에만 허용(developer.android.com/training/cars/notifications). 음성 + 알림 센터 한 줄 | 공식 문서 (docs/BLOCKED.md 11/04) |
| 돌아오면 상태 이어짐 | 예보 카드·남은 거리·관찰 신고 그대로 | 캡처 `artifacts/ui/v10/bgcheck.png` |
| 도착(여행 끝) | 서비스 끝남(ServiceRecord 0) | `dumpsys activity services` |
| 주의 | 앱의 사용자(user 10)에 알림 권한이 없으면 알림은 안 보이고 서비스·음성만 돈다(`pm grant` 기본 user 0이라 처음엔 importance=NONE). 출발 버튼에서 마이크와 함께 묻는다 | `dumpsys notification` AppSettings |
| 뒤에서 8초 듣기 | 확인 못 함 — 에뮬레이터 마이크 입력 없음(BLOCKED 10/30). 마이크 유형 서비스는 앱이 보일 때만 시작되므로 자동 시작(앱이 뒤)이면 앱이 다시 보일 때 붙인다 | 실차 확인 예정(F11-09) |
| 운전대 음성 키 | `input keyevent KEYCODE_VOICE_ASSIST` → 시스템 어시스턴트 `ai.umos.ambientai`가 받음, 앱 안 옴 → BLOCKED | logcat `[GAA] reason=assist` |

## PT 리허설 연속 3회 — 문 이벤트 빌드 (2026-11-02 09:10~09:14, 재시작 없이)
- 빌드: 디버그 1.0.0 + 문 신호(pet-trip 문 키프레임 6개). AVD RAM은 4096MB 그대로(3072MB는 에뮬레이터가 최소 4096MB로 올려 적용 불가 — BLOCKED 11/02). `demo_check.ps1` 점검 8줄 OK.

| 회차 | 시작 | 걸린 시간(초, 벽시계) | 장면 합 | 출발 체크 | 고속도로 | PT1 고갯길 비교 | PT2 이상 → 병원 사전 접수 | PT3 쓸수록 맞춰짐 | 결과 | 끝난 뒤 PC 남은 메모리 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 09:10:18 | 77 | 77 | 20 | 3 | 11 | 29 | 14 | 통과 | 7.9GB |
| 2 | 09:11:35 | 68 | 67 | 11 | 3 | 9 | 29 | 15 | 통과 | 8.1GB |
| 3 | 09:12:43 | 66 | 66 | 12 | 3 | 9 | 28 | 14 | 통과 | 7.8GB |

- 실패 0회. 1회차 출발 체크 20초는 콜드 부팅 직후 첫 실행(앱 첫 시작). 게스트 MemAvailable 약 2.0GB/4GB. 로컬 기록 `artifacts/rehearsal5/`.

## PT 리허설 연속 3회 (2026-11-02 06:47~06:52, 재시작 없이)
- 빌드: 디버그 APK(versionName 1.0.0, `ReplyGate` 감시 8초) 새로 설치 직후. 서버·DB·Ollama(gemma4:12b) 켜짐, `scripts/demo_check.ps1 -Prepare -Install` 점검 8줄 OK.
- 명령: `python scripts/rehearsal.py` 3번 이어서(앱·에뮬레이터 재시작 없음). 장면별 캡처·시간은 로컬 `artifacts/rehearsal4/`.

| 회차 | 시작 | 걸린 시간(초, 벽시계) | 장면 합 | 출발 체크 | 고속도로 | PT1 고갯길 비교 | PT2 이상 → 병원 사전 접수 | PT3 쓸수록 맞춰짐 | 결과 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 06:47:53 | 69 | 70 | 15 | 2 | 11 | 28 | 14 | 통과 |
| 2 | 06:49:02 | 76 | 76 | 11 | 2 | 11 | 29 | 23 | 통과 |
| 3 | 06:50:18 | 86 | 86 | 13 | 3 | 17 | 31 | 22 | 통과 |

- 실패 0회. 회차가 갈수록 PT1·PT3가 느려졌다(14 → 23초). 화면 글자를 기다리는 시간이라 실패는 아니지만, 그동안 PC 남은 메모리가 7.8GB → 3.3GB로 줄었다(Chrome 약 7GB). 발표 전에는 앱을 새로 띄우고 브라우저 탭을 줄인다.
- 이어서 장면 점프 20회(`python scripts/rehearsal.py --jumps 20`): 통과(110초, 마지막 쉼표 추천이 "듣고 있어요" 뒤 버튼까지).
- 같은 날 이전 빌드(감시 9초)에서도 3회 연속 통과: 71·68·67초 (docs/PROGRESS.md).


- **고유 테스트 수 (2026-11-04 v10-3, 기획서에 넣을 값): 앱 369개 · 서버 88개, 모두 통과.** (v10-3: 여러 마리 core/model +2, 체크 기억·폰 기록 app +2 → app 144, 서버 기록·사진·보호자 2명·정리 +6. v10-2: 흔들림 그래프 값·비교 수치 app +3 → app 142. v10-1: 안내 빈도·자동 시작·말하기 app +8, 조용히·안 타요 말 voice +1 → app 139 · voice 31. 11/03 UI v9 353 — UI v9 마무리: 내장 실제 데이터 app +3. UI v9: 주변 추천 app +10 − 도착 전 산책 3(주변 추천으로 합침), 배변 임박 engine/planner +4, 쇼핑 분류 서버 +1. 그 전 11/03: 긴급 도움 작은 수정 core/uistate +1, 시연 주행 안내 띠 app +7) 앱은 모듈별로 한 빌드 변형만 센다(안드로이드 모듈은 `testDebugUnitTest`, JVM 모듈은 `test`) — `./gradlew test`는 안드로이드 모듈을 디버그·릴리스로 두 번 돌려 결과 파일 합이 513이 되지만 같은 테스트다. 내역: app 131 · engine/pet 40 · engine/planner 41 · voice 30 · engine/motion 23 · core/uistate 20 · data 18 · core/model 11 · engine/fatigue 11 · engine/needs 10 · sim 7 · core/vehicle 4 · engine/pause 4 · core/restriction 3 (소스의 `@Test` 326개 + 매개변수 테스트 1개 14경우). 서버 `pytest --collect-only` 82.
- (이전 기록) 앱 단위 테스트: `./gradlew test` **243 통과, 실패 0** (debug, 10/29). 서버: `pytest` **61 통과**(PostgreSQL 3개 포함, Docker Desktop 켬).
- 근거: **자동** = 단위·시나리오 테스트(이름에 ID), **에뮬레이터** = `Pleos_Connect_API34`에서 시뮬레이터 `pet-trip`으로 확인(캡처는 로컬 `artifacts/pet/`, 저장소에 없음), **미검증** = 사람 확인이 필요.
- 화면은 최소 레이아웃(디자인 v5 대기). 모양 항목은 다시 본다.

| ID | 항목 | 근거 | 결과 |
| --- | --- | --- | --- |
| F1-01 | 직선 정속 → 낮음 | 자동 `MotionComfortEngineTest` | 통과 |
| F1-02 | 연속 커브 → 분마다 상승 | 자동 `MotionComfortEngineTest` | 통과 |
| F1-03 | 정차 → 감소 | 자동 `MotionComfortEngineTest` | 통과 |
| F1-04 | 민감 프로필 > 보통 | 자동 `MotionComfortEngineTest` | 통과 |
| F1-05 | 예보 시작 거리 ±10%·지속 ±15% | 자동 `MotionComfortEngineTest`(예보는 상태 불변 포함) | 통과 |
| F1-06 | 대역 필터 (0.02~0.5Hz, 결정 10/29) | 자동 `MotionComfortEngineTest` | 통과 |
| F1-07 | 고갯길 안내대로 < 안내 없이 | 자동 `DemoCompareTest` → [plan/demo_compare.md](plan/demo_compare.md) (OSM 대관령 옛길 55 → 30) | 통과 |
| F1-08 | 형상·방위 없음 → 종방향만, 예보 없음 | 자동 `MotionComfortEngineTest`, `TripBrainPetTest` | 통과 |
| F1-11 | 형상 두 구현(GPX vs 회전 안내·방위) | 자동 `TbtRouteShapeTest`, `ShapeProviderCompareTest` → [plan/shape_compare.md](plan/shape_compare.md) (14곳 중 12곳) | 통과 · 실차 TBT 미검증 |
| F1-12 | 짧은 굽이 건너뜀·30초 병합 | 자동 `ForecastSegmentTest` · 에뮬레이터 `artifacts/pet2/05-forecast`("3.2km 뒤 굽은 길 12분") | 통과 |
| F1-10 | 예보 한 줄·같은 구간 한 번·10분 쿨다운 | 자동 `TripBrainPetTest`, `PetTripScenarioTest`, `MotionPhrasesTest` · 에뮬레이터 `05-forecast` | 통과 |
| F2-01 | 멀미 주의 → 가까운 놀이터 휴게소 | 자동 `PetRestPlannerTest` | 통과 |
| F2-02 | 배변 간격 시각 맞춤 | 자동 `PetRestPlannerTest` | 통과 |
| F2-03 | 이유 기여도 순 1~2개 | 자동 `PetRestPlannerTest` | 통과 |
| F2-04 | 히스테리시스·쿨다운 | 자동 `PetRestPlannerTest` | 통과 |
| F2-05 | "여기서 쉴게요" → 경유지 | 자동 `VoiceQueueTest`(의도) · 에뮬레이터 `02b-suggest`(카드·버튼, 에뮬레이터 STT 없음 → 버튼 시트) | 로직 통과 · 실제 내비 경유지 반영은 미검증 |
| F2-08 | 출발 미리보기 = 주행 첫 추천 | 자동 `PetTripScenarioTest` · 에뮬레이터 `artifacts/pet2/01-depart-preview`(횡성휴게소) | 통과 |
| F2-07 | 서버 없음 → 내장 휴게소·"샘플" | 자동 `RestStopRepositoryTest`, `EmptyStatesTest` · 에뮬레이터(오프라인 줄) | 통과 |
| F3-01 | P단 "잠깐 다녀올게" → 대기 화면 | 자동 `PetEngineTest`(WaitMode), `VoiceQueueTest` · 에뮬레이터 `04-wait` | 통과 |
| F3-02 | 경과·배터리 변화 | 자동 `PetEngineTest` · 에뮬레이터 | 통과 |
| F3-03 | 제한 초과 1번 → 강조·폰 "시간 초과" | 자동 `PetEngineTest`, `WaitShareTest`, 서버 `test_f3_03_overdue_page_is_red` | 통과 |
| F3-04 | P 해제·주행 → 즉시 종료 | 자동 `PetEngineTest`, `ScreenRouterTest` | 통과 |
| F3-05 | P단 아니면 시작 불가 | 자동 `PetEngineTest` | 통과 |
| F3-07 | 서버 공유·폰 페이지 | 자동 서버 `test_wait`, 앱 `WaitShareTest` | 통과 · 실제 폰 열람은 미검증 |
| F3-08 | 토큰 무작위·2시간 만료·위치 필드 422 | 자동 서버 `test_wait` | 통과 |
| F4-01 | 키 없음 → 샘플 적재·표시 | 자동 서버 `test_places_vets` | 통과 |
| F4-02 | 경로 버퍼 검색 | 자동 서버 `test_places_vets`, 앱 `PetPlaceRepositoryTest` | 통과 |
| F4-03 | 도착 10분 전 산책 제안 1번 | 자동 `ArrivalWalkTest` · 에뮬레이터 `artifacts/pet2/05b-arrival-walk` | 통과 |
| F5-01 | 샘플 병원 가까운 순, 전화번호 없음 | 자동 서버·앱 | 통과 |
| F6-01 | 체크 3개 → 출발 | 자동 `PetEngineTest`, `EmptyStatesTest` · 에뮬레이터 `01-depart` | 통과 |
| F6-02 | 도착 P단 60초 → "내렸나요?" 1번 | 자동 `PetEngineTest` · 에뮬레이터 `06-arrival` | 통과 |
| F6-03 | 26℃↑·여름 → 강한 알림 | 자동 `PetEngineTest`, `EmptyStatesTest` · 에뮬레이터(시나리오 27℃) | 통과 |
| F7-01 | 이동 시간·멀미 추이·휴식 | 자동 `TripRecorderTest` | 통과 |
| F7-02 | 한계 시간 중앙값 | 자동 `PetEngineTest` | 통과 |
| DS-01 | 주행 상태면 즉시 주행 화면 | 자동 `ScreenRouterTest` | 통과 |
| DS-02 | 주행 화면 2줄·버튼 ≤2 | 자동 `DrivingRulesTest`, `DrivingUiScenarioTest`(pet-trip 포함 4개 시나리오 전 구간) | 통과 |
| DS-03 | RESTRICTED → 한 줄 | 자동 `DrivingUiScenarioTest` | 통과 |
| DS-04 | 프로필·설정은 정차 중에만 | 자동 `ScreenRouterTest`, `UxPolicyTest` | 통과 |
| SV-01 | `/health` | 자동 서버 `test_health` | 통과 |
| SV-02 | `alembic upgrade head` (0003) | PostgreSQL 테스트 3개 + 0001↔0003 왕복 + 샘플 장소 22·병원 10 적재 | 통과 |
| SV-03 | 토큰 없으면 401 | 자동 서버 | 통과 |
| DM-01 | 장면 점프 → 기대 AppMode | 자동 `DemoScenesTest` | 통과 |
| DM-04 | 점프 직후 쉼표 화면 휴게소 이름 | 자동 `TripRecorderTest` · 에뮬레이터 `artifacts/pet2/03-rest-name`(횡성휴게소에서 쉬는 중) | 통과 |
| DM-02 | 패널 장면 버튼·비교 결과 | 코드 · 에뮬레이터 패널 열림 | 로직 통과 · 화면 대기 |

## 실명·키 노출 검사 (2026-10-29)
- 범위: 추적 + 새 파일 280개(이미지·글꼴·jar 제외)와 커밋 작성자.
- 실명·개인 이메일(gmail·naver·daum): **없음**. 커밋 작성자는 `xuxtaku7610-del <…@users.noreply.github.com>` 하나.
- API 키 형태(serviceKey=, AIza, sk-, `*_API_KEY=값`, password=): **없음**. 키는 `server/.env`(gitignore)와 환경 변수로만.
- 전화번호 형태: `server/tests/test_places_vets.py`의 `033-000-0000` 하나 — 테스트용 가짜 번호. 샘플 병원에는 전화번호를 넣지 않았다.
- `server/.env`, `app-android/local.properties`: gitignore 확인, 추적 0개.
