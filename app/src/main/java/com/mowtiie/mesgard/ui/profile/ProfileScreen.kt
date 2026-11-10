package com.mowtiie.mesgard.ui.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.TitledScreen
import com.mowtiie.mesgard.ui.theme.MesgardTheme

@Composable
internal fun ProfileScreen(onSignOut: () -> Unit, modifier: Modifier = Modifier) {
    TitledScreen(title = stringResource(R.string.tab_profile), modifier = modifier) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onSignOut) {
                Text(stringResource(R.string.sign_out_button))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    MesgardTheme {
        ProfileScreen(onSignOut = {})
    }
}
