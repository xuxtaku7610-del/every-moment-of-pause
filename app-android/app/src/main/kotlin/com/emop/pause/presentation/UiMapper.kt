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
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.Reason
import com.emop.pause.engine.planner.RestStop
import com.emop.pause.runtime.Snapshot
import com.emop.pause.trip.TripUi
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
        val content: DriveContent = when {
            asking != null && asking.fatigueFirst -> DriveContent.Suggest(asking.minutesAway, asking.stop.name, listOf("졸음 위험"), urgent = true)
            asking != null -> DriveContent.Suggest(asking.minutesAway, asking.stop.name, asking.reasons.take(2).map { reasonText(it, name) })
            forecast != null -> DriveContent.Forecast(MotionPhrases.forecast(forecast, name))
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
        )
    }

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

    fun wait(w: WaitStatus, t: TripUi): WaitUi = WaitUi(
        headline = "주인이 곧 돌아와요",
        elapsed = elapsed(w.elapsedSec),
        // 실내 온도는 신호가 있을 때만 숫자로. 없으면 지어내지 않는다 (F3-06).
        cabin = w.cabinTempC?.let { "실내 ${it.roundToInt()}℃" } ?: "실내 온도 정보 없음",
        battery = w.batteryNow?.let { "배터리 $it%" },
        limitMin = w.limitSec / 60,
        overdue = w.overdue,
        share = ShareUi(t.share.pageUrl, t.share.connected),
    )

    // ───────────── 장소·병원 (F4·F5) ─────────────

    private val categoryKo = mapOf("restaurant" to "식당", "cafe" to "카페", "lodging" to "숙소", "tour" to "관광지", "walk" to "산책로")

    fun km(v: Double) = if (v >= 10) "${v.roundToInt()}km" else String.format(Locale.US, "%.1fkm", v)

    fun places(along: Found<PetPlace>?, near: Found<PetPlace>?, loading: Boolean): PlacesUi {
        val items = near?.items.orEmpty().map { p -> PlaceUi(p.id, p.name, categoryKo[p.category] ?: p.category, "목적지에서 ${km(p.offKm ?: 0.0)}", p.petPolicy, p.sample) } +
            along?.items.orEmpty().map { p -> PlaceUi(p.id, p.name, categoryKo[p.category] ?: p.category, "경로에서 ${km(p.offKm ?: 0.0)} · ${km(p.alongKm ?: 0.0)} 지점", p.petPolicy, p.sample) }
        val sample = items.any { it.sample } || listOfNotNull(along, near).any { it.source == PlaceSource.BUNDLED }
        return PlacesUi(
            "함께 가는 장소",
            items.distinctBy { it.id },
            if (sample) "샘플 데이터예요. 관광공사 반려동물 동반여행 키를 넣으면 실제 장소로 바뀌어요." else "한국관광공사 반려동물 동반여행 정보",
            loading,
        )
    }

    fun vets(found: Found<Vet>?, loading: Boolean) = VetsUi(
        found?.items.orEmpty().map { v -> VetUi(v.id, v.name, km(v.distanceKm), v.address, v.phone, v.sample) },
        if (found?.items.orEmpty().any { it.sample }) "샘플 데이터예요. 전화번호는 실제 인허가 데이터에서만 보여요." else "지방행정 인허가 데이터(동물병원)",
        loading,
    )

    // ───────────── 도착 (F6) ─────────────

    fun arrival(t: TripUi): ArrivalUi {
        val strong = t.arrival?.strong == true
        return ArrivalUi(
            question = "${t.pet.name} 내렸나요?",
            strong = strong,
            detail = if (strong) "차 안이 더워질 수 있어요. 꼭 확인해 주세요." else "내린 뒤 확인을 눌러 주세요.",
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
