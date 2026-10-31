package com.emop.pause.trip

import com.emop.pause.core.model.EtaInfo
import com.emop.pause.data.BundledRestStops
import com.emop.pause.data.alongRoute
import com.emop.pause.engine.motion.MotionCompare
import com.emop.pause.engine.motion.MotionConstants
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.mode.AppModeReducer
import com.emop.pause.mode.ModeInput
import com.emop.pause.mode.ModeState
import com.emop.pause.presentation.DemoScenes
import com.emop.pause.sim.Gpx
import com.emop.pause.sim.Scenario
import com.emop.pause.sim.ScenarioPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.math.abs

/**
 * 발표 숫자 맞추기 (docs/demo_numbers.md, DM-06). pet-trip을 처음부터 1초씩 돌려 장면마다 엔진 값(주행 자극)을 뽑고,
 * 화면끼리 어긋나면 안 되는 값을 확인한다: 예보 예상 ↔ 실제로 달린 뒤 지수 ↔ 고갯길 비교 "안내 없이",
 * 이동 기록 그래프 최고 ↔ 최고 지수, 멀미 주의 횟수 ↔ 주의 도달 시각.
 * 장면 값 표는 build/demo_numbers.txt에 쓴다(문서 "엔진 값" 열).
 */
class DemoNumbersTest {
    private val scenario = Scenario.parse(File("../../scenarios/pet-trip.json").readText())
    private val shape = RouteShape.fromLatLng(Gpx.parse(File("../../scenarios/${scenario.route.shapeGpx}").readText()))!!

    @Test
    fun `DM-06 장면마다 엔진 값이 서로 맞는다`() {
        val route = scenario.route
        val stops = BundledRestStops.parse(File("../../server/app/data/sample_reststops.json").readText())
            .alongRoute(route.points.map { it.toModel() }, route.totalDistanceM.toDouble())
        val brain = TripBrain(PresetStore.fromScenario(scenario.setup), shape, route.shapeStartM) { stops }
        val recorder = TripRecorder()
        var mode = ModeState()
        val scenes = DemoScenes.JUMPS.associateBy { it.tSec }
        val out = StringBuilder("장면\t시각\t주행 자극\t최근30분\t다음 굽은 길\t예보(시작·지속·예상·20분 곡선 최고)\n")
        var forecastAtScene: Pair<Int, Int>? = null
        for (t in 0..scenario.durationSec) {
            val f = ScenarioPlayer.frameAt(scenario, t.toDouble())
            val now = t * 1000L
            mode = AppModeReducer.reduce(mode, ModeInput(now, f.driveState, f.trip, f.sample.gear, f.sample.speedKph, f.sample.charging))
            val eta = if (f.remainDistanceM != null && f.remainTimeSec != null) EtaInfo(f.remainDistanceM!!, f.remainTimeSec!!) else null
            val s = brain.tick(BrainInput(now, 540 + t / 60, mode.mode, f.driveState, f.sample, scenario.capabilities, eta, route.totalDistanceM))
            s.speakForecast?.let { brain.onForecastSpoken(now) }
            s.pending?.let { brain.onShown(now); brain.onDeclined(now) }
            if (f.trip != com.emop.pause.core.model.TripPhase.NOT_STARTED) {
                recorder.onMotion(s.motion.index, s.motion.drivingSec, s.motion.firstCautionDrivingSec)
                recorder.tick(540 + t / 60, mode.mode, mode.pauseCause, s.fatigue?.score, s.fatigue?.continuousDriveMinutes, s.traveledKm, stops)
            }
            scenes[t]?.let { j ->
                val fc = s.forecast
                out.append("${j.label}\t$t\t${s.motion.index}\t${s.motionRecent.size}점 최고 ${s.motionRecent.maxOrNull() ?: "-"}\t")
                out.append("${s.nextCurveM?.let { "%.1fkm".format(it / 1000) } ?: "-"}\t")
                out.appendLine(fc?.let { "${"%.1f".format(it.startInM / 1000)}km · ${it.durationSec / 60}분 · 구간 끝 ${it.predictedIndex} · 곡선 최고 ${it.trajectory.max()} (${it.trajectory.size}점)" } ?: "-")
                if (j.label == "고갯길 예보") fc?.let { forecastAtScene = it.predictedIndex to it.trajectory.max() }
            }
        }
        val log = recorder.log()
        val compare = MotionCompare.run(shape, scenario.setup.pet!!.toModel())
        out.appendLine("이동 기록\t도착\t주행 ${log.drivingMin}분 · 휴식 ${log.visits.size}번 · 주의 ${log.cautionCount}번(처음 ${log.firstCautionMin}분) · 최고 ${log.motionPeak} · 그래프 ${log.motionSeries.size}점 최고 ${log.motionSeries.max()}")
        out.appendLine("고갯길 비교\t-\t안내 없이 ${compare.unguidedFinal} · 안내대로 ${compare.guidedFinal}")
        File("build/demo_numbers.txt").writeText(out.toString())
        println(out)

        val (predicted, curvePeak) = requireNotNull(forecastAtScene) { "고갯길 예보 장면에 예보가 없다" }
        // 예보는 고속도로 속도 그대로가 아니라 굽은 길 속도로 계산 → 실제로 달린 뒤 지수·비교 "안내 없이"와 같은 범위
        assertTrue(abs(predicted - log.motionPeak) <= 8, "예보 구간 끝 $predicted, 실제 최고 ${log.motionPeak}")
        assertTrue(abs(predicted - compare.unguidedFinal) <= 10, "예보 $predicted, 비교 안내 없이 ${compare.unguidedFinal}")
        assertTrue(curvePeak <= predicted + 3, "20분 곡선 최고 $curvePeak ≤ 구간 끝 $predicted")
        // 이동 기록: 그래프 최고 = 최고 지수, 주의 횟수와 도달 시각이 함께 있거나 함께 없다
        assertEquals(log.motionPeak, log.motionSeries.max())
        assertEquals(log.firstCautionMin != null, log.cautionCount > 0, "주의 ${log.cautionCount}번, 처음 ${log.firstCautionMin}")
        assertTrue(log.motionPeak >= MotionConstants.CAUTION)
        // 그래프는 여행 전체 (3시간 → 5분 점 30개 넘게)
        assertTrue(log.motionSeries.size >= (scenario.durationSec - 60) / 60 / TripRecorder.SAMPLE_MIN - 2, "점 ${log.motionSeries.size}개")
        assertEquals(1, log.visits.size)
        assertEquals("횡성휴게소", log.visits.first().name)
    }
}
