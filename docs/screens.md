# 화면 모음 (Pleos Connect 에뮬레이터, 2026-11-01~03 · UI 동결 11/8)

README에서 옮긴 전체 화면 표다. 주행 장면은 **실제 분할 창**(왼쪽 같이가개 1/3 + 오른쪽 Pleos 기본 내비)으로 찍었다. 기본 내비가 강릉씨티호텔로 안내 중이고, 다음 회전 카드("좌회전 80m · 기업지원허브 방면")는 NaviHelper `onTBTInfo` 실제 값이다(내비 화면과 같다). 주행·예보·쉼표·대기·리포트 숫자는 **시뮬레이터 실제 엔진 값**(앱 바에 "시연 데이터"), 프로필·장소·병원 목록·온보딩·주행 자극 보기는 발표 모드 장면. 앱 영역만 잘랐다(왼쪽 차량 패널·아래 공조 바는 Pleos 시스템 UI). 캡처 방법은 [recording.md](recording.md)·`python scripts/ui_capture.py`.

| 출발 전 (전체 창) | 주행 · 기본 내비 옆 1/3 | 굽은 길 예보 (숫자 없음) |
| --- | --- | --- |
| ![출발 전 체크 화면](screens/v8/01-depart.png) | ![기본 내비 옆 분할 창 주행 화면](screens/v8/02-drive-nav.png) | ![굽은 길 예보 화면](screens/v8/03-forecast-nav.png) |
| **쉼표 추천 (1/3 창, 음성 실패 시 버튼)** | **콩이가 이상해요 (음성 신고)** | **병원 가는 길 (사전 접수 보냄)** |
| ![쉼표 추천 화면](screens/v8/04-suggest-third.png) | ![콩이가 이상해요 화면](screens/v8/07-distress-nav.png) | ![병원 가는 길 화면](screens/v8/15-going-vet-nav.png) |
| **쉼표 (정차, 긴급 도움 버튼)** | **잠깐 대기 · 창문 너머 한눈에 (QR·제한 시간은 설정 안)** | **동물병원 (가장 가까운 곳 먼저 · 시연용 제휴 사전 접수)** |
| ![휴게소 쉼표 화면](screens/v8/05-rest.png) | ![잠깐 대기 화면](screens/v8/06-wait.png) | ![동물병원 화면](screens/v8/08-vets.png) |
| **실제 병원 ([전화] = 번호 크게 + tel: QR · 도착 예정 링크)** | **병원 접수함 (폰, 시연용)** | **도착 체크** |
| ![실제 병원 전화 화면](screens/v8/08c-vets-call.png) | ![병원 접수함 웹 화면](screens/v8/08b-clinic-inbox.png) | ![도착 체크 화면](screens/v8/09-arrival.png) |
| **도착 후 기록 리포트** | **리포트 (점프 뒤 실시간 신고 유지 · 보정 뒤에도 최고 55)** | **주행 자극 보기 (정차 중)** |
| ![도착 후 기록 리포트 화면](screens/v8/10-report.png) | ![보정 뒤 리포트 화면](screens/v8/10b-report-calibrated.png) | ![주행 자극 보기 화면](screens/v8/13-stimulus.png) |
| **반려견 프로필** | **온보딩** | **고갯길 비교 (발표 모드)** |
| ![반려견 프로필 화면](screens/v8/11-pet.png) | ![온보딩 화면](screens/v8/11-onboarding.png) | ![고갯길 비교 화면](screens/v8/12-compare.png) |
| **긴급 도움 · 위험 물질 섭취 (정차)** | **긴급 도움 (1/3 창, 카드 먼저)** | **동물병원 (다크)** |
| ![긴급 도움 화면](screens/v8/16-emergency.png) | ![1/3 창 긴급 도움 화면](screens/v8/16b-emergency-third.png) | ![다크 모드 동물병원 화면](screens/v8/16c-vets-dark.png) |

QR 주소의 `http://192.168.0.10:8000`은 캡처용 예시 주소다(`--es shareBase`로 노트북의 같은 Wi-Fi 주소를 넣는다). 이전 캡처는 `docs/screens/v6/`(재정비 전)·`docs/screens/v5/`(분할 창 첫 확인)에 이력으로 남겼다. 스토어 스크린샷 8장("시연 데이터" 표시 없이)은 [store/screenshots/](store/screenshots/).
