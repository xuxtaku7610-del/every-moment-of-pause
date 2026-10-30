package com.emop.pause.trip

/**
 * 동물병원 화면 "증상 메모" (UPGRADE 3, F5-06). 버튼 고르기 + 받아쓰기(Android STT, 화면에 그대로 보이고 고칠 수 있음).
 * **기기에만 저장**(`SymptomMemoStore`), 도착 예정 공유는 [include]를 체크했을 때만 증상·메모·오늘 요약을 함께 보낸다.
 * 주행 중에는 화면 입력이 없고 음성만("콩이가 이상해요" 흐름이 증상 이벤트를 남긴다).
 */
data class VetMemo(
    val chips: Set<String> = emptySet(),
    val text: String = "",
    val include: Boolean = false,
) {
    fun toggle(chip: String) = copy(chips = if (chip in chips) chips - chip else chips + chip)

    fun withText(t: String) = copy(text = t.take(MAX_TEXT))

    /** 받아쓴 문장을 덧붙인다(앞 메모가 있으면 " / "로). 빈 문장은 무시. */
    fun append(heard: String): VetMemo {
        val h = heard.trim()
        if (h.isEmpty()) return this
        return withText(if (text.isBlank()) h else "${text.trim()} / $h")
    }

    /** 공유에 실을 값. 체크하지 않았으면 비어 있다. */
    data class Shared(val symptoms: List<String>, val memo: String?, val tripLine: String?)

    companion object {
        val CHIPS = listOf("구토", "설사", "헐떡임", "축 처짐", "떨림", "기타")
        const val MAX_TEXT = 120

        fun share(m: VetMemo, tripLine: String?): Shared =
            if (!m.include) Shared(emptyList(), null, null)
            else Shared(CHIPS.filter { it in m.chips }, m.text.trim().ifEmpty { null }, tripLine)

        /** 이동 기록에서 한 줄: "이동 2시간 10분 · 멀미 주의 1회(최고 46) · 헐떡임 1회". 아직 움직이지 않았으면 null. */
        fun tripLine(drivingMin: Int, cautionCount: Int, peak: Int, events: List<String>): String? {
            if (drivingMin <= 0) return null
            val h = drivingMin / 60
            val m = drivingMin % 60
            val time = if (h > 0) "${h}시간 ${m}분" else "${m}분"
            val motion = if (cautionCount > 0) "멀미 주의 ${cautionCount}회(최고 $peak)" else "멀미 주의 없음"
            val ev = events.groupingBy { it }.eachCount().map { (k, n) -> "$k ${n}회" }
            return (listOf("이동 $time", motion) + ev).joinToString(" · ")
        }
    }
}
