# 테스크 체크리스트 — 같이가개 (반려견과 차로 가는 모든 순간)

**목표:** 11/14(토) 접수양식 + GitHub(소스·APK) + 시연 영상 제출 → 12/3~4 PT 시연. 2026-10-29 주제 전환(쉼표 → 같이가개, `_private/PIVOT_PROMPT.md`).
담당: **(나)** 직접 해야 하는 것 · **(에이전트)** 코딩 에이전트.
쉼표 때 끝낸 일(M0·M1, 엔진·서버·신호·발표 모드)은 git 이력과 [docs/PROGRESS.md](docs/PROGRESS.md)에 있다.

## 0. 정리
- [x] (에이전트) CONCEPT 읽고 재사용/수정/삭제 표 → [docs/plan/REUSE_MAP.md](docs/plan/REUSE_MAP.md)
- [x] (에이전트) Pleos SDK aar javap 재확인 → [docs/plan/sdk_check.md](docs/plan/sdk_check.md)
- [x] (에이전트) 뺀 기능 코드 제거(라디오·원정대·"아직 멀었어?"·파워냅·콘텐츠 API·content/), 공통 부품은 남김
- [x] (에이전트) 루트 문서 7개 새 주제로 갱신
- [x] (에이전트) 앱 표시 이름 "같이가개" (applicationId·패키지 유지)

## 1. 도메인·엔진 (테스트 먼저)
- [x] (에이전트) PetProfile(core:model) + Room `pet.db` 저장(서버 전송 안 함)
- [x] (에이전트) `engine:motion` MotionComfortEngine (F1-01~F1-10), 상수·근거 [docs/plan/motion_model.md](docs/plan/motion_model.md) — 대역 하한 0.02Hz 결정
- [x] (에이전트) PetRestPlanner: 주행 자극 누적·반려견 시설 가중치 (F2-01~F2-04·F2-06)
- [x] (에이전트) WaitMode (F3-01~F3-06)
- [x] (에이전트) BoardingCheck (F6-01~F6-04)
- [x] (에이전트) PetTripLog 패턴(주행 자극이 보통 이상이 된 시각)·TripRecorder 주행 자극 추이 (F7-01~F7-03)
- [x] (에이전트) TripBrain·TripCoordinator에 묶기 (예보 음성·대기·도착 체크)
- [x] (에이전트) 여행 기록 저장(PetStore.saveTrip) — 화면은 2026-10-31부터 도착 후 기록 리포트

## 2. 서버
- [x] (에이전트) 마이그레이션 0003: pet_places·vet_clinics·wait_sessions — PG 테스트 3개 통과(10/29 저녁)
- [x] (에이전트) TourAPI 수집기 + sample_pet_places.json + `/places/along-route`·`/places/near` (F4)
- [x] (에이전트) 동물병원 적재 스크립트(`server/data/raw/`) + sample_vets.json + `/vets/nearby` (F5)
- [x] (에이전트) 대기 공유 `/wait` + 폰 웹페이지 (F3-07·F3-08) + 앱 업로더 `WaitShare`
- [x] (에이전트) `/llm/phrases` `trip_summary` + 템플릿 (F7-04)
- [x] (나) 공공데이터포털 TourAPI 키 → `server/.env`의 `TOUR_API_KEY` (10/30 실제 수집 전환)
- [ ] (나) 동물병원 인허가 파일 내려받아 `server/data/raw/`에

## 3. 시연 장치
- [x] (에이전트) 장면 시나리오 depart/highway/rest_stop/wait/mountain_road/arrive + `scripts/vhal.ps1` 장면 (user 빌드라 VHAL 주입 대신 앱 시뮬레이터 점프)
- [x] (에이전트) mountain_road 시연 GPX → OSM 대관령 옛길(ODbL 표기) + 비교 실험 55 → 30 → docs/plan/demo_compare.md + CSV (F1-07)
- [x] (에이전트) 실차 앞 구간 형상 `TbtRouteShape`(회전 안내 + 진행 방위) + GPX와 비교 → docs/plan/shape_compare.md (F1-11)
- [x] (에이전트) 예보 구간: 짧은 굽이 하나는 버리고 30초 안 간격은 묶기 (F1-12)
- [x] (에이전트) 대기 폰 페이지 같은 Wi-Fi 연결 순서 → docs/demo_script.md
- [x] (에이전트) 발표 모드 패널: 장면 버튼·비교 결과

## 4. 화면 (디자인 v5 — docs/design/v5/STYLE_V5.md)
- [x] (에이전트) docs/ui_contract.md 새 주제로
- [x] (에이전트) 화면 10종 기본 레이아웃(UiState만 보고 그림) — 에뮬레이터 캡처 artifacts/pet/
- [x] (에이전트) 출발 체크 "오늘의 쉼표" = 주행 추천과 같은 플래너(`PetTripPreview`, F2-08)
- [x] (에이전트) 장면 점프 뒤 쉼표 화면 휴게소 이름 (DM-04)
- [x] (에이전트) P1: 도착 10분 전 산책 코스 음성 제안(F4-03), 발표 장면 "도착 전 산책"
- [x] (에이전트) v5: 시안 자르기·STYLE_V5(토큰·컴포넌트·치수·분할 창), 옛 문서 폐기 표시
- [x] (에이전트) v5: 토큰(라이트·다크, 배율)·부품·일러스트·화면 12종·온보딩 3단계·앱 아이콘, 검증 루프(docs/ui_review.md v5)
- [x] (에이전트) v5: 폰 대기 페이지(6번)·"돌아가는 중", 동물병원 도착 예정 공유(`/eta`)
- [x] (에이전트) 실제 데이터: TourAPI KorPetTourService2(강원 69곳·사진·동반 조건), 동물병원 인허가 파일(5,063곳)
- [x] (나) 디자이너 그림 출처 → 사용자 제작(생성형 AI), 릴리스 포함 (도구명은 【도구명】 채우기)
- [x] (에이전트) 1/2·1/3 실제 창(`am task resize`) 캡처
- [x] (에이전트) 시연 리허설 자동 실행(scripts/rehearsal.py, 118초), 녹화 순서 docs/recording.md
- [ ] (나) TourAPI 키 재발급 → server/.env 교체

## 4-1. 재정비 (2026-10-31, docs/states.md · docs/wording.md)
- [x] (에이전트) 엔진 값 이름 "주행 자극"(낮음 < 40 ≤ 보통 < 70 ≤ 높음), 반려견은 "콩이 관찰 상태"(음성 신고만, 없으면 "관찰 기록 없음")
- [x] (에이전트) 주행 중 숫자·그래프 없이 예보 한 줄, 숫자는 정차 중 "주행 자극 보기"·리포트·발표 비교만
- [x] (에이전트) 관찰 체크 칸·"오늘 멀미했어요?" 질문·주행 화면 도로 그림 삭제, 개인 보정은 음성 신고에서 자동
- [x] (에이전트) 길 안내는 Pleos 기본 내비 + 분할 창 다음 회전 카드(NaviHelper `onTBTInfo` 첫 항목)
- [x] (에이전트) 병원 사전 접수는 시연용 제휴 동물병원만 + 전달 동의, 실제 병원은 전화·도착 예정 링크
- [x] (에이전트) 이동 기록 화면 → 도착 후 기록 리포트(시간순 타임라인, 위치 없이 거리만)
- [ ] (에이전트) v6 화면 캡처 다시 찍기(docs/screens/v6는 재정비 전)

## 5. 마무리
- [x] (에이전트) README 심사 대응표·실행 방법·공공데이터 출처
- [x] (에이전트) docs/demo_script.md (CONCEPT 7장 3분)
- [x] (에이전트) docs/qa_report.md, 실명·키 노출 검사
- [ ] (나) 커밋(대기열: docs/PROGRESS.md), push
- [ ] (나) 시연 영상, 접수양식

## 계속 막힌 것 (docs/BLOCKED.md)
- 에뮬레이터 VHAL 값 주입 불가(user 빌드)
- NaviHelper 회전 안내·방위 주기 실기 확인
