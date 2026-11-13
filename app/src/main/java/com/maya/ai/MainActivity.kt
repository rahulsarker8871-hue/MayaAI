package com.maya.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.maya.ai.ui.MayaChatScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MayaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayaChatScreen(viewModel = viewModel)
        }
    }
}
