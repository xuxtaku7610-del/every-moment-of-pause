package com.emop.pause.uistate

/**
 * 함께 가는 장소 필터 (UPGRADE 6). 분류 칩: 전체 / 카페 / 식당 / 숙소 / 산책·관광.
 * "대형견 가능"·"실내 동반 가능"은 응답 글에 **명시된 곳(true)만** 보이고, 모르는 곳(null)은 빼되 몇 곳인지 따로 알린다.
 * 화면 안에서만 쓰는 선택 상태라 계약 밖(`rememberSaveable`) — 계산은 여기서 한다.
 */
object PlaceFilter {
    const val ALL = "전체"
    val GROUPS = listOf("카페" to setOf("카페"), "식당" to setOf("식당"), "숙소" to setOf("숙소"), "산책·관광" to setOf("산책로", "관광지"))

    data class Result(val shown: List<PlaceUi>, val unknown: Int)

    fun groups(items: List<PlaceUi>): List<Pair<String, Int>> =
        listOf(ALL to items.size) + GROUPS.map { (g, cats) -> g to items.count { it.category in cats } }.filter { it.second > 0 }

    private fun inGroup(p: PlaceUi, group: String?) =
        group == null || group == ALL || GROUPS.firstOrNull { it.first == group }?.second?.contains(p.category) == true

    fun apply(items: List<PlaceUi>, group: String?, largeDog: Boolean, indoor: Boolean): Result {
        val g = items.filter { inGroup(it, group) }
        val shown = g.filter { (!largeDog || it.largeDogOk == true) && (!indoor || it.indoorOk == true) }
        val unknown = g.count { (largeDog && it.largeDogOk == null) || (indoor && it.indoorOk == null) }
        return Result(shown, unknown)
    }

    /** "대형견 정보 없음 2곳·실내 정보 없음 4곳은 빼고 보여요" — 필터를 켰을 때만. */
    fun unknownNote(items: List<PlaceUi>, group: String?, largeDog: Boolean, indoor: Boolean): String? {
        val g = items.filter { inGroup(it, group) }
        val parts = listOfNotNull(
            g.count { it.largeDogOk == null }.takeIf { largeDog && it > 0 }?.let { "대형견 정보 없음 ${it}곳" },
            g.count { it.indoorOk == null }.takeIf { indoor && it > 0 }?.let { "실내 정보 없음 ${it}곳" },
        )
        return if (parts.isEmpty()) null else parts.joinToString("·") + "은 빼고 보여요"
    }
}
