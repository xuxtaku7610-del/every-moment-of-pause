# API 명세서 — 같이가개 (반려견과 차로 가는 모든 순간)

## 0. 공통 규칙

| 항목 | 값 |
| --- | --- |
| Base URL | `http://<host>:8000/api/v1` (에뮬레이터: `http://10.0.2.2:8000/api/v1`) |
| 형식 | JSON, UTF-8, 시각은 ISO 8601 (KST `+09:00`) |
| 인증 | `X-Device-Token: <token>` 헤더 (`POST /devices`, `GET /health`, 대기 공유 페이지 `GET /wait/{token}` 제외) |
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
| 410 | `WAIT_EXPIRED` | 만료되었거나 끝난 대기 공유 |
| 422 | `VALIDATION_ERROR` | Pydantic 검증 실패 |
| 429 | `RATE_LIMITED` | 기기당 분당 120회 초과 (`/health` 제외) |
| 503 | `LLM_UNAVAILABLE` | LLM 실패 (응답에 `fallback` 문장 포함) |

## 1. 엔드포인트 목록

| 메서드 | 경로 | 설명 | 기능 |
| --- | --- | --- | --- |
| GET | `/health` | 서버·DB·LLM 상태 | 설정 |
| POST | `/devices` | 익명 기기 등록, 토큰 발급 | 최초 실행 |
| DELETE | `/devices/me` | 내 데이터 삭제 | 설정 |
| POST | `/reststops/along-route` | 경로상 휴게소·졸음쉼터(반려견 놀이터 포함) | F2 |
| GET | `/reststops/{restStopId}` | 휴게소 상세 | F2 |
| POST | `/places/along-route` | 경로 버퍼 안의 반려동물 동반 장소 | F4 |
| GET | `/places/near` | 지점 주변 동반 장소(목적지 산책 코스) | F4 |
| GET | `/vets/nearby` | 가까운 동물병원 | F5 |
| POST | `/wait` | 대기 공유 시작, 토큰 발급 | F3 |
| PUT | `/wait/{token}` | 대기 상태 올리기 | F3 |
| DELETE | `/wait/{token}` | 대기 공유 끝 | F3 |
| GET | `/wait/{token}` (API) | 대기 상태 JSON | F3 |
| GET | `/wait/{token}` (루트, `/api/v1` 밖) | **보호자 폰 웹페이지** (HTML) | F3 |
| POST | `/llm/phrases` | 제안·기록 요약 문장 | F2·F7 |
| POST | `/trips` | 여행 시작 | F7 |
| POST | `/trips/{tripId}/events` | 여행 이벤트 일괄 업로드 | F7 |
| PATCH | `/trips/{tripId}` | 여행 종료 | F7 |
| GET | `/trips/{tripId}/report` | 여행 리포트 | F7 |

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
- 반대 방향 휴게소는 뺀다: `direction`(방향 종점 이름, 예 "부산")이 경로 진행 방향과 맞지 않으면 제외. 경로 끝이 시작보다 그 종점에 가까우면 그쪽 방향이다. 종점을 모르는 방향 표기는 남긴다 (2026-10-29).
- `routeLengthMeters`(선택, 내비 총거리)를 주면 `distanceAlongRouteKm`을 좌표열 직선 거리 대신 그 척도로 늘려 준다. 앱은 같은 계산(`RouteGeo`)을 기기에서도 한다.

요청
```json
{
  "points": [
    {"lat": 37.4602, "lng": 127.0417},
    {"lat": 37.2411, "lng": 127.1776},
    {"lat": 36.8065, "lng": 127.1522}
  ],
  "corridorMeters": 1500,
  "types": ["service_area", "drowsy_shelter"],
  "routeLengthMeters": 395000
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
      "facilities": ["pet_zone", "ev_fast"],
      "evChargers": {"fast": 4, "slow": 2},
      "updatedAt": "2026-11-01T03:00:00+09:00"
    }
  ],
  "count": 1
}
```
오류: 좌표 2개 미만 → `400 INVALID_REQUEST`

### 2.4 GET `/reststops/{restStopId}`
휴게소 상세(2.3 항목과 같은 형식). 없으면 `404 REST_STOP_NOT_FOUND`.

### 2.5 POST `/places/along-route`
경로 좌표 샘플을 보내면 경로 선분에서 `bufferMeters` 안의 반려동물 동반 장소를 경로 진행 순으로 돌려준다. 좌표는 저장하지 않는다.

요청
```json
{
  "points": [{"lat": 37.5665, "lng": 126.9780}, {"lat": 37.7519, "lng": 128.8761}],
  "bufferMeters": 5000,
  "categories": ["restaurant", "cafe", "lodging", "tour", "walk"],
  "routeLengthMeters": 230000,
  "limit": 30
}
```
응답 `200`
```json
{
  "items": [
    {
      "placeId": "pp_0007",
      "name": "샘플 반려견 동반 카페",
      "category": "cafe",
      "lat": 37.70, "lng": 128.70,
      "address": "강원특별자치도 평창군 (샘플)",
      "distanceAlongRouteKm": 182.4,
      "offRouteKm": 1.2,
      "petPolicy": "소형견 동반 가능 (샘플)",
      "sample": true,
      "source": "sample"
    }
  ],
  "count": 1
}
```
- `category`: `restaurant` · `cafe` · `lodging` · `tour` · `walk`(산책로·공원).
- `sample=true`면 앱은 "샘플" 배지를 붙인다(키 없이 적재한 데이터). `source`: `tourapi` | `sample`.
- 좌표 2개 미만 → `400 INVALID_REQUEST`, `bufferMeters`는 500~20000.

### 2.6 GET `/places/near`
쿼리: `lat`, `lng`, `radiusMeters`(기본 3000, 최대 10000), `categories`(쉼표 구분, 기본 `walk,cafe`), `limit`(기본 10).
응답: 2.5와 같은 항목에 `distanceKm`(직선 거리) 추가, 가까운 순. 도착 10분 전 "목적지 근처 산책 코스" 제안에 쓴다.

### 2.7 GET `/vets/nearby`
쿼리: `lat`, `lng`, `limit`(기본 5, 최대 20). 영업 중인 병원만, 가까운 순.
```json
{
  "items": [
    {"vetId": "vt_0003", "name": "샘플 동물병원", "address": "서울특별시 (샘플)", "lat": 37.56, "lng": 126.98, "distanceKm": 1.4, "phone": null, "sample": true, "source": "sample"}
  ],
  "count": 1
}
```
- 인허가 파일(`data/raw/`)로 적재했으면 `source=licensing`, `phone`은 파일에 있으면 채운다(사업장 번호). 샘플은 전화번호를 지어내지 않고 `null`.

### 2.8 대기 공유 (F3)
앱이 P단에서 "잠깐 다녀올게"를 시작하면 토큰을 받고, 10초마다 상태를 올린다. 보호자 폰은 차 화면의 QR(또는 짧은 주소)로 `GET /wait/{token}` 페이지를 연다. **위치는 받지도 저장하지도 않는다.**

**POST `/wait`** (기기 토큰 필요)
```json
{"limitMin": 10}
```
응답 `201`
```json
{"token": "k3J9...22자", "pageUrl": "/wait/k3J9...", "expiresAt": "2026-11-02T13:12:00+09:00"}
```
- 토큰은 `secrets.token_urlsafe(16)`(128비트). 만료 2시간. 기기당 동시에 하나(새로 만들면 이전 것은 끝남).

**PUT `/wait/{token}`** (만든 기기 토큰만)
```json
{"state": "waiting", "elapsedSec": 185, "limitMin": 10, "batteryPercent": 61, "cabinTempC": null}
```
- `state`: `waiting` · `overdue`(제한 시간 초과) · `ended`. `cabinTempC`는 신호가 없으면 `null`.
- 정의되지 않은 필드(예: `lat`, `lng`, `location`)는 `422` — 실수로라도 위치가 저장되지 않게.
- 만료·끝난 토큰 → `410 WAIT_EXPIRED`. 다른 기기 토큰 → `404 WAIT_NOT_FOUND`.

**DELETE `/wait/{token}`** → `204`, 상태 `ended`.

**GET `/api/v1/wait/{token}`** (인증 없음) → 마지막 상태 JSON (`state`, `elapsedSec`, `limitMin`, `batteryPercent`, `cabinTempC`, `updatedAt`, `stale`). `stale`은 마지막 갱신이 30초를 넘으면 `true`.

**GET `/wait/{token}`** (인증 없음, HTML) → 큰 글자 한 화면: "주인이 곧 돌아와요 / 3분째 / 배터리 61% / 실내 온도 정보 없음". 5초마다 새로고침(`<meta http-equiv="refresh" content="5">`), 초과면 빨간 배경 "시간 초과 — 차로 돌아가 주세요". 만료면 `410` 안내 페이지. 검색 엔진 색인 금지(`noindex`).

### 2.9 POST `/llm/phrases`
앱이 계산한 추천 결과를 자연스러운 한국어 2문장 이내로 바꾼다. 앱은 같은 내용의 템플릿 문장을 항상 함께 준비한다.

요청
```json
{
  "kind": "rest_suggestion",
  "facts": {
    "restStopName": "OO휴게소",
    "minutesAway": 18,
    "reasons": ["motion", "pet_zone"],
    "fatigueLevel": "low",
    "passengers": ["dog"]
  },
  "maxSentences": 2
}
```
응답 `200`
```json
{"text": "18분 뒤 OO휴게소에 반려견 놀이터가 있어요. 콩이 쉬어 갈까요?", "source": "ollama", "latencyMs": 840}
```
응답 `503`
```json
{"error": {"code": "LLM_UNAVAILABLE", "message": "LLM 응답 실패"}, "fallback": "18분 뒤 OO휴게소에서 쉬어 갈까요?"}
```
서버 검사: 2문장 초과·50자 초과·금칙어 포함 시 `fallback` 반환.

`kind`: `rest_suggestion`(F2 제안), `trip_summary`(F7 기록 요약, facts: `durationMin`·`stops`·`motionPeak`·`limitMin`). 안전·멀미 예보·"내렸나요?" 문구는 LLM을 쓰지 않는다.

서버 → Ollama 호출: `POST {OLLAMA_BASE_URL}/api/chat`
```json
{"model": "gemma4:12b", "messages": [{"role": "system", "content": "..."}, {"role": "user", "content": "..."}], "stream": false, "think": false, "keep_alive": "30m", "options": {"num_predict": 60, "temperature": 0.3}}
```
모델명은 `.env`의 `OLLAMA_MODEL`, 상주 시간은 `OLLAMA_KEEP_ALIVE`, 최대 토큰은 `OLLAMA_NUM_PREDICT`에서 읽는다. `temperature`는 0.3 고정. 실측(2026-10-27, `gemma4:12b`, 모델 상주, 100% GPU) 2.58초였으므로 2.5초 목표에 맞추려고 `num_predict`로 출력 길이를 묶는다. OpenAI 호환 API는 `think` 지원이 불확실해 쓰지 않는다.

### 2.10 POST `/trips`
요청
```json
{"startedAt": "2026-11-02T09:00:00+09:00", "originLabel": "서울", "destinationLabel": "부산", "plannedDistanceKm": 395, "passengerSummary": {"adults": 1, "children": 0, "pets": 1}, "powertrain": "ev"}
```
응답 `201`
```json
{"tripId": "tr_5b0e", "status": "active"}
```

`passengerSummary`는 **인원 수만** 받는다(결정 2026-10-29: 개인정보 안내 "동승자 이름·나이는 보내지 않아요"와 맞춤). 나이 목록·이름 같은 다른 필드는 `422`.

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
이벤트 종류: `recommendation_shown`, `recommendation_accepted`, `recommendation_declined`, `stop_started`, `stop_ended`, `fatigue_peak`, `motion_peak`, `pause_activity_started`, `pause_activity_ended`, `wait_started`, `wait_ended` (마이그레이션 0002에서 `kid_question` 삭제)

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
  "motionPeak": {"value": 62, "at": "2026-11-02T12:10:00+09:00"},
  "waits": 1,
  "tips": ["다음엔 첫 휴식을 20분 앞당겨 보세요."]
}
```

### 2.14 DELETE `/devices/me`
내 데이터 삭제. 이 기기와 그 기기의 여행·이벤트·대기 공유를 지운다. LLM 호출 기록은 기기 연결만 끊는다. 응답 `204`. 이후 같은 토큰은 `401`.

### 공통: 요청 ID·레이트 리밋 (2026-10-29)
- 모든 응답에 `X-Request-ID` 헤더. 요청에 `X-Request-ID`(영숫자·`-`·`_` 64자 이하)를 보내면 그대로 쓴다. 서버 로그(JSON 한 줄)에 같은 ID가 남는다.
- 기기 토큰(없으면 IP)마다 60초에 120회(`RATE_LIMIT_PER_MINUTE`). 넘으면 `429 RATE_LIMITED`, `Retry-After: 60`. `/health`는 제외.

## 3. 앱 내부 인터페이스 (참고)
서버 API는 아니지만 시뮬레이터와 실제 SDK가 같은 계약을 따르도록 정의한다.

```kotlin
interface VehicleSource {
    val capabilities: StateFlow<Set<Signal>>
    fun samples(): Flow<SignalSample>   // 1Hz
}
interface NaviSource {
    fun route(): Flow<RouteInfo?>        // 현재 위치·목적지, 총 거리 (폴리라인 없음, sdk_check.md)
    fun eta(): Flow<EtaInfo?>            // 목적지·다음 경유지
    suspend fun addWaypoint(lat: Double, lng: Double, name: String): Result<Unit>
}
interface PhraseProvider {
    suspend fun phrase(req: PhraseRequest): Phrase   // 실패 시 템플릿으로 내부 폴백
}
```
