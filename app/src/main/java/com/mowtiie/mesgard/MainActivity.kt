package com.mowtiie.mesgard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mowtiie.mesgard.ui.navigation.MesgardNavHost
import com.mowtiie.mesgard.ui.theme.MesgardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MesgardTheme {
                MesgardNavHost()
            }
        }
    }
}
