package com.pcodcompanion.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A pattern the app has noticed about the user. Stored once per stable [key]
 * (insert uses IGNORE conflict, so re-detecting the same pattern is a no-op).
 *
 * [lastShownAtMillis] tracks rotation on the Today screen.
 */
@Entity(
    tableName = "insights",
    indices = [Index(value = ["key"], unique = true)]
)
data class Insight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val message: String,
    val createdAtMillis: Long,
    @ColumnInfo(defaultValue = "0")
    val lastShownAtMillis: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val showCount: Int = 0
)
