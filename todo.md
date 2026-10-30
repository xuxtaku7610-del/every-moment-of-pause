# 테스크 체크리스트 — 같이가개 (반려견과 차로 가는 모든 순간)

**목표:** 11/14(토) 접수양식 + GitHub(소스·APK) + 시연 영상 제출 → 12/3~4 PT 시연. 2026-10-29 주제 전환(쉼표 → 같이가개, `_private/PIVOT_PROMPT.md`).
담당: **(나)** 직접 해야 하는 것 · **(CC)** Claude Code.
쉼표 때 끝낸 일(M0·M1, 엔진·서버·신호·발표 모드)은 git 이력과 [docs/PROGRESS.md](docs/PROGRESS.md)에 있다.

## 0. 정리
- [x] (CC) CONCEPT 읽고 재사용/수정/삭제 표 → [docs/plan/REUSE_MAP.md](docs/plan/REUSE_MAP.md)
- [x] (CC) Pleos SDK aar javap 재확인 → [docs/plan/sdk_check.md](docs/plan/sdk_check.md)
- [x] (CC) 뺀 기능 코드 제거(라디오·원정대·"아직 멀었어?"·파워냅·콘텐츠 API·content/), 공통 부품은 남김
- [x] (CC) 루트 문서 7개 새 주제로 갱신
- [x] (CC) 앱 표시 이름 "같이가개" (applicationId·패키지 유지)

## 1. 도메인·엔진 (테스트 먼저)
- [x] (CC) PetProfile(core:model) + Room `pet.db` 저장(서버 전송 안 함)
- [x] (CC) `engine:motion` MotionComfortEngine (F1-01~F1-10), 상수·근거 [docs/plan/motion_model.md](docs/plan/motion_model.md) — 대역 하한 0.02Hz 결정
- [x] (CC) PetRestPlanner: 멀미 누적·반려견 시설 가중치 (F2-01~F2-04·F2-06)
- [x] (CC) WaitMode (F3-01~F3-06)
- [x] (CC) BoardingCheck (F6-01~F6-04)
- [x] (CC) PetTripLog 패턴(한계 시간)·TripRecorder 멀미 추이 (F7-01~F7-03)
- [x] (CC) TripBrain·TripCoordinator에 묶기 (예보 음성·대기·도착 체크)
- [x] (CC) 이동 기록 저장(PetStore.saveTrip)과 한계 시간 표시

## 2. 서버
- [x] (CC) 마이그레이션 0003: pet_places·vet_clinics·wait_sessions — PG 테스트 3개 통과(10/29 저녁)
- [x] (CC) TourAPI 수집기 + sample_pet_places.json + `/places/along-route`·`/places/near` (F4)
- [x] (CC) 동물병원 적재 스크립트(`server/data/raw/`) + sample_vets.json + `/vets/nearby` (F5)
- [x] (CC) 대기 공유 `/wait` + 폰 웹페이지 (F3-07·F3-08) + 앱 업로더 `WaitShare`
- [x] (CC) `/llm/phrases` `trip_summary` + 템플릿 (F7-04)
- [x] (나) 공공데이터포털 TourAPI 키 → `server/.env`의 `TOUR_API_KEY` (10/30 실제 수집 전환)
- [ ] (나) 동물병원 인허가 파일 내려받아 `server/data/raw/`에

## 3. 시연 장치
- [x] (CC) 장면 시나리오 depart/highway/rest_stop/wait/mountain_road/arrive + `scripts/vhal.ps1` 장면 (user 빌드라 VHAL 주입 대신 앱 시뮬레이터 점프)
- [x] (CC) mountain_road 시연 GPX → OSM 대관령 옛길(ODbL 표기) + 비교 실험 55 → 30 → docs/plan/demo_compare.md + CSV (F1-07)
- [x] (CC) 실차 앞 구간 형상 `TbtRouteShape`(회전 안내 + 진행 방위) + GPX와 비교 → docs/plan/shape_compare.md (F1-11)
- [x] (CC) 예보 구간: 짧은 굽이 하나는 버리고 30초 안 간격은 묶기 (F1-12)
- [x] (CC) 대기 폰 페이지 같은 Wi-Fi 연결 순서 → docs/demo_script.md
- [x] (CC) 발표 모드 패널: 장면 버튼·비교 결과

## 4. 화면 (디자인 v5 — docs/design/v5/STYLE_V5.md)
- [x] (CC) docs/ui_contract.md 새 주제로
- [x] (CC) 화면 10종 기본 레이아웃(UiState만 보고 그림) — 에뮬레이터 캡처 artifacts/pet/
- [x] (CC) 출발 체크 "오늘의 쉼표" = 주행 추천과 같은 플래너(`PetTripPreview`, F2-08)
- [x] (CC) 장면 점프 뒤 쉼표 화면 휴게소 이름 (DM-04)
- [x] (CC) P1: 도착 10분 전 산책 코스 음성 제안(F4-03), 발표 장면 "도착 전 산책"
- [x] (CC) v5: 시안 자르기·STYLE_V5(토큰·컴포넌트·치수·분할 창), 옛 문서 폐기 표시
- [x] (CC) v5: 토큰(라이트·다크, 배율)·부품·일러스트·화면 12종·온보딩 3단계·앱 아이콘, 검증 루프(docs/ui_review.md v5)
- [x] (CC) v5: 폰 대기 페이지(6번)·"돌아가는 중", 동물병원 도착 예정 공유(`/eta`)
- [x] (CC) 실제 데이터: TourAPI KorPetTourService2(강원 69곳·사진·동반 조건), 동물병원 인허가 파일(5,063곳)
- [x] (나) 디자이너 그림 출처 → 사용자 제작(생성형 AI), 릴리스 포함 (도구명은 【도구명】 채우기)
- [x] (CC) 1/2·1/3 실제 창(`am task resize`) 캡처
- [x] (CC) 시연 리허설 자동 실행(scripts/rehearsal.py, 118초), 녹화 순서 docs/recording.md
- [ ] (나) TourAPI 키 재발급 → server/.env 교체

## 5. 마무리
- [x] (CC) README 심사 대응표·실행 방법·공공데이터 출처
- [x] (CC) docs/demo_script.md (CONCEPT 7장 3분)
- [x] (CC) docs/qa_report.md, 실명·키 노출 검사
- [ ] (나) 커밋(대기열: docs/PROGRESS.md), push
- [ ] (나) 시연 영상, 접수양식

## 계속 막힌 것 (docs/BLOCKED.md)
- 에뮬레이터 VHAL 값 주입 불가(user 빌드)
- NaviHelper 회전 안내·방위 주기 실기 확인
