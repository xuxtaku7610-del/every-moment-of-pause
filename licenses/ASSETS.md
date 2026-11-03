# 그림·아이콘 출처 (v5)

저장소 전체 라이선스는 [../LICENSE](../LICENSE)(All rights reserved, 공모전 심사·열람 목적만). 아래 표의 자료는 그 예외로 각자의 라이선스를 따른다.

| 자산 | 위치 | 출처 · 라이선스 | 비고 |
| --- | --- | --- | --- |
| 선 아이콘 | `androidx.compose.material:material-icons-extended` (Rounded) | Google Material Symbols · Apache-2.0 | 의존성으로 포함 |
| 컬러 아이콘 10개 (`fe_*.xml`) | `app/src/main/res/drawable/` | Microsoft Fluent UI Emoji, Flat 스타일 · MIT ([FluentUI-Emoji-MIT.txt](FluentUI-Emoji-MIT.txt)) | github.com/microsoft/fluentui-emoji 의 SVG를 벡터 드로어블로 옮김(모양 변경 없음): Warning, Hot beverage, Thermometer, Check mark button, Microphone, Hospital, Deciduous tree, House, Sport utility vehicle, Dog face |
| 앱 아이콘 (`ic_launcher_*.xml`), 일러스트(`Illustrations.kt`), 폰 페이지 SVG | 앱·서버 코드 | 이 프로젝트에서 직접 그림 | 다른 회사 로고·캐릭터를 본뜨지 않은 단순 도형 |
| 장소 사진 | 실행 중 TourAPI `firstimage` URL | 한국관광공사 (공공누리, `cpyrhtDivCd` Type1·Type3) | 앱에 묶지 않고 불러와 보여 주며 "사진 한국관광공사" 표기. Type3는 변경 금지 → 자르기만(썸네일), 보정 없음 |
| 디자이너 그림 (`docs/design/v5/assets/*.png`: 반려견·도로·휴게소·병원·대기·온보딩 그림, `ic_*` 아이콘, 앱 아이콘 원본) | 앱 assets (디버그·릴리스) | **사용자가 생성형 AI 디자인 도구로 직접 제작, 외부 저작물 아님.** 도구: 【도구명】 | 결정 2026-10-30: 릴리스 사용·공개 저장소 게시 허용. 실제 장소 사진이 아닌 그림 |
| QR 코드 | `com.google.zxing:core` 3.5.3 (인코더만, 2026-11-01) | ZXing · Apache-2.0 | 의존성으로 포함, 화면에서 직접 그림 |
| 글꼴 | `res/font/pretendard_*` | Pretendard · SIL OFL 1.1 ([Pretendard-OFL.txt](Pretendard-OFL.txt)) | |
| 고갯길 시연 경로 | `scenarios/geo/mountain_road.gpx` | © OpenStreetMap contributors · ODbL 1.0 | |
