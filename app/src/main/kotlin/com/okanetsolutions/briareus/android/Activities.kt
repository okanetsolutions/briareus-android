package com.okanetsolutions.briareus.android

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.okanetsolutions.briareus.android.ui.AndroidShell
import com.okanetsolutions.briareus.android.ui.Pane
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.okanetsolutions.briareus.android.ui.BriareusTheme
import com.okanetsolutions.briareus.android.ui.ConversationScreen
import com.okanetsolutions.briareus.android.ui.PairingScreen
import com.okanetsolutions.briareus.android.ui.PreviewScreen
import com.okanetsolutions.briareus.android.ui.StatusScreen
import com.okanetsolutions.briareus.android.ui.VoiceScreen

/** Every window holds the events stream open while it is shown, and shows the store's messages as toasts. */
abstract class BriareusActivity : ComponentActivity() {
    private var release: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        release = store.watch()
    }

    override fun onStop() {
        release?.invoke()
        release = null
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun content(body: @Composable () -> Unit) = setContent {
        BriareusTheme {
            val context = LocalContext.current
            LaunchedEffect(Unit) { store.messages.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() } }
            Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) { body() }
        }
    }
}

/** The main window: pairing until connected, then the projects and conversations beside the chosen one. */
class MainActivity : BriareusActivity() {
    private var notificationsEnabled by mutableStateOf(false)
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsEnabled = granted
        store.backgroundAlerts = granted
        if (granted) EventsService.start(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationsEnabled = store.backgroundAlerts
        app.navigator.restore(savedInstanceState)
        if (savedInstanceState == null) openIntent(intent)
        content {
            val connection by store.connection.collectAsState()
            if (connection == null) {
                PairingScreen(store)
            } else {
                LaunchedEffect(Unit) { if (store.backgroundAlerts) EventsService.start(this@MainActivity) }
                val voicePhase by app.voice.phase.collectAsState()
                key(connection?.address?.origin, connection?.device?.id, connection?.device?.permission, connection?.device?.repos?.toSet()) {
                    AndroidShell(
                        store, app.navigator, voicePhase != VoiceSession.Phase.OFF, notificationsEnabled,
                        onVoice = { Windows.openVoice(this) },
                        onBackgroundAlerts = { on ->
                            if (on && Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                notificationsEnabled = on
                                store.backgroundAlerts = on
                                if (on) EventsService.start(this) else EventsService.stop(this)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        app.navigator.save(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openIntent(intent)
    }

    private fun openIntent(intent: Intent) {
        val id = intent.getStringExtra(ConversationActivity.EXTRA_SESSION) ?: intent.data?.lastPathSegment
        if (id != null) app.navigator.open(Pane.Open(id))
    }

}

/** One conversation in its own panel. */
class ConversationActivity : BriareusActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(EXTRA_SESSION) ?: intent.data?.lastPathSegment
        if (id == null || store.connection.value == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        content {
            val connection by store.connection.collectAsState()
            LaunchedEffect(connection) { if (connection == null) finishAndRemoveTask() }
            ConversationScreen(store, id, onBack = null, onPopOut = null, onDeleted = { finishAndRemoveTask() })
        }
    }

    companion object {
        const val EXTRA_SESSION = "session"

        fun pendingIntent(context: Context, sessionId: String): PendingIntent = PendingIntent.getActivity(
            context, sessionId.hashCode(), Windows.conversationIntent(context, sessionId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}

/** The narrow status panel. */
class StatusActivity : BriareusActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (store.connection.value == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        content {
            val connection by store.connection.collectAsState()
            LaunchedEffect(connection) { if (connection == null) finishAndRemoveTask() }
            StatusScreen(store) { id -> Windows.openConversation(this, id) }
        }
    }
}

/** A ▶ Run preview in its own panel; opening another brings this panel back with the new address. */
class PreviewActivity : BriareusActivity() {
    private var url by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        url = intent.getStringExtra(EXTRA_URL)
        if (url == null || store.connection.value == null) {
            finish()
            return
        }
        content {
            val connection by store.connection.collectAsState()
            LaunchedEffect(connection) { if (connection == null) finishAndRemoveTask() }
            url?.let { key(it) { PreviewScreen(store, it) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_URL)?.let { url = it }
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}

/** The voice panel: a spoken conversation about your projects that also drives the other windows. */
class VoiceActivity : BriareusActivity() {
    private var startAfterPermission = false
    private val microphone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && startAfterPermission) app.voice.start()
        else if (!granted) Toast.makeText(this, "Briareus needs the microphone to talk with you.", Toast.LENGTH_LONG).show()
        startAfterPermission = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (store.connection.value == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        content {
            val connection by store.connection.collectAsState()
            LaunchedEffect(connection) { if (connection == null) { app.voice.stop(); finishAndRemoveTask() } }
            VoiceScreen(app.voice, app.voiceSettings) {
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) app.voice.start()
                else { startAfterPermission = true; microphone.launch(Manifest.permission.RECORD_AUDIO) }
            }
        }
    }
}
