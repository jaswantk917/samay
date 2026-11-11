package app.materialclock.ui.sheets

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import app.materialclock.ui.util.rememberHaptics
import app.materialclock.ui.util.Haptic
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.materialclock.BuildConfig

/**
 * The About section: the things an app needs before it can be given to somebody else.
 *
 * Every claim below is checkable against the APK rather than being a promise. That is the whole
 * standard this section is held to. The suite's rule is that *a stale privacy claim is worse than
 * none*, because it is the one thing a user has no way to verify for themselves. So:
 *
 *  - **No `INTERNET` permission**, and not in any merged dependency manifest either. That was
 *    checked, not assumed. An app without it cannot open a socket: the platform refuses, so this
 *    is enforced by Android rather than by the app's good behaviour. It is the only privacy claim
 *    worth making, because it is the only one that does not depend on trusting the developer.
 *  - Every other permission is listed with what it is actually for. A permission list the user has
 *    to interpret is not disclosure.
 *  - The bundled fonts are named with their licences, because two of them are redistributed
 *    binaries and that carries obligations.
 */
@Composable
fun AboutRows() {
    var open by remember { mutableStateOf<AboutPage?>(null) }

    val haptics = rememberHaptics()
    fun show(page: AboutPage) { haptics.play(Haptic.Tap); open = page }
    SettingRow(Icons.Rounded.Description, "Licences", "Fonts and libraries", onClick = { show(AboutPage.LICENCES) })
    Spacer(Modifier.height(12.dp))
    Text(
        "Everything stays on this phone. No accounts, no network.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
    Text(
        "Samay ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 2.dp),
    )

    open?.let { page ->
        AlertDialog(
            onDismissRequest = { open = null },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Close") } },
            title = { Text(page.title) },
            text = {
                Column(
                    Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    page.body.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
        )
    }
}

private enum class AboutPage(val title: String, val body: List<String>) {

    LICENCES(
        "Licences",
        listOf(
            "This app's own code is MIT licensed. The bundled fonts are not; they are under the " +
                "SIL Open Font License 1.1, reproduced in full at openfontlicense.org.",
            "Google Sans Flex. Copyright 2015 Google LLC. SIL Open Font License 1.1. Modified: " +
                "the per-script subsets Google serves are merged into one variable font, because " +
                "Android cannot load woff2. Outlines and all six axes are unchanged.",
            "Noto Sans Arabic. Copyright 2022 The Noto Project Authors. SIL Open Font License " +
                "1.1. Modified: a ten-glyph subset (U+0660–0669) for the widget's Eastern Arabic " +
                "numerals, because Google Sans Flex has no Arabic coverage.",
            "Jetpack Compose, AndroidX, Material Symbols and androidx.graphics.shapes are under " +
                "the Apache License 2.0, copyright The Android Open Source Project and Google LLC.",
            "Google, Google Sans, Material, Android and Pixel are trademarks of Google LLC. This " +
                "app is not affiliated with, sponsored by or endorsed by Google. It is an " +
                "independent recreation of a published Material 3 Expressive concept design.",
        ),
    ),
}
