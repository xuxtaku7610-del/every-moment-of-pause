# QA 점검표 — test_plan P0 (2026-10-29)

- 앱 단위 테스트: `./gradlew test` **실패 0**. 서버: `pytest` 48 통과, PostgreSQL 3 건너뜀(DB 없음).
- 근거 종류: **자동** = 단위·시나리오 테스트(이름에 ID 또는 P0 표기), **에뮬레이터** = `Pleos_Connect_API34` 확인(스크린샷은 로컬 `artifacts/`, 저장소에 없음), **미검증** = 아직 사람 확인이 필요.
- UI는 새 디자인(docs/design/v5) 대기 중이라 화면 모양 항목은 "로직 기준"으로 점검했다. 화면이 바뀌면 에뮬레이터 항목은 다시 본다.

| ID | 항목 | 근거 | 결과 |
| --- | --- | --- | --- |
| T1-1 | 프리셋 저장 → 재실행 시 "오늘도 이 멤버로 가요?" | 에뮬레이터 (재설치·재실행 후 화면, 10/29) | 통과 |
| T1-2 | 수면 "5시간" 음성 → 첫 휴식 앞당김 | 자동 `KoreanNumberTest`, `FatigueEngineTest`(수면 5h > 7h) | 로직 통과 · 음성 입력 흐름은 화면 대기 |
| T1-6 | 주행 중 프리셋 편집 차단 | 자동 `ScreenRouterTest`, `UxPolicyTest` | 통과 |
| T2-1 | 0→120분 단조 증가, 2시간 근처 HIGH | 자동 `FatigueEngineTest` | 통과 |
| T2-2 | 상위 기여 요인 3개 | 자동 `FatigueEngineTest` | 통과 |
| T2-5 | no-signal-car: 미지원 요인 빼고 재정규화 | 자동 `FatigueEngineTest`, `SignalMergeTest` | 통과 |
| T3-1 | 아이 화장실·반려견 타이머 각자 진행 | 자동 `NeedsEngineTest` | 통과 |
| T3-2 | 배터리 부족 → 충전 니즈, 급속충전 휴게소 | 자동 `NeedsEngineTest`, `RestPlannerTest` | 통과 |
| T4-1 | along-route 호출 → Room 캐시 | 자동 `RestStopRepositoryTest`, 서버 `test_reststops` | 통과 |
| T4-2 | 추천 1개 + 이유 1문장 음성 | 자동 `RestPlannerTest`, `ScoringPolicyTest`, `TripBrainScenarioTest` · 에뮬레이터(옥산·죽암 제안 음성) | 통과 |
| T4-3 | "응" → 경유지 추가 → "경로에 추가했어요" | 자동 `VoiceQueueTest`·`TripBrainScenarioTest` · 에뮬레이터(`--es say 응`) | 통과 (실제 내비 경유지 반영은 Pleos 에뮬레이터 내비 한계로 화면 확인만) |
| T4-4 | "나중에" → 10분 뒤 재평가 | 자동 `TripBrainScenarioTest`, `ScoringPolicyTest` | 통과 |
| T4-5 | 피로 HIGH → 가장 가까운 곳(졸음쉼터 우선) | 자동 `RestPlannerTest`, `TripBrainScenarioTest`(night-solo) | 통과 |
| T4-7 | 서버 꺼짐 → 캐시 → 내장 데이터 | 자동 `RestStopRepositoryTest`, `EmptyStatesTest`(오프라인 배너) | 통과 |
| T4-8 | LLM 2.5초 초과 → 템플릿 | 자동 `VoiceTest`, 서버 `test_slow_ollama_times_out_within_2_5_seconds` | 통과 |
| T4-12 | 주행 제안 카드 2줄·버튼 ≤2 | 자동 `DrivingRulesTest`, `DrivingUiScenarioTest`(시나리오 3개 전 구간) | 통과 |
| T5-1 | "아직 멀었어?" → ETA·남은 편수 답 | 자동 `VoiceQueueTest` · 에뮬레이터 | 통과 |
| T5-2 | 남은 18분 → 18분 이하 가장 가깝게 편성 | 자동 `ContentTest`, 서버 `test_playlist_closest_under_target_and_excludes` | 통과 |
| T5-5 | LLM 실패 → 템플릿 | 자동 서버 `test_kid_answer_is_template_only_no_llm`(아이 답은 항상 템플릿) | 통과 |
| T6-1 | 충전 시작 → 완료 예상 시간 → 카드 3개 | 자동 `PauseCuratorTest` | 통과 |
| T6-2 | 파워냅 → 기상 알림 | 자동 `NapAlarmPlanTest` · 에뮬레이터(정확 알람·알림 게시) | 통과 (장작 소리·조도는 화면 대기) |
| T6-3 | 원정대 미션 3개, 완료 시 라디오 열림 | 자동 `ContentTest`, `PauseCuratorTest` | 통과 |
| T6-6 | 파워냅 중 DRIVING/RESTRICTED → 즉시 주행 UI | 자동 `ScreenRouterTest`, `AppModeReducerTest` | 통과 |
| T6-7 | 파워냅 중 P 해제 → DRIVING | 자동 `AppModeReducerTest` | 통과 (실차 기어 값은 2초 확정 뒤 반영, T6-16) |
| T6-11 | 애니메이션은 PAUSE·PARKED일 때만 | 자동 `ScreenRouterTest` | 통과 |
| T6-12 | AppModeReducer 전이·경계 | 자동 `AppModeReducerTest`, `ScenarioModeTest` | 통과 |
| T6-13 | PARKED + 기어 N/D → DRIVING 유지 | 자동 `AppModeReducerTest` | 통과 |
| T6-14 | 기어 미지원: 속도 0 60초 규칙 | 자동 `AppModeReducerTest` | 통과 |
| T7-1 | 리포트: 휴식 횟수·최장 연속 주행·피로 피크·수락률 | 서버 `test_events_are_idempotent_and_report_is_stable` · 에뮬레이터(docs/ui_review.md 05 리포트) | 통과 · 앱 `TripRecorder` 단위 테스트는 없음(보강 필요) |
| T8-1 | seoul-busan-family 3분 재생 5단계 | 자동 `ScenarioModeTest`, `TripBrainScenarioTest`, `DemoScenesTest` · 에뮬레이터 | 통과 |
| T8-2 | 1×·10×·100× 전환 | 에뮬레이터(`--ei speed`) | 통과 (자동 테스트 없음) |
| T10-1 | CarUxRestrictions 변화 1초 이내 반응 | 자동 `ScreenRouterTest`·`UxLimitsTest`(화면 선택이 순수 함수) | 로직 통과 · 실측 지연은 미검증(VHAL 주입 불가, docs/vhal_check.md) |
| T10-2 | 주행 중 키보드·영상·자동 애니메이션 없음 | 자동 `DrivingRulesTest`, `DrivingUiScenarioTest`, `UxPolicyTest` | 통과 |
| T10-3 | 주행 중 운전자 참여 유도 없음 | 코드 검토: 퀴즈는 라디오가 뒷좌석 대상으로 정답까지 읽고 운전자 응답을 기다리지 않음(`RadioDirector`) | 통과 (콘텐츠 검수 때 문구 재확인) |

## 이번에 추가한 P1 (2026-10-29)
| ID | 항목 | 근거 |
| --- | --- | --- |
| T4-13 | 이유 = 실제 점수 기여 상위 1~2개 | `RestPlannerTest` |
| T6-15~17 | 실차 신호 잡음 처리(속도 3초 평균, 기어 2초·충전 5초 확정) | `SignalSmootherTest` |
| T6-18 | 실차 여행 중 앱 재시작 → 이어서 안내 | `TripResumeTest`, `RouteKeyTest` · 에뮬레이터: 출발 → `am force-stop` → 재실행 후 여행 ACTIVE 유지·30초마다 저장 확인 |

## 릴리스 빌드
- `-Ppause.minify=true assembleRelease`: R8 APK 9.9 MB(축소 없음 31.9 MB). 로컬 디버그 키로 임시 서명해 에뮬레이터 실행 → 크래시 없음, 실차 신호(AAOS·Pleos) 수신 확인. 절차는 [release.md](release.md).

## 노출 검사 (2026-10-29)
- 추적·미추적(무시 제외) 파일 254개에서 개인 이메일·로컬 사용자 경로·API 키 패턴·개인 키 블록 검색 → 발견 없음(공모전 공식 제출 주소 1건만, todo.md).
- 커밋 19개 작성자·커미터 모두 `xuxtaku7610-del` noreply, 커밋 메시지에 AI 표시 없음.
- `.env`·`local.properties`·`_private/`·`artifacts/`·`CLAUDE.md`·`AGENTS.md`는 .gitignore로 제외 확인(`git check-ignore`).
- 남은 확인: 시연 영상 화면, APK 서명 인증서의 이름 필드(release.md 참고), GitHub 프로필.
