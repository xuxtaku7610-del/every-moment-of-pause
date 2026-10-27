# 흐름도 — 쉼표 (멈춤의 모든 순간)

## 1. 핵심 유저 저니 (전체)

페르소나: 아빠 운전자(38), 아내·5세 아이·반려견과 서울→부산 5시간, 전기차.

```mermaid
flowchart TD
    A["출발 준비 · 정차"] --> B["동승자 프리셋 확인<br/>아이 5세 · 반려견 · 아내 교대 가능"]
    B --> C["수면 한마디<br/>어젯밤 몇 시간 주무셨어요?"]
    C --> D["휴식 계획 초안 표시<br/>예상 휴식 2회"]
    D --> E["주행 시작"]
    E --> F["1초마다 차량 신호 수집"]
    F --> G["피로 지수 · 니즈 타이머 갱신"]
    G --> H{"휴식이 필요한가?"}
    H -->|"아니오"| K{"뒷좌석 질문?"}
    K -->|"아직 멀었어?"| L["도착 맞춤 답변<br/>라디오 2편 남았어"]
    L --> F
    K -->|"없음"| F
    H -->|"예"| I{"피로 위험 높음?"}
    I -->|"예"| J1["최근접 졸음쉼터 우선"]
    I -->|"아니오"| J2["가족 니즈 맞춤 휴게소"]
    J1 --> M["음성 제안 · 이유 1문장"]
    J2 --> M
    M --> N{"운전자 응답"}
    N -->|"나중에"| O["10분 뒤 재평가"]
    O --> F
    N -->|"응"| P["경유지 추가"]
    P --> Q["휴게소 도착 · P단"]
    Q --> R["멈춤 큐레이터<br/>충전 27분 남음"]
    R --> S["운전자: 20분 파워냅<br/>아이: 원정대 미션<br/>반려견: 산책 타이머"]
    S --> T["충전 2분 전 기상 알림"]
    T --> U["출발 전 컨디션 체크"]
    U --> V{"목적지 도착?"}
    V -->|"아니오"| E
    V -->|"예"| W["여행 리포트"]
```

## 2. 시퀀스 — 휴게소 추천과 경유지 추가

```mermaid
sequenceDiagram
    autonumber
    participant V as Vehicle SDK
    participant N as NaviHelper
    participant E as 엔진(피로·니즈)
    participant P as 휴게소 플래너
    participant DB as Room 캐시
    participant S as 서버
    participant VO as 음성 에이전트
    actor D as 운전자

    Note over N,S: 출발 시 1회
    N->>P: 경로 좌표 · 총 거리
    P->>S: POST /reststops/along-route
    S-->>P: 경로상 휴게소 목록
    P->>DB: 캐시 저장

    loop 10초마다
        V->>E: SignalSample
        E->>P: 피로 점수 · 니즈 긴급도
    end

    P->>DB: 앞쪽 후보 조회
    P->>P: 후보 점수화
    P->>S: POST /llm/phrases 추천 정보
    alt 2.5초 이내 응답
        S-->>P: 제안 문장
    else 타임아웃 또는 오류
        P->>P: 템플릿 문장 사용
    end
    P->>VO: 제안 문장
    VO->>D: 18분 뒤 OO휴게소, 키즈존과 급속충전기가 있어요. 들를까요?
    D->>VO: 응
    VO->>N: 경유지 추가 요청
    N-->>VO: 경유지 추가 완료
    VO->>D: 경로에 추가했어요
    P->>DB: 추천 수락 이벤트 기록
```

## 3. 시퀀스 — "아직 멀었어?" 응답기

```mermaid
sequenceDiagram
    autonumber
    actor K as 아이
    participant VO as 음성 에이전트
    participant N as NaviHelper
    participant R as 라디오 플레이어
    participant S as 서버

    K->>VO: 아빠 아직 멀었어?
    VO->>VO: 의도 분류 · 남은 시간 질문
    VO->>N: 다음 정차지 ETA 조회
    N-->>VO: 18분
    VO->>R: 남은 에피소드 수 계산
    R-->>VO: 속담 라디오 2편
    VO->>S: POST /llm/kid-answer
    alt 성공
        S-->>VO: 속담 라디오 두 편만 들으면 키즈존 휴게소야!
    else 실패
        VO->>VO: 템플릿 · 라디오 N편 남았어
    end
    VO->>K: 음성 답변
```

## 4. 시퀀스 — 충전 대기 멈춤 큐레이터

```mermaid
sequenceDiagram
    autonumber
    participant V as Vehicle SDK
    participant C as 멈춤 큐레이터
    participant UI as 멈춤 화면
    participant VO as 음성 에이전트
    actor D as 운전자

    V->>C: 기어 P · 충전 시작 · 배터리 34%
    C->>C: 충전 완료까지 남은 시간 계산 27분
    C->>C: 피로 점수 · 동승자 · 남은 시간으로 활동 편성
    C->>UI: 추천 카드 3개
    C->>VO: 충전 끝까지 27분, 20분 파워냅 할까요?
    D->>VO: 응
    VO->>UI: 파워냅 모드 시작
    UI->>UI: 벽난로 화면 · 장작 소리 · 조도 낮춤
    loop 1분마다
        V->>C: 충전 상태 · 배터리
        C->>C: 남은 시간 재계산
    end
    C->>VO: 기상 알림 · 충전 2분 전
    VO->>D: 충전이 거의 끝났어요. 기분 어떠세요?
    D->>VO: 괜찮아
    C->>UI: 출발 준비 화면
```

## 5. 데이터 흐름

```mermaid
flowchart LR
    subgraph SRC["데이터 원천"]
        S1["Vehicle SDK<br/>1초 신호"]
        S2["NaviHelper<br/>경로 · ETA"]
        S3["사용자 입력<br/>프리셋 · 수면 · 음성"]
        S4["도로공사 API<br/>휴게소 · 편의시설"]
        S5["검수된 콘텐츠<br/>라디오 · 미션"]
    end
    subgraph DEVICE["기기 · 저장 안 함 또는 기기 저장"]
        M1["신호 링버퍼<br/>최근 30분 메모리"]
        M2["Room<br/>프리셋 · 캐시 · 여행"]
        M3["엔진 결과<br/>점수 · 추천"]
    end
    subgraph SERVER["서버"]
        D1[("rest_stops")]
        D2[("content")]
        D3[("trips · trip_events")]
    end
    S1 --> M1
    M1 --> M3
    S2 --> M3
    S3 --> M2
    M2 --> M3
    S4 --> D1
    S5 --> D2
    D1 -->|"출발 시 내려받기"| M2
    D2 -->|"출발 시 내려받기"| M2
    M3 -->|"이벤트만 업로드"| D3
    D3 -->|"리포트 집계"| M2
```

| 데이터 | 생성 위치 | 저장 위치 | 서버 전송 |
| --- | --- | --- | --- |
| 1초 차량 신호 | Vehicle SDK | 메모리 링버퍼 30분 | 안 함 |
| 동승자 프리셋 · 수면 시간 | 사용자 | Room | 안 함 |
| 경로 좌표 | NaviHelper | 메모리 | 휴게소 조회 시에만, 저장 안 함 |
| 휴게소 · 편의시설 | 도로공사 API | PostgreSQL → Room 캐시 | 내려받기 |
| 라디오 · 미션 | 검수 JSON | PostgreSQL → Room 캐시 | 내려받기 |
| 여행 이벤트 | 엔진 | Room → PostgreSQL | 일괄 업로드 (휴게소 ID · 시각 · 종류) |

## 6. 예외 흐름 요약

```mermaid
flowchart TD
    X["추천 시점"] --> A{"서버 연결?"}
    A -->|"예"| B["최신 휴게소 데이터"]
    A -->|"아니오"| C{"캐시 있음?"}
    C -->|"예"| D["캐시로 추천"]
    C -->|"아니오"| E["내장 기본 데이터<br/>주요 고속도로"]
    B --> F{"후보 있음?"}
    D --> F
    E --> F
    F -->|"예"| G["추천 생성"]
    F -->|"아니오"| H["시간 기반 일반 휴식 안내"]
    G --> I{"주행 제한 강함?"}
    I -->|"예"| J["음성만 · 화면 카드 최소"]
    I -->|"아니오"| K["음성 + 카드"]
```
