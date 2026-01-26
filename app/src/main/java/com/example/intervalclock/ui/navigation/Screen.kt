package com.example.intervalclock.ui.navigation

sealed class Screen(val route: String) {
    object AlarmList : Screen("alarm_list")
    object EditAlarm : Screen("edit_alarm?alarmId={alarmId}") {
        fun createRoute(alarmId: Int? = null) = "edit_alarm?alarmId=${alarmId ?: -1}"
    }
}
