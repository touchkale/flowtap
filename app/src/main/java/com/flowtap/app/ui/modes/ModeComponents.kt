package com.flowtap.app.ui.modes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowtap.app.R
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.ui.components.AppIcon
import com.flowtap.app.ui.components.RecommendedBlockCard
import com.flowtap.app.ui.components.StatusChip
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.util.RecommendedApps

data class InstalledApp(val pkg: String, val label: String)

fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName
            if (pkg == context.packageName) return@mapNotNull null
            InstalledApp(pkg, ri.loadLabel(pm).toString())
        }
}

fun formatAutoEnd(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

fun formatGoalShort(totalMinutes: Int): String = when {
    totalMinutes <= 0 -> "0m"
    totalMinutes < 60 -> "${totalMinutes}m"
    totalMinutes % 60 == 0 -> "${totalMinutes / 60}h"
    else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
}

private val AUTO_PRESETS = listOf(15, 25, 30, 45, 60, 90)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModeCard(
    mode: FocusMode,
    tagCount: Int,
    launchLabel: String?,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    deleteEnabled: Boolean = true,
) {
    val palette = LocalFocusPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                mode.name,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.tags_edit_mode_cd),
                    tint = palette.accentText,
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(
                    Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.modes_duplicate_cd),
                    tint = palette.accentText,
                )
            }
            IconButton(onClick = onDelete, enabled = deleteEnabled) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.tags_delete_mode_cd),
                    tint = if (deleteEnabled) palette.textTertiary else palette.textTertiary.copy(alpha = 0.4f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                StatusChip(
                    text = if (mode.dndEnabled) {
                        stringResource(R.string.tags_chip_dnd)
                    } else {
                        stringResource(R.string.tags_chip_no_dnd)
                    },
                    icon = Icons.Outlined.NotificationsOff,
                )
                StatusChip(
                    text = if (mode.strictMode) {
                        stringResource(R.string.tags_chip_strict)
                    } else {
                        stringResource(R.string.tags_chip_flexible)
                    },
                    icon = Icons.Outlined.Lock,
                )
                if (mode.allowlistOnly) {
                    StatusChip(
                        text = stringResource(
                            R.string.modes_chip_allowlist_x,
                            mode.allowedApps.size,
                        ),
                        icon = Icons.Outlined.Checklist,
                    )
                } else {
                    StatusChip(
                        text = pluralStringResource(
                            R.plurals.plural_blocked_short,
                            mode.blockedApps.size,
                            mode.blockedApps.size,
                        ),
                        icon = Icons.Outlined.Block,
                    )
                }
                if (mode.keepAwake == true) {
                    StatusChip(text = stringResource(R.string.modes_chip_awake))
                }
                mode.autoEndMinutes?.let {
                    StatusChip(
                        text = stringResource(R.string.modes_chip_auto_x, formatAutoEnd(it)),
                        icon = Icons.Outlined.Timer,
                    )
                }
                StatusChip(
                    text = stringResource(
                        R.string.modes_chip_goal_x,
                        formatGoalShort(mode.dailyGoalMinutes),
                    ),
                    icon = Icons.Outlined.Timer,
                )
                if (mode.launchPackage != null) {
                    StatusChip(
                        text = stringResource(
                            R.string.modes_chip_launch_x,
                            launchLabel ?: mode.launchPackage,
                        ),
                        icon = Icons.Outlined.Launch,
                    )
                }
                StatusChip(
                    text = pluralStringResource(R.plurals.plural_mode_tags, tagCount, tagCount),
                )
            }
    }
}

/**
 * Holds the editable state of one mode. Shared by the full-screen editor —
 * [buildUpdated] produces the [FocusMode] to persist, [isDirty] guards
 * against losing unsaved changes.
 */
class ModeEditorState(val initial: FocusMode) {
    var name by mutableStateOf(initial.name)
    var dnd by mutableStateOf(initial.dndEnabled)
    var strict by mutableStateOf(initial.strictMode)
    var keepAwake by mutableStateOf(initial.keepAwake)
    var autoEnd by mutableStateOf(initial.autoEndMinutes)
    var customMinutes by mutableStateOf(
        initial.autoEndMinutes?.takeIf { it !in AUTO_PRESETS }?.toString() ?: ""
    )
    var allowlistOnly by mutableStateOf(initial.allowlistOnly)
    var blocked by mutableStateOf(initial.blockedApps.toSet())
    var allowed by mutableStateOf(initial.allowedApps.toSet())
    var launchPkg by mutableStateOf(initial.launchPackage)
    var goalHours by mutableStateOf((initial.dailyGoalMinutes.coerceIn(5, 1440)) / 60)
    var goalMinutes by mutableStateOf((initial.dailyGoalMinutes.coerceIn(5, 1440)) % 60)

    val customValid: Boolean get() {
        val v = customMinutes.trim().toIntOrNull()
        return v != null && v in 5..720
    }

    val goalTotalMinutes: Int get() = (goalHours * 60 + goalMinutes).coerceIn(5, 1440)
    val goalValid: Boolean get() = goalTotalMinutes in 5..1440

    val saveEnabled: Boolean get() = name.isNotBlank() && (customMinutes.isBlank() || customValid) && goalValid

    private val resolvedAutoEnd: Int? get() = if (customMinutes.isNotBlank()) {
        customMinutes.trim().toIntOrNull()?.takeIf { it in 5..720 } ?: autoEnd
    } else {
        autoEnd
    }

    fun buildUpdated(): FocusMode = initial.copy(
        name = name.trim(),
        dndEnabled = dnd,
        strictMode = strict,
        keepAwake = keepAwake,
        autoEndMinutes = resolvedAutoEnd,
        allowlistOnly = allowlistOnly,
        blockedApps = blocked.toList(),
        allowedApps = allowed.toList(),
        launchPackage = launchPkg,
        dailyGoalMinutes = goalTotalMinutes,
    )

    fun isDirty(): Boolean = buildUpdated() != initial
}

@Composable
fun rememberModeEditorState(mode: FocusMode): ModeEditorState =
    remember(mode.id) { ModeEditorState(mode) }

/**
 * The full mode-settings form (no scrolling of its own — the host screen
 * scrolls). App pickers keep bounded lazy lists via [appListMaxHeight] /
 * [launchListMaxHeight] so hundreds of apps stay smooth.
 */
@Composable
fun ModeEditorForm(
    state: ModeEditorState,
    modifier: Modifier = Modifier,
    appListMaxHeight: Dp = 220.dp,
    launchListMaxHeight: Dp = 180.dp,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var appSearch by remember { mutableStateOf("") }
    var launchSearch by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    LaunchedEffect(Unit) { apps = loadLaunchableApps(context) }

    val sortedApps = remember(apps, appSearch) {
        val q = appSearch.trim().lowercase()
        apps.sortedBy { it.label.lowercase() }
            .filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
    }
    // Installed social/distracting apps first while browsing a blocklist.
    val recommended = remember(apps) {
        val byPkg = apps.associateBy { it.pkg }
        RecommendedApps.installed(apps.map { it.pkg }.toSet()).mapNotNull { byPkg[it] }
    }
    val showRecommended = !state.allowlistOnly && appSearch.isBlank() && recommended.isNotEmpty()
    val unselectedRecommended = recommended.filter { it.pkg !in state.blocked }
    val displayApps = if (showRecommended) {
        val recPkgs = recommended.map { it.pkg }.toSet()
        recommended + sortedApps.filter { it.pkg !in recPkgs }
    } else {
        sortedApps
    }
    val sortedLaunchApps = remember(apps, launchSearch) {
        val q = launchSearch.trim().lowercase()
        apps.sortedBy { it.label.lowercase() }
            .filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
    }

    Column(modifier = modifier) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = { Text(stringResource(R.string.tags_mode_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.sm))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = state.dnd,
                            onValueChange = { state.dnd = it },
                            role = Role.Switch,
                        )
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.tags_dnd),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.tags_dnd_sub),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.dnd, onCheckedChange = null)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(
                            value = state.strict,
                            onValueChange = { state.strict = it },
                            role = Role.Switch,
                        )
                        .padding(vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.tags_strict),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.tags_strict_sub),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.strict, onCheckedChange = null)
                }

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_keep_awake),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.modes_keep_awake_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                KeepAwakeSelector(selected = state.keepAwake, onSelect = { state.keepAwake = it })

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_auto_end),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.modes_auto_end_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                AutoEndSelector(
                    selected = state.autoEnd,
                    onSelect = {
                        state.autoEnd = it
                        if (it == null || it in AUTO_PRESETS) state.customMinutes = ""
                    },
                )
                Spacer(Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = state.customMinutes,
                    onValueChange = { input ->
                        state.customMinutes = input.filter { c -> c.isDigit() }.take(3)
                        state.customMinutes.trim().toIntOrNull()?.takeIf { v -> v in 5..720 }?.let {
                            state.autoEnd = it
                        }
                    },
                    label = { Text(stringResource(R.string.modes_auto_custom_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.customMinutes.isNotBlank() && !state.customValid,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_goal_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.modes_goal_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                GoalPicker(
                    hours = state.goalHours,
                    minutes = state.goalMinutes,
                    onHoursChange = { state.goalHours = it.coerceIn(0, 24) },
                    onMinutesChange = { state.goalMinutes = it.coerceIn(0, 59) },
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_block_mode),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.modes_block_mode_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                BlockModeSelector(selectedAllowlist = state.allowlistOnly, onSelect = { state.allowlistOnly = it })
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    if (state.allowlistOnly) {
                        stringResource(R.string.modes_allowed_apps)
                    } else {
                        stringResource(R.string.tags_blocked_apps)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = appSearch,
                    onValueChange = { appSearch = it },
                    label = { Text(stringResource(R.string.modes_app_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xs))
                if (showRecommended && unselectedRecommended.isNotEmpty()) {
                    RecommendedBlockCard(
                        unselectedCount = unselectedRecommended.size,
                        onBlockAll = {
                            state.blocked = state.blocked + recommended.map { it.pkg }.toSet()
                        },
                    )
                    Spacer(Modifier.height(Spacing.xs))
                }
                LazyColumn(modifier = Modifier.heightIn(max = appListMaxHeight)) {
                    items(displayApps, key = { it.pkg }) { app ->
                        val checked = if (state.allowlistOnly) app.pkg in state.allowed else app.pkg in state.blocked
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.presetMinHeight)
                                .clip(MaterialTheme.shapes.small)
                                .toggleable(
                                    value = checked,
                                    onValueChange = {
                                        if (state.allowlistOnly) {
                                            state.allowed = if (checked) state.allowed - app.pkg else state.allowed + app.pkg
                                        } else {
                                            state.blocked = if (checked) state.blocked - app.pkg else state.blocked + app.pkg
                                        }
                                    },
                                    role = Role.Checkbox,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.size(Spacing.sm))
                            AppIcon(pkg = app.pkg, label = app.label, size = 32.dp, corner = 9.dp)
                            Spacer(Modifier.size(Spacing.sm))
                            Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_launch_app),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.modes_launch_sub),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                OutlinedTextField(
                    value = launchSearch,
                    onValueChange = { launchSearch = it },
                    label = { Text(stringResource(R.string.modes_launch_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.xs))
                LazyColumn(modifier = Modifier.heightIn(max = launchListMaxHeight)) {
                    item(key = "none") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.presetMinHeight)
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = state.launchPkg == null,
                                    onClick = { state.launchPkg = null },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.launchPkg == null, onClick = null)
                            Spacer(Modifier.size(Spacing.sm))
                            Text(
                                stringResource(R.string.modes_launch_none),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    items(sortedLaunchApps, key = { it.pkg }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Dimens.presetMinHeight)
                                .clip(MaterialTheme.shapes.small)
                                .selectable(
                                    selected = state.launchPkg == app.pkg,
                                    onClick = { state.launchPkg = app.pkg },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.launchPkg == app.pkg, onClick = null)
                            Spacer(Modifier.size(Spacing.sm))
                            AppIcon(pkg = app.pkg, label = app.label, size = 32.dp, corner = 9.dp)
                            Spacer(Modifier.size(Spacing.sm))
                            Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
    }
}

@Composable
private fun KeepAwakeSelector(selected: Boolean?, onSelect: (Boolean?) -> Unit) {
    val palette = LocalFocusPalette.current
    val options = listOf(
        Triple(null, stringResource(R.string.modes_keep_system), null),
        Triple(true, stringResource(R.string.modes_keep_on), null),
        Triple(false, stringResource(R.string.modes_keep_off), null),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        options.forEach { (value, label, _) ->
            val isSelected = selected == value
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.presetMinHeight)
                    .background(
                        if (isSelected) palette.accent else palette.ringTrack,
                        MaterialTheme.shapes.small,
                    )
                    .border(
                        Dimens.cardBorderWidth,
                        if (isSelected) palette.accent else palette.cardBorder,
                        MaterialTheme.shapes.small,
                    )
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected = isSelected, onClick = { onSelect(value) }, role = Role.RadioButton)
                    .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun AutoEndSelector(selected: Int?, onSelect: (Int?) -> Unit) {
    val palette = LocalFocusPalette.current
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AutoEndChip(
            label = stringResource(R.string.modes_auto_off),
            isSelected = selected == null,
            onClick = { onSelect(null) },
            paletteSelected = palette.accent,
        )
        AUTO_PRESETS.forEach { mins ->
            AutoEndChip(
                label = formatAutoEnd(mins),
                isSelected = selected == mins,
                onClick = { onSelect(mins) },
                paletteSelected = palette.accent,
            )
        }
    }
}

@Composable
private fun AutoEndChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    paletteSelected: androidx.compose.ui.graphics.Color,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .heightIn(min = Dimens.presetMinHeight)
            .background(
                if (isSelected) paletteSelected else palette.ringTrack,
                MaterialTheme.shapes.small,
            )
            .border(
                Dimens.cardBorderWidth,
                if (isSelected) paletteSelected else palette.cardBorder,
                MaterialTheme.shapes.small,
            )
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = Spacing.sm, horizontal = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
        )
    }
}

/**
 * Per-mode daily goal picker: hours 0–24 + minutes 0–59, 1-minute granularity.
 * Displays hour-only like "5h" unless minutes are set, then "5h 30m".
 */
@Composable
private fun GoalPicker(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = hours.toString(),
            onValueChange = { input ->
                input.filter { c -> c.isDigit() }.take(2).toIntOrNull()?.let {
                    onHoursChange(it.coerceIn(0, 24))
                } ?: run { if (input.isEmpty()) onHoursChange(0) }
            },
            label = { Text(stringResource(R.string.modes_goal_hours)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = minutes.toString(),
            onValueChange = { input ->
                input.filter { c -> c.isDigit() }.take(2).toIntOrNull()?.let {
                    onMinutesChange(it.coerceIn(0, 59))
                } ?: run { if (input.isEmpty()) onMinutesChange(0) }
            },
            label = { Text(stringResource(R.string.modes_goal_minutes)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(Spacing.xs))
    Text(
        stringResource(
            R.string.modes_goal_preview_x,
            formatGoalShort((hours * 60 + minutes).coerceIn(5, 1440)),
        ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BlockModeSelector(selectedAllowlist: Boolean, onSelect: (Boolean) -> Unit) {
    val palette = LocalFocusPalette.current
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(
            false to stringResource(R.string.modes_blocklist),
            true to stringResource(R.string.modes_allowlist),
        ).forEach { (value, label) ->
            val isSelected = selectedAllowlist == value
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.presetMinHeight)
                    .background(
                        if (isSelected) palette.accent else palette.ringTrack,
                        MaterialTheme.shapes.small,
                    )
                    .border(
                        Dimens.cardBorderWidth,
                        if (isSelected) palette.accent else palette.cardBorder,
                        MaterialTheme.shapes.small,
                    )
                    .clip(MaterialTheme.shapes.small)
                    .selectable(selected = isSelected, onClick = { onSelect(value) }, role = Role.RadioButton)
                    .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                )
            }
        }
    }
}
