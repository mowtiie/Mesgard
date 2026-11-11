package com.mowtiie.mesgard.data.push

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.unifiedpush.android.connector.UnifiedPush

internal interface PushClient {
    /** The display name of the distributor app the user chose, or null when none is chosen. */
    fun distributorName(): String?

    fun register()

    fun unregister()
}

internal class UnifiedPushClient @Inject constructor(@param:ApplicationContext private val context: Context) :
    PushClient {
    override fun distributorName(): String? {
        val packageName = UnifiedPush.getSavedDistributor(context) ?: return null
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    override fun register() = UnifiedPush.register(context)

    override fun unregister() = UnifiedPush.unregister(context)
}
