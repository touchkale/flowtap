package com.flowtap.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentAppsTest {

    @Test
    fun all_hasNoDuplicates() {
        assertEquals(PaymentApps.ALL.size, PaymentApps.ALL.toSet().size)
    }

    @Test
    fun installed_filtersToInstalledInOrder() {
        val installed = setOf(
            "com.phonepe.app",
            "com.google.android.apps.nbu.paisa.user",
            "com.example.other",
        )
        assertEquals(
            listOf(
                "com.google.android.apps.nbu.paisa.user",
                "com.phonepe.app",
            ),
            PaymentApps.installed(installed),
        )
    }

    @Test
    fun installed_emptyWhenNothingMatches() {
        assertTrue(PaymentApps.installed(setOf("com.example.other")).isEmpty())
    }
}
