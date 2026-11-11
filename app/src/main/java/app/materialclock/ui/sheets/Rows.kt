package app.materialclock.ui.sheets

import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import app.materialclock.ui.util.Haptic
import app.materialclock.ui.util.rememberHaptics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The three row shapes every settings sheet in the app is built from.
 *
 * They all sit on [ListItem] with a transparent container, so they inherit the spec's own leading/
 * trailing slots, minimum heights and text styles rather than reimplementing them. The sheets then
 * differ only in content, and a change to list metrics reaches all of them at once.
 */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val haptics = rememberHaptics()
    val flip: (Boolean) -> Unit = { on -> haptics.play(if (on) Haptic.ToggleOn else Haptic.ToggleOff); onChange(on) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = flip) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        // One tap target for the whole row. `Role.Switch` is what tells a screen reader the row is
        // the control, rather than announcing an unlabelled switch beside some text.
        modifier = Modifier.clickable(role = Role.Switch) { flip(!checked) },
    )
}

/**
 * A single-choice row: shows the current value, opens a radio dialog to change it.
 *
 * A dialog rather than an inline expander, because these live inside a bottom sheet and nesting a
 * scrolling menu inside a draggable sheet fights the sheet's own gesture. The spec's own guidance
 * for a short, mutually exclusive set is exactly this.
 */
@Composable
fun <T> ChoiceRow(
    title: String,
    value: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(label(value)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { haptics.play(Haptic.Tap); open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Cancel") } },
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    options.forEach { option ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == value,
                                    role = Role.RadioButton,
                                    onClick = {
                                        haptics.play(Haptic.Segment)
                                        onSelect(option)
                                        open = false
                                    },
                                )
                                .padding(vertical = 10.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // null onClick: the Row owns the click, and a second target inside it
                            // would give a screen reader two ways to pick the same option.
                            RadioButton(selected = option == value, onClick = null)
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
        )
    }
}

/** A row that opens something else (a system picker, a sub-sheet). */
@Composable
fun NavigateRow(title: String, subtitle: String?, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it, maxLines = 1) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { haptics.play(Haptic.Tap); onClick() },
    )
}

@Composable
fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmallEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
    )
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLargeEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
    )
}

/**
 * A settings row the way Tring draws them: an icon, a title, one plain line, and the control on
 * the right. Tapping the row does what the control does, so the whole row is the target.
 */
@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    line: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (line != null) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

/** A [SettingRow] with a switch; the row and the switch both flip it. */
@Composable
fun SettingSwitch(icon: ImageVector, title: String, line: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = rememberHaptics()
    val flip: (Boolean) -> Unit = { on -> haptics.play(if (on) Haptic.ToggleOn else Haptic.ToggleOff); onChange(on) }
    SettingRow(icon, title, line, onClick = { flip(!checked) }) {
        Switch(checked = checked, onCheckedChange = flip)
    }
}

/** A short single choice as connected buttons under its [SettingRow], so every option is in view. */
@Composable
fun <T> ChoiceGroup(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val haptics = rememberHaptics()
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { i, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { if (option != selected) { haptics.play(Haptic.Segment); onSelect(option) } },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f),
            ) { Text(label(option), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge) }
        }
    }
}
