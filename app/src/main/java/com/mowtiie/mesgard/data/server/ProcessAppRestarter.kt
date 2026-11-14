package com.mowtiie.mesgard.data.server

import android.content.Context
import android.content.Intent
import android.os.Process
import com.mowtiie.mesgard.domain.server.AppRestarter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Opens the launcher activity in a new task and ends this process, so the next start reads the new server. */
internal class ProcessAppRestarter @Inject constructor(@ApplicationContext private val context: Context) :
    AppRestarter {
    override fun restart() {
        val launchIntent = requireNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)) {
            "The app has no launcher activity"
        }
        context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Process.killProcess(Process.myPid())
    }
}
