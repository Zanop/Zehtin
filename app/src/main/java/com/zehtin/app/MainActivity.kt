package com.zehtin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZehtinTheme {
                ZehtinApp()
            }
        }
    }
}

@Composable
fun ZehtinApp() {
    var currentScreen by rememberSaveable { mutableStateOf("login") }

    when (currentScreen) {
        "login" -> LoginScreen(
            onJoin = { currentScreen = "chat" }
        )
        "chat" -> ChatScreen(
            onOpenMembers = { currentScreen = "members" }
        )
        "members" -> MembersScreen(
            onBack = { currentScreen = "chat" }
        )
    }
}