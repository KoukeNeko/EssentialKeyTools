package dev.koukeneko.essentialkeytools.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import dev.koukeneko.essentialkeytools.R
import dev.koukeneko.essentialkeytools.actions.KeyAction
import dev.koukeneko.essentialkeytools.core.KeyGesture
import dev.koukeneko.essentialkeytools.settings.SettingsRepository
import dev.koukeneko.essentialkeytools.ui.LaunchableApp
import dev.koukeneko.essentialkeytools.ui.InstalledApps
import dev.koukeneko.essentialkeytools.ui.UiLabels
import dev.koukeneko.essentialkeytools.ui.components.NothingCard
import dev.koukeneko.essentialkeytools.ui.components.NothingSectionLabel
import dev.koukeneko.essentialkeytools.ui.screenContentPadding
import dev.koukeneko.essentialkeytools.ui.theme.NothingGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SCREEN_PADDING = 24.dp
private val TITLE_TO_CONTENT_GAP = 32.dp
private val CARD_GAP = 16.dp
private val LABEL_GAP = 12.dp
private val SECTION_GAP = 24.dp
private val ROW_GAP = 4.dp
private val ROW_VERTICAL_PADDING = 12.dp
private val ICON_SIZE = 32.dp
private val ICON_TO_LABEL_GAP = 16.dp

/**
 * Lets the user pick the action bound to a single [gesture]: a search field, built-in actions,
 * then every launchable app. The whole screen is one scrolling [LazyColumn] so the full app list
 * renders inline with the page scroll (no nested scroll region). Selecting a row saves the mapping
 * and returns via [onActionSaved].
 */
@Composable
fun ActionPickerScreen(
    gesture: KeyGesture,
    onActionSaved: () -> Unit,
    systemBarsPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SettingsRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var appsLoaded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { InstalledApps.loadLaunchable(context) }
        appsLoaded = true
    }

    fun save(action: KeyAction) {
        coroutineScope.launch {
            repository.setActionFor(gesture, action)
            onActionSaved()
        }
    }

    val matchingActions = filterBuiltInActions(UiLabels.builtInActions, query, context)
    val matchingApps = filterApps(apps, query)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = screenContentPadding(systemBarsPadding, SCREEN_PADDING)
    ) {
        item {
            Text(
                text = stringResource(R.string.picker_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(LABEL_GAP))
            Text(
                text = stringResource(UiLabels.gestureLabelRes(gesture)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(TITLE_TO_CONTENT_GAP))
            SearchField(query = query, onQueryChange = { query = it })
            Spacer(modifier = Modifier.height(CARD_GAP))
        }

        if (matchingActions.isNotEmpty()) {
            item {
                BuiltInActionsCard(actions = matchingActions, onSelect = ::save)
                Spacer(modifier = Modifier.height(SECTION_GAP))
            }
        }

        item {
            NothingSectionLabel(text = stringResource(R.string.picker_section_apps))
            Spacer(modifier = Modifier.height(LABEL_GAP))
            if (!appsLoaded) {
                Text(
                    text = stringResource(R.string.picker_apps_loading),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(matchingApps, key = { app -> app.packageName }) { app ->
            AppRow(app = app, onClick = { save(KeyAction.LaunchApp(app.packageName)) })
        }
    }
}

/** Case-insensitive substring match on the localized built-in action label. */
private fun filterBuiltInActions(
    actions: List<KeyAction>,
    query: String,
    context: Context
): List<KeyAction> {
    if (query.isBlank()) return actions
    val needle = query.trim().lowercase()
    return actions.filter { action ->
        val label = context.getString(UiLabels.builtInActionLabelRes(action.id))
        label.lowercase().contains(needle)
    }
}

/** Case-insensitive substring match on either the app label or its package name. */
private fun filterApps(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
    if (query.isBlank()) return apps
    val needle = query.trim().lowercase()
    return apps.filter { app ->
        app.label.lowercase().contains(needle) || app.packageName.lowercase().contains(needle)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = CircleShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = stringResource(R.string.picker_search_hint),
                style = MaterialTheme.typography.labelSmall,
                color = NothingGray
            )
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        // Monochrome outline only: white when focused, gray at rest — never a Material accent.
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            cursorColor = MaterialTheme.colorScheme.onBackground,
            focusedBorderColor = MaterialTheme.colorScheme.onBackground,
            unfocusedBorderColor = NothingGray
        )
    )
}

@Composable
private fun BuiltInActionsCard(actions: List<KeyAction>, onSelect: (KeyAction) -> Unit) {
    NothingCard(modifier = Modifier.fillMaxWidth()) {
        NothingSectionLabel(text = stringResource(R.string.picker_section_builtin))
        Spacer(modifier = Modifier.height(LABEL_GAP))
        Column(verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
            for (action in actions) {
                ActionTextRow(
                    label = stringResource(UiLabels.builtInActionLabelRes(action.id)),
                    onClick = { onSelect(action) }
                )
            }
        }
    }
}

@Composable
private fun ActionTextRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_VERTICAL_PADDING)
    )
}

@Composable
private fun AppRow(app: LaunchableApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = ROW_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconBitmap = remember(app.packageName) { app.icon.toBitmap().asImageBitmap() }
        Image(
            bitmap = iconBitmap,
            contentDescription = app.label,
            modifier = Modifier.size(ICON_SIZE)
        )
        Spacer(modifier = Modifier.width(ICON_TO_LABEL_GAP))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
