package com.flowtap.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Info
import com.flowtap.app.data.prefs.ThemeMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.flowtap.app.ui.util.rememberResumeTick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.BuildConfig
import com.flowtap.app.R
import com.flowtap.app.di.AppContainer
import com.flowtap.app.session.SessionManager
import com.flowtap.app.ui.components.SectionHeader
import com.flowtap.app.ui.onboarding.Permissions
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding

private val DAILY_PRESETS_MIN = listOf(180, 240, 300, 360)
private val WEEKLY_PRESETS_MIN = listOf(1200, 1500, 1800, 2100)

@Composable
fun SettingsScreen(container: AppContainer) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val palette = LocalFocusPalette.current

    val dailyGoal by vm.dailyGoalMinutes.collectAsStateWithLifecycle()
    val weeklyGoal by vm.weeklyGoalMinutes.collectAsStateWithLifecycle()
    val hardcore by vm.hardcoreMode.collectAsStateWithLifecycle()
    val keepAwake by vm.keepScreenAwake.collectAsStateWithLifecycle()
    val dndMaster by vm.dndMasterEnabled.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()

    val sessionActive by SessionManager.active.collectAsStateWithLifecycle()
    val locked = sessionActive != null && hardcore

    var showUninstallConfirm by remember { mutableStateOf(false) }

    // Permission/hardware rows refresh whenever returning from system screens.
    val refreshTick = rememberResumeTick()
    val adminActive = remember(refreshTick) { Permissions.isDeviceAdminActive(context) }
    val blockingActive = remember(refreshTick) { Permissions.isAccessibilityEnabled(context) }
    val dndGranted = remember(refreshTick) { Permissions.isDndGranted(context) }
    val notifGranted = remember(refreshTick) { Permissions.areNotificationsGranted(context) }
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
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(Spacing.sectionGap))

            if (locked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.card, MaterialTheme.shapes.medium)
                        .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
                        .padding(Spacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        stringResource(R.string.settings_locked_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        stringResource(R.string.settings_locked_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                Spacer(Modifier.height(Spacing.cardGap))
            }

            SectionHeader(title = stringResource(R.string.settings_goals))
            Spacer(Modifier.height(Spacing.headerGap))
            GoalCard(
                title = stringResource(R.string.settings_daily_goal),
                current = dailyGoal,
                presets = DAILY_PRESETS_MIN,
                enabled = !locked,
                onSelect = { vm.setDailyGoal(it) },
            )
            Spacer(Modifier.height(Spacing.rowGap))
            GoalCard(
                title = stringResource(R.string.settings_weekly_goal),
                current = weeklyGoal,
                presets = WEEKLY_PRESETS_MIN,
                enabled = !locked,
                onSelect = { vm.setWeeklyGoal(it) },
            )

            Spacer(Modifier.height(Spacing.sectionGap))
            SectionHeader(title = stringResource(R.string.settings_protection))
            Spacer(Modifier.height(Spacing.headerGap))
            SettingRow(
                title = stringResource(R.string.settings_uninstall_prot),
                subtitle = if (adminActive) {
                    stringResource(R.string.settings_uninstall_prot_on)
                } else {
                    stringResource(R.string.settings_uninstall_prot_off)
                },
                icon = if (adminActive) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                trailing = if (adminActive) {
                    stringResource(R.string.common_on)
                } else {
                    stringResource(R.string.common_off)
                },
                enabled = true,
                active = adminActive,
                onClick = {
                    if (!adminActive) Permissions.enableDeviceAdmin(context)
                },
            )
            Spacer(Modifier.height(Spacing.rowGap))
            SettingRow(
                title = stringResource(R.string.settings_app_blocking),
                subtitle = if (blockingActive) {
                    stringResource(R.string.settings_app_blocking_on)
                } else {
                    stringResource(R.string.settings_app_blocking_off)
                },
                icon = Icons.Outlined.Block,
                trailing = if (blockingActive) {
                    stringResource(R.string.common_on)
                } else {
                    stringResource(R.string.common_off)
                },
                enabled = true,
                active = blockingActive,
                onClick = {
                    if (!blockingActive) Permissions.enableAccessibility(context)
                },
            )
            Spacer(Modifier.height(Spacing.rowGap))
            SettingRow(
                title = stringResource(R.string.settings_dnd),
                subtitle = if (dndGranted) {
                    stringResource(R.string.settings_dnd_on)
                } else {
                    stringResource(R.string.settings_dnd_off)
                },
                icon = Icons.Outlined.NotificationsOff,
                trailing = if (dndGranted) {
                    stringResource(R.string.common_on)
                } else {
                    stringResource(R.string.common_off)
                },
                enabled = true,
                active = dndGranted,
                onClick = {
                    if (!dndGranted) Permissions.enableDnd(context)
                },
            )
            if (!notifGranted) {
                Spacer(Modifier.height(Spacing.rowGap))
                SettingRow(
                    title = stringResource(R.string.settings_notif),
                    subtitle = stringResource(R.string.settings_notif_off),
                    icon = Icons.Outlined.NotificationsActive,
                    trailing = stringResource(R.string.common_off),
                    enabled = true,
                    active = false,
                    onClick = { Permissions.openAppSettings(context) },
                )
            }

            Spacer(Modifier.height(Spacing.sectionGap))
            SectionHeader(title = stringResource(R.string.settings_behavior))
            Spacer(Modifier.height(Spacing.headerGap))
            SwitchRow(
                title = stringResource(R.string.settings_dnd_master),
                subtitle = if (dndMaster) {
                    stringResource(R.string.settings_dnd_master_on)
                } else {
                    stringResource(R.string.settings_dnd_master_off)
                },
                checked = dndMaster,
                enabled = !locked,
                onCheckedChange = {
                    if (it && !dndGranted) Permissions.enableDnd(context)
                    vm.setDndMaster(it)
                },
            )
            Spacer(Modifier.height(Spacing.rowGap))
            SwitchRow(
                title = stringResource(R.string.settings_hardcore),
                subtitle = stringResource(R.string.settings_hardcore_sub),
                checked = hardcore,
                enabled = !locked,
                onCheckedChange = { vm.setHardcore(it) },
            )
            Spacer(Modifier.height(Spacing.rowGap))
            SwitchRow(
                title = stringResource(R.string.settings_awake),
                subtitle = stringResource(R.string.settings_awake_sub),
                checked = keepAwake,
                enabled = !locked,
                onCheckedChange = { vm.setKeepScreenAwake(it) },
            )

            Spacer(Modifier.height(Spacing.sectionGap))
            SectionHeader(title = stringResource(R.string.settings_appearance))
            Spacer(Modifier.height(Spacing.headerGap))
            ThemeCard(
                current = themeMode,
                onSelect = { vm.setThemeMode(it) },
            )

            Spacer(Modifier.height(Spacing.sectionGap))
            SectionHeader(title = stringResource(R.string.settings_about))
            Spacer(Modifier.height(Spacing.headerGap))
            SettingRow(
                title = stringResource(R.string.settings_version_x, BuildConfig.VERSION_NAME),
                subtitle = stringResource(R.string.settings_version_sub),
                icon = Icons.Outlined.Info,
                trailing = null,
                enabled = false,
                active = false,
                onClick = {},
            )
            Spacer(Modifier.height(Spacing.rowGap))
            SettingRow(
                title = stringResource(R.string.settings_uninstall),
                subtitle = if (adminActive) {
                    stringResource(R.string.settings_uninstall_on)
                } else {
                    stringResource(R.string.settings_uninstall_off)
                },
                icon = Icons.Outlined.Delete,
                trailing = null,
                enabled = true,
                active = false,
                onClick = { showUninstallConfirm = true },
            )
            Spacer(Modifier.height(Spacing.listBottom))
        }
    }

    if (showUninstallConfirm) {
        UninstallConfirmDialog(
            adminActive = adminActive,
            sessionActive = sessionActive != null,
            onDismiss = { showUninstallConfirm = false },
            onConfirm = {
                showUninstallConfirm = false
                Permissions.deactivateDeviceAdmin(context)
                Permissions.requestUninstall(context)
            },
        )
    }
}

@Composable
private fun UninstallConfirmDialog(
    adminActive: Boolean,
    sessionActive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val adminNote = if (adminActive) stringResource(R.string.settings_uninstall_admin_note) else ""
    val sessionNote = if (sessionActive) {
        stringResource(R.string.settings_uninstall_session_note)
    } else {
        ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_uninstall_title)) },
        text = {
            Text(
                adminNote + sessionNote +
                    stringResource(R.string.settings_uninstall_confirm_note),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.settings_uninstall_action),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun ThemeCard(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val palette = LocalFocusPalette.current
    val options = listOf(
        Triple(
            ThemeMode.SYSTEM,
            stringResource(R.string.settings_theme_auto),
            Icons.Outlined.BrightnessAuto,
        ),
        Triple(
            ThemeMode.LIGHT,
            stringResource(R.string.settings_theme_light),
            Icons.Outlined.LightMode,
        ),
        Triple(
            ThemeMode.DARK,
            stringResource(R.string.settings_theme_dark),
            Icons.Outlined.DarkMode,
        ),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .padding(Spacing.cardPadding),
    ) {
        Text(
            stringResource(R.string.settings_theme_title),
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(R.string.settings_theme_sub),
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            options.forEach { (mode, label, icon) ->
                val selected = current == mode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.presetMinHeight)
                        .background(
                            if (selected) palette.accent else palette.ringTrack,
                            MaterialTheme.shapes.small,
                        )
                        .border(
                            Dimens.cardBorderWidth,
                            if (selected) palette.accent else palette.cardBorder,
                            MaterialTheme.shapes.small,
                        )
                        .clip(MaterialTheme.shapes.small)
                        .selectable(selected = selected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                        .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    title: String,
    current: Int,
    presets: List<Int>,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalFocusPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .padding(Spacing.cardPadding),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            presets.forEach { minutes ->
                val selected = current == minutes
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.presetMinHeight)
                        .background(
                            if (selected) palette.accent else palette.ringTrack,
                            MaterialTheme.shapes.small,
                        )
                        .border(
                            Dimens.cardBorderWidth,
                            if (selected) palette.accent else palette.cardBorder,
                            MaterialTheme.shapes.small,
                        )
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = enabled, role = Role.RadioButton) { onSelect(minutes) }
                        .padding(vertical = Spacing.md, horizontal = Spacing.sm),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected) {
                        androidx.compose.material3.Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(Spacing.xs))
                    }
                    Text(
                        text = "${minutes / 60}h",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else palette.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailing: String?,
    enabled: Boolean,
    active: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) palette.accent else palette.textTertiary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }
        if (trailing != null) {
            Spacer(Modifier.size(Spacing.sm))
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = if (active) palette.accentText else palette.textTertiary,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .toggleable(
                value = checked,
                enabled = enabled,
                onValueChange = onCheckedChange,
                role = Role.Switch,
            )
            .alpha(if (enabled) 1f else 0.6f)
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(subtitle, style = MaterialTheme.typography.labelLarge, color = palette.textSecondary)
        }
        Spacer(Modifier.size(Spacing.md))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = palette.accent,
                checkedBorderColor = palette.accent,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.ringTrack,
                uncheckedBorderColor = palette.cardBorder,
            ),
        )
    }
}