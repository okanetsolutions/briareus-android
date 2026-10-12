package com.okanetsolutions.briareus.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store

@Composable
fun SettingsScreen(store: Store, alerts: Boolean, onBackgroundAlerts: (Boolean) -> Unit) {
    val p = LocalPalette.current
    val connection by store.connection.collectAsState()
    var confirmForget by remember { mutableStateOf<Boolean?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge, color = p.ink)
        Column {
            connection?.let { c ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(c.device.label.ifBlank { "This device" }, style = MaterialTheme.typography.titleSmall, color = p.ink)
                    Text("${c.address.host} · ${c.device.permission}", style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
                HorizontalDivider(color = p.line)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Background notifications", Modifier.weight(1f), color = p.ink)
                Switch(alerts, onCheckedChange = onBackgroundAlerts)
            }
            TextButton(onClick = { confirmForget = false }) { Text("Forget this connection") }
            if (store.can("revoke_token")) TextButton(onClick = { confirmForget = true }) { Text("Revoke the token and forget", color = p.danger) }
        }
    }
    confirmForget?.let { revoke ->
        AlertDialog(
            onDismissRequest = { confirmForget = null },
            title = { Text(if (revoke) "Revoke this device's token?" else "Forget this connection?") },
            text = {
                Text(
                    if (revoke) "The token stops working on the server and everything saved here is erased. Running agents keep running."
                    else "The token and saved conversations are erased from this device. The token itself keeps working until it is revoked on the server, and running agents keep running.",
                )
            },
            confirmButton = { TextButton(onClick = { confirmForget = null; store.forget(revoke) }) { Text(if (revoke) "Revoke" else "Forget", color = p.danger) } },
            dismissButton = { TextButton(onClick = { confirmForget = null }) { Text("Cancel") } },
        )
    }
}
