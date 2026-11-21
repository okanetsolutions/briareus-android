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
 * [refreshes] the sessions when it opens, whenever the connection changes and on its Refresh button, and says when that
 * fails until a later refresh succeeds or a live update brings newer sessions.
 */
@Composable
fun StatusScreen(store: Store, refreshes: Boolean = false, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val sessions by store.sessions.collectAsState()
    val link by store.link.collectAsState()
    val connection by store.connection.collectAsState()
    val refreshed by store.refreshed.collectAsState()
    val sessionsKnown by store.sessionsKnown.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(refreshes) }
    // A failure belongs to its connection: the refresh count restarts and the empty list looks the same on the next one.
    var failedOn by remember(connection?.address, connection?.device) { mutableStateOf<StatusRefresh.Failure?>(null) }
    val failed = StatusRefresh.stillFailed(failedOn, sessions, refreshed, sessionsKnown)
    suspend fun refresh() {
        refreshing = true
        failedOn = null
        val before = store.refreshed.value
        try { store.refresh() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { failedOn = StatusRefresh.failure(store.sessions.value, before, store.refreshed.value); store.failed(e) }
        finally { refreshing = false }
    }
    if (refreshes) LaunchedEffect(connection?.address) { refresh() }
    val all = sessions.values.toList()
    val counts = SessionList.counts(all)
    val waiting = SessionList.needingYou(all)
    val working = all.filter { it.isActive && it.parentId == null }.sortedBy { it.title }
    val unknown = refreshes && StatusRefresh.nothingLoaded(sessionsKnown)

    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Status", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = p.ink)
                LinkIndicator(link)
                if (refreshes) TextButton(enabled = !refreshing, onClick = { scope.launch { refresh() } }) { Text(if (refreshing) "Refreshing…" else "Refresh") }
            }
        }
        if (failed) {
            item { Text(if (unknown) "Could not load the conversations." else "Could not refresh the conversations. Showing the last loaded status.", color = p.warn) }
        }
        if (!unknown || !(failed || refreshing)) item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Count("Need you", counts.needsYou, p.accent, Modifier.weight(1f))
                Count("Working", counts.working, p.warn, Modifier.weight(1f))
                Count("Ready", counts.ready, p.ok, Modifier.weight(1f))
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
        if (waiting.isEmpty() && working.isEmpty()) {
            when {
                unknown && refreshing -> item { Text("Loading conversations…", color = p.muted) }
                unknown && failed -> Unit
                else -> item { Text("All quiet. Nothing is running and nothing waits for you.", color = p.muted) }
            }
        }
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

/** When the Status tab's refresh warning applies, and when it has nothing to show yet. Pure, so it is tested on the JVM. */
internal object StatusRefresh {
    /** What was on screen when a refresh failed: the sessions, and how many refreshes had succeeded. */
    class Failure(val sessions: Map<String, Session>, val refreshed: Long)

    /**
     * The failure to warn about, or null when another refresh (the event stream's on reconnect, say) succeeded while
     * this one ran, so the screen is current.
     */
    fun failure(sessions: Map<String, Session>, before: Long, now: Long): Failure? = if (now == before) Failure(sessions, before) else null

    /**
     * Whether the warning still describes the screen: no refresh has succeeded since and, once the list is [known], no
     * live event changed the sessions. Before then the sessions on hand are only the few this device upserted (a
     * followed conversation, one it started), so they leave a failed first load unresolved.
     */
    fun stillFailed(failure: Failure?, sessions: Map<String, Session>, refreshed: Long, known: Boolean): Boolean =
        failure != null && failure.refreshed == refreshed && (!known || failure.sessions === sessions)

    /**
     * No list of this connection's sessions yet, neither saved nor refreshed: counts and "all quiet" would be guesses,
     * even with a few upserted sessions on hand. A known list, even an empty one, is not.
     */
    fun nothingLoaded(known: Boolean): Boolean = !known
}
