package com.okanetsolutions.briareus.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import com.okanetsolutions.briareus.android.Navigator
import com.okanetsolutions.briareus.android.Store

/** One task on a phone; the same selection keeps its list beside it on a wide tablet. */
@Composable
fun AndroidShell(
    store: Store, navigator: Navigator, voiceOn: Boolean, notificationsEnabled: Boolean, onVoice: () -> Unit, onBackgroundAlerts: (Boolean) -> Unit,
) {
    val screenState = rememberSaveableStateHolder()
    val pane by navigator.pane.collectAsState()
    val tab by navigator.tab.collectAsState()
    BackHandler(pane != null || tab != "Projects") {
        if (tab != "Projects") navigator.tab.value = "Projects" else navigator.back()
    }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                "Findings" -> FindingsScreen(store) { navigator.open(Pane.Open(it)) }
                "Usage" -> UsageScreen(store, onBack = null)
                "Settings" -> SettingsScreen(store, notificationsEnabled, onBackgroundAlerts)
                else -> HomeScreen(
                    store, pane, navigator::open, { navigator.open(Pane.Open(it)) },
                    { navigator.tab.value = "Findings" }, voiceOn, onVoice, navigator::back, screenState,
                )
            }
        }
        NavigationBar {
            listOf("Projects" to Icons.Outlined.Folder, "Findings" to Icons.Outlined.Flag, "Usage" to Icons.Outlined.BarChart, "Settings" to Icons.Outlined.Settings)
                .forEach { (label, icon) ->
                    NavigationBarItem(selected = tab == label, onClick = { navigator.tab.value = label }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
        }
    }
}
