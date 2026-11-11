package com.mowtiie.mesgard.di

import com.mowtiie.mesgard.data.attachment.ContentResolverAttachmentImporter
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.SupabaseAttachmentRemoteSource
import com.mowtiie.mesgard.domain.attachment.AttachmentImporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AttachmentModule {
    @Binds
    abstract fun bindAttachmentImporter(importer: ContentResolverAttachmentImporter): AttachmentImporter

    @Binds
    abstract fun bindAttachmentRemoteSource(source: SupabaseAttachmentRemoteSource): AttachmentRemoteSource
}
