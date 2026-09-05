package com.flowtap.app.nfc

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.flowtap.app.R

/**
 * Writes and reads the Flowtap NDEF format.
 *
 * Tag format (two records):
 *  1. MIME record  application/vnd-nfcfocus-mode  payload = ASCII modeId
 *  2. Android Application Record for com.nfcfocus.app (guarantees launch)
 *
 * Identity is the tag UID (stored at pairing); the MIME payload is a fallback.
 */
object NdefWriter {

    const val MIME_TYPE = "application/vnd-nfcfocus-mode"
    const val AAR_PACKAGE = "com.nfcfocus.app"

    fun buildMessage(modeId: Long): NdefMessage {
        val mime = NdefRecord.createMime(
            MIME_TYPE,
            modeId.toString().toByteArray(Charsets.US_ASCII),
        )
        val aar = NdefRecord.createApplicationRecord(AAR_PACKAGE)
        return NdefMessage(arrayOf(mime, aar))
    }

    /** Reads the UID from a dispatched NFC intent as uppercase hex, or null. */
    fun parseUid(intent: Intent): String? {
        val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        val id = tag?.id ?: return null
        return id.joinToString("") { byte -> "%02X".format(byte) }
    }

    /** Reads the modeId stored in our MIME record, or null if absent/unrecognized. */
    fun parseModeId(intent: Intent): Long? {
        val msgs: Array<NdefMessage>? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                ?.filterIsInstance<NdefMessage>()?.toTypedArray()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES) as? Array<NdefMessage>
        }
        val first = msgs?.firstOrNull()?.records?.firstOrNull() ?: return null
        if (first.tnf != NdefRecord.TNF_MIME_MEDIA) return null
        val type = first.toMimeType()
        if (type != MIME_TYPE) return null
        return String(first.payload, Charsets.US_ASCII).toLongOrNull()
    }

    /**
     * Writes our NDEF message onto the tag discovered in [intent].
     * Returns (success, message string resource).
     */
    fun writeModeId(intent: Intent, modeId: Long): Pair<Boolean, Int> {
        val tag: Tag? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return false to R.string.nfc_no_tag_detected
        val message = buildMessage(modeId)
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return false to R.string.nfc_read_only
                }
                ndef.writeNdefMessage(message)
                ndef.close()
                true to R.string.nfc_write_ok
            } else {
                val formatable = NdefFormatable.get(tag)
                if (formatable == null) return false to R.string.nfc_unsupported
                formatable.connect()
                formatable.format(message)
                formatable.close()
                true to R.string.nfc_write_ok
            }
        } catch (e: Exception) {
            false to R.string.nfc_write_failed_generic
        }
    }

    /** Tech classes we support for pairing (foreground dispatch). */
    fun supportedTechClasses(): Array<String> = arrayOf(
        Ndef::class.java.name,
        NdefFormatable::class.java.name,
        NfcA::class.java.name,
        NfcB::class.java.name,
        NfcF::class.java.name,
        NfcV::class.java.name,
    )
}
