package com.pcodcompanion.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pcodcompanion.data.local.entity.PlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanItemDao {
    @Query("SELECT * FROM plan_items ORDER BY isCompleted ASC, orderIndex ASC, id DESC")
    fun getAllItems(): Flow<List<PlanItem>>

    @Query("SELECT * FROM plan_items WHERE category = :category ORDER BY isCompleted ASC, orderIndex ASC")
    fun getItemsByCategory(category: String): Flow<List<PlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PlanItem)

    @Update
    suspend fun updateItem(item: PlanItem)

    @Update
    suspend fun updateItems(items: List<PlanItem>)

    @Delete
    suspend fun deleteItem(item: PlanItem)

    @Query("DELETE FROM plan_items")
    suspend fun deleteAllItems()

    @Query("UPDATE plan_items SET isCompleted = 0")
    suspend fun resetAllCompletions()

    @Query("SELECT * FROM plan_items ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllItemsSnapshot(): List<PlanItem>
}
