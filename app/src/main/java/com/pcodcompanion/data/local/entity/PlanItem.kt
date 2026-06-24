package com.pcodcompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_items")
data class PlanItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Lifestyle",  // Diet, Exercise, Lifestyle
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val daysOfWeek: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun"
)
