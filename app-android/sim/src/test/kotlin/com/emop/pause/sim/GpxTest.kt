package com.emop.pause.sim

import com.emop.pause.core.model.LatLng
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GpxTest {
    @Test
    fun `DM GPX trkpt·rtept 좌표를 순서대로 읽는다`() {
        val text = """
            <gpx><trk><trkseg>
              <trkpt lat="37.70" lon="128.40"><ele>700</ele></trkpt>
              <trkpt lon="128.41" lat="37.71"/>
            </trkseg></trk><rte><rtept lat="37.72" lon="128.42"></rtept></rte></gpx>
        """.trimIndent()
        assertEquals(listOf(LatLng(37.70, 128.40), LatLng(37.71, 128.41), LatLng(37.72, 128.42)), Gpx.parse(text))
    }
}
