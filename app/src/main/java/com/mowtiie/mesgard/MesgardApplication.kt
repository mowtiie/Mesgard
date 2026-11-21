package com.mowtiie.mesgard

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.mowtiie.mesgard.data.attachment.AttachmentVault
import com.mowtiie.mesgard.data.attachment.StoredFileFetcher
import com.mowtiie.mesgard.data.attachment.StoredFileKeyer
import com.mowtiie.mesgard.data.attachment.attachmentDirectory
import com.mowtiie.mesgard.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class MesgardApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    @Inject
    internal lateinit var workerFactory: HiltWorkerFactory

    @Inject
    internal lateinit var attachmentVault: AttachmentVault

    @Inject
    @ApplicationScope
    internal lateinit var applicationScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Files stored before they were encrypted are sealed once, in the background.
        applicationScope.launch(Dispatchers.IO) {
            attachmentVault.sealSettled(applicationContext.attachmentDirectory())
        }
    }

    /** Attachment photos are read through the vault, because the files on the phone are encrypted. */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(StoredFileKeyer())
            add(StoredFileFetcher.Factory(attachmentVault))
        }
        .build()
}

internal fun Context.vaultOf(): AttachmentVault = (applicationContext as MesgardApplication).attachmentVault
