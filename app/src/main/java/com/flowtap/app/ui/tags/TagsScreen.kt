package com.flowtap.app.ui.tags

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.R
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.di.AppContainer
import com.flowtap.app.nfc.PairTagActivity
import com.flowtap.app.ui.components.EmptyState
import com.flowtap.app.ui.components.SectionHeader
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.PaperCardShape
import com.flowtap.app.ui.theme.PrimaryButtonShape
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding

@Composable
fun TagsScreen(container: AppContainer) {
    val context = LocalContext.current
    val vm: TagsViewModel = viewModel(factory = TagsViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val modes by vm.modes.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val modeNames by vm.modeNames.collectAsStateWithLifecycle()

    var editingTag by remember { mutableStateOf<FocusTag?>(null) }
    var deletingTag by remember { mutableStateOf<FocusTag?>(null) }
    val sidePadding = rememberAdaptiveSidePadding()

    // MainNav's Scaffold already applies the status-bar inset; only the
    // navigation-bar inset is needed here so the top matches Home exactly.
    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = sidePadding),
        ) {
            Spacer(Modifier.height(Spacing.screenTop))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.tags_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary,
                    )
                }
                Button(
                    onClick = {
                        context.startActivity(Intent(context, PairTagActivity::class.java))
                    },
                    shape = PrimaryButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    modifier = Modifier.height(Dimens.presetMinHeight),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        stringResource(R.string.tags_add),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sectionGap))

            if (tags.isEmpty()) {
                EmptyState(
                    title = stringResource(R.string.tags_empty_title),
                    subtitle = stringResource(R.string.tags_empty_subtitle),
                    pulsing = true,
                )
                Spacer(Modifier.height(Spacing.lg))
                Button(
                    onClick = {
                        context.startActivity(Intent(context, PairTagActivity::class.java))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.primaryButtonHeight),
                    shape = PrimaryButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                ) {
                    Text(
                        stringResource(R.string.tags_pair_first),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                SectionHeader(title = stringResource(R.string.tags_your_tags))
                Spacer(Modifier.height(Spacing.headerGap))
                tags.forEach { tag ->
                    TagCard(
                        tag = tag,
                        modeName = modeNames[tag.modeId] ?: stringResource(R.string.tags_unknown_mode),
                        onClick = { editingTag = tag },
                        onDelete = { deletingTag = tag },
                    )
                    Spacer(Modifier.height(Spacing.rowGap))
                }
            }
            Spacer(Modifier.height(Spacing.listBottom))
        }
    }

    editingTag?.let { tag ->
        TagEditDialog(
            tag = tag,
            modes = modes,
            onDismiss = { editingTag = null },
            onSave = { updated ->
                vm.saveTag(updated)
                editingTag = null
            },
        )
    }

    // U1: destructive deletes confirm first; history is preserved.
    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(stringResource(R.string.tags_delete_title, tag.name)) },
            text = { Text(stringResource(R.string.tags_delete_keep_history)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteTag(tag)
                    deletingTag = null
                }) {
                    Text(
                        stringResource(R.string.common_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun TagEditDialog(
    tag: FocusTag,
    modes: List<FocusMode>,
    onDismiss: () -> Unit,
    onSave: (FocusTag) -> Unit,
) {
    var name by remember(tag.id) { mutableStateOf(tag.name) }
    var modeId by remember(tag.id) { mutableLongStateOf(tag.modeId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tags_edit_tag)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tags_tag_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.tags_bound_mode),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                LazyColumn(modifier = Modifier.heightIn(max = Dimens.dialogMaxHeight)) {
                    items(modes, key = { it.id }) { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.presetMinHeight)
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = modeId == mode.id,
                                    onClick = { modeId = mode.id },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = modeId == mode.id, onClick = null)
                            Spacer(Modifier.size(Spacing.sm))
                            Text(mode.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tag.copy(name = name.trim().ifBlank { tag.name }, modeId = modeId))
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun TagCard(
    tag: FocusTag,
    modeName: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    val shortUid = if (tag.uidHex.length > 12) "${tag.uidHex.take(12)}…" else tag.uidHex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .clip(PaperCardShape)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(palette.ringTrack, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Sell,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = palette.accentText,
            )
        }
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(tag.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                stringResource(R.string.tags_uid_mode, shortUid, modeName),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.tags_delete_tag_cd),
                tint = palette.textTertiary,
            )
        }
    }
}
