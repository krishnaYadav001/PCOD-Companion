package com.pcodcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pcodcompanion.data.local.entity.DailyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatestLogSync(): DailyLog?

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    fun getLogByDate(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date = :date LIMIT 1")
    suspend fun getLogByDateSync(date: String): DailyLog?

    @Query("SELECT EXISTS(SELECT 1 FROM daily_logs WHERE date = :date)")
    suspend fun existsForDate(date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(log: DailyLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Update
    suspend fun updateLog(log: DailyLog)

    @Delete
    suspend fun deleteLog(log: DailyLog)

    @Query("SELECT * FROM daily_logs ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int): List<DailyLog>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    suspend fun getAllLogsSnapshot(): List<DailyLog>

    @Query("DELETE FROM daily_logs")
    suspend fun deleteAllLogs()
}
