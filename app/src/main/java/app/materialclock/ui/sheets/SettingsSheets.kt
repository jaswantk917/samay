package app.materialclock.ui.sheets

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.materialclock.data.ClockSettings
import app.materialclock.data.DarkMode
import app.materialclock.data.HourFormat
import app.materialclock.data.WeekStart
import app.materialclock.ui.theme.Palette
import app.materialclock.ui.util.Haptic
import app.materialclock.ui.util.SheetColumn
import app.materialclock.ui.util.rememberHaptics
import app.materialclock.ui.util.stateMorph
import app.materialclock.widget.WidgetEntryPoints
import androidx.compose.material.icons.rounded.Palette as PaletteIcon

/** The one settings sheet. Every tab's settings button opens this same sheet. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SheetColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMediumEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
            AlarmSection(settings, onChange)
            TimerSection(settings, onChange)
            WorldSection(settings, onChange)
            AppearanceSection(settings, onChange)
            WidgetRow()
            AboutRows()
        }
    }
}

@Composable
private fun AlarmSection(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
) {
    SettingRow(Icons.Rounded.Snooze, "Snooze", "How long a snoozed alarm waits")
    ChoiceGroup(
        options = listOf(5, 10, 15, 20, 30),
        selected = settings.alarms.snoozeMinutes,
        label = { "$it min" },
        onSelect = { v -> onChange { it.copy(alarms = it.alarms.copy(snoozeMinutes = v)) } },
    )
    SettingRow(Icons.Rounded.AlarmOff, "Stop ringing after", "An alarm nobody answers goes quiet")
    ChoiceGroup(
        // 0 rings until it's dismissed, so it goes last: the answer for someone the default fails
        options = listOf(5, 10, 15, 30, 0),
        selected = settings.alarms.silenceAfterMinutes,
        label = { if (it == 0) "Never" else "$it min" },
        onSelect = { v -> onChange { it.copy(alarms = it.alarms.copy(silenceAfterMinutes = v)) } },
    )
    FullScreenIntentRow()
    SettingRow(Icons.Rounded.CalendarMonth, "Week starts on", "For the days on each alarm")
    ChoiceGroup(
        options = WeekStart.entries,
        selected = settings.alarms.weekStart,
        label = { if (it == WeekStart.SYSTEM) "Phone" else it.label.take(3) },
        onSelect = { v -> onChange { it.copy(alarms = it.alarms.copy(weekStart = v)) } },
    )
}

/** Shown only while the permission is missing; once it's granted there is nothing to say. */
@Composable
private fun FullScreenIntentRow() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val nm = context.getSystemService(android.app.NotificationManager::class.java)
    // re-read on every recomposition: the user grants this in Settings and comes back
    if (nm?.canUseFullScreenIntent() != false) return
    val open = {
        haptics.play(Haptic.Tap)
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                    .setData(Uri.parse("package:${context.packageName}"))
            )
        }
        Unit
    }
    SettingRow(
        Icons.Rounded.Fullscreen,
        "Full-screen alarms",
        "Not allowed, so a ringing alarm shows as a banner",
        onClick = open,
    ) { TextButton(onClick = open) { Text("Allow") } }
}

@Composable
private fun TimerSection(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            onChange { it.copy(timers = it.timers.copy(soundUri = picked?.toString() ?: SILENT)) }
        }
    }
    val pick = {
        haptics.play(Haptic.Tap)
        picker.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Timer sound")
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        )
    }
    val soundName = when (val u = settings.timers.soundUri) {
        null -> "Default alarm sound"
        SILENT -> "Silent"
        else -> runCatching {
            RingtoneManager.getRingtone(context, Uri.parse(u))?.getTitle(context)
        }.getOrNull() ?: "Default alarm sound"
    }
    SettingRow(Icons.Rounded.MusicNote, "Timer sound", soundName, onClick = pick) {
        TextButton(onClick = pick) { Text("Change") }
    }
    SettingSwitch(
        Icons.Rounded.Vibration,
        "Vibrate for timers",
        "Buzzes along with the sound when a timer ends",
        settings.timers.vibrate,
    ) { v -> onChange { it.copy(timers = it.timers.copy(vibrate = v)) } }
}

@Composable
private fun WorldSection(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
) {
    SettingSwitch(
        Icons.Rounded.Public,
        "World Clock",
        "A tab for the time in other cities",
        settings.world.enabled,
    ) { v -> onChange { it.copy(world = it.world.copy(enabled = v)) } }
    AnimatedVisibility(
        settings.world.enabled,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            SettingSwitch(
                Icons.Rounded.AvTimer,
                "Seconds",
                "Show seconds on city times",
                settings.world.showSeconds,
            ) { v -> onChange { it.copy(world = it.world.copy(showSeconds = v)) } }
            SettingRow(Icons.Rounded.Schedule, "Time format", null)
            ChoiceGroup(
                options = HourFormat.entries,
                selected = settings.world.hourFormat,
                label = { if (it == HourFormat.SYSTEM) "Phone" else it.label },
                onSelect = { v -> onChange { it.copy(world = it.world.copy(hourFormat = v)) } },
            )
        }
    }
}

@Composable
private fun AppearanceSection(
    settings: ClockSettings,
    onChange: ((ClockSettings) -> ClockSettings) -> Unit,
) {
    SettingRow(Icons.Rounded.Contrast, "Theme", null)
    ChoiceGroup(
        options = DarkMode.entries,
        selected = settings.theme.darkMode,
        label = { if (it == DarkMode.SYSTEM) "Phone" else it.label },
        onSelect = { v -> onChange { it.copy(theme = it.theme.copy(darkMode = v)) } },
    )
    SettingSwitch(
        Icons.Rounded.DarkMode,
        "Pure black",
        "True black in dark mode, for OLED screens",
        settings.theme.amoledBlack,
    ) { v -> onChange { it.copy(theme = it.theme.copy(amoledBlack = v)) } }
    SettingSwitch(
        Icons.Rounded.BlurOn,
        "Blur under the bars",
        "Lists melt into the top and bottom edges. Heavy on some phones",
        settings.theme.blur,
    ) { v -> onChange { it.copy(theme = it.theme.copy(blur = v)) } }
    SettingRow(Icons.Rounded.PaletteIcon, "Colours", "Samay's own, your wallpaper's, or one of these")
    PaletteRow(
        dynamic = settings.theme.dynamicColor,
        selected = settings.theme.palette,
        onDynamic = { onChange { it.copy(theme = it.theme.copy(dynamicColor = true)) } },
        onSelect = { v -> onChange { it.copy(theme = it.theme.copy(palette = v, dynamicColor = false)) } },
    )
}

@Composable
private fun WidgetRow() {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    if (WidgetEntryPoints.canPin(context)) {
        val add = { haptics.play(Haptic.Tap); WidgetEntryPoints.requestPin(context); Unit }
        SettingRow(Icons.Rounded.Widgets, "Clock widget", "Hold it on the home screen to change its look", onClick = add) {
            TextButton(onClick = add) { Text("Add") }
        }
    } else {
        SettingRow(Icons.Rounded.Widgets, "Clock widget", "Add one from your home screen's widget picker")
    }
}

@Composable
private fun PaletteRow(
    dynamic: Boolean,
    selected: Palette,
    onDynamic: () -> Unit,
    onSelect: (Palette) -> Unit,
) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val context = LocalContext.current
    val wallpaper = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        } else {
            androidx.compose.material3.dynamicLightColorScheme(context)
        }
    } else {
        null
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Samay's own palette first, then the wallpaper's, then the rest
        item(key = Palette.CONCEPT.name) {
            Swatch(
                name = Palette.CONCEPT.displayName,
                scheme = if (dark) Palette.CONCEPT.dark else Palette.CONCEPT.light,
                selected = !dynamic && selected == Palette.CONCEPT,
                onClick = { onSelect(Palette.CONCEPT) },
            )
        }
        if (wallpaper != null) {
            item(key = "wallpaper") {
                Swatch("Wallpaper", wallpaper, dynamic, onDynamic)
            }
        }
        items(Palette.entries.filter { it != Palette.CONCEPT }, key = { it.name }) { palette ->
            Swatch(
                name = palette.displayName,
                scheme = if (dark) palette.dark else palette.light,
                selected = !dynamic && palette == selected,
                onClick = { onSelect(palette) },
            )
        }
    }
}

/** The chosen palette swells from a circle into a cookie, so the pick reads by shape, not only a tick. */
@Composable
private fun Swatch(
    name: String,
    scheme: androidx.compose.material3.ColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    val shape = stateMorph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided, selected)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(56.dp)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        0f to scheme.primary,
                        0.5f to scheme.tertiary,
                        1f to scheme.secondary,
                    )
                )
                .clickable(role = Role.RadioButton) { if (!selected) haptics.play(Haptic.Segment); onClick() },
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(selected, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
