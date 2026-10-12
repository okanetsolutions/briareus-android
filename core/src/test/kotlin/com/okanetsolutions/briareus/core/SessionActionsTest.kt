package com.okanetsolutions.briareus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionActionsTest {
    @Test fun clearOnlyAppliesToIdleDeveloperChats() {
        assertTrue(SessionActions.canClear(Session(json("""{"id":"s","kind":"devchat","status":"idle"}"""))))
        for (raw in listOf(
            """{"id":"s","kind":"devchat","status":"running"}""",
            """{"id":"s","kind":"devchat","status":"idle","compacting":true}""",
            """{"id":"s","kind":"review","status":"idle"}""",
        )) assertFalse(SessionActions.canClear(Session(json(raw))))
    }

    @Test fun compactRequiresServerSupportAndNoCurrentCompaction() {
        assertTrue(SessionActions.canCompact(Session(json("""{"id":"s","canCompact":true}"""))))
        assertFalse(SessionActions.canCompact(Session(json("""{"id":"s"}"""))))
        assertFalse(SessionActions.canCompact(Session(json("""{"id":"s","canCompact":true,"compacting":true}"""))))
    }
}
