package com.emop.pause.presentation

import com.emop.pause.uistate.FatigueTone
import com.emop.pause.uistate.toneLabel
import com.emop.pause.uistate.ChipUi
import com.emop.pause.uistate.ChipIcon
import com.emop.pause.uistate.DriveUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.FatigueUi
import com.emop.pause.uistate.FamilyLine
import com.emop.pause.uistate.PlanStopUi
import com.emop.pause.uistate.BatteryUi
import com.emop.pause.uistate.ActivityKind
import com.emop.pause.uistate.ActivityUi
import com.emop.pause.uistate.PauseScreenUi
import com.emop.pause.uistate.MissionUi
import com.emop.pause.uistate.RadioLineUi
import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.PauseCause
import com.emop.pause.data.StopSource
import com.emop.pause.engine.fatigue.FatigueLevel
import com.emop.pause.engine.needs.NeedKind
import com.emop.pause.engine.needs.Passenger
import com.emop.pause.engine.pause.Audience
import com.emop.pause.engine.pause.PauseActivity
import com.emop.pause.engine.pause.PauseCurator
import com.emop.pause.engine.pause.PauseInput
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.PlanDecision
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.engine.planner.StopType
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
import com.emop.pause.trip.VoicePhase
import com.emop.pause.uistate.AvatarKind
import com.emop.pause.uistate.JourneyStop
import com.emop.pause.uistate.JourneyUi
import com.emop.pause.uistate.StopMark
import com.emop.pause.uistate.TimelineDot
import com.emop.pause.uistate.TimelinePoint
import com.emop.pause.uistate.VoiceSheetState
import com.emop.pause.uistate.toneOf
import kotlin.math.max
import kotlin.math.roundToInt

/** 동승자 이름 (사용자가 입력한 이름, 없으면 기본 호칭). */
data class Names(val child: String = "아이", val dog: String = "반려견")

/**
 * 엔진 상태(Snapshot + TripUi) → 화면 모델. 순수 함수라 단위 테스트한다.
 * 문구 원칙: 존댓말 평서문, 숫자·장소 먼저 (UI_SPEC §6).
 */
object UiMapper {
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
        return "%d:%02d".format(m / 60, m % 60)
    }

    /** "안성휴게소" → "안성", "옥천 졸음쉼터" → "옥천". 경로선 라벨용. */
    fun shortName(name: String): String =
        name.substringBefore("(").removeSuffix("휴게소").removeSuffix("졸음쉼터").removeSuffix("쉼터").trim().ifEmpty { name }

    fun meta(s: Snapshot): String? {
        val eta = s.eta ?: return null
        val dest = s.route?.destinationName ?: return null
        return "${dest}까지 ${duration((eta.remainTimeSec + 59) / 60)}"
    }

    private fun minutesTo(stop: RestStop, t: TripUi): Int {
        val speed = t.kmPerMin.takeIf { it > 0 } ?: (100.0 / 60)
        return max(1, ((stop.distanceAlongRouteKm - t.traveledKm) / speed).roundToInt())
    }

    fun offline(t: TripUi) = t.stopSource != null && t.stopSource != StopSource.SERVER

    // ───────────── 주행 ─────────────

    fun drive(s: Snapshot, t: TripUi, names: Names = Names()): DriveUi {
        val f = t.fatigue
        val score = f?.score ?: 0
        val offline = offline(t)
        val fatigueUi = f?.let {
            val detail = when {
                offline -> "피로 계산은 인터넷 없이도 계속돼요."
                it.signalLost -> "차량 신호가 끊겨 운전 시간으로만 계산하고 있어요."
                else -> buildString {
                    append(
                        if (it.continuousDriveMinutes < 1) "방금 출발했어요."
                        else "${duration(it.continuousDriveMinutes)}째 쉬지 않고 운전 중이에요.",
                    )
                    t.sleepHours?.takeIf { h -> h < 7f }?.let { h -> append(" 어젯밤 ${h.roundToInt()}시간 주무셨어요.") }
                }
            }
            FatigueUi(it.score, toneOf(it.score), detail)
        }
        val asking = t.asking
        val content: DriveContent = when {
            asking != null && asking.fatigueFirst -> DriveContent.Urgent(
                score,
                asking.stop.name,
                listOfNotNull(asking.stop.routeLabel, if (asking.stop.type == StopType.DROWSY_SHELTER) "화장실 있음" else null).joinToString(", "),
                km(asking.kmAway),
            )
            asking != null -> DriveContent.Next(asking.minutesAway, asking.stop.name, asking.stop.routeLabel, chips(asking, t), asking = true)
            t.heading != null -> DriveContent.Next(minutesTo(t.heading, t), t.heading.name, t.heading.routeLabel, facilityChips(t.heading, t), asking = false)
            // 피로가 주의 이상인데 지금 낼 제안이 없으면(앞쪽 후보 없음 등) "편안해요"라고 하지 않는다 — 다음 계획 쉼표를 보여 준다.
            toneOf(score) != com.emop.pause.uistate.FatigueTone.CALM && nextPlanned(t) != null -> nextPlanned(t)!!.let {
                DriveContent.Next(minutesTo(it, t), it.name, it.routeLabel, facilityChips(it, t), asking = false)
            }
            else -> calm(t, score)
        }
        return DriveUi(
            meta = meta(s),
            content = content,
            fatigue = fatigueUi,
            family = family(s, t, names),
            journey = journey(s, t),
            offline = offline,
            restricted = s.driveState == DriveState.RESTRICTED,
            voice = when (t.voice) {
                VoicePhase.IDLE -> null
                VoicePhase.LISTENING -> VoiceSheetState.LISTENING
                VoicePhase.FAILED -> VoiceSheetState.FAILED
            },
            toast = t.toast,
            radio = t.radio.takeIf { it.playing && it.title != null }?.let { RadioLineUi(it.title!!, it.index + 1, it.count, it.draft) },
        )
    }

    private fun calm(t: TripUi, score: Int): DriveContent.Calm {
        val next = nextPlanned(t)
        val line = next?.let { "다음 쉼표는 ${duration(minutesTo(it, t))} 뒤 ${it.name}로 잡아 뒀어요." }
            ?: "지금 컨디션이면 목적지까지 그대로 가도 괜찮아요."
        val chips = listOfNotNull(
            next?.let { ChipUi("${duration(minutesTo(it, t))} 뒤 ${shortName(it.name)}", ChipIcon.COMMA) },
            ChipUi("피로 ${com.emop.pause.uistate.toneLabel(toneOf(score))} $score"),
        )
        return DriveContent.Calm(line, chips)
    }

    private fun nextPlanned(t: TripUi): RestStop? =
        t.plan?.stops?.map { it.stop }?.firstOrNull { it.distanceAlongRouteKm > t.traveledKm + 1 }

    private fun km(v: Double) = if (v >= 10) v.roundToInt().toString() else "%.1f".format(v).removeSuffix(".0")

    /** 이유 칩: 실제 점수에 기여한 이유 먼저, 그다음 동승자에게 의미 있는 편의시설. 최대 3개. */
    private fun chips(d: PlanDecision.Suggest, t: TripUi): List<ChipUi> {
        val reasons = d.reasons.filterIsInstance<Reason.HasFacility>().map { it.facility }
        return (reasons + relevant(d.stop, t)).distinct().take(3).map(::chipOf)
    }

    private fun facilityChips(stop: RestStop, t: TripUi) = relevant(stop, t).take(3).map(::chipOf)

    private fun relevant(stop: RestStop, t: TripUi): List<Facility> {
        val order = buildList {
            if (t.passengers.any { it is Passenger.Child && it.age > 2 }) add(Facility.KIDS_ZONE)
            if (t.passengers.any { it is Passenger.Child && it.age <= 2 }) add(Facility.NURSING_ROOM)
            add(Facility.EV_FAST)
            if (t.passengers.any { it is Passenger.Dog }) add(Facility.PET_ZONE)
            add(Facility.SLEEP_ROOM)
        }
        return order.filter { it in stop.facilities }
    }

    fun chipOf(f: Facility): ChipUi = when (f) {
        Facility.KIDS_ZONE -> ChipUi("키즈존", ChipIcon.KID)
        Facility.EV_FAST -> ChipUi("급속충전", ChipIcon.BOLT)
        Facility.EV_SLOW -> ChipUi("완속충전", ChipIcon.BOLT)
        Facility.PET_ZONE -> ChipUi("반려견 놀이터", ChipIcon.PAW)
        Facility.NURSING_ROOM -> ChipUi("수유실", ChipIcon.BABY)
        Facility.SLEEP_ROOM -> ChipUi("수면실", ChipIcon.BED)
        Facility.SHOWER -> ChipUi("샤워실")
        Facility.ACCESSIBLE_TOILET -> ChipUi("장애인 화장실")
    }

    /** 함께 탄 가족 카드: 급한 순 최대 3줄. */
    fun family(s: Snapshot, t: TripUi, names: Names = Names()): List<FamilyLine> {
        val needs = t.needs?.needs.orEmpty()
        return needs.mapNotNull { n ->
            when (n.kind) {
                NeedKind.TOILET -> FamilyLine(AvatarKind.KID, names.child, if (n.dueInMin > 0) "화장실 ${n.dueInMin}분 안에" else "화장실이 급해요")
                NeedKind.NURSING -> FamilyLine(AvatarKind.KID, names.child, if (n.dueInMin > 0) "수유 ${n.dueInMin}분 안에" else "수유 시간이에요")
                NeedKind.PET_WALK -> FamilyLine(AvatarKind.PET, names.dog, if (n.dueInMin > 0) "산책 ${n.dueInMin}분 남음" else "산책 시간이 지났어요")
                NeedKind.CHARGE -> FamilyLine(AvatarKind.CHARGE, "배터리", "${s.sample.batterySocPercent ?: "-"}%, 다음 충전 필요")
            }
        }.take(3)
    }

    fun journey(s: Snapshot, t: TripUi): JourneyUi? {
        val totalKm = (s.route?.totalDistanceM ?: return null) / 1000.0
        if (totalKm <= 0) return null
        val next = t.asking?.stop ?: t.heading ?: nextPlanned(t)
        val planned = t.plan?.stops?.map { it.stop }.orEmpty()
        val marks = (planned + listOfNotNull(next)).distinctBy { it.id }.sortedBy { it.distanceAlongRouteKm }.map { st ->
            val mark = when {
                st.id == next?.id -> StopMark.NEXT
                st.distanceAlongRouteKm < t.traveledKm -> StopMark.PAST
                else -> StopMark.UPCOMING
            }
            val label = if (mark == StopMark.PAST) "" else "${duration(minutesTo(st, t))} 뒤 ${shortName(st.name)}"
            JourneyStop((st.distanceAlongRouteKm / totalKm).toFloat(), label, mark)
        }
        val start = "출발"
        return JourneyUi((t.traveledKm / totalKm).toFloat(), marks, start, s.route.destinationName)
    }

    // ───────────── 출발 준비 ─────────────

    fun preparePlan(t: TripUi, kmPerMinFallback: Double = 100.0 / 60): Pair<List<PlanStopUi>, JourneyUi?> {
        val plan = t.plan ?: return emptyList<PlanStopUi>() to null
        val speed = t.kmPerMin.takeIf { it > 0 } ?: kmPerMinFallback
        val rows = plan.stops.map { p ->
            PlanStopUi(p.stop.name, p.stop.facilities.sortedBy { it.ordinal }.filter { it != Facility.EV_SLOW }.map { chipOf(it).text }.ifEmpty { listOf("화장실") }.joinToString(", "), "${duration((p.stop.distanceAlongRouteKm / speed).roundToInt())} 뒤")
        }
        return rows to null
    }

    // ───────────── 멈춤 ─────────────

    /** 충전 속도를 모를 때 쓰는 평균(%/분, 급속 기준). 화면에는 "예상"으로 표시한다. */
    const val DEFAULT_CHARGE_RATE = 1.25
    const val CHARGE_TARGET = 80

    data class PauseExtra(
        val arrivedMinute: Int,
        val restChoice: Int = PauseInput.DEFAULT_REST_MIN,
        val stopName: String?,
        val chargeRatePerMin: Double? = null,
    )

    fun pause(s: Snapshot, t: TripUi, extra: PauseExtra, names: Names = Names()): PauseScreenUi {
        val charging = s.pauseCause == PauseCause.CHARGING
        val soc = s.sample.batterySocPercent
        val rate = extra.chargeRatePerMin ?: DEFAULT_CHARGE_RATE
        val chargeMin = if (charging && soc != null) max(1, ((CHARGE_TARGET - soc) / rate).roundToInt()) else null
        val children = t.passengers.filterIsInstance<Passenger.Child>().map { it.age }
        val dog = t.passengers.any { it is Passenger.Dog }
        val plan = PauseCurator.curate(
            PauseInput(if (charging) PauseCause.CHARGING else PauseCause.REST, chargeMin, extra.restChoice, soc, t.fatigue?.score ?: 0, t.sleepHours, children, dog),
        )
        val now = s.clockMinute
        val end = now + plan.totalMin
        val activities = plan.activities.map { a ->
            val who = when (a.audience) {
                Audience.DRIVER -> "운전자"
                Audience.CHILD -> names.child
                Audience.PET -> names.dog
            }
            when (a.kind) {
                PauseActivity.NAP -> ActivityUi(
                    ActivityKind.NAP, who, "${a.minutes}분 파워냅",
                    if (!a.climateKeep) "배터리가 낮아 공조를 끄고 쉬어요. 창문을 조금 열어 두세요."
                    else if (charging) "충전이 끝나기 ${plan.wakeBeforeEndMin}분 전에 조명과 소리로 깨워 드릴게요."
                    else "${a.minutes}분 뒤에 조명과 소리로 깨워 드릴게요.",
                    "눈 붙이기", a.recommended,
                )
                PauseActivity.EXPEDITION -> ActivityUi(ActivityKind.EXPEDITION, who, "휴게소 원정대 ${a.minutes}분", "화장실, 물 한 컵, 기지개 3번. 다 하면 다음 라디오가 열려요.", "시작", a.recommended)
                PauseActivity.PET_WALK -> ActivityUi(ActivityKind.PET_WALK, who, "산책 ${a.minutes}분", "반려견 놀이터 표지판을 따라가 보세요.", "타이머", a.recommended)
                PauseActivity.FOCUS -> ActivityUi(ActivityKind.FOCUS, who, "업무 집중 ${a.minutes}분", "알림을 줄이고 끝나기 1분 전에 알려 드릴게요.", "시작", a.recommended)
                PauseActivity.STRETCH -> ActivityUi(ActivityKind.STRETCH, who, "스트레칭 ${a.minutes}분", "목, 어깨, 허리 순서로 천천히 풀어요.", "시작", a.recommended)
            }
        }
        val elapsed = max(0, now - extra.arrivedMinute)
        val span = max(1, elapsed + plan.totalMin)
        val nowF = elapsed / span.toFloat()
        val timeline = buildList {
            add(TimelinePoint(0f, clock(extra.arrivedMinute), "도착", TimelineDot.PAST))
            add(TimelinePoint(nowF, clock(now), "지금", TimelineDot.NOW))
            plan.napMinutes?.let { add(TimelinePoint((elapsed + plan.totalMin - plan.wakeBeforeEndMin) / span.toFloat(), clock(end - plan.wakeBeforeEndMin), "기상 알림", TimelineDot.ALARM)) }
            add(TimelinePoint(1f, clock(end), if (charging) "충전 완료" else "다시 출발", TimelineDot.END))
        }
        return PauseScreenUi(
            meta = extra.stopName?.let { "${it}에서 쉬는 중" } ?: "쉬는 중",
            lead = if (charging) "충전이 끝날 때까지" else "${plan.totalMin}분 쉬어 가요",
            number = if (charging) plan.totalMin.toString() else plan.totalMin.toString(),
            unit = "분",
            battery = if (soc != null && t.chargeInfoAvailable) {
                BatteryUi(soc, CHARGE_TARGET, if (charging) (if (extra.chargeRatePerMin == null) "급속충전 중 (속도는 평균값)" else "급속충전 중") else "충전하지 않는 중", if (charging) "${clock(end)} 완료 예상" else null)
            } else null,
            restChoice = if (charging) null else extra.restChoice,
            activities = activities,
            timeline = timeline,
            nowFraction = nowF,
            notice = if (!t.chargeInfoAvailable && charging) "충전 정보 없음 — 쉬실 시간을 골라 주세요." else null,
            missions = t.missions.map { MissionUi(it.id, it.title, it.instructions, it.id in t.missionsDone, !it.reviewed) },
            radioUnlocked = t.radioUnlocked,
        )
    }

    fun fatigueIsHigh(t: TripUi) = t.fatigue?.level == FatigueLevel.HIGH
}
