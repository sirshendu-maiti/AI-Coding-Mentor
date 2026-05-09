package com.euphoria.aimentor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.euphoria.aimentor.ui.MentorApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // FIX: installSplashScreen() MUST be called before super.onCreate()
        installSplashScreen()
        
        // Enable edge-to-edge for proper WindowInsets (IME/Keyboard) handling in Compose
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        setContent {
            MentorApp()
        }
    }
}
