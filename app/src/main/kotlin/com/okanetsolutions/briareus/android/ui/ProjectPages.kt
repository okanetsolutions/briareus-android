package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store
import com.okanetsolutions.briareus.core.ApiError
import com.okanetsolutions.briareus.core.IssuePage
import com.okanetsolutions.briareus.core.ProjectBoardPage
import com.okanetsolutions.briareus.core.RepoFilePage
import com.okanetsolutions.briareus.core.RepoTreePage
import com.okanetsolutions.briareus.core.args
import com.okanetsolutions.briareus.core.obj
import com.okanetsolutions.briareus.core.str
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ProjectPage(title: String, onBack: (() -> Unit)?, retry: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(LocalPalette.current.canvas).padding(12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            if (onBack != null) TextButton(onClick = onBack) { Text("Back") }
            Text(title, Modifier.weight(1f).padding(12.dp), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = retry) { Text("Refresh") }
        }
        HorizontalDivider()
        content()
    }
}

@Composable
internal fun PageStatus(loading: Boolean, error: String?) {
    if (loading) CircularProgressIndicator(Modifier.padding(16.dp))
    if (error != null) Text(error, Modifier.padding(16.dp), color = LocalPalette.current.danger)
}

internal fun pageFailure(store: Store, error: Exception): String {
    if (error is CancellationException) throw error
    store.failed(error, silent = true)
    return (error as? ApiError)?.description ?: error.message ?: "Could not load this page."
}

@Composable
fun IssuesScreen(store: Store, repo: String, onPane: (Pane?) -> Unit, onBack: (() -> Unit)?) {
    var rows by remember(repo) { mutableStateOf<List<IssuePage>>(emptyList()) }
    var error by remember(repo) { mutableStateOf<String?>(null) }
    var loading by remember(repo) { mutableStateOf(true) }
    var reload by remember(repo) { mutableIntStateOf(0) }
    LaunchedEffect(repo, reload) {
        val client = store.client ?: return@LaunchedEffect
        loading = true
        try {
            check(store.can("pulls")) { "Issues are unavailable for this connection." }
            val answer = client.call("pulls", args("repo" to repo))
            if (store.client !== client) return@LaunchedEffect
            rows = IssuePage.list(answer)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) error = pageFailure(store, e)
        } finally { if (store.client === client) loading = false }
    }
    ProjectPage("Issues · ${store.projectTitle(repo)}", onBack, { reload++ }) {
        PageStatus(loading, error)
        if (!loading && error == null && rows.isEmpty()) Text("No open issues.", Modifier.padding(16.dp))
        LazyColumn {
            items(rows, key = { it.number }) { issue ->
                Column(Modifier.fillMaxWidth().clickable(enabled = store.can("issue")) { onPane(Pane.Issue(repo, issue.number)) }.padding(16.dp)) {
                    Text("#${issue.number} ${issue.title}", style = MaterialTheme.typography.titleMedium)
                    Text(listOfNotNull(issue.state, issue.author).joinToString(" · "), color = LocalPalette.current.muted)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun IssueScreen(store: Store, repo: String, number: Int, onPane: (Pane?) -> Unit, onBack: (() -> Unit)?) {
    var issue by remember(repo, number) { mutableStateOf<IssuePage?>(null) }
    var error by remember(repo, number) { mutableStateOf<String?>(null) }
    var loading by remember(repo, number) { mutableStateOf(true) }
    var busy by remember(repo, number) { mutableStateOf(false) }
    var uncertain by rememberSaveable(repo, number) { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repo, number, reload) {
        val client = store.client ?: return@LaunchedEffect
        loading = true
        try {
            check(store.can("issue")) { "Issue details are unavailable for this connection." }
            val answer = client.call("issue", args("repo" to repo, "issue" to number))
            if (store.client !== client) return@LaunchedEffect
            issue = requireNotNull(answer.obj("issue")?.let(IssuePage::parse)) { "The server returned an invalid issue." }
            error = if (uncertain && reload > 0 && !busy) {
                uncertain = false
                "Issue refreshed. Check Projects for an existing session before starting another."
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) error = pageFailure(store, e)
        } finally { if (store.client === client) loading = false }
    }
    fun write(start: Boolean) {
        val operation = if (start) "start_session" else "close_issue"
        if (busy || uncertain || !store.can(operation)) return
        val client = store.client ?: return
        busy = true
        uncertain = true
        scope.launch {
            try {
                val current = requireNotNull(issue)
                if (store.client !== client || !store.can(operation)) return@launch
                if (start) {
                    val answer = client.call("start_session", current.startArguments(repo))
                    if (store.client !== client) return@launch
                    val id = requireNotNull(answer.obj("session")?.str("id")) { "Session started, but no session ID was returned. Check Projects before trying again." }
                    onPane(Pane.Open(id))
                } else {
                    client.call("close_issue", args("repo" to repo, "issue" to number, "reason" to "completed"))
                    if (store.client !== client) return@launch
                    issue = current.copy(state = "closed")
                }
                error = null
                uncertain = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (store.client === client) {
                    error = pageFailure(store, e)
                    uncertain = (e as? ApiError)?.status !in 400..499
                }
            } finally { if (store.client === client) busy = false }
        }
    }
    ProjectPage("Issue #$number", onBack, { reload++ }) {
        PageStatus(loading, error)
        if (uncertain) Text("The write may have succeeded. Check Projects for an existing session, then Refresh here to enable actions again.", Modifier.padding(12.dp))
        issue?.let { current ->
            Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) {
                Text(current.title, style = MaterialTheme.typography.headlineSmall)
                Text(current.state, color = LocalPalette.current.muted)
                SelectionContainer { MarkdownText(current.body.ifBlank { "No description." }) }
                if (current.state == "open") {
                    if (store.can("start_session")) TextButton(onClick = { write(true) }, enabled = !busy && !uncertain) { Text("Start session on this issue") }
                    if (store.can("close_issue")) TextButton(onClick = { confirmClose = true }, enabled = !busy && !uncertain) { Text("Close as completed") }
                }
            }
        }
    }
    if (confirmClose) AlertDialog(
        onDismissRequest = { confirmClose = false }, title = { Text("Close issue #$number?") },
        text = { Text("Mark this issue as completed on GitHub.") },
        confirmButton = { TextButton(onClick = { confirmClose = false; write(false) }) { Text("Close issue") } },
        dismissButton = { TextButton(onClick = { confirmClose = false }) { Text("Cancel") } },
    )
}

@Composable
fun ProjectBoardScreen(store: Store, repo: String, onPane: (Pane?) -> Unit, onBack: (() -> Unit)?) {
    var board by remember(repo) { mutableStateOf<ProjectBoardPage?>(null) }
    var error by remember(repo) { mutableStateOf<String?>(null) }
    var loading by remember(repo) { mutableStateOf(true) }
    var reload by remember(repo) { mutableIntStateOf(0) }
    LaunchedEffect(repo, reload) {
        val client = store.client ?: return@LaunchedEffect
        loading = true
        try {
            check(store.can("project_board")) { "The project board is unavailable for this connection." }
            val answer = client.call("project_board", args("repo" to repo))
            if (store.client !== client) return@LaunchedEffect
            board = ProjectBoardPage.parse(answer)
            error = board?.error
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) error = pageFailure(store, e)
        } finally { if (store.client === client) loading = false }
    }
    ProjectPage(board?.title ?: "Project board", onBack, { reload++ }) {
        PageStatus(loading, error)
        if (board?.truncated == true) Text("Only the board's first 2,000 items are shown.", Modifier.padding(12.dp))
        if (!loading && error == null && board?.columns.orEmpty().all { it.cards.isEmpty() }) Text("No items match this board's view.", Modifier.padding(16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(board?.columns.orEmpty()) { column ->
                Column(Modifier.width(300.dp).padding(top = 12.dp)) {
                    Text("${column.name} · ${column.cards.size}", style = MaterialTheme.typography.titleMedium)
                    LazyColumn {
                        items(column.cards) { card ->
                            val canOpen = card.repo == repo && card.number > 0 &&
                                ((card.type == "issue" && store.can("issue")) || (card.type == "pull" && store.can("pull")))
                            Column(Modifier.fillMaxWidth().clickable(enabled = canOpen) {
                                onPane(if (card.type == "pull") Pane.Pull(repo, card.number) else Pane.Issue(repo, card.number))
                            }.padding(16.dp)) {
                                Text(card.title)
                                if (card.number > 0) Text("${card.repo.orEmpty()} #${card.number}", color = LocalPalette.current.muted)
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilesScreen(store: Store, repo: String, path: String, onBack: (() -> Unit)?) {
    var tree by remember(repo) { mutableStateOf<RepoTreePage?>(null) }
    var selected by remember(repo, path) { mutableStateOf(path) }
    var file by remember(repo, selected) { mutableStateOf<RepoFilePage?>(null) }
    var error by remember(repo, selected) { mutableStateOf<String?>(null) }
    var loading by remember(repo) { mutableStateOf(true) }
    var reload by remember(repo) { mutableIntStateOf(0) }
    LaunchedEffect(repo, selected, reload) {
        val client = store.client ?: return@LaunchedEffect
        loading = true
        try {
            check(store.can("repo_tree")) { "Repository files are unavailable for this connection." }
            if (tree == null) {
                val answer = client.call("repo_tree", args("repo" to repo))
                if (store.client !== client) return@LaunchedEffect
                tree = RepoTreePage.parse(answer)
            }
            val current = requireNotNull(tree)
            val entry = current.entries.firstOrNull { it.path == selected }
            file = if (entry != null && !entry.folder) {
                check(store.can("repo_file")) { "File content is unavailable for this connection." }
                val answer = client.call("repo_file", args("repo" to repo, "path" to selected, "ref" to current.ref))
                if (store.client !== client) return@LaunchedEffect
                RepoFilePage.parse(answer)
            } else null
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) error = pageFailure(store, e)
        } finally { if (store.client === client) loading = false }
    }
    ProjectPage("Files · ${store.projectTitle(repo)}", onBack, { tree = null; reload++ }) {
        PageStatus(loading, error)
        Row {
            if (selected.isNotEmpty()) TextButton(onClick = { selected = selected.substringBeforeLast('/', "") }) { Text("Up") }
            Text(selected.ifEmpty { "/" }, Modifier.padding(12.dp))
        }
        if (tree?.truncated == true) Text("The repository tree is incomplete.", Modifier.padding(12.dp))
        val opened = file
        if (opened != null) {
            val unavailable = opened.unavailable
            if (unavailable != null) Text(unavailable, Modifier.padding(12.dp))
            else SelectionContainer {
                Text(opened.content.orEmpty(), Modifier.verticalScroll(rememberScrollState()).padding(12.dp), fontFamily = FontFamily.Monospace)
            }
        } else {
            val children = tree?.children(selected).orEmpty()
            if (!loading && error == null && children.isEmpty()) Text("This folder is empty.", Modifier.padding(12.dp))
            LazyColumn {
                items(children, key = { it.path }) { entry ->
                    Text(
                        if (entry.folder) "${entry.name}/" else entry.name,
                        Modifier.fillMaxWidth().clickable(enabled = entry.folder || store.can("repo_file")) { selected = entry.path }.padding(16.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
