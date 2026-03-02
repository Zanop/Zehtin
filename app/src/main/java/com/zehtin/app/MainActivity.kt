package com.zehtin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketManager.init(this)
        enableEdgeToEdge()
        setContent {
            ZehtinTheme {
                NotificationPermissionHandler()
                ZehtinApp(context = this)
            }
        }
    }
}

@Composable
fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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