package com.example.intervalclock.data

import androidx.room.TypeConverter
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String {
        return days.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDayOfWeekSet(data: String): Set<DayOfWeek> {
        if (data.isBlank()) return emptySet()
        return data.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }
}
