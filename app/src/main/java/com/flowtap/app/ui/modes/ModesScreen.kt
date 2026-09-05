package com.flowtap.app.ui.modes

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.R
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.di.AppContainer
import com.flowtap.app.nfc.PairTagActivity
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.components.EmptyState
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.util.AppLabels

@Composable
fun ModesScreen(
    container: AppContainer,
    onCreateMode: () -> Unit,
    onEditMode: (Long) -> Unit,
) {
    val context = LocalContext.current
    val vm: ModesViewModel = viewModel(factory = ModesViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val modes by vm.modes.collectAsStateWithLifecycle()
    val tagsByMode by vm.tagsByMode.collectAsStateWithLifecycle()
    val active by SessionManager.active.collectAsStateWithLifecycle()

    var deletingMode by remember { mutableStateOf<FocusMode?>(null) }
    val sidePadding = rememberAdaptiveSidePadding()

    // Resolve launch-package labels once per mode list for the chips.
    val launchLabels: Map<String, String> = remember(modes) {
        val pkgs = modes.mapNotNull { it.launchPackage }.distinct()
        if (pkgs.isEmpty()) emptyMap() else AppLabels.labeledPackages(context, pkgs).toMap()
    }

    // Hide the Add button while scrolling down (swipe up), show it again on
    // scroll up — so it never covers content being read.
    val listState = rememberLazyListState()
    var fabVisible by remember { mutableStateOf(true) }
    var lastIndex by remember { mutableIntStateOf(0) }
    var lastOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                fabVisible = if (index != lastIndex) {
                    index < lastIndex
                } else {
                    offset <= lastOffset
                }
                lastIndex = index
                lastOffset = offset
            }
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onCreateMode,
                    containerColor = palette.accent,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        stringResource(R.string.modes_add),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = sidePadding,
                end = sidePadding,
                top = Spacing.screenTop,
                bottom = Spacing.listBottom,
            ),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.modes_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        stringResource(R.string.modes_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                Spacer(Modifier.height(Spacing.sectionGap))
            }

            if (modes.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.modes_empty_title),
                        subtitle = stringResource(R.string.modes_empty_subtitle),
                    )
                }
                item(key = "create") {
                    Column {
                        Spacer(Modifier.height(Spacing.sm))
                        OutlinedButton(
                            onClick = onCreateMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.primaryButtonHeight),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.size(Spacing.xs))
                            Text(
                                stringResource(R.string.modes_create_mode),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, PairTagActivity::class.java))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.primaryButtonHeight),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        ) {
                            Icon(Icons.Outlined.Nfc, contentDescription = null)
                            Spacer(Modifier.size(Spacing.sm))
                            Text(
                                stringResource(R.string.home_pair_tag),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            } else {
                items(modes, key = { it.id }) { mode ->
                    val locked = active?.modeId == mode.id
                    ModeCard(
                        mode = mode,
                        tagCount = tagsByMode[mode.id]?.size ?: 0,
                        launchLabel = mode.launchPackage?.let { launchLabels[it] },
                        onEdit = { onEditMode(mode.id) },
                        onDuplicate = { vm.duplicateMode(mode) },
                        onDelete = { deletingMode = mode },
                        deleteEnabled = !locked,
                    )
                    Spacer(Modifier.height(Spacing.rowGap))
                }
            }
        }
    }

    deletingMode?.let { mode ->
        val boundTags = tagsByMode[mode.id].orEmpty()
        val locked = active?.modeId == mode.id
        AlertDialog(
            onDismissRequest = { deletingMode = null },
            title = { Text(stringResource(R.string.tags_delete_title, mode.name)) },
            text = {
                Text(
                    when {
                        locked -> stringResource(R.string.modes_active_locked)
                        boundTags.isEmpty() -> stringResource(R.string.tags_delete_keep_history)
                        else -> pluralStringResource(
                            R.plurals.plural_delete_tags,
                            boundTags.size,
                            boundTags.size,
                            boundTags.joinToString { it.name },
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!locked) vm.deleteMode(mode)
                        deletingMode = null
                    },
                    enabled = !locked,
                ) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMode = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
