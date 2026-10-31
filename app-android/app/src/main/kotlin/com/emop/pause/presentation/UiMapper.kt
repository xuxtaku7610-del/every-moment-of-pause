package com.emop.pause.presentation

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PauseCause
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.data.Found
import com.emop.pause.data.PetPlace
import com.emop.pause.data.PlaceSource
import com.emop.pause.data.StopSource
import com.emop.pause.data.Vet
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.motion.MotionPhrases
import com.emop.pause.engine.motion.MotionState
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.pause.Audience
import com.emop.pause.engine.pause.PauseActivity
import com.emop.pause.engine.pause.PauseCurator
import com.emop.pause.engine.pause.PauseInput
import com.emop.pause.engine.pet.CheckItem
import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.engine.pet.CabinHeat
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.VetMemo
import com.emop.pause.trip.VoicePhase
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.ActivityUi
import com.emop.pause.uistate.ArrivalUi
import com.emop.pause.uistate.CheckKind
import com.emop.pause.uistate.CheckUi
import com.emop.pause.uistate.DepartUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.MotionUi
import com.emop.pause.uistate.PetEditUi
import com.emop.pause.uistate.PetSummaryUi
import com.emop.pause.uistate.PlaceUi
import com.emop.pause.uistate.PlacesUi
import com.emop.pause.uistate.PlanStopUi
import com.emop.pause.uistate.RestUi
import com.emop.pause.uistate.ShareUi
import com.emop.pause.uistate.SymptomUi
import com.emop.pause.uistate.SizeUi
import com.emop.pause.uistate.TendencyUi
import com.emop.pause.uistate.VetUi
import com.emop.pause.uistate.VetsUi
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.uistate.WaitUi
import com.emop.pause.uistate.motionLabel
import com.emop.pause.uistate.motionTone
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 엔진 상태(Snapshot + TripUi) → 화면 상태 (docs/ui_contract.md). 순수 함수라 단위 테스트한다.
 * 문구 원칙: 존댓말 평서문, 숫자·장소 먼저. 멀미 지수는 "주의 예보"로만 말한다.
 */
object UiMapper {
    /** 예보는 이만큼 안에 다가온 구간만 주행 화면에 보인다 (음성과 같은 기준). */
    const val FORECAST_SHOW_M = 5_000.0

    // ───────────── 공통 문구 ─────────────

    fun duration(min: Int): String {
        val m = max(0, min)
        val h = m / 60
        val r = m % 60
        return when {
            h == 0 -> "${r}분"
            r == 0 -> "${h}시간"
            else -> "${h}시간 ${r}분"
        }
    }

    fun clock(minuteOfDay: Int): String {
        val m = ((minuteOfDay % 1440) + 1440) % 1440
        return "%d:%02d".format(Locale.US, m / 60, m % 60)
    }

    fun meta(s: Snapshot): String? {
        val eta = s.eta ?: return null
        val dest = s.route?.destinationName ?: return null
        return "${dest}까지 ${duration((eta.remainTimeSec + 59) / 60)}"
    }

    fun offline(t: TripUi) = t.stopSource != null && t.stopSource != StopSource.SERVER

    /** "콩이 멀미 주의 45" / "콩이 컨디션 편안 12". */
    fun motion(m: MotionState, name: String): MotionUi {
        val tone = motionTone(m.index)
        val word = if (m.index >= MotionConstants.CAUTION) "멀미 ${motionLabel(tone)}" else "컨디션 편안"
        return MotionUi(m.index, tone, "$name $word ${m.index}")
    }

    private fun minutesTo(stop: RestStop, t: TripUi): Int {
        val speed = t.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        return max(1, ((stop.distanceAlongRouteKm - t.traveledKm) / speed).roundToInt())
    }

    private fun nextPlanned(t: TripUi): RestStop? =
        t.plan?.stops?.map { it.stop }?.firstOrNull { it.distanceAlongRouteKm > t.traveledKm + 1 }

    // ───────────── 주행 (F1·F2) ─────────────

    /** 쉼표 추천 이유 → 화면 알약. 실제 기여 순, 최대 2개 (F2-03). */
    fun reasonText(r: Reason, petName: String): String = when (r) {
        Reason.Motion -> "$petName 멀미가 쌓였어요"
        Reason.PottyDue -> "$petName 산책 시간"
        Reason.Fatigue -> "운전자 쉬어 갈 때"
        is Reason.HasFacility -> r.facility.labelKo
    }

    fun drive(s: Snapshot, t: TripUi): DriveUi {
        val name = t.pet.name
        val asking = t.asking
        val forecast = t.forecast?.takeIf { it.startInM <= FORECAST_SHOW_M }
        val d = t.distress
        val content: DriveContent = when {
            // "콩이가 이상해요"가 가장 먼저 (UPGRADE 7): 2줄 + 버튼 2개
            d != null -> DriveContent.Distress(
                d.advice.line,
                (d.stop?.name ?: d.vet?.name)?.let { n -> if (n.length > 14) n.take(13) + "…" else n },
                d.minutes,
                vet = d.vet != null,
            )
            asking != null && asking.fatigueFirst -> DriveContent.Suggest(asking.minutesAway, asking.stop.name, listOf("졸음 위험"), urgent = true, distance = kmTo(asking.stop, t))
            asking != null -> DriveContent.Suggest(
                asking.minutesAway, asking.stop.name, asking.reasons.take(2).map { reasonText(it, name) },
                distance = kmTo(asking.stop, t), direction = s.route?.destinationName?.let { "$it 방향" },
            )
            forecast != null -> DriveContent.Forecast(
                MotionPhrases.forecast(forecast, name),
                title = MotionPhrases.title(forecast),
                coach = MotionPhrases.coach(forecast, name),
                now = t.motion.index,
                // 예상 최고 = 그래프에 그리는 20분 곡선의 최고 (곡선이 없을 때만 구간 끝 예상)
                peak = forecast.trajectory.maxOrNull() ?: max(t.motion.index, forecast.predictedIndex),
                startMin = (forecast.startInM / 1000.0 / (t.kmPerMin.takeIf { it > 0 } ?: (60.0 / 60))).roundToInt(),
                durationMin = max(1, (forecast.durationSec / 60.0).roundToInt()),
                startM = forecast.startInM.roundToInt(),
                series = forecast.trajectory,
            )
            t.heading != null -> DriveContent.Heading(minutesTo(t.heading, t), t.heading.name)
            else -> DriveContent.Calm(
                nextPlanned(t)?.let { "다음 쉼표는 ${duration(minutesTo(it, t))} 뒤 ${it.name}예요." }
                    ?: "지금 컨디션이면 그대로 가도 괜찮아요.",
            )
        }
        return DriveUi(
            meta = meta(s),
            motion = if (t.fatigue != null || t.motion.index > 0) motion(t.motion, name) else null,
            content = content,
            voice = when (t.voice) {
                VoicePhase.IDLE -> null
                VoicePhase.LISTENING -> VoiceSheetState.LISTENING
                VoicePhase.FAILED -> VoiceSheetState.FAILED
            },
            toast = t.toast,
            restricted = s.driveState == DriveState.RESTRICTED,
            offline = offline(t),
            pet = name,
            sinceRest = t.fatigue?.continuousDriveMinutes?.takeIf { it > 0 }?.let { "마지막 휴식 후 ${duration(it)}" },
            nextStop = nextPlanned(t)?.let { "약 ${kmTo(it, t)}" },
            nextStopName = nextPlanned(t)?.name,
            nextStopMin = nextPlanned(t)?.let { minutesTo(it, t) },
            roadAhead = t.roadAhead,
            // 이미 굽은 길 구간 안(100m 안쪽·지난 시작점)이면 "지금"
            nextCurve = t.nextCurveM?.let { if (it < NOW_CURVE_M) NOW_CURVE else km(it / 1000.0) },
            recentMotion = t.motionRecent,
        )
    }

    /** 경로를 따라 남은 거리 "8km". */
    fun kmTo(stop: RestStop, t: TripUi): String = km(max(0.0, stop.distanceAlongRouteKm - t.traveledKm))

    /** 동물병원 가는 길 (주행 중 1순위 한 곳). 이름은 주행 화면 한 줄(14자)에 맞춰 줄인다. */
    fun vetDrive(name: String, distanceKm: Double): DriveContent.Vet =
        DriveContent.Vet(if (name.length > 14) name.take(13) + "…" else name, km(distanceKm), vetMinutes(distanceKm))

    /** 병원까지 예상 분: 시내 평균 30km/h + 주차 2분 (가정값, 내비 경로 시간이 오면 그것을 쓴다). */
    fun vetMinutes(distanceKm: Double): Int = max(1, (distanceKm / 30.0 * 60 + 2).roundToInt())

    // ───────────── 출발 체크 (F6) ─────────────

    fun petLine(p: PetProfile): String {
        val size = when (p.size) {
            PetSize.SMALL -> "소형"
            PetSize.MEDIUM -> "중형"
            PetSize.LARGE -> "대형"
        }
        val tendency = when (p.motionTendency) {
            MotionTendency.LOW -> "멀미 거의 안 함"
            MotionTendency.NORMAL -> "멀미 가끔"
            MotionTendency.HIGH -> "멀미 성향 높음"
        }
        return "$size · ${p.ageYears}살 · $tendency · 배변 ${p.pottyIntervalMin}분"
    }

    private val checkLabels = mapOf(CheckItem.HARNESS to "하네스·켄넬", CheckItem.WATER to "물", CheckItem.POTTY_BAGS to "배변봉투")

    fun checkKind(i: CheckItem) = CheckKind.valueOf(i.name)
    fun checkItem(k: CheckKind) = CheckItem.valueOf(k.name)

    fun depart(s: Snapshot, t: TripUi, limitLine: String?, savedProfile: Boolean): DepartUi {
        val speed = t.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        val plan = t.plan?.stops.orEmpty().map { p ->
            val fac = p.stop.facilities.filter { it == Facility.PET_ZONE || it == Facility.EV_FAST }.sortedBy { it.ordinal }
                .joinToString(", ") { it.labelKo }.ifEmpty { "화장실" }
            PlanStopUi(p.stop.name, fac, "${duration((p.stop.distanceAlongRouteKm / speed).roundToInt())} 뒤")
        }
        return DepartUi(
            meta = meta(s)?.replace("까지 ", "까지 예상 "),
            pet = PetSummaryUi(t.pet.name, petLine(t.pet), sample = !savedProfile),
            checks = CheckItem.entries.map { CheckUi(checkKind(it), checkLabels.getValue(it), it in t.checks) },
            ready = t.checks.containsAll(CheckItem.entries),
            plan = plan,
            rationale = if (t.plan == null) "경로를 받으면 쉴 곳을 잡아 드려요." else "${t.pet.name} 배변 간격(${t.pet.pottyIntervalMin}분)에 맞춰 쉬어 가요.",
            limitLine = limitLine,
            route = s.route?.destinationName?.let { "${it}까지" },
            duration = s.eta?.let { duration((it.remainTimeSec + 59) / 60) },
        )
    }

    // ───────────── 쉼표 (정차) ─────────────

    /** 충전 속도를 모를 때 쓰는 평균(%/분, 급속 기준). */
    const val DEFAULT_CHARGE_RATE = 1.25
    const val CHARGE_TARGET = 80

    fun rest(s: Snapshot, t: TripUi, restChoice: Int, stopName: String?, waitLimitMin: Int): RestUi {
        val charging = s.pauseCause == PauseCause.CHARGING
        val soc = s.sample.batterySocPercent
        val chargeMin = if (charging && soc != null) max(1, ((CHARGE_TARGET - soc) / DEFAULT_CHARGE_RATE).roundToInt()) else null
        val dog = t.passengers.any { it is Passenger.Dog }
        val plan = PauseCurator.curate(PauseInput(if (charging) PauseCause.CHARGING else PauseCause.REST, chargeMin, restChoice, t.fatigue?.score ?: 0, dog, t.motion.index))
        val name = t.pet.name
        val activities = plan.activities.map { a ->
            val who = if (a.audience == Audience.PET) name else "운전자"
            when (a.kind) {
                PauseActivity.PET_WALK -> ActivityUi(ActivityKind.PET_WALK, who, "산책 ${a.minutes}분", "반려견 놀이터 표지판을 따라가 보세요.", a.recommended)
                PauseActivity.PET_CARE -> ActivityUi(ActivityKind.PET_CARE, who, "물·배변 ${a.minutes}분", "물은 조금씩, 배변봉투를 챙겨 주세요.", a.recommended)
                PauseActivity.STRETCH -> ActivityUi(ActivityKind.STRETCH, who, "스트레칭 ${a.minutes}분", "목, 어깨, 허리 순서로 천천히 풀어요.", a.recommended)
            }
        }
        return RestUi(
            meta = stopName?.let { "${it}에서 쉬는 중" } ?: "쉬는 중",
            lead = if (charging) "충전이 끝날 때까지" else "${plan.totalMin}분 쉬어 가요",
            minutes = plan.totalMin,
            restChoice = if (charging) null else restChoice,
            activities = activities,
            motion = motion(t.motion, name),
            waitLimitMin = waitLimitMin,
            battery = soc?.let { "배터리 $it%" },
        )
    }

    // ───────────── 잠깐 대기 (F3) ─────────────

    fun elapsed(sec: Int) = if (sec >= 60) "${sec / 60}분째" else "${sec}초째"

    fun wait(w: WaitStatus, t: TripUi, month: Int = java.time.LocalDate.now().monthValue): WaitUi = WaitUi(
        // 보호자가 폰 페이지에서 "돌아가는 중"을 누르면 차 화면 문구가 바뀐다.
        headline = if (t.share.returning) "주인이 돌아오는 중이에요" else "주인이 곧 돌아와요",
        elapsed = elapsed(w.elapsedSec),
        // 실내 온도는 신호가 있을 때만 숫자로. 없으면 지어내지 않는다 (F3-06).
        cabin = w.cabinTempC?.let { "실내 ${it.roundToInt()}℃" },
        battery = w.batteryNow?.let { "배터리 $it%" },
        limitMin = w.limitSec / 60,
        overdue = w.overdue,
        share = ShareUi(t.share.pageUrl, t.share.connected),
        // 배터리는 신호가 있을 때만 (디자이너 메모: 측정·연동이 가능한 경우에만 표시).
        batteryUsed = w.batteryDelta?.let { "${max(0, -it)}%" },
        notice = if (w.elapsedSec < NOTICE_SEC) WAIT_NOTICE else null,
        heat = if (w.heatRising) "실내 온도가 오르고 있어요 (${w.cabinTempStartC?.roundToInt() ?: "?"}℃ → ${w.cabinTempC?.roundToInt()}℃)" else null,
        seasonNote = CabinHeat.seasonNote(w.cabinTempC, month),
    )

    /** 대기 모드를 켤 때 한 번 (처음 [NOTICE_SEC]초). */
    const val WAIT_NOTICE = "반려견을 차에 오래 혼자 두지 마세요. 이 화면은 보조 안내예요."
    const val NOTICE_SEC = 20

    // ───────────── 장소·병원 (F4·F5) ─────────────

    private val categoryKo = mapOf("restaurant" to "식당", "cafe" to "카페", "lodging" to "숙소", "tour" to "관광지", "walk" to "산책로")

    const val NOW_CURVE = "지금"
    private const val NOW_CURVE_M = 100.0

    fun km(v: Double) = if (v >= 10) "${v.roundToInt()}km" else String.format(Locale.US, "%.1fkm", v)

    /** 태그는 API 필드로 온 동반 구분(전구역/일부구역)만. 실내/실외·대형견은 태그로 판정하지 않는다(결정 10/30). */
    fun placeTags(p: PetPlace): List<String> = listOfNotNull(p.companion?.replace("동반가능", "동반")?.trim())

    /** 동반 조건 글에 명확히 써 있는 문장만, 원문 그대로: 동반 가능 동물 → 실내·실외 문장 → 필요 사항. */
    fun placeNotes(p: PetPlace): List<String> = listOfNotNull(p.sizeNote, p.spaceNote, p.petPolicy).distinct()

    private fun place(p: PetPlace, distance: String) =
        PlaceUi(p.id, p.name, categoryKo[p.category] ?: p.category, distance, p.petPolicy, p.sample, p.imageUrl, placeTags(p), placeNotes(p), p.largeDogOk, p.indoorOk)

    fun places(along: Found<PetPlace>?, near: Found<PetPlace>?, loading: Boolean): PlacesUi {
        val items = near?.items.orEmpty().map { p -> place(p, "목적지에서 ${km(p.offKm ?: 0.0)}") } +
            along?.items.orEmpty().map { p -> place(p, "경로에서 ${km(p.offKm ?: 0.0)} · ${km(p.alongKm ?: 0.0)} 지점") }
        val sample = items.any { it.sample } || listOfNotNull(along, near).any { it.source == PlaceSource.BUNDLED }
        return PlacesUi(
            "함께 가는 장소",
            items.distinctBy { it.id },
            if (sample) "샘플 데이터예요. 서버에 관광공사 반려동물 동반여행 데이터를 넣으면 실제 장소로 바뀌어요." else "한국관광공사 반려동물 동반여행 정보 (동반 조건은 현지 사정에 따라 바뀔 수 있어요)",
            loading,
            credit = if (items.any { it.imageUrl != null }) "사진 한국관광공사" else null,
        )
    }

    fun vets(
        found: Found<Vet>?,
        loading: Boolean,
        memo: VetMemo = VetMemo(),
        shareUrl: String? = null,
        listening: Boolean = false,
        tripLine: String? = null,
    ) = VetsUi(
        found?.items.orEmpty().map { v ->
            // 인허가 데이터는 "영업 중" 상태만 있다. 영업시간·24시는 지어내지 않는다.
            VetUi(v.id, v.name, km(v.distanceKm), v.address, v.phone, v.sample, vetMinutes(v.distanceKm), if (v.sample) null else "영업 중")
        },
        if (found?.items.orEmpty().any { it.sample }) "샘플 데이터예요. 전화번호는 실제 인허가 데이터에서만 보여요."
        else "지방행정 인허가 데이터(동물병원) · 영업시간은 데이터에 없어요. 가기 전에 전화로 확인해 주세요.",
        loading,
        VetMemo.CHIPS.map { SymptomUi(it, it in memo.chips) },
        shareUrl,
        memo.text,
        memo.include,
        listening,
        tripLine,
    )

    // ───────────── 도착 (F6) ─────────────

    fun arrival(t: TripUi): ArrivalUi {
        val strong = t.arrival?.strong == true
        val temp = t.arrival?.cabinTempC
        val cabin = temp?.let { "실내 ${it.roundToInt()}℃" }
        // 온도가 있으면 단계별 숫자, 없으면(여름이라 strong) 숫자 없는 일반 문구만 (UPGRADE 2, CabinHeat)
        val warning = when (CabinHeat.level(temp)) {
            CabinHeat.Level.STRONG -> "$cabin — 매우 더워요!"
            CabinHeat.Level.CAUTION -> "$cabin — 더워지고 있어요"
            CabinHeat.Level.OK -> null
            null -> if (strong) "여름에는 차 안이 빨리 더워져요" else null
        }
        return ArrivalUi(
            question = "${t.pet.name} 내렸나요?",
            strong = strong,
            detail = if (strong) "차 안이 금방 더워져요. 꼭 확인해 주세요." else "내린 뒤 확인을 눌러 주세요.",
            warning = warning,
            cabin = cabin,
        )
    }

    // ───────────── 반려견 프로필 ─────────────

    fun petEdit(d: PetProfile, firstRun: Boolean) = PetEditUi(
        name = d.name,
        size = SizeUi.valueOf(d.size.name),
        ageYears = d.ageYears,
        tendency = TendencyUi.valueOf(d.motionTendency.name),
        pottyIntervalMin = d.pottyIntervalMin,
        petZonePreferred = "pet_zone" in d.facilityPrefs,
        problems = d.problems(),
        firstRun = firstRun,
    )
}
