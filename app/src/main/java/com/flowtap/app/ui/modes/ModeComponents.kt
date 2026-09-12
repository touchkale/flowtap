package com.flowtap.app.ui.modes

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Launch
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
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
import com.flowtap.app.ui.theme.PaperCardShape
import com.flowtap.app.ui.theme.PrimaryButtonShape
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.util.RecommendedApps

data class InstalledApp(val pkg: String, val label: String)

fun loadLaunchableApps(context: Context): List<InstalledApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val raw = runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
        .mapNotNull { ri ->
            val pkg = runCatching { ri.activityInfo.packageName }.getOrNull()
                ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
            InstalledApp(pkg, label)
        }
    // Dedupe by package (M3): multiple launcher activities must not produce
    // duplicate Compose keys.
    return AppCatalog.deduplicate(raw)
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

/** Daily-goal presets in minutes; the last chip opens the custom dialog. */
private val GOAL_PRESETS = listOf(60, 120, 240, 360)

/** One summary line for a mode card: apps · ending · sound · goal · tags (+ options). */
@Composable
private fun modeSummaryLine(mode: FocusMode, tagCount: Int, launchLabel: String?): String {
    val apps = if (mode.allowlistOnly) {
        stringResource(R.string.modes_chip_allowlist_x, mode.allowedApps.size)
    } else {
        pluralStringResource(R.plurals.plural_blocked_short, mode.blockedApps.size, mode.blockedApps.size)
    }
    val ending = stringResource(
        if (mode.strictMode) R.string.tags_chip_strict else R.string.tags_chip_flexible,
    )
    val sound = stringResource(
        when (mode.notificationMode) {
            com.flowtap.app.data.model.NotificationMode.DND -> R.string.tags_chip_dnd
            com.flowtap.app.data.model.NotificationMode.SILENT -> R.string.modes_chip_silent
            com.flowtap.app.data.model.NotificationMode.NONE -> R.string.modes_chip_none
        },
    )
    val goal = stringResource(R.string.modes_chip_goal_x, formatGoalShort(mode.dailyGoalMinutes))
    val tags = pluralStringResource(R.plurals.plural_mode_tags, tagCount, tagCount)
    val parts = mutableListOf(apps, ending, sound, goal, tags)
    mode.autoEndMinutes?.let { parts += stringResource(R.string.modes_chip_auto_x, formatAutoEnd(it)) }
    if (mode.keepAwake == true) parts += stringResource(R.string.modes_chip_awake)
    mode.launchPackage?.let { parts += stringResource(R.string.modes_chip_launch_x, launchLabel ?: it) }
    return parts.joinToString(" · ")
}

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
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, PaperCardShape)
            .border(Dimens.cardBorderWidth, palette.cardBorder, PaperCardShape)
            .clip(PaperCardShape)
            .clickable(onClick = onEdit, role = Role.Button)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xxs))
                // One summary line instead of competing chips + icon buttons.
                Text(
                    modeSummaryLine(mode, tagCount, launchLabel),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.modes_more_cd),
                        tint = palette.textSecondary,
                    )
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.tags_edit_mode)) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.modes_duplicate)) },
                        onClick = {
                            menuOpen = false
                            onDuplicate()
                        },
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(stringResource(R.string.common_delete)) },
                        enabled = deleteEnabled,
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
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
    var notificationMode by mutableStateOf(initial.notificationMode)
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
    var goalMinutes by mutableStateOf(initial.dailyGoalMinutes.coerceIn(5, 1440))

    val customValid: Boolean get() {
        val v = customMinutes.trim().toIntOrNull()
        return v != null && v in 5..720
    }

    val goalValid: Boolean get() = goalMinutes in 5..1440
    val goalIsCustom: Boolean get() = goalMinutes !in GOAL_PRESETS

    val saveEnabled: Boolean get() = name.isNotBlank() && (customMinutes.isBlank() || customValid) && goalValid

    private val resolvedAutoEnd: Int? get() = if (customMinutes.isNotBlank()) {
        customMinutes.trim().toIntOrNull()?.takeIf { it in 5..720 } ?: autoEnd
    } else {
        autoEnd
    }

    fun buildUpdated(): FocusMode = initial.copy(
        name = name.trim(),
        notificationMode = notificationMode,
        strictMode = strict,
        keepAwake = keepAwake,
        autoEndMinutes = resolvedAutoEnd,
        allowlistOnly = allowlistOnly,
        blockedApps = blocked.toList(),
        allowedApps = allowed.toList(),
        launchPackage = launchPkg,
        dailyGoalMinutes = goalMinutes.coerceIn(5, 1440),
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
    var appsError by remember { mutableStateOf(false) }

    // M3: shared catalog, off-main, deduplicated by package.
    LaunchedEffect(Unit) {
        apps = runCatching { AppCatalog.load(context) }.getOrElse {
            appsError = true
            emptyList()
        }
    }

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
                var customGoalOpen by remember { mutableStateOf(false) }
                var advancedOpen by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = { state.name = it },
                    label = { Text(stringResource(R.string.tags_mode_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_goal_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                GoalPresetChips(
                    selectedMinutes = state.goalMinutes,
                    isCustom = state.goalIsCustom,
                    onSelect = { state.goalMinutes = it },
                    onCustom = { customGoalOpen = true },
                )
                if (customGoalOpen) {
                    CustomGoalDialog(
                        initialMinutes = state.goalMinutes,
                        onDismiss = { customGoalOpen = false },
                        onSave = {
                            state.goalMinutes = it
                            customGoalOpen = false
                        },
                    )
                }

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_block_mode),
                    style = MaterialTheme.typography.titleMedium,
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
                AdvancedToggle(expanded = advancedOpen, onToggle = { advancedOpen = !advancedOpen })
                if (advancedOpen) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    stringResource(R.string.modes_notif_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                NotificationModeSelector(
                    selected = state.notificationMode,
                    onSelect = { state.notificationMode = it },
                )
                Spacer(Modifier.height(Spacing.sm))
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
                Spacer(Modifier.height(Spacing.xs))
                // M5: explicit ending-rule summary beneath the toggle.
                Text(
                    stringResource(
                        if (state.strict) R.string.modes_end_strict_summary
                        else R.string.modes_end_flex_summary,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_keep_awake),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(Spacing.xs))
                KeepAwakeSelector(selected = state.keepAwake, onSelect = { state.keepAwake = it })

                Spacer(Modifier.height(Spacing.md))
                Text(
                    stringResource(R.string.modes_auto_end),
                    style = MaterialTheme.typography.titleMedium,
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
                    stringResource(R.string.modes_launch_app),
                    style = MaterialTheme.typography.titleMedium,
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
                // M5: warn when launch-on-start is simultaneously blocked.
                val launchConflict = state.launchPkg?.let { pkg ->
                    if (state.allowlistOnly) pkg !in state.allowed else pkg in state.blocked
                } ?: false
                if (launchConflict) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        stringResource(R.string.modes_launch_conflict),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                } // advancedOpen
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
                        if (isSelected) palette.selectedSurface else palette.card,
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
                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
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
                if (isSelected) palette.selectedSurface else palette.card,
                MaterialTheme.shapes.small,
            )
            .border(
                Dimens.cardBorderWidth,
                if (isSelected) palette.accent else palette.cardBorder,
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
            color = if (isSelected) palette.textPrimary else palette.textSecondary,
        )
    }
}

/**
 * Daily-goal presets (1h/2h/4h/6h) plus a Custom chip that opens the dialog.
 * The custom chip shows the picked value when active.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalPresetChips(
    selectedMinutes: Int,
    isCustom: Boolean,
    onSelect: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        GOAL_PRESETS.forEach { mins ->
            AutoEndChip(
                label = formatGoalShort(mins),
                isSelected = selectedMinutes == mins,
                onClick = { onSelect(mins) },
                paletteSelected = palette.accent,
            )
        }
        AutoEndChip(
            label = if (isCustom) {
                stringResource(
                    R.string.modes_goal_custom_x,
                    formatGoalShort(selectedMinutes),
                )
            } else {
                stringResource(R.string.modes_goal_custom)
            },
            isSelected = isCustom,
            onClick = onCustom,
            paletteSelected = palette.accent,
        )
    }
}

/** Custom goal dialog: hours + minutes fields, 5 min – 24 h range. */
@Composable
private fun CustomGoalDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var hours by remember(initialMinutes) {
        mutableStateOf((initialMinutes / 60).toString())
    }
    var minutes by remember(initialMinutes) {
        mutableStateOf((initialMinutes % 60).toString())
    }
    val total = ((hours.toIntOrNull() ?: -1) * 60) + (minutes.toIntOrNull() ?: -1)
    val valid = total in 5..1440 &&
        (hours.toIntOrNull() ?: -1) in 0..24 &&
        (minutes.toIntOrNull() ?: -1) in 0..59

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.modes_goal_custom_title)) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.modes_goal_hours_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = (hours.toIntOrNull() ?: -1) !in 0..24,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(stringResource(R.string.modes_goal_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = (minutes.toIntOrNull() ?: -1) !in 0..59,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSave(total) },
                enabled = valid,
            ) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

/** Collapsed-by-default container for rarely-changed mode settings. */
@Composable
private fun AdvancedToggle(expanded: Boolean, onToggle: () -> Unit) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .selectable(selected = expanded, onClick = onToggle, role = Role.Button)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.modes_advanced),
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = palette.textSecondary,
        )
    }
}

/** None / Silent / DND segmented control for session interruptions. */
@Composable
private fun NotificationModeSelector(
    selected: com.flowtap.app.data.model.NotificationMode,
    onSelect: (com.flowtap.app.data.model.NotificationMode) -> Unit,
) {
    val palette = LocalFocusPalette.current
    val options = listOf(
        Triple(
            com.flowtap.app.data.model.NotificationMode.NONE,
            stringResource(R.string.modes_notif_none),
            stringResource(R.string.modes_notif_none_sub),
        ),
        Triple(
            com.flowtap.app.data.model.NotificationMode.SILENT,
            stringResource(R.string.modes_notif_silent),
            stringResource(R.string.modes_notif_silent_sub),
        ),
        Triple(
            com.flowtap.app.data.model.NotificationMode.DND,
            stringResource(R.string.modes_notif_dnd),
            stringResource(R.string.modes_notif_dnd_sub),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { (value, label, _) ->
                val isSelected = selected == value
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.presetMinHeight)
                        .background(
                            if (isSelected) palette.selectedSurface else palette.card,
                            MaterialTheme.shapes.small,
                        )
                        .border(
                            Dimens.cardBorderWidth,
                            if (isSelected) palette.accent else palette.cardBorder,
                            MaterialTheme.shapes.small,
                        )
                        .clip(MaterialTheme.shapes.small)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else palette.textSecondary,
                    )
                }
            }
        }
        Text(
            options.first { it.first == selected }.third,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BlockModeSelector(selectedAllowlist: Boolean, onSelect: (Boolean) -> Unit) {
    val palette = LocalFocusPalette.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.ringTrack, MaterialTheme.shapes.small)
                .border(
                    Dimens.cardBorderWidth,
                    palette.cardBorder,
                    MaterialTheme.shapes.small,
                )
                .clip(MaterialTheme.shapes.small),
        ) {
            listOf(
                false to stringResource(R.string.modes_blocklist),
                true to stringResource(R.string.modes_allowlist),
            ).forEach { (value, label) ->
                val isSelected = selectedAllowlist == value
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) palette.accent else androidx.compose.ui.graphics.Color.Transparent,
                            MaterialTheme.shapes.small,
                        )
                        .clip(MaterialTheme.shapes.small)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(value) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = Spacing.sm, horizontal = Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else palette.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = if (selectedAllowlist) {
                stringResource(R.string.modes_allowlist_sub)
            } else {
                stringResource(R.string.modes_blocklist_sub)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
