# 운전자 주의분산 자체 점검표 (2026-11-02)

## 기준 문서
Pleos 문서를 2026-11-02에 직접 읽었다. 아래는 읽은 페이지의 항목만 근거로 쓴다.

| 약어 | 페이지 |
| --- | --- |
| RG-D | [Review: Minimizing Driver Distraction Elements](https://document.pleos.ai/en/docs/pleos-only/distribute/app-review-guide/review-driver-distraction-elements) |
| RG-P | [Review: App Functionality and Performance Stability](https://document.pleos.ai/en/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability) |
| DS | [Design considering driving safety](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) |
| UI | [Pleos Connect UI Design Guide 소개](https://document.pleos.ai/en/docs/connect/guide/docs-design/Introduction/intro) |
| DRP | [Driving Restriction Policy by Region](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy) |
| AAOS | [AAOS Driver distraction guidelines](https://source.android.com/docs/automotive/driver_distraction/guidelines) |

**못 읽었거나 숫자를 찾지 못한 곳 → "확인 필요"**
- 버튼 크기·간격: Basic button 페이지에서 숫자가 그림(svg) 안에만 있어 읽지 못했다.
- Display Layout 페이지는 읽지 않았다.
- AAOS Car UX Restrictions 규칙 페이지(car_uxr)는 읽지 않았다.
- DRP는 북미·유럽·기타 지역만 다룬다. **한국 규정은 없다.**

**미디어 앱 규칙(RG-D)은 해당 없음.** 같이가개는 영상·게임·브라우저가 없는 비미디어 앱이다. 비미디어 앱에는 DS·UI 원칙을 적용했다.

## 점검표

| 항목 (출처) | 같이가개에서 어떻게 지키는지 | 근거 파일·테스트 | 결과 |
| --- | --- | --- | --- |
| CarUxRestrictions로 제한 상태를 확인하고 제한된 기능을 줄이거나 끈다 (DS) | 주행 판단은 `CarUxRestrictionsManager` 하나만 본다(속도로 추정하지 않음). 최적화가 필요하면 DRIVING, 완전 제한이면 RESTRICTED. 둘 중 하나면 어떤 화면이 열려 있어도 즉시 주행 화면으로 바꾼다. 신호 평활 필터를 거치지 않는다 | `core/restriction/.../CarUxDriveStateRepository.kt`, `presentation/ScreenRouter.kt` · ScreenRouterTest `DS-01` | 지킴 |
| 제한 플래그별 처리 — NO_KEYBOARD·NO_SETUP·NO_VIDEO·LIMIT_STRING_LENGTH (DRP·DS) | 키보드·설정 막힘: 정차 중이어도 프로필 편집·설정을 열지 않는다. 영상 막힘: 움직이는 화면을 멈춘다. 문자열 제한: `maxRestrictedStringLength`에 맞춰 첫 문장만 남기거나 자른다(운전 안내는 음성으로 그대로) | `core/uistate/.../UxPolicy.kt`, `core/model/.../UxLimits.kt` · ScreenRouterTest `DS-04`, UxPolicyTest | 지킴 |
| Distraction Optimized 선언 (AAOS) | 런처 액티비티 하나에 `distractionOptimized=true`를 선언했다. 앱 전체가 한 액티비티다 | `app/src/main/AndroidManifest.xml` | 지킴 (선언은 플랫폼이 검사하지 않으므로 아래 항목들이 실제 근거) |
| 영상·움직이는 그림·자동 애니메이션을 주행 중 줄이거나 없앤다 (DS, UI) | 애니메이션은 AppMode=PAUSE이고 PARKED이며 "움직임 줄이기"가 꺼졌을 때만 허용한다. 주행 화면 파일에는 `animate*`·`InfiniteTransition`이 없다. 영상 없음 | `ScreenRouter.motionAllowed`, `ui/screens/DriveScreens.kt` · ScreenRouterTest "애니메이션은 PAUSE이고 PARKED일 때만" | 지킴 |
| 자동 스크롤 글자·반복 화면 전환 금지 (UI, DS) | 주행 화면에 스크롤 컨테이너(`verticalScroll`·`Lazy*`)가 없다. 화면 전환은 상태가 바뀔 때만 한다. 음성 실패 시 버튼 시트는 한 번만 띄운다(다시 묻기 없음) | `DriveScreens.kt`, `trip/ReplyGate.kt` · ReplyGateTest F2-14·F2-15 | 지킴 |
| 한 메시지 120자·24단어 이하, 한 줄 80자 이하, 3줄 이하 (UI) | 주행 화면 규칙을 더 짧게 잡았다: 한 덩어리 48자(2줄), 한 줄 24자, 장소 이름 14자, 이유 2개. 각 `Text`에 `maxLines` 1~2 | `core/uistate/.../DrivingRules.kt` · DrivingRulesTest `DS-02`, DrivingUiScenarioTest `DS-02`(시나리오 4개 전 구간) | 지킴 |
| 버튼 수 최소, 자주 쓰는 기능은 첫 화면, 여러 단계 메뉴 금지 (DS) | 주행 중 버튼은 쉼표 추천 2개(피로 높음 1개), 콩이가 이상해요 2개, 예보·안내 0개, 음성 실패 시트 2개. 메뉴·드롭다운 없음 | `DrivingRules.buttons`, `DS-02` 버튼 수 테스트 | 지킴 |
| 큰 터치 영역·간격 (DS: Pleos 버튼 가이드를 따르라) | 주 버튼은 전체 폭 또는 반 폭의 큰 버튼이다(스크린샷 04·05) | `ui/components` `PauseButton` | 확인 필요 — 가이드의 숫자를 읽지 못함 |
| 키보드보다 프리셋·최근 항목·음성 우선 (DS) | 주행 화면에 입력 칸이 없다. 증상 메모 입력 칸은 정차 중 동물병원 화면에만 있고, 음성 받아쓰기·증상 칩을 함께 둔다 | `DriveScreens.kt`, `PetScreens.kt` 동물병원 화면 · `DS-01` | 지킴 |
| 주행 중 숫자·그래프 최소 (같이가개 원칙, DS "minimal text") | 주행 화면에 주행 자극 숫자·그래프가 없다(예보 한 줄만). 보이는 숫자는 다음 쉼표까지 시간·거리, 남은 거리·도착 시각 같은 길 안내 값뿐이다. 그래프는 정차 중 "주행 자극 보기"와 도착 후 리포트에서만 | ScreenRouterTest `ST-02`, `docs/states.md` | 지킴 |
| 음성으로 손을 떼고 말하고 듣기 (UI, DS) | 쉼표 추천은 TTS로 묻고 8초 동안 "응/나중에"를 듣는다. 실패하면 버튼으로 넘어간다. 병원 전달 동의는 "네/아니요". 예보·안전 안내는 TTS로 말하고, 오디오 포커스·우선순위 큐를 쓴다 | `trip/ReplyGate.kt`, `voice/.../AndroidAudioFocus.kt` · ReplyGateTest | 일부 — **주행 중 "콩이 토했어" 음성 신고는 마이크와 이어져 있지 않다**(발표 모드·adb로만 들어감). 화면 힌트 문구는 말로 신고할 수 있다고 안내한다 ([data_inventory.md](data_inventory.md) 불일치 3) |
| 1~2초 안에 읽힘 (UI) | 위 글자 수·버튼 수 제한으로 설계했다 | DrivingRules 주석 | 확인 필요 — 실제 시선 시간 측정은 하지 않음 |
| 한 손 조작, 손발 동작 요구 금지 (UI) | 버튼 하나 누르기만 있다. 끌기·두 손 동작이 없다. 분할 창 손잡이는 시스템 기능 | 스크린샷 02~05 | 지킴 |
| 색 대비 4.5:1 이상 (UI) | 계산값(WCAG 식)은 아래와 같다 | `ui/theme/Color.kt` | 일부 — 라이트 테마 파란 버튼 흰 글자 **4.23**, 배경 위 보조 글자 **4.34** |
| 핵심 정보 글자 4mm 이상, 일반 3mm 이상 (UI) | 글자 크기 = 기준값 × 배율(화면 폭/1280, 1~2). 제목 24·지표 36·본문 16·캡션 12(배율 1 기준) | `ui/theme/Type.kt` | 확인 필요 — 실차 화면의 실제 mm를 재지 않음 |
| 아이콘은 글자 라벨과 함께 (UI) | 경고·병원·마이크 아이콘 옆에 늘 글자가 있다 | `DriveScreens.kt` | 지킴 |
| 낮·밤 모드 (UI) | 시스템 다크 모드를 따라 라이트·다크 색을 바꾼다 | `ui/theme/Theme.kt` `isSystemInDarkTheme()`, `scripts/ui_capture.py --dark` | 지킴 |
| 긴급 도움 (11/03) — 주행 중 새 버튼 없음 (DS) | 긴급 도움은 정차 화면 위 겹침이다. 화면 선택이 주행·RESTRICTED면 주행 화면을 먼저 고르고, 주행이 시작되면 겹침을 닫는다. 주행 중 응급 말은 기존 한 줄 + 버튼 2개 그대로. 자동 전화·자동 경로 변경 없음(경로는 [경로 안내]를 누를 때만, 전화는 번호·QR만) | `presentation/ScreenRouter.kt`, `ui/screens/EmergencyScreen.kt` · ScreenRouterTest `F10-05` | 지킴 |
| 잠깐 대기 화면 (11/03 한눈에 크게) | P단 대기 중에만 보이는 바깥 사람용 화면(주행이면 즉시 주행 화면, F3-04). 자동 애니메이션 없음(경과 시간 숫자만 갱신), 제한 시간을 넘겨도 빨강·깜박임 없이 아래 줄만 앰버. 다크 모드는 다크 토큰 | `ui/screens/WaitScreen.kt` · WaitGlanceTest F3-10~13 | 지킴 |
| 알림: 운전에 필요한 것만, 광고 없음 (UI, RG-D) | 시스템 알림(Notification)을 쓰지 않는다. 안내는 앱 화면 한 줄과 TTS뿐. 광고 없음 | grep `NotificationManager` 0건 | 지킴 |
| 입력 뒤 0.25초 안에 반응, 늦으면 진행 표시 (UI, RG-P) | 음성 듣는 중 "듣고 있어요…", 메모 받아쓰기 "듣는 중…"을 표시한다 | `DriveScreens.kt:441`, `PetScreens.kt` | 확인 필요 — 반응 시간을 재지 않음 |
| 10초 안에 실행·콘텐츠 로드, 버튼 2초 안 반응 (RG-P) | 서버·LLM은 2.5초 타임아웃 뒤 템플릿·내장 데이터로 넘어간다 | `MainViewModel` `withTimeoutOrNull(2_500)` | 확인 필요 — 실행 시간을 재지 않음 |
| 다시 열면 마지막 상태 복원 (RG-P, RG-D) | 실차 모드는 여행을 30초마다 저장하고, 3시간 안에 같은 목적지면 이어서 안내한다. 화면 상태 자체는 ViewModel이 유지한다 | `trip/TripResume.kt` | 일부 — 시뮬레이터·발표 모드는 복원하지 않음 |
| 위험 운전 부추기지 않기 (콘텐츠 안전) | 굽은 길 예보는 "천천히 가면 덜 흔들려요"로 감속만 권한다. 속도 숫자는 "○km/h 아래로"처럼 낮추는 방향만 쓴다 | `docs/wording.md` | 지킴 |
| 주의를 빼앗는 활동(게임·웹·운동) 유도 금지 (DS) | 쉼표 활동(산책 등)은 정차(PAUSE) 화면에만 있다 | `ScreenRouter` | 지킴 |
| 분할 창·해상도 변경에도 같은 기능, 호환 모드 금지 (RG-P) | 창 폭 단계 FULL·HALF·1/3에서 같은 기능을 보인다. 실제 분할 창(기본 내비 옆)으로 확인했다 | `scripts/ui_capture.py --nav/--split`, README v8 화면 | 지킴 |
| 차량 SDK 권한은 Pleos와 사전 협의·승인된 것만 선언 (RG-P) | 선언한 권한: Pleos `CAR_CLIMATE`·`CAR_ENERGY`·`CAR_DRIVE`·`CAR_DOORS`·`NAVI_ROUTE`, AAOS `CAR_SPEED`·`CAR_POWERTRAIN`·`CAR_ENERGY`·`CAR_INFO` | `core/vehicle`·`core/navi` 매니페스트 | 확인 필요 — 승인(콘솔 "Additional SDK Permissions", 프로젝트 ID)을 받지 않음 |
| 제출 앱 = 서비스 앱, 테스트용 더미 데이터 금지 (RG-P) | 실제 신호가 없으면 값을 숨긴다. 시뮬레이터 값에는 "시연 데이터"를 표시한다 | `docs/states.md` | 확인 필요 — 발표 모드·시뮬레이터·시연용 제휴 동물병원·내장 샘플 휴게소가 릴리스에 들어 있다. 심사에서 더미 데이터로 볼 수 있다 |

## 색 대비 계산값 (WCAG 상대 휘도, `Color.kt`)

| 조합 | 라이트 | 다크 |
| --- | --- | --- |
| 본문 글자 / 카드 | 16.03 | 14.92 |
| 보조 글자 / 카드 | 4.69 | 6.85 |
| 보조 글자 / 배경 (앱 바 "시연 데이터"·남은 시간 등) | **4.34** | 7.55 |
| 주의 글자 / 주의 바탕 | 4.90 | 8.14 (카드 위) |
| 파란 글자 / 카드 ("도착 오전 11:33") | **4.23** | — |
| 주 버튼 글자 / 파란 버튼 | **4.23** | 6.28 |

4.5 미만 셋은 모두 라이트 테마 파랑(`#3478E5`)과 보조 회색(`#687586`)이다. 화면 변경은 동결 전 사용자 결정이 필요하다. 색만 바꾸면 되는 안은 [../BLOCKED.md](../BLOCKED.md)에 제안으로 적었다.
