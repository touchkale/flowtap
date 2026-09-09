package com.flowtap.app.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flowtap.app.FlowtapApp
import com.flowtap.app.R
import com.flowtap.app.di.AppContainer
import com.flowtap.app.nfc.PairTagActivity
import com.flowtap.app.ui.components.AppIcon
import com.flowtap.app.ui.components.EmptyState
import com.flowtap.app.ui.components.PulsingNfcIcon
import com.flowtap.app.ui.components.RecommendedBlockCard
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import com.flowtap.app.ui.util.rememberResumeTick
import com.flowtap.app.util.RecommendedApps
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class InstalledApp(val pkg: String, val label: String)

@Composable
fun OnboardingScreen(
    container: AppContainer,
    onDone: () -> Unit,
) {
    val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(container))
    val step by vm.step.collectAsStateWithLifecycle()
    val palette = LocalFocusPalette.current

    // F10: track tag count so we can confirm + auto-advance after the first pairing.
    val tags by container.focusRepository.tags.collectAsStateWithLifecycle(initialValue = emptyList())
    var prevTagCount by rememberSaveable { mutableIntStateOf(tags.size) }
    var pairedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(tags.size) {
        if (tags.size > prevTagCount) {
            prevTagCount = tags.size
            if (step == 1) {
                pairedBanner = true
                delay(1200)
                if (step == 1) vm.next()
            }
        }
    }

    val sidePadding = rememberAdaptiveSidePadding()
    Scaffold(containerColor = palette.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sidePadding),
        ) {
            Spacer(Modifier.height(Spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 1) {
                    IconButton(onClick = { vm.back() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                } else {
                    Spacer(Modifier.size(Spacing.xl))
                }
                Text(
                    stringResource(R.string.ob_step_x, step),
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.weight(1f))
                StepDots(step)
            }
            Spacer(Modifier.height(Spacing.md))

            when (step) {
                1 -> PairTagStep(
                    vm = vm,
                    tagCount = tags.size,
                    showBanner = pairedBanner,
                    onNext = { vm.next() },
                )
                2 -> PermissionsStep(vm = vm, onNext = { vm.next() })
                3 -> BlockedAppsStep(vm, onDone = onDone)
            }
        }
    }
}

@Composable
private fun StepDots(current: Int) {
    val palette = LocalFocusPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        (1..3).forEach { i ->
            val width by animateDpAsState(
                targetValue = if (i == current) 22.dp else 8.dp,
                animationSpec = tween(300),
                label = "dot",
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .background(
                        if (i <= current) palette.accent else palette.ringTrack,
                        MaterialTheme.shapes.small,
                    ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Step 1 — Pair your first NFC tag
// ---------------------------------------------------------------------------

@Composable
private fun PairTagStep(
    vm: OnboardingViewModel,
    tagCount: Int,
    showBanner: Boolean,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val palette = LocalFocusPalette.current
    val app = context.applicationContext as FlowtapApp

    fun launchPairing(modeId: Long, tagName: String) {
        context.startActivity(
            Intent(context, PairTagActivity::class.java)
                .putExtra(PairTagActivity.EXTRA_MODE_ID, modeId)
                .putExtra(PairTagActivity.EXTRA_TAG_NAME, tagName),
        )
    }

    fun pairSuggestion(name: String, tagName: String) {
        scope.launch {
            val id = vm.ensureMode(name, "work")
            launchPairing(id, tagName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Spacer(Modifier.height(Spacing.lg))
        PulsingNfcIcon(size = 96.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            stringResource(R.string.ob_pair_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.ob_pair_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
        Spacer(Modifier.height(Spacing.lg))

        Text(
            stringResource(R.string.ob_suggestions),
            style = MaterialTheme.typography.labelLarge,
            color = palette.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
        SuggestionChip("Desk", "Deep Work") { pairSuggestion("Deep Work", "Desk") }
        SuggestionChip("Reading", "Reading") { pairSuggestion("Reading", "Reading") }
        SuggestionChip("Workout", "Workout") { pairSuggestion("Workout", "Workout") }

        AnimatedVisibility(
            visible = tagCount > 0 || showBanner,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.card, MaterialTheme.shapes.medium)
                    .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = if (showBanner) {
                        stringResource(R.string.ob_tag_paired)
                    } else {
                        pluralStringResource(R.plurals.plural_tags_paired, tagCount, tagCount)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Button(
            onClick = {
                context.startActivity(Intent(context, PairTagActivity::class.java))
            },
            modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        ) {
            Icon(Icons.Outlined.Nfc, contentDescription = null)
            Spacer(Modifier.size(Spacing.sm))
            Text(
                stringResource(R.string.ob_scan),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        TextButton(onClick = onNext) {
            Text(
                stringResource(R.string.ob_later),
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.listBottom))
    }
}

@Composable
private fun SuggestionChip(label: String, modeName: String, onClick: () -> Unit) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Nfc,
            contentDescription = null,
            tint = palette.accent,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.size(Spacing.md))
        Column {
            Text(label, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                stringResource(R.string.ob_new_mode, modeName),
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Step 2 — Permissions
// ---------------------------------------------------------------------------

private data class PermItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val granted: Boolean,
    val action: () -> Unit,
)

private data class PermChecks(
    val admin: Boolean,
    val nfcHardware: Boolean,
    val notifGranted: Boolean,
    val notifDeniedPermanently: Boolean,
    val blocking: Boolean,
    val dnd: Boolean,
    val battery: Boolean,
    val nfcEnabled: Boolean,
    val nfcTagAllowed: Boolean,
)

@Composable
private fun PermissionsStep(vm: OnboardingViewModel, onNext: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalFocusPalette.current
    // Permission rows refresh via launcher callbacks + resume (no polling).
    var refreshTick by remember { mutableStateOf(0) }
    var notifAsked by rememberSaveable { mutableStateOf(false) }
    val hardcore by vm.hardcoreMode.collectAsStateWithLifecycle(initialValue = false)
    val blockingMethod by vm.blockingMethod.collectAsStateWithLifecycle(
        initialValue = com.flowtap.app.data.prefs.BlockingMethod.FULL_POWER,
    )

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notifAsked = true
        refreshTick++
    }
    val resumeTick = rememberResumeTick()

    // Expensive platform checks are cached; labels resolve in composition
    // (stringResource cannot run inside remember).
    val checks = remember(refreshTick, resumeTick, notifAsked) {
        val notifGranted = Permissions.areNotificationsGranted(context)
        PermChecks(
            admin = Permissions.isDeviceAdminActive(context),
            nfcHardware = Permissions.hasNfcHardware(context),
            notifGranted = notifGranted,
            notifDeniedPermanently = Build.VERSION.SDK_INT >= 33 &&
                !notifGranted && notifAsked && !Permissions.shouldShowNotifRationale(context),
            blocking = Permissions.isAccessibilityEnabled(context),
            dnd = Permissions.isDndGranted(context),
            battery = Permissions.isBatteryOptimizationExempt(context),
            nfcEnabled = Permissions.isNfcEnabled(context),
            nfcTagAllowed = Permissions.isNfcTagIntentAllowed(context),
        )
    }
    val items = listOfNotNull(
        PermItem(
            stringResource(R.string.ob_perm_blocking),
            stringResource(R.string.ob_perm_blocking_sub),
            Icons.Outlined.Block,
            checks.blocking,
            { Permissions.enableAccessibility(context) },
        ),
        PermItem(
            stringResource(R.string.ob_perm_dnd),
            stringResource(R.string.ob_perm_dnd_sub),
            Icons.Outlined.NotificationsOff,
            checks.dnd,
            { Permissions.enableDnd(context) },
        ),
        PermItem(
            stringResource(R.string.ob_perm_notif),
            if (checks.notifDeniedPermanently) {
                stringResource(R.string.ob_perm_notif_denied_sub)
            } else {
                stringResource(R.string.ob_perm_notif_sub)
            },
            Icons.Outlined.NotificationsActive,
            checks.notifGranted,
            {
                if (Build.VERSION.SDK_INT >= 33 && !checks.notifGranted) {
                    if (checks.notifDeniedPermanently) {
                        Permissions.openAppSettings(context)
                    } else {
                        notifAsked = true
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
        ),
        PermItem(
            stringResource(R.string.ob_perm_background),
            stringResource(R.string.ob_perm_background_sub),
            Icons.Outlined.Settings,
            checks.battery,
            { Permissions.enableBatteryOptimizationExemption(context) },
        ),
        if (checks.nfcHardware) {
            PermItem(
                stringResource(R.string.ob_perm_nfc),
                stringResource(R.string.ob_perm_nfc_sub),
                Icons.Outlined.Nfc,
                checks.nfcEnabled,
                { Permissions.enableNfc(context) },
            )
        } else null,
        if (Build.VERSION.SDK_INT >= 36 && !checks.nfcTagAllowed) {
            PermItem(
                stringResource(R.string.ob_perm_nfc_scan),
                stringResource(R.string.ob_perm_nfc_scan_sub),
                Icons.Outlined.Nfc,
                checks.nfcTagAllowed,
                { Permissions.allowNfcTagIntents(context) },
            )
        } else null,
        PermItem(
            stringResource(R.string.ob_perm_uninstall),
            stringResource(R.string.ob_perm_uninstall_sub),
            Icons.Outlined.Lock,
            checks.admin,
            { Permissions.enableDeviceAdmin(context) },
        ),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.ob_grant_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.ob_grant_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(Spacing.lg))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(items, key = { it.title }) { item ->
                PermRow(item)
            }
            item(key = "blocking_method") {
                Text(
                    stringResource(R.string.ob_method_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                )
                Spacer(Modifier.height(Spacing.xs))
                MethodRow(
                    title = stringResource(R.string.ob_method_full),
                    subtitle = stringResource(R.string.ob_method_full_sub),
                    selected = blockingMethod ==
                        com.flowtap.app.data.prefs.BlockingMethod.FULL_POWER,
                    onClick = {
                        vm.setBlockingMethod(
                            com.flowtap.app.data.prefs.BlockingMethod.FULL_POWER,
                        )
                    },
                )
                Spacer(Modifier.height(Spacing.xs))
                MethodRow(
                    title = stringResource(R.string.ob_method_compat),
                    subtitle = stringResource(R.string.ob_method_compat_sub),
                    selected = blockingMethod ==
                        com.flowtap.app.data.prefs.BlockingMethod.COMPATIBILITY,
                    onClick = {
                        vm.setBlockingMethod(
                            com.flowtap.app.data.prefs.BlockingMethod.COMPATIBILITY,
                        )
                    },
                )
            }
            item(key = "hardcore") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.card, MaterialTheme.shapes.medium)
                        .border(
                            Dimens.cardBorderWidth,
                            palette.cardBorder,
                            MaterialTheme.shapes.medium,
                        )
                        .clip(MaterialTheme.shapes.medium)
                        .toggleable(
                            value = hardcore,
                            onValueChange = { vm.setHardcore(it) },
                            role = Role.Switch,
                        )
                        .padding(Spacing.cardPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.ob_hardcore_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                        )
                        Text(
                            stringResource(R.string.ob_hardcore_sub),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textSecondary,
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = hardcore,
                        onCheckedChange = null,
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        ) {
            Text(
                stringResource(R.string.common_continue),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun MethodRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary
                else palette.textPrimary,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                else palette.textSecondary,
            )
        }
    }
}

@Composable
private fun PermRow(item: PermItem) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = item.action, role = Role.Button)
            .padding(Spacing.cardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                item.subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.size(Spacing.sm))
        Icon(
            if (item.granted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (item.granted) {
                stringResource(R.string.ob_perm_granted)
            } else {
                stringResource(R.string.ob_perm_not_granted)
            },
            tint = if (item.granted) palette.accent else palette.textTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Step 3 — Choose blocked apps
// ---------------------------------------------------------------------------

@Composable
private fun BlockedAppsStep(vm: OnboardingViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val palette = LocalFocusPalette.current
    val selected by vm.selectedBlocked.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }

    LaunchedEffect(Unit) {
        apps = runCatching { com.flowtap.app.ui.modes.AppCatalog.load(context) }
            .getOrDefault(emptyList())
            .map { InstalledApp(it.pkg, it.label) }
    }

    val filtered = apps
        .filter { it.label.contains(query.trim(), ignoreCase = true) }
        .sortedBy { it.label.lowercase() }

    // Installed social/distracting apps, in curation order. Shown as a
    // one-tap section above the full directory while not searching.
    val recommended = remember(apps) {
        val byPkg = apps.associateBy { it.pkg }
        RecommendedApps.installed(apps.map { it.pkg }.toSet()).mapNotNull { byPkg[it] }
    }
    val recommendedPkgs = remember(recommended) { recommended.map { it.pkg }.toSet() }
    val unselectedRecommended = recommended.filter { it.pkg !in selected }
    val showRecommended = query.isBlank() && recommended.isNotEmpty()
    val listApps = if (showRecommended) {
        filtered.filter { it.pkg !in recommendedPkgs }
    } else {
        filtered
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.ob_block_title),
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(R.string.ob_block_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.ob_search)) },
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
        Spacer(Modifier.height(Spacing.md))
        if (!showRecommended && filtered.isEmpty()) {
            EmptyState(
                title = if (query.isBlank()) {
                    stringResource(R.string.ob_no_apps)
                } else {
                    stringResource(R.string.ob_no_match, query.trim())
                },
                subtitle = stringResource(R.string.ob_try_search),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                if (showRecommended && unselectedRecommended.isNotEmpty()) {
                    item(key = "recommended") {
                        RecommendedBlockCard(
                            unselectedCount = unselectedRecommended.size,
                            onBlockAll = { vm.setSelectedBlocked(selected + recommendedPkgs) },
                        )
                    }
                }
                if (showRecommended) {
                    items(recommended, key = { "rec-${it.pkg}" }) { app ->
                        AppRow(
                            app = app,
                            checked = app.pkg in selected,
                            onToggle = { vm.toggleBlocked(app.pkg) },
                        )
                    }
                }
                items(listApps, key = { it.pkg }) { app ->
                    AppRow(
                        app = app,
                        checked = app.pkg in selected,
                        onToggle = { vm.toggleBlocked(app.pkg) },
                    )
                }
                item { Spacer(Modifier.height(Spacing.sm)) }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        if (selected.isNotEmpty()) {
            Text(
                pluralStringResource(
                    R.plurals.plural_apps_selected,
                    selected.size,
                    selected.size,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = palette.accentText,
            )
            Spacer(Modifier.height(Spacing.sm))
        }
        Button(
            onClick = { vm.finish(onDone) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(Dimens.primaryButtonHeight),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
        ) {
            Text(
                if (saving) {
                    stringResource(R.string.ob_saving)
                } else {
                    stringResource(R.string.ob_start)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.presetMinHeight)
            .background(palette.card, MaterialTheme.shapes.small)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, onValueChange = { onToggle() }, role = Role.Checkbox)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pkg = app.pkg, label = app.label, size = 36.dp, corner = 10.dp)
        Spacer(Modifier.size(Spacing.md))
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Checkbox(checked = checked, onCheckedChange = null)
    }
}

private fun loadLaunchableApps(context: Context): List<InstalledApp> {
    // Shared catalog: deduped by package (M3). Kept for binary compat; new
    // code should call AppCatalog.load off-main.
    val raw = run {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
            .mapNotNull { ri ->
                val pkg = runCatching { ri.activityInfo.packageName }.getOrNull()
                    ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
                com.flowtap.app.ui.modes.InstalledApp(pkg, label)
            }
    }
    return com.flowtap.app.ui.modes.AppCatalog.deduplicate(raw)
        .map { InstalledApp(it.pkg, it.label) }
}