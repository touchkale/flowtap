package com.flowtap.app.ui.modes

import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    @Test
    fun deduplicate_collapsesMultipleLauncherActivities() {
        val apps = listOf(
            InstalledApp("com.example.app", "Example"),
            InstalledApp("com.example.app", "Example Alias"),
            InstalledApp("com.other.app", "Other"),
        )
        val result = AppCatalog.deduplicate(apps)
        assertEquals(listOf("com.example.app", "com.other.app"), result.map { it.pkg })
        // First label wins.
        assertEquals("Example", result.first { it.pkg == "com.example.app" }.label)
    }

    @Test
    fun deduplicate_sortsDeterministically() {
        val apps = listOf(
            InstalledApp("com.b.app", "Beta"),
            InstalledApp("com.a.app", "alpha"),
            InstalledApp("com.c.app", "Alpha"),
        )
        val result = AppCatalog.deduplicate(apps)
        assertEquals(
            listOf("com.a.app", "com.c.app", "com.b.app"),
            result.map { it.pkg },
        )
    }

    @Test
    fun filter_matchesLabelOrPackage() {
        val apps = listOf(
            InstalledApp("com.instagram.android", "Instagram"),
            InstalledApp("com.google.android.youtube", "YouTube"),
        )
        assertEquals(1, AppCatalog.filter(apps, "insta").size)
        assertEquals(1, AppCatalog.filter(apps, "youtube").size)
        assertEquals(2, AppCatalog.filter(apps, "").size)
        assertEquals(0, AppCatalog.filter(apps, "zzz-no-match").size)
    }
}
