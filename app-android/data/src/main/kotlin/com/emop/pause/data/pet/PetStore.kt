package com.emop.pause.data.pet

import com.emop.pause.core.model.PetProfile
import com.emop.pause.engine.pet.PetTripRecord
import com.emop.pause.engine.pet.PetTripLog

/** 반려견 프로필·이동 기록 저장소. 기기에만 저장한다 (architecture §6). */
interface PetStore {
    suspend fun profile(): PetProfile?
    suspend fun saveProfile(p: PetProfile)
    suspend fun recentTrips(limit: Int = PetTripLog.RECENT): List<PetTripRecord>
    suspend fun saveTrip(id: String, record: PetTripRecord, endedAt: Long?, series: List<Pair<Int, Int>>)
    suspend fun clear()
}

class RoomPetStore(private val dao: PetDao, private val clock: () -> Long = System::currentTimeMillis) : PetStore {
    private var cachedId: Long? = null

    override suspend fun profile(): PetProfile? = dao.defaultProfile()?.also { cachedId = it.id }?.let(PetRows::toProfile)

    override suspend fun saveProfile(p: PetProfile) {
        val id = cachedId ?: dao.defaultProfile()?.id ?: 0
        cachedId = dao.upsertProfile(PetRows.toRow(p, id, clock()))
    }

    override suspend fun recentTrips(limit: Int): List<PetTripRecord> {
        val id = cachedId ?: dao.defaultProfile()?.id ?: return emptyList()
        return dao.recentTrips(id, limit).map(PetRows::toRecord)
    }

    /** [series]는 (주행 분, 멀미 지수) — 5분 간격 기록 그래프. */
    override suspend fun saveTrip(id: String, record: PetTripRecord, endedAt: Long?, series: List<Pair<Int, Int>>) {
        val petId = cachedId ?: dao.defaultProfile()?.id ?: 0
        dao.saveTrip(
            LocalTripRow(id, petId, record.startedMs, endedAt, record.drivingMin, record.firstCautionMin, record.motionPeak, record.stops, record.symptomNote),
            series.map { (minute, index) -> MotionSampleRow(id, minute, index, resting = false) },
        )
    }

    override suspend fun clear() {
        dao.clearAll()
        cachedId = null
    }
}
