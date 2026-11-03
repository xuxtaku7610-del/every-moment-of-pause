package com.emop.pause.uistate

/**
 * 미리보기·발표 모드 "장면 고정"용 상태. 이름(콩이)과 장소는 시연 시나리오(pet-trip)의 예시이고,
 * 실제 화면은 사용자가 입력한 이름과 엔진 값을 쓴다. 장면 고정은 발표 모드에서만 열리고 화면에 "시연 데이터"가 붙는다.
 */
object Fixtures {
    private val observed = ObservationUi("신고 1건 · 헐떡임(출발 39분 뒤)", 1)

    val drive = DriveUi(
        meta = "강릉까지 2시간 10분",
        content = DriveContent.Calm("다음 쉼표는 52분 뒤 횡성휴게소예요."),
        pet = "콩이",
        sinceRest = "마지막 휴식 후 52분",
        nextStop = "약 72km",
        nextStopName = "횡성휴게소",
        nextStopMin = 46,
        nextStopFacilities = listOf("반려견 놀이터"),
        guide = GuideUi(GuideIconUi.KEEP_RIGHT, "1.2km", "강릉·속초 방면"),
        remain = "143km · 2시간 10분",
        arrive = "오전 11:40",
        voiceHint = "“콩이 토했어”처럼 말하면 기록해요",
    )

    val forecast = drive.copy(
        meta = "강릉까지 25분",
        content = DriveContent.Forecast("3km 뒤 굽은 길 · 8분", "천천히 가면 덜 흔들려요", MotionTone.CAUTION, "경로 형상: 시연 경로 (OSM GPX) · 지금 속도 기준"),
        guide = GuideUi(GuideIconUi.LEFT, "3km", "대관령 방면", demo = true),
        remain = "18km · 25분",
        arrive = "오후 12:05",
        observation = observed,
    )

    val suggest = drive.copy(
        content = DriveContent.Suggest(9, "횡성휴게소", listOf("흔들리는 길을 오래 달렸어요", "반려견 놀이터"), distance = "8km", direction = "강릉 방향"),
    )

    val urgent = drive.copy(content = DriveContent.Suggest(6, "둔내 졸음쉼터", listOf("졸음 위험"), urgent = true, distance = "5km"))

    val restricted = suggest.copy(restricted = true)

    val voiceFail = suggest.copy(voice = VoiceSheetState.FAILED)

    val distress = drive.copy(
        meta = "강릉까지 1시간 50분",
        content = DriveContent.Distress("에어컨을 낮추고, 다음 안전한 곳에서 멈춰요", "둔내 졸음쉼터", 6),
        nextStop = null,
        observation = observed,
    )

    val restCare = RestUi(
        meta = "둔내 졸음쉼터에서 쉬는 중",
        lead = "15분 쉬어 가요",
        minutes = 15,
        restChoice = 15,
        activities = emptyList(),
        waitLimitMin = 10,
        care = CareUi(
            "헐떡임 — 덥거나 여름",
            listOf(
                "그늘에 세우고 에어컨을 켠 채 창문을 조금 열어 주세요.",
                "물은 조금씩 자주 주세요. 한 번에 많이 마시게 하지 않아요.",
                "시원한 물로 몸을 적셔요(물에 담그지는 않아요). 바로 동물병원에 연락해요.",
            ),
            reviewed = true,
        ),
        distressChips = listOf("헐떡임", "구토", "침 흘림·하품", "낑낑·떨림", "축 처짐"),
        observation = observed,
        stimulusAvailable = true,
    )

    val vetDrive = drive.copy(meta = "동물병원까지 15분", content = DriveContent.Vet("시연용 제휴 동물병원", "2.3km", 15, "병원: 도착하면 바로 볼게요"), nextStop = null, observation = observed)

    val depart = DepartUi(
        meta = "강릉까지 예상 2시간 50분",
        pet = PetSummaryUi("콩이", "소형 · 9살 · 흔들림에 예민한 편(보호자 응답) · 배변 90분"),
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
        limitLine = "지난 여행들은 출발 약 1시간 10분 뒤 주행 자극이 보통 이상이 됐어요. (기록 2번 기준)",
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
        waitLimitMin = 10,
        battery = "배터리 57%",
        stimulusAvailable = true,
    )

    val stimulus = StimulusUi(
        now = MotionUi(22, MotionTone.CALM, "주행 자극 낮음 22"),
        recent = List(31) { k -> 30 - k / 2 },
        peak = 46,
        forecast = "18km 뒤 굽은 길 · 12분 — 천천히 가면 덜 흔들려요",
        basis = "경로 형상: 시연 경로 (OSM GPX) · 지금 속도 기준",
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
        temp = "24℃",
    )

    val waitOverdue = wait.copy(
        elapsed = "11분째", overdue = true, cabin = "실내 27℃", temp = "27℃", heat = "실내 온도가 오르고 있어요 (24℃ → 27℃)",
        batteryUsed = null, battery = null, overdueLine = "설정한 10분이 지났어요",
    )

    /** 실내 온도 신호가 없는 차 (온도 줄을 숨긴다). */
    val waitNoTemp = wait.copy(cabin = null, temp = null, seasonNote = null)

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
            VetUi("demo-partner", "시연용 제휴 동물병원", "1.0km", "시연용 가상 병원 — 실제 병원이 아니에요", null, true, minutes = 4, partner = true),
            VetUi("vs_1", "[샘플] 강릉시 동물병원 A", "2.3km", "강원특별자치도 강릉시 (샘플)", null, true, minutes = 7),
            VetUi("vs_2", "[샘플] 강릉시 동물병원 B", "3.1km", "강원특별자치도 강릉시 (샘플)", null, true, minutes = 8),
        ),
        note = "샘플 데이터예요. 전화번호는 실제 인허가 데이터에서만 보여요.",
        memo = listOf("구토", "설사", "헐떡임", "축 처짐", "떨림", "기타").map { SymptomUi(it, it == "구토") },
        memoText = "구토 두 번, 물은 조금 마셨어요",
        memoInclude = true,
        tripLine = "주행 2시간 10분(휴식 제외) · 주행 자극 최고 46 · 관찰 신고 헐떡임 1회",
        preview = listOf(
            VetRowUi("반려견", "콩이"),
            VetRowUi("증상", "구토"),
            VetRowUi("메모", "구토 두 번, 물은 조금 마셨어요"),
            VetRowUi("오늘 요약", "주행 2시간 10분(휴식 제외) · 주행 자극 최고 46 · 관찰 신고 헐떡임 1회"),
            VetRowUi("도착 예정", "약 4분 뒤 (자동 갱신)"),
            VetRowUi("보내지 않음", "위치·경로·사진"),
        ),
    )

    /** 제휴가 없는 실제 병원만 있을 때 (실차): 전화·도착 예정 링크만. */
    val vetsCall = vets.copy(
        items = listOf(VetUi("v1", "강릉 ○○동물병원", "1.5km", "강원특별자치도 강릉시", "033-000-0000", false, minutes = 5, status = "영업 중")),
        note = "지방행정 인허가 데이터(동물병원) · 영업시간은 데이터에 없어요. 가기 전에 전화로 확인해 주세요.",
        preview = emptyList(),
        // 도착 예정 링크를 만든 뒤 (QR로 보인다, 11/01)
        shareUrl = "http://192.168.0.10:8000/eta/k3J9x2",
    )

    val arrival = ArrivalUi("콩이 내렸나요?", strong = false, detail = "내린 뒤 확인을 눌러 주세요.")

    val arrivalHot = ArrivalUi("콩이 내렸나요?", strong = true, detail = "차 안이 금방 더워져요. 꼭 확인해 주세요.", warning = "실내 28℃ — 더워지고 있어요", cabin = "실내 28℃")

    val tripLog = TripLogUi(
        meta = "10월 31일, 강릉까지",
        petName = "콩이",
        totalMin = 178,
        distance = "194km",
        stops = 1,
        reports = 1,
        timeline = listOf(
            TimelineUi(TimelineKind.START, "0:00", "출발", "오전 9:00 · 서울", 0),
            TimelineUi(TimelineKind.REPORT, "0:39", "헐떡임 신고", "주행 중 · 시속 98km", 39),
            TimelineUi(TimelineKind.REST, "1:31", "횡성휴게소 15분", "반려견 산책", 91),
            TimelineUi(TimelineKind.STIMULUS, "2:20", "흔들림 많은 구간 18분", "주행 자극 보통 · 최고 55", 140),
            TimelineUi(TimelineKind.ARRIVE, "2:58", "도착", "강릉", 178),
        ),
        stimulusSeries = listOf(0, 3, 6, 9, 12, 14, 15, 9, 7, 10, 13, 15, 17, 30, 41, 46, 44, 30, 22, 18, 30, 45, 52, 55, 50, 41, 30, 22, 18, 15, 12, 10, 8, 8, 6, 5),
        marks = listOf(
            TrendMarkUi(TrendMarkKind.SYMPTOM, 39, "헐떡임"),
            TrendMarkUi(TrendMarkKind.STOP, 91, "휴식", 15),
            TrendMarkUi(TrendMarkKind.HIGH, 140, "흔들림 많음", 18),
        ),
        insight = "이번엔 출발 2시간 20분 뒤 주행 자극이 보통 이상이 됐어요.",
        summary = "이번 주행은 2시간 39분(휴식 제외), 휴식 1번, 주행 자극 최고 55였어요.",
        share = listOf(
            VetRowUi("관찰 신고", "헐떡임 1회 (출발 39분 뒤, 주행 중)"),
            VetRowUi("주행", "2시간 43분(휴식 제외) · 194km · 휴식 1번"),
            VetRowUi("주행 자극", "최고 55 (차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님)"),
        ),
        history = listOf(HistoryUi("10월 26일", "강릉 · 2시간 58분 · 신고 없음"), HistoryUi("10월 19일", "속초 · 2시간 16분 · 신고 1건")),
        calibration = "콩이 맞춤 정도: 기록 4회, 민감도 보통 → 조금 높음",
    )

    val pet = PetEditUi("콩이", SizeUi.SMALL, 9, TendencyUi.HIGH, 90, true, emptyList(), firstRun = false)

    val settings = SettingsUi(
        signals = listOf(
            SignalRowUi("주행 제한 상태", true, "CarUxRestrictions"),
            SignalRowUi("속도와 기어", true, "AAOS 차량 속성 · Pleos Vehicle SDK"),
            SignalRowUi("방위(주행 자극 예보)", false),
        ),
        version = "1.0.0",
        shapeSource = "시연 경로 (OSM GPX)",
    )

    val emergency = EmergencyUi(EmergencyUi.OPTIONS)

    val emergencyCard = emergency.copy(
        selected = "toxin",
        card = EmergencyCardUi(
            id = "toxin",
            title = "위험 물질 섭취",
            lines = listOf("먹은 것·양·시각을 확인하고, 포장이나 남은 것을 챙겨요.", "억지로 토하게 하지 않아요."),
            source = "출처: RSPCA",
            reviewed = true,
            hidden = false,
        ),
        vet = vetsCall.items.first(),
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
        Scene.STIMULUS -> Screen.Stimulus(stimulus)
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
        Scene.WAIT_NOTEMP -> Screen.Wait(waitNoTemp)
        Scene.PLACES -> Screen.Places(places)
        Scene.VETS -> Screen.Vets(vets)
        Scene.VETS_CALL -> Screen.Vets(vetsCall)
        Scene.EMERGENCY -> Screen.Emergency(emergency)
        Scene.EMERGENCY_CARD -> Screen.Emergency(emergencyCard)
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
