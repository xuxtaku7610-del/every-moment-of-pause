package com.emop.pause.engine.fatigue

import com.emop.pause.core.model.DriveState
import com.emop.pause.core.model.Signal
import com.emop.pause.core.model.SignalSample
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FatigueEngineTest {
    private val allCaps = Signal.entries.toSet()

    /** fromSec부터 1초 간격으로 [seconds]번 갱신한다(첫 갱신은 경과 0초). startMinute는 현지 시각(분). */
    private fun FatigueEngine.run(
        fromSec: Int,
        seconds: Int,
        startMinute: Int = 9 * 60,
        drive: DriveState = DriveState.DRIVING,
        speed: Float? = 100f,
        temp: Float? = 22f,
        recirc: Boolean? = false,
        caps: Set<Signal> = allCaps,
        onEach: (Int, FatigueResult) -> Unit = { _, _ -> },
    ): FatigueResult {
        var r: FatigueResult? = null
        for (t in fromSec until fromSec + seconds) {
            val sample = SignalSample(t * 1000L, speedKph = speed, hvacTempC = temp, recirculationOn = recirc)
            r = update(FatigueInput(t * 1000L, startMinute + t / 60, drive, sample, caps))
            onEach(t, r)
        }
        return r!!
    }

    @Test
    fun `P0 연속 주행 0~120분 동안 점수가 단조 증가하고 2시간 근처에서 HIGH`() {
        val e = FatigueEngine()
        var prev = -1
        var firstHighMin: Int? = null
        e.run(0, 125 * 60) { t, r ->
            assertTrue(r.score >= prev, "t=$t score ${r.score} < $prev")
            prev = r.score
            if (firstHighMin == null && r.level == FatigueLevel.HIGH) firstHighMin = t / 60
        }
        // 오전·쾌적한 실내·수면 입력 없음: 연속 주행이 주원인, 단조로움(고속 정속)이 거든다.
        assertTrue(firstHighMin!! in 95..121, "HIGH 도달 $firstHighMin 분")
    }

    @Test
    fun `P0 상위 기여 요인 3개 - 연속 주행, 오후 2~4시, 난방`() {
        val e = FatigueEngine()
        val r = e.run(0, 110 * 60 + 1, startMinute = 13 * 60 + 50, temp = 27f, speed = 60f)
        assertEquals(
            listOf(FatigueFactor.CONTINUOUS_DRIVE, FatigueFactor.TIME_OF_DAY, FatigueFactor.CABIN_WARMTH),
            r.topContributors.map { it.factor },
        )
        assertEquals(110, r.topContributors[0].detailMinutes)
        assertEquals(110, r.topContributors[2].detailMinutes)
        assertEquals(3, r.topContributors.size)
    }

    @Test
    fun `P1 휴게소 15분 이상 정차 후 출발하면 누적값이 리셋 비율만큼 줄어든다`() {
        val e = FatigueEngine()
        val before = e.run(0, 100 * 60)
        e.run(100 * 60, 15 * 60, drive = DriveState.PARKED, speed = 0f)
        val after = e.run(115 * 60, 1)
        assertEquals((100 * FatigueEngine.RESET_KEEP).toInt(), after.continuousDriveMinutes)
        assertTrue(after.score < before.score / 2, "${after.score} vs ${before.score}")
    }

    @Test
    fun `정체로 2분 정차(PARKED)는 연속 주행을 리셋하지 않는다`() {
        val e = FatigueEngine()
        e.run(0, 60 * 60)
        e.run(3600, 120, drive = DriveState.PARKED, speed = 0f)
        val r = e.run(3720, 60)
        assertEquals(60, r.continuousDriveMinutes) // 59분 59초 + 정차 후 60초
    }

    @Test
    fun `P1 수면 5시간 입력이 7시간 입력보다 높게 계산된다`() {
        val short = FatigueEngine(sleepHours = 5f).run(0, 60 * 60)
        val enough = FatigueEngine(sleepHours = 7f).run(0, 60 * 60)
        assertTrue(short.score > enough.score, "${short.score} vs ${enough.score}")
        assertTrue(FatigueFactor.SLEEP_DEBT in short.topContributors.map { it.factor })
    }

    @Test
    fun `P0 공조 신호 미지원 차종은 해당 요인을 빼고 재정규화한다`() {
        val caps = setOf(Signal.SPEED)
        val r = FatigueEngine().run(0, 90 * 60, temp = null, recirc = null, caps = caps)
        assertFalse(FatigueFactor.CABIN_WARMTH in r.activeFactors)
        assertEquals(setOf(FatigueFactor.CONTINUOUS_DRIVE, FatigueFactor.TIME_OF_DAY, FatigueFactor.MONOTONY), r.activeFactors)
        assertFalse(r.signalLost)
        // 남은 보조 요인(시간대 0.30 · 단조로움 0.25)으로 재정규화: 단조로움 1.0 → 30 × 0.25/0.55
        val expected = 90 * 70.0 / 120 + 30 * 0.25 / 0.55
        assertEquals(Math.round(expected).toInt(), r.score)
    }

    @Test
    fun `신호가 전혀 없는 차도 시간 기반으로 계산하고 크래시하지 않는다`() {
        val r = FatigueEngine().run(0, 60 * 60, speed = null, temp = null, recirc = null, caps = emptySet())
        assertEquals(setOf(FatigueFactor.CONTINUOUS_DRIVE, FatigueFactor.TIME_OF_DAY), r.activeFactors)
        assertEquals(35, r.score)
    }

    @Test
    fun `P1 지원 신호가 30초 넘게 끊기면 신호 없음으로 바꾸고 시간 기반만 쓴다`() {
        val e = FatigueEngine()
        e.run(0, 60 * 60)
        val held = e.run(3600, 30, speed = null, temp = null, recirc = null)
        assertFalse(held.signalLost, "30초까지는 마지막 값 유지")
        val lost = e.run(3630, 2, speed = null, temp = null, recirc = null)
        assertTrue(lost.signalLost)
        assertEquals(setOf(FatigueFactor.CONTINUOUS_DRIVE, FatigueFactor.TIME_OF_DAY), lost.activeFactors)
    }

    @Test
    fun `P2 시계가 거꾸로 가도 연속 주행 시간이 줄거나 음수가 되지 않는다`() {
        val e = FatigueEngine()
        e.run(0, 600)
        val sample = SignalSample(0, speedKph = 100f)
        val r = e.update(FatigueInput(-3_600_000L, 9 * 60, DriveState.DRIVING, sample, allCaps))
        assertEquals(9, r.continuousDriveMinutes)
        val next = e.update(FatigueInput(-3_599_000L, 9 * 60, DriveState.DRIVING, sample, allCaps))
        assertTrue(next.continuousDriveMinutes >= 9)
    }

    @Test
    fun `시간대 요인은 새벽과 오후 식곤증 시간에 높다`() {
        assertEquals(1.0, FatigueEngine.timeOfDay(3 * 60))
        assertEquals(0.8, FatigueEngine.timeOfDay(14 * 60 + 30))
        assertEquals(0.0, FatigueEngine.timeOfDay(10 * 60))
        assertEquals(0.6, FatigueEngine.timeOfDay(23 * 60 + 10))
        assertEquals(0.8, FatigueEngine.timeOfDay(24 * 60 + 30), "자정을 넘긴 분도 처리")
    }

    @Test
    fun `범위 밖 수면 시간은 무시한다`() {
        val e = FatigueEngine()
        e.sleepHours = 25f
        val r = e.run(0, 60)
        assertFalse(FatigueFactor.SLEEP_DEBT in r.activeFactors)
    }
}
