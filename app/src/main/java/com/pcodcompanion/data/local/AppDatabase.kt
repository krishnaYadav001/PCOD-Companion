package com.pcodcompanion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pcodcompanion.data.local.dao.CycleEntryDao
import com.pcodcompanion.data.local.dao.DailyLogDao
import com.pcodcompanion.data.local.dao.InsightDao
import com.pcodcompanion.data.local.dao.PlanItemDao
import com.pcodcompanion.data.local.entity.CycleEntry
import com.pcodcompanion.data.local.entity.DailyLog
import com.pcodcompanion.data.local.entity.Insight
import com.pcodcompanion.data.local.entity.PlanItem

@Database(
    entities = [DailyLog::class, CycleEntry::class, PlanItem::class, Insight::class],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun cycleEntryDao(): CycleEntryDao
    abstract fun planItemDao(): PlanItemDao
    abstract fun insightDao(): InsightDao

    companion object {
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plan_items ADD COLUMN daysOfWeek TEXT NOT NULL DEFAULT 'Mon,Tue,Wed,Thu,Fri,Sat,Sun'")
            }
        }
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN exerciseName TEXT DEFAULT NULL")
            }
        }
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN stressLevel TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN sleepQuality TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN proteinIncluded INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN medications TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN streakDays INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN emotionalCheckIn TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS insights (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        key TEXT NOT NULL,
                        message TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        lastShownAtMillis INTEGER NOT NULL DEFAULT 0,
                        showCount INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_insights_key ON insights(key)"
                )
            }
        }
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN planCompletionPct INTEGER NOT NULL DEFAULT -1")
            }
        }
    }
}
