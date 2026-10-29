# 테스크 체크리스트 — 쉼표 (멈춤의 모든 순간)

**목표:** 11/14(토) 접수양식 + GitHub(소스·APK) + 시연 영상 제출 → 11/23 1차 결과 → 12/3~4 PT에서 실제 동작 시연.
담당 표기: **(나)** 직접 해야 하는 것 · **(CC)** Claude Code로 진행 · **(나+CC)** 함께.

## 일정 한눈에

| 마일스톤 | 기간 | 완료 기준 |
| --- | --- | --- |
| M0 환경 준비 | 10/27 ~ 10/29 | 에뮬레이터에서 빈 앱 실행, 서버 `/health` 200, Ollama 응답 |
| M1 뼈대 + 시뮬레이터 | 10/30 ~ 11/2 | 시나리오 재생으로 신호가 화면에 흐름 |
| M2 핵심 엔진 + 추천 | 11/3 ~ 11/7 | 휴게소 추천이 음성으로 나오고 경유지 추가까지 동작 |
| M3 시그니처 기능 | 11/8 ~ 11/11 | "아직 멀었어?", 도착 맞춤 라디오, 멈춤 큐레이터 동작 |
| M4 다듬기 + 제출 | 11/12 ~ 11/14 | 영상·저장소·APK·접수양식 제출 |
| M5 본선 고도화 | 11/15 ~ 12/2 | 확장 기능, PT 덱, 리허설 |

## M0 · 환경 준비 (10/27 ~ 10/29)
- [ ] (나) Pleos Playground 가입, 프로젝트 생성 (카테고리 결정), SDK 권한 상태 확인 · STT/TTS 신청 가능하면 신청
- [ ] (나) Android Studio + Pleos Connect Emulator 설치, AVD 설정, 공식 튜토리얼(안전 운전 점수 계산기) 빌드
- [ ] (나) 공공데이터포털 가입 → 한국도로공사 휴게소 기준정보·편의시설 API 활용 신청
- [x] (나) Ollama 모델 확정: `OLLAMA_MODEL=gemma4:12b` (`/api/chat`, `think=false`, `keep_alive=30m`). 실측: 모델 상주 상태 2.58초, 100% GPU → `num_predict=60`, `temperature=0.3`
- [x] (나) Android SDK Platform 35 설치 (compileSdk 35)
- [x] (나) GitHub에 **실명이 드러나지 않는 계정**으로 저장소 생성, `git config user.name`도 익명 닉네임
- [x] (CC) 저장소 구조 생성, `.gitignore`, `docker-compose.yml`, `server/.env.example`
- [x] (CC) FastAPI 골격 + `/health`, PostgreSQL 연결, Alembic 초기 마이그레이션(database.md DDL)
- [x] (CC) Android 멀티모듈 골격 (app, core:*, engine:*, voice, data, sim), Hilt, 디자인 토큰 테마 (compileSdk 35 빌드, 에뮬레이터 3화면 확인 10/28)
- [ ] (CC) GitHub Actions: 서버 pytest + APK 디버그 빌드 — 첫 실행(10/28) server 성공, android는 `gradlew` 실행 권한 누락으로 실패 → 수정(ef8cffc). BLOCKED: 다음 push 후 결과 확인

## M1 · 뼈대 + 시뮬레이터 (10/30 ~ 11/2)
- [x] (CC) `VehicleSource` / `NaviSource` 인터페이스 + `Sim*` 구현
- [x] (CC) `ScenarioPlayer`: JSON 타임라인 배속 재생 (1×·10×·100×)
- [x] (CC) 시나리오 3종 작성: `seoul-busan-family`, `night-solo`, `no-signal-car`
- [x] (나+CC) `PleosVehicleSource`: Vehicle SDK 연동 + capability 확인, 에뮬레이터 VHAL 값으로 검증 (기어·SoC·내기순환 수신. 속도·충전 여부는 SDK에 없음 → 결정 대기)
- [ ] (나+CC) `PleosNaviSource`: NaviHelper 경로·ETA·경유지 추가 검증 — 코드 완료, 연결 확인. BLOCKED: 에뮬레이터 내비에 목적지를 설정해야 ETA·경유지 추가를 확인할 수 있음 (사용자 조작)
- [x] (나+CC) NaviHelper 3.0.5 aar로 호환성 변경 실제 확인: `onNavigationStatus` 인자 타입, 좌표 인자 순서(위도·경도), `totalDistance/totalTime` → `remainDistance/remainTime` 이름
- [x] (CC) `DriveStateRepository`: `CarUxRestrictions` → PARKED/DRIVING/RESTRICTED (매핑 테스트. 실제 전환은 에뮬레이터 신호 패널 조작 필요)
- [x] (CC) `AppModeReducer`: DriveState + 기어 + 충전 + 여행 진행 → PREPARE/DRIVING/PAUSE/ARRIVED + 테스트
- [x] (CC) 디버그 화면: 현재 신호·주행 상태 실시간 표시
- [x] (나) 화면 시안 확정 — docs/design/UI_SPEC.md v2 + mockups 17장

## M2 · 핵심 엔진 + 추천 (11/3 ~ 11/7)
- [x] (CC) `FatigueEngine` v1 + 단위 테스트 (기여 요인, 신호 미지원 재정규화, 정차 리셋, 신호 끊김, 시계 역행)
- [x] (CC) `NeedsEngine` (아이·반려동물·충전) + 테스트
- [x] (CC) 서버: 도로공사 수집 잡, `/reststops/along-route`, `/reststops/{id}`. `EX_API_KEY`가 비어 있으면 `server/app/data/sample_reststops.json`으로 적재 (가상 휴게소 12곳. 실제 API 호출부는 `TODO(ex-api)`: 키·응답 형식 확인 후 구현) + `/devices` 토큰
- [x] (CC) 앱: 휴게소 캐시(Room) + 앱 번들 기본 데이터 (서버 → 캐시 → 내장 순서)
- [x] (CC) `RestPlanner` 점수화 + 이유 생성 + 테스트 (+ `SuggestionPolicy`: 나중에 10분, 연속 거절 3회 2배)
- [x] (CC) 서버 `/llm/phrases` (Ollama, 2.5초 타임아웃, `num_predict=60`·`temperature=0.3`, 검사, 폴백) — httpx mock 테스트. 실제 Ollama 연동 확인은 아침 항목
- [x] (CC) 서버 시작 시 Ollama warm-up (lifespan 백그라운드, `messages=[]`, 실패해도 기동) — architecture.md §3.3
- [x] (CC) `voice`: Android TTS/STT 공급자, "응/나중에" 의도 인식, 템플릿 문장 (STT 실제 인식은 마이크 필요 → 아침 확인. 시뮬레이터는 `--es say 응` 주입)
- [x] (CC) 화면: 출발 준비(프리셋·수면 한마디), 주행 글랜스, 제안 카드 (시안 확정 전 임시 디자인)
- [ ] (나) 매일 에뮬레이터 스크린샷 → 비평 → 수정 1회

## U · UI 재구축 (10/28 ~ 10/31) — UI_SPEC v2 기준
- [x] (CC) 디자인 토큰(Color/Type/Dimens), Pretendard(OFL) 포함
- [x] (CC) 컴포넌트 13종 + @Preview (`ui/components`)
- [x] (CC) 화면 17종 + 반응형 3단계 + 발표 모드 패널(디버그 신호 화면 흡수)
- [x] (CC) 화면별 시안 비교 검증 (`docs/ui_review.md`, `artifacts/ui/final/`)
- [x] (CC) 앱 아이콘(adaptive)·스플래시
- [ ] (CC) 커밋 — BLOCKED: commit 권한 (docs/BLOCKED.md, 대기열은 docs/PROGRESS.md)

## F · 백엔드 우선 (10/29, `_private/BACKEND_FIRST_PROMPT.md`)
- [x] (CC) 화면 계약 `:core:uistate` + `docs/ui_contract.md` (새 디자인은 화면만 교체)
- [x] (CC) COMMERCIAL_READINESS P0: A 신호·B 추천·C 음성·D 정책·E 멈춤·F 콘텐츠·H 품질·I 발표 모드·G 설정·J 서버·K 버전 — 남은 P0 2개는 BLOCKED(Docker 데몬, Pleos 문서)
- [ ] (나) VHAL 장면 값 바꾸기 — BLOCKED: user 빌드 에뮬레이터 (`docs/vhal_check.md`)

## M3 · 시그니처 기능 (11/8 ~ 11/11)
- [ ] (나+CC) `content/` 라디오 에피소드 20편 이상 · 미션 15개 이상 작성 (LLM 초안 → **직접 검수**) — 초안 20·15 완료(`content/drafts/`, reviewed=false). BLOCKED: 사람 검수
- [x] (CC) 서버 `/content/*`, 플레이리스트 편성 알고리즘 + 테스트 (앱 `engine:content`와 같은 배낭 DP)
- [x] (CC) "아직 멀었어?" 응답기 + `/llm/kid-answer` (아이 답은 템플릿만 — LLM 금지 규칙)
- [x] (CC) 라디오 플레이어 (TTS 낭독, 퀴즈, 오디오 포커스) — `RadioDirector`·`SpeechQueue`
- [ ] (CC) `PauseCurator` + 멈춤 화면: 충전 대기 카드, 파워냅(벽난로·소리·타이머·기상), 원정대 미션, 펫 산책 — 큐레이터·화면·벽난로·타이머 완료, AlarmManager 기상·원정대 완료. 남은 것: 장작 소리 파일, 미션 화면(새 디자인)
- [ ] (CC) 여행 API(`/trips`, `/events`, `/report`) + 오프라인 큐 + 리포트 화면 — 서버 API 완료(10/29). 남은 것: 앱 오프라인 이벤트 큐·업로드(P1)

## M4 · 다듬기 + 제출 (11/12 ~ 11/14)
- [ ] (나+CC) test_plan.md P0 항목 전부 통과
- [ ] (나+CC) UI 마감: 3분할 1/3 창, 다크·라이트, 터치 타깃
- [ ] (나) 시연 영상 2분 녹화·편집 (시나리오 5단계)
- [ ] (CC) README 실행 방법 최종화, Releases에 APK 업로드
- [ ] (나) 저장소·영상·APK 개인정보 노출 점검
- [ ] (나) 접수양식 작성: 01 기본정보, 02 앱 소개(초안 기반), 03·04 동의·서명
- [ ] (나) **11/14** 이메일 제출: `appdevchallenge@hyundai-partners.com`, 제목 `[차량용 앱 개발 공모전] 성명_성명`
- [ ] (나, 선택) 숏폼 영상 업로드 `#HMG차량용앱개발공모전` + 2개

## M5 · 본선 고도화 (11/15 ~ 12/2)
- [ ] (CC) 확장: 정체 호흡 코치, 빗길 가중, 교대 운전 제안, 하차 안심 루틴
- [ ] (나+CC) 피로 모델 v2: 본인 주행 로그 + 합성 데이터로 개인화 보정 (5060 Ti, 선택: 학교 5090)
- [ ] (나+CC) Pleos STT/TTS가 Playground에 열리면 Android 기본 STT/TTS에서 교체 (문장 생성은 Ollama 유지)
- [ ] (CC) PT 덱, 예상 질문 답변집
- [ ] (나) 오프라인 리허설 3회 이상
- [ ] (나) 수정 제출이 필요하면 마감 전 제목 `[수정제출] [차량용 앱 개발 공모전] ...`로 전체 자료 재발송 (마감 11/15 이후 자료는 PT 전까지 고도화 가능)

## 차단 요소 · 결정 대기
- [ ] Pleos STT/TTS SDK 공모전 참가자 권한 여부 (설명회·문의)
- [ ] 본선 시연이 에뮬레이터인지 실차인지
- [ ] 외부 서버(로컬 LLM) 호출 허용 범위
