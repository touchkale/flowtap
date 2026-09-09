package com.flowtap.app.session

import com.flowtap.app.data.prefs.BlockingMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingReadinessTest {

    @Test
    fun fullPower_readyOnlyWithAccessAndConnection() {
        val ready = BlockingReadiness(
            method = BlockingMethod.FULL_POWER,
            accessibilityEnabled = true,
            accessibilityServiceConnected = true,
            usageAccessGranted = false,
            overlayGranted = false,
            sessionActive = false,
            selectedAppCount = 6,
        )
        assertTrue(ready.isReady)
        assertNull(ready.missingRequirement())
        assertEquals("Ready", ready.statusLabel())

        val noPerm = ready.copy(accessibilityEnabled = false)
        assertFalse(noPerm.isReady)
        assertEquals("accessibility", noPerm.missingRequirement())

        val noConn = ready.copy(accessibilityServiceConnected = false)
        assertFalse(noConn.isReady)
        assertEquals("service-connection", noConn.missingRequirement())
    }

    @Test
    fun compat_requiresUsageAndOverlay() {
        val ready = BlockingReadiness(
            method = BlockingMethod.COMPATIBILITY,
            accessibilityEnabled = false,
            accessibilityServiceConnected = false,
            usageAccessGranted = true,
            overlayGranted = true,
            sessionActive = false,
            selectedAppCount = 3,
        )
        assertTrue(ready.isReady)
        assertFalse(ready.hasEngineConflict)

        val noUsage = ready.copy(usageAccessGranted = false)
        assertFalse(noUsage.isReady)
        assertEquals("usage-access", noUsage.missingRequirement())

        val noOverlay = ready.copy(usageAccessGranted = true, overlayGranted = false)
        assertFalse(noOverlay.isReady)
        assertEquals("overlay", noOverlay.missingRequirement())
    }

    @Test
    fun compatWithAccessibility_conflicts() {
        val conflict = BlockingReadiness(
            method = BlockingMethod.COMPATIBILITY,
            accessibilityEnabled = true,
            accessibilityServiceConnected = true,
            usageAccessGranted = true,
            overlayGranted = true,
            sessionActive = false,
            selectedAppCount = 3,
        )
        assertTrue(conflict.isReady)
        assertTrue(conflict.hasEngineConflict)
    }

    @Test
    fun status_neverClaimsOnForPermissionAlone() {
        val idleNotReady = BlockingReadiness(
            method = BlockingMethod.FULL_POWER,
            accessibilityEnabled = false,
            accessibilityServiceConnected = false,
            usageAccessGranted = false,
            overlayGranted = false,
            sessionActive = false,
            selectedAppCount = 6,
        )
        assertEquals("Action needed", idleNotReady.statusLabel())

        val activeNotReady = idleNotReady.copy(sessionActive = true)
        assertEquals("Timer only — blocking unavailable", activeNotReady.statusLabel())

        val activeReady = idleNotReady.copy(
            accessibilityEnabled = true,
            accessibilityServiceConnected = true,
            sessionActive = true,
        )
        assertEquals("Blocking active", activeReady.statusLabel())
    }
}
