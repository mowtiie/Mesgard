package com.mowtiie.mesgard.ui.lock

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.theme.MesgardTheme
import kotlinx.coroutines.launch

private val ContentSpacing = 16.dp
private val IconSize = 48.dp

@Composable
internal fun LockScreenRoute(modifier: Modifier = Modifier, viewModel: LockViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    val promptTitle = stringResource(R.string.lock_prompt_title)
    var isPrompting by remember { mutableStateOf(false) }

    fun requestUnlock() {
        if (activity == null || isPrompting) return
        isPrompting = true
        scope.launch {
            when (BiometricAuthenticator.availability(context)) {
                AuthAvailability.AVAILABLE -> when (BiometricAuthenticator.authenticate(activity, promptTitle)) {
                    AuthOutcome.SUCCESS -> viewModel.onAuthenticated()
                    AuthOutcome.CANCELLED, AuthOutcome.FAILED -> Unit
                }
                AuthAvailability.NOT_SET_UP, AuthAvailability.UNAVAILABLE -> viewModel.onAuthenticationUnavailable()
            }
            isPrompting = false
        }
    }

    LaunchedEffect(Unit) { requestUnlock() }
    LockScreen(onUnlockClicked = ::requestUnlock, modifier = modifier)
}

@Composable
internal fun LockScreen(onUnlockClicked: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(ContentSpacing * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContentSpacing, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSize),
            )
            Text(text = stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineMedium)
            Button(onClick = onUnlockClicked) { Text(stringResource(R.string.lock_unlock_button)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LockScreenPreview() {
    MesgardTheme { LockScreen(onUnlockClicked = {}) }
}
