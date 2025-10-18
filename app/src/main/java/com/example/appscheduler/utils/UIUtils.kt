package com.example.appscheduler.utils

import kotlin.time.Duration.Companion.milliseconds

object UIUtils {
    fun convertMillisToHoursAndMinutes(milliseconds: Long): Pair<Int, Int> {
        val duration = milliseconds.milliseconds
        return duration.toComponents { hours, minutes, _, _ ->
            // Use String.format for consistent leading zeros
            Pair(hours.toInt(), minutes)
        }
    }
}