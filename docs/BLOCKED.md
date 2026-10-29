# 막힌 항목 — 쉼표

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
