package com.okanetsolutions.briareus.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.okanetsolutions.briareus.android.Store
import com.okanetsolutions.briareus.android.VoiceRecorder
import kotlinx.coroutines.launch

/** Keyboard input with optional voice transcription appended to the current draft. */
@Composable
fun VoiceField(
    store: Store,
    text: String,
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minHeight: Dp = 52.dp,
    maxHeight: Dp = 220.dp,
    enabled: Boolean = true,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var transcribing by remember { mutableStateOf(false) }
    val current by rememberUpdatedState(text)
    DisposableEffect(Unit) { onDispose { recorder.stopQuietly() } }
    val off = store.voiceNotesOff()

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { recorder.start(); recording = true }.onFailure { store.failed(Exception("The microphone could not start.")) }
        else store.say("Briareus needs the microphone to write by voice.")
    }

    fun toggle() {
        if (off != null || transcribing || !enabled) return
        if (!recording) { micPermission.launch(android.Manifest.permission.RECORD_AUDIO); return }
        recording = false
        val audio = recorder.stop() ?: return
        transcribing = true
        scope.launch {
            try {
                val heard = store.client?.transcribe(audio, VoiceRecorder.CONTENT_TYPE).orEmpty().trim()
                if (heard.isNotEmpty()) onText(if (current.isBlank()) heard else current.trimEnd() + " " + heard)
            } catch (e: Exception) {
                store.failed(e)
            } finally {
                transcribing = false
            }
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (off == null) {
            IconButton(onClick = ::toggle, enabled = enabled && !transcribing, modifier = Modifier.size(52.dp)) {
                when {
                    transcribing -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    recording -> Icon(Icons.Outlined.StopCircle, "Stop recording", Modifier.size(30.dp), tint = p.danger)
                    else -> Icon(Icons.Outlined.Mic, "Record a voice note", Modifier.size(30.dp), tint = p.accent)
                }
            }
        }
        androidx.compose.material3.OutlinedTextField(
            value = text, onValueChange = onText,
            modifier = Modifier.weight(1f).heightIn(min = minHeight, max = maxHeight),
            enabled = enabled,
            placeholder = { Text(if (recording) "Recording…" else if (transcribing) "Transcribing…" else placeholder) },
        )
        if (text.isNotBlank() && !recording && !transcribing) {
            IconButton(onClick = { onText("") }, enabled = enabled) { Icon(Icons.Outlined.Close, "Clear the text", tint = p.muted) }
        }
    }
}
