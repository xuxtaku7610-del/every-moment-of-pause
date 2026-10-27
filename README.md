# 쉼표 — 멈춤의 모든 순간 (Every Moment of Pause)

> 달릴 때는 **언제, 어디서 멈출지** 먼저 알려주고, 멈췄을 때는 **그 시간을 무엇으로 채울지** 골라주는 Pleos Connect 차량용 앱.

현대자동차그룹 차량용 앱 개발 공모전 출품작 (대학(원)생 부문, 개인)

---

## 1. 프로젝트 개요

### 문제
- 2026년 상반기 고속도로 사망자 중 **졸음·주시태만이 71.6%** (한국도로공사). 운전자는 피로를 늦게 자각하고, 도착 시간 압박으로 휴식을 미룬다.
- 가족 이동에서 "쉬어야 하는 이유"는 운전자 피로만이 아니다. 아이 화장실·수유, 반려동물 산책, 전기차 충전이 제각각 다른 시점에 필요하다. (국내 반려동물 양육 가구 29.2%, 2025 농식품부)
- 막상 멈추면(휴게소·충전·졸음쉼터) 그 시간은 **기다림**이 된다. 아이는 "아직 멀었어?"를 반복하고, 운전자는 제대로 쉬지 못한 채 다시 출발한다.

### 해결
쉼표는 차량 데이터(주행 시간·속도·기어·공조·배터리·충전 상태)와 내비 경로를 함께 읽어 **이동 중 '멈춤'의 모든 순간**을 설계한다.

| 단계 | 쉼표가 하는 일 |
| --- | --- |
| 출발 전 (정차) | 동승자 프리셋(아이·반려동물·교대 운전자), "어젯밤 몇 시간 주무셨어요?" 한마디로 휴식 계획 초안 |
| 달릴 때 (주행) | 피로 위험 지수 + 가족 니즈 타이머 → 경로상 최적 휴게소를 **이유 한 문장과 함께 음성 제안**, "응" 한마디로 경유지 추가 |
| 달릴 때 (뒷좌석) | 아이가 "아직 멀었어?" → "속담 라디오 2편 들으면 도착!" 도착 시간에 맞춘 라디오 편성 |
| 멈췄을 때 (P단) | 남은 정차·충전 시간에 맞춰 **파워냅(벽난로 모드) · 아이 원정대 미션 · 펫 산책 타이머 · 업무 집중** 중 하나를 추천 |
| 도착 후 | 여행 리포트: 휴식 횟수, 최장 연속 주행, 피로 피크, 다음 여행 팁 |

### 심사 항목과의 연결
| 심사 항목 | 쉼표의 근거 |
| --- | --- |
| 차량 특화성 | 연속 주행·공조·배터리·충전·기어·경로를 동시에 읽어야만 가능한 판단. 스마트폰 앱으로는 불가 |
| 창의성 | "경고하는 차"가 아니라 "멈춤을 먼저 계획하고, 멈춘 시간을 채워주는 차". 충전 시간 = 콘텐츠 길이 |
| 기술 활용성 | Vehicle SDK, NaviHelper SDK, CarUxRestrictions, (승인 시) Gleo AI STT/TTS/LLM, 로컬 LLM(Ollama) 폴백, 공공데이터(한국도로공사 휴게소 API) |

---

## 2. 핵심 기능

### MVP (11/15 접수 전 완성 목표)
| ID | 기능 | 상태 |
| --- | --- | --- |
| F1 | 출발 준비: 동승자 프리셋 + 수면 한마디 | 필수 |
| F2 | 피로 위험 지수 v1 (규칙 기반, 기여 요인 공개) | 필수 |
| F3 | 가족 니즈 타이머 (아이·반려동물·충전) | 필수 |
| F4 | 휴게소 추천 + 음성 제안 + 경유지 추가 | 필수 (핵심) |
| F5 | "아직 멀었어?" 응답기 + 도착 맞춤 라디오 | 필수 (시그니처) |
| F6 | 멈춤 큐레이터: 충전 대기 · 파워냅(벽난로) · 원정대 미션 · 펫 산책 | 필수 (시그니처) |
| F7 | 여행 리포트 | 필수 |
| F8 | 시연용 주행 시뮬레이터 (서울→부산 5시간 → 3분 재생) | 필수 (개발·시연용) |

### 확장 (서류 통과 후, 12/3 PT 전)
정체 구간 호흡 코치 · 빗길 위험 가중 · 교대 운전 제안 · 하차 안심 루틴 · 피로 모델 v2(개인화) · 보호자 폰 알림

---

## 3. 기술 스택

| 영역 | 기술 | 비고 |
| --- | --- | --- |
| 차량 앱 | Kotlin 2.x, Jetpack Compose, Hilt, Coroutines/Flow, Room | Android Automotive OS (Pleos Connect), minSdk 26 |
| 차량 연동 | Pleos Connect Vehicle SDK, NaviHelper SDK, `CarUxRestrictionsManager` | 둘 다 신청 없이 사용 가능 |
| 음성 | Gleo AI STT/TTS (승인 시) → Android `SpeechRecognizer` / `TextToSpeech` 폴백 | 공급자 추상화 |
| 백엔드 | Python 3.11, FastAPI, SQLAlchemy 2, Alembic, Pydantic v2 | 휴게소 데이터, 라디오 콘텐츠, LLM 게이트웨이 |
| DB | PostgreSQL 16 (로컬 개발은 SQLite 허용) | Docker Compose |
| LLM | Ollama (집 PC RTX 5060 Ti 16GB, 7~8B 한국어 모델) | OpenAI 호환 API, 템플릿 문장 폴백 |
| 외부 데이터 | 한국도로공사 공공 API (휴게소 기준정보·편의시설) | 공공데이터포털 인증키 |
| 테스트 | JUnit5, MockK, Turbine, Compose UI Test / pytest, httpx | |
| 빌드·배포 | Gradle 8, GitHub Actions(APK 빌드), GitHub Releases | 1차 접수 가산점: 소스 + APK |

---

## 4. 저장소 구조

```
every-moment-of-pause/
├─ README.md · architecture.md · flowchart.md · api_spec.md
├─ database.md · test_plan.md · todo.md
├─ app-android/                 # Pleos Connect 앱 (Gradle 멀티모듈)
│  ├─ app/                      # Compose 화면, ViewModel, 내비게이션
│  ├─ core/model/               # 도메인 모델
│  ├─ core/vehicle/             # Vehicle SDK 어댑터 (+ capability 확인)
│  ├─ core/navi/                # NaviHelper 어댑터
│  ├─ core/restriction/         # CarUxRestrictions → DriveState
│  ├─ engine/fatigue/           # 피로 위험 지수
│  ├─ engine/needs/             # 가족 니즈 타이머
│  ├─ engine/planner/           # 휴게소 점수화·추천
│  ├─ engine/pause/             # 멈춤 큐레이터
│  ├─ voice/                    # STT·TTS·LLM 공급자
│  ├─ data/                     # Room, Retrofit, 캐시
│  └─ sim/                      # 주행 시나리오 리플레이어
├─ server/                      # FastAPI 백엔드
│  ├─ app/ (api, models, services, jobs)
│  ├─ alembic/
│  └─ tests/
├─ content/                     # 라디오 에피소드·미션 원본 (검수된 JSON)
├─ scenarios/                   # 시뮬레이터 시나리오 JSON
└─ docker-compose.yml
```

---

## 5. 실행 방법

### 사전 준비
- Android Studio (최신 안정판), JDK 17
- Pleos Playground 가입 → 프로젝트 생성 → **Pleos Connect Emulator** 설치 및 AVD 설정
- Docker Desktop, Python 3.11
- Ollama 설치 + 한국어 모델 1개 (`ollama pull <모델명>`)
- 공공데이터포털 한국도로공사 휴게소 API 인증키

### 1) 환경 변수
```bash
cp server/.env.example server/.env
# EX_API_KEY=<도로공사 인증키>
# OLLAMA_BASE_URL=http://localhost:11434
# OLLAMA_MODEL=<모델명>
# DATABASE_URL=postgresql+psycopg://pause:pause@localhost:5432/pause
```

### 2) 백엔드
```bash
docker compose up -d db
cd server
python -m venv .venv && .venv\Scripts\activate      # Windows
pip install -r requirements.txt
alembic upgrade head
python -m app.jobs.ingest_reststops                  # 휴게소 데이터 적재
python -m app.jobs.seed_content                      # 라디오·미션 콘텐츠 적재
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```
확인: `http://localhost:8000/docs`

### 3) 차량 앱
1. Android Studio에서 `app-android/` 열기
2. `local.properties`에 `SERVER_BASE_URL=http://10.0.2.2:8000` (에뮬레이터 → 호스트 PC)
3. Pleos Connect Emulator AVD 선택 후 Run
4. 시연: 앱 설정 → **시뮬레이션 모드** → `scenarios/seoul-busan-family.json` 재생

### 4) 테스트
```bash
cd server && pytest
cd app-android && ./gradlew test connectedAndroidTest
```

---

## 6. 개인정보·제출 유의사항
- 앱은 계정 없이 **익명 기기 토큰**만 사용. 위치는 여행 중에만 쓰고 서버에는 휴게소 이벤트 단위로만 저장.
- 공모전 규정상 **공유 링크·첨부자료에 개발자 실명·계정정보가 노출되면 안 됨** → GitHub 계정명, 커밋 작성자명, APK 서명 정보, 영상 화면을 점검.
- 1차 접수 가산점: 시연 영상 · GitHub 저장소(소스 + APK 함께). APK만 따로 제출하지 않는다.

## 7. 참고
- 아키텍처: [architecture.md](architecture.md) · 흐름도: [flowchart.md](flowchart.md) · API: [api_spec.md](api_spec.md)
- DB: [database.md](database.md) · 테스트: [test_plan.md](test_plan.md) · 일정: [todo.md](todo.md)
