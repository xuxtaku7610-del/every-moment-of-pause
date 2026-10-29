package com.emop.pause.trip

import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.engine.motion.MotionCompare
import com.emop.pause.engine.motion.RouteShape
import com.emop.pause.sim.Gpx
import com.emop.pause.sim.Scenario
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * F1-07 시연 비교: 대관령 옛길(OSM, scenarios/geo/mountain_road.gpx)을 "안내 없이" vs "안내대로".
 * 결과 CSV는 build/demo_compare/에 쓰고, docs/plan/demo_compare.md·demo_compare.csv로 옮겨 둔다.
 */
class DemoCompareTest {
    private val shape = RouteShape.fromLatLng(Gpx.parse(File("../../scenarios/geo/mountain_road.gpx").readText()))!!

    @Test
    fun `F1-07 안내대로 가면 멀미 지수가 낮다 (시연 반려견과 보통 반려견)`() {
        val scenarioPet = Scenario.parse(File("../../scenarios/pet-trip.json").readText()).setup.pet!!.toModel()
        val normal = PetProfile("보통", PetSize.MEDIUM, 3, MotionTendency.NORMAL)
        val out = File("build/demo_compare").apply { mkdirs() }
        val summary = StringBuilder()
        for ((label, pet) in listOf("demo" to scenarioPet, "normal" to normal)) {
            val r = MotionCompare.run(shape, pet)
            assertTrue(r.guidedFinal < r.unguidedFinal, "$label: ${r.unguidedFinal} → ${r.guidedFinal}")
            assertTrue(r.guidedPeak <= r.unguidedPeak)
            assertTrue(r.guidedSec > r.unguidedSec, "안내대로는 느리게 가므로 더 걸린다")
            File(out, "demo_compare_$label.csv").writeText(MotionCompare.csv(r))
            summary.appendLine("$label length=${"%.0f".format(shape.lengthM)} guidedMinKph=${r.guidedMinKph} guidedAvgKph=${r.guidedAvgKph} unguided=${r.unguidedFinal}(peak ${r.unguidedPeak}, ${r.unguidedSec}s) guided=${r.guidedFinal}(peak ${r.guidedPeak}, ${r.guidedSec}s)")
        }
        File(out, "summary.txt").writeText(summary.toString())
        println(summary)
    }
}
