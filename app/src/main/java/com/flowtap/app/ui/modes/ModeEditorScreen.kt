package com.flowtap.app.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.R
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.PaperCardShape
import com.flowtap.app.ui.theme.PrimaryButtonShape
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.ui.components.AppIcon
import com.flowtap.app.ui.components.EmptyState

/**
 * Full-screen mode settings. Opened for both creating ([modeId] == null) and
 * editing — every per-mode setting lives here with room to breathe instead of
 * a cramped dialog. Back (system or app-bar) asks to discard when dirty.
 *
 * The Tags section lets you tap tags to pair them with this mode — they are
 * re-bound to it on save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeEditorScreen(
    container: AppContainer,
    modeId: Long?,
    onDone: () -> Unit,
) {
    val vm: ModeEditorViewModel = viewModel(factory = ModeEditorViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val modes by vm.modes.collectAsStateWithLifecycle()
    val allTags by vm.tags.collectAsStateWithLifecycle()
    val tagsByMode by vm.tagsByMode.collectAsStateWithLifecycle()
    val modeNames by vm.modeNames.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val saveError by vm.saveError.collectAsStateWithLifecycle()
    val attached by vm.attachTagIds.collectAsStateWithLifecycle()
    val isCreate = modeId == null
    // Loading / not-found states (M5): never a blank screen.
    if (!isCreate && modes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.modes_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
        }
        return
    }
    val mode: FocusMode? = if (isCreate) {
        FocusMode(name = "", iconName = "work")
    } else {
        modes.firstOrNull { it.id == modeId }
    }
    if (mode == null) {
        // Deleted elsewhere: leave once loaded and still absent.
        LaunchedEffect(modeId) { onDone() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.modes_not_found),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
        }
        return
    }

    // M1: draft lives in the ViewModel, surviving rotation/recreation.
    val state = vm.draftFor(mode)
    val attachTagIds = attached
    var showDiscard by remember { mutableStateOf(false) }
    var showAppPicker by rememberSaveable { mutableStateOf(false) }
    val sidePadding = rememberAdaptiveSidePadding()
    val activeSession by com.flowtap.app.session.SessionManager.active.collectAsStateWithLifecycle()
    val editingActive = activeSession?.modeId == mode.id

    // Tags already here can't be re-paired; everything else is tappable.
    val candidates = remember(allTags, modeId) { allTags.filter { it.modeId != modeId } }
    val pairedHere = if (isCreate) emptyList() else tagsByMode[modeId] ?: emptyList()
    val dirty = state.isDirty() || attachTagIds.isNotEmpty()

    fun save() {
        if (!state.saveEnabled || isSaving) return
        val updated = state.buildUpdated()
        if (isCreate && updated.name.isBlank()) return
        vm.save(updated, attachTagIds) { onDone() }
    }

    fun tryBack() {
        if (dirty) showDiscard = true else onDone()
    }

    if (showAppPicker) {
        ModeAppPickerScreen(
            state = state,
            onDone = { showAppPicker = false },
        )
        return
    }

    BackHandler { tryBack() }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                // MainNav's Scaffold already offsets below the status bar.
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        if (isCreate) {
                            stringResource(R.string.modes_create_mode)
                        } else {
                            stringResource(R.string.tags_edit_mode)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::tryBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.card,
                    titleContentColor = palette.textPrimary,
                    navigationIconContentColor = palette.textPrimary,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .imePadding()
                    .padding(horizontal = sidePadding, vertical = Spacing.sm),
            ) {
                Button(
                    onClick = ::save,
                    enabled = state.saveEnabled && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.primaryButtonHeight),
                    shape = PrimaryButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                ) {
                    Text(
                        if (isSaving) stringResource(R.string.common_saving) else stringResource(R.string.common_save),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sidePadding),
        ) {
            Spacer(Modifier.height(Spacing.md))
            // M4: edits apply to the next session while one is running.
            if (editingActive) {
                Text(
                    stringResource(R.string.modes_next_session_note),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.accentText,
                )
                Spacer(Modifier.height(Spacing.sm))
            }
            ModeEditorForm(
                state = state,
                launchListMaxHeight = 280.dp,
                onOpenAppPicker = { showAppPicker = true },
            )

            Spacer(Modifier.height(Spacing.sectionGap))
            Text(
                stringResource(R.string.modes_tags_title),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Text(
                stringResource(R.string.modes_tags_sub),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
            // M5: paired tags by name, not just a count.
            if (pairedHere.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xs))
                pairedHere.forEach { tag ->
                    Text(
                        "• ${tag.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.accentText,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            if (candidates.isEmpty()) {
                Text(
                    stringResource(R.string.modes_tags_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(candidates, key = { it.id }) { tag ->
                        val checked = tag.id in attachTagIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.presetMinHeight)
                                .clip(MaterialTheme.shapes.small)
                                .toggleable(
                                    value = checked,
                                    onValueChange = { vm.toggleAttach(tag.id) },
                                    role = Role.Checkbox,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.size(Spacing.sm))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(palette.ringTrack, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Sell,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = palette.accentText,
                                )
                            }
                            Spacer(Modifier.size(Spacing.sm))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tag.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.textPrimary,
                                )
                                Text(
                                    stringResource(
                                        R.string.modes_tags_now_x,
                                        modeNames[tag.modeId]
                                            ?: stringResource(R.string.tags_unknown_mode),
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }
                }
            }

            if (saveError != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    saveError ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = {
                    vm.clearSaveError()
                    save()
                }) {
                    Text(stringResource(R.string.common_retry))
                }
            }
            Spacer(Modifier.height(Spacing.listBottom))
            Spacer(Modifier.height(Dimens.primaryButtonHeight))
        }
    }

    if (showDiscard) {
        AlertDialog(
            onDismissRequest = { showDiscard = false },
            title = { Text(stringResource(R.string.modes_discard_title)) },
            text = { Text(stringResource(R.string.modes_discard_text)) },
            confirmButton = {
                TextButton(onClick = onDone) {
                    Text(
                        stringResource(R.string.modes_discard_ok),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscard = false }) {
                    Text(stringResource(R.string.modes_discard_keep))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeAppPickerScreen(
    state: ModeEditorState,
    onDone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val palette = LocalFocusPalette.current
    val sidePadding = rememberAdaptiveSidePadding()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedOnly by rememberSaveable { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    fun loadApps() {
        apps = null
        loadFailed = false
    }

    LaunchedEffect(apps, loadFailed) {
        if (apps == null && !loadFailed) {
            apps = runCatching { AppCatalog.load(context) }
                .onFailure { loadFailed = true }
                .getOrNull()
        }
    }

    BackHandler(onBack = onDone)
    val selectedPackages = if (state.allowlistOnly) state.allowed else state.blocked
    val installed = apps.orEmpty()
    val installedPackages = remember(installed) { installed.mapTo(mutableSetOf()) { it.pkg } }
    val unavailable = remember(selectedPackages, installedPackages) {
        selectedPackages.filterNot { it in installedPackages }.sorted()
    }
    val visibleApps = remember(installed, query, selectedOnly, selectedPackages) {
        val needle = query.trim()
        installed
            .asSequence()
            .filter { !selectedOnly || it.pkg in selectedPackages }
            .filter {
                needle.isEmpty() || it.label.contains(needle, ignoreCase = true) ||
                    it.pkg.contains(needle, ignoreCase = true)
            }
            .sortedWith(compareByDescending<InstalledApp> { it.pkg in selectedPackages }
                .thenBy { it.label.lowercase() })
            .toList()
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(R.string.modes_choose_apps_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.card,
                    titleContentColor = palette.textPrimary,
                    navigationIconContentColor = palette.textPrimary,
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .imePadding()
                    .padding(horizontal = sidePadding, vertical = Spacing.sm),
            ) {
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
                    shape = PrimaryButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = palette.onAccent,
                    ),
                ) {
                    Text(
                        pluralStringResource(
                            R.plurals.plural_done_apps,
                            selectedPackages.size,
                            selectedPackages.size,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sidePadding),
        ) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(
                    if (state.allowlistOnly) R.string.modes_picker_allow_sub
                    else R.string.modes_picker_block_sub,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.modes_app_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                Icons.Outlined.Clear,
                                contentDescription = stringResource(R.string.ob_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = !selectedOnly,
                    onClick = { selectedOnly = false },
                    label = { Text(stringResource(R.string.modes_filter_all)) },
                )
                FilterChip(
                    selected = selectedOnly,
                    onClick = { selectedOnly = true },
                    label = {
                        Text(
                            stringResource(
                                R.string.modes_filter_selected_x,
                                selectedPackages.size,
                            )
                        )
                    },
                )
            }
            Spacer(Modifier.height(Spacing.sm))

            when {
                apps == null && !loadFailed -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.modes_apps_loading),
                        color = palette.textSecondary,
                    )
                }
                loadFailed -> EmptyState(
                    title = stringResource(R.string.modes_apps_error),
                    subtitle = stringResource(R.string.modes_apps_error_sub),
                    action = stringResource(R.string.common_retry),
                    onAction = ::loadApps,
                )
                visibleApps.isEmpty() && (unavailable.isEmpty() || !selectedOnly) -> EmptyState(
                    title = if (query.isBlank()) {
                        stringResource(R.string.modes_apps_empty)
                    } else {
                        stringResource(R.string.ob_no_match, query.trim())
                    },
                    subtitle = stringResource(R.string.ob_try_search),
                )
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.xs),
                ) {
                    items(visibleApps, key = { it.pkg }) { app ->
                        val checked = app.pkg in selectedPackages
                        AppPickerRow(
                            label = app.label,
                            pkg = app.pkg,
                            checked = checked,
                            unavailable = false,
                            onToggle = {
                                if (state.allowlistOnly) {
                                    state.allowed = if (checked) state.allowed - app.pkg else state.allowed + app.pkg
                                } else {
                                    state.blocked = if (checked) state.blocked - app.pkg else state.blocked + app.pkg
                                }
                            },
                        )
                    }
                    if (selectedOnly) {
                        items(unavailable, key = { "unavailable-$it" }) { pkg ->
                            AppPickerRow(
                                label = pkg,
                                pkg = pkg,
                                checked = true,
                                unavailable = true,
                                onToggle = {
                                    if (state.allowlistOnly) state.allowed = state.allowed - pkg
                                    else state.blocked = state.blocked - pkg
                                },
                            )
                        }
                    }
                    item { Spacer(Modifier.height(Spacing.listBottom)) }
                }
            }
        }
    }
}

@Composable
private fun AppPickerRow(
    label: String,
    pkg: String,
    checked: Boolean,
    unavailable: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.settingRowMinHeight)
            .background(if (checked) palette.selectedSurface else palette.card, PaperCardShape)
            .border(
                Dimens.cardBorderWidth,
                if (checked) palette.accent else palette.cardBorder,
                PaperCardShape,
            )
            .clip(PaperCardShape)
            .toggleable(
                value = checked,
                onValueChange = { onToggle() },
                role = Role.Checkbox,
            )
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (unavailable) {
            Box(
                modifier = Modifier.size(36.dp).background(palette.ringTrack, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Block, contentDescription = null, tint = palette.textTertiary)
            }
        } else {
            AppIcon(pkg = pkg, label = label, size = 36.dp, corner = 10.dp)
        }
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
            Text(
                if (unavailable) stringResource(R.string.modes_app_unavailable)
                else if (checked) stringResource(R.string.modes_app_selected)
                else stringResource(R.string.modes_app_available),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
        Checkbox(checked = checked, onCheckedChange = null)
    }
}
