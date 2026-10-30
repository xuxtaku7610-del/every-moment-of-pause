# 진행 기록 — 같이가개 (이전 주제: 쉼표)

세션이 끊기면 이 파일 + `todo.md` + `docs/BLOCKED.md` + `_private/PIVOT_PROMPT.md`만 보고 이어 간다. 최신이 위.
규칙: git add·commit·push 하지 않는다. 작업 단위마다 아래 "커밋 대기열 (같이가개)"에 메시지와 파일만 남기고, 커밋은 사용자가 직접 한다.

## 다음 할 일 (바로 이어서)
1. 사용자: 커밋 대기열 2·3·4차 처리, `licenses/ASSETS.md` 【도구명】 채우기, TourAPI 키 재발급 후 `server/.env` 교체(교체 뒤 `-m app.jobs.ingest_pet_places`는 캐시 7일 안이면 호출 0건).
2. 사용자: 실제 폰으로 대기 페이지·도착 예정 페이지 열기, 실제 Pleos 분할 창 확인, 에뮬레이터 내비로 `onTBTInfo` 확인.
3. P1 남은 것: F5-03 병원 안내 중 흔들림 기준, 증상 음성 메모(지금은 고르기만), NaviHelper `requestRoute`·위치 콜백 주기.

## UPGRADE (2026-10-30~, `_private/UPGRADE_PROMPT.md`)
- 사전: 에뮬레이터 확인 — `onTBTInfo` 수신 확인(성남→강릉 32건), `distance`는 구간 거리 → `RouteTurn.fromSegments` 누적(F1-14). `geo fix`는 내비 위치를 못 움직임 → 실차 확인 예정. vhal_check.md "결정: 시뮬레이터로 시연". README "실차 확인 필요" 표.
- 1 고갯길 비교 크게: `CompareChart`(uistate, CSV·summary 파싱, DM-05 테스트) + `CompareScreen`(정지 그림, 거리 0~15km × 지수, 회색·primary 굵은 선, 끝점 55·30, 사실 3칸 −45% · +7분 12초 · 대관령 옛길 15.0km). 데이터는 `docs/plan/demo_compare.csv`·`demo_compare_summary.txt` 사본(`assets/compare/`), DemoCompareTest가 엔진 결과와 같은지 확인. 발표 모드 "고갯길 비교 크게 보기"·adb `--ez compare true`, 주행이 시작되면 닫힘. 캡처 `artifacts/upgrade/01-compare.png`.
- 2 실내 온도: javap로 확인해 보니 기존 어댑터가 **설정 온도**(`getHvacTemperature`)를 실내 온도로 읽고 있었다 → 측정값 `checkHvacTemperatureCurrentCapabilities`·`registerHvacTemperatureCurrent`·`getHvacTemperatureCurrent(CELSIUS)`로 고침(지원·권한은 기존 capability 경로). `CabinHeat`(engine:pet, 26℃ 주의·30℃ 강한 경고·대기 중 2℃ 상승, 근거 주석) + 테스트. 값이 있으면 대기 "실내 24℃"·오르면 배너, 도착 경고 단계별 숫자, 폰 페이지 배너(`wait_sessions.cabin_temp_start_c`). 값이 없으면 온도 표시 숨김, 6~9월에만 "여름에는 차 안이 빨리 더워져요"(숫자 없음). 설정 "실내 온도 측정값 (대기·도착 경고)". 시뮬레이터 장면은 시나리오의 실내 온도 값 사용.
- 3 증상 음성 메모: 동물병원 화면(정차 중) 버튼(구토·설사·헐떡임·축 처짐·떨림·기타) + "음성 메모"(Android STT 한 번 받아쓰기, 결과를 입력칸에 그대로·고칠 수 있음, 에뮬레이터는 adb `--es memo`로 대신). `VetMemo`(테스트 F5-06) + `SymptomMemoStore`(기기에만, 내 데이터 삭제에 포함). 체크박스 "도착 예정 공유에 증상 메모와 오늘 요약 넣기"를 켰을 때만 `/eta`에 symptoms·memo·tripLine("이동 2시간 10분 · 멀미 주의 1회(최고 46)") — 서버 `eta_shares.memo·trip_line`. 주행 중에는 화면 입력 없음(7번 음성 흐름이 증상 이벤트를 남김).
- 4 개인 맞춤 보정: `MotionCalibration`(engine:motion, 로그 계수·η 0.6·3회부터·0.6~1.6, 테스트 F7-05) → `PetProfile.motionScale`가 민감도에 곱해짐(다음 여행부터). 이동 기록 화면 위 "오늘 콩이 멀미했어요?" 네·조금·아니요·건너뛰기(도착 확인 뒤 한 번, 음성으로도 묻고 `MotionAnswerParser`로 해석). `CalibrationStore`(기기에만, 반려견별 30개, 내 데이터 삭제에 포함). 프로필 "콩이 맞춤 정도: 기록 n회, 민감도 보통 → …" + 맞춤 초기화. 근거 motion_model.md "개인 보정".
- 5 회전 안내만으로 예보: `GuidanceFromShape`(OSM 고갯길 → 가짜 NaviHelper 안내 87건, 10초마다 앞 3km) + `TbtRouteShape`. `TbtOnlyForecastTest` — 미리 정한 허용 오차(예보 비율 ≥70% · 시작 차이 ≤600m · 지속 비율 0.5~2배) 모두 통과(100% · 20m · 0.65). `PetTripScenarioTest` F1-15: 회전 안내 모드에서도 고갯길 2,620m 앞에서 예보(GPX보다 180초 늦음). 처음 짠 "지금 지수는 같다" 단정은 틀렸다(방위 없는 시나리오라 가정 원호로 54 → 77) → 실제 성질로 바꾸고 문서에 남김. 발표 모드 전환·설정 "앞 구간 형상" 표시. shape_compare.md 갱신.
- 6 장소 전국 + 필터: 17개 시·도, 날짜별 호출 장부(하루 900건, 같은 날 여러 번 실행해도 합산), 첫 전국 수집 224건(오늘 합 350/900) — **327곳**(시·도별 public_data.md), 사진 225. `large_dog_ok`·`indoor_ok`는 명시된 경우만(154/6/167, 2/8/317). 앱 `PlaceFilter`(전체/카페/식당/숙소/산책·관광 + 대형견 가능·실내 동반 가능, 정보 없음은 따로 알림, 테스트 F4-08). 휴게소 놀이터 정보와 섞지 않음.
- 7 "콩이가 이상해요": `PetDistress`(engine:pet, 키워드 분류 + 신호로 표의 각 줄, 테스트 F9-01~03) · `DistressStops`(가장 가까운 정차/놀이터 휴게소 앞당김, F9-06) · TripCoordinator(음성·adb 말 → 2줄 안내 + 정차 장소·병원, 안전 우선 음성, 부드러운 주행이면 멀미 민감도 30분 ×1.2~1.3) · 주행 화면 `DriveContent.Distress`(버튼 2개 + 하단 "일반 안내예요. 심하면 동물병원에 연락하세요") · 응급은 병원 경유지 + 증상 메모 자동 + 도착 예정 공유 · 정차 화면 "콩이 돌보기" 체크리스트(content/care 8개, 검수 전 표시, F9-05) + "콩이가 이상해요" 칩 · 이동 기록 "오늘 헐떡임 1회(출발 n분 뒤)"·공유 요약에 반영. 발표 장면 "콩이가 이상해요"(2,400초, 여름 실내 31℃ 장면 값). 에뮬레이터: 31℃ + "헥헥거려" → "에어컨을 낮추고…" + 여주휴게소 약 15분(이 구간 가장 가까운 정차 장소), 정차 칩 → 체크리스트 카드 확인.
- 마무리: `./gradlew test` 종료 코드 0 — 고유 278개 실패 0, `pytest` 74 통과. 리허설 장면 8개 모두 OK 125초(대본 3:00 유지: 7번 30초 넣고 출발·대기·고갯길 줄이고 도착 전 산책은 선택). 캡처 `artifacts/upgrade/`(01 비교 · 02 대기 온도·도착 경고 · 03 증상 메모 · 05 설정 · 07 반려견 안내 · 08 체크리스트), `artifacts/rehearsal/`. README 기능 표(F3·F4·F5·F7·F8·F9)·데이터 건수·"실차 확인 필요".

## 커밋 대기열 (같이가개 5차 — UPGRADE, 10/30 오후) — 4차 다음에
1. `[UP] 에뮬레이터 확인: TBT 구간 거리 누적, 실차 확인 표` — `app-android/core/model/src/main/kotlin/com/emop/pause/core/model/Signals.kt app-android/core/model/src/test/kotlin/com/emop/pause/core/model/RouteTurnTest.kt app-android/core/navi docs/plan/sdk_check.md docs/vhal_check.md`
2. `[UP] 고갯길 비교 전체 화면 그래프` — `app-android/core/uistate/src/main/kotlin/com/emop/pause/uistate/CompareChart.kt app-android/core/uistate/src/test/kotlin/com/emop/pause/uistate/CompareChartTest.kt app-android/app/src/main/kotlin/com/emop/pause/ui/screens/CompareScreen.kt app-android/app/src/main/assets/compare docs/plan/demo_compare_summary.txt app-android/app/src/test/kotlin/com/emop/pause/trip/DemoCompareTest.kt`
3. `[UP] 실내 온도 측정값(설정 온도 아님)·온도 단계·폰 배너` — `app-android/core/vehicle app-android/engine/pet/src/main/kotlin/com/emop/pause/engine/pet/CabinHeat.kt app-android/engine/pet/src/main/kotlin/com/emop/pause/engine/pet/WaitMode.kt app-android/engine/pet/src/test server/app/api/wait.py`
4. `[UP] 증상 음성 메모와 도착 예정 공유 첨부` — `app-android/app/src/main/kotlin/com/emop/pause/trip/VetMemo.kt app-android/app/src/main/kotlin/com/emop/pause/trip/SymptomMemoStore.kt app-android/app/src/test/kotlin/com/emop/pause/trip/VetMemoTest.kt server/app/api/eta.py server/tests/test_eta.py`
5. `[UP] 개인 맞춤 보정` — `app-android/engine/motion/src/main/kotlin/com/emop/pause/engine/motion/MotionCalibration.kt app-android/engine/motion/src/test/kotlin/com/emop/pause/engine/motion/MotionCalibrationTest.kt app-android/app/src/main/kotlin/com/emop/pause/trip/CalibrationStore.kt app-android/app/src/main/kotlin/com/emop/pause/trip/MotionAnswerParser.kt app-android/app/src/test/kotlin/com/emop/pause/trip/MotionAnswerParserTest.kt app-android/core/model/src/main/kotlin/com/emop/pause/core/model/Pet.kt docs/plan/motion_model.md`
6. `[UP] 회전 안내만으로 예보(가짜 내비)와 비교` — `app-android/engine/motion/src/main/kotlin/com/emop/pause/engine/motion/GuidanceFromShape.kt app-android/app/src/test/kotlin/com/emop/pause/trip/TbtOnlyForecastTest.kt app-android/app/src/test/kotlin/com/emop/pause/trip/PetTripScenarioTest.kt docs/plan/shape_compare.md`
7. `[UP] 장소 전국 적재·하루 호출 장부·필터` — `server/app/jobs/ingest_pet_places.py server/app/models.py server/alembic/versions/0004_place_details.py server/app/api/places.py server/tests/test_places_vets.py app-android/core/uistate/src/main/kotlin/com/emop/pause/uistate/PlaceFilter.kt app-android/core/uistate/src/test/kotlin/com/emop/pause/uistate/PlaceFilterTest.kt app-android/data docs/plan/public_data.md`
8. `[UP] "콩이가 이상해요" 규칙·체크리스트·시연 장면` — `app-android/engine/pet/src/main/kotlin/com/emop/pause/engine/pet/PetDistress.kt app-android/engine/pet/src/test/kotlin/com/emop/pause/engine/pet/PetDistressTest.kt app-android/engine/motion/src/main/kotlin/com/emop/pause/engine/motion/MotionComfortEngine.kt app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/test/kotlin/com/emop/pause/trip/DistressFlowTest.kt app-android/core/uistate app-android/app/src/main/kotlin/com/emop/pause/ui app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/build.gradle.kts content/care`
9. `[UP] 리허설·대본·녹화·문서` — `scripts/rehearsal.py docs/demo_script.md docs/recording.md README.md test_plan.md api_spec.md database.md docs/BLOCKED.md docs/PROGRESS.md`
(여러 항목이 같은 파일(TripCoordinator.kt·MainViewModel.kt·PetScreens.kt·ScreenStates.kt·0004)을 고치므로 `git add -A` 한 번으로 묶는 편이 안전하다.)

## V5 후속 (2026-10-30 저녁) — 사용자 결정 5개 + 분할 창·리허설·README·녹화
- 결정 1: 디자이너 그림 = 사용자가 생성형 AI 디자인 도구로 직접 제작 → `licenses/ASSETS.md`(도구명 【도구명】), 디버그 전용 해제(main assets, 시트·안 쓰는 원본은 APK 제외).
- 결정 2: 실내/실외·대형견을 판정하지 않음 → `space_note`·`size_note`(동반 조건 원문 문장), 화면 태그는 동반 구분만. 편의시설 목록의 "테라스"는 제외.
- 결정 3: 서울·경기(영동고속도로 축 15km) + 강원 → 117곳(서울 34·경기 14·강원 69, 사진 71). 추가 호출 50건, 캐시 120개.
- 결정 4: vets.csv에 업태·진료 대상 열 없음 → 이름 규칙 유지(224곳 제외). 그런 열이 있으면 먼저 쓰도록(`KIND_COLUMNS`, 테스트). 기준·건수 `docs/plan/public_data.md`.
- 결정 5: `app/logmask.py` — 모든 로그 레코드에서 serviceKey·api_key 등 쿼리 값을 `***`로(create_app·수집 잡에서 설치), 테스트 2개.
- 분할 창: 에뮬레이터에 car splitscreen 기능이 있고 `am task resize`로 **실제** 1280dp·853dp 창 캡처 성공(`artifacts/ui/final_v5/14-split-*`, README).
- 리허설: `scripts/rehearsal.py`(1배속, 장면 7개, 캡처 13장 `artifacts/rehearsal/`). 처음 2장면 막힘 → 원인과 고친 것: ① 서버 DB에 휴게소 0곳이라 쉼표 추천 없음 → 휴게소 적재 + 앱이 빈 서버 목록을 실패로 보고 대체(F2-09) ② 음성으로 묻는 중 추천 카드가 사라짐·결과 한 줄이 기본 배치에서만 보임 → 카드 유지·한 줄을 공통 아래쪽으로 ③ 도착 전 한 줄이 실제 데이터에서 카페("쉬어 가기") → 확인 문구 변경·대본 반영. 최종 **7장면 모두 OK, 118초**(3분 안, 대본 유지). 시트 버튼 "나중에" → "다음 휴게소로"로 통일.
- 정정: 같은 세션 중간에 "앱 테스트 249개 통과"라고 본 것은 이전 결과 파일이었다(그때 테스트 컴파일 실패 — V5MapperTest 인자). 고친 뒤 실제 `./gradlew test` 종료 코드 0, 고유 250개 실패 0. pytest 72 통과.
- 녹화: `docs/recording.md`(adb screenrecord·에뮬레이터 webm 둘 다 동작 확인, 장면별 길이 2:55, 자막 초안).

## 커밋 대기열 (같이가개 4차 — V5 후속, 10/30 저녁) — 3차 다음에
1. `[V5] 디자이너 그림 릴리스 포함·출처 표기` — `app-android/app/build.gradle.kts licenses/ASSETS.md docs/design/v5/assets app-android/app/src/debug/assets/DEMO_ASSETS.md app-android/app/src/main/kotlin/com/emop/pause/ui/components/Kit.kt docs/design/v5/STYLE_V5.md`
2. `[V5] TourAPI 동반 조건은 원문만(실내·대형견 판정 제거), 서울·경기 영동선 축 적재` — `server/app/jobs/ingest_pet_places.py server/app/models.py server/alembic/versions/0004_place_details.py server/app/api/places.py server/tests/test_places_vets.py app-android/data app-android/core/uistate app-android/app/src/main/kotlin/com/emop/pause/presentation/UiMapper.kt app-android/app/src/test/kotlin/com/emop/pause/presentation/V5MapperTest.kt api_spec.md database.md docs/ui_contract.md`
3. `[V5] 동물병원 진료 대상 열 우선, 적재 기준 문서` — `server/app/jobs/load_vets.py docs/plan/public_data.md`
4. `[V5] 로그 쿼리스트링 키 마스킹` — `server/app/logmask.py server/app/main.py server/tests/test_health.py`
5. `[V5] 리허설에서 찾은 것: 빈 휴게소 목록 대체, 음성 묻는 중 추천 카드 유지, 결과 한 줄 공통` — `app-android/data/src/main/kotlin/com/emop/pause/data/RestStopRepository.kt app-android/data/src/test/kotlin/com/emop/pause/data/RestStopRepositoryTest.kt app-android/app/src/main/kotlin/com/emop/pause/ui/screens/PetScreens.kt test_plan.md`
6. `[V5] 리허설 스크립트·대본·녹화 순서·README 화면과 데이터 건수` — `scripts/rehearsal.py docs/demo_script.md docs/recording.md README.md docs/screens/v5 docs/PROGRESS.md docs/BLOCKED.md todo.md`
(0004 마이그레이션은 3·4차가 같은 파일을 고친다 — 아직 아무 데도 배포 전이라 하나로 둔다. 2·3·4차를 `git add -A` 한 번으로 묶어도 된다.)

## V5 (2026-10-30) — 디자인 v5 + 공공데이터 실제 전환 (`_private/V5_PROMPT.md`)
- 0 디자인: 새 overview(12화면+토큰 표)를 `docs/design/v5/screens/01~13`으로 자름, DESIGNER_NOTES 값 + 픽셀 샘플로 `STYLE_V5.md`(색 라이트·다크, 글자·간격 = 기준값×배율 2, 컴포넌트, 화면 치수, 분할 창). UI_SPEC·STYLE_V3·STYLE_V4에 "폐기 — v5가 기준".
- 화면: 토큰·부품(Kit)·직접 그린 일러스트(Illustrations: 원근 도로=경로 곡률, 휴게소·병원·SUV·차·반려견·앱 표시)·화면 12종 + 온보딩 3단계(멀미 민감도 `MotionQuiz`)·설정. 계약에 v5 값 추가(ui_contract "v5에서 더한 값"). 아이콘 = Material Symbols Rounded + Fluent UI Emoji Flat 10개(벡터), 디자이너 그림(`docs/design/v5/assets`)은 디버그 빌드에서 우선 사용. 앱 아이콘 = 파란 바탕·반려견 얼굴·노란 차 adaptive 벡터.
- 검증: 장면마다 시안↔캡처 비교 최대 4회(docs/ui_review.md v5), 최종 `artifacts/ui/final_v5/`, README용 `docs/screens/v5/`. 앱 영역이 1680dp라 FULL 기준을 1500dp로.
- 공공데이터: TourAPI 실제 엔드포인트 `KorPetTourService2`(`areaBasedList2`·`detailPetTour2`), 강원 69곳 적재(사진 35, 대형견 정보 29, 실내/실외 7), 7일 파일 캐시·호출 예산 300(첫 수집 70건, 이후 0건). 동물병원 vets.csv(cp949, EPSG:5174 확인) → 영업 중 반려동물 병원 5,063곳(농장 동물 병원 224곳 제외, 좌표 없음 204곳 건너뜀). 실데이터가 들어오면 샘플 행 삭제("[샘플]" 사라짐). 실제 응답 일부를 `server/tests/fixtures/`에.
- 보안: httpx INFO 로그가 serviceKey가 든 URL을 찍는 것을 발견 → 수집 잡에서 httpx 로그를 WARNING으로. 저장소 전체에 키 문자열 없음 확인.
- 추가: 폰 대기 페이지 v5(라이트·다크, "돌아가는 중" → 차 화면 "주인이 돌아오는 중이에요"), 동물병원 도착 예정 공유(`POST /eta`, `/eta/{token}`, 위치 없음·2시간). 마이그레이션 0004.
- 테스트: `./gradlew test` 고유 249개(디버그·릴리스 실행 380) 실패 0, `pytest` 68 통과(PostgreSQL 포함).

## 커밋 대기열 (같이가개 3차 — V5, 10/30) — 2차 다음에
1. `[V5] 디자인 v5 기준 문서: 시안 자르기·STYLE_V5, 이전 디자인 폐기 표시` — `docs/design/v5/STYLE_V5.md docs/design/v5/DESIGNER_NOTES.md docs/design/v5/overview.png docs/design/v5/overview_a.png docs/design/v5/screens docs/design/v5/icon_1024.png docs/design/v5/assets/README.md docs/design/UI_SPEC.md docs/design/v3/STYLE_V3.md docs/design/v4/STYLE_V4.md` (assets/*.png는 출처 확인 후 따로)
2. `[V5] 공공데이터 실제 전환: TourAPI KorPetTourService2·캐시, 동물병원 인허가 파일, 마이그레이션 0004` — `server/app/jobs/ingest_pet_places.py server/app/jobs/load_vets.py server/app/models.py server/alembic/versions/0004_place_details.py server/app/api/places.py server/tests/fixtures server/tests/test_places_vets.py server/tests/test_schema.py .gitignore`
3. `[V5] 폰 대기 페이지 v5·돌아가는 중, 동물병원 도착 예정 공유` — `server/app/pages.py server/app/api/wait.py server/app/api/eta.py server/app/api/devices.py server/app/jobs/cleanup.py server/app/main.py server/tests/test_wait.py server/tests/test_eta.py api_spec.md database.md`
4. `[V5] 화면 계약 v5 값·온보딩·멀미 민감도 진단` — `app-android/core/uistate app-android/engine/motion/src/main/kotlin/com/emop/pause/engine/motion/MotionPhrases.kt app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripBrain.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripCoordinator.kt app-android/app/src/main/kotlin/com/emop/pause/trip/WaitShare.kt app-android/app/src/main/kotlin/com/emop/pause/di/DataModule.kt app-android/data app-android/app/src/test docs/ui_contract.md test_plan.md`
5. `[V5] 화면 v5: 토큰·부품·일러스트·12화면·앱 아이콘, 아이콘 출처` — `app-android/app/src/main/kotlin/com/emop/pause/ui app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/src/main/res app-android/app/build.gradle.kts app-android/app/src/debug/assets/DEMO_ASSETS.md app-android/gradle/libs.versions.toml licenses/ASSETS.md licenses/FluentUI-Emoji-MIT.txt`
6. `[V5] 검증 기록·README 화면·진행 기록` — `docs/ui_review.md docs/screens/v5 README.md docs/PROGRESS.md docs/BLOCKED.md todo.md`
(2차와 같은 파일(TripCoordinator.kt·TripBrain.kt·MainViewModel.kt·PetScreens.kt 등)이 겹치므로 2·3차를 `git add -A` 한 번으로 묶어도 된다. 그때 `docs/design/v5/assets/*.png`를 넣을지 먼저 정한다.)

## 커밋 대기열 (같이가개 2차, 10/29 저녁)
1. `[P2] 출발 미리보기를 주행 추천과 같은 플래너로(PetTripPreview), 점프 뒤 쉼표 화면 휴게소 이름` — `app-android/engine/planner/src/main/kotlin/com/emop/pause/engine/planner/PetTripPreview.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripCoordinator.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripRecorder.kt app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/test/kotlin/com/emop/pause/trip/PetTripScenarioTest.kt app-android/app/src/test/kotlin/com/emop/pause/trip/TripRecorderTest.kt`
2. `[P2] 서버: 마이그레이션 실행이 앱 로거를 끄지 않게` — `server/alembic/env.py`
3. `[P3] 고갯길 GPX를 OSM 대관령 옛길로(ODbL 출처 표기), 비교 실험 다시` — `scripts/gen_demo_data.py scenarios/geo/mountain_road.gpx scenarios/pet-trip.json scripts/vhal.ps1 app-android/app/src/main/kotlin/com/emop/pause/presentation/DemoScenes.kt app-android/app/src/main/kotlin/com/emop/pause/ui/screens/DemoPanel.kt app-android/app/src/test/kotlin/com/emop/pause/trip/DemoCompareTest.kt app-android/core/uistate app-android/app/src/main/kotlin/com/emop/pause/ui/screens/PetScreens.kt docs/plan/demo_compare.md docs/plan/demo_compare.csv docs/plan/demo_compare_normal.csv README.md docs/ui_contract.md`
4. `[P1] 실차 앞 구간 형상 TbtRouteShape(회전 안내·진행 방위)와 GPX 비교, 예보 구간 병합·짧은 굽이 제외` — `app-android/core/model app-android/core/navi app-android/engine/motion app-android/app/src/main/kotlin/com/emop/pause/runtime/SignalRuntime.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripBrain.kt app-android/app/src/test/kotlin/com/emop/pause/trip/ShapeProviderCompareTest.kt docs/plan/shape_compare.md docs/plan/motion_model.md docs/plan/sdk_check.md`
5. `[P4] 도착 10분 전 산책 제안(F4-03)과 발표 장면` — `app-android/app/src/main/kotlin/com/emop/pause/trip/ArrivalWalk.kt app-android/app/src/test/kotlin/com/emop/pause/trip/ArrivalWalkTest.kt`
6. `[P5] 시연 대본(폰 대기 페이지 연결 순서), test_plan·QA·진행 기록` — `docs/demo_script.md test_plan.md docs/qa_report.md docs/PROGRESS.md docs/BLOCKED.md todo.md`
(파일이 여러 항목에 걸치면(TripCoordinator.kt·DemoScenes.kt 등) 한 번에 `git add -A` 후 1개 커밋도 가능.)

## 커밋 대기열 (같이가개 1차 — 처리됨: 사용자가 `38bdcc7 같이가개 전환 작업`으로 커밋)
메시지 그대로, 파일은 경로 기준. 삭제 파일은 `git add -A <경로>`로 함께 잡힌다.
1. `[P0] 같이가개 전환: 재사용 지도·SDK 재확인 문서` — `docs/plan/REUSE_MAP.md docs/plan/sdk_check.md docs/plan/CONCEPT.md`
2. `[P0] 뺀 기능 제거: 라디오·원정대·아직 멀었어·파워냅(앱)` — `app-android/settings.gradle.kts app-android/engine/content app-android/engine/pause app-android/voice app-android/core/uistate app-android/app`
3. `[P0] 뺀 기능 제거: 콘텐츠 API·아이 답변·콘텐츠 테이블(서버, 마이그레이션 0002)` — `server/ content/`
4. `[P0] 루트 문서 7개를 같이가개로 갱신, 앱 이름` — `README.md architecture.md flowchart.md api_spec.md database.md test_plan.md todo.md app-android/app/src/main/res/values/strings.xml docs/PROGRESS.md docs/BLOCKED.md`
5. `[P1] 멀미 예보 엔진 MotionComfortEngine(대역 필터·누적량·예보)과 근거 문서` — `app-android/engine/motion app-android/core/model/src/main/kotlin/com/emop/pause/core/model/Pet.kt app-android/settings.gradle.kts docs/plan/motion_model.md`
6. `[P1] 대기 모드·타고 내릴 때 체크·이동 기록 패턴(engine:pet)` — `app-android/engine/pet`
7. `[P1] 반려견 맞춤 쉼표 PetRestPlanner(멀미 항·시설 가중치·이유)와 반려견 배변 간격` — `app-android/engine/planner app-android/engine/needs app-android/voice`
8. `[P1] 반려견 프로필·이동 기록 Room(pet.db)` — `app-android/data`
9. `[P1] 방위 신호·경로 형상(GPX)·TripBrain/TripCoordinator 연결(예보 음성·대기·도착 체크)` — `app-android/core app-android/sim app-android/app architecture.md flowchart.md test_plan.md docs/plan/REUSE_MAP.md`
10. `[P2] 시연 데이터 생성기: 합성 고갯길 GPX, pet-trip 시나리오, 영동선 휴게소·동반 장소·동물병원 샘플` — `scripts/gen_demo_data.py scenarios/ server/app/data/`
11. `[P2] 서버: 마이그레이션 0003(pet_places·vet_clinics·wait_sessions), 동반 장소 수집(TourAPI·샘플)·경로 버퍼 검색, 동물병원 적재(인허가 CSV·샘플)` — `server/app/models.py server/alembic/versions/0003_pet_tables.py server/app/jobs server/app/api/places.py server/app/api/vets.py server/app/geo.py server/app/config.py server/.env.example server/requirements.txt server/tests database.md`
12. `[P2] 서버: 대기 공유 /wait와 보호자 폰 페이지, 기록 요약 trip_summary, 삭제·정리 잡` — `server/app/api/wait.py server/app/api/llm.py server/app/llm server/app/main.py server/app/api/devices.py server/app/jobs/cleanup.py server/tests`
13. `[P2] 앱: 장소·병원·대기 공유 API, 대기 공유 업로더(WaitShare)` — `app-android/data/src/main/kotlin/com/emop/pause/data/net app-android/app`
14. `[P3] 시연 장치: 고갯길 비교 실험(MotionCompare)·결과 문서, 장면 점프 pet-trip, vhal.ps1 장면, 발표 모드 비교·신호 줄` — `app-android/engine/motion app-android/app scripts/vhal.ps1 docs/plan/demo_compare.md docs/plan/demo_compare.csv docs/plan/demo_compare_normal.csv`
15. `[P4] 화면 계약 재작성(같이가개 화면 10종)과 최소 화면, 장소·병원 저장소(내장 샘플)` — `app-android/core/uistate app-android/app app-android/data docs/ui_contract.md`
16. `[P4] 예보 속도 상한·문구 길이, 대기 공유 주소 adb` — `app-android/engine/motion docs/plan/motion_model.md`
17. `[P5] README 대응표·실행 방법, 시연 대본, QA 점검·노출 검사, 진행 기록` — `README.md docs/demo_script.md docs/qa_report.md docs/PROGRESS.md docs/BLOCKED.md todo.md .gitignore server/data/raw/README.md`

## 2026-10-29 (저녁) — 사용자 결정 반영·알려진 문제·P1
- 한 일: ① 출발 미리보기 = `PetTripPreview`(PetRestPlanner를 피로·배변 엔진과 함께 앞으로 돌림) → 미리보기·주행 모두 횡성휴게소. ② 점프 뒤 "길가" → 재생 중 이름을 비우고 실시간 기록에서 채움. ③ Docker 켜고 PG 테스트 3개 통과, 0001↔0003 왕복, 샘플 적재; alembic이 앱 로거를 꺼 로그 테스트 2개가 깨지던 것 수정. ④ TourAPI 키 없음·vets.csv 없음 → 샘플 유지. ⑤ 고갯길 = OSM 지방도 456호선 15.0km(Overpass), ODbL 표기 README·GPX·앱 설정; 비교 55 → 30(−45%, 7분 12초 더). ⑥ `RouteShapeProvider` + `TbtRouteShape`(회전 안내 원호·방위 무늬), GPX와 비교 14곳 중 12곳. ⑦ 실제 도로에서 예보가 정상 부근 굽이 하나만 "1분"으로 말하던 것 → 30초 병합·300m 미만 제외 → "3.2km 뒤 굽은 길 12분, 콩이 멀미 걱정 — 17km/h 아래로". ⑧ F4-03 도착 10분 전 산책 제안. ⑨ motion_model.md 0장 가정값 표, demo_script 폰 연결 순서.
- 검증: `./gradlew test` 243 통과, 서버 pytest 61 통과. 에뮬레이터 캡처 `artifacts/pet2/`(01 출발 미리보기, 03 쉼표 이름, 05 예보, 05b 산책 제안).
- 남은 리스크: 실차 TBT 값 미확인, 장면 점프 직후 지수는 재생을 따라잡는 몇 초 동안 0으로 보일 수 있음.

## 2026-10-29 — PIVOT 5단계: 마무리
- 한 일: README 심사 대응표(근거·확인 방법), 실행 방법(pet-trip 장면 명령, 폰 대기 페이지, vhal.ps1, 데이터 생성기), 공공데이터 출처. `docs/demo_script.md`(CONCEPT 7장 3분, 예비 대응·숫자 출처). `docs/qa_report.md` P0 점검표 재작성. 노출 검사: 280파일·커밋 작성자 — 실명·개인 이메일·키 없음, 전화번호 형태는 테스트용 가짜 `033-000-0000` 하나, `.env`·`local.properties` 추적 0.
- 검증: `./gradlew test` 230 통과(debug), 서버 pytest 58 통과·3 skip(PG).
- 다음: 사용자 확인 대기(위 "다음 할 일").

## 2026-10-29 — PIVOT 4단계: 화면 (최소)
- 한 일: `:core:uistate` 재작성 — 화면 10종(`Depart`·`Drive`(Calm·Forecast·Suggest·Heading)·`Rest`·`Wait`·`Places`·`Vets`·`Arrival`·`TripLog`·`PetProfile`·`Settings`), 이벤트, `DrivingRules`(2줄·버튼 2·이유 2), `UxPolicy`, `Fixtures`(장면 15개). `ScreenRouter`(대기 우선, 첫 실행 프로필, 위로 여는 화면), `UiMapper` 재작성, `MainViewModel` 재작성(체크·대기·장소/병원·도착 저장·요약 LLM/템플릿·증상 고르기·한계 시간). 화면 `ui/screens/PetScreens.kt` + 부품 `ui/components/Kit.kt`(애니메이션 없음), 옛 쉼표 화면·부품 삭제. `PetPlaceRepository`(서버 → 앱 내장 샘플, 경로 버퍼·주변·병원). `docs/ui_contract.md` 재작성(Mermaid 렌더 확인).
- 검증: 에뮬레이터 `Pleos_Connect_API34`에 설치, `pet-trip` 장면별 캡처 `artifacts/pet/01-depart`~`06-arrival`: 출발 체크·쉼표 추천(횡성휴게소·반려견 놀이터, STT 없어 버튼 시트)·쉼표·대기(주인이 곧 돌아와요·실내 24℃)·고갯길 예보·도착 강한 알림(시나리오 27℃) 확인. `car_service -h`는 여전히 user 빌드 예외.
- 고친 것: ① adb `--es say 잠깐 다녀올게`는 셸이 띄어쓰기로 잘라 "잠깐"만 들어감 → 따옴표(README·vhal.ps1). ② 고갯길 예보가 고속도로 100km/h 그대로 들어간다고 가정해 "멀미 걱정"이 과하게 나옴 → 예보 시뮬레이션에서 굽이마다 횡가속 3m/s²(가정) 이하로 감속하는 "안내 없이" 운전자, 지속 시간도 같은 방식(결정). ③ HIGH 예보 문구 52자 > 48 → 줄이고, 이름이 길면 이름을 뺀 문장.
- 남은 리스크: 출발 체크 "오늘의 쉼표"(TripPlanPreview)는 문막을, 주행 중 플래너는 횡성을 고른다 — 미리 보기 계획이 반려견 니즈·멀미를 모름. 쉼표 화면 메타가 점프 뒤 "길가"로 나옴(점프 재생 때 휴게소 목록이 비어 있음).

## 2026-10-29 — PIVOT 3단계: 시연 장치
- 한 일: `MotionCompare`(같은 형상·프로필로 "안내 없이 45km/h" vs "안내대로: 굽이마다 1m/s² 이하, 0.5m/s²로 미리 감속") + `DemoCompareTest` → `docs/plan/demo_compare.md`·CSV 2개. `DemoScenes` pet-trip 7장면(출발 체크 0·고속도로 210·쉼표 추천 3420·휴게소 쉼표 5240·잠깐 대기 5540+"잠깐 다녀올게"·고갯길 예보 8748·도착 체크 10820), `PetTripScenarioTest`(첫 제안 3460초 횡성휴게소·놀이터, 고갯길 3km 앞 예보, 고갯길에서 지수 +15 이상). `scripts/vhal.ps1` 장면 6개 + 앱 시뮬레이터 점프(기본) / `-Vehicle`. 발표 모드 패널: 응답 버튼("여기서 쉴게요"·"잠깐 다녀올게"), 고갯길 비교, 멀미·예보·대기·도착·방위 줄. 점프 재생 중 예보는 "말함"으로 치지 않고 버림(점프 뒤 다시 말할 수 있게).
- 검증: `./gradlew test` 통과. 비교 결과 시연 반려견 37 → 26(−30%, +3분), 보통 21 → 14.
- 결정: 첫 비교 방식(위험 구간 전체를 최저 권장 속도 25km/h로)은 13분이나 더 걸려 "조금 일찍 감속"이 아니므로 굽이별 속도 계획으로 바꿈. CONCEPT의 "82 → 41"은 예시이고 실제 모델 결과(37 → 26)를 그대로 쓴다.

## 2026-10-29 — PIVOT 2단계: 서버
- 한 일: `scripts/gen_demo_data.py`(결정적): 합성 고갯길 `scenarios/geo/mountain_road.gpx`(12.2km, 대관령 옛길의 특징만 본뜬 생성 좌표 — 실제 도로 좌표 아님, GPX 설명에 명시), `scenarios/pet-trip.json`(서울→강릉 230km, 장면 depart 0·highway 210·rest_stop 5232·wait 5532·mountain_road 8871·arrive 10696초, 콩이 소형·9살·멀미 성향 높음·배변 90분), 영동선 휴게소 7곳(이름 실제·좌표 경로 위 근사·시설 샘플), 동반 장소 22곳·동물병원 10곳(지역명 실제, 시설 "[샘플]", 전화번호 없음). 마이그레이션 0003, 모델 `PetPlace`·`VetClinic`·`WaitSession`(위치 컬럼 없음, 토큰 해시). `jobs/ingest_pet_places`(TourAPI 형식 파서·페이지 수집 `TODO(tour-api)`, 키 없으면 샘플), `jobs/load_vets`(인허가 CSV: 영업 중만, cp949/utf-8, 위도·경도 또는 EPSG:5174→WGS84 `pyproj`, 실패 행 건너뜀), `/places/along-route`·`/places/near`·`/vets/nearby`, `/wait`(POST·PUT·DELETE·GET JSON, extra=forbid로 위치 422, 기기당 1개, 2시간 만료 410) + 루트 `/wait/{token}` HTML(큰 글자·5초 새로고침·초과 빨강·noindex·no-store), `/llm/phrases` `trip_summary`(진단·치료 금칙, 숫자 확인, 템플릿 폴백), 내 데이터 삭제·정리 잡에 대기 공유. 앱: Retrofit 새 API, `WaitShare`(시작 토큰→10초 PUT→끝 DELETE, 실패 시 "연결 안 됨").
- 검증: 서버 pytest 58 통과·3 skip(PG). 앱 `WaitShareTest` 3 통과.
- 결정: 고갯길 형상은 지도 데이터를 쓰지 않고 합성(정직하게 표기). 폰 주소 앞부분은 `SHARE_BASE_URL`/adb `--es shareBase`(에뮬레이터용 10.0.2.2는 폰에서 안 열림). `pyproj`를 서버 venv에 설치(requirements 추가). 휴게소 샘플이 30곳이 되어 수집 테스트의 개수를 상수로.
- 다음: 3단계 시연 장치.

## 2026-10-29 — PIVOT 1단계: 도메인·엔진
- 한 일: `PetProfile`(core:model, 검사 포함). **`engine:motion`** `MotionComfortEngine`(a_x=Δv/Δt, a_y=v·Δψ/Δt 또는 v²κ, 축별 2차 버터워스 대역, 누설 적분 MSDV, 프로필 민감도, 지수 0~100, 앞 구간 예보·권장 속도, 예보는 상태 복사로 미리 돌림), `RouteShape`(GPX → 10m 재표본·멩거 곡률), `MotionConstants`(계수 한 곳), `MotionPhrases`(템플릿만). **`engine:pet`** `WaitMode`(P단만 시작, 경과·배터리 변화, 초과 1번, P 해제·주행 시 즉시 종료), `BoardingCheck`(출발 3항목, P단 60초+도착이면 "내렸나요?" 1번, 26℃↑ 또는 온도 모르는 여름이면 강한 알림), `PetTripLog`(최근 10번 주의 도달 분 중앙값 = 한계 시간, 병원용 요약), `Josa`. **`PetRestPlanner`**(ScoringPolicy.PET: 시각 0.3·멀미 0.2·피로 0.15·시설 0.35·간격 −0.1, 이유 Motion/PottyDue/시설, 히스테리시스·쿨다운 그대로), `RestPhrases.petSuggestion`. `Passenger.Dog(walkIntervalMin)`. Room `pet.db`(`PetDatabase`·`RoomPetStore`). 방위 신호(`Signal.HEADING`, NaviHelper `CurrentLocationInfo.heading`, 1초 위치 요청 `TODO(pleos-sdk)`), `RouteInfo.shape`(시나리오 `route.shapeGpx`), `Gpx` 읽기. `TripBrain`(멀미·예보·PetRestPlanner, 예보는 5km 안·이미 들어선 구간 제외·같은 구간 1번·10분 쿨다운), `TripCoordinator`(예보 음성·대기 모드·도착 체크·프로필 저장), `TripRecorder`(멀미 추이·주의 도달 분).
- 검증: `./gradlew test` 208개 통과(debug 기준, 새 테스트: MotionComfortEngineTest 12·MotionPhrasesTest 1·PetEngineTest 18·PetRestPlannerTest 9·PetRowsTest 3·GpxTest 1·TripRecorderTest 1·TripBrainPetTest 2).
- 결정: **멀미 대역 하한 0.1Hz → 0.02Hz** (CONCEPT 4장과 다름). 수직 기준 ISO Wf와 달리 수평 저주파는 0.2Hz 아래 감수성이 거의 일정(Donohew & Griffin 2004)하고, 고갯길 좌우 교대가 0.03~0.07Hz라 0.1Hz면 고갯길 지수가 11에 그침. 보정 결과 보통 프로필은 고갯길 25~30분에 주의(40). motion_model.md §3·architecture·test_plan 반영. `PetProfile`은 모든 엔진이 쓰므로 `core:model`에 둠. 개인 데이터는 캐시와 다른 `pet.db`(파괴적 마이그레이션 없음). 이유 문장은 실제 기여도 순(놀이터 0.35 > 멀미 0.10 같은 경우 놀이터가 먼저), 음성 문장은 반려견 이유를 앞 문장에.
- 다음: 2단계 서버.

## 2026-10-29 — PIVOT 0단계: 정리 (쉼표 → 같이가개)
- 한 일: `docs/plan/REUSE_MAP.md`(모듈·클래스별 재사용/수정/삭제). Pleos aar javap 재확인 → `docs/plan/sdk_check.md`: **폴리라인 없음** 그대로, 대신 `onTBTInfo`(회전 종류·거리), `RouteStateInfo.trafficStatus`(정체 구간), `CurrentLocationInfo.heading`(방위), `SteeringWheel.registerSteeringWheelAngle`, `Seat.registerSeatOccupancy`, `HVAC.registerHvacTemperatureCurrent`, `Fuel.registerFuelPercent`를 새로 확인(권한은 `javap -v` 어노테이션). 앱에서 `engine:content`·`RadioDirector`·`ContentRepository`·`NapAlarm`(+알람 권한·리시버)·`EtaAnswer` 삭제, `VoiceIntent`는 응답 + "잠깐 다녀올게"(대기 모드)만, 음성 우선순위 `KID_ANSWER/RADIO` → `INFO/BACKGROUND`, `PauseCurator`는 산책·물/배변·스트레칭. 서버에서 `/content/*`·`/llm/kid-answer`·콘텐츠 모델 삭제, 마이그레이션 0002(콘텐츠 테이블 drop, 이벤트 `kid_question` → `motion_peak`·`wait_started`·`wait_ended`). `content/` 삭제. 루트 문서 7개 재작성(새 테스트 ID F1-01…, DS-, SV-, DM-). 앱 이름 "같이가개".
- 검증: `./gradlew test` 통과(실패 0), 서버 pytest 39 통과·3 skip(PG, Docker 꺼짐). Mermaid(architecture·flowchart·database) `npx @mermaid-js/mermaid-cli` 렌더 13장 성공.
- 결정: applicationId·패키지·모듈 이름 유지(PIVOT 0-5). 멀미 예보의 앞 구간 형상은 SDK에 없으므로 시연 GPX → TBT 근사 → 없음 순. 대기 공유는 위치를 받지 않는다(PUT 본문 extra=forbid). 화면(`ui/`)은 4단계에서 최소 레이아웃으로 갈아 끼우므로 0단계에서는 뺀 기능 부분만 지움(아이 동승자 타일 등은 4단계에서 정리).
- 다음: 1단계 도메인·엔진.

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

## 커밋 대기열 (쉼표 — 처리됨: 사용자가 `3aa3a6c 쉼표 최종 상태`로 한 번에 커밋)
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
