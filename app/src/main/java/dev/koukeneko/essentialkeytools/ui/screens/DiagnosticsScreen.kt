package dev.koukeneko.essentialkeytools.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.diagnostics.CrashReportStore
import dev.koukeneko.essentialkeytools.diagnostics.ProcessExit
import dev.koukeneko.essentialkeytools.diagnostics.formatProcessExit
import dev.koukeneko.essentialkeytools.diagnostics.readRecentProcessExits
import dev.koukeneko.essentialkeytools.ui.ISSUES_URL
import dev.koukeneko.essentialkeytools.ui.components.NothingButton
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.openExternalUrl
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val BODY_GAP = 16.dp
private val BUTTON_GAP = 12.dp
private val EXIT_ROW_GAP = 8.dp

private const val CLIP_LABEL = "Essential Key Tools crash report"

/**
 * Turns "it crashed" into something reportable. The last crash this app caught, plus the exits
 * Android itself recorded, rendered so the user can copy or share them into a bug report.
 *
 * Nothing here leaves the device on its own: every way out of this screen is a deliberate user
 * action, which is what keeps the app free of any data-collection disclosure.
 */
@Composable
fun DiagnosticsScreen(
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val store = remember(context) { CrashReportStore.create(context) }
    var crashReport by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(store) {
        crashReport = withContext(Dispatchers.IO) { store.read() }
    }
    val processExits by produceState(initialValue = emptyList<ProcessExit>(), context) {
        value = withContext(Dispatchers.IO) { readRecentProcessExits(context) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(screenContentPadding(systemBarsPadding, SCREEN_PADDING))
    ) {
        Text(
            text = stringResource(R.string.diagnostics_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))

        CrashReportCard(
            report = crashReport,
            onCopy = { report ->
                // Android 13+ shows its own copy confirmation, so the app adds no toast of its own.
                coroutineScope.launch {
                    clipboard.setClipEntry(
                        ClipEntry(ClipData.newPlainText(CLIP_LABEL, report))
                    )
                }
            },
            onShare = { shareReport(context, it) },
            onReport = { openExternalUrl(context, ISSUES_URL) },
            onClear = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) { store.clear() }
                    crashReport = null
                }
            }
        )
        Spacer(modifier = Modifier.height(CARD_GAP))
        ProcessExitsCard(exits = processExits)
    }
}

@Composable
private fun CrashReportCard(
    report: String?,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onReport: () -> Unit,
    onClear: () -> Unit
) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.diagnostics_crash_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        if (report == null) {
            Text(
                text = stringResource(R.string.diagnostics_crash_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@NothingCard
        }

        Text(
            text = stringResource(R.string.diagnostics_crash_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(BODY_GAP))
        // A stack trace is wide: let it scroll sideways inside the card instead of wrapping into
        // an unreadable block or pushing the page out of the viewport.
        Text(
            text = report,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = false,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        )
        Spacer(modifier = Modifier.height(BODY_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(BUTTON_GAP)) {
            NothingButton(
                text = stringResource(R.string.diagnostics_action_copy),
                onClick = { onCopy(report) },
                modifier = Modifier.fillMaxWidth()
            )
            NothingButton(
                text = stringResource(R.string.diagnostics_action_share),
                onClick = { onShare(report) },
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
            NothingButton(
                text = stringResource(R.string.diagnostics_action_report),
                onClick = onReport,
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
            NothingButton(
                text = stringResource(R.string.diagnostics_action_clear),
                onClick = onClear,
                outlined = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProcessExitsCard(exits: List<ProcessExit>) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.diagnostics_exits_label))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Text(
            text = stringResource(R.string.diagnostics_exits_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(BODY_GAP))
        if (exits.isEmpty()) {
            Text(
                text = stringResource(R.string.diagnostics_exits_empty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@NothingCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(EXIT_ROW_GAP)) {
            for (exit in exits) {
                Text(
                    text = formatProcessExit(exit),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

private fun shareReport(context: android.content.Context, report: String) {
    val share = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, report)
    }
    try {
        context.startActivity(Intent.createChooser(share, null))
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, R.string.diagnostics_share_failed, Toast.LENGTH_LONG).show()
    }
}
