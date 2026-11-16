package com.mowtiie.mesgard.ui.about

internal data class ThirdPartyNotice(val name: String, val license: String)

private const val APACHE_2 = "Apache License 2.0"

/** The main libraries and the artwork Mesgard is built with. The full texts are in each project's repository. */
internal val ThirdPartyNotices = listOf(
    ThirdPartyNotice("AndroidX (Compose, Room, Navigation, Lifecycle, WorkManager, Biometric)", APACHE_2),
    ThirdPartyNotice("Coil", APACHE_2),
    ThirdPartyNotice("Dagger and Hilt", APACHE_2),
    ThirdPartyNotice("Kotlin and kotlinx (coroutines, serialization)", APACHE_2),
    ThirdPartyNotice("Ktor and OkHttp", APACHE_2),
    ThirdPartyNotice("Material Icons", APACHE_2),
    ThirdPartyNotice("SQLCipher for Android", "BSD-style license"),
    ThirdPartyNotice("supabase-kt", "MIT License"),
    ThirdPartyNotice("UnifiedPush connector", APACHE_2),
    ThirdPartyNotice("unDraw illustrations", "unDraw license (free for personal and commercial use)"),
)
