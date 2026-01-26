package com.example.intervalclock.di

import com.example.intervalclock.alarm.AlarmScheduler
import com.example.intervalclock.alarm.AlarmSchedulerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AlarmModule {

    @Binds
    abstract fun bindAlarmScheduler(
        alarmSchedulerImpl: AlarmSchedulerImpl
    ): AlarmScheduler
}
