package com.okanetsolutions.briareus.android

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Routes conversation intents into the main task and opens preview or voice activities. */
object Windows {
    fun conversationIntent(context: Context, sessionId: String): Intent =
        Intent(context, MainActivity::class.java)
            .setData("briareus://session/$sessionId".toUri())
            .putExtra(ConversationActivity.EXTRA_SESSION, sessionId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    fun openConversation(context: Context, sessionId: String) = context.startActivity(conversationIntent(context, sessionId))

    fun openStatus(context: Context) = context.startActivity(
        Intent(context, StatusActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    /** A ▶ Run preview, in the app's own browser panel so it carries the server's Cloudflare Access service token. */
    fun openPreview(context: Context, url: String) = context.startActivity(
        Intent(context, PreviewActivity::class.java).putExtra(PreviewActivity.EXTRA_URL, url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    fun openVoice(context: Context) = context.startActivity(
        Intent(context, VoiceActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
