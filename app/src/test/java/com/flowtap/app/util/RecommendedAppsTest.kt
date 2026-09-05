package com.flowtap.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedAppsTest {

    @Test
    fun all_hasNoDuplicates() {
        assertEquals(RecommendedApps.ALL.size, RecommendedApps.ALL.toSet().size)
    }

    @Test
    fun installed_preservesCurationOrderAndFiltersUninstalled() {
        val installed = setOf(
            "com.facebook.katana",
            "com.whatsapp",
            "com.example.notCurated",
        )
        assertEquals(
            listOf("com.whatsapp", "com.facebook.katana"),
            RecommendedApps.installed(installed),
        )
    }

    @Test
    fun installed_emptyWhenNothingMatches() {
        assertTrue(RecommendedApps.installed(setOf("com.example.other")).isEmpty())
    }
}
