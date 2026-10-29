# 진행 기록 — 쉼표

세션이 끊기면 이 파일 + `todo.md` + `docs/BLOCKED.md`만 보고 이어 간다. 최신이 위.

## 다음 할 일 (바로 이어서)
1. **커밋 대기열 처리** (BLOCKED: auto mode가 `git commit` 거부, 10/29 2차) — 아래 "커밋 대기열" 순서대로. 1번(서버)은 이미 `git add`로 스테이징됨.
2. 남은 P1 로직: 마이크 거부·STT 실패 시 버튼만(로직 점검), `_short` 문구, 기상 알림 무응답 1분·3분 단계(T6-10), 반려동물 차내 경고, 정차 종료 시 "다시 출발할게요", 앱 오프라인 여행 이벤트 큐(`/trips` 업로드).
3. `TripRecorder`(앱 리포트) 단위 테스트 보강 (qa_report T7-1).
4. 새 디자인(docs/design/v5)이 오면: `ui/`·`PauseRoot`만 교체, qa_report 에뮬레이터 항목 다시 확인.

## 2026-10-29 — P1 로직 + 단계 S(UI 무관)
- 한 일: **신호 잡음 처리** `SignalSmoother`(실차 경로만: 속도 3초 평균·0은 즉시, 기어 2초·충전 5초 확정, 1초 틱으로 확정) — DriveState는 거치지 않음. **이유 문장** = 실제 점수 기여 상위 1~2개(`RestPlanner.reasonsOf`, 편의시설 니즈 몫·피로). **한국어 숫자** `KoreanNumber`("다섯 시간"·"오 시간"·"여섯 시간 반"·범위 밖 null). **여행 상태 복원** `TripResume`/`TripResumeStore`(실차만, 30초마다 피로·니즈 누적값 저장, 3시간 안·같은 목적지면 재시작 시 ACTIVE로 되돌리고 "이어서 안내할게요", 끝·삭제 시 지움, 경로는 해시만). EV 항목은 기존 "마지막 급속충전 30분 안" 규칙 + 화면 "완료 예상"으로 충족 확인. **릴리스**: 환경 변수 서명 설정, `-Ppause.minify=true` R8 + `proguard-rules.pro`, `docs/release.md`. **README** 기능 현황·실행 방법 실제 명령으로 갱신. **qa_report** P0 34개 점검표. **노출 검사** 254파일·커밋 19개 깨끗.
- 검증: `./gradlew test assembleDebug` 실패 0, R8 릴리스 APK 9.9 MB → 디버그 키 임시 서명으로 에뮬레이터 실행, 크래시 없음(`artifacts/release/r8_launch.png`). 에뮬레이터 복원: 출발 → force-stop → 재실행 후 저장값 유지·재저장 확인. **에뮬레이터에서 잡은 버그 2개:** ① 경로 키에 `|`와 좌표가 들어 있어 저장값이 안 읽힘(+좌표 저장) → 해시만 저장 ② 시작 직후 기본 Snapshot(출처 VEHICLE, 시각 0)을 실차 프레임으로 보고 저장값을 지움 → 신호 출처가 있는 프레임만 확인. 또 경로 키는 현재 위치가 바뀌면 달라져 → 복원은 목적지 키(`resumeKey`).
- 결정: 실차 AppMode 판단은 잡음 필터를 거친 값(기어 2초 지연 허용), 주행 안전은 CarUxRestrictions 그대로. R8은 실기기 검증 전까지 옵트인.
- 남은 리스크: 실차 P단 여행 화면이 출발 준비처럼 보임(UI 보류 중). "이어서 안내할게요" 음성은 에뮬레이터 TTS 로그로 확인 못 함.

## 2026-10-29 — 단계 I 발표 모드(로직) + G 개인정보 약속 맞춤 + K 버전
- 한 일: 점프 장면을 `presentation/DemoScenes`로 옮기고 "아직 멀었어?" 장면 추가(점프 뒤 말 주입). **버그 수정:** 점프 직후 휴게소 목록 로딩 전 계획이 돌아 "앞쪽 휴게소 정보가 없어요"를 말하고 10분 쿨다운에 걸려, "피로 상승 제안" 장면에서 제안이 안 나왔음 → 목록이 비면(로딩 중) 계획하지 않음. G: 개인정보 화면의 약속과 서버를 맞춤 — `/trips` 동승자는 인원 수만(나이·이름 422), 보관 정리 잡 `app.jobs.cleanup`(여행 90일·LLM 기록 30일·플레이리스트 30일·기기 마지막 접속 180일, `last_seen_at` 하루 1번 갱신). K: versionCode = git 커밋 수 + 1(지금 20).
- 검증: `DemoScenesTest`(7장면 AppMode·정차 원인), TripBrainScenarioTest +1(로딩 전 제안 없음), 서버 pytest 48 통과. 에뮬레이터 점프 4440 → 죽암 제안 음성 → 두 번 묻고 버튼 시트 (`artifacts/contract/i_jump_4440.png`). aapt2 `versionCode='20'`.
- 결정: 동승자 정보는 서버에 인원 수만(api_spec §2.10·database.md 갱신).
- 다음: P1 로직(신호 잡음 처리, 여행 상태 복원, EV 충전 예상, 이유 문장, 한국어 숫자 정규화, 마이크 거부 시 버튼만), 단계 S 중 UI 무관(README 기능·실행, qa_report, 노출 검사, 릴리스 빌드 절차).

## 2026-10-29 — 단계 J 서버 + G 데이터 삭제
- 한 일: api_spec 남은 엔드포인트 전부 — `/content/episodes`·`/content/playlists`(배낭 편성, 앱과 같은 규칙)·`/content/missions`(reviewed=true만), `/llm/kid-answer`(**LLM 없이 템플릿만**), `/trips`·`/trips/{id}/events`(clientEventId 멱등)·`PATCH /trips/{id}`(종료 후 409)·`/trips/{id}/report`, `DELETE /devices/me`. 콘텐츠 적재 잡 `app.jobs.load_content`. 미들웨어: 요청 ID(X-Request-ID 에코·생성), JSON 한 줄 접근 로그(토큰·본문 없음), 기기당 분당 120회 레이트 리밋(429 + Retry-After, /health 제외). `server/Dockerfile`, `.dockerignore`, compose `api` 서비스(host.docker.internal로 Ollama). 앱 "모두 지우기" → `ServerSession.forget()`(서버 삭제 성공 시 토큰 폐기, 오프라인이면 토큰 유지).
- 검증: 서버 pytest **45 통과**·3 skip(PG), 새 `test_content_trips.py` 15개. `./gradlew test assembleDebug` 171개 통과(forget 테스트 포함). `docker compose config` 통과, 이미지 빌드는 Docker 데몬 꺼짐 → BLOCKED.
- 결정: `/llm/kid-answer`는 LLM을 부르지 않는다 — CLAUDE.md 규칙 4가 api_spec 예시(source=ollama)보다 우선. api_spec §2.9 갱신, §2.14·공통(요청 ID·레이트 리밋) 추가.
- 다음: I 발표 모드(로직), G 첫 실행·설정 로직 점검, K 출시 준비(versionCode 자동).

## 2026-10-29 — 단계 H 품질
- 한 일: 빈·로딩·오류 상태 표(`docs/ui_contract.md`)와 `EmptyStatesTest`. 주행 중 쉼표 계획 다시 잡기(`TripPlanPreview` fromKm·firstGapMin, 남은 시간 안에 휴게소가 없으면 가장 가까운 곳, 주행 재개·5분마다, 점프 직후 출발 기준 계획이 덮어쓰던 경합 수정).
- 검증: `./gradlew test assembleDebug` 통과(EmptyStatesTest 5, TripPlanPreviewTest +2). 에뮬레이터 t=4370: 이전 "77분 뒤 추풍령" → "26분 뒤 죽암휴게소"(아이 화장실 18분), 경로선 죽암·칠곡·통도사 (`artifacts/contract/h_replan_4370.png`). 오프라인: 서버를 띄우지 않은 상태로 이 세션의 모든 에뮬레이터 확인을 했음(내장 데이터 BUNDLED·템플릿 문장·오프라인 배너).
- 다음: J 서버(Dockerfile, 구조화 로그·요청 ID·레이트 리밋, /content, /llm/kid-answer, /trips, DELETE /devices/me).

## 2026-10-29 — 단계 F-E 멈춤 + F 콘텐츠
- 한 일: 새 모듈 `:engine:content` — Episode·Mission 모델, `ContentLibrary`(검수본 우선, 초안은 allowDrafts일 때만), `PlaylistPlanner`(0/1 배낭 DP, 5초 단위, 같은 여행 중복 금지, 합 같으면 편수 적게), `ExpeditionTracker`(나이·정차 원인 맞춤 3개, 다 하면 다음 구간 라디오, 여행 중 반복 없음). `content/drafts/` 라디오 20편(속담·상식·안전·예절 각 5, 퀴즈 포함, 문장 40자 이하 검사)·미션 15개(휴게소 10·충전 5), reviewed=false, 원문 `content/tools/build_drafts.py`. 앱: 검수본은 모든 빌드, 초안은 **디버그 assets에만** 복사. `RadioDirector`(남은 시간 맞춤 편성, 문장마다 RADIO 순위·0.6초 쉼, 퀴즈는 뒷좌석 대상 정답까지 읽음), 음성 "라디오 틀어 줘/꺼", 멈춤 시작 시 원정대·주행 시작 시 열린 라디오 재생, "아직 멀었어?" 답에 남은 편수. 화면 계약에 `MissionUi`·`RadioLineUi`(draft)·`UiEvent.CompleteMission/SetRadio`. 파워냅 `NapAlarm`(AlarmManager `setAlarmClock`, 시뮬레이션 배속 반영 `NapAlarmPlan`, 기상 알림 채널 USAGE_ALARM + 전체 화면 인텐트).
- 검증: `./gradlew test assembleDebug` 통과(ContentTest 8, NapAlarmPlanTest 2). 에뮬레이터: dumpsys alarm `exactAllowReason=policy_permission window=0` → 100배속에서 알림 "일어날 시간이에요" 게시 확인. 라디오 `1/16 가는 말이 고와야 오는 말이 곱다 [DRAFT]` (`artifacts/contract/ff_radio_draft.png`).
- 결정: 창 알람은 최소 10분 지연이라 `USE_EXACT_ALARM` 선언(정책 확인 → BLOCKED). 배터리 20% 이하 공조 유지 금지는 PauseCurator(T6-9)에 이미 있음.
- 교훈: KDoc에 `/content/*`처럼 `/*`가 들어가면 중첩 주석이 열려 파일 전체가 깨지고, KSP는 "could not be resolved"로만 알려 준다. 같은 증상이면 먼저 `/*`를 찾는다.
- 다음: H 품질(빈·로딩·오류 상태 정의, 오프라인 전체 시연, 주행 중 재계획), J 서버.

## 2026-10-29 — 단계 F-D 주행 안전 정책
- 한 일: `UxLimits`(CarUxRestrictions 활성 플래그, 비트값 javap 확인, 시뮬레이터는 DriveState로 근사), `DriveStateRepository.uxLimits`(실차: `getActiveRestrictions`·`getMaxRestrictedStringLength`). 화면 계약 `UxPolicy`: NO_VIDEO → 움직임 정지, NO_SETUP·NO_KEYBOARD → 설정·온보딩 차단(ScreenRouter `setupAllowed`), LIMIT_STRING_LENGTH → 주행 화면 문장 첫 문장/말줄임. 매니페스트 `distractionOptimized`는 이미 선언(확인).
- 검증: `./gradlew test assembleDebug` 통과 — UxLimitsTest 2, UxPolicyTest 4, ScreenRouterTest +1, **DrivingUiScenarioTest 3**(시나리오 3종을 엔진→UiMapper→UxPolicy로 흘려 모든 주행 시점이 DrivingRules 통과).
- 남은 것: Pleos 문서 Driving Restriction 가이드와 대조(문서 접근 필요), 문구 축약본 `_short`(P1).
- 다음: F-E 멈춤(AlarmManager 기상, 배터리 20% 공조 규칙 확인, 원정대 미션 완료 → 라디오 열림).

## 2026-10-29 — 단계 F-C 음성
- 한 일: `VoiceIntent`("아직 멀었어/얼마나 남았어/언제 도착해" → 남은 시간 질문, 존댓말·시간 명사면 어른 말투), `EtaAnswer`(템플릿만, 아이 답은 라디오 편수 → 다음 쉼표 → 남은 시간 순, 어른 답은 시간·거리·다음 쉼표, 받침 조사 처리), `SpeechQueue`(안전 > 제안 > 아이 답 > 라디오, 높은 순위가 낮은 순위를 끊음, 포커스 거절 시 500ms마다 재시도·10초 넘으면 버림, 안전 경고는 무조건, 말하는 중 포커스 잃으면 한 번 다시), `AndroidAudioFocus`(제안·답 GAIN_TRANSIENT_MAY_DUCK, 라디오 GAIN, TTS도 USAGE_ASSISTANT). TripCoordinator: 언제든 ETA 질문에 답, 피로 HIGH 제안은 SAFETY 순위, 설정 "아이 답변" 끄면 어른 답.
- 검증: `./gradlew test assembleDebug` 143개 통과(VoiceQueueTest 12). 에뮬레이터: 주행 중 `--es say '아직 멀었어?'` → "42분 뒤에 옥산휴게소에서 한 번 쉬고 가자. 조금만 기다려 줘!" (`artifacts/contract/fc_eta_kid.png`).
- 결정: 상시 듣기(웨이크워드)는 없다 — "아직 멀었어?"는 STT 듣기 창·발표 모드 버튼·adb 주입으로 들어온다. 운전대 음성 버튼 연동은 Pleos 음성 SDK가 열리면(M5).
- 다음: F-D 주행 안전 정책(CarUxRestrictions 플래그별 처리, distractionOptimized).

## 2026-10-29 — 단계 F-B 추천
- 한 일: `sample_reststops.json`을 경부선 실제 휴게소 이름·순서로 교체(부산방향 19 + 서울방향 4, 좌표는 시나리오 경로 위 근사치, 편의시설 샘플, 이름 확인 필요 → BLOCKED). `ScoringPolicy`(도착 시각 0.35·피로 근접 0.2·편의시설 0.35·휴식 간격 페널티 0.1, 히스테리시스 15%). 플래너가 직전 제안을 히스테리시스로 유지, 지나치면 다음 후보. 거절 뒤 피로가 HIGH로 오르면 쿨다운 무시. 반대 방향 휴게소 제외(방향 종점 기준, 앱 RouteGeo·서버 geo 같은 규칙). 경로 거리를 내비 총거리 척도로 환산(`routeLengthMeters`, 직선 341km vs 395km 차이로 정차 지점이 어긋나던 문제). Room 캐시 v2(노선·방향).
- 검증: `./gradlew test assembleDebug` 통과(ScoringPolicyTest 9, RestStopRepositoryTest 갱신), 서버 pytest 30 통과·3 skip. 시뮬레이터: 첫 제안 t=3180 옥산(피로 주의), t=4380 죽암(시나리오 충전 정차 지점) — 발표 모드 점프 "피로 상승 제안"(4440)과 맞음.
- 결정: "다음 곳으로"/"나중에" 모두 10분 뒤 재평가 + 같은 곳이면 히스테리시스 유지(T4-4). 방향 표기를 모르는 휴게소는 제외하지 않음.
- 남은 리스크: 주행 화면의 "계획 쉼표"(출발 전 TripPlanPreview)는 주행 중 다시 계산하지 않아, 아이 화장실이 급해도 77분 뒤 휴게소가 보일 수 있음 → H에서 주행 중 재계획.
- 다음: F-C 음성("아직 멀었어" 응답기, TTS 우선순위 큐, 오디오 포커스).

## 2026-10-29 — 단계 F-A 신호 + 3번 VHAL 점검
- 한 일: `SignalSource`(PLEOS_SDK/AAOS_PROPERTY/SIM/NONE)와 순수 `SignalMerge`(신호별 우선순위). `AaosPropertySource`(CarPropertyManager: PERF_VEHICLE_SPEED·GEAR_SELECTION·PARKING_BRAKE_ON·EV_BATTERY_LEVEL·EV_CHARGE_STATE, javap 확인). 실차 모드 = Pleos SDK → AAOS 속성 → (선택) 시나리오 보충(`--es fill <시나리오>`). Snapshot.sources, 설정·발표 모드에 출처 표시. CAR_SPEED·CAR_ENERGY 런타임 요청. AppModeReducer: 기어 UNKNOWN = 미지원, 기어 미지원 차종은 주차 브레이크로 즉시 PAUSE. `scripts/vhal.ps1`, `docs/vhal_check.md`.
- 검증: `./gradlew test assembleDebug` 통과(SignalMergeTest 4, AaosMappingTest 4, AppModeReducerTest +6). 에뮬레이터 실차 모드에서 AAOS 지원 `[SPEED, GEAR, PARKING_BRAKE, CHARGING]`, 합친 값이 런처와 일치. VHAL 값 주입은 user 빌드라 불가 → BLOCKED.
- 결정: CAR_ENERGY도 dangerous(에뮬레이터 확인) → CAR_SPEED와 함께 앱 시작 시 요청. 기어 신호가 있으면 주차 브레이크는 보지 않는다(architecture §5.2 갱신). `gradle.properties` 메타스페이스 768m → 1536m(in-process 컴파일 OOM).
- 다음: F-B 추천(경부선 실제 휴게소 샘플, ScoringPolicy, 히스테리시스, 쿨다운).

## 2026-10-29 — 1번 UI와 엔진 분리 (화면 계약)
- 한 일: 새 모듈 `:core:uistate`(순수 Kotlin) — 화면별 상태 `ScreenStates.kt`, `UiContract.kt`(AppUiState·Screen·UiEvent·Scene), `Fixtures.screen(Scene)`, `DrivingRules`. 앱 `presentation/`에 `UiMapper`(이동)·`ScreenRouter`(순수 화면 선택 규칙). ViewModel은 `ui: StateFlow<AppUiState>` 하나와 `onEvent(UiEvent)`만 노출, `PauseRoot`는 Screen → 화면 호출만. `docs/ui_contract.md`(화면별 값·버튼·이벤트·주행 제한 표, Mermaid 렌더 확인). test_plan.md 전 항목에 ID(T장-번호) 부여.
- 검증: `./gradlew test assembleDebug` 통과(새 테스트 DrivingRulesTest 4, ScreenRouterTest 7). 에뮬레이터 설치 후 주행 화면 정상(`artifacts/contract/drive_after_contract.png`).
- 결정: test_plan T4-12 "버튼 1개" → "버튼 ≤ 2개(수락·거절)"로 고침(UI_SPEC §4.1 우선). 화면 계약 모듈의 상태는 Compose 안정성 설정 파일(`app/compose_stability.conf`)로 안정 취급.
- 다음: 단계 F-A 신호.

## 2026-10-29 — 계획 변경: 백엔드 우선 (`_private/BACKEND_FIRST_PROMPT.md`)
- 한 일: 0번 정리. 진행 중인 v4 UI 코드 작업은 없음(`docs/design/v3·v4`는 시안 문서만). 커밋 대기열 1번 시도 → 다시 거부(BLOCKED.md 10/29). `server/`만 스테이징 상태.
- 결정: UI 보류, 백엔드 우선 (디자인은 docs/design/v5/로 들어올 예정). 화면 코드는 빌드가 깨지지 않을 만큼만 손댄다.
- 다음: 1번 UI 계약(UiState를 도메인 쪽 모듈로, `docs/ui_contract.md`) → 2번 단계 F(A→B→C→D→E→F→H→J→I→G→K).

## 2026-10-28 밤 — 단계 U (UI 재구축) 완료
- 한 일: UI_SPEC v2 토큰(Color/Type/Dimens/Theme, Pretendard OFL 포함), 컴포넌트 13종(`ui/components`), 화면 17종(`ui/screens` Drive·Stop·Setup), 반응형 3단계, 발표 모드 패널(로고 5번 탭, 디버그 신호 화면 흡수, 장면 점프·시안 고정·응답 주입), 화면 모델(`ui/model`: ScreenModels·Fixtures·UiMapper). 엔진 추가: `TripPlanPreview`(출발 전 쉼표 계획), `PauseCurator`(engine:pause), `TripRecorder`(리포트), 음성 "한 번 더 묻고 → 버튼" 흐름, 점프 시 엔진 따라잡기, 앱 아이콘·스플래시.
- 검증: `./gradlew test` 전체 통과(새 테스트 TripPlanPreviewTest 4, PauseCuratorTest 7), 서버 pytest 28 통과·3 skip(PG). 에뮬레이터 캡처 17화면 시안 비교 → `docs/ui_review.md`, 최종 `artifacts/ui/final/`.
- 결정: 아래 "결정" 참고.
- 남은 리스크: 내장 휴게소 데이터가 성겨 실제 시뮬레이션에서 제안 카드가 늦게 나옴(F-B에서 해결). 파워냅 기상은 아직 앱 타이머(AlarmManager는 E P0). 이름 입력 화면 없음(기본 호칭 "아이"/"반려견").

### 결정
- 결정: 설정 진입 = 출발 준비의 `바꾸기` 버튼(시안에 설정 진입점이 없음). 주행 중엔 설정이 자동으로 닫힌다.
- 결정: 설정 "제안과 알림"에 `움직임 줄이기` 토글 추가(UI_SPEC §3 Fireplace `reduceMotion`의 입력처).
- 결정: 시뮬레이션의 출발 준비 멤버 타일은 시나리오 `setup`을 보여 준다(실차는 프리셋).
- 결정: 파워냅 타이머·리포트 시각은 시뮬레이션 시계 기준(배속 시연에서 맞도록).
- 결정: 앱 창 높이 1210dp에 맞춰 세로 고정값 대신 weight 사용(파워냅 벽난로 480×240).
- 결정: 디버그 adb 확장 `--es scene <이름|none> --ei forceWidth <dp> --ez demo true --es onboard done|reset --ez nap true`.

## 커밋 대기열 (권한 허용 후 순서대로, 메시지 그대로)
1. `[M2] 서버: 휴게소 수집 잡(샘플), /reststops, /devices 토큰, /llm/phrases` — `server/`
2. `[M2] 엔진: 피로·니즈·플래너·제안 정책 + 테스트` — `app-android/engine/fatigue app-android/engine/needs app-android/engine/planner/src/main/kotlin/com/emop/pause/engine/planner/{RestPhrases,RestPlanner,RestStop,SuggestionPolicy}.kt` 등
3. `[M2] 앱: 휴게소 캐시·내장 데이터·서버 세션, 음성 공급자, TripBrain` — `app-android/data app-android/voice app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/debug scenarios/`
4. `[U] 디자인 토큰 v2와 Pretendard 글꼴(OFL)` — `ui/theme`, `res/font`, `licenses/`, `assets/licenses`
5. `[U] 핵심 컴포넌트 13종` — `ui/components`
6. `[U] 화면 17종·반응형·발표 모드 패널` — `ui/screens`, `ui/model`, `MainActivity.kt`, `MainViewModel.kt`
7. `[U] 출발 전 쉼표 계획(TripPlanPreview)·멈춤 큐레이터(PauseCurator)·여행 기록` — `engine/planner/TripPlanPreview*`, `engine/pause`, `TripRecorder.kt`, `SettingsStore.kt`
8. `[U] 앱 아이콘·스플래시, 문서(UI 검증·진행·막힘)` — `res/drawable res/mipmap-anydpi-v26 res/values* AndroidManifest.xml docs/ todo.md .gitignore`
9. `[U] 화면 계약: :core:uistate(UiState·UiEvent·Fixtures·DrivingRules), ScreenRouter, docs/ui_contract.md, test_plan ID` — `app-android/core/uistate app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/app/compose_stability.conf docs/ui_contract.md test_plan.md`
10. `[F-A] 신호 출처(SignalSource·SignalMerge), AAOS 차량 속성 어댑터, 주차 브레이크 모드 규칙` — `app-android/core app-android/app architecture.md app-android/gradle.properties`
11. `[F-A] VHAL 점검 스크립트와 결과` — `scripts/vhal.ps1 docs/vhal_check.md docs/BLOCKED.md`
12. `[F-B] 경부선 휴게소 샘플, ScoringPolicy·히스테리시스·쿨다운, 반대 방향 제외, 경로 거리 척도` — `server/ app-android/data app-android/engine/planner app-android/app api_spec.md`
13. `[F-B] 경로가 바뀌면 휴게소 후보 다시 받기` — `TripCoordinator.kt RouteKeyTest.kt`
14. `[F-C] 아직 멀었어 응답기, TTS 우선순위 큐, 오디오 포커스` — `app-android/voice app-android/app`
15. `[F-D] CarUxRestrictions 플래그별 정책(UxLimits·UxPolicy), 주행 화면 시나리오 검증 테스트` — `app-android/core app-android/sim app-android/app`
16. `[F] 콘텐츠 초안 20편·미션 15개, 플레이리스트 배낭 편성, 원정대, 도착 맞춤 라디오` — `content/ app-android/engine/content app-android/app app-android/core/uistate app-android/voice`
17. `[F-E] 파워냅 시스템 알람(AlarmManager)과 기상 알림` — `app-android/app`
18. `[H] 주행 중 쉼표 계획 다시 잡기, 빈·로딩·오류 상태 정의와 테스트` — `app-android/engine/planner app-android/app docs/ui_contract.md`
19. `[J] 콘텐츠·여행·아이 답변(템플릿)·기기 삭제 API, 요청 ID·구조화 로그·레이트 리밋, Dockerfile` — `server/ docker-compose.yml .dockerignore api_spec.md`
20. `[G] 내 데이터 삭제 시 서버 기기 삭제` — `app-android/data app-android/app`
21. `[I] 발표 모드 점프 장면 로직·테스트, 점프 직후 제안 누락 수정` — `app-android/app`
22. `[G] 동승자 인원 수만 받기, 보관 기간 정리 잡` — `server/ api_spec.md database.md`
23. `[K] versionCode 자동 증가` — `app-android/app/build.gradle.kts`
24. `[P1] 실차 신호 잡음 처리(SignalSmoother)·이유 문장 기여도 순·한국어 숫자 정규화` — `app-android/core/model app-android/engine/planner app-android/voice app-android/app/src/main/kotlin/com/emop/pause/runtime test_plan.md`
25. `[P1] 실차 여행 상태 복원(TripResume)` — `app-android/engine/fatigue app-android/engine/needs app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/test app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt`
26. `[S] 릴리스 서명·R8 옵트인, README 실행 방법, qa_report, release.md` — `app-android/app/build.gradle.kts app-android/app/proguard-rules.pro README.md docs/ docs/design/COMMERCIAL_READINESS.md`
(한 번에 하려면 `git add -A` 후 1개 커밋도 가능. AGENTS.md는 .gitignore에 추가함.)
