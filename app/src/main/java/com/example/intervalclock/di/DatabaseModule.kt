package com.example.intervalclock.di

import android.content.Context
import androidx.room.Room
import com.example.intervalclock.data.AlarmDao
import com.example.intervalclock.data.AlarmRepository
import com.example.intervalclock.data.AlarmRepositoryImpl
import com.example.intervalclock.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "interval_clock.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AppDatabase): AlarmDao {
        return database.alarmDao()
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(alarmDao: AlarmDao): AlarmRepository {
        return AlarmRepositoryImpl(alarmDao)
    }
}
