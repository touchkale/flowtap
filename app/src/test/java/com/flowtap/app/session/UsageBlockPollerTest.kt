package com.flowtap.app.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageBlockPollerTest {

    @Test
    fun currentPackage_returnsMostRecentInWindow() {
        val now = 1_000_000L
        val events = listOf(
            UsageBlockPoller.ForegroundEvent("com.a", now - 4000),
            UsageBlockPoller.ForegroundEvent("com.b", now - 1000),
            UsageBlockPoller.ForegroundEvent("com.c", now - 3000),
        )
        assertEquals("com.b", UsageBlockPoller.currentPackage(events, now))
    }

    @Test
    fun currentPackage_nullWhenStaleOrEmpty() {
        val now = 1_000_000L
        assertNull(UsageBlockPoller.currentPackage(emptyList(), now))
        val stale = listOf(UsageBlockPoller.ForegroundEvent("com.a", now - 6000))
        assertNull(UsageBlockPoller.currentPackage(stale, now))
    }

    @Test
    fun currentPackage_ignoresFutureEvents() {
        val now = 1_000_000L
        val events = listOf(
            UsageBlockPoller.ForegroundEvent("com.future", now + 5000),
            UsageBlockPoller.ForegroundEvent("com.past", now - 1000),
        )
        assertEquals("com.past", UsageBlockPoller.currentPackage(events, now))
    }

    @Test
    fun isBlocked_blocklist() {
        assertTrue(
            UsageBlockPoller.isBlocked(
                pkg = "com.instagram.android",
                blockedApps = listOf("com.instagram.android"),
                allowlistOnly = false,
                allowedApps = emptyList(),
            ),
        )
        assertFalse(
            UsageBlockPoller.isBlocked(
                pkg = "com.example.other",
                blockedApps = listOf("com.instagram.android"),
                allowlistOnly = false,
                allowedApps = emptyList(),
            ),
        )
    }

    @Test
    fun isBlocked_allowlist() {
        assertFalse(
            UsageBlockPoller.isBlocked(
                pkg = "com.allowed",
                blockedApps = emptyList(),
                allowlistOnly = true,
                allowedApps = listOf("com.allowed"),
            ),
        )
        assertTrue(
            UsageBlockPoller.isBlocked(
                pkg = "com.other",
                blockedApps = emptyList(),
                allowlistOnly = true,
                allowedApps = listOf("com.allowed"),
            ),
        )
    }
}
