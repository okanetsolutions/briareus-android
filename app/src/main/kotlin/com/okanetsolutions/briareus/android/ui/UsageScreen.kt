package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store
import com.okanetsolutions.briareus.core.UsagePage
import com.okanetsolutions.briareus.core.args
import java.util.Locale
import kotlinx.coroutines.CancellationException

@Composable
fun UsageScreen(store: Store, onBack: (() -> Unit)?) {
    var rows by remember { mutableStateOf<List<Pair<String, UsagePage>>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(reload) {
        val client = store.client ?: return@LaunchedEffect
        loading = true
        try {
            check(store.can("usage_all") || store.can("usage")) { "Usage is unavailable for this connection." }
            val answer = if (store.can("usage_all")) {
                listOf("All projects" to UsagePage.parse(client.call("usage_all", args("period" to "month"))))
            } else {
                if (store.projects.value.isEmpty()) store.refresh()
                if (store.client !== client) return@LaunchedEffect
                store.projects.value.map { project ->
                    if (store.client !== client) return@LaunchedEffect
                    project.title to UsagePage.parse(client.call("usage", args("repo" to project.repo)))
                }
            }
            if (store.client !== client) return@LaunchedEffect
            rows = answer
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) error = pageFailure(store, e)
        } finally { if (store.client === client) loading = false }
    }
    ProjectPage("Usage · This month", onBack, { reload++ }) {
        PageStatus(loading, error)
        if (!loading && error == null && rows.isEmpty()) Text("No project usage available.", Modifier.padding(16.dp))
        LazyColumn {
            items(rows) { (title, usage) ->
                Column(Modifier.padding(16.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    val cost = usage.cost?.let { String.format(Locale.US, "$%.2f", it) } ?: "Not priced"
                    Text(cost + if (usage.unpriced > 0 && usage.cost != null) "+" else "", style = MaterialTheme.typography.headlineMedium)
                    Text("${usage.sessions} sessions · ${usage.turns} turns")
                    Text("${usage.input} input tokens · ${usage.output} output tokens")
                    if (usage.unpriced > 0) Text("${usage.unpriced} turns could not be priced.", color = LocalPalette.current.muted)
                    if (usage.turns == 0L) Text("No usage recorded this month.", color = LocalPalette.current.muted)
                }
                HorizontalDivider()
            }
        }
    }
}
