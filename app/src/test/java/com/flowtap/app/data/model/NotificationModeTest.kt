package com.flowtap.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationModeTest {

    @Test
    fun fromKey_defaultsToSilent() {
        assertEquals(NotificationMode.SILENT, NotificationMode.fromKey(null))
        assertEquals(NotificationMode.SILENT, NotificationMode.fromKey("bogus"))
        assertEquals(NotificationMode.DND, NotificationMode.fromKey("dnd"))
        assertEquals(NotificationMode.NONE, NotificationMode.fromKey("none"))
    }

    @Test
    fun legacy_mapsDndFlag() {
        assertEquals(NotificationMode.DND, NotificationMode.legacy(true))
        assertEquals(NotificationMode.SILENT, NotificationMode.legacy(false))
    }
}
