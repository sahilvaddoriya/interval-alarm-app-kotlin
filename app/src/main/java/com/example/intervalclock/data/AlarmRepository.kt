package com.example.intervalclock.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AlarmRepository {
    fun getAllAlarms(): Flow<List<AlarmEntity>>
    suspend fun getAlarmById(id: Int): AlarmEntity?
    suspend fun insertAlarm(alarm: AlarmEntity): Long
    suspend fun updateAlarm(alarm: AlarmEntity)
    suspend fun deleteAlarm(alarm: AlarmEntity)
}

class AlarmRepositoryImpl @Inject constructor(
    private val alarmDao: AlarmDao
) : AlarmRepository {
    override fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    override suspend fun getAlarmById(id: Int): AlarmEntity? = alarmDao.getAlarmById(id)

    override suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    override suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    override suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)
}
