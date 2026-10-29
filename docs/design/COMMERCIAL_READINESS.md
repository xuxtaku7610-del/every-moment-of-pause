# 상용화·공모전 완성도 체크리스트 — 쉼표

"공모전에서 시연이 되는 앱"을 넘어 "앱 마켓에 올릴 수 있는 앱"이 목표다. 각 항목은 끝나면 체크하고, 어떻게 검증했는지 한 줄을 남긴다. 우선순위: **P0** 시연·심사 필수 · **P1** 접수 전 · **P2** 본선 전.

## A. 차량 신호와 상태 (차량 특화성)
- [x] P0 속도는 Vehicle SDK에서 못 받으면 AAOS `CarPropertyManager`의 `PERF_VEHICLE_SPEED`로 받는다(현재 디버그 화면에서 speed "미지원"). 둘 다 없으면 미지원 처리.
  - 검증: `AaosPropertySource` + `SignalMergeTest`, 에뮬레이터에서 speed `[AAOS_PROPERTY]` 수신 (docs/vhal_check.md, 10/29).
- [x] P0 신호별 `SignalSource`(PLEOS_SDK / AAOS_PROPERTY / SIM / NONE)를 기록하고 디버그 화면과 설정의 "이 차에서 쓰는 신호"에 표시.
  - 검증: `Snapshot.sources`, 발표 모드 신호 줄·설정 신호 행에 출처 (artifacts/vhal/vehicle_signals_demo_panel.png).
- [x] P0 AppModeReducer 확정 규칙 + 경계 조건 테스트(문서 §5.2).
  - 검증: `AppModeReducerTest` 15개(T6-12~14, 주차 브레이크·기어 UNKNOWN 포함).
- [x] P1 신호 잡음 처리: 속도 1초 값 이동평균, 기어 깜빡임 2초 디바운스, 충전 상태 변화 5초 확정.
- [x] P1 앱 프로세스가 죽었다 살아나도 여행 상태 복원(Room + SavedStateHandle). 여행 중 재시작 시 "이어서 안내할게요".
- [ ] P2 여행 추적은 포그라운드 서비스로(화면이 다른 앱일 때도 피로·니즈 계산 유지). 알림/상태 표시는 Pleos 정책 범위 안에서.

## B. 추천 품질 (핵심 가치)
- [x] P0 점수 = 도착 시간 적합도 + 피로 긴급도 + 니즈 충족(편의시설 매칭) + 다음 휴식까지 간격 페널티. 가중치는 `ScoringPolicy` 한 곳에, 단위 테스트로 고정.
  - 검증: `ScoringPolicy` + `ScoringPolicyTest` (가중치 고정, 10/29).
- [x] P0 히스테리시스: 한 번 제안한 휴게소는 더 나은 후보가 점수 15% 이상 높을 때만 바꾼다(제안이 깜빡이지 않게).
  - 검증: `ScoringPolicy.stabilize`, `T4-4` 테스트 (10/29).
- [x] P0 제안 간격: 거절 후 최소 10분, 연속 3회 거절 시 20분. 피로 "위험"이면 예외(가장 가까운 졸음쉼터).
  - 검증: `SuggestionPolicy` + `T4-11`, 시나리오 테스트 (10/29).
- [x] P0 지나친 휴게소·반대 방향 휴게소 제외. 경로 이탈 시 후보 재계산.
  - 검증: 앱 `alongRoute`·서버 `along-route` 방향 필터 테스트. 경로가 바뀌면(routeKey) 주행 중 다시 받기 — `RouteKeyTest`.
- [x] P1 EV: 남은 주행 가능 거리와 다음 급속충전 휴게소 거리 비교, 부족하면 충전 니즈 "높음". 충전 완료 시간은 현재 충전 속도(없으면 차종 평균)로 추정하고 "예상"이라고 표시.
- [x] P1 이유 문장은 실제 점수에 가장 크게 기여한 요인 1~2개로 만든다(거짓 이유 금지).
- [ ] P2 개인화: 사용자가 자주 거절하는 시간대·편의시설 가중치를 낮춘다(기기 안에서만).

## C. 음성 (주행 중 핵심 인터랙션)
- [x] P0 제안 직후 8초 듣기 창. "응/좋아/그래/가자/들를게" = 수락, "아니/다음/나중에/괜찮아" = 거절, 그 외 = 한 번 더 묻고 끝.
  - 검증: `TripCoordinator.listenOnce`(8초) + 한 번 더 묻기, `VoiceTest`·`VoiceQueueTest` T4-3.
- [x] P0 "아직 멀었어/얼마나 남았어/언제 도착해" 계열 인식 → 응답기. 화자 구분은 하지 않고, 질문 표현으로 아이/성인 답변 톤 선택.
  - 검증: `VoiceIntent`·`EtaAnswer` (T5-1·T5-6), 에뮬레이터 주입 확인 (10/29).
- [x] P0 TTS 우선순위 큐: 안전 경고 > 제안 > 아이 답변 > 라디오. 내비 음성 안내 중에는 대기(오디오 포커스).
  - 검증: `SpeechQueue` + `VoiceQueueTest` T5-9 (우선순위·끊기·포커스 대기).
- [x] P0 오디오 포커스: 제안·답변은 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, 라디오는 `AUDIOFOCUS_GAIN` + 포커스 잃으면 일시정지.
  - 검증: `AndroidAudioFocus`, 큐가 종류별로 요청 (테스트). 라디오 일시정지는 F 라디오 플레이어에서 onLoss 연결.
- [ ] P1 마이크 권한 거부·STT 실패 시 화면 버튼만으로 모든 기능 사용 가능.
- [x] P1 한국어 숫자 정규화("다섯 시간", "5시간", "오 시간").

## D. 주행 안전 정책 (심사·출시 필수)
- [x] P0 `CarUxRestrictions` 플래그별 처리: `NO_VIDEO`(벽난로 애니메이션 정지), `LIMIT_STRING_LENGTH`(문구 축약본 사용), `NO_KEYBOARD`(입력 화면 차단), `NO_SETUP`(설정 차단).
  - 검증: `UxLimits`·`UxPolicy` + 테스트, ScreenRouter `setupAllowed` (10/29).
- [ ] P0 매니페스트 `distractionOptimized` 메타데이터 선언(주행 중 사용할 Activity). Pleos 문서의 Driving Restriction 가이드와 대조.
  - 선언 확인(MainActivity meta-data). Pleos 문서 Driving Restriction 가이드 대조는 남음.
- [x] P0 주행 중 화면: 텍스트 블록 2줄, 버튼 ≤2개, 스크롤·자동 애니메이션 없음. UI 테스트로 검증.
  - 검증: `DrivingRules` + `DrivingUiScenarioTest`(시나리오 3종 전 구간) (10/29).
- [ ] P1 모든 문구에 축약본(`strings.xml`의 `_short`) 준비.

## E. 멈춤 경험 (시그니처)
- [x] P0 파워냅: 배터리 20% 이하에서는 공조 유지형 제안 금지. 타이머는 시스템 알람(AlarmManager)으로 걸어 앱이 죽어도 깨움.
  - 검증: `PauseCuratorTest` T6-9, `NapAlarm`(setAlarmClock) + 에뮬레이터 알림 확인 (10/29).
- [x] P0 원정대 미션: 나이별 3개, 완료 체크 → 다음 구간 라디오 열림. 정차 중에만.
  - 검증: `ExpeditionTracker` + `ContentTest` T6-3, 멈춤 시작·주행 시작 연결. 미션 화면 표시는 새 디자인에서(`PauseScreenUi.missions`).
- [ ] P1 반려동물: 차에 두고 내릴 때(도어 열림→닫힘 + 시동 유지) 실내 온도·공조 상태를 확인해 경고 문구. 실제 원격 알림은 로드맵으로 명시.
- [ ] P1 정차 종료(DRIVING 진입) 시 진행 중 활동을 안전하게 정리하고 "다시 출발할게요" 한 줄.

## F. 콘텐츠
- [x] P0 라디오 에피소드 20편·미션 15개 초안(`content/drafts/`, reviewed=false). 사실 확인이 필요한 문장은 `"check": true`로 표시.
  - 검증: `content/drafts/` + `ContentTest`(20·15, reviewed=false, 40자).
- [x] P0 플레이리스트: 목표 시간 이하에서 가장 가까운 조합(0/1 배낭 DP), 같은 여행에서 중복 재생 금지.
  - 검증: `PlaylistPlanner` + `ContentTest` T5-2·T9-7·T9-8.
- [x] P1 TTS 낭독용 스크립트 규칙: 한 문장 40자 이하, 문장 사이 0.6초 쉼.
  - 검증: 생성기가 40자 검사, RadioDirector 0.6초 쉼.
- [x] P1 디버그 빌드만 초안 재생("DRAFT" 표시), 릴리스는 reviewed=true만.
  - 검증: 초안은 debug assets에만, `ContentLibrary.allowDrafts`, `RadioLineUi.draft`/발표 모드 `[DRAFT]`.

## G. 첫 실행·설정·개인정보
- [x] P0 첫 실행 온보딩 3단계(정차 중에만): 이 앱이 하는 일 → 동승자 프리셋 → 마이크 권한 이유.
  - 검증: 단계 U 화면 13~15 + `ScreenRouter`(정차 중만, NO_SETUP이면 차단), 진행 저장 `SettingsStore.onboardDone`.
- [x] P0 설정: 동승자 관리, 제안 민감도(빠르게/보통/여유), 음성 켜기/끄기, 장작 소리, 데이터 삭제, 오픈소스 라이선스, 버전.
  - 검증: 단계 U 화면 16, 값은 `SettingsStore`·`PresetStore`에 저장, 데이터 삭제는 서버까지.
- [x] P0 개인정보 안내 화면: 수집하는 것(휴게소 단위 여행 이벤트), 수집하지 않는 것(경로 좌표 저장, 수면·동승자 정보 서버 전송), 보관 기간.
  - 검증: 설정 > 개인정보. 서버를 약속에 맞춤 — 동승자 인원 수만, 90일 정리 잡 (10/29).
- [x] P1 "내 데이터 삭제" = 기기 Room 초기화 + 서버 기기 토큰 삭제 API.
  - 검증: `deleteData` → 프리셋·설정·기록 초기화 + `ServerSession.forget()` → `DELETE /devices/me` (테스트 양쪽).

## H. 품질·성능
- [x] P0 빈 상태·로딩·오류 상태를 모든 화면에 정의(문구는 UI_SPEC §6 원칙).
  - 검증: `docs/ui_contract.md` 표 + `EmptyStatesTest` (10/29).
- [x] P0 오프라인: 서버·LLM이 꺼져도 시연 시나리오 전체 동작(템플릿·캐시·번들 데이터).
  - 검증: 서버 없이 시나리오 전 구간(제안·수락·정차·파워냅·라디오·ETA 답) 에뮬레이터 확인, 저장소 BUNDLED 테스트 T4-7 (10/29).
- [ ] P1 다크 모드 토큰 적용 확인, 글자 크기 설정 1.3배에서 넘침 없음.
- [ ] P1 콜드 스타트 2초 이내, 3시간 시뮬레이션 후 메모리 200MB 이하.
- [ ] P1 strings.xml 한국어 기본 + 영어(en) 번역.
- [ ] P2 Compose UI 테스트: 주요 흐름 5개(출발→제안 수락→정차→파워냅→리포트).

## I. 시연(PT) 대비
- [x] P0 **발표 모드**: 로고 5번 탭 → 숨은 데모 패널(시나리오 선택, 배속, 특정 장면으로 점프: 출발/아직 멀었어/피로 상승 제안/충전 정차/파워냅/도착). 심사 앞에서 3분 안에 모든 시그니처를 확실하게 보여주기 위함.
  - 검증: `DemoScenes` + `DemoScenesTest`, 에뮬레이터 점프 확인 (10/29).
- [x] P0 데모 패널은 릴리스 빌드에서도 숨겨진 채 존재(심사용 APK에서 쓸 수 있게), 일반 사용자는 접근 경로를 모름.
  - 검증: 로고 5번 탭은 빌드 종류와 무관(`onLogoTap`), 시나리오 assets는 모든 빌드에 포함. adb 제어만 디버그 전용.
- [ ] P1 시연 스크립트 `docs/demo_script.md`: 3분 타임라인, 각 장면 대사, 실패 시 대처.

## J. 서버
- [ ] P0 `docker compose up`으로 DB + 서버 함께 기동(서버 Dockerfile). Ollama는 호스트(`host.docker.internal`).
  - 작성 완료(`server/Dockerfile`, compose `api`), `docker compose config` 통과. 이미지 빌드·기동 확인은 Docker Desktop을 켠 뒤(BLOCKED).
- [x] P0 구조화 로그, 요청 ID, 타임아웃, 기기당 레이트 리밋.
  - 검증: `RequestContextMiddleware` + 테스트(요청 ID 에코, JSON 로그, 429), LLM 2.5초 타임아웃 기존 테스트.
- [ ] P1 도로공사 수집 잡: 키가 생기면 실제 수집, 없으면 샘플. 실패해도 기존 데이터 유지.
  - 샘플 적재·실패 시 유지는 테스트됨. 실제 API 호출부는 키 발급 후 `TODO(ex-api)`.
- [x] P1 `DELETE /devices/me`(데이터 삭제).
  - 검증: `test_delete_me_removes_device_and_trips`.

## K. 출시 준비
- [x] P0 앱 이름 "쉼표", 아이콘(쉼표 기호, `rest` 배경, adaptive icon), 스플래시.
  - 검증: 단계 U (adaptive icon·스플래시).
- [x] P0 버전 체계 `versionName 0.x.y`, `versionCode` 자동 증가.
  - 검증: versionCode = 커밋 수 + 1, aapt2 확인(20).
- [x] P1 릴리스 서명: keystore는 저장소 밖(`%USERPROFILE%\.keystores\`), 비밀번호는 `local.properties`나 환경 변수. R8 축소, APK 크기 기록.
- [ ] P1 README: 스크린샷(`artifacts/ui/final/`), 기능, 실행 방법, 아키텍처 그림, 심사 항목 대응표.
- [ ] P1 오픈소스 라이선스 목록(Pretendard OFL 포함).
