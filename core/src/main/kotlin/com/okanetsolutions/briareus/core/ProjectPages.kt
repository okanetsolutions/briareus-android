package com.okanetsolutions.briareus.core

import kotlinx.serialization.json.JsonObject

data class IssuePage(val number: Int, val title: String, val body: String, val state: String, val author: String?) {
    companion object {
        fun parse(value: JsonObject): IssuePage? {
            val number = value.int("number")?.takeIf { it > 0 } ?: return null
            val title = value.str("title") ?: return null
            return IssuePage(number, title, value.str("body").orEmpty(), value.str("state") ?: "open", value.str("author"))
        }
        fun list(value: JsonObject): List<IssuePage> = value.objects("issues").mapNotNull(::parse)
    }

    fun startArguments(repo: String): JsonObject = args(
        "repo" to repo, "activity" to "issue",
        "prompt" to "Work on issue #$number in $repo: $title\n\n$body",
    )
}

data class RepoPath(val path: String, val folder: Boolean) {
    val name: String get() = path.substringAfterLast('/')
    val parent: String get() = path.substringBeforeLast('/', "")
}

data class RepoTreePage(val ref: String, val truncated: Boolean, val entries: List<RepoPath>) {
    fun children(path: String): List<RepoPath> = entries.filter { it.parent == path }
        .sortedWith(compareByDescending<RepoPath> { it.folder }.thenBy { it.name.lowercase() })

    companion object {
        fun parse(value: JsonObject): RepoTreePage {
            require(value.array("entries") != null) { "The server returned an invalid repository tree." }
            val paths = value.objects("entries").mapNotNull { entry ->
                entry.nonEmpty("path")?.let { RepoPath(it, entry.str("type") == "tree") }
            }.associateByTo(linkedMapOf()) { it.path }
            for (entry in paths.values.toList()) {
                var parent = entry.parent
                while (parent.isNotEmpty()) {
                    paths.putIfAbsent(parent, RepoPath(parent, true))
                    parent = parent.substringBeforeLast('/', "")
                }
            }
            return RepoTreePage(value.str("sha") ?: value.str("ref").orEmpty(), value.bool("truncated") == true, paths.values.toList())
        }
    }
}

data class RepoFilePage(val path: String, val content: String?, val unavailable: String?) {
    companion object {
        fun parse(value: JsonObject): RepoFilePage {
            val path = requireNotNull(value.str("path")) { "The server returned an invalid file." }
            val unavailable = when {
                value.bool("binary") == true -> "This is a binary file."
                value.bool("tooLarge") == true -> "This file is too large to display."
                value.str("content") == null -> "The server did not return file content."
                else -> null
            }
            return RepoFilePage(path, value.str("content"), unavailable)
        }
    }
}

data class BoardCardPage(val title: String, val repo: String?, val number: Int, val type: String)
data class BoardColumnPage(val name: String, val cards: List<BoardCardPage>)
data class ProjectBoardPage(val title: String, val columns: List<BoardColumnPage>, val truncated: Boolean, val error: String?) {
    companion object {
        fun parse(value: JsonObject): ProjectBoardPage = ProjectBoardPage(
            value.obj("project")?.str("title") ?: "Project board",
            value.objects("columns").map { column ->
                BoardColumnPage(column.str("name") ?: "No status", column.objects("items").map { card ->
                    BoardCardPage(card.str("title") ?: "Private item", card.str("repo"), card.int("number") ?: 0, card.str("type") ?: "issue")
                })
            }, value.bool("truncated") == true, value.nonEmpty("projectsError"),
        )
    }
}

data class UsagePage(val cost: Double?, val sessions: Long, val turns: Long, val input: Long, val output: Long, val unpriced: Long) {
    companion object {
        fun parse(value: JsonObject): UsagePage = UsagePage(
            value.double("costUsd"), value.long("sessions") ?: 0, value.long("turns") ?: 0,
            value.long("inputTokens") ?: 0, value.long("outputTokens") ?: 0, value.long("unpricedTurns") ?: 0,
        )
    }
}
