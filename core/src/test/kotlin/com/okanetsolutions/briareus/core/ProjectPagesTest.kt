package com.okanetsolutions.briareus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPagesTest {
    @Test fun issuesComeFromPullsAndStartWithAnIssueActivity() {
        val issues = IssuePage.list(json("""{"issues":[{"number":42,"title":"Fix login","body":"Reject old tokens","author":"nadin"},{"title":"invalid"}],"pulls":[]}"""))
        assertEquals(listOf(42), issues.map { it.number })
        assertEquals("open", issues.single().state)
        assertEquals(
            json("""{"repo":"o/r","activity":"issue","prompt":"Work on issue #42 in o/r: Fix login\n\nReject old tokens"}"""),
            issues.single().startArguments("o/r"),
        )
    }

    @Test fun treeSynthesizesMissingParentsAndPinsContentToSha() {
        val tree = RepoTreePage.parse(json("""{"ref":"main","sha":"abc123","truncated":true,"entries":[{"path":"z.txt","type":"blob"},{"path":"src/lib/a.kt","type":"blob"}]}"""))
        assertEquals("abc123", tree.ref)
        assertTrue(tree.truncated)
        assertEquals(listOf("src", "z.txt"), tree.children("").map { it.path })
        assertEquals(listOf("src/lib"), tree.children("src").map { it.path })
        assertTrue(tree.children("").first().folder)
        assertFalse(tree.children("").last().folder)
    }

    @Test fun emptyTextIsDifferentFromBinaryOrMissingContent() {
        assertEquals("", RepoFilePage.parse(json("""{"path":"empty.txt","content":""}""")).content)
        assertNull(RepoFilePage.parse(json("""{"path":"empty.txt","content":""}""")).unavailable)
        assertEquals("This is a binary file.", RepoFilePage.parse(json("""{"path":"logo.png","binary":true}""")).unavailable)
        assertEquals("This file is too large to display.", RepoFilePage.parse(json("""{"path":"big.txt","tooLarge":true}""")).unavailable)
    }

    @Test fun boardReadsItemsAndServerProjectError() {
        val board = ProjectBoardPage.parse(json("""{"project":{"title":"Roadmap"},"columns":[
            {"name":"Todo","items":[{"type":"issue","repo":"o/r","number":42,"title":"Fix login"}]}],"truncated":true}"""))
        assertEquals("Roadmap", board.title)
        assertEquals(42, board.columns.single().cards.single().number)
        assertTrue(board.truncated)
        assertEquals("Projects access denied", ProjectBoardPage.parse(json("""{"projectsError":"Projects access denied"}""")).error)
    }

    @Test fun usagePreservesUnknownPriceAndUnpricedTurns() {
        val usage = UsagePage.parse(json("""{"sessions":3,"turns":7,"inputTokens":1200,"outputTokens":450,"costUsd":null,"unpricedTurns":7}"""))
        assertNull(usage.cost)
        assertEquals(3L, usage.sessions)
        assertEquals(1200L, usage.input)
        assertEquals(7L, usage.unpriced)
    }

    @Test fun addedRoutesMatchTheClientApi() {
        assertEquals(ApiRoute("usage_all", "GET", "usage/all"), Routes["usage_all"])
        assertEquals(ApiRoute("repo_tree", "GET", "repo/tree"), Routes["repo_tree"])
        assertEquals(ApiRoute("repo_file", "GET", "repo/file"), Routes["repo_file"])
        assertEquals(ApiRoute("project_board", "GET", "project-board"), Routes["project_board"])
        assertEquals(ApiRoute("close_issue", "POST", "issues/{issue}/close"), Routes["close_issue"])
    }
}
