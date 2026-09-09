package com.flowtap.app.nfc

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flowtap.app.FlowtapApp
import com.flowtap.app.R
import com.flowtap.app.data.model.FocusMode
import com.flowtap.app.data.model.FocusTag
import com.flowtap.app.ui.theme.Dimens
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.ui.theme.Motion
import com.flowtap.app.ui.theme.FlowtapTheme
import com.flowtap.app.ui.theme.Spacing
import com.flowtap.app.ui.util.rememberAdaptiveSidePadding
import kotlinx.coroutines.launch

/**
 * Pairs a physical NFC tag to a mode: select a mode (or pass EXTRA_MODE_ID),
 * tap the sticker, the app writes our NDEF format, reads the UID and saves the
 * tag. Foreground dispatch handles the tap so nothing else intercepts it.
 */
class PairTagActivity : ComponentActivity() {

    private var adapter: NfcAdapter? = null
    private var busy = false
    private var currentModeId: Long? = null
    private var currentTagName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adapter = NfcAdapter.getDefaultAdapter(this)
        currentModeId = intent.getLongExtra(EXTRA_MODE_ID, -1L).takeIf { it >= 0 }
        currentTagName = intent.getStringExtra(EXTRA_TAG_NAME)

        setContent {
            val app = application as FlowtapApp
            val themeMode by app.container.settingsDataStore.themeMode
                .collectAsStateWithLifecycle(
                    initialValue = com.flowtap.app.data.prefs.ThemeMode.SYSTEM,
                )
            val darkTheme = com.flowtap.app.ui.theme.rememberDarkTheme(themeMode)
            com.flowtap.app.ui.util.SystemBarAppearance(darkTheme)
            FlowtapTheme(
                active = false,
                darkTheme = darkTheme,
            ) {
                PairTagScreen(
                    initialModeId = currentModeId ?: -1L,
                    initialTagName = currentTagName ?: "",
                    nfcAdapter = adapter,
                    onBack = { finish() },
                    onModeSelected = { currentModeId = it },
                    onTagNameUpdated = { currentTagName = it },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val nfc = adapter ?: return
        if (!nfc.isEnabled) return
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType(NdefWriter.MIME_TYPE)
            },
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        )
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
        )
        runCatching { nfc.enableForegroundDispatch(this, pendingIntent, filters, techLists) }
    }

    override fun onPause() {
        super.onPause()
        runCatching { adapter?.disableForegroundDispatch(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (busy) return
        busy = true

        val modeId = currentModeId
        if (modeId == null) {
            busy = false
            Toast.makeText(this, getString(R.string.nfc_select_mode_first), Toast.LENGTH_SHORT).show()
            return
        }

        val app = application as FlowtapApp
        val name = currentTagName
        app.appScope.launch {
            try {
                // N2: check UID before the destructive physical write, so an
                // already-paired tag keeps its payload. Then await DB save
                // before reporting success — success must mean persisted.
                val uid = NdefWriter.parseUid(intent)
                if (uid.isNullOrEmpty()) {
                    runOnUiThread {
                        Toast.makeText(
                            this@PairTagActivity,
                            getString(R.string.nfc_no_tag),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    return@launch
                }
                val repo = app.container.focusRepository
                val existing = repo.getTagByUid(uid)
                if (existing != null) {
                    runOnUiThread {
                        Toast.makeText(
                            this@PairTagActivity,
                            getString(R.string.nfc_already_paired, existing.name),
                            Toast.LENGTH_SHORT,
                        ).show()
                        finish()
                    }
                    return@launch
                }
                val (ok, msgRes) = NdefWriter.writeModeId(intent, modeId)
                if (!ok) {
                    runOnUiThread {
                        Toast.makeText(this@PairTagActivity, getString(msgRes), Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                try {
                    repo.addTag(
                        FocusTag(
                            name = name?.takeIf { it.isNotBlank() }
                                ?: getString(R.string.nfc_new_tag),
                            uidHex = uid,
                            modeId = modeId,
                        )
                    )
                } catch (e: Exception) {
                    // Physical write succeeded but DB did not: surface recovery
                    // guidance instead of a false success.
                    runOnUiThread {
                        Toast.makeText(
                            this@PairTagActivity,
                            getString(R.string.nfc_pair_failed, e.message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    return@launch
                }
                runOnUiThread {
                    Toast.makeText(
                        this@PairTagActivity,
                        getString(R.string.nfc_paired),
                        Toast.LENGTH_SHORT,
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@PairTagActivity,
                        getString(R.string.nfc_pair_failed, e.message),
                        Toast.LENGTH_LONG,
                    ).show()
                }
            } finally {
                // F9: always release the busy flag, even on unexpected failures.
                runOnUiThread { busy = false }
            }
        }
    }

    companion object {
        const val EXTRA_MODE_ID = "extra_mode_id"
        const val EXTRA_TAG_NAME = "extra_tag_name"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairTagScreen(
    initialModeId: Long,
    initialTagName: String,
    nfcAdapter: NfcAdapter?,
    onBack: () -> Unit,
    onModeSelected: (Long) -> Unit,
    onTagNameUpdated: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as FlowtapApp
    val scope = rememberCoroutineScope()
    val palette = LocalFocusPalette.current

    val modes by app.container.focusRepository.modes.collectAsStateWithLifecycle(initialValue = emptyList())

    var modeId by remember { mutableLongStateOf(initialModeId) }
    var tagName by remember { mutableStateOf(initialTagName) }
    var creatingNew by remember { mutableStateOf(false) }
    var newModeName by remember { mutableStateOf("") }

    val readyToScan = modeId >= 0

    LaunchedEffect(modeId) { if (modeId >= 0) onModeSelected(modeId) }
    LaunchedEffect(tagName) { onTagNameUpdated(tagName) }

    val sidePadding = rememberAdaptiveSidePadding()
    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (readyToScan) {
                            stringResource(R.string.nfc_tap_tag)
                        } else {
                            stringResource(R.string.nfc_choose_mode)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sidePadding),
        ) {
            Spacer(Modifier.height(Spacing.md))
            Spacer(Modifier.height(Spacing.lg))

            if (!readyToScan) {
                if (modes.isEmpty() && !creatingNew) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        Spacer(Modifier.height(Spacing.xl))
                        Text(
                            stringResource(R.string.nfc_no_modes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(modes, key = { it.id }) { mode ->
                        ModeRow(
                            mode = mode,
                            selected = modeId == mode.id,
                            onClick = { modeId = mode.id },
                        )
                    }
                    item {
                        ModeRow(
                            mode = null,
                            selected = creatingNew,
                            onClick = { creatingNew = !creatingNew },
                        )
                    }
                    if (creatingNew) {
                        item {
                            OutlinedTextField(
                                value = newModeName,
                                onValueChange = { newModeName = it },
                                label = { Text(stringResource(R.string.nfc_new_mode_name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    val name = newModeName.trim()
                                    if (name.isEmpty()) return@Button
                                    scope.launch {
                                        val id = app.container.focusRepository.addMode(
                                            FocusMode(name = name, iconName = "work", accentIndex = 0)
                                        )
                                        modeId = id
                                        creatingNew = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.primaryButtonHeight),
                                shape = MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    stringResource(R.string.nfc_create_continue),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            } else {
                ScanningBody(
                    modeName = modes.firstOrNull { it.id == modeId }?.name
                        ?: stringResource(R.string.nfc_selected_mode),
                    tagName = tagName,
                    onTagNameChange = { tagName = it },
                    nfcSupported = nfcAdapter != null,
                    nfcEnabled = nfcAdapter?.isEnabled == true,
                    onEnableNfc = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                    },
                )
            }
        }
    }
}

@Composable
private fun ModeRow(
    mode: FocusMode?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.card, MaterialTheme.shapes.medium)
            .border(Dimens.cardBorderWidth, palette.cardBorder, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(Spacing.sm))
        Text(
            text = mode?.name ?: stringResource(R.string.nfc_create_new),
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
        )
    }
}

@Composable
private fun ScanningBody(
    modeName: String,
    tagName: String,
    onTagNameChange: (String) -> Unit,
    nfcSupported: Boolean,
    nfcEnabled: Boolean,
    onEnableNfc: () -> Unit,
) {
    val palette = LocalFocusPalette.current
    val transition = rememberInfiniteTransition(label = "nfc")
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.PulseMs / 2, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nfcAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Box(
            modifier = Modifier
                .size(140.dp)
                .alpha(pulse)
                .scale(pulse),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(palette.accent.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Nfc,
                    contentDescription = stringResource(R.string.common_nfc_cd),
                    modifier = Modifier.size(64.dp),
                    tint = palette.accent,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.nfc_tap_phone),
            style = MaterialTheme.typography.titleLarge,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.nfc_bound_to, modeName),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        OutlinedTextField(
            value = tagName,
            onValueChange = onTagNameChange,
            label = { Text(stringResource(R.string.nfc_tag_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!nfcSupported) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.nfc_no_hardware),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
        } else if (!nfcEnabled) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                stringResource(R.string.nfc_off_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = onEnableNfc,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.primaryButtonHeight),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    stringResource(R.string.nfc_turn_on),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}
