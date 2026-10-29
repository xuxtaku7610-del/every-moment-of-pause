package com.emop.pause.uistate

/**
 * 미리보기·발표 모드 "장면 고정"용 상태. 이름(콩이)과 장소는 시연 시나리오(pet-trip)의 예시이고,
 * 실제 화면은 사용자가 입력한 이름과 엔진 값을 쓴다.
 */
object Fixtures {
    private val motionCaution = MotionUi(45, MotionTone.CAUTION, "콩이 멀미 주의 45")
    private val motionCalm = MotionUi(12, MotionTone.CALM, "콩이 컨디션 편안 12")

    val drive = DriveUi(
        meta = "강릉까지 2시간 10분",
        motion = motionCalm,
        content = DriveContent.Calm("다음 쉼표는 32분 뒤 횡성휴게소예요."),
    )

    val forecast = drive.copy(
        meta = "강릉까지 25분",
        motion = motionCaution,
        content = DriveContent.Forecast("3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요."),
    )

    val suggest = drive.copy(
        motion = motionCaution,
        content = DriveContent.Suggest(18, "횡성휴게소", listOf("콩이 멀미가 쌓였어요", "반려견 놀이터")),
    )

    val urgent = drive.copy(content = DriveContent.Suggest(6, "둔내 졸음쉼터", listOf("졸음 위험"), urgent = true))

    val restricted = suggest.copy(restricted = true)

    val voiceFail = suggest.copy(voice = VoiceSheetState.FAILED)

    val depart = DepartUi(
        meta = "강릉까지 예상 3시간",
        pet = PetSummaryUi("콩이", "소형 · 9살 · 멀미 성향 높음 · 배변 90분"),
        checks = listOf(
            CheckUi(CheckKind.HARNESS, "하네스·켄넬", true),
            CheckUi(CheckKind.WATER, "물", true),
            CheckUi(CheckKind.POTTY_BAGS, "배변봉투", false),
        ),
        ready = false,
        plan = listOf(PlanStopUi("횡성휴게소", "반려견 놀이터, 급속충전", "1시간 25분 뒤")),
        rationale = "콩이 배변 간격(90분)에 맞춰 쉬어 가요.",
        limitLine = "콩이는 약 1시간 10분이 한계예요. (기록 2번 기준)",
    )

    val rest = RestUi(
        meta = "횡성휴게소에서 쉬는 중",
        lead = "15분 쉬어 가요",
        minutes = 15,
        restChoice = 15,
        activities = listOf(
            ActivityUi(ActivityKind.PET_WALK, "콩이", "산책 10분", "반려견 놀이터 표지판을 따라가 보세요.", true),
            ActivityUi(ActivityKind.PET_CARE, "콩이", "물·배변 5분", "물은 조금씩, 배변봉투를 챙겨 주세요.", false),
            ActivityUi(ActivityKind.STRETCH, "운전자", "스트레칭 5분", "목, 어깨, 허리 순서로 천천히 풀어요.", false),
        ),
        motion = MotionUi(22, MotionTone.CALM, "콩이 컨디션 편안 22"),
        waitLimitMin = 10,
        battery = "배터리 57%",
    )

    val wait = WaitUi(
        headline = "주인이 곧 돌아와요",
        elapsed = "3분째",
        cabin = "실내 24℃",
        battery = "배터리 57%",
        limitMin = 10,
        overdue = false,
        share = ShareUi("http://192.168.0.10:8000/wait/k3J9x2", true),
    )

    val waitOverdue = wait.copy(elapsed = "11분째", overdue = true, cabin = "실내 온도 정보 없음")

    val places = PlacesUi(
        title = "함께 가는 장소",
        items = listOf(
            PlaceUi("ps_1", "[샘플] 횡성군 반려견 동반 카페", "카페", "경로에서 0.9km · 140km 지점", "소형견 동반 가능, 실내 이동장 권장 (샘플)", true),
            PlaceUi("ps_2", "[샘플] 강릉 반려견 동반 산책로 1", "산책로", "목적지에서 2.3km", "목줄 착용, 배변봉투 지참 (샘플)", true),
        ),
        note = "샘플 데이터예요. 관광공사 반려동물 동반여행 키를 넣으면 실제 장소로 바뀌어요.",
    )

    val vets = VetsUi(
        items = listOf(VetUi("vs_1", "[샘플] 강릉시 동물병원 A", "1.4km", "강원특별자치도 강릉시 (샘플)", null, true)),
        note = "샘플 데이터예요. 전화번호는 실제 데이터에서만 보여요.",
    )

    val arrival = ArrivalUi("콩이 내렸나요?", strong = false, detail = "내린 뒤 확인을 눌러 주세요.")

    val tripLog = TripLogUi(
        meta = "10월 31일, 서울에서 강릉까지",
        summary = "이번 이동은 3시간, 휴식 1번, 멀미 주의 예보 최고 46이었어요.",
        durationMin = 180,
        stops = 1,
        motionPeak = 46,
        motionSeries = listOf(0, 3, 6, 9, 12, 14, 15, 9, 7, 10, 13, 15, 17, 30, 41, 46, 44),
        limitLine = "콩이는 약 1시간 10분이 한계예요. (기록 2번 기준)",
        vetSummary = listOf("콩이 이동 기록", "이동 시간 3시간 · 휴식 1번", "멀미 주의 예보 최고 46 (0~100, 진단 아님)"),
        symptoms = listOf(SymptomUi("침 흘림", true), SymptomUi("헐떡임", false), SymptomUi("구토", false), SymptomUi("괜찮았어요", false)),
    )

    val pet = PetEditUi("콩이", SizeUi.SMALL, 9, TendencyUi.HIGH, 90, true, emptyList(), firstRun = false)

    val settings = SettingsUi(
        signals = listOf(
            SignalRowUi("주행 제한 상태", true, "CarUxRestrictions"),
            SignalRowUi("속도와 기어", true, "AAOS 차량 속성 · Pleos Vehicle SDK"),
            SignalRowUi("방위(멀미 예보)", false),
        ),
        version = "1.0.0",
    )

    fun screen(scene: Scene): Screen = when (scene) {
        Scene.DEPART -> Screen.Depart(depart)
        Scene.DRIVE -> Screen.Drive(drive)
        Scene.FORECAST -> Screen.Drive(forecast)
        Scene.SUGGEST -> Screen.Drive(suggest)
        Scene.URGENT -> Screen.Drive(urgent)
        Scene.RESTRICTED -> Screen.Drive(restricted)
        Scene.REST -> Screen.Rest(rest)
        Scene.WAIT -> Screen.Wait(wait)
        Scene.WAIT_OVERDUE -> Screen.Wait(waitOverdue)
        Scene.PLACES -> Screen.Places(places)
        Scene.VETS -> Screen.Vets(vets)
        Scene.ARRIVAL -> Screen.Arrival(arrival)
        Scene.TRIPLOG -> Screen.TripLog(tripLog)
        Scene.PET -> Screen.PetProfile(pet)
        Scene.SETTINGS -> Screen.Settings(settings)
    }
}
