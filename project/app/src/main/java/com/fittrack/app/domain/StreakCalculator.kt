package com.fittrack.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields

/**
 * Pure helpers for the consistency indicator: workouts are grouped by ISO
 * week, and the streak counts consecutive weeks (ending this week or last
 * week) with at least one completed workout.
 */
object StreakCalculator {

    private data class YearWeek(val year: Int, val week: Int)

    private fun yearWeek(epochMillis: Long, zone: ZoneId): YearWeek {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        return yearWeek(date)
    }

    private fun yearWeek(date: LocalDate): YearWeek {
        val iso = WeekFields.ISO
        return YearWeek(date.get(iso.weekBasedYear()), date.get(iso.weekOfWeekBasedYear()))
    }

    /** Number of completed workouts in the current ISO week. */
    fun workoutsThisWeek(completedStartTimes: List<Long>, now: Long, zone: ZoneId): Int {
        val current = yearWeek(now, zone)
        return completedStartTimes.count { yearWeek(it, zone) == current }
    }

    /**
     * Consecutive-week streak. The current week counts if it has a workout;
     * otherwise the streak is measured ending at last week (so the streak
     * doesn't read zero on a Monday morning).
     */
    fun weeklyStreak(completedStartTimes: List<Long>, now: Long, zone: ZoneId): Int {
        if (completedStartTimes.isEmpty()) return 0
        val weeksWithWorkouts = completedStartTimes.map { yearWeek(it, zone) }.toHashSet()

        var cursor = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        if (yearWeek(cursor) !in weeksWithWorkouts) {
            cursor = cursor.minusWeeks(1)
            if (yearWeek(cursor) !in weeksWithWorkouts) return 0
        }
        var streak = 0
        while (yearWeek(cursor) in weeksWithWorkouts) {
            streak++
            cursor = cursor.minusWeeks(1)
        }
        return streak
    }

    /**
     * Workout counts for the last [weeks] ISO weeks, oldest first, including
     * the current week as the final entry.
     */
    fun workoutsPerWeek(completedStartTimes: List<Long>, now: Long, zone: ZoneId, weeks: Int): List<Int> {
        val byWeek = completedStartTimes.groupingBy { yearWeek(it, zone) }.eachCount()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (weeks - 1 downTo 0).map { offset ->
            byWeek[yearWeek(today.minusWeeks(offset.toLong()))] ?: 0
        }
    }
}
