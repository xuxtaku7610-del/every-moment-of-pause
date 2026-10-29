package com.emop.pause.uistate

/**
 * 시안(docs/design/mockups 폴더의 PNG)과 같은 상태. @Preview, UI 검증 캡처, 발표 모드 "장면 고정"에 쓴다.
 * 이름(지우·콩이)은 시안의 예시이고, 실제 화면은 사용자가 입력한 이름을 쓴다 (UI_SPEC §6).
 */
object Fixtures {
    private val journeyDrive = JourneyUi(
        0.38f,
        listOf(JourneyStop(0.47f, "18분 뒤 안성", StopMark.NEXT), JourneyStop(0.76f, "1시간 50분 뒤 칠곡", StopMark.UPCOMING)),
        "서울",
        "부산",
    )

    val fatigueCaution = FatigueUi(61, FatigueTone.CAUTION, "1시간 40분째 쉬지 않고 운전 중이에요. 어젯밤 5시간 주무셨어요.")

    val family = listOf(
        FamilyLine(AvatarKind.KID, "지우", "화장실 25분 안에"),
        FamilyLine(AvatarKind.PET, "콩이", "산책 40분 지남"),
        FamilyLine(AvatarKind.CHARGE, "배터리", "34%, 여기서 충전"),
    )

    val drive = DriveUi(
        meta = "부산 해운대까지 3시간 10분",
        content = DriveContent.Next(
            18,
            "안성휴게소",
            "경부선 부산방향",
            listOf(ChipUi("키즈존", ChipIcon.KID), ChipUi("급속충전 4대", ChipIcon.BOLT), ChipUi("반려견 놀이터", ChipIcon.PAW)),
            asking = true,
        ),
        fatigue = fatigueCaution,
        family = family,
        journey = journeyDrive,
    )

    val half = drive.copy(
        content = (drive.content as DriveContent.Next).copy(chips = listOf(ChipUi("키즈존", ChipIcon.KID), ChipUi("급속충전", ChipIcon.BOLT))),
    )

    val calm = DriveUi(
        meta = "부산 해운대까지 4시간 20분",
        content = DriveContent.Calm("다음 쉼표는 50분 뒤 안성휴게소로 잡아 뒀어요.", listOf(ChipUi("50분 뒤 안성", ChipIcon.COMMA), ChipUi("피로 편안 22"))),
        fatigue = FatigueUi(22, FatigueTone.CALM, "45분째 운전 중이에요."),
        family = emptyList(),
        journey = JourneyUi(
            0.12f,
            listOf(JourneyStop(0.47f, "50분 뒤 안성", StopMark.NEXT), JourneyStop(0.76f, "2시간 40분 뒤 칠곡", StopMark.UPCOMING)),
            "서울",
            "부산",
        ),
    )

    val urgent = DriveUi(
        meta = "부산 해운대까지 2시간 05분",
        content = DriveContent.Urgent(86, "옥천 졸음쉼터", "경부선 부산방향, 화장실 있음", "3"),
        fatigue = FatigueUi(86, FatigueTone.ALERT, "2시간 10분째 쉬지 않고 운전 중이에요."),
        family = emptyList(),
        journey = null,
    )

    val listening = drive.copy(voice = VoiceSheetState.LISTENING)
    val voiceFail = drive.copy(voice = VoiceSheetState.FAILED)

    val offline = drive.copy(
        content = (drive.content as DriveContent.Next).copy(chips = listOf(ChipUi("키즈존", ChipIcon.KID), ChipUi("급속충전", ChipIcon.BOLT))),
        fatigue = fatigueCaution.copy(detail = "피로 계산은 인터넷 없이도 계속돼요."),
        family = emptyList(),
        journey = JourneyUi(0.38f, listOf(JourneyStop(0.47f, "18분 뒤 안성", StopMark.NEXT)), "서울", "부산"),
        offline = true,
    )

    val prepare = PrepareScreenUi(
        meta = "부산 해운대까지 예상 4시간 50분",
        members = listOf(
            MemberUi("driver", "운전자", AvatarKind.ADULT, true),
            MemberUi("relief", "교대 1명", AvatarKind.ADULT, true),
            MemberUi("kid", "지우 5살", AvatarKind.KID, true),
            MemberUi("dog", "콩이", AvatarKind.PET, true),
        ),
        sleepHours = 5,
        proactive = true,
        plan = listOf(PlanStopUi("안성휴게소", "키즈존, 급속충전, 반려견 놀이터", "1시간 20분 뒤"), PlanStopUi("칠곡휴게소", "수유실, 급속충전", "3시간 10분 뒤")),
        journey = JourneyUi(0f, listOf(JourneyStop(0.28f, "1시간 20분", StopMark.NEXT), JourneyStop(0.66f, "3시간 10분", StopMark.UPCOMING)), null, "부산"),
        rationale = "평소보다 20분 일찍 첫 휴식을 잡았어요.",
    )

    val onboardMembers = listOf(
        MemberUi("relief", "어른 2명", AvatarKind.ADULT, true),
        MemberUi("kid", "아이 1명 · 5살", AvatarKind.KID, true),
        MemberUi("dog", "반려견 1마리", AvatarKind.PET, true),
    )

    val pause = PauseScreenUi(
        meta = "안성휴게소에서 쉬는 중",
        lead = "충전이 끝날 때까지",
        number = "27",
        unit = "분",
        battery = BatteryUi(46, 80, "급속충전 중 182kW", "13:12 완료 예상"),
        restChoice = null,
        activities = listOf(
            ActivityUi(ActivityKind.NAP, "운전자", "20분 파워냅", "충전이 끝나기 2분 전에 조명과 소리로 깨워 드릴게요.", "눈 붙이기", true),
            ActivityUi(ActivityKind.EXPEDITION, "지우", "휴게소 원정대 15분", "화장실, 물 한 컵, 기지개 3번. 다 하면 다음 라디오가 열려요.", "시작", false),
            ActivityUi(ActivityKind.PET_WALK, "콩이", "산책 10분", "반려견 놀이터는 주차장 B 옆에 있어요.", "타이머", false),
        ),
        timeline = listOf(
            TimelinePoint(0f, "12:18", "도착", TimelineDot.PAST),
            TimelinePoint(0.24f, "12:45", "지금", TimelineDot.NOW),
            TimelinePoint(0.88f, "13:10", "기상 알림", TimelineDot.ALARM),
            TimelinePoint(1f, "13:12", "충전 완료", TimelineDot.END),
        ),
        nowFraction = 0.24f,
    )

    val nap = NapUi(13 * 60, 20 * 60, "충전이 끝나기 2분 전, 13:10에 깨워 드릴게요", soundOn = true)

    val report = ReportUi(
        meta = "10월 31일, 서울에서 부산까지",
        sentence = listOf("서울에서 출발해", " 안성에서 충전하며 20분 자고", " 칠곡에서 지우와 원정대를 마친 뒤 부산에 도착했어요."),
        distanceKm = 398,
        stops = 2,
        longestMin = 100,
        log = listOf(
            ReportStopUi("안성휴게소", listOf("충전 27분", "파워냅 20분", "콩이 산책"), "12:18", "40분"),
            ReportStopUi("칠곡휴게소", listOf("원정대 완료", "운전 교대"), "14:58", "25분"),
        ),
        fatigueSeries = listOf(6, 14, 24, 36, 48, 58, 64, 11, 18, 26, 36, 46, 56, 9, 14, 20, 24),
        restMarks = listOf(0.375f, 0.75f),
        tip = "첫 휴식을 20분 앞당기면 피로가 주의 단계에 닿지 않았을 거예요.",
    )

    val settings = SettingsUi(
        signals = listOf(SignalRowUi("속도와 기어", true), SignalRowUi("배터리와 충전", true), SignalRowUi("공조와 실내 순환", false)),
        members = prepare.members,
        version = "0.1.0",
    )

    /** 발표 모드 "시안 장면 고정"의 화면 상태. 1/2·1/3 창은 상태가 같고 폭만 다르다. */
    fun screen(scene: Scene): Screen = when (scene) {
        Scene.DRIVE -> Screen.Drive(drive)
        Scene.HALF, Scene.THIRD -> Screen.Drive(half)
        Scene.CALM -> Screen.Drive(calm)
        Scene.URGENT -> Screen.Drive(urgent)
        Scene.LISTENING -> Screen.Drive(listening)
        Scene.VOICE_FAIL -> Screen.Drive(voiceFail)
        Scene.OFFLINE -> Screen.Drive(offline)
        Scene.PREPARE -> Screen.Prepare(prepare)
        Scene.PAUSE -> Screen.Pause(pause)
        Scene.NAP -> Screen.Nap(nap)
        Scene.REPORT -> Screen.Report(report)
        Scene.ONBOARD1 -> Screen.Onboard(OnboardUi(OnboardStep.CONCEPT, onboardMembers))
        Scene.ONBOARD2 -> Screen.Onboard(OnboardUi(OnboardStep.MEMBERS, onboardMembers))
        Scene.ONBOARD3 -> Screen.Onboard(OnboardUi(OnboardStep.MIC, onboardMembers))
        Scene.SETTINGS -> Screen.Settings(settings)
    }
}
