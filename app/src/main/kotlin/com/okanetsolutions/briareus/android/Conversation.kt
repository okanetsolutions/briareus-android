package com.okanetsolutions.briareus.android

import com.okanetsolutions.briareus.core.ApiError
import com.okanetsolutions.briareus.core.BriareusJson
import com.okanetsolutions.briareus.core.Session
import com.okanetsolutions.briareus.core.Transcript
import com.okanetsolutions.briareus.core.TranscriptEvent
import com.okanetsolutions.briareus.core.args
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlin.math.min

/**
 * One conversation's transcript, followed with `GET /sessions/{id}/events` while a window shows it. A saved transcript
 * resumes from its last line, so reopening asks only for what changed.
 */
class Conversation(private val store: Store, val id: String) {
    private val transcript = Transcript()
    private val _events = MutableStateFlow<List<TranscriptEvent>>(emptyList())
    val events: StateFlow<List<TranscriptEvent>> = _events.asStateFlow()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    enum class SendState { READY, SENDING, UNCERTAIN }
    private val pendingSendKey = "pending-send:$id"
    private val _sendState = MutableStateFlow(if (store.cache.read(pendingSendKey) != null) SendState.UNCERTAIN else SendState.READY)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    suspend fun send(text: String, attachments: List<String>): Boolean {
        if (_sendState.value != SendState.READY || !store.can("message")) return false
        val client = store.client ?: return false
        _sendState.value = SendState.SENDING
        var dispatched = false
        try {
            store.cache.write(pendingSendKey, args("pending" to true))
            dispatched = true
            val result = client.call("message", args("sessionId" to id, "text" to text, "attachments" to attachments.ifEmpty { null }))
            if (store.client !== client) return false
            Session.parse(result["session"])?.let(store::upsert)
            store.cache.remove(pendingSendKey)
            _sendState.value = SendState.READY
            return true
        } catch (e: ApiError) {
            if (store.client !== client) return false
            if (e.isRefusal) {
                store.cache.remove(pendingSendKey)
                _sendState.value = SendState.READY
            } else _sendState.value = SendState.UNCERTAIN
            store.failed(e)
            return false
        } catch (e: CancellationException) {
            if (store.client === client) _sendState.value = SendState.UNCERTAIN
            throw e
        } catch (e: Exception) {
            if (store.client !== client) return false
            _sendState.value = if (dispatched) SendState.UNCERTAIN else SendState.READY
            if (!dispatched) store.cache.remove(pendingSendKey)
            store.failed(e)
            return false
        }
    }

    suspend fun resolveSend(): Boolean {
        if (_sendState.value != SendState.UNCERTAIN) return false
        val client = store.client ?: return false
        return try {
            val result = client.call("session", args("sessionId" to id, "since" to transcript.cursor))
            if (store.client !== client) return false
            Session.parse(result["session"])?.let(store::upsert)
            if (transcript.append(result["events"] as? JsonArray)) publish()
            store.cache.remove(pendingSendKey)
            _sendState.value = SendState.READY
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (store.client === client) store.failed(e)
            false
        }
    }

    private var viewers = 0
    private var job: Job? = null
    private var saveJob: Job? = null

    init {
        (store.cache.read(cacheKey) as? JsonArray)?.let {
            transcript.append(it)
            publish()
            _loading.value = false
        }
    }

    private val cacheKey get() = "transcript:$id"

    /** Streams while the returned release has not run. */
    fun watch(): () -> Unit {
        viewers++
        if (job == null) job = store.scope.launch { follow() }
        var released = false
        return {
            if (!released) {
                released = true
                if (--viewers == 0) { job?.cancel(); job = null }
            }
        }
    }

    /** Reads the whole transcript again (F5 on Windows): drops the saved lines and starts from zero. */
    fun reload() {
        job?.cancel()
        transcript.clear()
        publish()
        store.cache.remove(cacheKey)
        _loading.value = true
        if (viewers > 0) job = store.scope.launch { follow() }
    }

    fun close() {
        saveJob?.cancel()
        job?.cancel()
        job = null
    }

    private suspend fun follow() {
        var backoff = 1_000L
        while (true) {
            val client = store.client ?: return
            try {
                // A first read fills the screen at once; the stream then carries every line after it, resuming by seq.
                if (transcript.size == 0) {
                    val first = client.call("session", args("sessionId" to id, "since" to transcript.cursor))
                    if (store.client !== client) return
                    Session.parse(first["session"])?.let(store::upsert)
                    if (transcript.append(first["events"] as? JsonArray)) publish()
                }
                _loading.value = false
                client.stream("session_events", args("sessionId" to id, "since" to transcript.cursor)).collect { event ->
                    if (store.client !== client) return@collect
                    backoff = 1_000L
                    val data = runCatching { BriareusJson.parseToJsonElement(event.data) }.getOrNull() ?: return@collect
                    when (event.event) {
                        null, "message" -> TranscriptEvent.parse(data)?.let { if (transcript.append(listOf(it))) publish() }
                        "session" -> Session.parse(data)?.let(store::upsert)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiError) {
                if (store.client !== client) return
                _loading.value = false
                if (e.unauthorized || e.status == 404) { store.failed(e); return }
                e.retryAfter?.let { backoff = maxOf(backoff, (it * 1000).toLong()) }
            } catch (_: Exception) {
                _loading.value = false
            }
            delay(backoff)
            backoff = min(backoff * 2, 30_000L)
        }
    }

    private fun publish() {
        _events.value = transcript.events
        saveJob?.cancel()
        saveJob = store.scope.launch {
            delay(1_500)
            store.cache.write(cacheKey, JsonArray(transcript.events.map { it.raw }))
        }
    }

    /** Reads the lines after the cursor once, without a stream: what a notification needs to offer the agent's options. */
    suspend fun catchUp() {
        val client = store.client ?: return
        runCatching { client.call("session", args("sessionId" to id, "since" to transcript.cursor)) }.onSuccess { r ->
            if (store.client !== client) return@onSuccess
            if (transcript.append(r["events"] as? JsonArray)) publish()
            _loading.value = false
        }
    }

    /** The question the agent waits on, if any. */
    fun pendingQuestion(): TranscriptEvent? = transcript.pendingQuestion()
}
