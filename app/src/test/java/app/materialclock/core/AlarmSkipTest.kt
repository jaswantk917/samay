package app.materialclock.core

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlarmSkipTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    // a Wednesday evening
    private val now = ZonedDateTime.of(2026, 11, 11, 22, 0, 0, 0, zone)
    private val weekdays = Alarm(id = 1, time = LocalTime.of(7, 0), days = Alarm.WEEKDAYS)

    @Test
    fun skipPassesOverOnlyTheNextRing() {
        val thursday = weekdays.nextFire(now)!!
        assertEquals(DayOfWeek.THURSDAY, thursday.dayOfWeek)
        val skipped = weekdays.copy(skippedAtMillis = thursday.toInstant().toEpochMilli())
        assertEquals(DayOfWeek.FRIDAY, skipped.nextFire(now)!!.dayOfWeek)
        assertEquals(thursday, skipped.skipping(now))
    }

    @Test
    fun aSkipThatHasPassedIsIgnored() {
        val thursday = weekdays.nextFire(now)!!
        val skipped = weekdays.copy(skippedAtMillis = thursday.toInstant().toEpochMilli())
        val friday = thursday.plusHours(1)
        assertNull(skipped.skipping(friday))
        assertEquals(DayOfWeek.FRIDAY, skipped.nextFire(friday)!!.dayOfWeek)
    }

    @Test
    fun skippingTheLastWeekdayRollsToMonday() {
        val fridayNight = now.plusDays(2)
        val friday = weekdays.nextFire(now.plusDays(1))!!
        assertEquals(DayOfWeek.FRIDAY, friday.dayOfWeek)
        val skipped = weekdays.copy(skippedAtMillis = friday.toInstant().toEpochMilli())
        assertEquals(DayOfWeek.MONDAY, skipped.nextFire(now.plusDays(1))!!.dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, skipped.nextFire(fridayNight)!!.dayOfWeek)
    }
}
