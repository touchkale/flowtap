package com.flowtap.app.ui.modes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                appListMaxHeight = 320.dp,
                launchListMaxHeight = 280.dp,
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
