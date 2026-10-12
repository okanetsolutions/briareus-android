package com.okanetsolutions.briareus.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** A page of changed files pinned to the pull request's head and base revisions. */
data class PullFiles(
    val files: List<PullFile> = emptyList(),
    val nextPage: Int? = 1,
    val headSha: String? = null,
    val baseSha: String? = null,
    val truncated: Boolean = false,
) {
    fun arguments(repo: String, number: Int): JsonObject = args(
        "repo" to repo, "pr" to number, "page" to nextPage,
        "headSha" to headSha, "baseSha" to baseSha,
    )

    fun append(response: JsonObject): PullFiles {
        val rows = response["files"] as? JsonArray ?: throw ApiError(ApiError.Kind.NON_JSON)
        val revision = response.obj("pr")
        val head = revision?.str("headSha")
        val base = revision?.str("baseSha")
        val headChanged = headSha != null && head != null && head != headSha
        val baseChanged = baseSha != null && base != null && base != baseSha
        if (headChanged || baseChanged) {
            throw ApiError(ApiError.Kind.HTTP, 409, "The pull request changed. Refresh before loading more files.")
        }
        return copy(
            files = (files + rows.mapNotNull { (it as? JsonObject)?.let(PullFile::parse) }).distinctBy { it.filename },
            nextPage = response.int("nextPage")?.takeIf { it > (nextPage ?: 0) },
            headSha = headSha ?: head,
            baseSha = baseSha ?: base,
            truncated = response.bool("truncated") == true,
        )
    }
}

data class PullFile(val filename: String, val previous: String?, val status: String, val patch: String?, val url: String?) {
    companion object {
        fun parse(value: JsonObject): PullFile? = value.str("filename")?.let {
            PullFile(it, value.str("previousFilename"), value.str("status").orEmpty(), value.str("patch"), value.str("url"))
        }
    }
}
