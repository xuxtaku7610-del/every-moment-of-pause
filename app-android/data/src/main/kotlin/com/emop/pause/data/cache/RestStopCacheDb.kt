package com.emop.pause.data.cache

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.emop.pause.data.RestStopCache
import com.emop.pause.data.StopRecord
import com.emop.pause.engine.planner.Facility
import com.emop.pause.engine.planner.StopType

/** database.md §3 cached_rest_stops: 출발 시 경로 기준으로 통째로 교체한다. */
@Entity(tableName = "cached_rest_stops", primaryKeys = ["id"])
data class CachedRestStop(
    val id: String,
    val name: String,
    val stopType: String,
    val lat: Double,
    val lng: Double,
    val routeName: String? = null,
    val direction: String? = null,
)

/** database.md §3 cached_facilities. */
@Entity(tableName = "cached_facilities", primaryKeys = ["restStopId", "facilityCode"])
data class CachedFacility(val restStopId: String, val facilityCode: String, val quantity: Int)

@Dao
abstract class RestStopDao {
    @Query("SELECT * FROM cached_rest_stops")
    abstract suspend fun stops(): List<CachedRestStop>

    @Query("SELECT * FROM cached_facilities")
    abstract suspend fun facilities(): List<CachedFacility>

    @Query("DELETE FROM cached_rest_stops")
    abstract suspend fun clearStops()

    @Query("DELETE FROM cached_facilities")
    abstract suspend fun clearFacilities()

    @Insert
    abstract suspend fun insertStops(rows: List<CachedRestStop>)

    @Insert
    abstract suspend fun insertFacilities(rows: List<CachedFacility>)

    @Transaction
    open suspend fun replaceAll(stops: List<CachedRestStop>, facilities: List<CachedFacility>) {
        clearFacilities()
        clearStops()
        insertStops(stops)
        insertFacilities(facilities)
    }
}

@Database(entities = [CachedRestStop::class, CachedFacility::class], version = 2, exportSchema = false)
abstract class PauseDatabase : RoomDatabase() {
    abstract fun restStops(): RestStopDao
}

/** Room 캐시를 [RestStopCache]로 감싼다. */
class RoomRestStopCache(private val dao: RestStopDao) : RestStopCache {
    override suspend fun load(): List<StopRecord> {
        val fac = dao.facilities().groupBy({ it.restStopId }) { Facility.fromCode(it.facilityCode) }
        return dao.stops().mapNotNull { s ->
            val type = StopType.fromCode(s.stopType) ?: return@mapNotNull null
            StopRecord(s.id, s.name, type, s.lat, s.lng, fac[s.id].orEmpty().filterNotNull().toSet(), s.routeName, s.direction)
        }
    }

    override suspend fun replace(records: List<StopRecord>) {
        dao.replaceAll(
            records.map { CachedRestStop(it.id, it.name, it.type.code, it.lat, it.lng, it.routeName, it.direction) },
            records.flatMap { r -> r.facilities.map { CachedFacility(r.id, it.code, 1) } },
        )
    }
}
