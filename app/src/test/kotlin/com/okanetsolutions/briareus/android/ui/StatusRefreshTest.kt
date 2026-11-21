package com.okanetsolutions.briareus.android.ui

import com.okanetsolutions.briareus.core.BriareusJson
import com.okanetsolutions.briareus.core.Session
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRefreshTest {
    private val loaded = mapOf("s" to Session(BriareusJson.parseToJsonElement("""{"id":"s","status":"idle"}""") as JsonObject))

    @Test fun aFailureWarnsUntilARefreshSucceedsOrTheSessionsChange() {
        val failure = StatusRefresh.failure(loaded, before = 3, now = 3)
        assertTrue(StatusRefresh.stillFailed(failure, loaded, refreshed = 3, known = true))
        // A later refresh that brings back equal sessions still clears it.
        assertFalse(StatusRefresh.stillFailed(failure, loaded, refreshed = 4, known = true))
        // So does a live event that replaces the sessions.
        assertFalse(StatusRefresh.stillFailed(failure, loaded.toMap(), refreshed = 3, known = true))
        assertFalse(StatusRefresh.stillFailed(null, loaded, refreshed = 3, known = true))
    }

    @Test fun noWarningWhenAnotherRefreshSucceededMeanwhile() {
        assertNull(StatusRefresh.failure(loaded, before = 3, now = 4))
    }

    @Test fun anEmptyListIsLoadedOnceItIsKnown() {
        // A new or changed connection: nothing saved or refreshed yet.
        assertTrue(StatusRefresh.nothingLoaded(known = false))
        // A saved or refreshed list, even an empty one, is the whole list.
        assertFalse(StatusRefresh.nothingLoaded(known = true))
    }

    @Test fun aFirstRefreshThatFailsWarnsUntilOneSucceeds() {
        val failure = StatusRefresh.failure(emptyMap(), before = 0, now = 0)
        assertTrue(StatusRefresh.stillFailed(failure, emptyMap(), refreshed = 0, known = false))
        assertFalse(StatusRefresh.stillFailed(failure, emptyMap(), refreshed = 1, known = true))
    }

    @Test fun anUpsertAfterAFailedFirstLoadLeavesItUnresolved() {
        val failure = StatusRefresh.failure(emptyMap(), before = 0, now = 0)
        // A followed or newly started conversation lands in the map before any list was read.
        assertTrue(StatusRefresh.stillFailed(failure, loaded, refreshed = 0, known = false))
        assertTrue(StatusRefresh.nothingLoaded(known = false))
        // The first successful refresh resolves it.
        assertFalse(StatusRefresh.stillFailed(failure, loaded, refreshed = 1, known = true))
    }
}
