# QA 점검표 — 같이가개 test_plan P0 (2026-10-29)

- 앱 단위 테스트: `./gradlew test` **실패 0** (debug 기준 약 230개). 서버: `pytest` **58 통과**, PostgreSQL 전용 3 건너뜀(Docker Desktop 꺼짐 → BLOCKED).
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
| F1-07 | 고갯길 안내대로 < 안내 없이 | 자동 `DemoCompareTest` → [plan/demo_compare.md](plan/demo_compare.md) (37 → 26) | 통과 |
| F1-08 | 형상·방위 없음 → 종방향만, 예보 없음 | 자동 `MotionComfortEngineTest`, `TripBrainPetTest` | 통과 |
| F1-10 | 예보 한 줄·같은 구간 한 번·10분 쿨다운 | 자동 `TripBrainPetTest`, `PetTripScenarioTest`, `MotionPhrasesTest` · 에뮬레이터 `05-forecast` | 통과 |
| F2-01 | 멀미 주의 → 가까운 놀이터 휴게소 | 자동 `PetRestPlannerTest` | 통과 |
| F2-02 | 배변 간격 시각 맞춤 | 자동 `PetRestPlannerTest` | 통과 |
| F2-03 | 이유 기여도 순 1~2개 | 자동 `PetRestPlannerTest` | 통과 |
| F2-04 | 히스테리시스·쿨다운 | 자동 `PetRestPlannerTest` | 통과 |
| F2-05 | "여기서 쉴게요" → 경유지 | 자동 `VoiceQueueTest`(의도) · 에뮬레이터 `02b-suggest`(카드·버튼, 에뮬레이터 STT 없음 → 버튼 시트) | 로직 통과 · 실제 내비 경유지 반영은 미검증 |
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
| SV-02 | `alembic upgrade head` (0003) | PostgreSQL 테스트 | **미검증** (Docker 꺼짐) |
| SV-03 | 토큰 없으면 401 | 자동 서버 | 통과 |
| DM-01 | 장면 점프 → 기대 AppMode | 자동 `DemoScenesTest` | 통과 |
| DM-02 | 패널 장면 버튼·비교 결과 | 코드 · 에뮬레이터 패널 열림 | 로직 통과 · 화면 대기 |

## 실명·키 노출 검사 (2026-10-29)
- 범위: 추적 + 새 파일 280개(이미지·글꼴·jar 제외)와 커밋 작성자.
- 실명·개인 이메일(gmail·naver·daum): **없음**. 커밋 작성자는 `xuxtaku7610-del <…@users.noreply.github.com>` 하나.
- API 키 형태(serviceKey=, AIza, sk-, `*_API_KEY=값`, password=): **없음**. 키는 `server/.env`(gitignore)와 환경 변수로만.
- 전화번호 형태: `server/tests/test_places_vets.py`의 `033-000-0000` 하나 — 테스트용 가짜 번호. 샘플 병원에는 전화번호를 넣지 않았다.
- `server/.env`, `app-android/local.properties`: gitignore 확인, 추적 0개.
