# 쉼표 → 같이가개 재사용 지도

기준: [CONCEPT.md](CONCEPT.md) (F1~F7). 작성 2026-10-29.
**재사용** = 거의 그대로 · **수정** = 같은 자리에서 고치거나 확장 · **삭제** = 코드에서 뺌 · **신규** = 새로 만듦.
applicationId·패키지(`com.emop.pause`)·모듈 이름은 빌드 위험 때문에 그대로 둔다(PIVOT 0-5).

## 앱 (app-android)

| 모듈 / 클래스 | 판정 | 이유 |
| --- | --- | --- |
| `core:model` `Signals`·`States`·`SignalSample` | 수정 | 그대로 쓰되 F1용 `headingDeg`(방위)를 신호에 추가 |
| `core:model` `SignalSmoother` | 재사용 | F1 가속도 추정 전 속도 잡음 처리 (CONCEPT 3장 F1) |
| `core:model` `SignalMerge`·`UxLimits` | 재사용 | 신호별 출처 우선순위, CarUxRestrictions 플래그 |
| `core:vehicle` `PleosVehicleSource`·`AaosPropertySource` | 재사용 | 기어·배터리·속도·주차 브레이크 (sdk_check.md) |
| `core:navi` `NaviSource`·`PleosNaviSource` | 수정 | 남은 거리·경유지 추가 그대로. 방위(`CurrentLocationInfo.heading`)를 신호로 넘김 |
| `core:restriction` | 재사용 | 주행 안전 규칙 (CONCEPT 5장) |
| `core:uistate` `UiContract`·`ScreenStates`·`Fixtures` | 수정 | 화면 계약 틀은 유지, 화면 목록을 새 주제 10종으로 다시 씀 (4단계) |
| `core:uistate` `DrivingRules`·`UxPolicy` | 재사용 | 주행 중 2줄·버튼 2개 규칙 |
| `engine:fatigue` `FatigueEngine` | 재사용 | F2 안의 운전자 피로 요인 |
| `engine:needs` `NeedsEngine` | 수정 | 반려견 배변 간격을 프로필 값으로 (F2). 아이 니즈는 남겨도 무해하지만 입력에서 뺌 |
| `engine:planner` `RestPlanner`·`ScoringPolicy`·`SuggestionPolicy`·`RestPhrases` | 수정 → `PetRestPlanner` | 점수에 멀미 누적·반려견 시설 가중치 추가. 히스테리시스·쿨다운·이유 문장 유지 (F2) |
| `engine:planner` `TripPlanPreview` | 재사용 | 출발 체크 화면의 "오늘의 쉼표" |
| `engine:pause` `PauseCurator` | 수정 | 파워냅·원정대·집중 제거, 반려견 산책·물·배변 위주로 |
| `engine:content` (`Content`·`Expedition`·`Playlist`) | **삭제** | 라디오·원정대 기능 제외 (CONCEPT "뺀 것") |
| 신규 `engine:motion` `MotionComfortEngine` | 신규 | F1 멀미 예보 (CONCEPT 4장) |
| 신규 `core:model` `PetProfile` · `engine:pet` `WaitMode`·`BoardingCheck`·`PetTripLog`·`Josa` | 신규 | 프로필은 모든 엔진이 쓰므로 `core:model`, F3·F6·F7 도메인은 `engine:pet` |
| 신규 `data` `PetDatabase`(`pet.db`)·`RoomPetStore` | 신규 | 개인 데이터는 캐시 DB(`pause.db`, 스키마 바뀌면 지움)와 분리 |
| `voice` `SpeechQueue`·`Speech`·`ReplyIntent`·`KoreanNumber`·`AudioFocus`·`PhraseProvider` | 재사용 | 음성 제안 "여기서 쉴게요"·안전 우선 큐 |
| `voice` `EtaAnswer` | **삭제** | "아직 멀었어?" 제외 |
| `voice` `VoiceIntent` | 수정 | ETA·라디오 의도 제거, 응답(응/나중에)만 + "잠깐 다녀올게" 추가 예정 |
| `data` `RestStopRepository`·`BundledRestStops`·`RouteGeo`·캐시·`ApiClient`·`ServerSession` | 재사용 | 휴게소 후보·서버 토큰 (F2) |
| `data` `PauseApi` | 수정 | 콘텐츠 API 제거, 장소·병원·대기 공유 API 추가 |
| `sim` `Scenario`·`ScenarioPlayer`·`SimSources` | 수정 | 장면(depart/highway/rest_stop/wait/mountain_road/arrive)·방위 신호 |
| `app` `AppModeReducer` | 재사용 | PREPARE→DRIVING→PAUSE→ARRIVED |
| `app` `SignalRuntime` | 재사용 | 시뮬레이터·실차 신호 합류, 점프 |
| `app` `TripBrain` | 수정 | MotionComfortEngine·PetRestPlanner·WaitMode를 묶음 |
| `app` `TripCoordinator` | 수정 | 라디오·원정대·ETA 응답 제거, 음성 제안 흐름 유지 |
| `app` `TripRecorder`·`TripResume` | 수정 | F7 이동 기록(멀미 지수 추이) |
| `app` `RadioDirector`·`ContentRepository` | **삭제** | 라디오·콘텐츠 제외 |
| `app` `NapAlarm` (+ `USE_EXACT_ALARM`) | **삭제** | 파워냅 제외 |
| `app` `PresetStore` | 수정 → 반려견 프로필은 Room(`PetProfile`) | 아이·교대 운전자 프리셋 제거 |
| `app` `SettingsStore` | 수정 | `kidAnswer`·`napSound` 제거 |
| `app` `presentation/ScreenRouter`·`UiMapper`·`DemoScenes` | 수정 | 새 화면 10종 라우팅 |
| `app` `ui/` (theme·components·screens) | 수정(최소) | 디자인 보류(v5 예정). 상태만 보고 그리는 기본 레이아웃으로 교체 |
| 앱 이름·문자열 | 수정 | "같이가개" (applicationId 유지) |

## 서버 (server)

| 모듈 | 판정 | 이유 |
| --- | --- | --- |
| `api/health`·`devices`·`deps`·`middleware`·`errors`·`db`·`config` | 재사용 | 공통 틀·기기 토큰·요청 ID·레이트 리밋 |
| `api/reststops`·`jobs/ingest_reststops`·`geo` | 재사용 | F2 휴게소 + 반려견 놀이터 표시 추가 |
| `api/llm` `/llm/phrases`·`llm/ollama`·`llm/phrases` | 수정 | 기록 요약 문장 + 템플릿 대체 (F7) |
| `api/llm` `/llm/kid-answer` | **삭제** | 아이 응답 제외 |
| `api/content`·`content.py`·`jobs/load_content` | **삭제** | 라디오·미션 제외 |
| 모델 `content_episodes`·`missions`·`playlists`·`playlist_items` | **삭제**(마이그레이션 0002에서 drop) | 같은 이유 |
| `api/trips`·`jobs/cleanup` | 재사용 | 이동 기록 서버 보관(선택)·보관 기간 정리 |
| 신규 `api/places`·`jobs/ingest_pet_places` | 신규 | F4 TourAPI 반려동물 동반여행 |
| 신규 `api/vets`·`jobs/load_vets` | 신규 | F5 동물병원 인허가 |
| 신규 `api/wait` | 신규 | F3 보호자 폰 웹페이지 |

## 기타
| 대상 | 판정 | 이유 |
| --- | --- | --- |
| `content/` (라디오·미션 초안) | **삭제** | 기능 제외 |
| `scenarios/seoul-busan-family.json` 등 | 수정 | 반려견 동반 장면으로 교체 |
| `scripts/vhal.ps1` | 수정 | 장면 이름 depart/highway/rest_stop/wait/mountain_road/arrive |
| `docs/design/*` (쉼표 시안) | 보관 | 디자인 v5가 오기 전까지 참고용, 기준 아님 |
