package com.flowtap.app.nfc

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.flowtap.app.FlowtapApp
import com.flowtap.app.R
import com.flowtap.app.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Invisible router that receives NFC tag taps even when the app is closed
 * (manifest intent filter on our custom MIME type). It resolves the tag to a
 * mode and hands off to SessionManager, then finishes without showing UI.
 */
class NfcRouterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent) {
        val app = application as FlowtapApp
        val uid = NdefWriter.parseUid(intent)
        val modeIdFromPayload = NdefWriter.parseModeId(intent)

        app.appScope.launch {
            val repo = app.container.focusRepository
            val tag = uid?.let { repo.getTagByUid(it) }
            val mode = when {
                tag != null -> repo.getMode(tag.modeId)
                modeIdFromPayload != null -> repo.getMode(modeIdFromPayload)
                else -> null
            }

            if (mode == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@NfcRouterActivity,
                        getString(R.string.nfc_unknown_tag),
                        Toast.LENGTH_SHORT,
                    ).show()
                    finish()
                }
                return@launch
            }

            SessionManager.handleTagTap(mode, tag)
            withContext(Dispatchers.Main) { finish() }
        }
    }
}
