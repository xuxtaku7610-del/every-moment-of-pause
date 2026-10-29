package com.emop.pause.data

import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.StopType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 앱 내장 기본 휴게소 (assets/reststops_default.json = server/app/data/sample_reststops.json).
 * 서버도 캐시도 없을 때 마지막으로 쓴다 (test_plan 4장 P0).
 */
object BundledRestStops {
    const val ASSET = "reststops_default.json"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Area(
        val serviceAreaCode: String,
        val serviceAreaName: String,
        val stopType: String = "service_area",
        val yValue: Double,
        val xValue: Double,
        val routeName: String? = null,
        val direction: String? = null,
    )

    @Serializable private data class Fac(val serviceAreaCode: String, val facilityCode: String, val quantity: Int = 1)
    @Serializable private data class Doc(val restAreas: List<Area>, val facilities: List<Fac> = emptyList())

    fun parse(text: String): List<StopRecord> {
        val doc = json.decodeFromString(Doc.serializer(), text)
        val fac = doc.facilities.filter { it.quantity > 0 }.groupBy({ it.serviceAreaCode }) { Facility.fromCode(it.facilityCode) }
        return doc.restAreas.mapNotNull { a ->
            val type = StopType.fromCode(a.stopType) ?: return@mapNotNull null
            // 서버 수집 잡과 같은 ID 규칙 (rs_ + 코드 소문자)
            StopRecord(
                "rs_${a.serviceAreaCode.lowercase()}", a.serviceAreaName, type, a.yValue, a.xValue,
                fac[a.serviceAreaCode].orEmpty().filterNotNull().toSet(),
                a.routeName?.ifBlank { null },
                a.direction?.ifBlank { null },
            )
        }
    }
}
