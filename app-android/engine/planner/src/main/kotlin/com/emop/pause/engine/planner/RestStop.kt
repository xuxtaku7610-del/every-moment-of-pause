package com.emop.pause.engine.planner

enum class StopType(val code: String) {
    SERVICE_AREA("service_area"),
    DROWSY_SHELTER("drowsy_shelter");

    companion object {
        fun fromCode(code: String): StopType? = entries.firstOrNull { it.code == code }
    }
}

/** 편의시설 코드 (database.md facility_codes). */
enum class Facility(val code: String, val labelKo: String) {
    NURSING_ROOM("nursing_room", "수유실"),
    KIDS_ZONE("kids_zone", "키즈존"),
    PET_ZONE("pet_zone", "반려동물 놀이터"),
    EV_FAST("ev_fast", "급속충전기"),
    EV_SLOW("ev_slow", "완속충전기"),
    SHOWER("shower", "샤워실"),
    ACCESSIBLE_TOILET("accessible_toilet", "장애인 화장실"),
    SLEEP_ROOM("sleep_room", "수면실");

    companion object {
        /** 모르는 코드는 버린다 (서버가 새 코드를 추가해도 앱이 깨지지 않게). */
        fun fromCode(code: String): Facility? = entries.firstOrNull { it.code == code }
    }
}

/** 경로상 휴게소·졸음쉼터 하나. distanceAlongRouteKm는 출발점 기준 경로 거리. */
data class RestStop(
    val id: String,
    val name: String,
    val type: StopType,
    val distanceAlongRouteKm: Double,
    val facilities: Set<Facility> = emptySet(),
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    /** "경부선 부산방향" 같은 노선·방향 (모르면 null). */
    val routeLabel: String? = null,
)
