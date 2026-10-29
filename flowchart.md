# 흐름도 — 같이가개 (반려견과 차로 가는 모든 순간)

## 1. 핵심 유저 저니 (CONCEPT 7장 시연 흐름)

```mermaid
flowchart TD
    A["출발 체크<br/>콩이 프로필 · 하네스/켄넬 · 물 · 배변봉투"] --> B{"체크 완료?"}
    B -->|"아니오"| A
    B -->|"예"| C["이대로 출발<br/>D단 · 속도"]
    C --> D["주행 글랜스<br/>멀미 예보 한 줄 · 다음 쉼표"]
    D --> E{"10초마다 판단"}
    E -->|"앞 구간 굽은 길"| F["멀미 예보<br/>3km 뒤 굽은 길 8분, 조금 일찍 감속"]
    E -->|"멀미 누적·배변 간격·피로"| G["반려견 맞춤 쉼표 1곳 제안"]
    E -->|"이상 없음"| D
    F --> D
    G --> H{"운전자 응답"}
    H -->|"여기서 쉴게요 / 응"| I["경유지 추가"]
    H -->|"나중에 · 응답 없음"| J["쿨다운 후 재평가"]
    J --> D
    I --> K["휴게소 P단 → 쉼표 화면<br/>산책 · 물 · 배변"]
    K --> L{"잠깐 다녀올게?"}
    L -->|"예"| M["대기 모드<br/>차 화면 바깥 사람용 안내<br/>보호자 폰 웹페이지"]
    L -->|"아니오"| N["다시 출발"]
    M -->|"제한 시간 초과"| M2["폰 · 차 화면 시간 초과 알림"]
    M -->|"D단"| N
    M2 -->|"D단"| N
    N --> D
    D -->|"목적지 도착"| O["도착 체크<br/>콩이 내렸나요?"]
    O --> P["이동 기록<br/>멀미 추이 · 한계 시간 · 병원용 요약"]
```

## 2. 시퀀스 — 멀미 예보 (F1)

```mermaid
sequenceDiagram
    participant SIG as 차량 신호 1초
    participant NAV as NaviHelper · 경로 형상
    participant ME as MotionComfortEngine
    participant TB as TripBrain
    participant V as 음성 · 화면

    loop 1초마다
        SIG->>ME: 속도(잡음 처리) · 방위
        ME->>ME: a_x = Δv/Δt, a_y = v·Δψ/Δt (없으면 v²·κ)
        ME->>ME: 0.02~0.5Hz 대역 → 제곱 적분 → 프로필 보정 → 지수 0~100
    end
    loop 10초마다
        NAV->>ME: 앞 10~20분 경로 형상(곡률) · 정체 구간
        ME->>ME: 현재 속도로 가정해 미리 적분
        ME-->>TB: 예보(위험 구간 시작 거리 · 지속 시간 · 예상 지수)
        TB->>TB: 쿨다운 10분 · 같은 구간이면 다시 말하지 않음
        TB->>V: "3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속" (템플릿)
    end
```

## 3. 시퀀스 — 반려견 맞춤 쉼표와 경유지 추가 (F2)

```mermaid
sequenceDiagram
    participant TB as TripBrain
    participant PL as PetRestPlanner
    participant LLM as 서버 /llm/phrases
    participant V as 음성(STT·TTS)
    participant NAV as NaviHelper

    TB->>PL: 멀미 누적 · 주행 시간 · 배변 간격 · 운전자 피로 · 앞쪽 휴게소
    PL->>PL: 점수 = 니즈 시각 맞춤 + 반려견 시설 가중치, 히스테리시스 15%
    PL-->>TB: 추천 1곳 + 이유(기여도 상위 1~2개)
    TB->>LLM: 제안 문장 (2.5초)
    alt 2.5초 안 · 검사 통과
        LLM-->>TB: 문장
    else 실패
        TB->>TB: 템플릿 문장
    end
    TB->>V: "18분 뒤 OO휴게소에 반려견 놀이터가 있어요. 콩이 쉬어 갈까요?"
    V-->>TB: "여기서 쉴게요"
    TB->>NAV: addWaypoint(위도, 경도, 이름)
    NAV-->>TB: onWaypointChanged
    TB->>V: "경로에 OO휴게소를 넣었어요"
```

## 4. 시퀀스 — 잠깐 대기 모드 (F3)

```mermaid
sequenceDiagram
    participant U as 운전자
    participant APP as 앱 WaitMode
    participant S as 서버 /wait
    participant P as 보호자 폰

    U->>APP: "잠깐 다녀올게" (P단에서만)
    APP->>S: POST /wait {limitMin}
    S-->>APP: token (2시간 만료)
    APP->>APP: 차 화면 → "주인이 곧 돌아와요 · 3분째 · 실내 확인 중" + QR
    loop 10초마다
        APP->>S: PUT /wait/{token} {state, elapsedSec, batteryPercent, cabinTempC}
    end
    loop 5초마다
        P->>S: GET /wait/{token} (HTML)
        S-->>P: 경과 시간 · 배터리 · 실내 온도(없으면 "정보 없음")
    end
    APP->>APP: 제한 시간 초과 → 초과 이벤트 1번
    APP->>S: state = overdue
    S-->>P: 빨간 화면 "시간 초과 — 차로 돌아가 주세요"
    U->>APP: D단(또는 DriveState DRIVING)
    APP->>S: DELETE /wait/{token}
    APP->>APP: 대기 모드 종료 → 주행 화면
```

## 5. 시퀀스 — 도착 체크와 이동 기록 (F6·F7)

```mermaid
sequenceDiagram
    participant SIG as 기어 · 도착
    participant BC as BoardingCheck
    participant UI as 화면
    participant LOG as PetTripLog

    SIG->>BC: 기어 P 60초 이상 + 목적지 도착
    BC->>BC: 실내 26℃ 이상 또는 6~9월(온도 모름) → 강한 알림
    BC-->>UI: "콩이 내렸나요?"
    UI-->>BC: "내렸어요"
    BC->>LOG: 여행 끝
    LOG->>LOG: 이동 시간 · 멀미 추이 · 휴식 · 주의 도달 시각
    LOG-->>UI: "콩이는 약 1시간 10분이 한계예요" · 병원용 요약
```

## 6. 데이터 흐름

```mermaid
flowchart LR
    subgraph PUBLIC["공공 데이터"]
        EX["도로공사 휴게소"]
        TOUR["관광공사 TourAPI<br/>반려동물 동반여행"]
        LIC["지방행정 인허가<br/>동물병원 파일"]
    end
    subgraph SERVER["서버"]
        J1["ingest_reststops"]
        J2["ingest_pet_places"]
        J3["load_vets"]
        DB[("PostgreSQL")]
        API["FastAPI"]
    end
    subgraph APP["앱"]
        CACHE[("Room 캐시")]
        ENG["엔진"]
        PROF[("반려견 프로필<br/>기기에만")]
    end
    EX --> J1 --> DB
    TOUR --> J2 --> DB
    LIC --> J3 --> DB
    DB --> API -->|"경로 버퍼 · 주변 검색"| CACHE --> ENG
    PROF --> ENG
    ENG -->|"대기 상태 (위치 없음)"| API
```

키가 없으면 각 잡은 `server/app/data/sample_*.json`을 같은 정규화 경로로 적재하고 `source=sample`을 남긴다. 앱은 같은 샘플을 내장 데이터로 묶어 서버 없이도 동작한다.

## 7. 예외 흐름 요약

| 상황 | 동작 |
| --- | --- |
| 서버 없음 | 휴게소·장소·병원은 내장·캐시 데이터, 문장은 템플릿, 대기 모드는 차 화면만("폰 공유 연결 안 됨") |
| LLM 2.5초 초과·검사 실패 | 템플릿 문장 |
| 방위·경로 형상 없음 | 종방향 가속만으로 멀미 지수, 예보는 "곡선 정보 없음" |
| 실내 온도 신호 없음 | "실내 온도 정보 없음"으로 표시, 여름철이면 도착 체크를 강한 단계로 |
| 음성 인식 실패 | 한 번 더 묻고, 그래도 모르면 버튼만. 응답 없음은 "나중에" |
| 주행 시작(D단·DRIVING) | 대기 모드·쉼표 화면·설정을 즉시 닫고 주행 화면 |
| 경유지 추가 실패 | "경로에 넣지 못했어요. 내비에서 직접 골라 주세요" |
