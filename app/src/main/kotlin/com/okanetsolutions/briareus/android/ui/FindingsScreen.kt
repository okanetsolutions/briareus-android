package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store
import com.okanetsolutions.briareus.core.Triage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FindingsScreen(store: Store, onOpen: (String) -> Unit) {
    val p = LocalPalette.current
    val sessions by store.sessions.collectAsState()
    val connection by store.connection.collectAsState()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun refresh() {
        refreshing = true
        error = null
        try { store.refresh() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = "Could not refresh findings. Showing the last loaded reviews."; store.failed(e) }
        finally { refreshing = false }
    }
    LaunchedEffect(connection?.address) { refresh() }
    val held = remember(sessions) {
        sessions.values.filter { !it.isActive && !it.isClosed }.mapNotNull { session -> Triage.of(session)?.let { session to it } }
            .sortedWith(compareBy({ it.first.repo }, { it.second.prNumber ?: 0 }, { it.first.title }))
    }
    LazyColumn(Modifier.fillMaxSize().background(p.canvas), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row {
                Text("Findings", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = p.ink)
                TextButton(enabled = !refreshing, onClick = { scope.launch { refresh() } }) { Text(if (refreshing) "Refreshing…" else "Refresh") }
            }
        }
        error?.let { message -> item { Text(message, color = p.warn) } }
        if (held.isEmpty()) item { Text(if (refreshing) "Loading held reviews…" else "No findings are waiting for your decision.", color = p.muted) }
        items(held, key = { it.first.id }) { (session, triage) ->
            TextButton(onClick = { onOpen(session.id) }) { Text("${store.projectTitle(session.repo)} · ${session.title}") }
            TriageCard(store, session, triage)
        }
    }
}
