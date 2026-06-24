package com.pcodcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pcodcompanion.data.local.entity.Insight
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights ORDER BY createdAtMillis DESC")
    fun getAllInsights(): Flow<List<Insight>>

    @Query("SELECT * FROM insights ORDER BY createdAtMillis DESC")
    suspend fun getAllInsightsSnapshot(): List<Insight>

    /** New patterns are added with INSERT OR IGNORE so re-detection is a no-op. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(insight: Insight): Long

    @Query("UPDATE insights SET lastShownAtMillis = :now, showCount = showCount + 1 WHERE id = :id")
    suspend fun markShown(id: Long, now: Long)

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
