package com.mowtiie.mesgard.ui.about

import androidx.lifecycle.ViewModel
import com.mowtiie.mesgard.domain.server.ServerConfigStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class AboutViewModel @Inject constructor(serverConfigStore: ServerConfigStore) : ViewModel() {
    /** Only the host, never the access key. */
    val serverHost: String? = serverConfigStore.current?.host
}
