package com.example.intervalclock.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.intervalclock.data.AlarmEntity
import com.example.intervalclock.data.Converters
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlarmItem(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), // Google Clock style
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 24.dp, horizontal = 16.dp) // More padding
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val start = LocalTime.ofSecondOfDay(alarm.startTimeInMinutes * 60L)
                val formatter = DateTimeFormatter.ofPattern("hh:mm")
                val amPmFormatter = DateTimeFormatter.ofPattern("a")
                
                // Determine content color based on card state for optimal contrast
                val contentColor = if (alarm.isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = start.format(formatter),
                        style = MaterialTheme.typography.displayMedium, // Larger font
                        color = contentColor
                    )
                    Text(
                        text = start.format(amPmFormatter).lowercase(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                         color = contentColor
                    )
                }
                
                val end = LocalTime.ofSecondOfDay(alarm.endTimeInMinutes * 60L)
                
                Text(
                    text = "Until ${end.format(DateTimeFormatter.ofPattern("hh:mm a"))} • Every ${alarm.intervalInMinutes} min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                
                val daysText = formatDays(alarm.daysOfWeek) 
                
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
                
                if (alarm.label.isNotBlank()) {
                     Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.labelLarge,
                        // Use primary color when disabled to stand out, but contentColor (onPrimaryContainer) when enabled
                        color = if (alarm.isEnabled) contentColor else MaterialTheme.colorScheme.primary
                    )
                }
                
                if (alarm.isEnabled && alarm.nextTriggerTime != null && alarm.nextTriggerTime > 0) {
                     val nextTime = java.time.Instant.ofEpochSecond(alarm.nextTriggerTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime()
                     val nextFormatter = DateTimeFormatter.ofPattern("EEE hh:mm a")
                     Text(
                        text = "Next: ${nextTime.format(nextFormatter)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = contentColor 
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}

fun formatDays(days: Set<java.time.DayOfWeek>): String {
    if (days.size == 7) return "Every day"
    if (days.isEmpty()) return "Never"

    val sortedDays = days.sortedBy { day -> if (day == java.time.DayOfWeek.SUNDAY) 0 else day.value }
    val dayIndices = sortedDays.map { if (it == java.time.DayOfWeek.SUNDAY) 0 else it.value }
    
    val ranges = mutableListOf<List<java.time.DayOfWeek>>()
    var currentRange = mutableListOf<java.time.DayOfWeek>()
    
    for (i in dayIndices.indices) {
        val day = sortedDays[i]
        val index = dayIndices[i]
        
        if (currentRange.isEmpty()) {
            currentRange.add(day)
        } else {
            val lastDayIndex = if (currentRange.last() == java.time.DayOfWeek.SUNDAY) 0 else currentRange.last().value
            if (index == lastDayIndex + 1) {
                currentRange.add(day)
            } else {
                ranges.add(currentRange)
                currentRange = mutableListOf(day)
            }
        }
    }
    if (currentRange.isNotEmpty()) {
        ranges.add(currentRange)
    }
    
    val parts = mutableListOf<String>()
    for (range in ranges) {
        if (range.size >= 3) {
            val start = range.first().name.take(3).toLowerCase().capitalize()
            val end = range.last().name.take(3).toLowerCase().capitalize()
            parts.add("$start-$end")
        } else {
            range.forEach { day ->
                parts.add(day.name.take(3).toLowerCase().capitalize())
            }
        }
    }
    
    return parts.joinToString(" ")
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
