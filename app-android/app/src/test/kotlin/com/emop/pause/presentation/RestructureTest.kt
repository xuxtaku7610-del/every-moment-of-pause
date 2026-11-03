package com.emop.pause.presentation

import com.emop.pause.core.model.AppMode
import com.emop.pause.core.model.EtaInfo
import com.emop.pause.core.model.GuideIcon
import com.emop.pause.core.model.NextGuide
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.RouteTurn
import com.emop.pause.core.model.TurnKind
import com.emop.pause.engine.motion.MotionAnswer
import com.emop.pause.engine.motion.MotionCalibration
import com.emop.pause.engine.pet.DistressKind
import com.emop.pause.engine.pet.PetTripRecord
import com.emop.pause.runtime.Snapshot
import com.emop.pause.runtime.SourceMode
import com.emop.pause.trip.AutoCalibration
import com.emop.pause.trip.DistressRecord
import com.emop.pause.trip.LiveReports
import com.emop.pause.trip.StimulusSegment
import com.emop.pause.trip.StopVisit
import com.emop.pause.trip.TripLog
import com.emop.pause.trip.TripUi
import com.emop.pause.uistate.DriveContent
import com.emop.pause.uistate.GuideIconUi
import com.emop.pause.uistate.ServiceState
import com.emop.pause.uistate.TimelineKind
import com.emop.pause.uistate.TrendMarkKind
import com.emop.pause.uistate.TrendMarkUi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 2026-10-31 재정비 (docs/states.md): 관찰 상태는 음성 신고만(F7-06), 길 안내는 NaviHelper 실제 값만(F1-11),
 * 리포트 타임라인·그래프 표시·병원 공유(F7-08·F7-09), 개인 보정 자동 표본(F7-05b), 8개 상태 대응(ST-01).
 */
class RestructureTest {
    private val kong = TripUi(pet = PetProfile("콩이"))

    // ───── F7-06 콩이 관찰 상태 = 음성 신고만 ─────

    @Test
    fun `F7-06 신고가 없으면 관찰 기록 없음 (양호라고 쓰지 않는다), 주행 자극과 섞지 않는다`() {
        val o = UiMapper.observation(emptyList())
        assertEquals("관찰 기록 없음", o.line)
        assertEquals(0, o.count)
        assertFalse("양호" in o.line)
        // 주행 자극이 높아도 관찰 상태는 그대로
        val ui = UiMapper.drive(Snapshot(mode = AppMode.DRIVING), kong.copy(motion = com.emop.pause.engine.motion.MotionState(index = 80)))
        assertEquals("관찰 기록 없음", ui.observation.line)
    }

    @Test
    fun `F7-06 신고가 있으면 건수와 마지막 신고, 진단·확률 표현 없음`() {
        val o = UiMapper.observation(listOf(DistressRecord("헐떡임", 39), DistressRecord("구토", 134)))
        assertEquals("신고 2건 · 구토(출발 2시간 14분 뒤)", o.line)
        listOf("진단", "확률", "%", "멀미").forEach { assertFalse(it in o.line, it) }
    }

    // ───── F1-11 길 안내 = NaviHelper 실제 값만 ─────

    @Test
    fun `F1-11 다음 회전·남은 거리·도착은 NaviHelper 값 그대로, 없으면 숨긴다`() {
        val s = Snapshot(mode = AppMode.DRIVING, clockMinute = 17 * 60 + 11, eta = EtaInfo(194_000, 9180), guide = NextGuide(GuideIcon.LEFT, 80.0, " 기업지원허브 방면 "))
        val ui = UiMapper.drive(s, kong)
        assertNotNull(ui.guide)
        val g = ui.guide!!
        assertEquals(GuideIconUi.LEFT, g.icon)
        assertEquals("80m", g.distance)
        assertEquals("기업지원허브 방면", g.towards)
        assertFalse(g.demo)
        assertEquals("194km · 2시간 33분", ui.remain)
        assertEquals("오후 7:44", ui.arrive)
        // 실차에서 안내가 없으면 만들지 않는다
        val none = UiMapper.drive(s.copy(guide = null), kong.copy(demoTurn = RouteTurn(TurnKind.LEFT, 500.0)))
        assertNull(none.guide)
        // 시뮬레이터는 시연 안내(표시 "시연")
        val sim = UiMapper.drive(s.copy(guide = null, source = SourceMode.SIMULATION), kong.copy(demoTurn = RouteTurn(TurnKind.KEEP_RIGHT, 1240.0)))
        assertEquals(GuideIconUi.KEEP_RIGHT, sim.guide!!.icon)
        assertTrue(sim.guide!!.demo)
        assertEquals("1.2km", sim.guide!!.distance)
    }

    @Test
    fun `F1-11 도착 시각 - 자정 넘김과 오전 오후`() {
        assertEquals("오전 12:05", UiMapper.clock12(5))
        assertEquals("오전 9:00", UiMapper.clock12(9 * 60))
        assertEquals("오후 12:00", UiMapper.clock12(12 * 60))
        assertEquals("오전 1:10", UiMapper.arrive(23 * 60 + 40, EtaInfo(10_000, 90 * 60)))
        assertNull(UiMapper.arrive(600, null))
    }

    // ───── F7-08 리포트 타임라인·그래프 표시 ─────

    private val log = TripLog(
        visits = listOf(StopVisit("횡성휴게소", arrivedMinute = 540 + 91, stayedMin = 15, petWalk = true)),
        fatigueSeries = emptyList(), restMarks = emptyList(), peakFatigue = 0, longestDriveMin = 0, distanceKm = 193.6,
        startMinute = 540, motionSeries = listOf(0, 10, 55, 20), motionPeak = 55, firstCautionMin = 125, drivingMin = 163,
        segments = listOf(StimulusSegment(140, 158, 55)), elapsedMin = 178,
    )

    @Test
    fun `F7-08 타임라인은 시간순 - 출발·신고·휴식·흔들림 구간·도착, 신고는 정차 포함 시계로`() {
        val events = listOf(DistressRecord("헐떡임", 39, DistressKind.PANTING, speedKph = 98f))
        val tl = UiMapper.timeline(log, events, "강릉")
        assertEquals(listOf(TimelineKind.START, TimelineKind.REPORT, TimelineKind.REST, TimelineKind.STIMULUS, TimelineKind.ARRIVE), tl.map { it.kind })
        assertEquals("오전 9:00", tl[0].detail)
        assertEquals("0:39" to "헐떡임 신고", tl[1].time to tl[1].title)
        assertEquals("주행 중 · 시속 98km", tl[1].detail)
        assertEquals("1:31" to "횡성휴게소 15분", tl[2].time to tl[2].title)
        assertEquals("반려견 산책", tl[2].detail)
        assertEquals("흔들림 많은 구간 18분", tl[3].title)
        assertEquals("주행 자극 보통 · 최고 55", tl[3].detail)
        assertEquals("2:58" to "도착", tl[4].time to tl[4].title)
        // 신고 시각은 휴식 뒤면 휴식 길이만큼 밀린다 (주행 100분 → 출발 뒤 115분)
        assertEquals("1:55", UiMapper.timeline(log, listOf(DistressRecord("구토", 100)), null).first { it.kind == TimelineKind.REPORT }.time)
        // 속도를 모르면 속도 없이
        assertEquals("주행 중", UiMapper.timeline(log, listOf(DistressRecord("구토", 10)), null).first { it.kind == TimelineKind.REPORT }.detail)
    }

    @Test
    fun `F7-08 한 줄 문장의 시각은 타임라인 흔들림 구간 시각과 같은 계산 (주행 분을 따로 쓰지 않는다)`() {
        // 주행 134분에 처음 보통(정차 제외 시계)이어도 리포트는 정차 포함 시계 하나 — 타임라인 첫 구간 시작
        val l = log.copy(firstCautionMin = 134, segments = listOf(StimulusSegment(154, 170, 55), StimulusSegment(171, 175, 48)))
        val first = UiMapper.firstStimulusMin(l)
        assertEquals(154, first)
        val stimulusRow = UiMapper.timeline(l, emptyList(), null).first { it.kind == TimelineKind.STIMULUS }
        assertEquals(stimulusRow.minute, first)
        val line = com.emop.pause.engine.pet.PetTripLog.insight("콩이", PetTripRecord(0, 163, first, 55, 1), null)
        assertTrue(line.startsWith("이번엔 출발 2시간 34분 뒤"), line)
        assertEquals("2:34", stimulusRow.time)
        assertNull(UiMapper.firstStimulusMin(l.copy(segments = emptyList())))
    }

    @Test
    fun `F7-08 출발 시각을 모르면 타임라인·표시 없음, 그래프 표시는 휴식·신고·흔들림 구간`() {
        assertTrue(UiMapper.timeline(log.copy(startMinute = null), emptyList(), null).isEmpty())
        val marks = UiMapper.trendMarks(540, log.visits, listOf(DistressRecord("침 흘림·하품", 100)), log.segments)
        assertEquals(
            listOf(
                TrendMarkUi(TrendMarkKind.STOP, 91, "휴식", 15),
                TrendMarkUi(TrendMarkKind.SYMPTOM, 115, "침 흘림"),
                TrendMarkUi(TrendMarkKind.HIGH, 140, "흔들림 많음", 18),
            ),
            marks,
        )
    }

    @Test
    fun `F7-10 장면 점프 때 실시간 신고는 지우지 않고 이어서 - 같은 여행, 점프 끝 시각 안쪽만`() {
        // PT3: 2,400초 대본 신고(헐떡임) 뒤 2,410초에 보호자가 "콩이 토했어" → 10,843초 도착으로 점프
        val scripted = DistressRecord("헐떡임", 39, DistressKind.PANTING, simSec = 2400)
        val live = DistressRecord("구토", 39, DistressKind.VOMIT, simSec = 2410, live = true)
        val carried = LiveReports.carry(listOf(scripted, live), sameTrip = true, untilSec = 10_843)
        assertEquals(mapOf(2410 to listOf(live)), carried, "대본 신고는 대본으로 다시 쌓고, 실시간 신고만 이어서")
        // 뒤로 점프(신고 전 시각)·다른 시나리오면 남기지 않는다
        assertTrue(LiveReports.carry(listOf(live), sameTrip = true, untilSec = 2_400).isEmpty())
        assertTrue(LiveReports.carry(listOf(live), sameTrip = false, untilSec = 10_843).isEmpty())
        // 실차(시뮬레이터 시각 없음)는 점프가 없다
        assertTrue(LiveReports.carry(listOf(live.copy(simSec = null)), sameTrip = true, untilSec = 10_843).isEmpty())
        // 다시 넣은 신고는 리포트 타임라인·병원 공유·오늘 요약에 그대로 남는다
        val events = listOf(scripted, live)
        val tl = UiMapper.timeline(log, events, "강릉").filter { it.kind == TimelineKind.REPORT }.map { it.title }
        assertEquals(listOf("헐떡임 신고", "구토 신고"), tl)
        val share = UiMapper.reportShare(PetTripRecord(0, 163, 125, 55, 1), events, 194.0).first { it.key == "관찰 신고" }.value
        assertTrue("구토 1회" in share && "헐떡임 1회" in share, share)
        val today = com.emop.pause.trip.VetMemo.tripLine(163, 1, 55, events.map { UiMapper.symptomName(it.label) })!!
        assertTrue(today.endsWith("관찰 신고 헐떡임 1회, 구토 1회"), today)
    }

    // ───── F7-09 병원 공유 내용 ─────

    @Test
    fun `F7-09 병원 공유 - 관찰 신고(시각·주행 중), 주행 요약, 주행 자극은 차량 기준 표기`() {
        val r = PetTripRecord(0, drivingMin = 163, firstCautionMin = 125, motionPeak = 55, stops = 1)
        val rows = UiMapper.reportShare(r, listOf(DistressRecord("헐떡임", 39), DistressRecord("헐떡임", 70)), 193.6).associate { it.key to it.value }
        assertEquals("헐떡임 2회 (출발 39분 뒤, 주행 중)", rows["관찰 신고"])
        assertEquals("2시간 43분(휴식 제외) · 194km · 휴식 1번", rows["주행"])
        assertEquals("최고 55 (차량 흔들림(가속·제동·회전) 기준 · 반려견 측정 아님)", rows["주행 자극"])
        assertEquals("없음", UiMapper.reportShare(r, emptyList(), null).first().value)
    }

    // ───── F7-05b 개인 보정 자동 표본 ─────

    @Test
    fun `F7-05b 신고에서 자동 표본 - 구토 예, 침·헐떡임 조금, 다른 신고는 없음, 한 여행에 더 강한 답만`() {
        val a = AutoCalibration()
        assertEquals(MotionAnswer.LITTLE, a.onReport(DistressKind.PANTING, 30)!!.answer)
        assertNull(a.onReport(DistressKind.NAUSEA, 30), "같은 세기는 다시 남기지 않는다")
        assertNull(a.onReport(DistressKind.ANXIOUS, 30))
        val y = a.onReport(DistressKind.VOMIT, 55)!!
        assertEquals(MotionAnswer.YES to 55, y.answer to y.peak)
        assertEquals(1.0, y.weight)
        assertNull(a.onReport(DistressKind.VOMIT, 60))
    }

    @Test
    fun `F7-05b 높음 구간 뒤 30분 신고 없으면 괜찮았음(약하게) 한 번, 신고가 있으면 없음, 높음이 없으면 없음`() {
        val a = AutoCalibration()
        var t = 0L
        fun run(sec: Int, index: Int) = (0 until sec).mapNotNull { a.onTick(t++, index) }
        assertTrue(run(600, 50).isEmpty(), "보통만으로는 표본 없음")
        assertTrue(run(120, 75).isEmpty())
        assertTrue(run(MotionCalibration.QUIET_WINDOW_SEC - 1, 30).isEmpty())
        val quiet = run(5, 30)
        assertEquals(1, quiet.size)
        assertEquals(MotionAnswer.NO, quiet[0].answer)
        assertEquals(75, quiet[0].peak)
        assertEquals(MotionCalibration.QUIET_WEIGHT, quiet[0].weight)
        assertTrue(quiet[0].weight <= 0.5)
        // 두 번째 높음 구간은 다시 남기지 않는다 (한 여행에 한 번)
        run(60, 80)
        assertTrue(run(MotionCalibration.QUIET_WINDOW_SEC + 10, 20).isEmpty())

        val b = AutoCalibration()
        t = 0
        (0 until 60).forEach { b.onTick(t++, 80) }
        (0 until 600).forEach { b.onTick(t++, 30) }
        b.onReport(DistressKind.PANTING, 80)
        assertTrue((0 until MotionCalibration.QUIET_WINDOW_SEC).mapNotNull { b.onTick(t++, 30) }.isEmpty(), "30분 안에 신고가 있으면 괜찮았음 아님")
    }

    // ───── ST-01 8개 상태 ─────

    @Test
    fun `ST-01 화면과 주행 내용으로 8개 상태를 정한다`() {
        val r = ScreenRouter
        assertEquals(ServiceState.DEPART, r.serviceState(Route.DEPART, null, false))
        assertEquals(ServiceState.DRIVING, r.serviceState(Route.DRIVE, DriveContent.Calm("x"), false))
        assertEquals(ServiceState.FORECAST, r.serviceState(Route.DRIVE, DriveContent.Forecast("3km 뒤 굽은 길 · 8분", "천천히 가면 덜 흔들려요"), false))
        assertEquals(ServiceState.SUGGEST, r.serviceState(Route.DRIVE, DriveContent.Suggest(9, "횡성휴게소", emptyList()), false))
        assertEquals(ServiceState.DISTRESS, r.serviceState(Route.DRIVE, DriveContent.Distress("x", null, null), false))
        assertEquals(ServiceState.WAIT, r.serviceState(Route.WAIT, null, false))
        assertEquals(ServiceState.VET, r.serviceState(Route.DRIVE, DriveContent.Vet("병원", "1km", 4), true))
        assertEquals(ServiceState.VET, r.serviceState(Route.VETS, null, true))
        assertEquals(ServiceState.STOPPED, r.serviceState(Route.VETS, null, false))
        assertEquals(ServiceState.REPORT, r.serviceState(Route.ARRIVAL, null, false))
        assertEquals(ServiceState.REPORT, r.serviceState(Route.TRIPLOG, null, false))
        assertEquals(ServiceState.STOPPED, r.serviceState(Route.STIMULUS, null, false))
        assertEquals((1..8).toList(), ServiceState.entries.map { it.no }.filter { it > 0 })
    }

    // ───── F5-12 병원 이동: 제휴(시연) 병원만 접수, 전달 동의 ─────

    @Test
    fun `F5-12 시연용 제휴 병원은 시뮬레이터에서만 맨 앞, 실차 목록은 실제 병원 그대로`() {
        val real = com.emop.pause.data.Vet("v1", "헤세드동물병원", 37.0, 128.0, 1.5, null, "033-000-0000", sample = false)
        val at = com.emop.pause.core.model.LatLng(37.0, 128.0)
        val sim = com.emop.pause.trip.DemoPartner.prepend(listOf(real), at, simulation = true)
        assertEquals(listOf("시연용 제휴 동물병원", "헤세드동물병원"), sim.map { it.name })
        assertTrue(sim[0].partner)
        assertFalse(sim[1].partner)
        assertEquals(listOf(real), com.emop.pause.trip.DemoPartner.prepend(listOf(real), at, simulation = false))
        // 화면: 제휴 표시는 시연 병원만
        val ui = UiMapper.vets(com.emop.pause.data.Found(sim, com.emop.pause.data.PlaceSource.SERVER), false)
        assertEquals(listOf(true, false), ui.items.map { it.partner })
    }

    @Test
    fun `F5-12 전달 동의 - 음성 답 해석과 보낼 내용 미리보기(위치는 보내지 않음)`() {
        val c = com.emop.pause.trip.DemoPartner::consent
        assertEquals(true, c("네 보내줘"))
        assertEquals(true, c("응"))
        assertEquals(false, c("아니요"))
        assertEquals(false, c("보내지 마 안 돼"))
        assertNull(c("뭐라고?"))
        assertNull(c(null))
        val memo = com.emop.pause.trip.VetMemo(chips = setOf("구토"), text = "두 번", include = true)
        val rows = UiMapper.vetPreview("콩이", memo, "주행 39분(휴식 제외) · 주행 자극 최고 4 · 관찰 신고 헐떡임 1회", 4).associate { it.key to it.value }
        assertEquals("콩이", rows["반려견"])
        assertEquals("구토", rows["증상"])
        assertEquals("두 번", rows["메모"])
        assertEquals("약 4분 뒤 (자동 갱신)", rows["도착 예정"])
        assertEquals("위치·경로·사진", rows["보내지 않음"])
        // 함께 보내기를 끄면 증상·메모·요약은 빠진다
        assertNull(UiMapper.vetPreview("콩이", memo.copy(include = false), "x", 4).firstOrNull { it.key == "증상" })
    }
}
