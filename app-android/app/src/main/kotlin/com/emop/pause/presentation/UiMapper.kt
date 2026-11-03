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
import com.emop.pause.engine.pet.PetTripRecord
import com.emop.pause.engine.pet.WaitStatus
import com.emop.pause.engine.pet.CabinHeat
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.StopVisit
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.NearbyKind
import com.emop.pause.uistate.NearbyKindUi
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
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TrendMarkUi
import com.emop.pause.uistate.VetRowUi
import com.emop.pause.uistate.VetUi
import com.emop.pause.uistate.VetsUi
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.uistate.WaitUi
import com.emop.pause.uistate.WAIT_ENGLISH
import com.emop.pause.uistate.WAIT_ENGLISH_RETURNING
import com.emop.pause.uistate.motionLabel
import com.emop.pause.uistate.motionTone
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.GuideIcon
import com.emop.pause.core.model.TurnKind
import com.emop.pause.runtime.SourceMode
import com.emop.pause.trip.DistressRecord
import com.emop.pause.trip.StimulusSegment
import com.emop.pause.trip.TripLog
import com.emop.pause.trip.TripRecorder
import com.emop.pause.uistate.GuideIconUi
import com.emop.pause.uistate.GuideUi
import com.emop.pause.uistate.ObservationUi
import com.emop.pause.uistate.StimulusUi
import com.emop.pause.uistate.TimelineKind
import com.emop.pause.uistate.TimelineUi
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 엔진 상태(Snapshot + TripUi) → 화면 상태 (docs/ui_contract.md). 순수 함수라 단위 테스트한다.
 * 문구 원칙: 존댓말 평서문, 숫자·장소 먼저. 엔진 0~100 값은 "주행 자극"(차량 흔들림)으로만 말하고 반려견 상태와 섞지 않는다 —
 * 반려견 상태는 보호자 음성 신고만으로 정한 "관찰 상태"다 (2026-10-31 재정비).
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
        // 목적지 이름이 비어 있으면(내비 안내 전) 앱 바에 "까지 0분"을 쓰지 않는다
        val dest = s.route?.destinationName?.takeIf { it.isNotBlank() } ?: return null
        return "${dest}까지 ${duration((eta.remainTimeSec + 59) / 60)}"
    }

    fun offline(t: TripUi) = t.stopSource != null && t.stopSource != StopSource.SERVER

    /** "주행 자극 보통 45" (정차 중·리포트·발표 비교에서만). [name]은 예전 호출과 맞추려고 남겨 둔다. */
    @Suppress("UNUSED_PARAMETER")
    fun motion(m: MotionState, name: String = ""): MotionUi {
        val tone = motionTone(m.index)
        return MotionUi(m.index, tone, "주행 자극 ${motionLabel(tone)} ${m.index}")
    }

    // ───────────── 콩이 관찰 상태 (음성 신고만) ─────────────

    /**
     * 관찰 상태 한 줄 (재정비 원칙 1): 신고가 없으면 "관찰 기록 없음"("양호" 아님), 있으면 건수와 마지막 신고.
     * 주행 자극은 넣지 않는다.
     */
    fun observation(events: List<DistressRecord>): ObservationUi {
        val last = events.lastOrNull() ?: return ObservationUi(NO_OBSERVATION, 0)
        return ObservationUi("신고 ${events.size}건 · ${last.label}(출발 ${duration(last.drivingMin)} 뒤)", events.size)
    }

    const val NO_OBSERVATION = "관찰 기록 없음"

    // ───────────── 길 안내 (NaviHelper 실제 값, 상태 2) ─────────────

    fun guideIcon(i: GuideIcon) = GuideIconUi.valueOf(i.name)

    fun guideIcon(k: TurnKind): GuideIconUi = when (k) {
        TurnKind.LEFT, TurnKind.SHARP_LEFT, TurnKind.RAMP_LEFT -> GuideIconUi.LEFT
        TurnKind.RIGHT, TurnKind.SHARP_RIGHT, TurnKind.RAMP_RIGHT -> GuideIconUi.RIGHT
        TurnKind.KEEP_LEFT -> GuideIconUi.KEEP_LEFT
        TurnKind.KEEP_RIGHT -> GuideIconUi.KEEP_RIGHT
        TurnKind.UTURN -> GuideIconUi.UTURN
        TurnKind.ROUNDABOUT -> GuideIconUi.ROUNDABOUT
        TurnKind.STRAIGHT -> GuideIconUi.STRAIGHT
    }

    /** 다음 길 안내: 실차는 NaviHelper TBT 첫 항목, 시뮬레이터는 시연 안내(GPX에서 만든 회전, "시연" 표시). 없으면 null. */
    fun guide(s: Snapshot, t: TripUi): GuideUi? {
        s.guide?.let { g -> return GuideUi(guideIcon(g.icon), guideDistance(g.distanceM), g.description.trim().ifEmpty { null }) }
        val demo = t.demoTurn?.takeIf { s.source == SourceMode.SIMULATION } ?: return null
        return GuideUi(guideIcon(demo.kind), guideDistance(demo.distanceM), null, demo = true)
    }

    /** 내비와 같은 거리 표기: 1km 미만은 10m 단위("80m"), 그 위는 "1.2km"·"15km". */
    fun guideDistance(m: Double): String = if (m < 1000) "${max(10, (m / 10).roundToInt() * 10)}m" else MotionPhrases.distance(m)

    /** "194km · 2시간 33분" (NaviHelper 남은 거리·시간). */
    fun remain(eta: EtaInfo?): String? = eta?.takeIf { it.remainDistanceM > 0 }?.let { "${km(it.remainDistanceM / 1000.0)} · ${duration((it.remainTimeSec + 59) / 60)}" }

    /** 도착 예정 "오후 7:44" = 지금 시각(신호 시계) + 남은 시간. */
    fun arrive(clockMinute: Int, eta: EtaInfo?): String? = eta?.takeIf { it.remainDistanceM > 0 }?.let { clock12((clockMinute + (it.remainTimeSec + 59) / 60) % (24 * 60)) }

    /** "오전 9:05" / "오후 7:44". */
    fun clock12(minuteOfDay: Int): String {
        val h = (minuteOfDay / 60) % 24
        val m = minuteOfDay % 60
        val ampm = if (h < 12) "오전" else "오후"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$ampm $h12:${"%02d".format(m)}"
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
        Reason.Motion -> "흔들리는 길을 오래 달렸어요"
        Reason.PottyDue -> "배변 시간이 다가와요"
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
            // 주변 추천 (UI v9, 일반 도로에서만 — 코디네이터가 고속도로·예보·쉼표 추천 중에는 띄우지 않는다)
            t.nearby != null -> DriveContent.Nearby(
                when (t.nearby.place.kind) {
                    NearbyKind.WALK -> NearbyKindUi.WALK
                    NearbyKind.SHOP -> NearbyKindUi.SHOP
                    NearbyKind.VET -> NearbyKindUi.VET
                },
                t.nearby.title, t.nearby.reason, line = t.nearby.line,
            )
            // 주행 자극 예보 (상태 3): 경고 한 줄만 — 숫자·그래프는 정차 중 "주행 자극 보기"에서
            forecast != null -> DriveContent.Forecast(
                title = MotionPhrases.title(forecast),
                coach = MotionPhrases.coach(forecast),
                // 예보 단계는 개인 보정 반영(personalPeak) — 화면 숫자가 아니라 "얼마나 일찍·세게 알릴지"에만 (결정 11/01)
                level = motionTone(forecast.personalPeak),
                basis = forecastBasis(t),
            )
            t.heading != null -> DriveContent.Heading(minutesTo(t.heading, t), t.heading.name)
            else -> DriveContent.Calm(
                nextPlanned(t)?.let { "다음 쉼표는 ${duration(minutesTo(it, t))} 뒤 ${it.name}예요." }
                    ?: "지금 컨디션이면 그대로 가도 괜찮아요.",
            )
        }
        val next = nextPlanned(t)
        return DriveUi(
            meta = meta(s),
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
            nextStop = next?.let { "약 ${kmTo(it, t)}" },
            nextStopName = next?.name,
            nextStopMin = next?.let { minutesTo(it, t) },
            nextStopFacilities = next?.let(::petFacilities).orEmpty(),
            guide = guide(s, t),
            remain = remain(s.eta),
            arrive = arrive(s.clockMinute, s.eta),
            destination = s.route?.destinationName?.takeIf { it.isNotBlank() },
            observation = observation(t.distressEvents),
            voiceHint = "“${name.ifEmpty { "반려견" }} 토했어”처럼 말하면 기록해요",
        )
    }

    /** 휴게소 데이터에 **있는** 반려견 시설만 (확인 안 된 시설은 쓰지 않는다). */
    fun petFacilities(stop: RestStop): List<String> = stop.facilities.filter { it == Facility.PET_ZONE }.map { it.labelKo }

    /** 예보 근거 한 줄: 무엇으로 앞길을 봤는지 + 속도 기준. */
    fun forecastBasis(t: TripUi): String = "경로 형상: ${t.shapeSource ?: "없음"} · 지금 속도 기준"

    // ───────────── 주행 자극 보기 (정차 중, 원칙 2) ─────────────

    fun stimulus(t: TripUi, peak: Int?): StimulusUi {
        val f = t.forecast
        return StimulusUi(
            now = motion(t.motion).takeIf { t.motion.drivingSec > 0 || t.motion.index > 0 },
            recent = t.motionRecent,
            peak = peak,
            forecast = f?.let { "${MotionPhrases.title(it)} — ${MotionPhrases.coach(it)}" },
            basis = f?.let { forecastBasis(t) },
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
        // 보호자 응답(온보딩 질문) — 측정·진단이 아니다 (사용자 결정 10/31: "흔들림에 예민한 정도(보호자 응답)")
        val tendency = when (p.motionTendency) {
            MotionTendency.LOW -> "흔들림에 덜 예민한 편"
            MotionTendency.NORMAL -> "흔들림 예민함 보통"
            MotionTendency.HIGH -> "흔들림에 예민한 편"
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
            route = s.route?.destinationName?.takeIf { it.isNotBlank() }?.let { "${it}까지" },
            duration = s.eta?.takeIf { it.remainTimeSec > 0 && s.route?.destinationName?.isNotBlank() == true }?.let { duration((it.remainTimeSec + 59) / 60) },
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
            waitLimitMin = waitLimitMin,
            battery = soc?.let { "배터리 $it%" },
            observation = observation(t.distressEvents),
            stimulusAvailable = t.motion.drivingSec > 0,
        )
    }

    // ───────────── 잠깐 대기 (F3) ─────────────

    fun elapsed(sec: Int) = if (sec >= 60) "${sec / 60}분째" else "${sec}초째"

    /** [acOn] = 공조 에어컨 신호(Pleos `registerHvacAcState`)가 실제로 켜짐일 때만 true. 없거나 꺼짐이면 표시하지 않는다. */
    fun wait(w: WaitStatus, t: TripUi, month: Int = java.time.LocalDate.now().monthValue, acOn: Boolean? = null): WaitUi = WaitUi(
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
        temp = w.cabinTempC?.let { "${it.roundToInt()}℃" },
        acOn = acOn == true,
        english = if (t.share.returning) WAIT_ENGLISH_RETURNING else WAIT_ENGLISH,
        overdueLine = if (w.overdue) "설정한 ${w.limitSec / 60}분이 지났어요" else null,
    )

    /** 대기 모드를 켤 때 한 번 (처음 [NOTICE_SEC]초). */
    const val WAIT_NOTICE = "반려견을 차에 오래 혼자 두지 마세요. 이 화면은 보조 안내예요."
    const val NOTICE_SEC = 20

    // ───────────── 장소·병원 (F4·F5) ─────────────

    private val categoryKo = mapOf("restaurant" to "식당", "cafe" to "카페", "lodging" to "숙소", "tour" to "관광지", "walk" to "산책로")


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
        // 내장 데이터도 이제 실제 TourAPI 데이터(data/embedded, 11/03) — 샘플 행이 있을 때만 "샘플"이라고 한다
        val sample = items.any { it.sample }
        val bundled = listOfNotNull(along, near).any { it.source == PlaceSource.BUNDLED }
        return PlacesUi(
            "함께 가는 장소",
            items.distinctBy { it.id },
            when {
                sample -> "샘플 데이터예요. 서버에 관광공사 반려동물 동반여행 데이터를 넣으면 실제 장소로 바뀌어요."
                bundled -> "한국관광공사 반려동물 동반여행 정보 (앱 내장 데이터 · 동반 조건은 현지 사정에 따라 바뀔 수 있어요)"
                else -> "한국관광공사 반려동물 동반여행 정보 (동반 조건은 현지 사정에 따라 바뀔 수 있어요)"
            },
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
            VetUi(v.id, v.name, km(v.distanceKm), v.address, v.phone, v.sample, vetMinutes(v.distanceKm), if (v.sample) null else "영업 중", partner = v.partner)
        },
        if (found?.items.orEmpty().any { it.sample && !it.partner }) "샘플 데이터예요. 전화번호는 실제 인허가 데이터에서만 보여요."
        else "지방행정 인허가 데이터(동물병원) · 영업시간은 데이터에 없어요. 가기 전에 전화로 확인해 주세요.",
        loading,
        VetMemo.CHIPS.map { SymptomUi(it, it in memo.chips) },
        shareUrl,
        memo.text,
        memo.include,
        listening,
        tripLine,
    )

    /** 병원에 보낼 내용 미리보기 (전달 동의 전, 상태 7). 위치는 보내지 않는다는 줄을 늘 함께. */
    fun vetPreview(pet: String, memo: VetMemo, tripLine: String?, minutes: Int?): List<VetRowUi> = buildList {
        val shared = VetMemo.share(memo.copy(include = true), tripLine)
        add(VetRowUi("반려견", pet))
        if (memo.include) {
            shared.symptoms.takeIf { it.isNotEmpty() }?.let { add(VetRowUi("증상", it.joinToString(", "))) }
            shared.memo?.let { add(VetRowUi("메모", it)) }
            shared.tripLine?.let { add(VetRowUi("오늘 요약", it)) }
        }
        minutes?.let { add(VetRowUi("도착 예정", "약 ${it}분 뒤 (자동 갱신)")) }
        add(VetRowUi("보내지 않음", "위치·경로·사진"))
    }

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

    // ───────────── 도착 후 기록 리포트 (상태 8) ─────────────

    /** 주행 중 신고 이름 → 짧은 이름 ("침 흘림·하품" → "침 흘림"). */
    fun symptomName(eventLabel: String): String = when (eventLabel) {
        "침 흘림·하품" -> "침 흘림"
        "낑낑·떨림" -> "낑낑거림"
        else -> eventLabel
    }

    /** 오늘 관찰 신고 한 줄 (UPGRADE 7): "오늘 헐떡임 1회(출발 39분 뒤)". */
    fun eventsLine(events: List<DistressRecord>): String? =
        events.groupBy { it.label }.map { (k, v) -> "$k ${v.size}회(출발 ${duration(v.first().drivingMin)} 뒤)" }
            .takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "오늘 ")

    /**
     * 주행 분(정차 제외) → 출발 뒤 분(정차 포함, 그래프·타임라인 시계). 그 시각 전에 시작한 휴식 길이를 더한다.
     * [stops]는 (출발 뒤 시작 분, 길이 분).
     */
    fun elapsedMin(drivingMin: Int, stops: List<Pair<Int, Int>>): Int {
        var m = drivingMin
        stops.sortedBy { it.first }.forEach { (start, len) -> if (start <= m) m += len }
        return m
    }

    /**
     * 리포트 한 줄의 "출발 X 뒤 주행 자극이 보통 이상" = 타임라인 첫 "흔들림 많은 구간"의 시작 (출발 뒤, 정차 포함).
     * 한 계산만 쓴다 — 주행 분(정차 제외)을 따로 쓰면 캡처마다 2시간 14분·2시간 35분처럼 달라진다(11/01). 구간이 없으면 null.
     */
    fun firstStimulusMin(log: TripLog): Int? = log.segments.minOfOrNull { it.startMin }

    /** "2:14" (출발 뒤). */
    fun hm(min: Int): String = "${min / 60}:${"%02d".format(min % 60)}"

    /** 리포트 그래프 표시: 휴식 구간 · 관찰 신고 · 주행 자극 보통 이상 구간. 출발 시각을 모르면 없음. */
    fun trendMarks(startMinute: Int?, visits: List<StopVisit>, events: List<DistressRecord>, segments: List<StimulusSegment> = emptyList()): List<TrendMarkUi> {
        val start = startMinute ?: return emptyList()
        val stops = visits.map { (it.arrivedMinute - start) to it.stayedMin }
        return stops.filter { it.second > 0 }.map { (m, len) -> TrendMarkUi(TrendMarkKind.STOP, m, "휴식", len) } +
            events.map { e -> TrendMarkUi(TrendMarkKind.SYMPTOM, elapsedMin(e.drivingMin, stops), symptomName(e.label)) } +
            segments.map { g -> TrendMarkUi(TrendMarkKind.HIGH, g.startMin, "흔들림 많음", max(1, g.endMin - g.startMin)) }
    }

    /**
     * 시간순 타임라인 (상태 8의 주인공): 출발 → 주행 자극 보통 이상 구간 → 관찰 신고 → 휴식 → 병원 → 도착.
     * 각 줄은 실제 기록만 — 이름을 모르는 휴식은 "길가", 속도를 모르는 신고는 속도를 빼고, 데이터가 없으면 줄을 뺀다.
     */
    fun timeline(log: TripLog, events: List<DistressRecord>, destination: String?, vetName: String? = null): List<TimelineUi> {
        val start = log.startMinute ?: return emptyList()
        val stops = log.visits.map { (it.arrivedMinute - start) to it.stayedMin }
        val items = mutableListOf(TimelineUi(TimelineKind.START, hm(0), "출발", clock12(start % (24 * 60)), 0))
        log.segments.forEach { g ->
            val len = max(1, g.endMin - g.startMin)
            items += TimelineUi(TimelineKind.STIMULUS, hm(g.startMin), "흔들림 많은 구간 ${duration(len)}", "주행 자극 ${motionLabel(motionTone(g.peak))} · 최고 ${g.peak}", g.startMin)
        }
        events.forEach { e ->
            val m = elapsedMin(e.drivingMin, stops)
            val detail = listOfNotNull("주행 중", e.speedKph?.takeIf { it >= 1f }?.let { "시속 ${it.roundToInt()}km" }).joinToString(" · ")
            items += TimelineUi(TimelineKind.REPORT, hm(m), "${symptomName(e.label)} 신고", detail, m)
        }
        log.visits.forEach { v ->
            val m = v.arrivedMinute - start
            val name = v.name ?: TripRecorder.ROADSIDE
            val vet = vetName != null && name == vetName
            val detail = listOfNotNull("반려견 산책".takeIf { v.petWalk }, "충전 ${v.chargedMin}분".takeIf { v.chargedMin > 0 }).joinToString(" · ").ifEmpty { null }
            items += TimelineUi(if (vet) TimelineKind.VET else TimelineKind.REST, hm(m), "$name ${duration(v.stayedMin)}", detail, m)
        }
        if (log.elapsedMin > 0) items += TimelineUi(TimelineKind.ARRIVE, hm(log.elapsedMin), "도착", destination, log.elapsedMin)
        return items.sortedWith(compareBy({ it.minute }, { it.kind.ordinal }))
    }

    /** "병원에 공유" 내용: 관찰 신고(시각·주행 중) → 주행 요약 → 주행 자극(차량 기준 표기). 값이 있는 줄만. */
    fun reportShare(r: PetTripRecord, events: List<DistressRecord>, distanceKm: Double?): List<VetRowUi> = buildList {
        if (events.isNotEmpty()) {
            add(VetRowUi("관찰 신고", events.groupBy { symptomName(it.label) }.map { (k, v) -> "$k ${v.size}회 (출발 ${duration(v.first().drivingMin)} 뒤, 주행 중)" }.joinToString(", ")))
        } else {
            add(VetRowUi("관찰 신고", "없음"))
        }
        val dist = distanceKm?.takeIf { it >= 1 }?.let { " · ${km(it)}" }.orEmpty()
        add(VetRowUi("주행", "${duration(r.drivingMin)}(휴식 제외)$dist · 휴식 ${r.stops}번"))
        add(VetRowUi("주행 자극", "최고 ${r.motionPeak} (차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님)"))
    }
}
