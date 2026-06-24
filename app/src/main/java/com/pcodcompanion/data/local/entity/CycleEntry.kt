package com.pcodcompanion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cycle_entries")
data class CycleEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startDate: String,       // yyyy-MM-dd
    val endDate: String? = null, // nullable until period ends
    val flowLevel: String = "Medium",  // Light, Medium, Heavy
    val symptoms: String = "",   // comma-separated
    val notes: String = ""
)
