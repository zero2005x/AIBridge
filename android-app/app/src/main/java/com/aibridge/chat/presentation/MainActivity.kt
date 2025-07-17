package com.aibridge.chat.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aibridge.chat.presentation.theme.AiChatTheme
import com.aibridge.chat.presentation.ui.AiChatApp
import com.aibridge.chat.presentation.viewmodel.AuthViewModel
import com.aibridge.chat.presentation.viewmodel.LoginStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AiChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val windowSizeClass = calculateWindowSizeClass(this@MainActivity)
                    val loginStatus by authViewModel.loginStatus.collectAsStateWithLifecycle()
                    
                    AiChatApp(
                        windowSizeClass = windowSizeClass,
                        loginStatus = loginStatus,
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}
