package com.emop.pause.data.pet

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.emop.pause.core.model.MotionTendency
import com.emop.pause.core.model.PetProfile
import com.emop.pause.core.model.PetSize
import com.emop.pause.engine.pet.PetTripRecord

/**
 * 개인 데이터 DB (database.md §3). 휴게소 캐시(`pause.db`, 스키마가 바뀌면 지움)와 나눠서
 * 반려견 프로필·이동 기록이 캐시 정리 때 함께 지워지지 않게 한다. **서버로 보내지 않는다.**
 */
@Entity(tableName = "pet_profiles")
data class PetProfileRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val size: String,
    val ageYears: Int,
    val motionTendency: String,
    val pottyIntervalMin: Int,
    /** 쉼표로 구분한 시설 코드. */
    val facilityPrefs: String,
    val isDefault: Boolean = true,
    val updatedAt: Long,
)

@Entity(tableName = "local_trips")
data class LocalTripRow(
    @PrimaryKey val id: String,
    val petId: Long,
    val startedAt: Long,
    val endedAt: Long?,
    val drivingMin: Int,
    val firstCautionMin: Int?,
    val motionPeak: Int,
    val stops: Int,
    val symptomNote: String?,
)

@Entity(tableName = "motion_samples", primaryKeys = ["localTripId", "minute"])
data class MotionSampleRow(val localTripId: String, val minute: Int, val motionIndex: Int, val resting: Boolean)

@Dao
abstract class PetDao {
    @Query("SELECT * FROM pet_profiles ORDER BY id LIMIT 1") // v10-3: 첫 프로필(저장 순서) = 기록이 붙는 프로필
    abstract suspend fun defaultProfile(): PetProfileRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertProfile(row: PetProfileRow): Long

    /** v10-3 여러 마리: 저장 순서(id)대로 최대 3. 기존 한 마리는 그대로 첫 프로필이다(스키마 변경 없음). */
    @Query("SELECT * FROM pet_profiles ORDER BY id LIMIT 3")
    abstract suspend fun allProfiles(): List<PetProfileRow>

    @Query("DELETE FROM pet_profiles WHERE id = :id")
    abstract suspend fun deleteProfile(id: Long)

    @Query("SELECT * FROM local_trips WHERE petId = :petId ORDER BY startedAt DESC LIMIT :limit")
    abstract suspend fun recentTrips(petId: Long, limit: Int): List<LocalTripRow>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertTrip(row: LocalTripRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSamples(rows: List<MotionSampleRow>)

    @Query("SELECT * FROM motion_samples WHERE localTripId = :tripId ORDER BY minute")
    abstract suspend fun samples(tripId: String): List<MotionSampleRow>

    @Query("DELETE FROM motion_samples")
    abstract suspend fun clearSamples()

    @Query("DELETE FROM local_trips")
    abstract suspend fun clearTrips()

    @Query("DELETE FROM pet_profiles")
    abstract suspend fun clearProfiles()

    @Transaction
    open suspend fun saveTrip(trip: LocalTripRow, samples: List<MotionSampleRow>) {
        upsertTrip(trip)
        insertSamples(samples)
    }

    /** 설정 "내 데이터 삭제". */
    @Transaction
    open suspend fun clearAll() {
        clearSamples()
        clearTrips()
        clearProfiles()
    }
}

@Database(entities = [PetProfileRow::class, LocalTripRow::class, MotionSampleRow::class], version = 1, exportSchema = false)
abstract class PetDatabase : RoomDatabase() {
    abstract fun pets(): PetDao
}

/** Room 행 ↔ 도메인. 순수 함수라 단위 테스트한다. */
object PetRows {
    fun toRow(p: PetProfile, id: Long = 0, nowMs: Long) = PetProfileRow(
        id = id,
        name = p.name.trim(),
        size = p.size.name,
        ageYears = p.ageYears,
        motionTendency = p.motionTendency.name,
        pottyIntervalMin = p.pottyIntervalMin,
        facilityPrefs = p.facilityPrefs.sorted().joinToString(","),
        updatedAt = nowMs,
    )

    /** 모르는 값(앱 업데이트로 바뀐 enum 등)은 기본값으로 읽는다. */
    fun toProfile(r: PetProfileRow) = PetProfile(
        name = r.name,
        size = PetSize.entries.firstOrNull { it.name == r.size } ?: PetSize.MEDIUM,
        ageYears = r.ageYears,
        motionTendency = MotionTendency.entries.firstOrNull { it.name == r.motionTendency } ?: MotionTendency.NORMAL,
        pottyIntervalMin = r.pottyIntervalMin,
        facilityPrefs = r.facilityPrefs.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        id = r.id,
    )

    fun toRecord(r: LocalTripRow) = PetTripRecord(r.startedAt, r.drivingMin, r.firstCautionMin, r.motionPeak, r.stops, r.symptomNote)
}
