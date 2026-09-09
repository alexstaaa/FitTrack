package com.fittrack.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        LocalDateTime.of(year, month, day, hour, 0).atZone(zone).toInstant().toEpochMilli()

    // Wednesday July 1, 2026 as "now".
    private val now = at(2026, 7, 1)

    @Test
    fun `empty history has zero streak and zero this week`() {
        assertEquals(0, StreakCalculator.weeklyStreak(emptyList(), now, zone))
        assertEquals(0, StreakCalculator.workoutsThisWeek(emptyList(), now, zone))
    }

    @Test
    fun `counts workouts in current iso week`() {
        val times = listOf(
            at(2026, 6, 29), // Monday same week
            at(2026, 6, 30),
            at(2026, 6, 24), // previous week
        )
        assertEquals(2, StreakCalculator.workoutsThisWeek(times, now, zone))
    }

    @Test
    fun `streak counts consecutive weeks including current`() {
        val times = listOf(
            at(2026, 6, 30), // this week
            at(2026, 6, 24), // 1 week back
            at(2026, 6, 17), // 2 weeks back
        )
        assertEquals(3, StreakCalculator.weeklyStreak(times, now, zone))
    }

    @Test
    fun `streak survives an empty current week`() {
        val times = listOf(
            at(2026, 6, 24), // last week
            at(2026, 6, 17),
        )
        assertEquals(2, StreakCalculator.weeklyStreak(times, now, zone))
    }

    @Test
    fun `streak breaks on a gap week`() {
        val times = listOf(
            at(2026, 6, 30), // this week
            at(2026, 6, 10), // 3 weeks back — gap in between
        )
        assertEquals(1, StreakCalculator.weeklyStreak(times, now, zone))
    }

    @Test
    fun `workouts per week returns oldest first with current week last`() {
        val times = listOf(
            at(2026, 6, 30), // this week ×1
            at(2026, 6, 24), // last week ×2
            at(2026, 6, 23),
        )
        val perWeek = StreakCalculator.workoutsPerWeek(times, now, zone, weeks = 3)
        assertEquals(listOf(0, 2, 1), perWeek)
    }
}
