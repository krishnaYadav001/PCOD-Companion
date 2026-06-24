package com.pcodcompanion.di

import android.content.Context
import androidx.room.Room
import com.pcodcompanion.data.local.AppDatabase
import com.pcodcompanion.data.local.dao.CycleEntryDao
import com.pcodcompanion.data.local.dao.DailyLogDao
import com.pcodcompanion.data.local.dao.InsightDao
import com.pcodcompanion.data.local.dao.PlanItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pcod_companion_db"
        )
            .addMigrations(AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDailyLogDao(database: AppDatabase): DailyLogDao = database.dailyLogDao()

    @Provides
    fun provideCycleEntryDao(database: AppDatabase): CycleEntryDao = database.cycleEntryDao()

    @Provides
    fun providePlanItemDao(database: AppDatabase): PlanItemDao = database.planItemDao()

    @Provides
    fun provideInsightDao(database: AppDatabase): InsightDao = database.insightDao()
}
