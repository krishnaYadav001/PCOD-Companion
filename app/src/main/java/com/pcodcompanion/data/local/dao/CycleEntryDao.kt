package com.pcodcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pcodcompanion.data.local.entity.CycleEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleEntryDao {
    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC")
    fun getAllEntries(): Flow<List<CycleEntry>>

    @Query("SELECT * FROM cycle_entries WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    fun getActiveCycle(): Flow<CycleEntry?>

    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC LIMIT 1")
    fun getLatestEntry(): Flow<CycleEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CycleEntry)

    @Update
    suspend fun updateEntry(entry: CycleEntry)

    @Delete
    suspend fun deleteEntry(entry: CycleEntry)

    @Query("SELECT * FROM cycle_entries ORDER BY startDate DESC")
    suspend fun getAllEntriesSnapshot(): List<CycleEntry>

    @Query("DELETE FROM cycle_entries")
    suspend fun deleteAllEntries()
}
