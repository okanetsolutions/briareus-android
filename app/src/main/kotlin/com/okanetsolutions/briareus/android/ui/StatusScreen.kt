package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.core.Session
import com.okanetsolutions.briareus.core.SessionList
import com.okanetsolutions.briareus.android.Store
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The status panel: a narrow window to keep beside a video. Counts at the top, then every conversation waiting for you
 * with a quick reply (the agent's options as buttons), then what is working right now. As the main window's Status tab it
 * [refreshes] the sessions when it opens, when the connection changes or drops its sessions, and on its Refresh button,
 * and says when that fails until a refresh succeeds.
 */
@Composable
fun StatusScreen(store: Store, refreshes: Boolean = false, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val sessions by store.sessions.collectAsState()
    val link by store.link.collectAsState()
    val connection by store.connection.collectAsState()
    val epoch by store.epoch.collectAsState()
    val refreshed by store.refreshed.collectAsState()
    val sessionsKnown by store.sessionsKnown.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(refreshes) }
    var failure by remember { mutableStateOf<StatusRefresh.Mark?>(null) }
    val failed = StatusRefresh.stillFailed(failure, StatusRefresh.Mark(epoch, refreshed))
    // Only the latest refresh speaks: one the effect cancelled for a newer one must not end the newer one's "Loading".
    var latest by remember { mutableIntStateOf(0) }
    suspend fun refresh() {
        val run = ++latest
        refreshing = true
        failure = null
        fun mark() = StatusRefresh.Mark(store.epoch.value, store.refreshed.value)
        val before = mark()
        try { store.refresh() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { if (run == latest) failure = StatusRefresh.failure(before, mark()); store.failed(e) }
        finally { if (run == latest) refreshing = false }
    }
    if (refreshes) LaunchedEffect(connection?.address, epoch) { refresh() }
    val all = sessions.values.toList()
    val counts = SessionList.counts(all)
    val waiting = SessionList.needingYou(all)
    val working = all.filter { it.isActive && it.parentId == null }.sortedBy { it.title }
    // No list of this connection's sessions yet, neither saved nor refreshed: counts and "all quiet" would be guesses.
    val unknown = refreshes && !sessionsKnown

    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = p.ink)
                LinkIndicator(link)
                if (refreshes) TextButton(enabled = !refreshing, onClick = { scope.launch { refresh() } }) { Text(if (refreshing) "Refreshing…" else "Refresh") }
            }
        }
        if (unknown) {
            item { if (refreshing) Text("Loading conversations…", color = p.muted) else Text("Could not load the conversations.", color = p.warn) }
        } else {
            if (failed) item { Text("Could not refresh the conversations. Showing the last loaded status.", color = p.warn) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Count("Need you", counts.needsYou, p.accent, Modifier.weight(1f))
                    Count("Working", counts.working, p.warn, Modifier.weight(1f))
                    Count("Ready", counts.ready, p.ok, Modifier.weight(1f))
                }
            }
        }
        if (waiting.isNotEmpty()) item { Text("Waiting for you", style = MaterialTheme.typography.labelMedium, color = p.muted) }
        items(waiting, key = { "w:" + it.id }) { WaitingCard(store, it, onOpen) }
        if (working.isNotEmpty()) item { Text("Working", style = MaterialTheme.typography.labelMedium, color = p.muted) }
        items(working, key = { "r:" + it.id }) { s ->
            Column(Modifier.fillMaxWidth().background(p.raise, RoundedCornerShape(10.dp)).clickable { onOpen(s.id) }.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(p.warn)
                    Text(s.title, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    listOfNotNull(store.projectTitle(s.repo), s.lastTool).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!unknown && waiting.isEmpty() && working.isEmpty()) item { Text("All quiet. Nothing is running and nothing waits for you.", color = p.muted) }
    }
}

@Composable
private fun Count(label: String, n: Int, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier.background(p.raise, RoundedCornerShape(10.dp)).padding(12.dp)) {
        Text("$n", style = MaterialTheme.typography.titleLarge, color = if (n > 0) color else p.muted)
        Text(label, style = MaterialTheme.typography.labelSmall, color = p.muted)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WaitingCard(store: Store, s: Session, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val conversation = remember(s.id) { store.conversation(s.id) }
    val events by conversation.events.collectAsState()
    LaunchedEffect(s.id, s.awaitingAnswer) { if (s.awaitingAnswer) conversation.catchUp() }
    val question = remember(events, s.raw) { if (s.awaitingAnswer) conversation.pendingQuestion() else null }
    var reply by remember(s.id) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    fun send(text: String) {
        if (text.isBlank() || sending) return
        sending = true
        scope.launch { if (store.send(s.id, text.trim())) reply = ""; sending = false }
    }
    Column(Modifier.fillMaxWidth().background(p.raise, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.clickable { onOpen(s.id) }, verticalAlignment = Alignment.CenterVertically) {
            Dot(stateColor(s.state))
            Column(Modifier.padding(start = 8.dp)) {
                Text(s.title, style = MaterialTheme.typography.bodyMedium, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${store.projectTitle(s.repo)} · ${s.state.label}", style = MaterialTheme.typography.labelSmall, color = p.accent)
            }
        }
        val text = question?.question ?: question?.text ?: s.lastText
        if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 5, overflow = TextOverflow.Ellipsis)
        if (store.can("message")) {
            question?.options?.takeIf { it.isNotEmpty() }?.let { options ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { OutlinedButton(onClick = { send(it) }, enabled = !sending, modifier = Modifier.heightIn(min = 44.dp)) { Text(it) } }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceField(store, reply, { reply = it }, Modifier.weight(1f), placeholder = "Record a reply", maxHeight = 140.dp, enabled = !sending)
                TextButton(onClick = { send(reply) }, enabled = reply.isNotBlank() && !sending) { Text("Send") }
            }
        }
    }
}

/** When the Status tab's refresh warning applies. Pure, so it is tested on the JVM. */
internal object StatusRefresh {
    /** Where the store's sessions stood: which connection's ([Store.epoch]), after how many successful refreshes. */
    data class Mark(val epoch: Long, val refreshed: Long)

    /**
     * What a refresh that failed leaves to warn about: the mark it started from, or null when another refresh (the
     * event stream's on reconnect, say) succeeded or the sessions were dropped while it ran, so it says nothing of now.
     */
    fun failure(before: Mark, now: Mark): Mark? = before.takeIf { it == now }

    /** The warning stands until a refresh succeeds or the connection's sessions are dropped; upserts do not touch it. */
    fun stillFailed(failure: Mark?, now: Mark): Boolean = failure == now
}
