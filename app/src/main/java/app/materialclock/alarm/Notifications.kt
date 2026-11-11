package app.materialclock.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import app.materialclock.MainActivity
import app.materialclock.R
import app.materialclock.core.ClockTimer
import app.materialclock.core.Stopwatch
import app.materialclock.core.TimerState
import app.materialclock.core.clockFormat

/**
 * Every channel and every notification the app posts.
 *
 * ## The timer and stopwatch notifications carry no service
 *
 * They do not need one. A countdown is a *deadline*, not a process: the remaining time is
 * `deadline − elapsedRealtime()`, correct whether or not this app has run since. So the ongoing
 * notification is posted once with `setUsesChronometer` and the system renders the ticking digits
 * itself, at zero cost to the app, for as long as the notification exists. Notifications outlive
 * the process that posted them. A foreground service would burn a wakelock to recompute a number
 * the platform can derive from a timestamp.
 *
 * `setChronometerCountDown(true)` is what makes the timer count *down* rather than up; it needs
 * API 24 and the app's floor is 26. The expiry itself is an exact alarm, so nothing has to be
 * awake to notice it.
 *
 * The ringer is the one thing that does take a foreground service, because looping audio is real
 * ongoing work (see [AlarmService]).
 */
object Notifications {

    const val CHANNEL_ALARM = "alarm"
    const val CHANNEL_TIMER = "timer"
    const val CHANNEL_STOPWATCH = "stopwatch"

    const val ID_RINGING = 1
    const val ID_TIMER = 2
    const val ID_STOPWATCH = 3

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return

        // No sound on the channel: the ringer plays its own looping audio, and a channel sound
        // would fire a second, one-shot copy over the top of it.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALARM, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "A ringing alarm"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, "Timers", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A running timer"
                setSound(null, null)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_STOPWATCH, "Stopwatch", NotificationManager.IMPORTANCE_LOW).apply {
                description = "A running stopwatch"
                setSound(null, null)
            }
        )
    }

    /* ── Timer ──────────────────────────────────────────────────────────────────────────────── */

    fun showTimer(context: Context, timer: ClockTimer) {
        val running = timer.state == TimerState.RUNNING
        val b = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(timer.label.ifBlank { "Timer" })
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context, TAB_TIMERS))
            // a Live Update: pinned at the top of the shade, with a ticking chip in the status bar
            .setRequestPromotedOngoing(true)
            .addAction(
                0,
                if (running) "Pause" else "Resume",
                broadcast(context, ClockActionReceiver.ACTION_TIMER_TOGGLE, 20),
            )
            .addAction(0, "+1 min", broadcast(context, ClockActionReceiver.ACTION_TIMER_ADD, 21))
            .addAction(0, "Cancel", broadcast(context, ClockActionReceiver.ACTION_TIMER_CANCEL, 22))

        val remaining = timer.remaining(SystemClock.elapsedRealtime())
        if (running) {
            // The platform ticks this. `when` is the moment it reaches zero.
            b.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + remaining.toMillis())
                .setShowWhen(true)
        } else {
            b.setUsesChronometer(false)
                .setShowWhen(false)
                .setShortCriticalText(timer.pausedRemaining.clockFormat())
                .setContentText("Paused · ${timer.pausedRemaining.clockFormat()}")
        }
        if (Build.VERSION.SDK_INT >= 37) {
            // Android 17's metric layout: the big ticking number Google Clock uses
            val value = if (running) {
                NotificationCompat.Metric.TimeDifference.forTimer(
                    SystemClock.elapsedRealtime() + remaining.toMillis(),
                    NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
                )
            } else {
                NotificationCompat.Metric.TimeDifference.forPausedTimer(
                    timer.pausedRemaining,
                    NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
                )
            }
            b.setStyle(NotificationCompat.MetricStyle().addMetric(NotificationCompat.Metric(value, if (running) "Left" else "Paused")))
        }
        post(context, ID_TIMER, b.build())
    }

    fun hideTimer(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_TIMER)
    }

    /* ── Stopwatch ──────────────────────────────────────────────────────────────────────────── */

    fun showStopwatch(context: Context, sw: Stopwatch) {
        val b = NotificationCompat.Builder(context, CHANNEL_STOPWATCH)
            .setSmallIcon(R.drawable.ic_stat_stopwatch)
            .setContentTitle("Stopwatch")
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp(context, TAB_STOPWATCH))
            .setRequestPromotedOngoing(true)
            .addAction(
                0,
                if (sw.running) "Stop" else "Start",
                broadcast(context, ClockActionReceiver.ACTION_SW_TOGGLE, 30),
            )
            .addAction(
                0,
                if (sw.running) "Lap" else "Reset",
                broadcast(
                    context,
                    if (sw.running) ClockActionReceiver.ACTION_SW_LAP else ClockActionReceiver.ACTION_SW_RESET,
                    31,
                ),
            )

        val elapsed = sw.elapsed(SystemClock.elapsedRealtime())
        if (sw.running) {
            // Counting up: `when` is the instant it started, which the platform subtracts from now.
            b.setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - elapsed.toMillis())
                .setShowWhen(true)
        } else {
            b.setUsesChronometer(false)
                .setShowWhen(false)
                .setShortCriticalText(sw.accumulated.clockFormat())
                .setContentText(sw.accumulated.clockFormat(withHours = true))
        }
        if (Build.VERSION.SDK_INT >= 37) {
            val value = if (sw.running) {
                NotificationCompat.Metric.TimeDifference.forStopwatch(
                    SystemClock.elapsedRealtime() - elapsed.toMillis(),
                    NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
                )
            } else {
                NotificationCompat.Metric.TimeDifference.forPausedStopwatch(
                    sw.accumulated,
                    NotificationCompat.Metric.TimeDifference.FORMAT_CHRONOMETER,
                )
            }
            b.setStyle(NotificationCompat.MetricStyle().addMetric(NotificationCompat.Metric(value, if (sw.running) "Running" else "Stopped")))
        }
        post(context, ID_STOPWATCH, b.build())
    }

    fun hideStopwatch(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_STOPWATCH)
    }

    /* ── Plumbing ───────────────────────────────────────────────────────────────────────────── */

    const val TAB_ALARMS = "alarms"
    const val TAB_TIMERS = "timers"
    const val TAB_STOPWATCH = "stopwatch"
    const val EXTRA_TAB = "tab"

    fun openApp(context: Context, tab: String): PendingIntent = PendingIntent.getActivity(
        context,
        tab.hashCode(),
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_TAB, tab),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun broadcast(context: Context, action: String, requestCode: Int, id: Long = 0L): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ClockActionReceiver::class.java).setAction(action)
                .putExtra(AlarmReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /**
     * Posts, unless the user said no.
     *
     * `POST_NOTIFICATIONS` is a runtime permission from API 33 and every one of these calls sits
     * on a path that must not throw, whether a receiver, a service or a coroutine with no UI.
     * Checking here once is cheaper than a try/catch at every call site and it fails the way it
     * should: the alarm still rings, it is just not announced.
     */
    private fun post(context: Context, id: Int, n: android.app.Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        runCatching { nm.notify(id, n) }
    }
}
