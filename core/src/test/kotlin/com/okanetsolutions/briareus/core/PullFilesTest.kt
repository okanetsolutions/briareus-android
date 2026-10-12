package com.okanetsolutions.briareus.core

import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PullFilesTest {
    private fun page(json: String) = BriareusJson.parseToJsonElement(json) as JsonObject

    @Test fun pinsLaterPagesAndDeduplicatesPaths() {
        val first = PullFiles().append(page("""{"pr":{"headSha":"h","baseSha":"b"},"files":[{"filename":"a.kt","patch":"+a"}],"nextPage":2}"""))
        assertEquals(args("repo" to "o/r", "pr" to 4, "page" to 2, "headSha" to "h", "baseSha" to "b"), first.arguments("o/r", 4))
        val last = first.append(page("""{"files":[{"filename":"a.kt"},{"filename":"b.png"}],"nextPage":null}"""))
        assertEquals(listOf("a.kt", "b.png"), last.files.map { it.filename })
        assertEquals("+a", last.files.first().patch)
        assertNull(last.files.last().patch)
        assertNull(last.nextPage)
    }

    @Test(expected = ApiError::class) fun refusesMixedRevisions() {
        PullFiles(headSha = "old").append(page("""{"pr":{"headSha":"new"},"files":[]}"""))
    }

    @Test(expected = ApiError::class) fun malformedResponseIsNotAnEmptyList() {
        PullFiles().append(page("""{"error":"unavailable"}"""))
    }
}
