# API 명세서 — 쉼표 (멈춤의 모든 순간)

## 0. 공통 규칙

| 항목 | 값 |
| --- | --- |
| Base URL | `http://<host>:8000/api/v1` (에뮬레이터: `http://10.0.2.2:8000/api/v1`) |
| 형식 | JSON, UTF-8, 시각은 ISO 8601 (KST `+09:00`) |
| 인증 | `X-Device-Token: <token>` 헤더 (`POST /devices`, `GET /health` 제외) |
| 버전 | URL 경로 `/v1` |
| 타임아웃 권장 | 일반 5초, `/llm/*` 2.5초 (앱은 초과 시 템플릿 사용) |
| 문서 | FastAPI 자동 문서 `/docs` |

### 오류 형식
```json
{
  "error": {
    "code": "REST_STOP_NOT_FOUND",
    "message": "해당 휴게소를 찾을 수 없습니다.",
    "detail": {"restStopId": "rs_999"}
  }
}
```

| HTTP | code | 의미 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 필드 누락·형식 오류 |
| 401 | `INVALID_DEVICE_TOKEN` | 토큰 없음·만료 |
| 404 | `*_NOT_FOUND` | 리소스 없음 |
| 409 | `TRIP_ALREADY_ENDED` | 종료된 여행에 쓰기 |
| 422 | `VALIDATION_ERROR` | Pydantic 검증 실패 |
| 429 | `RATE_LIMITED` | 기기당 분당 120회 초과 (`/health` 제외) |
| 503 | `LLM_UNAVAILABLE` | LLM 실패 (응답에 `fallback` 문장 포함) |

## 1. 엔드포인트 목록

| 메서드 | 경로 | 설명 | 사용 화면 |
| --- | --- | --- | --- |
| GET | `/health` | 서버·DB·LLM 상태 | 설정 |
| POST | `/devices` | 익명 기기 등록, 토큰 발급 | 최초 실행 |
| POST | `/reststops/along-route` | 경로상 휴게소·졸음쉼터 목록 | 출발 준비 |
| GET | `/reststops/{restStopId}` | 휴게소 상세 | 정차 화면 |
| GET | `/content/episodes` | 라디오 에피소드 목록 | 라디오 |
| POST | `/content/playlists` | 목표 시간에 맞춘 플레이리스트 편성 | 라디오 |
| GET | `/content/missions` | 원정대 미션 목록 | 멈춤 화면 |
| POST | `/llm/phrases` | 휴게소 제안 문장 생성 | 제안 카드 |
| POST | `/llm/kid-answer` | "아직 멀었어?" 답변 생성 | 음성 |
| POST | `/trips` | 여행 시작 | 출발 준비 |
| POST | `/trips/{tripId}/events` | 여행 이벤트 일괄 업로드 | 백그라운드 |
| PATCH | `/trips/{tripId}` | 여행 종료 | 도착 |
| GET | `/trips/{tripId}/report` | 여행 리포트 | 리포트 |

## 2. 상세

### 2.1 GET `/health`
**인증 면제** (`X-Device-Token` 불필요), **rate limit 대상 아님**.

응답
```json
{"status": "degraded", "db": "ok", "llm": "degraded", "version": "0.1.0"}
```

| 필드 | 값 | 판정 |
| --- | --- | --- |
| `db` | `ok` \| `down` | `SELECT 1` 성공이면 `ok`, 실패하거나 1초를 넘기면 `down` |
| `llm` | `ok` \| `degraded` \| `down` | Ollama `/api/tags`가 1초 안에 응답하고 `OLLAMA_MODEL`이 목록에 있으면 `ok`. 응답은 오지만 모델이 없거나 느리면 `degraded`. 응답이 없으면 `down`. 결과는 30초 캐시 |
| `status` | `ok` \| `degraded` \| `down` | `db = down`이면 `down`. 아니고 `llm`이 `ok`가 아니면 `degraded`. 둘 다 `ok`면 `ok` |
| `version` | `x.y.z` | 서버 버전 |

| HTTP | 조건 |
| --- | --- |
| `503` | `db = down` |
| `200` | 그 외 (`degraded` 포함) |

앱 동작: `503`이면 캐시·템플릿 모드로 전환한다. `llm = down`이면 LLM 호출을 건너뛰고 템플릿 문장을 쓴다.

### 2.2 POST `/devices`
요청
```json
{"platform": "pleos-connect", "appVersion": "0.1.0"}
```
응답 `201`
```json
{"deviceId": "dv_8f2c1a", "token": "eyJhbGciOi...", "issuedAt": "2026-11-02T09:00:00+09:00"}
```

### 2.3 POST `/reststops/along-route`
경로를 일정 간격으로 샘플링한 좌표를 보내면, 진행 방향 기준 앞쪽 휴게소를 경로상 거리 순으로 반환한다. 좌표는 저장하지 않는다.

요청
```json
{
  "points": [
    {"lat": 37.4602, "lng": 127.0417},
    {"lat": 37.2411, "lng": 127.1776},
    {"lat": 36.8065, "lng": 127.1522}
  ],
  "corridorMeters": 1500,
  "types": ["service_area", "drowsy_shelter"]
}
```

응답 `200`
```json
{
  "items": [
    {
      "restStopId": "rs_0123",
      "name": "OO휴게소",
      "type": "service_area",
      "routeName": "경부선",
      "direction": "부산",
      "lat": 37.0123,
      "lng": 127.1011,
      "distanceAlongRouteKm": 42.3,
      "facilities": ["nursing_room", "kids_zone", "pet_zone", "ev_fast"],
      "evChargers": {"fast": 4, "slow": 2},
      "updatedAt": "2026-11-01T03:00:00+09:00"
    }
  ],
  "count": 1
}
```
오류: 좌표 2개 미만 → `400 INVALID_REQUEST`

### 2.4 GET `/reststops/{restStopId}`
응답 `200`: 2.3의 항목 + `phone`, `address`, `themes`(테마휴게소 정보).

### 2.5 GET `/content/episodes`
쿼리: `topic`(proverb·common_sense·safety·manners), `age`(정수), `maxSeconds`

응답 `200`
```json
{
  "items": [
    {
      "episodeId": "ep_proverb_012",
      "topic": "proverb",
      "title": "가는 말이 고와야 오는 말이 곱다",
      "durationSec": 240,
      "minAge": 5,
      "maxAge": 9,
      "script": "오늘의 속담은 ...",
      "quiz": {"question": "고운 말을 하면 어떻게 될까?", "choices": ["좋은 말이 돌아와요", "아무 일도 없어요"], "answerIndex": 0}
    }
  ]
}
```
`reviewed = true`인 에피소드만 반환한다.

### 2.6 POST `/content/playlists`
남은 시간에 꼭 맞게 에피소드를 고른다(합계가 목표 시간 이하이면서 가장 가깝게).

요청
```json
{"targetSeconds": 1080, "topics": ["proverb", "safety"], "age": 5, "excludeEpisodeIds": ["ep_proverb_012"]}
```
응답 `200`
```json
{
  "playlistId": "pl_77a1",
  "totalSec": 1050,
  "items": [
    {"episodeId": "ep_proverb_004", "durationSec": 300},
    {"episodeId": "ep_safety_002", "durationSec": 450},
    {"episodeId": "ep_proverb_019", "durationSec": 300}
  ]
}
```

### 2.7 GET `/content/missions`
쿼리: `context`(rest_stop·charging), `age`, `maxSeconds`

응답 `200`
```json
{
  "items": [
    {"missionId": "ms_001", "context": "rest_stop", "title": "물 한 컵 마시기", "instructions": "휴게소 정수기에서 물 한 컵!", "durationSec": 120, "minAge": 4}
  ]
}
```

### 2.8 POST `/llm/phrases`
앱이 계산한 추천 결과를 자연스러운 한국어 2문장 이내로 바꾼다. 앱은 같은 내용의 템플릿 문장을 항상 함께 준비한다.

요청
```json
{
  "kind": "rest_suggestion",
  "facts": {
    "restStopName": "OO휴게소",
    "minutesAway": 18,
    "reasons": ["kids_zone", "ev_fast"],
    "fatigueLevel": "medium",
    "passengers": ["child_5", "dog"]
  },
  "maxSentences": 2
}
```
응답 `200`
```json
{"text": "18분 뒤 OO휴게소에 키즈존과 급속충전기가 있어요. 들렀다 갈까요?", "source": "ollama", "latencyMs": 840}
```
응답 `503`
```json
{"error": {"code": "LLM_UNAVAILABLE", "message": "LLM 응답 실패"}, "fallback": "18분 뒤 OO휴게소에서 쉬어 갈까요?"}
```
서버 검사: 2문장 초과·50자 초과·금칙어 포함 시 `fallback` 반환.

서버 → Ollama 호출 (2.8·2.9 공통): `POST {OLLAMA_BASE_URL}/api/chat`
```json
{"model": "gemma4:12b", "messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "..."}], "stream": false, "think": false, "keep_alive": "30m", "options": {"num_predict": 60, "temperature": 0.3}}
```
모델명은 `.env`의 `OLLAMA_MODEL`, 상주 시간은 `OLLAMA_KEEP_ALIVE`, 최대 토큰은 `OLLAMA_NUM_PREDICT`에서 읽는다. `temperature`는 0.3 고정. 실측(2026-10-27, `gemma4:12b`, 모델 상주, 100% GPU) 2.58초였으므로 2.5초 목표에 맞추려고 `num_predict`로 출력 길이를 묶는다. OpenAI 호환 API는 `think` 지원이 불확실해 쓰지 않는다.

### 2.9 POST `/llm/kid-answer`
요청
```json
{"question": "아직 멀었어?", "minutesToNextStop": 18, "remainingEpisodes": 2, "nextStopHighlights": ["kids_zone"], "childAge": 5}
```
응답 `200`
```json
{"text": "속담 라디오 두 편만 들으면 키즈존 휴게소에 도착해!", "source": "ollama"}
```

### 2.10 POST `/trips`
요청
```json
{"startedAt": "2026-11-02T09:00:00+09:00", "originLabel": "서울", "destinationLabel": "부산", "plannedDistanceKm": 395, "passengerSummary": {"adults": 2, "children": [5], "pets": ["dog"]}, "powertrain": "ev"}
```
응답 `201`
```json
{"tripId": "tr_5b0e", "status": "active"}
```

### 2.11 POST `/trips/{tripId}/events`
네트워크가 끊긴 동안 쌓인 이벤트를 한 번에 올린다. `clientEventId`로 중복 업로드를 무시한다.

요청
```json
{
  "events": [
    {"clientEventId": "e-001", "type": "recommendation_shown", "occurredAt": "2026-11-02T10:42:00+09:00", "restStopId": "rs_0123", "payload": {"score": 0.82, "fatigue": 61}},
    {"clientEventId": "e-002", "type": "recommendation_accepted", "occurredAt": "2026-11-02T10:42:05+09:00", "restStopId": "rs_0123"},
    {"clientEventId": "e-003", "type": "stop_started", "occurredAt": "2026-11-02T11:00:00+09:00", "restStopId": "rs_0123", "payload": {"reason": "charging", "plannedMinutes": 27}}
  ]
}
```
응답 `202`
```json
{"accepted": 3, "duplicates": 0}
```
이벤트 종류: `recommendation_shown`, `recommendation_accepted`, `recommendation_declined`, `stop_started`, `stop_ended`, `fatigue_peak`, `kid_question`, `pause_activity_started`, `pause_activity_ended`

### 2.12 PATCH `/trips/{tripId}`
요청
```json
{"status": "ended", "endedAt": "2026-11-02T14:20:00+09:00", "actualDistanceKm": 398.4}
```
응답 `200`: `{"tripId": "tr_5b0e", "status": "ended"}`. 이미 종료 → `409 TRIP_ALREADY_ENDED`

### 2.13 GET `/trips/{tripId}/report`
```json
{
  "tripId": "tr_5b0e",
  "durationMin": 320,
  "stops": 2,
  "longestContinuousDriveMin": 105,
  "fatiguePeak": {"value": 74, "at": "2026-11-02T12:58:00+09:00"},
  "suggestionsShown": 3,
  "suggestionsAccepted": 2,
  "kidQuestions": 4,
  "tips": ["다음엔 첫 휴식을 20분 앞당겨 보세요."]
}
```

## 3. 앱 내부 인터페이스 (참고)
서버 API는 아니지만 시뮬레이터와 실제 SDK가 같은 계약을 따르도록 정의한다.

```kotlin
interface VehicleSource {
    val capabilities: StateFlow<Set<Signal>>
    fun samples(): Flow<SignalSample>   // 1Hz
}
interface NaviSource {
    fun route(): Flow<RouteInfo?>        // 좌표 샘플, 총 거리
    fun eta(): Flow<EtaInfo?>            // 목적지·다음 경유지
    suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit>
}
interface PhraseProvider {
    suspend fun phrase(req: PhraseRequest): Phrase   // 실패 시 템플릿으로 내부 폴백
}
```
