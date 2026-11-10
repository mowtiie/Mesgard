package com.mowtiie.mesgard.ui.chats

private const val MAX_DISPLAYED_COUNT = 999

internal fun formatUnreadCount(count: Int): String =
    if (count > MAX_DISPLAYED_COUNT) "$MAX_DISPLAYED_COUNT+" else count.toString()
