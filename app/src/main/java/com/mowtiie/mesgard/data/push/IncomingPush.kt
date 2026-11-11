package com.mowtiie.mesgard.data.push

internal const val EXTRA_CONVERSATION_ID = "conversation_id"
private const val KEY_TITLE = "title"
private const val KEY_BODY = "body"

internal data class IncomingPush(val conversationId: String, val title: String, val body: String) {
    companion object {
        fun fromData(data: Map<String, String>): IncomingPush? {
            val conversationId = data[EXTRA_CONVERSATION_ID]?.takeIf { it.isNotBlank() }
            val title = data[KEY_TITLE]?.takeIf { it.isNotBlank() }
            val body = data[KEY_BODY]?.takeIf { it.isNotBlank() }
            if (conversationId == null || title == null || body == null) return null
            return IncomingPush(conversationId, title, body)
        }
    }
}
