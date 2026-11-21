package com.okanetsolutions.briareus.android

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTabTest {
    @Test fun restoresKnownTabs() {
        for (tab in TABS) assertEquals(tab, restoredTab(tab))
    }

    @Test fun theFindingsTabBecameStatus() {
        assertEquals("Status", restoredTab("Findings"))
    }

    @Test fun anUnknownOrMissingTabIsProjects() {
        assertEquals("Projects", restoredTab("Inbox"))
        assertEquals("Projects", restoredTab(null))
    }
}
