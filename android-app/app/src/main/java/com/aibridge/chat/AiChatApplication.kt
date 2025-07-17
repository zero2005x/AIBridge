package com.aibridge.chat

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiChatApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}
