package com.emop.pause.uistate

/**
 * 미리보기·발표 모드 "장면 고정"용 상태. 이름(콩이)과 장소는 시연 시나리오(pet-trip)의 예시이고,
 * 실제 화면은 사용자가 입력한 이름과 엔진 값을 쓴다. 숫자는 시안 v5(docs/design/v5)와 맞췄다.
 */
object Fixtures {
    private val motionCaution = MotionUi(45, MotionTone.CAUTION, "콩이 멀미 주의 45")
    private val motionCalm = MotionUi(24, MotionTone.CALM, "콩이 컨디션 편안 24")

    /** 시연 고갯길처럼 좌우로 굽는 앞 1km (곡률 1/m, 50m 간격). */
    private val bends = List(21) { k -> (if (k < 6) 0.0 else kotlin.math.sin((k - 6) / 3.0) / 120).toFloat() }

    val drive = DriveUi(
        meta = "강릉까지 2시간 10분",
        motion = motionCalm,
        content = DriveContent.Calm("다음 쉼표는 52분 뒤 횡성휴게소예요."),
        pet = "콩이",
        sinceRest = "마지막 휴식 후 52분",
        nextStop = "약 72km",
        roadAhead = List(21) { k -> (k / 20.0 / 900).toFloat() },
    )

    val forecast = drive.copy(
        meta = "강릉까지 25분",
        motion = MotionUi(38, MotionTone.CALM, "콩이 컨디션 편안 38"),
        content = DriveContent.Forecast(
            "3km 뒤 굽은 길 8분, 콩이 멀미 주의 — 조금 일찍 감속해 주세요.",
            title = "3km 뒤 굽은 길 · 8분",
            coach = "콩이 멀미 주의 — 조금 일찍 감속해요",
            now = 38,
            peak = 71,
        ),
        roadAhead = bends,
    )

    val suggest = drive.copy(
        motion = motionCaution,
        content = DriveContent.Suggest(9, "횡성휴게소", listOf("굽은 길 전에 쉬어 가요", "반려견 놀이터"), distance = "8km", direction = "강릉 방향"),
    )

    val urgent = drive.copy(content = DriveContent.Suggest(6, "둔내 졸음쉼터", listOf("졸음 위험"), urgent = true, distance = "5km"))

    val restricted = suggest.copy(restricted = true)

    val voiceFail = suggest.copy(voice = VoiceSheetState.FAILED)

    val distress = drive.copy(
        meta = "강릉까지 1시간 50분",
        content = DriveContent.Distress("에어컨을 낮추고, 다음 안전한 곳에서 멈춰요", "둔내 졸음쉼터", 6),
        nextStop = null,
    )

    val restCare = RestUi(
        meta = "둔내 졸음쉼터에서 쉬는 중",
        lead = "15분 쉬어 가요",
        minutes = 15,
        restChoice = 15,
        activities = emptyList(),
        motion = MotionUi(22, MotionTone.CALM, "콩이 컨디션 편안 22"),
        waitLimitMin = 10,
        care = CareUi(
            "헐떡임 — 덥거나 여름",
            listOf(
                "그늘에 세우고 에어컨을 켠 채 창문을 조금 열어 주세요.",
                "물은 조금씩 자주 주세요. 한 번에 많이 마시게 하지 않아요.",
                "얼음물에 담그지 말고, 미지근한 물로 몸을 적신 뒤 바로 동물병원에 연락해요.",
            ),
            reviewed = false,
        ),
        distressChips = listOf("헐떡임", "구토", "침 흘림·하품", "낑낑·떨림", "축 처짐"),
    )

    val vetDrive = drive.copy(meta = "동물병원까지 15분", motion = motionCaution, content = DriveContent.Vet("강릉동물병원", "2.3km", 15), nextStop = null)

    val depart = DepartUi(
        meta = "강릉까지 예상 2시간 50분",
        pet = PetSummaryUi("콩이", "소형 · 9살 · 멀미 성향 높음 · 배변 90분"),
        checks = listOf(
            CheckUi(CheckKind.HARNESS, "하네스·켄넬", true),
            CheckUi(CheckKind.WATER, "물", true),
            CheckUi(CheckKind.POTTY_BAGS, "배변봉투", false),
        ),
        ready = false,
        plan = listOf(
            PlanStopUi("횡성휴게소", "반려견 놀이터, 급속충전", "1시간 25분 뒤"),
            PlanStopUi("강릉휴게소", "반려견 놀이터", "2시간 30분 뒤"),
        ),
        rationale = "콩이 배변 간격(90분)에 맞춰 쉬어 가요.",
        limitLine = "콩이는 약 1시간 10분이 한계예요. (기록 2번 기준)",
        route = "강릉까지",
        duration = "2시간 50분",
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
        elapsed = "4분째",
        cabin = "실내 24℃",
        battery = "배터리 57%",
        limitMin = 10,
        overdue = false,
        share = ShareUi("http://192.168.0.10:8000/wait/k3J9x2", true),
        batteryUsed = "1%",
    )

    val waitOverdue = wait.copy(elapsed = "11분째", overdue = true, cabin = "실내 27℃", heat = "실내 온도가 오르고 있어요 (24℃ → 27℃)", batteryUsed = null, battery = null)

    val places = PlacesUi(
        title = "함께 가는 장소",
        items = listOf(
            PlaceUi("ps_1", "[샘플] 강릉 반려견 동반 카페", "카페", "목적지에서 1.2km", "소형견 동반 가능 (샘플)", true, tags = listOf("일부구역 동반"), notes = listOf("소형견 동반 가능 (샘플)")),
            PlaceUi("ps_2", "[샘플] 강릉 반려견 동반 산책로 1", "산책로", "목적지에서 2.8km", "목줄 착용, 배변봉투 지참 (샘플)", true, tags = listOf("전구역 동반"), notes = listOf("목줄 착용, 배변봉투 지참 (샘플)")),
            PlaceUi("ps_3", "[샘플] 강릉 반려견 동반 식당", "식당", "목적지에서 3.1km", "테라스석 동반 가능 (샘플)", true, tags = listOf("일부구역 동반"), notes = listOf("테라스석 동반 가능 (샘플)")),
        ),
        note = "샘플 데이터예요. 서버에 관광공사 반려동물 동반여행 데이터를 넣으면 실제 장소로 바뀌어요.",
    )

    val vets = VetsUi(
        items = listOf(
            VetUi("vs_1", "[샘플] 강릉시 동물병원 A", "2.3km", "강원특별자치도 강릉시 (샘플)", null, true, minutes = 7),
            VetUi("vs_2", "[샘플] 강릉시 동물병원 B", "3.1km", "강원특별자치도 강릉시 (샘플)", null, true, minutes = 8),
        ),
        note = "샘플 데이터예요. 전화번호는 실제 인허가 데이터에서만 보여요.",
        memo = listOf("구토", "설사", "헐떡임", "축 처짐", "떨림", "기타").map { SymptomUi(it, it == "구토") },
        memoText = "구토 두 번, 물은 조금 마셨어요",
        memoInclude = true,
        tripLine = "이동 2시간 10분 · 멀미 주의 1회(최고 46)",
    )

    val arrival = ArrivalUi("콩이 내렸나요?", strong = false, detail = "내린 뒤 확인을 눌러 주세요.")

    val arrivalHot = ArrivalUi("콩이 내렸나요?", strong = true, detail = "차 안이 금방 더워져요. 꼭 확인해 주세요.", warning = "실내 28℃ — 더워지고 있어요", cabin = "실내 28℃")

    val tripLog = TripLogUi(
        meta = "10월 31일, 강릉까지",
        summary = "이번 이동은 2시간 58분, 휴식 2번, 멀미 주의 예보 최고 46이었어요.",
        durationMin = 178,
        stops = 2,
        motionPeak = 46,
        motionSeries = listOf(0, 3, 6, 9, 12, 14, 15, 9, 7, 10, 13, 15, 17, 30, 41, 46, 44, 30, 22),
        limitLine = "콩이는 1시간 10분쯤이 한계예요",
        vetSummary = listOf("콩이 이동 기록", "이동 시간 2시간 58분 · 휴식 2번", "멀미 주의 예보 최고 46 (0~100, 진단 아님)"),
        symptoms = listOf(SymptomUi("침 흘림", true), SymptomUi("헐떡임", false), SymptomUi("구토", false), SymptomUi("괜찮았어요", false)),
        warnings = 1,
        history = listOf(HistoryUi("10월 26일", "강릉 · 2시간 58분 · 멀미 주의 1번"), HistoryUi("10월 19일", "속초 · 2시간 16분 · 편안")),
    )

    val pet = PetEditUi("콩이", SizeUi.SMALL, 9, TendencyUi.HIGH, 90, true, emptyList(), firstRun = false)

    val settings = SettingsUi(
        signals = listOf(
            SignalRowUi("주행 제한 상태", true, "CarUxRestrictions"),
            SignalRowUi("속도와 기어", true, "AAOS 차량 속성 · Pleos Vehicle SDK"),
            SignalRowUi("방위(멀미 예보)", false),
        ),
        version = "1.0.0",
        shapeSource = "시연 경로 (OSM GPX)",
    )

    fun onboarding(step: Int) = OnboardingUi(
        step = step,
        pet = pet.copy(firstRun = true),
        signals = settings.signals,
        quiz = MotionQuiz.ui(if (step == 3) listOf(2, 1, 1) else List(3) { null }),
        result = if (step == 3) MotionQuiz.result(listOf(2, 1, 1)) else null,
    )

    fun screen(scene: Scene): Screen = when (scene) {
        Scene.DEPART -> Screen.Depart(depart)
        Scene.DRIVE -> Screen.Drive(drive)
        Scene.FORECAST -> Screen.Drive(forecast)
        Scene.SUGGEST -> Screen.Drive(suggest)
        Scene.URGENT -> Screen.Drive(urgent)
        Scene.RESTRICTED -> Screen.Drive(restricted)
        Scene.VET_DRIVE -> Screen.Drive(vetDrive)
        Scene.DISTRESS -> Screen.Drive(distress)
        Scene.REST_CARE -> Screen.Rest(restCare)
        Scene.REST -> Screen.Rest(rest)
        Scene.WAIT -> Screen.Wait(wait)
        Scene.WAIT_OVERDUE -> Screen.Wait(waitOverdue)
        Scene.PLACES -> Screen.Places(places)
        Scene.VETS -> Screen.Vets(vets)
        Scene.ARRIVAL -> Screen.Arrival(arrival)
        Scene.ARRIVAL_HOT -> Screen.Arrival(arrivalHot)
        Scene.TRIPLOG -> Screen.TripLog(tripLog)
        Scene.PET -> Screen.PetProfile(pet)
        Scene.SETTINGS -> Screen.Settings(settings)
        Scene.ONBOARD_1 -> Screen.Onboarding(onboarding(1))
        Scene.ONBOARD_2 -> Screen.Onboarding(onboarding(2))
        Scene.ONBOARD_3 -> Screen.Onboarding(onboarding(3))
    }
}
