package com.okanetsolutions.briareus.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusRefreshTest {
    private fun mark(epoch: Long, refreshed: Long) = StatusRefresh.Mark(epoch, refreshed)

    @Test fun aFailureWarnsUntilARefreshSucceeds() {
        val failure = StatusRefresh.failure(mark(1, 3), mark(1, 3))
        assertEquals(mark(1, 3), failure)
        // Nothing but a refresh or a dropped list moves the mark, so upserts leave the warning up.
        assertTrue(StatusRefresh.stillFailed(failure, mark(1, 3)))
        // A later successful refresh clears it, even one that brought back the same sessions.
        assertFalse(StatusRefresh.stillFailed(failure, mark(1, 4)))
        assertFalse(StatusRefresh.stillFailed(null, mark(1, 3)))
    }

    @Test fun aFailedFirstLoadWarnsUntilOneSucceeds() {
        val failure = StatusRefresh.failure(mark(1, 0), mark(1, 0))
        assertTrue(StatusRefresh.stillFailed(failure, mark(1, 0)))
        assertFalse(StatusRefresh.stillFailed(failure, mark(1, 1)))
    }

    @Test fun droppedSessionsRetireTheWarning() {
        // A new or changed connection resets the count, so only the epoch tells the two apart.
        val failure = StatusRefresh.failure(mark(1, 0), mark(1, 0))
        assertFalse(StatusRefresh.stillFailed(failure, mark(2, 0)))
    }

    @Test fun noWarningWhenSomethingChangedWhileItRan() {
        // Another refresh succeeded meanwhile.
        assertNull(StatusRefresh.failure(mark(1, 3), mark(1, 4)))
        // The sessions were dropped for a new or changed connection meanwhile.
        assertNull(StatusRefresh.failure(mark(1, 3), mark(2, 0)))
    }
}
