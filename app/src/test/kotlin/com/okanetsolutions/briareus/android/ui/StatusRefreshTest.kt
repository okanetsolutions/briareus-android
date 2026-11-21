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
        assertTrue(StatusRefresh.stillFailed(failure, loaded, refreshed = 3))
        // A later refresh that brings back equal sessions still clears it.
        assertFalse(StatusRefresh.stillFailed(failure, loaded, refreshed = 4))
        // So does a live event that replaces the sessions.
        assertFalse(StatusRefresh.stillFailed(failure, loaded.toMap(), refreshed = 3))
        assertFalse(StatusRefresh.stillFailed(null, loaded, refreshed = 3))
    }

    @Test fun noWarningWhenAnotherRefreshSucceededMeanwhile() {
        assertNull(StatusRefresh.failure(loaded, before = 3, now = 4))
    }

    @Test fun anEmptyListIsLoadedOnceItIsKnown() {
        // A new or changed connection: nothing saved or refreshed yet.
        assertTrue(StatusRefresh.nothingLoaded(emptyMap(), known = false))
        // A saved or refreshed empty list means there are no conversations.
        assertFalse(StatusRefresh.nothingLoaded(emptyMap(), known = true))
        assertFalse(StatusRefresh.nothingLoaded(loaded, known = false))
    }

    @Test fun aFirstRefreshThatFailsWarnsUntilOneSucceeds() {
        val failure = StatusRefresh.failure(emptyMap(), before = 0, now = 0)
        assertTrue(StatusRefresh.stillFailed(failure, emptyMap(), refreshed = 0))
        assertFalse(StatusRefresh.stillFailed(failure, emptyMap(), refreshed = 1))
    }
}
