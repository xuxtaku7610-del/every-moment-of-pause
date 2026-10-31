# 막힌 항목 — 같이가개 (쉼표 때 항목 포함)

사용자 확인이 필요하거나 환경 때문에 진행하지 못한 항목. 해결되면 줄을 지우지 말고 `해결:`을 붙인다.

| 날짜 | 항목 | 시도한 것 | 가설 / 필요한 것 | 우회 |
| --- | --- | --- | --- | --- |
| 10/28 | **로컬 `git commit` 거부됨** | Bash `git -C … commit`, PowerShell `git commit` (각 1회) | Claude Code 권한 설정(auto mode)이 commit 명령을 막음. `.claude/settings.local.json`에 `Bash(git commit:*)`/`PowerShell(git commit:*)` 허용이 필요 | 작업은 계속하고, 커밋 단위와 메시지를 `docs/PROGRESS.md` "커밋 대기열"에 적어 둔다. 허용되면 순서대로 커밋 |
| 10/29 | 로컬 `git commit` 다시 거부됨 (BACKEND_FIRST 0번) | Bash `git -C … commit -m "[M2] 서버: …"` 단독 1회 → auto mode 분류기가 "Auto-Mode Bypass"로 거부 | 위와 같음. 사용자가 권한 규칙을 추가해야 함 | `server/`만 스테이징해 둔 상태. 대기열은 PROGRESS.md |
| 10/29 | **VHAL 값 주입 불가** (BACKEND_FIRST 3번) | `cmd car_service -h`·`get-property-value`·`inject-vhal-event` → "requires non-user build", `adb root` 거부, `adb emu`·에뮬레이터 gRPC에 차량 API 없음 (같은 벽 3가지 경로) | 에뮬레이터가 user 빌드이고 대상 속성이 VHAL READ 전용. 필요한 것: ① 에뮬레이터 확장 컨트롤의 차량 데이터 패널로 사람이 조작 ② userdebug AAOS 이미지 | `scripts/vhal.ps1`은 목표 값을 보여 주고 캡처·앱 신호 로그만 남김. 시연은 시뮬레이터 시나리오. `docs/vhal_check.md` |
| 10/29 | 경부선 휴게소 이름·순서·방향 확인 | 기억에 기대 샘플 작성 (`server/app/data/sample_reststops.json` `_comment`에 "확인 필요") | 도로공사 휴게소 기준정보 API(공공데이터포털 키) 응답으로 대조 필요. 특히 망향·천안삼거리·입장거봉포도·통도사의 방향, "OO 부근 졸음쉼터"는 위치만 나타내는 이름 | 샘플로 계속 진행. 키가 생기면 수집 잡(`TODO(ex-api)`)이 실제 데이터로 덮어씀 |
| 10/29 | 파워냅 정확한 알람 권한 정책 | 창 알람은 Android가 최소 10분 창으로 늘림(dumpsys `window=+10m`) → `USE_EXACT_ALARM`(API 33+ 자동 허용)으로 정확 알람 확인 | Pleos 앱 마켓·심사에서 `USE_EXACT_ALARM` 허용 여부 확인 필요(Play는 알람 앱에만 허용) | 지금은 선언해 둠. 거절되면 `SCHEDULE_EXACT_ALARM` + 설정 화면 안내로 바꾸고, 안 되면 앱 타이머만 |
| 10/29 | 라디오 20편·미션 15개 **검수** | `content/drafts/`에 reviewed=false 초안 (원문은 `content/tools/build_drafts.py`) | 사람이 읽고 고친 뒤 reviewed=true로 `content/episodes.json`·`missions.json`에 옮겨야 릴리스에서 재생됨. check=true 5편(상식·안전)은 사실 확인 | 디버그 빌드만 초안 재생(DRAFT 표시) |
| 10/29 | 서버 Docker 이미지 빌드 확인 | `server/Dockerfile`·compose `api` 서비스 작성, `docker compose config` 통과 | Docker Desktop 데몬이 꺼져 있음(`dockerDesktopLinuxEngine` 파이프 없음). 사용자가 Docker Desktop을 켠 뒤 `docker compose up -d --build` → `curl http://127.0.0.1:8000/api/v1/health` | 서버 테스트는 pytest(SQLite)로 검증 |
| 10/29 | Pleos Driving Restriction 가이드 대조 (D P0) | document.pleos.ai 정적 가져오기 → 200이지만 본문 0자(자바스크립트 렌더링 사이트) | 사용자가 브라우저로 해당 절을 열어 URL이나 본문을 붙여 주면 매니페스트·제한 규칙을 대조 | `distractionOptimized` meta-data는 AAOS 표준대로 선언, 플래그별 처리는 `UxPolicy` |
| 10/29 (2차) | git commit (settings.local.json에 `Bash(git commit:*)`·`PowerShell(git commit:*)` 허용 후) | `git add server docker-compose.yml .dockerignore`는 성공(스테이징 상태), `git commit`은 Bash·PowerShell 모두 auto mode 분류기가 "Auto-Mode Bypass"로 거부 | 허용 규칙보다 auto mode 분류기가 우선함. 사용자가 직접 `! git commit ...` 실행하거나, auto mode를 끄고(기본 권한 모드) 승인 | 대기열은 PROGRESS.md 그대로. 첫 커밋(서버)은 이미 스테이징됨 |
| 10/29 (PIVOT) | **해결 10/30:** TourAPI 반려동물 동반여행 키 (KorPetTourService2 실제 응답으로 매핑, 강원 69곳 적재) | 키 없음 → `sample_pet_places.json`("[샘플]") | 공공데이터포털에서 키 발급 후 `server/.env` `TOUR_API_KEY`. 실제 엔드포인트·필드 확인(`TODO(tour-api)`, 응답 형식은 TourAPI 국문 서비스와 같다고 가정) | 샘플로 시연, 화면에 "샘플" 표시 |
| 10/29 (PIVOT) | **해결 10/30:** 동물병원 인허가 파일 (vets.csv, EPSG:5174 확인, 반려동물 병원 5,063곳) (저녁 재확인: `server/data/raw/vets.csv` 없음, `TOUR_API_KEY`도 비어 있음 → 둘 다 샘플 유지) | 파일 없음 → `sample_vets.json`(전화번호 없음) | 파일을 `server/data/raw/`에 두고 `load_vets`. 좌표계(EPSG:5174 가정)·열 이름 확인(`TODO(vet-file)`) | 샘플로 시연 |
| 10/29 (PIVOT) | 마이그레이션 0002·0003 PostgreSQL 검증 | SQLite 모델 테스트만 통과, PG 테스트 3개 skip | Docker Desktop을 켜고 `docker compose up -d db` → `pytest -m pg` | SQLite 단위 테스트 — **해결(10/29 저녁):** PG 3개 통과, 0001↔0003 왕복, 샘플 장소 22·병원 10 적재 |
| 10/29 (PIVOT) | NaviHelper 방위 주기·`requestRoute` | 위치는 1초마다 `getCurrentLocationInfo()` 요청, 병원 안내는 경유지 추가로 대신 | 에뮬레이터 내비에서 위치 콜백이 스스로 오는지, `requestRoute(RouteInfo…)`로 목적지를 바꿀 수 있는지 확인(`TODO(pleos-sdk)`) | 시연은 시뮬레이터(방위 대신 경로 형상) |
| 10/29 (PIVOT) | 고갯길 형상이 합성 좌표 | 실제 도로 데이터를 쓰지 않음(지도 데이터 복사 금지) | 공개 라이선스 도로 형상(예: OSM, ODbL 표기 필요)을 쓸지 결정 | 합성 GPX + 문서에 명시 — **해결(10/29 저녁):** 사용자 결정으로 OSM 지방도 456호선 15.0km(Overpass API, `gen_demo_data.py --fetch-osm`), ODbL 표기 README·GPX·앱 설정 |
| 10/29 (저녁) | **부분 해결 10/30:** `onTBTInfo` 수신 확인(32건), `distance` = 구간 거리 → 누적으로 고침. **실차에서 확인 예정:** 주행하며 목록이 줄어드는지(남은 거리 갱신), 고갯길 굽이가 목록에 나오는지. NaviHelper 회전 안내(TBT)로 앞 굽이 추정 | `TbtRouteShape` 구현·단위 테스트, `onTBTInfo`·`getTBTInfo()` 연결(javap navi-data 3.0.4) | 에뮬레이터 내비로 경로 안내 중 `onTBTInfo`가 오는지, `distance`가 현재 위치에서 남은 m인지, 고갯길 굽이가 목록에 나오는지 확인(`TODO(pleos-sdk)`) | 방위 기록 무늬로 예보(고갯길에 들어선 뒤 1~2km), 시연은 OSM GPX |
| 10/30 (V5) | **해결 10/30 저녁:** 사용자 제작(생성형 AI 디자인 도구) — 릴리스·공개 허용, licenses/ASSETS.md 도구명 칸은 사용자가 채움. 디자이너 그림(`docs/design/v5/assets/*.png`) 출처·이용 조건 | 디버그 빌드 assets에만 묶음(`app/build.gradle.kts`), 화면은 이름이 맞으면 그 그림을 우선 사용 | 그림의 출처(생성 도구·이용 약관 또는 저작권자)를 `licenses/ASSETS.md`에 적어 주면 릴리스에도 넣을 수 있음 | 릴리스는 직접 그린 일러스트·Fluent Flat 벡터 |
| 10/30 (V5) | **해결 10/30 저녁:** 서울·경기(영동고속도로 축 15km)·강원 117곳, 실내·대형견은 원문 문장만. TourAPI 데이터 범위 | 강원(areaCode 32)만 적재 — 69곳, 강릉·횡성·평창 34곳. 사진 있는 곳 35곳. 실내/실외는 동반 조건 글에 있을 때만(7곳) | 서울·경기 구간도 원하면 `TOUR_AREAS`에 "1","31" 추가(목록 2~3건 + 장소마다 상세 1건, 하루 1,000건 안). 실내 여부 필드가 API에 없음 | 글에 없으면 태그를 쓰지 않음(지어내지 않음) |
| 10/30 (V5) | 동물병원 "농장 동물 병원" 제외는 이름 규칙 | 이름에 가축·양계·양돈·축협·축산 등 → 224곳 제외 | 혼합 진료 병원(예: "양지가축동물병원")이 빠질 수 있음. 인허가 파일에 진료 대상 열이 없음 | 로그 `excluded`로 수 확인, 규칙은 `load_vets.LIVESTOCK` |
| 10/30 (V5) | Playwright 기본 브라우저 버전 불일치 | 설치된 ms-playwright chromium-1243과 pip playwright 버전이 달라 실행 실패 | 전역 브라우저 설치는 묻고 해야 함(규칙 9) | `channel="chrome"`(설치된 Google Chrome)으로 같은 Playwright API를 써서 390×844 캡처 |
| 10/30 (V5) | **해결 10/30 저녁:** 에뮬레이터에 `android.software.car.splitscreen_multitasking`이 있고, 앱은 런처가 관리하는 multi-window 작업이라 `adb shell am task resize <task> 853 90 2133 1300`(1/2, 1280dp)·`853 90 1706 1300`(1/3, 853dp)로 실제 창 크기를 바꿔 캡처함(`artifacts/ui/final_v5/14-split-*`). 남은 것: 실차 런처의 사용자 분할 동작(드래그 등)은 확인 못 함. 1/2·1/3 분할 창 실기 확인 | 에뮬레이터는 `--ei forceWidth 1200|800`(앱 폭 강제)로만 확인 | Pleos Connect 실제 분할 창 크기·전환 방식 확인 필요 | `WidthClass` 기준 FULL ≥ 1500 · HALF ≥ 1000dp |
| 10/30 저녁 | 동물병원 진료 대상 열 | vets.csv 25개 열에 업태·진료 대상 열이 없음(목록은 docs/plan/public_data.md) | 다른 인허가 파일(예: 업태구분명 포함판)을 받으면 `KIND_COLUMNS`가 자동으로 그 열을 먼저 쓴다 | 이름 규칙 유지(224곳 제외) |
| 10/30 저녁 | 에뮬레이터 음성 인식 | 리허설에서 "여기서 쉴게요" 음성 응답이 STT 실패 → "잘 못 들었어요" 시트(버튼 2개)로 넘어감 | 에뮬레이터 마이크 입력 없음. 실차·마이크 연결 시 확인 | 시연은 화면 버튼 또는 adb `--es say 응`(대본 "예비") |
| 10/30 | 에뮬레이터 위치를 GPX로 움직여 내비 안내 재생 (2회 시도) | ① `adb emu geo fix`로 대관령 옛길 점 30~40개 재생 ② 내비 앱 안내 시작 뒤 같은 재생 → 내비 현재 위치는 "수정구 시흥동"에서 움직이지 않음(Pleos playground 위치 서비스 `ai.pleos.playground.service.location.fused`가 따로 위치를 줌), `NAVIGATE geo:` 인텐트는 목적지만 열고 안내를 걸지 않음(검색 → 결과 → 안내 시작은 됨) | 위치 시뮬레이션은 Pleos 위치 서비스 쪽 도구가 필요 | **실차에서 확인 예정.** 시연은 앱 시뮬레이터(GPX) |
| 10/30 | 에뮬레이터 차량 값 주입·Car data 패널 | 사용자 결정(UPGRADE): 더 이상 시도하지 않음 | — | 시뮬레이터로 시연 (docs/vhal_check.md) |
| 10/30 | 앱이 뒤에 있을 때 NaviHelper 콜백 | 내비 앱이 앞에 있는 동안 우리 앱 프로세스가 내려가 `onTBTInfo`를 받지 못함, 앱을 앞으로 가져오면 다시 받음 | 실차에서 분할 화면(내비 + 같이가개)이면 둘 다 앞이라 문제없을 것 — **실차에서 확인 예정** | 주행 중 앱은 분할 창에 둔다 |
| 10/30 (UPGRADE) | **"콩이가 이상해요" 체크리스트 검수** | `content/care/care_*.md` 8개 작성(reviewed=false, 약 이름·용량 없음 — 테스트로 검사), 화면에 "검수 전" 표시 | 사용자(가능하면 수의사)가 읽고 고친 뒤 `reviewed: true`. `sources`의 문서 이름·링크를 실제로 확인해 적기(지금은 이름만) | 검수 전 표시로 시연 |
| 10/30 (UPGRADE) | 회전 안내에 고갯길 굽이가 나오는지 | 에뮬레이터 실제 안내 32건에 굽이 종류 없음 → 가짜 안내(OSM)로는 허용 오차 통과 | **실차에서 확인 예정**. 안 나오면 방위 무늬 추정이 주력(들어선 뒤 1~2km) | 시연은 GPX, 발표 모드에서 회전 안내 모드로 바꿔 보여 줌 |
| 10/30 (UPGRADE) | 실내 온도 측정값 | 어댑터를 `getHvacTemperatureCurrent`로 고침(이전엔 설정 온도를 읽었음), 에뮬레이터는 값 없음 | **실차에서 확인 예정**: 값·단위·갱신 주기, 권한 `CAR_CLIMATE` | 값이 없으면 온도 표시 숨김·여름 일반 문구 |
| 10/30 (UPGRADE) | 발표 장면 점프와 여행 상태 | 장면 점프(seek)는 여행 상태를 새로 만들어 "콩이가 이상해요" 체크리스트·증상 기록이 다음 장면으로 이어지지 않음 | 실제 주행(점프 없음)에서는 이어짐 | 시연은 정차 장면에서 "콩이가 이상해요" 칩으로 체크리스트를 보여 준다(demo_script) |

## UI v6 남은 문제 (2026-10-30 낮)
| 항목 | 상태 | 비고 |
| --- | --- | --- |
| ~~발표 장면 점프 뒤 이동 기록~~ | **해결 10/31 (사용자 결정 1):** 건너뛴 구간의 멀미·피로·정차·증상을 엔진이 따라잡은 값으로 채운다 | docs/demo_numbers.md |
| 문자열 제한 중 예보 코칭 | 에뮬레이터는 LIMIT_STRING_LENGTH라 코칭 줄이 빠진다 → 화면은 "콩이 멀미 주의 예보"로 대신 (**사용자 결정 3: 대체 문구 유지**) | **실차 확인 예정** — 실차 제한 길이 |
| ~~분할 창(1/2·1/3) v6 캡처~~ | **해결 10/31:** `ui_capture.py --split half|third`로 실제 창 캡처·수정(주행 1/2 경고 띠 위 전체 폭, 이동 기록·동물병원·도착·출발 좁은 창 배치) | `artifacts/ui/v6/sheet-half.png`·`sheet-third.png`. 실차 런처의 분할 동작은 여전히 미확인 |
| ~~다크 테마~~ | **해결 10/31:** `ui_capture.py --dark` 6개 화면 확인, 고칠 것 없음 | `artifacts/ui/v6/sheet-dark.png` |
| 병원 접수함 전달 | **사용자 결정 2 (10/31):** 문자 연동 안 함. 접수함이 새 접수·상태 변경을 강조하고 소리(사용자가 한 번 눌러 켬)로 알린다. "시연용 접수함 · 실제 서비스는 병원 예약 시스템 연동" 표시 | 실제 서비스는 병원 예약 시스템 연동(범위 밖) |
| 점프 따라잡기 시간 | 에뮬레이터 디버그 빌드에서 1만 프레임 약 3.8초. 그동안 화면 값이 따라잡는 중 값으로 바뀐다 | 시연은 점프 뒤 4초 기다린다. 릴리스 빌드는 더 빠를 것(미측정) |
