package com.mowtiie.mesgard.data.push

import com.mowtiie.mesgard.domain.lifecycle.AppVisibility
import javax.inject.Inject

internal class PushMessageHandler @Inject constructor(
    private val notifier: MessageNotifier,
    private val appVisibility: AppVisibility,
) {
    /** The foreground app already shows new messages live, so only a background app notifies. */
    fun handle(content: ByteArray) {
        if (appVisibility.isForeground.value) return
        IncomingPush.fromContent(content)?.let(notifier::show)
    }
}
