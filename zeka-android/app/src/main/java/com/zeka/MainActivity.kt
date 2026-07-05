package com.zeka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zeka.presentation.ui.chat.ChatScreen
import com.zeka.presentation.ui.theme.ZekaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZekaTheme {
                ChatScreen()
            }
        }
    }
}
