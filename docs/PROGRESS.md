# 진행 기록 — 같이가개 (이전 주제: 쉼표)

세션이 끊기면 이 파일 + `todo.md` + `docs/BLOCKED.md` + `_private/PIVOT_PROMPT.md`만 보고 이어 간다. 최신이 위.
규칙: git add·commit·push 하지 않는다. 작업 단위마다 아래 "커밋 대기열 (같이가개)"에 메시지와 파일만 남기고, 커밋은 사용자가 직접 한다.

## 시연 영상 3분 편집본 + 릴리스 APK 확인 (2026-11-05 오후, `_private/VIDEO_CUT_PROMPT.md`)
- 시작 메모리 10.4GB. ffmpeg가 없어 `server/.venv`에 imageio-ffmpeg 0.8.1(ffmpeg 바이너리 포함) 설치 — 전역 설치 없음, requirements에는 안 넣음.
- **A 영상**: `scripts/cut_demo_video.py`(새 파일) → `artifacts/video/`(저장소 밖) `gatigage_demo_v1.mp4`(자막)·`_nosub.mp4`·`.srt`·`cut_list.md`·`narration_script.md`. 1920×1080 · H.264 veryfast crf 20 · 30fps · 무음 AAC 트랙. 총 **178.8초**, 장면 10 + 닫는 5초(앱 이름·Pleos Connect 차량용 앱·GitHub), 전환 0.3초 크로스페이드, 첫 3초 제목 자막. 장면 7은 녹화 순서(9 뒤)와 달리 7번 자리.
- 폰 겹침(5·7·10): 오른쪽 아래 작은 창을 그대로 겹치면 폰 세로 비율(390×844) 때문에 28% 폭이면 높이가 화면을 넘고 차 화면 버튼을 가린다 → 그 구간만 차 화면을 1440×810으로 줄여 왼쪽, 폰 425×920(화면 폭 22%)을 오른쪽에 둠. 자막 띠는 앱 화면 아래 끝(y≈975) 밑 시스템 바 위에만(2줄일 때도) — 프레임 확인에서 처음 2줄 띠(150px)가 "안내 종료"·[긴급 도움]을 가려 줄임.
- 자른 기준: 점프 대기·알림 창·개발자 설정 화면(02의 28~32초)·빈 화면 제외, 장면 9는 에뮬레이터 마이크 없음 문구("잘 못 들었어요") 앞 "듣고 있어요…"까지만. 장면 8 정차 그래프는 그 시점 엔진 값이라 5/100으로 낮음(비교 화면 55→30이 본론).
- 내레이션 대본은 장면 길이에 맞춰 초당 3.9~4.8글자(공백 제외), 화면에 나온 사실만. 자막·대본에 진단·측정·감지 없음("반려견 측정 아님" 고정 문구만).
- **B 릴리스 APK**(11/05 14:05 빌드): 서명 v2 · `CN=Gatigage, OU=EMOP, O=EMOP, C=KR`. 디버그 앱 지우고 설치 → 서버 끈 채 첫 설정 → 출발 → 예보 → 쉼표 → 대기 → 도착 요약 한 바퀴 통과, 발표 모드(로고 5번) 동작, APK 안 카드 12장 모두 reviewed true · 초안 0 · 장소 327 · 병원 5,063 → docs/qa_report.md 맨 위. 디버그 앱 다시 설치(onboard done) → `demo_check.ps1 -Stop`, 남은 메모리 9.6GB.
- 커밋: `_private/COMMIT_COMMANDS.md` 33번(스크립트), 기록은 32번에 함께.

## 최종 완성 (2026-11-05, 11/8 동결 전 마지막 묶음 — 새 기능 없음)
- 시작 메모리: PC 남은 17.0GB(에뮬레이터 켠 뒤 11.1GB). Docker Desktop이 꺼져 있어 켜고 `docker compose up -d db` → 서버 health ok(db·llm).
- **A README 이미지 v10** (사용자가 기본 내비 안내를 건 뒤): `docs/readme/` 12장 — 분할 창 847×1210(같이가개 쪽만): drive(기본 내비 안내 중 분할)·forecast(분할)·suggest·nearby(1/3 창 `--split third` — 분할 손잡이 12초 사이에 버튼이 지나가서), 전체 1682×1210: depart(오늘 타는 아이 보리·콩이 칩 + 지난번과 같아요, 디버그 `--es pets 콩이,보리`)·helper("쉬할 데 있어?" 답)·stimulus(이동 기록 "주행 자극 자세히 보기" 그래프 — 휴게소 정차 "주행 자극 보기"는 출발 뒤 고속도로뿐이라 5/100 평평)·emergency·wait·arrival·summary(QR, `--es shareBase http://10.0.2.2:8000` — 실제 IP 아님), phone 1704×1784(보호자 대기 페이지 + 기록 페이지 390×844 2배 나란히). 모두 `--clean`, 그림 청크만 남김. README "화면 구성" 4줄×3칸 12칸, report.png는 파일은 두고 표에서만 뺌(옛 표의 고갯길 비교·병원 접수함 칸도 12칸 지시에 따라 뺌).
- **음성 실패 문구**: 에뮬레이터는 마이크가 없어 쉼표·주변 추천 카드가 8초 뒤 "잘 못 들었어요. 버튼으로 골라 주세요."가 된다 → 캡처는 설정 "음성으로 제안"을 끈 상태(실제 설정, 카드 + 버튼 + "…라고 말해도 돼요")로 찍고 다시 켰다.
- **버그 고침 (캡처 중 발견)**: 설정 스위치(음성으로 제안·움직임 줄이기)를 눌러도 설정 화면을 다시 열 때까지 그대로 보였다 — `MainViewModel.bump()`가 같은 값 `copy()`라 StateFlow가 버림. `AppState.settingsRev`를 늘리게 고침, `SettingsRevTest`(V10-05). 화면 모양은 그대로.
- **스토어 스크린샷**: 01-depart(칩)·02-drive-split(기본 내비 안내 중 분할)·03-forecast(같은 분할)·04-suggest(버튼 상태, 앱 영역 가운데) 다시, 05~08은 v10 그대로.
- **B 대표 이미지**: `scripts/make_banner.py`(HTML + Pretendard를 Playwright·설치된 Chrome으로 찍음 — Pillow를 새로 설치하지 않으려고, PNG는 그림 청크만 남김)로 새 drive·forecast를 넣어 `docs/readme/banner.png` 1600×760 다시 만듦. 옛 배너의 C2PA 청크도 함께 없어짐(저장소 바이너리 도구 이름 검색 0).
- **C 시연 대본·녹화**: `docs/demo_script.md` 맨 앞 "3분 시연 흐름 (v10)" 표(장면 10 + 닫는 자막, 180초, 조작·화면·자막 25자 이하·내레이션 4~5음절/초), 옛 PT 3장면 표는 `<details>`로. `docs/recording.md` 0장 순서·2장 표를 같은 표로. "주행 자극은 차량 흔들림 기준 · 반려견 측정 아님"(28자)은 화면 아래 고정 2줄. `scripts/record_scenes.py` 장면을 v10 순서 10개로(녹화 전 force-stop, `captureClean`, 폰 PIP 3개 모두 390×844, 기록 페이지는 스크롤). **녹화** `artifacts/recording/2026-11-05/` 기기 mp4 10개 400초(6분 40초, 점프 대기 포함 — 편집에서 180초로) + 폰 Playwright webm 3개(05b 보호자 대기·07b 병원 접수함·10b 기록 페이지) + index.md. 스크립트 고침: 녹화 끝에 `screenrecord`가 끝날 때까지 기다리고 pull 3번 재시도(02·03 파일 없음), 버튼은 글자 정확히 일치로 찾기(7에서 제목 "…동물병원으로 안내할게요"를 눌러 접수가 안 감), 7은 전체 창이고 9 뒤에 찍는다(파일 번호는 영상 순서) — [동물병원으로]가 기본 내비 목적지를 병원으로 바꿔 안내가 끝나고 주변 추천도 멈춤. 사용자가 안내를 다시 건 뒤 2·3·9를 다시 찍음. 녹화 중 최저 남은 메모리 6.28GB.
- **D 돌봄 카드 초안**: heatstroke = 검수 카드 문구 "시원한 물로 몸을 적셔요(물에 담그지는 않아요)"로 통일(머리 이야기 뺌), foreign_object = triggers의 양말·뼈 삭제·"삼킨 것" 추가, 본문 "삼킨 것과 삼킨 시각", wait_heat 그대로, barking 보류(false). 나머지 10장 대조표 `_private/care_review_drafts.md`(페이지 12개 모두 열림) — 그대로 6 · 1문장 수정 3(potty_signal·senior·arrival_rest) · 2문장 수정 1(reststop_walk), 10장 합계 일치 25 / 부분 9 / 근거 없음 1. `reviewed`는 모두 그대로 false. content/care/README.md 초안 표 빈틈 열에 결정 반영.
- **E 점검**: `./gradlew test` 종료 0, 고유 **앱 382 → 383**(설정 스위치 고침 V10-05 뒤, 릴리스 변형 단위 테스트 포함 실패 0), pytest **102 passed**. 릴리스 assets(`mergeReleaseAssets`·`compressReleaseAssets`, 서명 없이): 카드 12장, `_drafts`·`_candidates` 0, 내장 장소 327·병원 5,063(샘플 0). 리허설 기본 3회 연속(앱 재시작 없이) 76·73·75초 모두 OK, `--all` 13장면 224초 OK, `--jumps 20` 102초 OK → docs/qa_report.md 맨 위 표. 숫자 맞추기: qa_report(369·88)·release_notes(353·82, --all 9장면)를 383·102·13장면으로, README 리허설 "70초 안팎" → "75초 안팎".
- 문구 검사: 반려견 상태 뜻 "진단"은 코드 주석·옛 디자인 문서 4곳("멀미 민감도 진단")뿐 → "흔들림에 예민한 정도(보호자 응답)". 나머지 "정상"(대관령 정상·영업/정상·[N] 정상 표기), "측정"(차량·시간 측정), "감지"(wording 금지 목록)는 반려견 상태 뜻이 아님. 【 】 0(README TODO 11/13 두 개는 HTML 주석). 생성 도구 이름: 텍스트는 .gitignore·.dockerignore 무시 규칙뿐(PROGRESS 옛 기록 3곳의 도구 이름은 지움), 바이너리는 `docs/readme/banner.png`(C2PA)뿐 — 12:20에 이 작업 밖에서 banner.png(쉼표·주변 추천 두 장)와 make_banner.py가 바뀌었고 새 파일에 다시 C2PA `caBX` 청크가 있어 그 청크만 뺐다(픽셀 같음 확인, 그림 내용은 그대로) → 저장소 그림 도구 이름·C2PA 0. 이메일은 Pleos 공개 문의 주소 1개(BLOCKED.md), 전화번호 형태는 테스트 CSV의 인허가 관리번호뿐.
- 마무리: 에뮬레이터 시연 상태 되돌림(디버그로 넣은 둘째 반려견 보리 삭제 = onboard reset → done, 음성으로 제안 켜짐, captureClean 끔), 기본 내비 안내 종료, `demo_check.ps1 -Stop`(Gradle·서버·에뮬레이터·DB 정리), 남은 메모리 11.0GB.
- 커밋: `_private/COMMIT_COMMANDS.md` 24~33번.

## 원형 반려견 사진 가장자리 (2026-11-04, 사용자 지시)
- **원인**: `pet_kongi_avatar.png`(272×271)는 그림 안에 원이 그려져 있고 원 테두리 픽셀이 반투명(짧은 변 기준 원 안 2,091px). `PetPhoto`가 이 그림을 그대로 원형으로 잘라, 가장자리에 뒤 면 색이 비쳤다(다크면 검은 띠, 라이트면 들쭉날쭉한 흰 테). 같은 부품이라 출발·주행·도착·도착 요약·프로필·온보딩·탄 아이 칩 모두 같은 문제였고, 도착 요약(160dp)에서 가장 크게 보였다.
- **고침** (`ui/components/Kit.kt` `PetPhoto`): 모든 경우 한 틀 = 자르기 → 배경(`surfaceAlt`) 채우기 → 그림. 원형 그림은 `PHOTO_EDGE_SCALE` 1.07배로 키워 반투명 테두리를 잘림선 밖으로(1.04는 39px 남음, 1.07은 0). 폰으로 올린 사진·실루엣도 같은 틀.
- **검사**: `PetPhotoEdgeTest`(V10-04) = 그 배율로 보이는 원 안 alpha<250 픽셀 0. 캡처 라이트 7화면 + 다크 2화면 `artifacts/ui/v10/sheet-photofix.png`, 다크 확대 전/후 `13c-summary-photo-edge-dark-before-after.png`.
- **캡처 교체**: `docs/readme/` summary·arrival·depart·drive(분할 창 847×1210), 스토어 `01-depart`·`07-arrival-door`(1700×1212, 같은 자르기 위치 844,89). **스토어 `02-drive-split`(README 대표 이미지)은 그대로** — 기본 내비 길 안내가 걸린 상태로 찍은 그림이라 지금 다시 찍으면 안내가 빠진다(사진 40dp라 차이도 작음). 길 안내를 건 뒤 `ui_capture.py --out hero --nav --clean hero3 01-drive`로 바꾸면 된다.
- **검증**: `./gradlew test` 종료 0, 고유 **앱 382**(+1), pytest 102 passed, 기본 리허설 5장면 OK(73초).
- 커밋: `_private/COMMIT_COMMANDS.md` 23번.

## 콩이 도우미 — 결과 (2026-11-04, `_private/HELPER_PROMPT.md`)
- **분류 순서** (앱 `engine/pet/HelperRouter`, 오프라인): 응급 말(PetDistress) → 앱 기능 6개(다음 쉼표·쉬할 곳·실내 온도·대기·충전·병원) → 카드 triggers(띄어쓰기 무시, 응급 카드 먼저) → 남은 신고 종류 → 모름(위험 낱말이면 병원 안내). 규칙이 모르고 위험 낱말이 없을 때만 서버 `POST /helper/ask`(임베딩 `embeddinggemma` ≥ 0.55 → LLM 분류 확신 ≥ 0.6 → 카드 줄만 근거로 2~3문장 → 근거 검사, 실패면 카드 줄 그대로). 서버가 없거나 7초 넘으면 규칙 결과로.
- **임계값**: 한국어 코사인 점수가 겹친다(범위 밖 최고 0.42, 맞힌 문항 최저 0.33) → 임베딩만으로는 범위 밖을 가르지 못해 VECTOR_MIN 0.55(거의 안 씀) + LLM 분류(엄격한 "직접 답할 때만" 규칙)로. `docs/plan/helper_eval.md`.
- **평가** 85문항: 85/85, 응급 우선 20/20, 금지 내용 0, 범위 밖 "잘 몰라요" 10/10, 모델 답 근거 통과 24/30(나머지는 카드 줄 그대로), 2단계 p50 843ms·p95 938ms. 첫 실행은 같은 Ollama에 다른 모델(qwen3:0.6b, 사용자 프로세스)이 올라와 있어 시간 초과가 16건(근거 14/30, p95 6.0초) — 다시 돌린 값이 위.
- **카드**: 26장 = 검수 12(머리말 id·category·triggers·voice·action 추가, 본문 그대로) + 초안 14(`content/care/_drafts`, reviewed false, 출처는 11/04 페이지를 열어 인용을 확인한 Merck·RSPCA, 빈틈은 sources 줄과 content/care/README.md 표). AVMA 안 씀.
- **릴리스 차단**: `ignoreAssetsPatterns`를 정하면 aapt 기본 `<dir>_*`가 사라져 **릴리스 assets에 `_drafts/`가 들어가 있었다** → `!<dir>_drafts`·`!<dir>_candidates` 추가. 압축 assets 확인: 릴리스 카드 12장(water 등 초안 0), 디버그 26장. 코드도 이중 차단(`HelperAnswers.catalog`·`card`), 테스트 F9-30·31·37.
- **화면**: `HelperScreen`(면 2장: 묻기 — 칩 6개·입력 칸(정차만)·[말로 묻기], 답 — 최근 답 크게 + 이전 4개 한 줄). 진입: 쉼표 화면 오른쪽 면 아래(왼쪽 칸은 꽉 차서 아래로 밀려 안 보였음 — 리허설에서 발견), 대기 아래 줄, 리포트 앱 바, 출발 전 프로필 칸. 주행 중은 [눌러서 말하기] 받아쓰기가 질문이면 음성 한 줄 + 결과 한 줄(`TripCoordinator.speakHelper`), 응급 말은 기존 흐름.
- **질문 원문**: 기기는 화면에 최근 5개(메모리), 기록은 카드 id·시각만. 서버는 저장·로그 안 함(SV-26).
- **자료 도구** `server/tools/collect_care_sources.py`: 목록 주소만·robots.txt·5초 간격·인용 1~2개만 `_drafts/_candidates/`. 11/04 water·emergency 2건 실제 실행(후보 파일 2개, 요약 칸은 비움).
- **시연**: `DemoScenes.HELPER_JUMPS` 2장면(발표 패널·adb `--es ask`), 리허설 `--all` 13장면 OK(224초) — 주행 중 "콩이가 초콜릿 먹은 것 같아" → 응급 안내 + 병원(버튼 2개), 휴게소 "쉬할 데 있어?" → 평창휴게소 30km 앞 놀이터 → [경유지로 넣기]. 기본 5장면 그대로 OK. 행동 이름 "여기서 쉴게요"는 30km 앞 곳에 어색해 "경유지로 넣기"로.
- **검증**: `./gradlew test` 종료 0, pytest 102 passed. 고유 테스트 **앱 381 · 서버 102**. 서버(:8000)는 새 라우트 때문에 같은 명령으로 다시 띄웠다.

## 커밋 대기열 (같이가개 22차 — 콩이 도우미, 11/04) — 실제 명령은 `_private/COMMIT_COMMANDS.md` 19~22번

## v10-3 편의 기능 — 결과 (2026-11-04)
- **5 리포트**: 도착 요약에서 끝 — [마치기](주) · [폰으로 기록 받기](QR, 서버 `POST /api/v1/reports` → `/report/<토큰>` 24시간, SVG 그래프·타임라인·병원 요약, 위치 칸 없음) · 작은 [자세히 보기](차 안 리포트, 오프라인). 리포트 앱 바 = 긴급 도움 + 닫기, [타임라인]/[주행 자극 자세히 보기]는 본문 위. 새 도착이면 리포트 열림·링크를 지운다.
- **3 체크 기억**: [이대로 출발]·자동 시작 때 기록(최근 3번, SettingsStore) → [지난번과 같아요], 빠뜨린 항목 앰버 "지난번에 빠뜨렸어요". 실차에서 출발 버튼은 3개를 다 해야 눌리므로 빠뜨림은 자동 시작(체크 없이 출발)에서만 생긴다 — 에뮬레이터 캡처 없음, `CheckMemoryTest`.
- **4 대기**: 보호자 폰 큰 온도 경고(`data-hot`, 측정값 있을 때만) + [경고 소리·진동 켜기](브라우저 허용 범위 — 사용자 한 번 누름), 5초마다 `#wait`만 바꿔 소리 허용 유지. 온도 없으면 경과 시간 안내. 차 화면 아래 줄 맨 앞 앰버. 보호자 2명 = 같은 링크 `?g=2`, 먼저 누른 사람 `returning_by`(마이그레이션 0007), 설정 시트 보호자 1·2 QR, 차 화면 "보호자 2가 돌아가는 중이에요".
- **2 사진**: 프로필 [폰으로 사진 올리기] → QR → `/upload/<토큰>`(10분, 1장, 5MB, 폰 브라우저가 1024px JPEG로 줄임, 서버는 앞 바이트만 확인) → 차가 3초마다 받아 `files/pets/<id>.jpg`(512px, 투명은 흰 바탕) → 서버 바이트 즉시 삭제. 서버 없음·새 프로필이면 한 줄 안내. 사진 자리 = 올린 사진 → 시연이면 콩이 → 실루엣. 처리방침(md·html)·데이터 처리 표·api_spec §2.8-3~5. 정리 작업·내 데이터 삭제에 새 표 포함.
- **1 여러 마리**: `PetProfile.id`, `PetGroup`(합친 설정·이름 찾기·"콩이만 타"), `pet_profiles` 여러 행(기존 한 마리 = 첫 행, 스키마 그대로, `defaultProfile` = 첫 저장), 프로필 탭·[아이 추가]·[이 프로필 지우기](최대 3), 출발 전 "오늘 타는 아이" 칩(카드 안), 지난번 선택은 `settings.riders`. 개인 보정은 아이별 기록, 엔진 계수 = 탄 아이 중 가장 큰 값. 신고 이름 구분, 이름 없고 여러 마리면 "누가요?"(8초) → 못 들으면 정차 화면 고르기(주행 중 버튼 2개 규칙). 시뮬레이터는 시나리오 반려견 한 마리라 "누가요?"는 시연 안 됨.
- **캡처** `artifacts/ui/v10/`: `sheet-v103.png`(출발 전 칩·지난번과 같아요 / 요약 QR / 프로필 사진 QR / 대기 보호자 1·2 QR), `v103-profile-photo.png`(업로드 뒤), `v103-phone-report.png`·`v103-phone-upload.png`·`v103-phone-wait-g2.png`(폰 390×844), `sheet-v103-end.png`. README summary·report, 스토어 08-report 다시. 시험용 프로필·사진·체크 기억은 지우고 에뮬레이터를 시연 상태로 되돌림(`--es onboard reset` → `done`).
- **검증**: `./gradlew test` 종료 0, pytest 88 passed, 리허설 기본 OK(74초)·`--all` OK(200초, 11장면). 고유 테스트 **앱 369 · 서버 88** (app 144). 리허설 출발 단계가 [이대로 출발]을 못 찾으면 이제 실패로 보고한다(전에는 조용히 넘어감).

## v10-3 편의 기능 — 계획 (2026-11-04, `_private/V10_3_FEATURES.md`, v10 UI 기준)
순서: 5 리포트 흐름 → 3 출발 체크 기억 → 4 대기 온도·보호자 2명 → 2 사진 QR → 1 여러 마리(가장 큼).
- **5 리포트**: 하차 확인 → 도착 요약 → 끝. 요약에 [폰으로 기록 받기](서버 `POST /api/v1/reports` → `/report/<토큰>` 24시간, 타임라인·흔들림 그래프(SVG, v10 띠)·병원 요약, 위치 없음) + [마치기] + 작은 [자세히 보기](차 안 리포트, 오프라인). 리포트 앱 바는 긴급 도움 + 닫기, 타임라인/주행 자극 전환은 본문 안.
- **3 체크 기억**: 지난번 체크를 기기에 저장 → [지난번과 같아요], 최근 3번 중 빠뜨린 항목은 앰버 "지난번에 빠뜨렸어요".
- **4 대기**: 온도 오름(차 신호 있을 때만, 26℃·+2℃ 기존 기준) → 보호자 페이지 큰 경고 + 진동·소리(브라우저 허용 범위, 사용자가 한 번 누른 뒤), 차 화면 아래 줄 앰버. 온도 신호 없으면 경과 시간 안내. 보호자 2명: 같은 링크 `?g=1|2`, "돌아가는 중"은 "보호자 1/2"로 표시(마이그레이션 0007 `returning_by`), 두 번째 QR은 설정 시트.
- **2 사진**: 프로필 편집 [폰으로 사진 올리기] → QR → `/upload/<토큰>`(10분, 1장, 5MB 이하, 폰 브라우저가 줄여서 올림 — 서버에 이미지 라이브러리 없음) → 차가 받아 기기 저장 → 서버 원본 지움. 서버 없으면 한 줄 안내, 사진 자리는 기본 실루엣(시연 데이터일 때만 콩이 사진). 처리방침·데이터 처리 표.
- **1 여러 마리**: 프로필 최대 3(기존 한 마리 = 첫 프로필, 스키마 그대로), 출발 전 "오늘 타는 아이" 칩(여러 개), 자동 시작은 지난번 선택, 음성 "콩이만 타". 엔진 = 탄 아이들 합친 설정(배변 간격 가장 짧은 · 예민한 정도 가장 높은 · 보정 계수 가장 큰). 신고는 이름으로, 이름 없으면 한 마리 = 그 아이 / 여러 마리 = "누가요?"(음성 8초 → 주행 중 버튼은 2마리까지). 리포트·사전 접수·대기 = 탄 아이들 이름.

## v10-2 UI 기준 다시 세우기 — 결과 (2026-11-04)
- **기준 문서** `docs/design/v10/STYLE_V10.md`: 글자 4단계(제목 24 SemiBold · 수치 36 Bold · 설명 16 Medium · 보조 14 Regular sub, 주행 중 최소 = 설명), 구분 우선순위(여백 → 구분선 → 배경 명도 → 카드, 화면당 카드 ≤ 2, 테두리·그림자 없음), 화면별 비율 표, 그래프 규칙, 부품. 예외: 잠깐 대기 큰 글(창 밖 2m).
- **코드**: `Type.kt` 글자 토큰 4개(+ 단위·버튼)만 — 옛 토큰 10개 지움(컴파일이 막음). `Kit.kt` `Panel` → `Block`(테두리 없음), `Title`·`Body`·`Caption`, `SectionHeader` → `Heading`, 새 `StimulusChart`·`StimulusLegend`·`levelBands`(낮음·보통·높음 띠). 지운 부품: Panel·V5Card·WarningBanner·SectionHeader·SectionTitle·IndexGauge·GaugeScale·ForecastChart·forecastPeakMin·RecentChart·AxisLabels·Fluent·StatCard·StatValue·toneColor.
- **화면**: 주행(다음 쉼표 시간 = 수치), 출발 전(좌 60 / 우 40, 체크 n/3 수치, 출발·긴급 도움 한 줄), 쉼표(15분 수치, 좌 40 / 우 60 한 면), 동물병원(거리·도착 수치, 왼쪽 한 면), 긴급 도움(띠 첫 줄 = 제목), 도착 요약(수치 4칸 + 구분선, 카드 안 카드 없앰), 리포트(요약 줄 박스 없음, 그래프 높이 고정 220 + 수치 3개, 56/44), 설정(카드 4 → 2), 주행 자극 보기(수치 3개 + `StimulusChart`: −10분~20분, 지나온 실선·예측 점선·지금 선, 굽은 길 앰버 면 "11:58~12:10 굽은 길"), 고갯길 비교(수치 3개 최고·평균·소요 시간, 같은 단계 띠, 직접 색 #8A94A3 → sub). 기능·흐름 변경 없음.
- **캡처** `artifacts/ui/v10/`: 전/후 `<화면>-before-after.png` 32장(라이트), `<화면>-beforedark-afterdark.png` 9장, 1/3 `sheet-third.png`·1/2 `sheet-half.png`. `docs/readme/` 10장(배너 그대로) · `docs/store/screenshots/` 8장 다시(02는 기본 내비 안내 중 분할, 찍은 뒤 안내 종료).
- **검증**: `./gradlew test` 종료 0, pytest 82 passed, 리허설 기본 OK(74초)·`--all` OK(200초, 11장면). 고유 테스트 **앱 365 · 서버 82** (app 142).

## v10-2 UI 기준 다시 세우기 — 계획 (2026-11-04, `_private/V10_2_UI.md`, 기준 문서 `docs/design/v10/STYLE_V10.md`를 먼저 씀)
기능·흐름은 그대로, 배치·글자·구분만. 글자 4단계(제목 24 · 수치 36 · 설명 16 · 보조 14), 면은 테두리 없는 `Block`(배경 명도), 화면당 카드 ≤ 2.
| 화면 | 가장 중요한 한 가지 | 바꿀 것 |
| --- | --- | --- |
| 주행(분할) | 다음 쉼표까지 시간 → **수치** | "1시간 3분 뒤"를 수치로, 장소는 제목, 거리·시설은 설명·알약. 맨 위 한 장은 상태 면 |
| 출발 전 | 체크 n/3 + 출발 | 좌 60(인사·사진 원 120·체크) / 우 40(경로·예상 시간 수치·쉼표 목록·출발). 카드 1장 + 구분선 |
| 프로필 편집 | 이름(제목) | 좌 34(사진 원 140·요약) / 우 66(입력) — 테두리 제거 |
| 온보딩 | 단계 제목 | 한 장, 그림 16:9 하나(2단계) |
| 설정 | 안내 빈도 | 2열 면 2장, 섹션 머리 = 제목, 줄은 구분선 |
| 쉼표(정차) | 쉬는 시간 **수치**("15분") | 좌 40(수치·관찰 한 줄·행동 버튼) / 우 60(돌보기·지금 하면 좋은 것 — 한 면 안 구분선) |
| 잠깐 대기 | 큰 글(v9 그대로) | 글자 토큰만 4단계로 |
| 긴급 도움 | 바로 동물병원 찾기·경고 띠 | 좌 66 / 우 34, 경고 띠 글자 ≤ 제목, 관련 안내는 구분선 목록 |
| 동물병원·사전 접수 | 가장 가까운 병원 거리 **수치** | 목록 55 / 접수 45, 목록 줄은 구분선(줄마다 박스 없음) |
| 하차 확인 | "콩이 내렸나요?" 제목 | 한 장 가운데, 사진 원 180 |
| 도착 요약 | 수치 4칸 | 수치 칸 테두리 없이 구분선(VDivider) |
| 리포트 | 요약 수치 | 위 요약 줄(수치 4칸) / 좌 44 타임라인 · 우 56 그래프(≤ 40%) + 공유, 같은 단계 띠 |
| 주행 자극 보기 | 현재 지수 **수치** | 수치 3개(현재 · 예측 최고(시각) · 위험 구간) → `StimulusChart`(지나온 실선·예측 점선·지금 선·굽은 길 앰버 면 + "12:05~12:17 굽은 길") |
| 고갯길 비교 | 최고값 차이 **수치** | 수치 3개(최고값·평균·소요 시간) → 같은 축 두 선(sub·primary) + 단계 띠, #8A94A3 직접 색 제거 |

## v10-1 주행 경험 — 결과 (2026-11-04 새벽, 아래 계획대로)
- **분할 창**: 회전·도착 카드 삭제 → 앱 바 "184km · 도착 오전 11:33". 평소 한 면(다음 쉼표 ─ 콩이 한 줄), 맨 위 한 장(예보 앰버 띠 · 쉼표/주변 추천 연한 파랑 면, 테두리 없음), 버튼 카드가 뜨면 다음 쉼표 숨김. 1/3·1/2 같은 한 열, 글자 크기 그대로. 캡처 `artifacts/ui/v10/*-after3.png`·`*-after2.png`, 격자 `sheet-after3.png`·`sheet-after2.png`.
- **[눌러서 말하기]**: 맨 아래 폭 전체(56×배율 = 112dp), 받아쓰기 8초(`TripCoordinator.listenOnce`) → `MainViewModel.reply` 기존 분류 + "조용히 해 줘"·"오늘은 안 타요"(`VoiceIntent`). 버튼 2개 카드·듣는 중·RESTRICTED면 숨김(`DrivingRules.talkShown`, 버튼 수 = 카드 2 또는 말하기 1). 마이크 없으면 한 번 묻고 거절이면 글자 힌트. 운전대 버튼은 BLOCKED(11/04)·README 검증 줄에 다음 단계 한 줄.
- **포그라운드 서비스** `service/TripService` + `TripServiceController`(PauseApp에서 붙임): 여행 ACTIVE·안 타요 아님이면 켬, 끝·안 타요면 끔. 앱이 뒤면 `say()`가 예보·쉼표·주변·응급을 상주 알림 본문으로도. 뒤에서 답이 없던 추천은 다음 정차 화면 한 줄(`RestUi.notices`). 에뮬레이터 확인은 docs/qa_report.md 맨 위.
- **자동 시작·오늘은 안 타요·빈도·조용히** (`trip/GuidancePolicy.kt`): 설정 "안내 빈도 많이/보통/적게"·"조용히 모드", 출발 전 화면 [오늘은 안 타요]/[다시 켜기], 날짜·빈도만 SharedPreferences.
- **검증**: `./gradlew test` 종료 0, pytest 82 passed, 리허설 기본 OK(74초), `--all` OK(202초, 11장면). 고유 테스트 **앱 362 · 서버 82** (app 139 · voice 31).
- 리허설 `calibrate`가 기다리던 "관찰 상태" 글자를 "콩이 ·"로(패널 이름표를 없앴다).

## 커밋 대기열 (같이가개 20차 — v10-1, 11/04) — 실제 명령은 `_private/COMMIT_COMMANDS.md` 9~11번

## v10-1 주행 경험 — 지금 동작 확인 (2026-11-04 새벽, 바꾸기 전, `_private/V10_1_DRIVE.md`)
에뮬레이터 Pleos_Connect_API34, 디버그 앱(11/03 빌드), pet-trip 시뮬레이터. 캡처 `artifacts/ui/v10/*-before3.png`(1/3)·`*-before2.png`(1/2).
- **분할 창 주행 패널**: 1/3 = 예보 띠 → 다음 회전(또는 도착 한 줄) → 다음 쉼표 → 관찰 상태 → 음성 힌트. 1/2 = 왼쪽 길 안내·관찰 상태 / 오른쪽 다음 쉼표(카드 3~4장, 테두리 각각). 회전·도착 카드는 옆 기본 내비와 겹친다. 운전자가 말을 시작하는 버튼은 **없음**(힌트 글자만, 듣기는 쉼표·주변 추천 질문 때만).
- **내비 전체 화면(앱이 뒤)**: 같이가개 프로세스는 살아 있고 시뮬레이터·엔진이 계속 돈다. 뒤에서 예보 음성이 실제로 나옴(05:16:58 `requestAudioFocus … callingPack=com.emop.pause USAGE_ASSISTANT`, 맨 위 활동 = 내비). 돌아오면 예보 카드 그대로 이어짐. **알림은 하나도 없음**(`dumpsys notification` 0건), 포그라운드 서비스 없음 → 메모리가 부족하면 시스템이 프로세스를 죽일 수 있고, 뒤에서 마이크(받아쓰기)는 Android 14 규칙상 막힌다(에뮬레이터는 마이크 입력이 없어 듣기 자체는 확인 불가, BLOCKED 10/30).
- **운전대 음성 버튼**: `adb shell input keyevent KEYCODE_VOICE_ASSIST`(앱이 앞) → 시스템 어시스턴트 `ai.umos.ambientai/.service.AiAssistantService`가 받음(`reason=assist`), 앱에는 안 옴.
- **자동 시작**: 실차 모드는 출발 버튼(체크 3개)으로만 여행 시작(`setVehicleTrip(ACTIVE)`). 목적지 + D단이어도 시작 안 함. "오늘은 안 타요"·안내 빈도·조용히 모드 없음(설정은 제안 시점·음성 제안·움직임 줄이기만).

### 공식 문서 확인 (11/04)
- FGS 유형(developer.android.com/develop/background-work/services/fgs/service-types): 엔진·TTS 유지에 맞는 표준 유형이 없어 `specialUse`(+ `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`), 뒤에서 받아쓰기는 `microphone`(RECORD_AUDIO 허용 + **앱이 보일 때 시작해야 함**: "you cannot create a microphone foreground service while your app is in the background").
- AAOS 알림(developer.android.com/training/cars/notifications): 일반 앱의 heads-up은 **IMPORTANCE_HIGH + 범주 CALL·MESSAGE·NAVIGATION일 때만**. CAR_* 범주는 시스템 권한(signature|privileged). 같이가개 안내는 셋 다 아님(내비 범주를 빌려 쓰지 않는다) → **heads-up 불가, 음성 + 상주 알림 한 줄 갱신(알림 센터)**.
- 운전대 음성 키(source.android.com/docs/automotive/voice/voice_interaction_guide/integration_flows): "PTT is handled by CarInputService", `KEYCODE_VOICE_ASSIST` → 기본 어시스턴트. 입력 가로채기 `CAR_MONITOR_INPUT`은 signature|privileged → **일반 앱 불가 → BLOCKED**. Pleos 문서에 STT SDK(`ai.pleos.playground.stt.SpeechToText`, 권한 `pleos.car.permission.STT_SERVICE`)는 있으나 aar를 받지 않았고 운전대 버튼 API는 없음 → 이번에는 쓰지 않음(규칙 1: javap 확인 전).

### 계획 (이 순서로)
1. **주행 패널(1/3·1/2 공통 한 열)**: 회전·도착 카드 삭제 → 앱 바 오른쪽 작은 한 줄 "27km · 도착 오전 11:45". 평소 = 한 면 안에 ① 다음 쉼표(장소 · 남은 시간/거리 · 놀이터 알약) ─구분선─ ② 콩이 한 줄(작은 사진 + 관찰 기록 없음/신고 n건). 필요할 때만 맨 위 한 장(예보·쉼표 추천·주변 추천·이상해요·병원 가는 길). 버튼 카드가 뜨면 다음 쉼표 덩어리는 숨김(같은 내용·높이). 글자 크기는 그대로.
2. **[눌러서 말하기]**: 맨 아래 폭 전체·높이 76dp+, 마이크 + 글자. 누르면 받아쓰기 8초 → 기존 분류(신고·응급·잠깐 다녀올게·응답) + 새 "조용히 해 줘"·"오늘은 안 타요". 버튼 카드가 떠 있거나 듣는 중이면 숨김(버튼 2개 규칙). 마이크 권한이 없으면 한 번 요청, 거절이면 글자 힌트. 운전대 버튼은 BLOCKED + README 다음 단계(README는 이번에 한 줄만).
3. **포그라운드 서비스 `TripService`**: 여행 ACTIVE ~ 끝(또는 오늘은 안 타요). 유형 `specialUse`, 앱이 보이고 마이크 허용이면 `|microphone`. 뒤에서 시작이 막히면(자동 시작) 앱이 다시 보일 때 시작. 상주 알림 "같이가개가 콩이와의 이동을 챙기고 있어요" + 앱이 뒤에 있을 때 안내(예보·쉼표·주변·응급) 한 줄로 갱신, 누르면 앱. 뒤에서도 8초 듣기(서비스가 마이크 유형일 때만), 답이 없던 추천은 다음 정차 때 화면 한 줄.
4. **자동 시작·오늘은 안 타요·빈도·조용히** (순수 로직 `AutoStart`·`GuidancePolicy` + 테스트): 실차 모드, 목적지 있음 + D단 + 5km/h 이상이면 여행 시작 → "콩이 같이 타나요?"(8초, "아니·안 타"면 오늘은 안 타요), 체크는 첫 정차 때 한 줄. 오늘은 안 타요 = 그날 안내 전부 끔·자동 시작 안 함(출발 전 화면 버튼·음성). 빈도 많이(낮음 예보까지)/보통(지금)/적게(예보 높음만·주변 추천 끔). 조용히 = 응급·쉼표 추천만 말함, 1시간 뒤 또는 정차 때 해제.
5. 문서(wording·states·ui_contract·test_plan·qa_report)·캡처 1/3·1/2·검증·COMMIT_COMMANDS.

## UI v9 마무리 (2026-11-03 밤, `_private/UI_V9_FIX_PROMPT.md`)
- **내장 데이터 = 실제 데이터**: `scripts/export_embedded_places.py`(서버 설정의 DB에서 읽음) → `app-android/data/embedded/pet_places.json` 327곳 98,335 B(tour 113·lodging 84·walk 79·cafe 26·restaurant 18·shop 7, 사진 주소 뺌) · `vets.json` 영업 중 5,063곳 1,106,235 B(이름·좌표·주소·사업장 전화). serviceKey·이메일·주민번호 꼴 검사 통과. data 모듈이 샘플 대신 이 파일을 assets로 묶는다(휴게소는 샘플 그대로). 서버 샘플 파일·서버 테스트는 그대로.
- **이용 조건 (공공데이터포털, 11/03 웹 확인)**: `행정안전부_동물_동물병원`(data.go.kr 15045050, 파일데이터) **"이용허락범위 제한 없음"**, `한국관광공사_반려동물_동반여행_서비스`(15135102) **"이용허락범위 제한 없음"** → 병원도 내장. 출처: 앱 설정 데이터 출처·파일 `source`·`licenses/ASSETS.md`·`docs/store/data_inventory.md`.
- **APK 크기**: 내장 장소·병원 압축 후 23,993 + 245,715 B(전 샘플 1,067 + 636 B) → **약 +268 KB**(릴리스 56.9 MB → 약 57.1 MB 추정). 이 PC에 서명 값(local.properties `pause.*`·PAUSE_* 환경 변수)이 없어 **릴리스 APK는 다시 만들지 못함**(`assembleRelease`가 안내 후 멈춤). 서버 없는 동작은 `EmbeddedPlacesTest`(F4-16·F5-11, `./gradlew test`가 debug·release 단위 테스트 둘 다 돈다).
- **화면**: 주변 추천 제목 = 장소 이름, 아래 "9.7km 앞 · 반려견 동반 쇼핑", 이유 "목적지에서 1.4km". 긴급 도움은 번호가 없으면 "전화번호 정보 없음" 줄 숨김(동물병원 목록은 그대로). 도착 요약 카드 세로 가운데. 하차 확인은 이미 카드 안 세로 가운데라 그대로.
- **캡처 다시**: `docs/readme/` suggest·nearby(듣는 중, 실패 문구 없음)·emergency·arrival·summary, 스토어 04-suggest·05-emergency-vet·07-arrival-door. 기본 내비 안내 종료함.
- **검증**: `./gradlew test` 종료 0, 리허설 기본 OK(71초), `--all` OK(204초, 11장면). 고유 테스트 **앱 353 · 서버 82** (app 131).

## UI v9 — 결과 (2026-11-03 밤, 아래 계획대로)
- **배변 임박 쉼표**: `PetRestPlanner` F2-16. pet-trip에서 점프 따라잡기(3,460·4,060초 제안 = 나중에) 뒤 **4,660초 횡성휴게소 10분, 이유 [배변 시간이 다가와요, 반려견 놀이터]** (`NearbyScenarioTest`, 에뮬레이터 11분·16km). 발표 장면 "배변 임박 쉼표" 4,620초.
- **주변 추천**: `trip/NearbyPick.kt`(`HighwayGuess` 5분 평균 80km/h, `NearbyPick`) + `TripCoordinator.offerNearby`(응답 창 `ReplyGate`·경유지 `addWaypoint` 재사용). pet-trip 고속도로 구간(1,800~5,000초)에는 안 뜸, **10,071초(목적지 10km) "9.7km 앞 · 야옹아멍멍해봐 강릉내곡점", 이유 "목적지에서 1.4km · 반려견 동반 쇼핑"** — 서버 DB의 TourAPI 실제 행(콘텐츠 타입 38, source_id 3544731). 목적지 3km 안 TourAPI 산책로는 없어 쇼핑이 뜬다. 발표 장면 "주변 추천" 10,040초(기존 "도착 전 산책" 9,660초 대체, `ArrivalWalk` 삭제).
- **서버**: 분류 `shop`(TourAPI 쇼핑 38) + 마이그레이션 0006, `ingest_pet_places --cache-only`로 캐시에서 재적재(외부 호출 0, shop 7건). 앱 내장 장소·병원은 샘플뿐이라 **서버 없이(릴리스 오프라인)는 주변 추천이 안 뜬다**(샘플 이름은 쓰지 않음). 병원 추천은 "산업동물" 이름 제외.
- **화면**: 주행 패널 카드 높이 = 내용, 위로 붙임, 쉼표 계획 없음 한 줄, 1/3 창 다음 쉼표 한 줄씩(관찰 상태가 밀리지 않게), 쉼표 추천·주변 추천은 카드 안에 실패 안내 + 버튼. 출발 전 전체 창 한 장 카드(`TripEndsV9.kt DepartFull`), 긴급 도움 고른 뒤 칩 줄(1/3은 2×2) + 왼쪽 카드(병원 [가까운 병원 경로 안내]·[전화]·[사전 접수]) + 오른쪽 "관련 안내", 대기 [대기 종료하기] 주 버튼, 하차 확인 아래 오늘 이동 한 줄, 도착 요약(`Route.SUMMARY`, `UiEvent.OpenReport`). 라이트·다크·전체·1/2·1/3 캡처 확인(`artifacts/ui/v9/*-after|dark3|half|third.png`, 묶음 `sheet-dark3.png`·`sheet-half.png`·`sheet-fix.png`).
- **캡처**: 전 = `docs/screens/v8/`, 후 = `docs/readme/` depart·drive·nearby·forecast·suggest·emergency·wait·arrival·summary·report(주행 4장은 같이가개 분할 창만 847×1210), 원본 `artifacts/ui/v9/*-after.png`. 스토어 8장 `docs/store/screenshots/` 교체(1700×1212). README 화면 표를 새 그림으로(주변 추천·도착 요약 추가).
- **검증**: `./gradlew test` 종료 0, pytest 82 통과, 리허설 기본 1회 OK(75초, 5장면), `--all` 1회 OK(200초, 11장면 — 배변 임박 7초·주변 추천 46초). **고유 테스트 앱 350 · 서버 82** (app 128·engine/planner 41).

## 커밋 대기열 (같이가개 19차 — UI v9, 11/03) — 실제 명령은 `_private/COMMIT_COMMANDS.md` 1~5번
1. `[서버]` shop 분류 — models·places·ingest·0006·test_places_vets
2. `[쉼표]` 배변 임박 — PetRestPlanner·RestPhrases·PetRestPlannerTest
3. `[UI v9]` 주변 추천 + 화면 — Josa·uistate 5개·trip(NearbyPick·TripCoordinator·ArrivalWalk 삭제)·presentation 3개·MainViewModel·MainActivity·screens 5개·테스트 4개
4. `[시연]` rehearsal·ui_capture·record_scenes
5. `[DOC]` README·test_plan·PROGRESS·wording·states·qa_report·release_notes·docs/readme·docs/store/screenshots

## UI v9 — 계획 (2026-11-03, `_private/UI_V9_PROMPT.md`, 바꾸기 전에 적음)
규칙: v6 토큰·Kit 부품만(새 색 없음), 기존 기능 제거 없음, 라이트·다크 · 전체·1/2·1/3 창 확인. 지도·경로 그림·판정("정상")·추정 문장·"콩이 도우미" 버튼 없음. 주행 화면 버튼 2개·글 2줄·스크롤·자동 애니메이션 없음.

**0. 사용자 결정 (11/03, 고속도로 주변 추천)** — 고속도로에서는 설 곳이 휴게소뿐 → 주변 추천(산책로·공원·마켓·병원)은 **일반 도로에서만**. 조건 1(배변 15분 전)은 쉼표 추천에 합침: 다음 휴게소가 30분 안이면 이유 첫 줄 "배변 시간이 다가와요", 30분 넘으면 반려견 놀이터가 있는 더 가까운 휴게소를 다시 찾음(졸음쉼터 제외). 응급은 지금처럼 고속도로에서도 병원 안내. 시연 = (1) pet-trip 배변 임박 쉼표 이유 변경 (2) 도착 10km 전 "야옹아멍멍해봐 강릉내곡점"(TourAPI 쇼핑) 1회.
- **고속도로 판단 방법**: NaviHelper `TBTInfo`는 `type`·`distance`·`description`(방면, 실제 값 "기업지원허브 방면")뿐이라 도로 이름이 없다(javap 3.0.4, sdk_check.md). 도로 이름은 `TrafficCongestionInfo.roadName`(정체 구간에만)뿐 → 쓰지 않음. **최근 5분 평균 속도 80km/h 이상 = 고속도로**(`RoadKind`, 속도 신호 1초 표본).

**1. 주행 중 주변 추천** (새 기능, `trip/NearbyPick.kt` 순수 로직 + `TripCoordinator`)
- 종류: 산책로·공원(TourAPI `walk` = 관광지 자연 A01) / 반려동물 마켓·용품점(TourAPI 쇼핑 38 → 새 분류 `shop`, 서버 마이그레이션 0006 + 캐시에서 재분류, 외부 호출 없음) / 동물병원(인허가). 음식점·카페 제외. `sample=true`(샘플 이름) 행은 쓰지 않음.
- 조건(일반 도로에서만, 한 번에 하나): ② 오늘 관찰 신고 있음 + 앞 5km·경로 1km 안 영업 중 병원 → 병원 ③ 목적지 10km 안 → 목적지 반경 3km 산책로 → 마켓 (기존 "도착 전 산책" `ArrivalWalk` 토스트를 이 카드로 통합). 응급·"이상해요"·예보·쉼표 추천이 떠 있으면 미룸, 같은 장소 다시 안 뜸, 추천 사이 15분, 버튼 8초 규칙(`ReplyGate`) 재사용. [들를게요] = 기존 `addWaypoint`.
- 화면: `DriveContent.Nearby`(아이콘 + "2.1km 앞 · 이름" + 이유 한 줄 + [들를게요][괜찮아요]), 음성 한 줄.
- 발표 패널 장면 "배변 임박 쉼표"·"주변 추천"(기존 "도착 전 산책" 대체), rehearsal --all에 두 장면.

**2. 주행 패널 (1/3·1/2)** `DriveScreens.kt` — 쉼표 계획 없음 = 큰 빈 카드 → 한 줄(`ArriveLine`과 같은 Panel 한 줄). 순서: 예보 띠 → 다음 회전 → 주변 추천 또는 다음 쉼표 → 관찰 상태 한 줄 → 음성 힌트. 카드 높이는 내용에 맞추고 위로 붙임(weight(1f) 빼고 아래 Spacer).

**3. 출발 전 (전체 창)** `PetScreens.kt DepartScreen` — 큰 Panel 하나: 위 인사 "오늘도 안전한 여행 되세요" + "출발 전 세 가지만 확인해 주세요" + 오른쪽 `PetPhoto`(아래 작은 글자 [프로필 바꾸기] = ButtonKind.QUIET compact). 아래 왼쪽 체크 3개(`ChecklistRow` 촘촘히), 오른쪽 경로 요약(목적지·예상 시간·예상 쉼표·첫 쉼표). 버튼 [이대로 출발](PRIMARY) + [긴급 도움](`EmergencyButton`, WARNING). 시연 띠는 그대로 아래. 1/2·1/3은 같은 순서 세로 스크롤.

**4. 쉼표 추천** `SuggestPanel` — 카드 안 위로 붙임: 장소 → 도착까지·거리 → 이유 2개(체크) → 음성 실패 안내(작은 글자) → 버튼 2개. 가운데 weight Spacer 제거, 패널 높이 = 내용.

**5. 긴급 도움** `EmergencyScreen.kt` — 고른 뒤: 왼쪽 큰 카드(앰버 띠 "바로 동물병원에 연락해요" → 본문 → [가까운 병원 경로 안내](주)[전화](보조) → 제휴면 [사전 접수] → 출처 한 줄), 오른쪽 좁은 칸 "관련 안내"(다른 상황 3개 바로가기 + "동물병원 찾는 방법" = `OpenVets`). 상황 타일은 처음에만 크게, 고른 뒤 위쪽 칩 줄(`Choice`). 1/2·1/3은 카드 → 관련 안내 세로.

**6. 잠깐 대기** `WaitScreen.kt` — 그대로, [돌아왔어요] → **[대기 종료하기]** PRIMARY. [긴급 도움][⋯ 설정] 그대로.

**7. 도착 후** — 하차 확인(`ArrivalScreen`) 질문 아래 오늘 이동 요약 한 줄(`ArrivalUi.tripLine`). [내렸어요] 뒤 새 화면 `Screen.Summary`(`SummaryUi`): 체크 아이콘 + "오늘도 함께해서 고마워요" + "콩이와의 이동을 정리했어요" + `StatCard` 4칸(이동 거리·이동 시간·휴식·관찰 신고) + 사진 + [이동 기록 보기] → `UiEvent.OpenReport` → 기존 리포트. 라우트 `Route.SUMMARY`(ARRIVED·내렸어요·리포트 안 엶).

**8. 캡처** — 라이트 `--clean`, 주행 장면은 같이가개 분할 창만 잘라 `docs/readme/` 10장, `docs/store/screenshots` 8장 교체(규격 그대로). 전 = `docs/screens/v8/`.

## 다음 할 일 (바로 이어서, 11/8 동결 전 — 사용자)
1. 커밋 대기열 11차 처리(아래, 12개). 스토어 제출 전 결정: docs/BLOCKED.md "11/02 (스토어)" 줄들.
2. 메모리: AVD 3072MB는 에뮬레이터 최소값(4096MB) 때문에 적용 불가(11/02 시도, 원래 값 유지). 남은 제안은 gradle.properties 힙 줄이기 — 시연 중에는 Chrome 탭 정리가 가장 효과적.
3. keystore 만들기(`C:\keys\`, docs/release.md 1단계) → local.properties 네 줄 또는 환경 변수(2단계) → `assembleRelease` → `apksigner verify`·설치 확인(3~4단계) → GitHub Releases에 `gatigage-v1.0.0.apk`.
4. ~~`content/care` 검수~~ → 11/03 처리(아래 "돌봄 카드 검수 반영"). 수의사 검수가 가능하면 한 번 더.
5. (11/02 처리함) 공개 문서의 도구 이름 표기 정리 — 아래 "공개 전 정리" 절.
6. ~~`licenses/ASSETS.md` 도구명~~ → 도구 이름은 적지 않음(11/03). LICENSE(11/02 추가, 공모전 심사·열람만 허용) 문구 확인, README 대응표 사업성 줄("기획서 6번 참고")이 기획서 번호와 맞는지 확인, 문 신호 실차 확인(위치 값 뜻·운전석 위치), 동물병원 데이터 이용 조건 확인.
7. 실제 폰으로 대기·도착 예정·병원 접수함 QR 열기, 시연 영상 녹화(docs/recording.md, 녹화 전 `demo_check.ps1 -Prepare -ShareBase …`, 브라우저 탭 정리).

## README 마무리 + 심사위원 첫 실행 (2026-11-03, `_private/README_FIX_PROMPT.md`)
- **빈칸 채움**: README 사업성 확대 3단계(국내 → 연동 → 해외·확장) + 2차(PT) 계획 "콩이 도우미", 상용화 사진 업로드 방식, 고객 지원 = GitHub Issues(README·listing·privacy.md·privacy.html, 이메일·전화 안 씀), 처리방침 시행일 2026-11-13. README의 빈칸 표시는 0개(영상·Releases는 `<!-- TODO 11/13 -->`).
- **시연 주행 안내 띠** (DM-10, `DemoTripHint`): 실차 모드 출발 전 화면에서 속도·기어가 10초 동안 없거나 **에뮬레이터 기기**(Build HARDWARE ranchu 등)면 화면 아래 파랑 띠 "차량 신호가 없어요. 시연 주행으로 둘러볼까요?" + [시연 주행 시작]·[닫기]. Pleos 에뮬레이터는 실차 모드에서도 속도 0(AAOS)·기어 P(Pleos SDK)를 줘서 신호 조건만으로는 안 뜸 → 기기 판별 추가(사용자 결정). 시작 = 발표 모드에서 pet-trip 고르기와 같은 `useScenario`. 닫으면 그 실행 동안 안 뜸, 주행 화면·시뮬레이터에는 없음. README 실행 4단계·listing 심사 노트·release_notes·wording 갱신.
- **에뮬레이터 확인**: `--ez vehicle true` → 온보딩 "나중에" → 10초 뒤 띠 → [시연 주행 시작] → 콩이·강릉 2시간 34분 출발 전 화면("시연 데이터"), 띠 사라짐.
- **대표 이미지 다시 찍음**: 기본 내비 강릉씨티호텔 안내 중(사용자가 건 안내가 풀려 있어 최근 목록 → 목적지로 다시 걸음) `ui_capture.py --out hero --nav --clean` → 원본 `artifacts/ui/hero/01-drive-hero2.png`(앱 새로 띄운 뒤, 처음 장은 지난 리허설의 병원 안내가 남아 버림) → 1700×1212 `docs/store/screenshots/02-drive-split.png` 교체, README 첫 이미지도 이 파일로. 캡션에서 "시연 데이터" 설명·"1/3" 뺌(이 장은 1/2 분할).
- **남은 빈칸 정리**: listing 처리방침 주소 `https://xuxtaku7610-del.github.io/every-moment-of-pause/privacy.html`(11/13 Pages를 켜면 열림 주석), 프로젝트 ID·IARC·SDK 권한 승인 = "실제 마켓 출시 신청 때 입력(공모전 단계에서는 해당 없음)". 접속 기록은 서버를 띄워 확인한 사실대로: uvicorn 기본 접속 로그가 IP:포트·요청 경로(쿼리의 좌표 포함)를 서버 실행 창·`artifacts/demo_check/uvicorn.*.log`(개발 PC)에 남김, 외부 전송 없음, 자동 삭제 없음 → "출시 전 좌표를 빼고 보관 기간·자동 삭제". 앱 자체 JSON 접속 로그(`pause.access`)는 핸들러가 없어 지금은 출력되지 않음. 【도구명】은 "도구 이름은 적지 않는다"로(ASSETS·todo·PROGRESS), release.md 처리방침 줄도 주소로. 저장소 전체 【 】 검색 0건(_private·artifacts·build·.venv 제외).
- 검증(2차): `./gradlew test` 종료 0, pytest 81 통과, 리허설 `--all` 1회 통과(144초, 9장면 OK).
- **고유 테스트 수 = 앱 339 · 서버 81** (app +7). 검증: `./gradlew test` 종료 0, 리허설 `--all` 1회 통과(141초, 9장면 OK).

## 커밋 대기열 (같이가개 18차 — README 마무리·시연 주행 띠, 11/03) — 실제 명령은 `_private/COMMIT_COMMANDS.md` 3~5번
1. `[시연] 출발 전 화면 시연 주행 안내 띠` — `DemoTripHint.kt`·`DemoTripHintTest.kt`·`MainViewModel.kt`·`PetScreens.kt`·core/uistate `ScreenStates.kt`·`UiContract.kt`
2. README 빈칸·실행 4단계 → 17차 README 커밋에 함께
3. `docs/store/privacy.md`·`docs/privacy.html`·`docs/qa_report.md`·`docs/release.md`·`licenses/ASSETS.md`·`todo.md`·`docs/store/screenshots/02-drive-split.png`(+ listing·release_notes·wording·test_plan·PROGRESS) → 문서 커밋에 함께

## README 개편 — 심사위원 기준 (2026-11-03, `_private/README_PROMPT.md`)
- README 385줄 → 약 390줄이지만 첫 화면(제목·한 줄 소개·링크 줄·대표 화면·문제 → 해결 3줄·심사 기준 표)만 펼쳐 두고, 개발자용 상세 실행·기술 스택·저장소 구조·8개 상태·개인정보·데이터 출처·참고는 `<details>`로 접음. 순서는 지시의 1~11번.
- 새 문서: `docs/features.md`(배경·단계별 표·F1~F10·뺀 것·공통 원칙), `docs/screens.md`(화면 21장 표·캡처 설명), `docs/tech_notes.md`(빌드 설정·SDK 호환성·aar 확인 결과·Playground 현황·주행 안전 규칙·DB 테스트 정책·minSdk 근거).
- 첫 화면에서 뺀 것: "가칭", "「쉼표」에서 전환", 날짜별 기록, 내부 버전 이름(v8 등) → docs/features.md·docs/screens.md. 깨져 있던 중복 대응표(옛 119~124줄)는 심사 기준 표·검증 절로 합침.
- 핵심 기능 6칸은 스토어 스크린샷(시연 표시 없는 것) 재사용, 대표 화면은 기본 내비 안내 중 실제 분할 창 `docs/screens/v8/02-drive-nav.png`(지금 내비 안내가 꺼져 있어 새로 찍지 않음, 캡션에 "시연 데이터" 설명).
- 비워 둔 것: 시연 영상·Releases 링크(`<!-- TODO 11/13 -->`). 확대 3단계·사진 업로드·고객 지원·시행일은 README_FIX에서 채움(위).
- 검사: 상대 경로 링크·이미지 전부 존재(README·features·screens·tech_notes), 앵커 링크 존재, mermaid는 mermaid-cli 임시 실행으로 렌더 확인, 금지 표현은 "반려견 측정·진단 아님" 부정문만.

## 돌봄 카드 검수 반영 (2026-11-03, 사용자 지시 · `_private/care_review.md` 기준)
- **본문**: seizure 2번("몸을 붙잡지 말고, 부딪힐 물건을 치우고 주변을 조용히 해요." — "입에 손" 뺌), emergency 4번·panting_hot 3번("시원한 물로 몸을 적셔요(물에 담그지는 않아요)", panting_hot은 뒤에 "바로 동물병원에 연락해요." 유지), panting_anxious 1·3번, nausea 2번(3번 유지), vomit 1·2번, unsure 2번 삭제(4줄 → 3줄)·4번. 나머지 줄은 그대로.
- **sources**: 대조표 "추가할 만한 페이지"대로 — AVMA·"사용자 확인 필요"·"(비움)"이 모두 없어짐(Merck·RSPCA만, bleeding·breathing·vomit은 그대로 + 검수 표시).
- **reviewed: true 12장.** 화면의 "검수 전" 알약이 사라지고 릴리스에도 응급 카드 본문이 나온다. 차단 규칙은 그대로(reviewed=false 카드면 숨김 — 테스트는 copy로 확인).
- **주행 중 음성**: 바뀐 본문과 어긋나는 줄 없음 → 그대로(응급 카드 voice 4개는 "바로 동물병원에 연락해요. …" 40자 이하, PetDistress 한 줄 안내 7개는 체크리스트 내용을 말하지 않음).
- **테스트**: CareSheetTest(reviewed=true, "사용자 확인 필요"·AVMA 없음, 시원한 물·담그지 않음, "미지근" 없음), EmergencyCardTest(reviewed=true, 지금 카드는 릴리스에도 본문), Fixtures(쉼표 돌봄 카드·응급 카드 문구·reviewed). 고유 수 그대로 332.
- **스토어 스크린샷 05** 검수 뒤 다시 찍음(`--clean`, "검수 전" 알약 없음). listing·README·release_notes·wording·BLOCKED·content/care/README·test_plan 갱신.
- **검증**: `./gradlew test` 종료 0, 리허설 `--all` 1회 통과(145초, 9장면 OK).

## 긴급 도움 작은 수정 3개 (2026-11-03, 11/8 동결 전)
1. **제휴 병원 [사전 접수]**: 긴급 도움의 가장 가까운 병원이 제휴(시연용 가상) 병원이면 [경로 안내] 아래 [사전 접수](보조) → `UiEvent.OpenVets` = 기존 동물병원 화면 사전 접수(IntakePanel) 그대로. 동물병원 화면의 NearestVet에는 버튼을 넣지 않음(옆에 사전 접수 패널이 이미 있음). 에뮬레이터: 휴게소 → 긴급 도움 → 위험 물질 → [사전 접수] → 사전 접수 패널 확인.
2. **"진료 가능 여부는 전화로 확인해요"**는 전화번호가 있을 때만(`VetUi.callNote`). 새 테스트 F10-08(`EmergencyCardTest`).
3. **스토어 스크린샷 8장 다시 찍음**: "시연 데이터" 표시 없이. 디버그 adb `--ez captureClean true`(ViewModel `AppState.captureClean`, 분할 창·다크 전환에도 유지) = `scripts/ui_capture.py --clean`, 끝나면 자동으로 false. 기본 동작은 그대로 표시(확인함). 원본 `artifacts/ui/store3/*-clean.png` → 앱 영역 1:1 → 1700×1212. 4번은 음성 8초가 지난 뒤 "잘 못 들었어요. 버튼으로 골라 주세요." 상태로 찍힘(실제 동작).
- Playwright: 사용자 허락으로 `server/.venv`에 설치(1.64.1, 브라우저는 `%LOCALAPPDATA%\ms-playwright`). `server/requirements*.txt`에는 넣지 않음(녹화 도구용). `record_scenes.py`의 웹 장면은 `server\.venv\Scripts\python scripts/record_scenes.py`로 돌리면 영상(webm)으로 찍힌다.
- **고유 테스트 수 = 앱 332 · 서버 81** (README·qa_report·release_notes 갱신).
- **검증**: `./gradlew test` 종료 0, pytest 81 통과, 리허설 `--all` 1회 통과(143초, 9장면).

## 제출 준비 묶음 (2026-11-03, `_private/PREFREEZE_PROMPT.md`)
1. **Pleos 마켓 패키지 갱신** (11/02 패키지 위에 긴급 도움·새 대기 화면 반영): listing.md(상세 설명 "긴급 도움" 절·업데이트 노트·스크린샷 표), privacy.md·privacy.html("긴급 도움에서 고른 상황은 저장·전송 안 함" 한 단락), 스크린샷 05 → `05-emergency-vet.png`(정차 중 긴급 도움 → 병원), 06 → 새 대기 화면(1700×1212, 앱 영역 1:1). data_inventory·distraction_check는 12·13차에서 이미 반영. 메타데이터 검사: 저장소 PNG 89장에 caBX·tEXt·iTXt·zTXt·eXIf 0, 새 스크린샷·아이콘도 IHDR·IDAT·IEND만.
2. **돌봄 카드 검수 대조표** `_private/care_review.md`(저장소 밖): 12장 44문장 — 일치 14 · 부분 26 · 근거 없음 4. 열어 본 페이지 Merck 4·RSPCA 5 + PDF 2, AVMA 2쪽은 본문이 비어 인용하지 않음. 주의: RSPCA "Nervous dogs"는 "Avoid reassuring your dog" — care_panting_anxious 1번(이름 부르며 쓰다듬기)과 반대. 찬물은 Merck(담그지 말 것)와 RSPCA(찬물 괜찮음)가 엇갈림 → 제안은 "시원한 물". reviewed는 그대로 false.
3. **커밋 명령 목록** `_private/COMMIT_COMMANDS.md`: 11~14차가 같은 파일에 섞여 있어 차수별로는 못 나눔 → 파일마다 한 커밋, 기능 묶음 10개(앞 커밋 위에서 빌드되는 순서). 시작 `git status`·`git status --ignored`, 끝 `git status`가 깨끗해야 정상.
4. **녹화 원본 자동 촬영** `scripts/record_scenes.py`: 1회 실행(`--no-wait`) → `artifacts/recording/2026-11-03/` mp4 10개(1920×1080, 모두 moov 있음, 합 약 260초) + `index.md`. 기본 내비 안내가 걸려 있지 않아 분할 창 장면(2·3)은 전체 창으로 찍힘(시작 전 확인이 안내 문구를 띄움 — 실제 녹화 전 사용자가 내비 안내를 건다). 병원 접수함·보호자 대기 페이지는 Playwright가 없어 Chrome 헤드리스 1초 간격 PNG(12장·8장, 폰 폭은 iframe으로 390px 배치). 처음 실행에서 4번 "주행 자극 보기"가 쉼표 화면 첫 화면 밖이라 못 찾음 → 한 번 끌어 올리고 다시 찾게 고쳐 `--only 4` 재촬영 통과, 7번도 폰 페이지 폭 수정 뒤 재촬영. `--only`는 index의 다른 줄을 그대로 둔다.
5. **공개·Release 문서**: `docs/release_notes_v1.0.0.md`(기능·설치(디버그 앱 먼저 제거)·서버 없이/필요·알려진 제한·검증 숫자), `docs/release.md` 6절(About 한 줄·topics·GitHub Pages로 privacy.html 공개), README 제출 준비 절에 한 줄.
6. **폰 QR 도우미** `demo_check.ps1 -DetectIp`: Wi-Fi(없으면 기본 경로 어댑터) IPv4 → `-ShareBase http://<IP>:8000`, 방화벽은 읽기만 하고 규칙이 없으면 관리자 명령(New/Remove-NetFirewallRule)만 출력. 이 PC는 Wi-Fi 어댑터가 없어 이더넷 192.168.0.x를 골랐다(폰과 같은 공유기인지 확인 필요). recording.md에 사용법.

## 잠깐 대기 — 한눈에 크게 (2026-11-03, `_private/WAIT_PROMPT.md`)
- **화면** (`ui/screens/WaitScreen.kt` 새 파일, PetScreens의 옛 WaitScreen은 옮김): 한 장 가운데 정렬 — 집·반려견 그림(글자 색으로 물들여 다크에서도 보임) → "주인이 곧 돌아와요"(기준 72 = metric의 2배, `WaitLayout`이 창 폭·높이에 맞춰 한 줄로) → 실내 온도 같은 크기 "24℃" + "실내 온도"(측정값 있을 때만, 없으면 줄 숨김) → "4분째" → "My owner will be back soon". 보호자 줄 [돌아왔어요][긴급 도움][설정] 보조 버튼(76dp, 1/3 창은 아이콘 빼고 한 줄).
- **옮긴 것(지운 것 없음)**: 제한 시간 칩·설정 시간·실내 온도·배터리 사용·배터리·보호자 폰 QR·"폰 공유 연결 안 됨" → 설정 시트. 켤 때 안내("오래 혼자 두지 마세요")·여름 안내 → 보호자 줄 한 줄. 온도 오름 안내 → 온도 아래 앰버 한 줄. **바뀐 동작 1가지**: 제한 시간을 넘기면 전에는 배경이 앰버로 바뀌고 큰 글이 "보호자에게 알렸어요"였는데, 지금은 큰 글·배경 그대로 + 보호자 줄 "설정한 10분이 지났어요"(지시대로).
- **에어컨 표시**: Pleos `HVAC.registerHvacAcState` → `SignalSample.hvacAcOn`이 코드에 있음 → 켜짐일 때만 "에어컨 켜짐" 작은 표시(온도 줄 옆). 앱은 공조를 제어하지 않는다.
- **긴급 도움 버튼**: 대기 화면에서는 지시("차분하게")대로 앰버가 아닌 보조 버튼 + 경고 아이콘(다른 정차 화면은 앰버 그대로).
- **캡처** `artifacts/ui/v8/`: 라이트 온도 있음 `10b-wait-temp-w3`, 온도 없음 `10d-wait-notemp-w3`, 넘김 `10c-wait-overdue-w2`, 시뮬레이터 실값 `10-wait-w2`, 설정 시트 `10e-wait-sheet`, 다크 `10b-wait-temp-dark2`·`10d-wait-notemp-dark2`, 1/3 `10b-wait-temp-third2`·`10c-wait-overdue-third2`. README `docs/screens/v8/06-wait.png` 교체.
- **고유 테스트 수 = 앱 331 · 서버 81** (앱: 모듈별 한 빌드 변형, 소스 `@Test` 318 + 매개변수 1개 14경우). 앞 절의 "499"는 안드로이드 모듈을 디버그·릴리스로 두 번 센 값. README 대응표 두 곳(319·308)과 qa_report를 331로 통일.
- **검증**: `./gradlew test` 종료 0(실패 0, 새 `WaitGlanceTest` 4개), pytest 81 통과, 리허설 `--all` 1회 통과(144초, 9장면 — 기본 5장면 포함, "(예비) 휴게소·잠깐 대기" 9초). 보호자 폰 페이지(server/app/pages.py)는 바꾸지 않음.

## 긴급 도움 — 결과 (2026-11-03, 아래 계획대로 완료)
- **분류**: 응급 말 48개 → EMERGENCY + 상황 4가지 **48/48 통과**(F10-01), 응급이 아닌 말 15개 그대로(F10-02, 구토 단독 = VOMIT, 반려견 이름 "초코"·"포도"는 위험 물질 아님 — "초코"는 규칙에서 뺐고 음식은 먹는 말과 함께일 때만). 기존 `PetDistressTest` 그대로 통과.
- **카드 4개** (content/care, reviewed: false): 출처는 11/03 직접 연 Merck 2쪽·RSPCA 2쪽만. AVMA는 본문을 읽지 못해 뺐다. 페이지에 없는 2가지(경련 "입에 손 넣지 않기", 초콜릿·포도·양파 이름)는 sources에 "사용자 확인 필요".
- **릴리스 미검수 차단**: `EmergencyCardUi.from` + `showDrafts = BuildConfig.DEBUG || 발표 모드 || 시뮬레이터` → 릴리스 실차는 경고 띠 + 병원 버튼만(F10-06 테스트). 릴리스 APK 실기 확인은 keystore가 없어 못 함(단위 테스트로만).
- **화면**: 정차 화면 4곳 버튼(쉼표 왼쪽 패널 아래, 대기 "돌아왔어요" 위, 출발 전 프로필 칸 아래, 리포트 앱 바 — 1/2·1/3 창은 기존 두 버튼이 밀려나서 화면 아래로). 긴급 도움 화면(전체 창 고르기 | 카드·병원, 좁은 창은 고른 뒤 카드·병원 먼저). 동물병원 화면 위계 다시 배치(가장 가까운 곳 → [경로 안내][전화] → 사전 접수 그대로 → "다른 병원 보기" → 아래 고정 문구). 앱은 전화를 걸 수 없어(`tel:` 없음) [전화] = 번호 크게 + tel: QR. 캡처 `artifacts/ui/v8/18*`·`12c`·`*-third2`·`*-dark`, README 새 그림 3장 + 05·08·08c 교체.
- **정차 중 음성** "콩이 초콜릿 먹었어" → 같은 카드(에뮬레이터 확인). 주행 중은 기존 흐름(한 줄 + 버튼 2개), 신고 기록 이름 = 상황 종류.
- **곁가지 수정**: 새 여행(PREPARE)이면 지난 병원 안내(`goingVet`)를 끝낸다 — 긴급 도움 "경로 안내" 뒤 출발하지 않은 채 다음 리허설의 고속도로 장면이 병원 카드로 바뀌던 것. 리허설·캡처 시작에 `--es emergency none`(위로 연 화면은 장면 이동으로 닫히지 않음 — 원래 동작).
- **문구 점검(5번)**: "건강해요"·"양호" 화면 문구 없음(주석·테스트의 "양호 아님"뿐). 주행 자극 설명 → "차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님"으로 통일(STIMULUS_NOTE·병원 공유·발표 비교·설정·PetTripLog, 리포트 그래프는 좁은 창에서 잘리지 않게 제목 아래 줄로).
- **리포트 vs 실시간 신고 — 보고만(고치지 않음)**: ① 같은 신고가 쉼표 화면 "신고 1건 · 헐떡임(출발 39분 뒤)"·리포트 요약 "관찰 신고 1건"·타임라인 "0:39 헐떡임 신고" 세 형식으로 보인다(제안: 시각 표기를 "출발 뒤 0:39" 하나로). ② 리포트 "병원에 공유"와 긴급 도움 "바로 동물병원 찾기"가 같은 동물병원 화면을 연다 — 리포트 쪽은 공유 미리보기가 목적이라 제목을 "병원에 공유할 내용 보기"로 바꾸면 덜 헷갈림. ③ 정차 화면에서 병원 버튼이 "동물병원"(보조)과 "긴급 도움"(앰버) 두 개 — 의도된 구분(평소/급할 때)이지만 PT에서 한 줄 설명 필요.
- **검증**: `./gradlew test` 종료 0 — testcase 499개(디버그·릴리스 단위 테스트 합, 실패 0, 새 테스트 8개: EmergencyKindTest 3·EmergencyCardTest 4·ScreenRouterTest 1). 서버 pytest 81 통과. 리허설 기본 1회 통과(68초), `--all` 통과(141초, 9장면 — 새 "(예비) 긴급 도움 → 위험 물질 → 병원" 11초). `--all` 첫 두 번은 각각 "쉼표 추천 75초 초과"(다음 실행 통과, 기존과 같은 흔들림), "고속도로"(위 goingVet — 고침) 실패.
- **사용자 할 일**: 카드 4개 검수(care_breathing·seizure·bleeding·toxin: sources 페이지 본문과 3줄 대조, "사용자 확인 필요" 2곳) → `reviewed: true`면 릴리스에도 본문.

## 긴급 도움 — 구현 계획 (2026-11-03, `_private/EMERGENCY_PROMPT.md`, 11/8 동결 전)
코드 확인 결과: 앱은 전화를 직접 걸지 못한다(`tel:`·ACTION_DIAL·CALL_PHONE 없음) → "전화"는 번호를 크게 + `tel:` QR(기존 `LinkQr`)로 폰에서 걸게 한다.
릴리스/디버그 구분은 `BuildConfig.DEBUG`(app 모듈), 발표 모드는 `AppState.demoOpen`(릴리스에도 있음).

**재사용 (새 구조 안 만듦)**
- 분류: `PetDistress.classify`·`DistressKind.EMERGENCY`·`advise`(문구 "가까운 동물병원으로 안내할게요"·VET) 그대로. 주행 중 흐름(`TripCoordinator.handleDistress` → `DriveContent.Distress` [동물병원으로] [괜찮아졌어요]) 그대로.
- 카드: `CareSheet.parse`·`CareRepository`(assets = content/care), 화면 부품 `Panel`·`AlertStrip`·`SymptomTile`·`PauseButton`·`KeyValue`·`StatusChip("검수 전")`·`LinkQr`.
- 병원: `openVets`(위치·DemoPartner)·`GoVet`(경유지, 누를 때만)·`IntakePanel`(사전 접수 그대로)·`ContactPanel`.
- 색: `warningSoft`·`warning`·`warningInk`·`primary`만 (새 색 없음).

**바꿀 파일**
| 파일 | 변경 |
| --- | --- |
| engine/pet `PetDistress.kt` | `EmergencyKind { BREATHING, SEIZURE, BLEEDING, TOXIN }` + `emergencyKind(text)`. EMERGENCY 정규식에 4가지 말 추가(기존 패턴은 그대로 앞에 둠 → 기존 분류 불변). `advise(kind, s, emergency)` = 문구·행동 같고 `care`만 하위 카드 id. `label(kind, emergency)` = "호흡 곤란"·"경련·의식 이상"·"외상·출혈"·"위험 물질 섭취" |
| core/uistate `CareSheet.kt` | 머리말 `voice`·`action`·`category`·`triggers` 읽기(기본값 있음). `EmergencyCard.from(sheet, showDrafts)` 순수 함수 = 미검수 + 릴리스(발표 모드 아님)면 본문·출처 빈 칸, 첫 줄 "바로 동물병원에 연락해요"만 |
| core/uistate `ScreenStates.kt`·`UiContract.kt`·`Fixtures.kt` | `EmergencyUi`(상황 4개·고른 것·카드·가장 가까운 병원·로딩), `Screen.Emergency`, `UiEvent.OpenEmergency`·`PickEmergency(id)`, `Scene.EMERGENCY`·`EMERGENCY_CARD` |
| app `ScreenRouter.kt` | `Overlay.EMERGENCY` → `Route.EMERGENCY`(주행이면 DRIVE가 먼저 — 주행 중 안 열림) |
| app `MainViewModel.kt` | 긴급 도움 열기·고르기(병원 목록 같이 불러옴), 정차 중 음성 응급 → 같은 카드, adb `openEmergency`, 카드 공개 여부 = `BuildConfig.DEBUG \|\| demoOpen` |
| app `TripCoordinator.kt` | `reply`에서 응급 하위 종류를 같이 넘김 → 신고 기록 `DistressRecord.label`에 상황 종류, `careId` = 하위 카드 |
| app `CareRepository.kt` | `action: VET` 미검수 카드는 릴리스 "콩이 돌보기"에서도 본문 숨김(기존 8개 카드 표시는 그대로) |
| app `Kit.kt` | `ButtonKind.WARNING`(연한 앰버 면 + 앰버 테두리 + 경고 아이콘 + warningInk, 높이 76dp 이상) — `PauseButton` 재사용 |
| app `PetScreens.kt` | 정차 화면 4곳에 "긴급 도움" 버튼: 쉼표 = 왼쪽 패널 "동물병원" 아래, 대기 = 오른쪽 패널 "돌아왔어요" 위, 출발 전 = 프로필 패널 "프로필 바꾸기" 아래. `VetsScreen` 위계 다시 배치(① 가장 가까운 병원 ② [경로 안내][전화] ③ 사전 접수 그대로 ④ "다른 병원 보기") + 고정 문구 2줄 |
| app `TripLogScreen.kt` | 앱 바 오른쪽 "긴급 도움" (1/3 창은 화면 아래) |
| app 새 `ui/screens/EmergencyScreen.kt` | "어떤 상황인가요?" + 타일 2×2(1/3 창 1열) + "바로 동물병원 찾기" / 고르면 카드 + 병원 요약 + [경로 안내][전화] |
| app `MainActivity.kt` | `Screen.Emergency` 그리기, adb `--es emergency toxin\|breathing\|seizure\|bleeding\|menu\|none` |
| content/care 새 4개 | care_breathing·care_seizure·care_bleeding·care_toxin (id·title·category: emergency·triggers·voice ≤40자·action: VET·reviewed: false·sources), 3줄, 첫 줄 "바로 동물병원에 연락해요" |
| scripts | rehearsal.py `--all`에 "(예비) 긴급 도움 → 위험 물질 섭취 → 병원"(기본 5장면 그대로), ui_capture.py 장면 추가 → artifacts/ui/v8/ |
| docs | wording.md "긴급 도움" 표(먼저), states.md, README, test_plan, docs/store/distraction_check.md·data_inventory.md, demo_script |

**테스트**
- `PetDistressTest`: 기존 테스트 그대로 + 대표 발화 40개 이상 → EMERGENCY + 하위 종류, 구토 단독 = VOMIT, 일반 말 = null.
- `CareSheetTest`: 새 카드 4개 형식(3줄·첫 줄·voice 40자·action VET·금지어), `EmergencyCard` 릴리스 미검수 차단·디버그/발표 모드 공개.
- `ScreenRouterTest`: 긴급 도움은 정차 중만, 주행이면 DRIVE.
- 끝: `./gradlew test` + 서버 pytest, 리허설 기본 1회 + `--all` 1회.

**확인할 것(사용자)**: 카드 4개 검수 → `reviewed: true`면 릴리스에도 본문이 나온다.

## Pleos App Market 제출 패키지 (2026-11-02 오후, 앱 코드·화면 변경 없음)
- 읽은 Pleos 페이지: 심사 가이드 5개 하위 페이지(콘텐츠 안전·기능 성능·주의분산·등록 정보·개인정보 보안), 제출 가이드, UI 디자인 가이드 소개, 주행 안전 설계, 지역별 주행 제한, 앱 카테고리, 태깅 가이드, Basic button(숫자는 그림 안이라 못 읽음), AAOS 주의분산 가이드. 못 찾음: IARC 페이지 링크, 한국 주행 제한 규정.
- `docs/store/`: data_inventory.md(코드 기준 표 + 불일치 12), privacy.md + `docs/privacy.html`(외부 리소스 없음), distraction_check.md, listing.md, screenshots 8장(1700×1212, 앱 영역 1:1, 테두리만 채움), icon-512.png(런처 벡터 렌더, 24KB).
- 스크린샷: `scripts/ui_capture.py`에 장면 2개(07b 응급 신고 → 동물병원, 13b 도착 뒷문) 추가. `triplog()`는 점프 뒤 15초 기다렸다가 "내렸어요"를 누른다(바로 누르면 리포트 휴식 0번, BLOCKED 11/02).
- C2PA 제거: v3·v4·v5·v5/assets 29장 + `docs/design/ui_design.png` 1장(다른 도구의 C2PA, 지난 도구 이름 검색에 안 걸렸던 것) = 30장, `caBX` 청크만 뺐다. 제거 전후 IHDR+압축 해제 IDAT SHA-256이 모두 같다. 저장소 그림 100장 재검사 → 남은 C2PA·도구 이름 0.
- 큰 발견(코드 결정 필요, BLOCKED): 주행 중 음성 신고가 마이크와 이어져 있지 않음, 반려견 이름·메모가 서버로 가고 "함께 보내기"가 자동으로 켜짐, 라이트 테마 대비 4.23·4.34.
- `./gradlew test testDebugUnitTest` exit 0, 실패 0.

## 공개 전 정리 + 리포트 간소화 (2026-11-02 오전)
- 지운 것: `docs/design/mockups/` 전체 19개(옛 「쉼표」 시안 PNG 17장 01-drive~17-icon·overview.png·mockups.html). 가리키던 `docs/design/UI_SPEC.md`(폐기 문서)는 맨 위에 "삭제함" 한 줄 + 참조 16곳을 "(삭제) mockups/…"로.
- 도구 이름 표기: docs/BLOCKED.md·docs/PROGRESS.md·todo.md → "코딩 에이전트"·"에이전트 지침 파일"·"(에이전트)", .gitignore 주석 2곳(무시 규칙은 그대로). .dockerignore는 주석에 없음(무시 규칙만).
- 다시 검색(추적·새 파일 449개, gitignore 제외, 바이너리 포함, 대소문자 무시): 텍스트는 .gitignore·.dockerignore의 무시 규칙뿐. **PNG 29장**(docs/design/v3 2·v4 2·v5 2·v5/assets 23)에 `caBX` 청크 = C2PA 콘텐츠 자격 증명(생성 도구 출처 기록)이 있다. v5/assets는 앱 assets로 APK에 들어간다. 생성형 AI 그림의 출처 기록이라 지우지 않고 보고만 함(licenses/ASSETS.md "생성형 AI 디자인 도구"와 같은 사실). JPG 14장은 JFIF만, 텍스트 메타데이터 없음. exiftool이 없어(전역 설치는 묻고) Python으로 PNG 청크·JPEG APP 세그먼트를 직접 읽음.
- 루트 문서의 「쉼표」 표현(그대로 둠, 재사용 설명·현재 용어): architecture.md 3·103·139·221행(쉼표에서 가져온 것 설명), "반려견 맞춤 쉼표"·"쉼표 화면"·"오늘의 쉼표"(현재 기능 이름), `engine:fatigue` 운전자 피로(F2 요인), api_spec.md 190행("쉼표 구분" = 쉼표 문자), 2.10 `passengerSummary.children`(서버 필드 그대로, 인원 수만)·2.11 `fatigue_peak` 이벤트·예시 "부산" 목적지. 파워냅·원정대·라디오·아직 멀었어는 루트 2개 문서에 없음(2.11에 "kid_question 삭제" 이력 한 줄).
- LICENSE 새로(All rights reserved, 공모전 심사·열람 목적만, 예외: OSM ODbL 1.0·공공데이터 3종 제공처 조건·서드파티 라이브러리 각 라이선스 → licenses/ASSETS.md·README §7). 실명 없음. README §7·ASSETS.md 맨 위에 연결.
- 리포트 간소화(화면 모양 바뀜 — 사용자 지시): 첫 화면 = 요약 한 줄 + 시간순 타임라인(한 줄 요약·"맞춤 정도"는 스크롤 밖 아래 고정). 앱 바 "주행 자극 자세히 보기"(↔ "타임라인 보기")를 누르면 주행 자극 그래프 + 병원에 보여 줄 내용(공유 버튼·지난 기록). 병원 공유 내용(`ShareReport`)은 그대로. 고갯길 비교·"주행 자극 보기"는 그대로 정차 중 버튼.
- 스크립트: rehearsal.py 도착 장면(예비, 기본 목록 밖)·ui_capture.py `03-triplog`은 "오늘의 길"을 기다리고, `03b-triplog-detail`(새)·도착 장면은 "주행 자극 자세히 보기"를 눌러 "병원에 보여 줄 내용"을 확인. 첫 리허설에서 PT3 "맞춤 정도"가 타임라인 스크롤 아래로 밀려 막힘 → 스크롤 밖으로 옮겨 해결.
- 리허설: 기본 1회 통과 70초, `--all` 1회 통과 130초. README 스크린샷 `docs/screens/v8/10-report.png`만 새로(앱 영역 1024×726). `10b-report-calibrated.png`는 옛 배치 그대로(지시대로 1장만).
- 검증: `./gradlew test` 종료 0 — 319개(앱 171 + 그 밖 148), 실패 0.

## AVD 메모리 3072MB 시도 + 리허설 3회 (2026-11-02 09시, 사용자 지시)
- `config.ini` `hw.ramSize` 4096 → 3072(백업 후) → 콜드 부팅해도 게스트 MemTotal 4GB. `-verbose`: "Increasing RAM size to 4096MB"(에뮬레이터가 이 이미지 최소값으로 올림), `-memory 3072`도 같음 → **적용 불가, config.ini 원래 값(4096)으로 되돌림**. AVD 설정 변경 없음.
- 리허설 연속 3회(문 이벤트 빌드, 재시작 없이) 통과: 77·68·66초 → [qa_report.md](qa_report.md).

## 차량 이벤트 연결 — 문 (2026-11-02 오전, 11/8 동결 전, 새 화면 없음)
- **Door API 확인 (javap Vehicle 2.3.2):** `Vehicle.getDoor(): Door`, `Door.checkDoorPositionCapabilities(Function2<CapabilityConfig, Map<DoorArea, AreaBounds<Integer>>, Unit>, Function1<Exception, Unit>)`, `getCurrentDoorPosition(DoorArea, Function1<Integer, Unit>, Function1<Exception, Unit>)`, `registerDoorPosition(DoorArea, DoorPositionListener)`·`unregisterDoorPosition`, `DoorPositionListener.onPositionUpdated(Integer)` + `VehicleListener.onFailed(Exception)`, `DoorArea(DoorValue...)`·`getValues()`, `DoorValue` ROW_1_LEFT·ROW_1_RIGHT·ROW_2_LEFT·ROW_2_RIGHT·ROW_3_LEFT·ROW_3_RIGHT·HOOD·REAR. `@RequiresPermission("pleos.car.permission.CAR_DOORS")`(읽기·등록), 쓰기는 `CONTROL_CAR_DOORS`(안 씀). 에뮬레이터: `CAR_DOORS` protectionLevel normal(설치 시 허용), 실차 모드 로그 "문 위치 지원 영역 6곳 → 쓰는 문 [ROW_1_LEFT, ROW_2_LEFT, ROW_2_RIGHT]". AAOS `DOOR_POS` 대체는 필요 없었음.
- 위치 값의 뜻은 문서·aar에 없음 → 0만 닫힘, 그 밖은 열림(`DoorState.isOpen`, 실차 확인 예정). 운전석 = ROW_1_LEFT(좌핸들 가정, 실차 확인 예정).
- 신호: `Signal.DOOR`, `SignalSample.doors: DoorState?`(운전석·뒷좌석 왼쪽·오른쪽, 문마다 nullable), `SignalMerge`, `PleosVehicleSource`(지원 영역만 등록), 시나리오 키프레임 `doorDriver`·`doorRearLeft`·`doorRearRight`, pet-trip에 DOOR + 문 6개 키프레임(휴게소 5252초 운전석, 도착 10915초 뒷문·10935초 운전석 — 모두 "내렸나요?"(10899초) 뒤라 리허설 그대로). 설정 "이 차에서 쓰는 신호"에 "문 열림 (도착·잠깐 내릴 때)" 한 줄, 발표 모드 신호 줄 "doors"(디버그 패널).
- 동작 `engine/pet/DoorWatch`(닫힘 → 열림 순간만, P단만): a) 도착 + 뒷문 → `BoardingCheck.promptNow`로 "콩이 내렸나요?" 바로(기존 화면·문장) — **완료**. b) 휴게소 + 운전석 문 → 대기 모드는 켜지 않고 "문이 열렸어요. 차에 두고 다녀오면 "잠깐 다녀올게"라고 말해 주세요." 한 줄(정차마다 한 번) — **완료**. c) 도착 + 운전석 문 + (답 없음 또는 뒷문 한 번도 안 열림) → "문이 열렸어요." + 기존 "내렸나요?" 문장 한 번(여행마다 한 번, 반복·경적·외부 전송 없음) — **완료**. c)는 도착에서만(휴게소 운전석 문은 b). 문 신호가 없으면 셋 다 꺼지고 이전처럼 P단 60초.
- 수동 재현: adb `--es door rear|driver|both|closed|none`(디버그, 시뮬레이터에서만 시나리오 값보다 먼저). 에뮬레이터에서 도착 장면 점프 뒤 `--es door rear` → 2초 안에 "콩이 내렸나요?"(기존 화면, 캡처 로컬 `artifacts/door-arrival.png`). docs/demo_script.md 한 줄.
- 화면 모양 변화: 없음(설정 신호 목록에 줄 하나, 디버그 패널 줄 하나 — 기존 목록 재사용). 새 문구는 wording.md "문 이벤트" 표.
- 문서: README(대응표 = 공모 요강 4항목 차량특화성·창의성·기술 활용성·사업성("기획서 6번 참고"), 완성도·주행 안전 내용은 기술 활용성·차량특화성 줄로 합침, **SDK 사용표**, **릴리스 APK / 서버 필요 표**, 실차 확인 예정에 문 신호, aar 확인 표에 문), states.md, wording.md, test_plan F6-06~F6-09, demo_script.
- 검증: `./gradlew test` 종료 0 — 앱 319개(앱 모듈 171 + 그 밖 148, 실패 0, 새 테스트 `DoorWatchTest` 10개·`ScenarioPlayerTest` 1개). 리허설 1회 통과(69초, 문 키프레임 넣은 pet-trip). 서버는 바꾸지 않음(pytest 81, 오늘 아침 결과).

## 시연 안정화 2차 (2026-11-02 아침, 11/8 동결 전 마지막 — 버그 수정·안정화만)
- **원인 (한 줄):** 장면 점프로 취소된 쉼표 추천이 취소를 삼키고(문장 공급자 `runCatching`) 계속 진행해 초기화 뒤 화면을 다시 쓰고, 취소된 차례의 받아쓰기가 공용 응답 칸에 늦게 값을 넣어 다음 차례의 듣기 상태를 덮을 수 있었다(원래 멈춤은 재현하지 못함 — 이 두 경로를 막음).
- 1차(아래 "시연 안정화와 제출 준비")에 더해: `ReplyGate` 감시 9초 → **8초**(어떤 경우에도 "듣는 중" 8초 이하), 테스트 F2-14에 "듣는 중 최장 시간 ≤ 8초" 검사, **F2-15 늦은 콜백 무시**(취소 뒤 5초에 온 "응"이 새 차례 상태·응답을 바꾸지 못함). `ReplyGateTest` 6개.
- `scripts/demo_check.ps1` 다시 짬: 기본은 **점검만**(줄마다 OK/WARN/FAIL — Gradle 데몬 종료, adb 기기·부팅, 앱 설치·버전, PostgreSQL 컨테이너, 서버 상태(status·db·llm만), Ollama·gemma4:12b(WARN), PC 남은 메모리), `-Prepare`(켜기)·`-Rehearsal`(리허설 1회)·`-Stop`. 비밀값은 읽지도 출력하지도 않음. 11/02 실행: 에뮬레이터 꺼짐 상태 FAIL 1(adb), `-Prepare -Install` 뒤 8줄 OK.
- 빌드 메모리 — **이전 값 기록(바꾸지 않음):** `org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1536m -Dfile.encoding=UTF-8`, `kotlin.daemon.jvmargs=-Xmx2g`, `org.gradle.parallel=true`, `org.gradle.caching=true`. 제안은 BLOCKED.md 11/02(사용자 승인 필요). AVD도 제안만.
- 리허설 연속 3회(재시작 없이) 통과: 70·76·86초 → [qa_report.md](qa_report.md) 표. 이어서 점프 20회 1회 통과(110초).
- 돌봄 문구: care_nausea 제목 "침 흘림·하품 — 속이 불편할 때" 유지, **본문 되돌림**(1차에서 바꾼 마지막 줄 → 원래 "멀미가 자주 반복되면"). sources = Merck Veterinary Manual·AVMA "Pets in vehicles"·RSPCA "Dogs die in hot cars" 중 맞는 것만, 주소는 11/02 페이지를 직접 열어 확인한 3개(Merck 2·RSPCA 1)만. AVMA는 자동으로 본문을 읽지 못해 주소 비움 + "사용자 확인 필요", 맞는 자료가 없는 3개(불안 헐떡임·응급·잘 모르겠음)는 비움 + "사용자 확인 필요". reviewed=false 유지.
- 릴리스: 서명 값을 `local.properties`(pause.keystore·pause.keyAlias·pause.keystorePassword·pause.keyPassword) 또는 환경 변수(PAUSE_*, 우선)에서 읽음. 값이 없으면 `assembleRelease`가 패키징 전에 안내 문구로 멈춤(`./gradlew test`는 영향 없음). versionName **1.0.0**, 출력 `gatigage-v1.0.0.apk`. R8은 기본 꺼짐 그대로(`-Ppause.minify=true` 옵트인, 켜져 있지 않음). 디버그 키 임시 서명으로 이름·`apksigner verify`·versionName 확인 후 지움(약 57MB). `.gitignore`에 `*.jks`·`*.keystore`·`local.properties` 있음. docs/release.md 단계 다시 씀.
- README: 8개 상태 표 새로, 실차 확인 예정 표 항목 이름 맞춤 + **1920×1080 배치** 줄 추가, 시연 방법에 발표 모드(로고 5번 탭)·분할 창(손잡이 끌기), 대응표 숫자(앱 308·서버 81, 리허설 70·76·86초), 릴리스 절. 검색: 실명·개인 이메일·키 없음, 커밋 작성자 noreply 하나, 도구 이름 표기는 위 5번 항목(공개 문서 3개 파일), 진단·측정 표현은 부정형("측정 아님")만.
- 화면 모양 변화: 없음(UI·문구 코드 변경 없음).
- 검증: `./gradlew test` 종료 0 — 앱 308개(앱 모듈 170 + 그 밖 모듈 138, 실패 0). 서버 pytest 81 통과. (문 이벤트 뒤 319개, 위 절)

## 시연 안정화와 제출 준비 (2026-11-02, 새 기능 없음)
- 버그 "듣고 있어요" 멈춤(BLOCKED 11/01): 쉼표 추천 응답을 `trip/ReplyGate.kt`로 뗌 — 음성 8초 한 번 → 무조건 버튼(다시 묻기 없앰, `RestPhrases.askAgain` 삭제), 차례 번호(점프·초기화 뒤 지난 차례는 상태를 못 바꿈), 받아쓰기 결과는 그 차례 안에서만(전에는 취소된 차례의 받아쓰기가 공용 응답 칸에 ""를 넣음), 다른 스레드 감시(9초 넘은 "듣는 중" → 버튼, 화면 반영도 엔진 스레드 밖). `PhraseProvider`가 취소를 템플릿으로 삼키던 것 고침(취소된 제안이 초기화 뒤 화면을 덮을 수 있었음). 원래 멈춤은 재현하지 못함 — 가능한 경로를 막음.
- 테스트: `ReplyGateTest` 5개(F2-13 8초 뒤 버튼·인식기 없음·버튼 15초·감시, F2-14 점프 20회 연속), `VoiceTest` 취소 1개. 앱 307개 통과(0 실패).
- 에뮬레이터 점프 20회(`python scripts/rehearsal.py --jumps 20`): 이전 빌드 1회 통과(버튼까지 45.2초). 최종 빌드 5회 중 3회 통과(45.4·45.3·45.7초). 실패 1회는 마지막 점프 직후 에뮬레이터 자체가 꺼짐(04:49, 앱 로그 정상 진행 중), 1회는 끝 화면에 기본 내비가 앞에 있었음(같이가개가 뒤로 감, 로그가 지워져 원인 미확인 — 디버그 APK를 막 다시 설치한 직후).
- R8 릴리스 빌드 첫 실행 크래시 발견·수정: `TripCoordinator` init이 속성 선언보다 앞이라 엔진 스레드가 생성자보다 먼저 `onSnapshot` → `auto` null. init을 클래스 끝으로. 서명 없는 R8 APK 11.2 MB, 디버그 키 임시 서명본으로 실행 확인.
- 프로필 칸 "거의 안 해요/가끔/자주 해요" → "덜 예민/보통/예민"(wording.md).
- 돌봄 문구: care_nausea 제목 "속이 불편할 때", 마지막 줄 "멀미" 표현 바꿈. sources 8개 = 자료 후보(Merck 2·AVMA 3·RSPCA 1, 주소는 11/02 웹 검색으로 확인, 본문 대조 전) + 찾지 못한 2개 "확인 필요". reviewed=false 유지.
- 메모리: AVD `hw.ramSize=4096`, 시연 중 게스트 실제 사용 약 2GB(MemAvailable 1.9GB), 호스트 qemu 5.3GB(작업 집합). PC 32GB 중 Chrome 약 8.5GB. `scripts/demo_check.ps1`(메모리 → DB·서버 → 에뮬레이터 → 설치 → 내비 안내 → 리허설, `-Stop` 정리), docs/recording.md·README에 연결. AVD 설정은 바꾸지 않음(사용자 결정 대기).
- 문서: README(실차 확인 예정 표 11/02, 대응표에 완성도·주행 안전 줄, 시연 방법, 데이터 출처·라이선스 절, Windows 명령의 깨진 글자 `\a` 고침), release.md(`C:\keys\` 단계), licenses/ASSETS.md(ZXing), test_plan F2-13·F2-14, BLOCKED, wording.

### PT 리허설 연속 3회 (재시작 없이, 최종 빌드, 2026-11-02 04:4x)
| 회차 | 걸린 시간(초) | 출발 체크 | 고속도로 | PT1 고갯길 비교 | PT2 병원 사전 접수 | PT3 쓸수록 맞춰짐 | 실패 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | 71 (장면 합 67) | 12 | 3 | 9 | 30 | 14 | 없음 |
| 2 | 68 (66) | 11 | 3 | 9 | 29 | 14 | 없음 |
| 3 | 67 (67) | 12 | 3 | 9 | 30 | 14 | 없음 |
(장면별 값은 회차별 `artifacts/rehearsal3/timing*.md`. 같은 조건으로 그 전 빌드에서도 3회 연속 통과: 68·66·67초.)

## 커밋 대기열 (같이가개 17차 — README 개편, 11/03) — 16차 다음에 (실제 명령은 `_private/COMMIT_COMMANDS.md` 3번)
1. `[DOC] README 심사위원 기준 개편, 기능·화면·기술 메모 문서 분리` — `README.md`, 새 `docs/features.md`·`docs/screens.md`·`docs/tech_notes.md`, `docs/PROGRESS.md`

## 커밋 대기열 (같이가개 16차 — 돌봄 카드 검수 반영, 11/03) — 사용자가 11~15차를 커밋한 뒤(`bab6b4b`까지) 남은 것 (실제 명령은 `_private/COMMIT_COMMANDS.md`)
1. `[돌봄] 카드 12장 사용자 검수 반영 (문구·출처, reviewed=true)` — `content/care/*.md`, core/uistate `Fixtures.kt`, `CareSheetTest.kt`, `EmergencyCardTest.kt`
2. `[DOC] 검수 끝 반영, 스크린샷 05 다시 찍음` — `docs/store/screenshots/05-emergency-vet.png`, `docs/store/listing.md`, `README.md`, `docs/release_notes_v1.0.0.md`, `docs/wording.md`, `docs/BLOCKED.md`, `test_plan.md`, `docs/PROGRESS.md`
- 앞 묶음은 사용자가 `a263fd6`까지 커밋함.

## 커밋 대기열 (같이가개 15차 — 긴급 도움 작은 수정, 11/03) — 14차 다음에 (실제 명령은 `_private/COMMIT_COMMANDS.md` 7·8·9·10번에 합침)
1. `[긴급] 제휴 병원 사전 접수 버튼, 번호 없으면 전화 확인 줄 숨김` — core/uistate `ScreenStates.kt`(VetUi.callNote·intakeButton), `EmergencyCardTest.kt`(F10-08), app `ui/screens/EmergencyScreen.kt`
2. `[시연] 캡처 때만 시연 데이터 표시 숨김` — app `MainViewModel.kt`·`MainActivity.kt`(디버그 adb captureClean), `scripts/ui_capture.py --clean`
3. `[DOC] 스토어 스크린샷 8장 다시 찍음, 테스트 수 332` — `docs/store/screenshots/*.png`, `docs/store/listing.md`, `test_plan.md`, `docs/qa_report.md`, `README.md`, `docs/release_notes_v1.0.0.md`, `docs/PROGRESS.md`

## 커밋 대기열 (같이가개 14차 — 제출 준비 묶음, 11/03) — 13차 다음에 (실제 명령은 `_private/COMMIT_COMMANDS.md`, 11~14차를 기능 묶음 10개로)
1. `[시연] -DetectIp, 녹화 원본 자동 촬영` — `scripts/demo_check.ps1`, `scripts/record_scenes.py`, `docs/recording.md`
2. `[DOC] 마켓 패키지 갱신(긴급 도움·대기), 릴리스 노트 초안, 공개 절차` — `docs/store`(listing·privacy·screenshots 05·06), `docs/privacy.html`, `docs/release.md`, `docs/release_notes_v1.0.0.md`, `README.md`, `docs/PROGRESS.md`

## 커밋 대기열 (같이가개 13차 — 잠깐 대기 한눈에 크게, 11/03) — 12차 다음에
1. `[대기] 잠깐 대기 화면 한 장으로 크게 (온도는 측정값만, 영어 한 줄)` — 새 `app/.../ui/screens/WaitScreen.kt`·`presentation/WaitLayout.kt`, `ui/screens/PetScreens.kt`(옛 WaitScreen 뺌), `presentation/UiMapper.kt`, `MainViewModel.kt`, core/uistate `ScreenStates.kt`·`Fixtures.kt`·`UiContract.kt`(Scene.WAIT_NOTEMP), 새 `app/src/test/.../WaitGlanceTest.kt`, `scripts/ui_capture.py`
2. `[문서] 대기 화면·고유 테스트 수 반영` — `docs/wording.md`, `docs/states.md`, `README.md`, `docs/screens/v8/06-wait.png`, `test_plan.md`, `docs/qa_report.md`, `docs/store/listing.md`·`distraction_check.md`, `docs/PROGRESS.md`

## 커밋 대기열 (같이가개 12차 — 긴급 도움, 11/03) — 11차 다음에
1. `[긴급] 응급 말 4가지 구분 (호흡·경련·출혈·위험 물질)` — engine/pet `PetDistress.kt`, 새 `EmergencyKindTest.kt`
2. `[긴급] 응급 카드 4개 초안 (reviewed=false)` — content/care `care_breathing.md`·`care_seizure.md`·`care_bleeding.md`·`care_toxin.md`, `content/care/README.md`
3. `[긴급] 응급 카드 상태·릴리스 미검수 차단` — core/uistate `CareSheet.kt`·`ScreenStates.kt`·`UiContract.kt`·`Fixtures.kt`, 새 `EmergencyCardTest.kt`, app `trip/CareRepository.kt`
4. `[긴급] 긴급 도움 화면·라우팅·정차 화면 버튼` — app `presentation/ScreenRouter.kt`, `MainViewModel.kt`, `MainActivity.kt`, `trip/TripCoordinator.kt`, `ui/components/Kit.kt`, 새 `ui/screens/EmergencyScreen.kt`, `ui/screens/PetScreens.kt`, `ui/screens/TripLogScreen.kt`, `app/src/test/.../ScreenRouterTest.kt`
5. `[긴급] 동물병원 화면 위계 (가장 가까운 곳 먼저, 전화 번호·QR)` — `ui/screens/PetScreens.kt`(VetsScreen·ContactPanel), `ui/screens/EmergencyScreen.kt`(NearestVet)
6. `[문구] 주행 자극 설명 통일 (가속·제동·회전)` — `ScreenStates.kt`, `UiMapper.kt`, `Fixtures.kt`, `PetTripLog.kt`, `CompareScreen.kt`, `DemoPanel.kt`, `PetScreens.kt`(설정), `TripLogScreen.kt`, `RestructureTest.kt`
7. `[시연] 리허설·캡처에 긴급 도움 장면` — `scripts/rehearsal.py`, `scripts/ui_capture.py`
8. `[문서] 긴급 도움 반영` — `docs/wording.md`, `docs/states.md`, `README.md`, `docs/screens/v8/05-rest.png`·`08-vets.png`·`08c-vets-call.png`·`16-emergency.png`·`16b-emergency-third.png`·`16c-vets-dark.png`, `test_plan.md`, `docs/store/distraction_check.md`·`data_inventory.md`, `docs/demo_script.md`, `docs/PROGRESS.md`
(1·3·4는 서로 의존 — 한 번에 커밋해도 됨. 11차 대기열과 같은 파일(README·PetScreens·TripCoordinator·MainViewModel·content/care·PROGRESS)이 겹치므로 11차를 먼저 커밋.)

## 커밋 대기열 (같이가개 11차 — 시연 안정화, 11/02) — 10차(ff95949) 다음에
1. `[FIX] 쉼표 추천 응답: 음성 8초 뒤 무조건 버튼, 점프 뒤 늦은 콜백 무시` — `app-android/app/src/main/kotlin/com/emop/pause/trip/ReplyGate.kt app-android/app/src/test/kotlin/com/emop/pause/trip/ReplyGateTest.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripCoordinator.kt app-android/voice app-android/engine/planner test_plan.md`
2. `[FIX] 프로필 칸 문구 덜 예민·보통·예민` — `app-android/app/src/main/kotlin/com/emop/pause/ui/screens/PetScreens.kt`
3. `[BUILD] 릴리스 1.0.0: 서명 값 local.properties·환경 변수, 없으면 안내 후 멈춤, gatigage-v1.0.0.apk` — `app-android/app/build.gradle.kts docs/release.md`
4. `[DOC] 돌봄 문구 제목·자료 후보` — `content/care docs/wording.md`
5. `[DOC] 시연 점검 스크립트, 리허설 기록, README 최종 점검` — `scripts docs README.md licenses`
(1번의 TripCoordinator에는 init 위치 변경(R8 첫 실행 크래시)도 들어 있다. 새 파일: `ReplyGate.kt`, `ReplyGateTest.kt`, `scripts/demo_check.ps1`. 5번 `docs`는 3·4번에서 뺀 나머지: BLOCKED·PROGRESS·qa_report·recording.)
6. `[FEAT] 문 신호(Pleos Door): 도착 뒷문 → 내렸나요 바로, 휴게소 운전석 문 → 대기 모드 제안, 도착 후 안전 안내 한 번` — `app-android/core/model app-android/core/vehicle app-android/engine/pet app-android/sim app-android/app/src/main/kotlin/com/emop/pause/trip/TripCoordinator.kt app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/ui/screens/DemoPanel.kt scenarios/pet-trip.json scripts/gen_demo_data.py`
7. `[DOC] SDK 사용표, 릴리스·서버 기능 표, 대응표 심사 항목 4개` — `README.md docs/states.md docs/wording.md docs/demo_script.md test_plan.md docs/PROGRESS.md`
(6번의 TripCoordinator는 1번과 같은 파일 — 1번을 먼저 커밋하려면 `git add -p`로 나누거나 1·6번을 한 커밋으로. 새 파일: `engine/pet/.../DoorWatch.kt`, `DoorWatchTest.kt`. 5번 `docs`에서 7번 문서는 빼고 커밋.)
8. `[CHORE] 옛 쉼표 시안 삭제, 문서의 도구 이름 표기 정리` — `docs/design/mockups docs/design/UI_SPEC.md docs/BLOCKED.md todo.md .gitignore` (삭제는 `git add -A docs/design/mockups`)
9. `[DOC] LICENSE (공모전 심사·열람 목적, 데이터·라이브러리 예외)` — `LICENSE README.md licenses/ASSETS.md`
10. `[UI] 도착 리포트 첫 화면을 요약·타임라인으로, 그래프는 자세히 보기` — `app-android/app/src/main/kotlin/com/emop/pause/ui/screens/TripLogScreen.kt scripts/rehearsal.py scripts/ui_capture.py docs/screens/v8/10-report.png docs/states.md docs/PROGRESS.md`
(README·PROGRESS·states는 7·9·10번에 걸쳐 있다 — 7~10번을 한 커밋으로 묶어도 된다.)
11. `[DOC] Pleos App Market 제출 패키지: 데이터 처리 표·개인정보 처리방침·주의분산 점검표·등록 문서·스크린샷 8장·아이콘` — `docs/store docs/privacy.html README.md docs/BLOCKED.md docs/PROGRESS.md scripts/ui_capture.py`
12. `[CHORE] 디자인 PNG 30장 C2PA 메타데이터 청크 제거(픽셀 그대로)` — `docs/design/v3 docs/design/v4 docs/design/v5 docs/design/ui_design.png`
(11번의 README·PROGRESS·BLOCKED는 7~10번과 같은 파일 — 7~11번을 한 커밋으로 묶어도 된다. 앱 코드는 바꾸지 않았다.)

## 마지막 UI 수정 (2026-11-01, 사용자 결정 2개 + 캡처 검수 6건, 11/8 UI 동결)
- 결정 1 개인 보정 안 ①: 엔진이 두 값을 낸다 — `MotionState.index`(보정 전, 화면·기록·비교) / `personal`(개인 보정 × "이상해요" 가중, PetRestPlanner·PetDistress). 예보는 `trajectory`(보정 전) + `personalPredicted`·`personalPeak`(안내 문구·예보 단계). `MotionConstants.sensitivity`에서 보정 계수를 빼고 `personalScale`로. DemoNumbersTest DM-08(계수 상한으로 돌려도 분마다 같은 숫자). PT3 리포트 보정 뒤에도 최고 55.
- 결정 2: 점프 전 실시간 신고(`DistressRecord.live`·`simSec`)를 `LiveReports.carry`로 골라 따라잡기 중 그 시각에 다시 넣는다. RestructureTest F7-10, 리허설 PT3가 "구토 신고"를 확인.
- 주행: 회전 안내가 없으면 카드 대신 "강릉 · 184km · 2시간 3분 · 도착 오전 11:33" 한 줄(`ArriveLine`, `DriveUi.destination`). 시뮬레이터 모드도 기본 내비 TBT를 받는다(`SignalRuntime.simGuide`) — 캡처는 기본 내비 안내를 실제로 걸고 찍음(방법 docs/recording.md). 1/3 창 쉼표 추천은 큰 수치 두 칸 → "32분 뒤 · 48km" 한 줄(잘림 해결).
- 리포트: 한 줄 문장 시각 = 타임라인 첫 흔들림 구간(`UiMapper.firstStimulusMin`, 10/31 캡처의 2시간 14분/35분 → 2시간 34분 하나). 병원 공유·오늘 요약·기록 요약 "주행 X(휴식 제외)"(앱·서버 템플릿).
- 동물병원: `KeyValue` 긴 값은 이름 아래 줄로(줄임표 없음). 링크 QR: 대기(보호자 폰)·도착 예정 링크·병원 접수함·보호자 링크(`ui/components/Qr.kt`, zxing-core 3.5.3, 서버 주소 앞부분이 없으면 글자만).
- 출발 전: 어절 줄바꿈("급속충전기"), 체크 칸 줄을 높이에 고르게 나눔.
- 버그: 0초 점프 뒤 출발 전 화면이 기본 프로필·쉼표 0번으로 남음(경쟁 조건) → 빈 따라잡기 뒤 지금 프레임을 다시 읽음.
- 검증: `./gradlew test` 종료 0, pytest 81 통과, PT 리허설 5장면 OK 68초(`--phone`). 캡처 `artifacts/ui/v8/` 시트 5장 새로, README 스크린샷 `docs/screens/v8/`(18장). docs/demo_numbers.md "2026-11-01 최종" 표.

## 서비스 구조 재정비 8단계 (2026-10-31 저녁~, `_private/RESTRUCTURE_PROMPT.md`, 사용자 결정 1~6)
기준 문서: [docs/states.md](states.md) (8개 상태·전환·데이터 출처). 결정: 위치 기록 없음(거리만), 시연 접수함은 가상 "시연용 제휴 동물병원"만, 개인 보정 = 신고 자동 + 높음 구간 뒤 30분 조용하면 약하게(0.4), 주행 자극 단계 낮음/보통/높음(40·70), 온보딩 "흔들림에 예민한 정도(보호자 응답)", 분할 창은 주행 중만(정차 화면은 전체 창 기준, 좁은 창에서도 버튼 글자 줄임표 금지).

- **2단계 상태·계약 정리 (완료):**
  - `ServiceState`(8개)와 `ScreenRouter.serviceState`, `Route.STIMULUS`/`Overlay.STIMULUS`("주행 자극 보기"), `AppUiState.state`·`demoData`.
  - 화면 계약: `DriveUi`의 멀미 숫자·도로 곡률·최근 그래프를 빼고 `guide`·`remain`·`arrive`·`observation`·`nextStopFacilities`·`voiceHint`를 넣음. `Forecast`는 제목+운전 안내 한 줄. `StimulusUi` 새로. `TripLogUi`는 타임라인 리포트로 바꿈. `ToggleSymptom`·`AnswerMotion`·`MotionAnswerUi`·`PetStatusUi` 삭제.
  - 기록: `DistressRecord`(신고·주행 분·시각·속도·주행 자극), `TripRecorder.segments`(보통 이상 구간)·`elapsedMin`. 개인 보정 자동 `AutoCalibration`(+ `MotionCalibration.QUIET_WEIGHT` 0.4, `CalibrationSample.weight`).
  - 문구: 예보·쉼표 이유·한계 문장·병원 요약·온보딩·프로필을 "주행 자극"/"관찰 신고"/"흔들림에 예민한 정도(보호자 응답)"로. "멀미 초기일 수 있어요" → "속이 불편할 수 있어요".
  - 지운 것: 관찰 체크 칸, "오늘 멀미했어요?"(화면·음성·`MotionAnswerParser`), v7 상태 카드. 테스트 `RestructureTest`(10) 새로, `TripLogStatusTest`·`MotionAnswerParserTest` 삭제.
- **3단계 NaviHelper·분할 창 (완료):**
  - `NextGuide`(TBT 첫 항목: 아이콘·거리·방면 `description`) → `NaviSource.guide()` → `Snapshot.guide`. 안내 아이콘은 확인된 `TurnType` 상수 + 굽이가 아닌 안내는 상수 이름으로(TOLL·SERVICE/REST·ARRIVE).
  - 시뮬레이터는 GPX에서 만든 안내를 "시연"으로 표시만(`TripBrain.demoTurn`).
  - 분할 창: 런처 오른쪽 손잡이 드래그(`input swipe 2547 720 1700 720`)로 같이가개(853~1694) + 기본 내비(1718~2559). 분할 중 `onTBTInfo` 32건·`onDestinationInfo` 수신 확인 → BLOCKED 10/30 "앱이 뒤면 콜백 없음"은 분할 창에서 해결. 캡처 `ui_capture.py --nav`.
  - 결정: 지도·길 안내는 Pleos 기본 내비, 외부 내비(카카오 등)는 쓰지 않는다.
- **4단계 일반 주행 (완료):** `DriveScreens.kt` 다시 씀 — 도로 그림·멀미 숫자·게이지·그래프 삭제. 1/3 창: 예보 띠 → 다음 회전 카드(아이콘·거리·방면, 도착·남은 거리) → 지금 할 일(다음 쉼표/추천/이상해요/병원) → 관찰 상태 → 음성 힌트. 1/2·전체 창은 왼쪽(길 안내·관찰) | 오른쪽(지금 할 일). 버튼은 줄임표 없이 2줄까지(`PauseButton`). 앱 바 "시연 데이터".
  - 캡처: `artifacts/ui/v8/sheet-drive-nav.png` (01 일반·02 예보·07 이상해요·08 병원 = 실제 분할 창, 06 추천 = 1/3 창 `--split third` — 답을 기다리는 제안이 분할하는 10초 사이 끝나서).
- 검증(4단계 시점): `./gradlew test` 종료 0, 고유 295개 실패 0.
- **5단계 병원 이동 (완료):** 시연용 제휴 동물병원(`trip/DemoPartner`, 가상 병원·시뮬레이터에서만 목록 맨 앞, `Vet.partner`). 사전 접수(병원 접수함)는 제휴 병원만 — 실제 병원(공공 인허가)은 `ContactPanel`: "사전 접수 연동이 없어요" + 전화번호 + 도착 예정 링크(접수 아님, `intake=false`). 전달 동의: 주행 중 "콩이가 이상해요 → 동물병원으로" 뒤 음성으로 "…보낼까요? 네/아니요"(`DemoPartner.consent`), 정차 중 동물병원 화면은 "병원에 보낼 내용" 미리보기(`UiMapper.vetPreview`, "보내지 않음: 위치·경로·사진") + "동의하고 보내기". 주행 중 병원 패널 한 줄: 제휴 "정차하면 보낼 내용을 확인해요"/보낸 뒤 병원 응답, 비제휴 "정차 후 전화 …". 캡처 `artifacts/ui/v8/sheet-vet.png`.
- **6단계 정차 화면 (완료):** 출발 전·쉼표·대기는 전체 창이 기준(3칸·2칸), 1/2·1/3 창은 같은 순서로 쌓고 스크롤. 버튼·병원 이름·값(`KeyValue`)은 줄임표 대신 2줄, 앱 바 오른쪽 글은 남는 자리만(버튼을 밀지 않음, 어절 줄바꿈). 목적지가 비면 "까지 0분" 대신 "기본 내비에서 목적지를 정해 주세요". 프로필·온보딩 "흔들림에 예민한 정도 (보호자 응답)", 설정 "주행 자극은 차량 흔들림 기준이며 반려견 측정·진단이 아니에요". 쉼표 화면 "주행 자극 보기"(정차 중만). 캡처 `sheet-stopped.png`.
- **7단계 도착 후 리포트 (완료):** `TripLogScreen.kt` 타임라인 리포트 — 요약(총 이동 시간·거리·휴식·관찰 신고) → 시간순 타임라인(출발·신고·휴식·흔들림 많은 구간·병원·도착, 아이콘·출발 뒤 시각·한 줄) | 주행 자극 그래프(휴식·신고·흔들림 구간 표시, "차량 흔들림 기준 · 반려견 측정 아님") · 병원에 보여 줄 내용 + "병원에 공유". 한 시계(출발 뒤, 정차 포함)로 통일. 경로선 없음(위치 기록 안 함). 캡처 `sheet-report.png`(전체·1/2·1/3·다크).
  - 버그: 발표 점프 따라잡기 중 신고가 개인 보정 표본으로 쌓여 실행마다 민감도가 올라가 숫자가 바뀜(55 → 72) → 따라잡기 중에는 표본을 내보내지 않음. adb `--es calib reset`은 모든 반려견 기록을 지움(출발 전 이름이 아직 기본값이라 다른 키를 지우던 문제).

- **8단계 전체 연결·검증 (완료):**
  - 앱 `./gradlew test` 종료 0 — 고유 298개 실패 0 (새 `RestructureTest` 12개, `ScreenRouterTest` ST-02). 서버 pytest 81 통과.
  - PT 리허설 `scripts/rehearsal.py` 5장면 OK 66초 — PT2는 음성 동의 "네"를 넣고(`TripCoordinator.ask`가 마이크와 화면·adb 응답 중 먼저 온 것), PT3는 음성 신고 "콩이 토했어" → 리포트 "맞춤 정도: 기록 4회".
  - 서버 문구: 기록 요약 프롬프트·템플릿·쉼표 이유를 주행 자극으로, LLM 요약 금지어에 "멀미".
  - 문서: [states.md](states.md)(8개 상태·전환·데이터 출처), [wording.md](wording.md)(옛 표현 → 새 표현), test_plan(F1-11·F5-12·F7-05b·F7-06·F7-08·F7-09·ST-01~03), ui_contract(v8 계약), STYLE_V6(v8 화면), BLOCKED(분할 창 해결, 보정이 주행 자극 숫자를 바꾸는 문제·점프 뒤 실시간 신고·1920), demo_numbers, 에이전트 지침 파일 결정. README·architecture·flowchart·demo_script·motion_model 등은 문구표대로 정리.
  - 캡처 `artifacts/ui/v8/`: `sheet-drive-nav.png`(주행, 분할 창), `sheet-vet.png`, `sheet-stopped.png`, `sheet-report.png`, `sheet-rehearsal.png`.

## 커밋 대기열 (같이가개 10차 — 처리됨: 사용자가 `ff95949`로 커밋)
1. `[RS] 개인 보정은 예보·쉼표에만, 점프 뒤 실시간 신고 유지` — `app-android/engine app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/app/src/main/kotlin/com/emop/pause/runtime app-android/core/uistate app-android/app/src/test`
2. `[UI] 길 안내 한 줄·쉼표 추천 잘림·링크 QR·출발 전 줄바꿈` — `app-android/app/src/main/kotlin/com/emop/pause/ui app-android/app/build.gradle.kts app-android/gradle/libs.versions.toml`
3. `[RS] 서버 요약 "(휴식 제외)"` — `server/app/llm/phrases.py server/tests`
4. `[DOC] 발표 숫자 최종, README v8 스크린샷, 녹화 방법` — `README.md docs test_plan.md scripts`
(1·2번은 화면 계약(`ScreenStates.kt`)을 함께 바꾸므로 `git add -A` 한 번으로 묶어도 된다. 새 파일: `ui/components/Qr.kt`, `docs/screens/v8/`. 에이전트 지침 파일는 gitignore.)

## 커밋 대기열 (같이가개 9차 — 처리됨: 사용자가 `a013157`로 커밋)
1. `[RS] 주행 자극·관찰 상태 분리, 8개 상태, 개인 보정 자동` — `app-android/core app-android/engine app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/src/main/kotlin/com/emop/pause/runtime app-android/data app-android/app/src/test`
2. `[RS] 주행 화면(분할 창)·병원 이동·정차 화면·도착 후 리포트` — `app-android/app/src/main/kotlin/com/emop/pause/ui`
3. `[RS] 서버 문구` — `server/app/llm/phrases.py server/tests`
4. `[DOC] 상태·문구표·스타일·테스트 계획, 리허설·캡처 스크립트` — `docs README.md architecture.md flowchart.md api_spec.md todo.md test_plan.md scripts`
(1·2번은 같은 파일(화면 계약)을 함께 바꾸므로 `git add -A` 한 번으로 묶는 편이 안전하다. 지운 파일: `MotionAnswerParser.kt`, `MotionAnswerParserTest.kt`, `TripLogStatusTest.kt`.)

## 이동 기록 v7 재설계 (2026-10-31 오후, 사용자 승인 계획 + 결정 1~6)
- 화면: `ui/screens/TripLogScreen.kt`(새 파일, `PetScreens.kt`에서 분리). 오늘 상태 카드 → 통계 3칸 → 멀미 지수 추이(가장 넓게) → 관찰 기록 | 병원 공유 요약. 기존 기능(멀미 주의 횟수·날짜·기록 확인·질문·맞춤 정도·이전 기록·요약 문장) 모두 유지, 이벤트·저장 구조 그대로.
- 상태 판정 `UiMapper.petStatus`(결정 1, 규칙 줄마다 테스트), 근거 줄에 멀미 주의 횟수(결정 2), "괜찮았어요" 배타(결정 3), 빨강 = 관찰 필요만(결정 4, STYLE_V6 §2.1, `attention*` 토큰), 표기 "도착 시점 지수"·"오늘 콩이 상태"(결정 6).
- 그래프: 휴식 구간·증상 기록·도착 시점 점, 기록 없으면 0으로 채우지 않음. 부품 `TrendLegend`·`StatCard`·`SymptomTile`.
- 창 폭 단계를 배율 2 기준 dp로 잼(`Page`) — 1920 화면에서도 같은 비율 창이 같은 단계.
- 1920×1080(결정 5): `wm size`로 캡처하고 매번 `wm size reset`(마지막 확인 2560x1440). 이 AVD 런처는 왼쪽 차량 영역·조작 막대를 그대로 두어 앱 영역이 1067×850이 된다 → 이동 기록은 쌓기+스크롤(상태·통계가 첫 화면, 그래프부터 스크롤).
- 검수 4회(잘림·그래프 높이·줄바꿈·질문 버튼), 캡처 `artifacts/ui/v7/`(전후 2560·1920·1/2·다크, 구토·괜찮았어요 고른 상태).
- 검증: `./gradlew test` 종료 0(고유 297개, 실패 0, 새 `TripLogStatusTest` 12개), PT 리허설 5장면 OK 62초.

## 커밋 대기열 (같이가개 8차 — 처리됨: `e674119`)
1. `[UI] 이동 기록 v7: 오늘 상태·통계·넓은 그래프·관찰·병원 요약` — `app-android/core/uistate app-android/app/src/main/kotlin/com/emop/pause/presentation/UiMapper.kt app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripRecorder.kt app-android/app/src/main/kotlin/com/emop/pause/ui app-android/app/src/test/kotlin/com/emop/pause/presentation/TripLogStatusTest.kt`
2. `[DOC] v7 스타일·화면 계약·테스트 계획, 캡처 스크립트` — `docs/design/v6/STYLE_V6.md docs/ui_contract.md test_plan.md docs/demo_numbers.md docs/PROGRESS.md scripts/ui_capture.py`

## 숫자 맞추기·분할 창·다크 (2026-10-31, 사용자 결정 1~3 + 캡처 검수)
- 결정 1: 장면 점프로 건너뛴 구간을 엔진이 1초씩 따라잡은 값(`TripCoordinator.replayed`)으로 이동 기록을 다시 쌓는다. 지나간 발표 장면의 증상(DemoScenes `say`)도 그 시각에 기록하고, 정차 이름은 휴게소 목록이 오면 채운다. 기록기는 5분 점을 그 안의 최고값으로 찍고, 멀미 주의는 1초 값과 히스테리시스로 센다(`TripRecorder.cautionCount`). 한 줄 문장은 `PetTripLog.insight`로 이번 여행 값을 먼저 본다.
- 결정 2: 병원 접수함에 새 접수·변경 강조, 사용자가 켜는 알림 소리, 시연용 표시를 넣었다(`render_clinic` + `CLINIC_JS`, F5-11).
- 결정 3: 실차 문자열 길이는 대체 문구 유지, BLOCKED에 "실차 확인 예정".
- 주행 기본: 시나리오 고속도로에 교통 흐름(`gen_demo_data.py --scenario-only`), 장면 210 → 1800초, 상태 패널 빈자리에 최근 30분 그래프 + 다음 굽은 길(`MotionComfortEngine.nextCurveInM`).
- 멀미 예보: "안내 없이" 순항 속도 = 굽은 길 곡률 중앙값 기준, 감속·가속 한계(motion_model §6). 엔진 1분 간격 21점 예측(`MotionForecast.trajectory` → `Forecast.series`)만 그래프로 잇는다. 8748초 예보 57 ↔ 실제 55 ↔ 비교 55.
- 이동 기록: 세 칸(그래프 · 관찰 · 병원 요약) 한 화면, "기록 확인"은 앱 바, 시간 축 눈금은 폭에 맞춰 30분/1시간/2시간.
- 분할 창 1/2·1/3, 다크 캡처·수정(`ui_capture.py --split/--dark`). 점프 따라잡기 10.6 → 3.8초(silent에서 경로 키 생략).
- 버그: 분할 창 전환(런처 relaunch)·다크 전환으로 화면이 다시 만들어질 때 첫 실행 adb 명령(seek 등)을 또 적용해 "콩이 내렸나요?"가 다시 떴다 → `MainActivity`는 처음 만들 때만 적용.
- 문서: docs/demo_numbers.md(장면·화면·엔진·대본 숫자), demo_script·motion_model·BLOCKED·test_plan(DM-06·DM-07·F5-11)·STYLE_V6.
- 검증: `./gradlew test` 종료 0 (실패 0), pytest 통과, PT 리허설 5장면 OK 61초.

## 커밋 대기열 (같이가개 7차 — 처리됨: `cceafe4`)
1. `[UP] 예보 순항 속도·예측 곡선, 다음 굽은 길` — `app-android/engine/motion scenarios/pet-trip.json scripts/gen_demo_data.py docs/plan/motion_model.md`
2. `[UP] 장면 점프 기록 채우기, 이동 기록 숫자 일치` — `app-android/app/src/main/kotlin/com/emop/pause/trip app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/src/main/kotlin/com/emop/pause/presentation app-android/engine/pet app-android/core/uistate app-android/app/src/test test_plan.md`
3. `[UP] 병원 접수함 알림·시연용 표시` — `server/app/api/eta.py server/app/pages.py server/tests/test_clinic.py`
4. `[UI] 주행 빈자리·예보 그래프·이동 기록 세 칸, 분할 창·다크` — `app-android/app/src/main/kotlin/com/emop/pause/ui docs/design/v6/STYLE_V6.md scripts/ui_capture.py`
5. `[DOC] 발표 숫자 표, 리허설` — `docs/demo_numbers.md docs/demo_script.md scripts/rehearsal.py docs/BLOCKED.md docs/PROGRESS.md`
(2번과 4번이 같은 파일(TripCoordinator·ScreenStates 등)을 건드리므로 `git add -A` 한 번으로 묶는 편이 안전하다.)

## UI 전면 재설계 v6 (2026-10-30 낮, 사용자 "전체 UI 전면 재설계" 지시)
- 기준 문서 `docs/design/v6/STYLE_V6.md` (STYLE_V5는 "대체됨"). 색·글자는 지시 값(#F5F6F8·#3478E5·#E99532·#16866C·네이비 #1C3150, 수치 36·제목 24·섹션 18·본문 16·보조 14·캡션 12)이 v5보다 우선. 앰버 글자는 대비 때문에 #9A5A10.
- 토큰 `ui/theme/{Color,Type,Dimens}.kt`(모서리 16→14, 칩 8, 앱 바 48) → 부품 `Kit.kt` 새로(Panel·Divider·SectionHeader·Metric·KeyValue·IndexGauge·AlertStrip·QUIET 버튼, ForecastChart·TrendChart는 축·눈금·주의 이상 면·굽은 길 시간대·최고 값 표시) → 화면 `ScreenFrame.kt`·`DriveScreens.kt`·`PetScreens.kt`로 나눠 전부 다시 그림. 경로 일러스트는 무채색 바탕·비율 고정·굽은 길 앰버 구간(지도 타일 없음).
- 주행: 모든 상태가 "경로 보기 + 상태 패널" 한 뼈대. 예보는 경고 띠(20/16) + 지금·20분 뒤 수치 + 엔진 값 그래프(지어낸 곡선 제거). 이동 기록은 요약 수치·인사이트 한 줄 + 그래프 | 관찰·병원 요약이 한 화면.
- 계약은 더하기만: `Forecast.startMin·durationMin·startM`, `DriveUi.nextStopName·nextStopMin` (기존 테스트 그대로 통과).
- 보호자·병원 페이지(`pages.py`)도 v6 색, 표 이름 줄바꿈 고침.
- 검수: `scripts/ui_capture.py`(16개 화면, 실제 엔진 값) — 주행·예보·이동 기록은 에뮬레이터 캡처로 3~4회 반복(`artifacts/ui/v6/*-it1~it4.png`), 전후 나란히 `artifacts/ui/v6/<화면>-before-after.png`. README 화면 표를 v6로(`docs/screens/v6/`).
- 검증: `./gradlew test` 종료 0 — 고유 282개 실패 0, pytest 79 통과, PT 리허설 5장면 OK(74초), ERD Mermaid 렌더 OK.

## UPGRADE 변경분 (2026-10-30 낮, 사용자 메시지 — 3번·8번 교체)
- 참고: `_private/UPGRADE_PROMPT.md` 파일은 08:34 그대로라(3번이 아직 "증상 음성 메모") **사용자 메시지의 새 3번·8번을 기준**으로 했다.
- 새 3 "동물병원 사전 접수 + 실시간 도착 알림": 서버 `POST /eta` `intake=true` → 병원 접수함 `/clinic/{token}`(보호자 링크와 다른 토큰, 해시만), `PUT /eta/{token}` 도착 예정 갱신·도착, `GET /eta/{token}` 병원 응답·열람, 접수함 폼 `POST /clinic/{token}/reply?r=received|ready|busy`(303). 마이그레이션 0005(위치 컬럼 없음, 2시간 만료 그대로). `test_clinic.py` 5개(F5-07~10). 앱 `ClinicIntake`(갱신 주기·시뮬레이터 카운트다운·상태 문구, `ClinicIntakeTest`) → MainViewModel `tickIntake`(1분 이상 바뀌면 15초 간격·같으면 60초, 응답은 15초마다), 병원 응답이 오면 화면 한 줄 + 같은 문장 음성(`TripCoordinator.announce`). 주행 화면 병원 카드 아래 "병원: 도착하면 바로 봐요"(24자, DrivingRules 검사), 동물병원 화면 "사전 접수 보내기" + 접수함 링크·상태. 증상 메모·오늘 이동 한 줄은 기존 `VetMemo.share` 재사용.
- 7번 연결: 응급(축 처짐·호흡 곤란) "동물병원으로" → 병원 경유지 + 증상 자동 첨부 + **사전 접수**(F9-08).
- 새 8: demo_script를 PT 3장면(고갯길 비교 · 이상 → 사전 접수 → 접수함 실시간 도착 · 쓸수록 맞춰짐)으로 다시 짬, 예비 장면 표. `rehearsal.py` PT 장면(병원 역할로 접수함 열고 "바로 볼게요" 누름, Chrome 헤드리스 캡처), 디버그 adb `--es calib yes,yes,little|reset`(보정 시드). 이동 기록에 "맞춤 정도" 줄. **리허설 5장면 모두 OK, 62초**.
- 5번 한계: shape_compare.md 맨 앞 "한계" 절 — 회전 안내만이면 180초 늦음, 고갯길 뒤 지수 77(GPX 54), 낙관적 가정.

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

## 커밋 대기열 (같이가개 6차 — 사전 접수 + UI v6, 10/30 낮) — 5차 다음에
1. `[UP] 동물병원 사전 접수와 실시간 도착 알림 (서버)` — `server/app/api/eta.py server/app/models.py server/app/pages.py server/alembic/versions/0005_clinic_intake.py server/tests/test_clinic.py api_spec.md database.md`
2. `[UP] 사전 접수 앱 연결, 응급 → 사전 접수` — `app-android/app/src/main/kotlin/com/emop/pause/trip/ClinicIntake.kt app-android/app/src/test/kotlin/com/emop/pause/trip/ClinicIntakeTest.kt app-android/data/src/main/kotlin/com/emop/pause/data/net/PauseApi.kt app-android/app/src/main/kotlin/com/emop/pause/di/DataModule.kt app-android/app/src/main/kotlin/com/emop/pause/MainViewModel.kt app-android/app/src/main/kotlin/com/emop/pause/MainActivity.kt app-android/app/src/main/kotlin/com/emop/pause/trip/TripCoordinator.kt app-android/core/uistate test_plan.md`
3. `[UP] PT 3장면 대본·리허설, 회전 안내 한계` — `docs/demo_script.md scripts/rehearsal.py docs/plan/shape_compare.md README.md`
4. `[UI] 디자인 v6 토큰·부품·전 화면` — `app-android/app/src/main/kotlin/com/emop/pause/ui app-android/app/src/main/kotlin/com/emop/pause/presentation/UiMapper.kt docs/design/v6 docs/design/v5/STYLE_V5.md docs/ui_contract.md`
5. `[UI] 화면 전후 캡처 도구, README 화면 v6` — `scripts/ui_capture.py docs/screens/v6 README.md docs/PROGRESS.md docs/BLOCKED.md`
(2번과 4번이 같은 파일(MainViewModel·ScreenStates·UiMapper)을 고치므로 `git add -A` 한 번이 안전하다.)

## V5 후속 (2026-10-30 저녁) — 사용자 결정 5개 + 분할 창·리허설·README·녹화
- 결정 1: 디자이너 그림 = 사용자가 생성형 AI 디자인 도구로 직접 제작 → `licenses/ASSETS.md`(도구 이름은 적지 않음, 11/03 정리), 디버그 전용 해제(main assets, 시트·안 쓰는 원본은 APK 제외).
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
- 결정: `/llm/kid-answer`는 LLM을 부르지 않는다 — 에이전트 지침 파일 규칙 4가 api_spec 예시(source=ollama)보다 우선. api_spec §2.9 갱신, §2.14·공통(요청 ID·레이트 리밋) 추가.
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
