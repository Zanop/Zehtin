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
        WebSocketManager.init(this)
        enableEdgeToEdge()
        setContent {
            ZehtinTheme {
                ZehtinApp(context = this)
            }
        }
    }
}

@Composable
fun ZehtinApp(context: android.content.Context) {
    val startScreen = remember {
        android.util.Log.d("Zehtin", "savedName='${WebSocketManager.savedName}' savedCode='${WebSocketManager.savedInviteCode}'")
        if (WebSocketManager.savedName.isNotEmpty() &&
            WebSocketManager.savedInviteCode.isNotEmpty()) "chat" else "login"
    }

    var currentScreen by rememberSaveable { mutableStateOf(startScreen) }

    LaunchedEffect(Unit) {
        if (WebSocketManager.savedName.isNotEmpty() &&
            WebSocketManager.savedInviteCode.isNotEmpty()) {
            WebSocketManager.connect(
                WebSocketManager.savedName,
                WebSocketManager.savedInviteCode
            )
        }
    }

    when (currentScreen) {
        "login" -> LoginScreen(onJoin = { currentScreen = "chat" })
        "chat" -> ChatScreen(onOpenMembers = { currentScreen = "members" })
        "members" -> MembersScreen(onBack = { currentScreen = "chat" })
    }
}