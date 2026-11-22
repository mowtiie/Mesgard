package com.mowtiie.mesgard.data

import com.mowtiie.mesgard.domain.model.GroupKind

internal fun String.toKind(): GroupKind = GroupKind.entries.firstOrNull { it.toWireName() == this } ?: GroupKind.OTHER

internal fun GroupKind.toWireName(): String = name.lowercase()
