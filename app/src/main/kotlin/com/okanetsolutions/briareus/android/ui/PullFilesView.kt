package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store
import com.okanetsolutions.briareus.core.ApiError
import com.okanetsolutions.briareus.core.PullFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Lazy file expansion keeps large patches out of the view until the user opens them. */
@Composable
fun PullFilesView(store: Store, repo: String, number: Int, revision: Int) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    var files by remember(repo, number, revision) { mutableStateOf(PullFiles()) }
    var busy by remember(repo, number, revision) { mutableStateOf(false) }
    var error by remember(repo, number, revision) { mutableStateOf<String?>(null) }
    suspend fun load() {
        if (busy || files.nextPage == null) return
        val client = store.client ?: return
        busy = true
        try {
            val page = client.call("pull_files", files.arguments(repo, number))
            if (client === store.client) { files = files.append(page); error = null }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            store.failed(e, silent = true)
            error = (e as? ApiError)?.description ?: "The changed files could not be read."
        } finally {
            busy = false
        }
    }
    LaunchedEffect(repo, number, revision) { load() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Files changed", style = MaterialTheme.typography.titleSmall, color = p.ink)
        files.files.forEach { file ->
            var expanded by remember(repo, number, revision, file.filename) { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth().background(p.raise)) {
                Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp)) {
                    Text(if (expanded) "− " else "+ ", color = p.muted)
                    Text(file.filename, Modifier.weight(1f), color = p.ink, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Mono))
                    Text(file.status, color = p.muted, style = MaterialTheme.typography.labelSmall)
                }
                if (expanded) {
                    file.previous?.let { Text("Renamed from $it", Modifier.padding(12.dp), color = p.muted) }
                    val patch = file.patch
                    if (patch == null) {
                        Text("No text diff is available for this file.", Modifier.padding(12.dp), color = p.muted)
                    } else SelectionContainer {
                        Column(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
                            patch.lineSequence().forEach { line ->
                                val color = when {
                                    line.startsWith("+") -> p.ok
                                    line.startsWith("-") -> p.danger
                                    line.startsWith("@@") -> p.accent
                                    else -> p.ink
                                }
                                Text(line, color = color, softWrap = false, style = MaterialTheme.typography.bodySmall.copy(fontFamily = Mono))
                            }
                        }
                    }
                    file.url?.takeIf { it.startsWith("https://") }?.let { url ->
                        TextButton(onClick = { runCatching { uri.openUri(url) }.onFailure { store.say("No browser can open this file.") } }) { Text("Open on GitHub") }
                    }
                }
            }
        }
        if (busy) CircularProgressIndicator()
        error?.let { Text(it, color = p.danger) }
        if (!busy && files.nextPage != null) TextButton(onClick = { scope.launch { load() } }) { Text(if (error != null) "Retry" else "Load more files") }
        if (!busy && error == null && files.files.isEmpty()) Text("No changed files.", color = p.muted)
        if (files.truncated) Text("GitHub omitted some changes; open the pull request on GitHub for the full list.", color = p.muted)
    }
}
