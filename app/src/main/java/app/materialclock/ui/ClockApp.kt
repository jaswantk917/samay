package app.materialclock.ui

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import app.materialclock.core.humanUntil
import app.materialclock.ui.util.Haptic
import app.materialclock.ui.util.LocalBlur
import app.materialclock.ui.util.edgeBlur
import app.materialclock.ui.util.pressMorph
import app.materialclock.ui.util.rememberHaptics
import java.time.ZonedDateTime
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.materialclock.alarm.Notifications
import app.materialclock.core.Alarm
import app.materialclock.ui.screens.AlarmsScreen
import app.materialclock.ui.screens.StopwatchScreen
import app.materialclock.ui.screens.TimersScreen
import app.materialclock.ui.screens.WorldClockScreen
import app.materialclock.ui.sheets.AddCitySheet
import app.materialclock.ui.sheets.AlarmEditSheet
import app.materialclock.ui.sheets.AlarmSettingsSheet
import app.materialclock.ui.sheets.StopwatchSettingsSheet
import app.materialclock.ui.sheets.TimerSettingsSheet
import app.materialclock.ui.sheets.WorldSettingsSheet
import app.materialclock.ui.theme.ClockTheme

/** Breathing room between the dock and the navigation bar. */
private val DOCK_CLEARANCE = 20.dp

/**
 * How much of the bottom of every screen the floating dock is standing on.
 *
 * Derived from [DOCK_HEIGHT] rather than restated as a number. It was written out as a literal 96
 * once ("64 of pill, 20 under it, 12 above the last row"), and the moment the pill grew to 72 the
 * comment was still true and the constant was silently 8 dp short, which shows up as the last row
 * of every list tucked a little too far under the dock. There is nothing to notice and nothing to
 * fail; it just quietly looks wrong.
 */
private val DOCK_RESERVE = DOCK_HEIGHT + DOCK_CLEARANCE + 12.dp

enum class Tab(val label: String, val icon: ImageVector, val key: String) {
    ALARMS("Alarms", Icons.Outlined.Alarm, Notifications.TAB_ALARMS),
    WORLD("World Clock", Icons.Outlined.Public, "world"),
    TIMERS("Timers", Icons.Outlined.HourglassEmpty, Notifications.TAB_TIMERS),
    STOPWATCH("Stopwatch", Icons.Outlined.Timer, Notifications.TAB_STOPWATCH),
}

/**
 * The app shell.
 *
 * Navigation is [ClockDock], a floating pill with a detached add button and no navigation bar, so
 * the alarm grid and the world-clock list run full-bleed to the bottom edge. That component owes
 * the accessibility contract a `ShortNavigationBar` would have supplied for free; see its own
 * documentation for what it pays and why.
 *
 * Every tab shares one header: a large title that tucks away on scroll, one quiet status line, and
 * a settings button that opens that tab's sheet. Content blurs as it slides under the header and
 * under the dock.
 */
@Composable
fun ClockApp(startTab: String? = null, vm: ClockViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    ClockTheme(settings.theme) { CompositionLocalProvider(LocalBlur provides settings.theme.blur) {
        var tab by rememberSaveable { mutableStateOf(startTab?.let { k -> Tab.entries.firstOrNull { it.key == k } } ?: Tab.ALARMS) }
        var editing by remember { mutableStateOf<Alarm?>(null) }
        var showSettings by remember { mutableStateOf(false) }
        var addingCity by remember { mutableStateOf(false) }
        val snackbar = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Asked once, on first composition. Denying it costs the notifications and nothing else.
        // The alarm still rings, because the ringer is a foreground service and audio, not a post.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }

        val haptics = rememberHaptics()
        val density = LocalDensity.current
        var headerHeight by remember { mutableStateOf(120.dp) }
        // the title tucks away on the way down and comes back on any scroll up
        var titleHeight by remember { mutableFloatStateOf(0f) }
        var collapse by remember { mutableFloatStateOf(0f) }
        val collapser = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    collapse = (collapse - available.y).coerceIn(0f, titleHeight)
                    return Offset.Zero
                }
            }
        }
        val reveal = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
        LaunchedEffect(tab) {
            if (collapse > 0f) animate(collapse, 0f, animationSpec = reveal) { v, _ -> collapse = v }
        }

        val alarms by vm.alarms.collectAsStateWithLifecycle()
        val minute by rememberWallTicker(60_000L)
        val subtitle = when (tab) {
            Tab.ALARMS -> remember(alarms, minute) {
                val now = ZonedDateTime.now()
                alarms.mapNotNull { it.nextFire(now) }.minOrNull()?.let { "Rings in ${humanUntil(now, it)}" }
            }
            else -> null
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(collapser),
        ) {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            // The dock floats over the content rather than displacing it, so the space it occupies
            // has to be reserved here by hand; otherwise the last alarm tile sits under the pill.
            val underDock = navBottom + DOCK_RESERVE
            val body = PaddingValues(start = 16.dp, end = 16.dp, top = headerHeight, bottom = underDock)
            val edgeToEdge = PaddingValues(top = headerHeight, bottom = underDock)

            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab",
                modifier = Modifier.fillMaxSize().edgeBlur(
                    top = { headerHeight - with(density) { collapse.toDp() } },
                    bottom = navBottom + DOCK_HEIGHT + DOCK_CLEARANCE,
                ),
            ) { current ->
                when (current) {
                    Tab.ALARMS -> {
                        AlarmsScreen(
                            alarms = alarms,
                            weekStart = settings.alarms.weekStart,
                            onToggle = vm::toggleAlarm,
                            onEdit = { editing = it },
                            contentPadding = body,
                        )
                    }

                    Tab.WORLD -> {
                        val cities by vm.cities.collectAsStateWithLifecycle()
                        val now by rememberWallTicker()
                        WorldClockScreen(
                            cities = cities,
                            home = vm.homeZone,
                            nowUtcMillis = now,
                            settings = settings.world,
                            // Swiping a city away is instant and undoable, rather than instant and
                            // final or safe and nagging.
                            onRemove = { city ->
                                vm.removeCity(city.zone)
                                scope.launch {
                                    val r = snackbar.showSnackbar(
                                        message = "Removed ${city.city}",
                                        actionLabel = "Undo",
                                        // With an action label the default is Indefinite, which
                                        // leaves a bar sitting over the list after every swipe.
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (r == SnackbarResult.ActionPerformed) vm.addCity(city)
                                }
                            },
                            contentPadding = edgeToEdge,
                        )
                    }

                    Tab.TIMERS -> {
                        val timer by vm.timer.collectAsStateWithLifecycle()
                        // Only tick while a timer exists; the setting screen has nothing moving.
                        val now by rememberElapsedTicker(active = timer != null)
                        TimersScreen(
                            timer = timer,
                            draft = vm.draftDuration,
                            nowElapsedMillis = now,
                            onDigit = vm::pressDigit,
                            onBackspace = vm::backspace,
                            onWind = vm::windToMinutes,
                            onStart = { vm.startTimer() },
                            onPauseResume = { vm.pauseOrResumeTimer() },
                            onAddTen = { vm.addTenSeconds() },
                            onCancel = { vm.cancelTimer() },
                            contentPadding = edgeToEdge,
                        )
                    }

                    Tab.STOPWATCH -> {
                        val sw by vm.stopwatch.collectAsStateWithLifecycle()
                        val now by rememberElapsedTicker(active = sw.running)
                        StopwatchScreen(
                            stopwatch = sw,
                            nowElapsedMillis = now,
                            onToggle = { vm.toggleStopwatch() },
                            onLap = { vm.lap() },
                            onReset = { vm.resetStopwatch() },
                            contentPadding = edgeToEdge,
                        )
                    }
                }
            }

            Header(
                title = tab.label,
                subtitle = subtitle,
                onSettings = { haptics.play(Haptic.Tap); showSettings = true },
                collapse = { collapse },
                onTitleHeight = { titleHeight = it.toFloat() },
                modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
            )
            // the list blurs right up under the clock; a soft fade keeps the clock readable
            val surface = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(Brush.verticalGradient(0f to surface.copy(alpha = 0.75f), 1f to surface.copy(alpha = 0f))),
            )

            ClockDock(
                destinations = Tab.entries,
                selected = tab,
                onSelect = { if (it != tab) haptics.play(Haptic.Segment); tab = it },
                // Timers and Stopwatch have nothing to add: you create those with the controls on
                // the screen itself. The button therefore leaves rather than sitting there inert.
                showAdd = tab == Tab.ALARMS || tab == Tab.WORLD,
                addLabel = if (tab == Tab.WORLD) "Add city" else "Add alarm",
                onAdd = {
                    haptics.play(Haptic.Tap)
                    if (tab == Tab.WORLD) addingCity = true else editing = vm.blankAlarm()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = DOCK_CLEARANCE),
            )

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = DOCK_HEIGHT + DOCK_CLEARANCE + 8.dp),
            )
        }

        editing?.let { draft ->
            AlarmEditSheet(
                initial = draft,
                weekStart = settings.alarms.weekStart,
                onDismiss = { editing = null },
                onSave = { vm.saveAlarm(it) },
                onDelete = if (draft.id == 0L) null else ({ id -> vm.deleteAlarm(id) }),
            )
        }

        if (addingCity) {
            val cities by vm.cities.collectAsStateWithLifecycle()
            val now by rememberWallTicker(60_000L)
            AddCitySheet(
                existing = cities,
                nowUtcMillis = now,
                onAdd = vm::addCity,
                onDismiss = { addingCity = false },
            )
        }

        if (showSettings) {
            when (tab) {
                Tab.ALARMS -> AlarmSettingsSheet(settings, vm::updateSettings) { showSettings = false }
                Tab.WORLD -> WorldSettingsSheet(settings, vm::updateSettings) { showSettings = false }
                Tab.TIMERS -> TimerSettingsSheet(settings, vm::updateSettings) { showSettings = false }
                Tab.STOPWATCH ->
                    StopwatchSettingsSheet(settings, vm::updateSettings) { showSettings = false }
            }
        }
    }
    }
}

@Composable
private fun Header(
    title: String,
    subtitle: String?,
    onSettings: () -> Unit,
    collapse: () -> Float,
    onTitleHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = -collapse() }
            .background(Brush.verticalGradient(0f to surface.copy(alpha = 0.8f), 0.65f to surface.copy(alpha = 0.6f), 1f to surface.copy(alpha = 0f)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 10.dp),
    ) {
        Row(
            Modifier
                .onSizeChanged { onTitleHeight(it.height) }
                .graphicsLayer {
                    val p = if (size.height > 0f) collapse() / size.height else 0f
                    alpha = 1f - p
                    scaleX = 1f - 0.08f * p
                    scaleY = 1f - 0.08f * p
                    transformOrigin = TransformOrigin(0f, 1f)
                }
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                AnimatedContent(title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") {
                    Text(it, style = MaterialTheme.typography.displaySmallEmphasized, color = MaterialTheme.colorScheme.onSurface)
                }
                AnimatedVisibility(subtitle != null) {
                    AnimatedContent(subtitle.orEmpty(), label = "subtitle") {
                        Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val press = remember { MutableInteractionSource() }
            FilledTonalIconButton(
                onClick = onSettings,
                shape = pressMorph(MaterialShapes.Clover4Leaf, MaterialShapes.Cookie4Sided, press),
                interactionSource = press,
                modifier = Modifier.size(48.dp),
            ) { Icon(Icons.Rounded.Settings, "Settings") }
        }
    }
}
