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
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box

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
    var updateUrl by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Check for updates on startup
        UpdateManager.checkForUpdates(context) { url ->
            updateUrl = url
        }

        if (WebSocketManager.savedName.isNotEmpty() &&
            WebSocketManager.savedInviteCode.isNotEmpty()) {
            WebSocketManager.connect(
                WebSocketManager.savedName,
                WebSocketManager.savedInviteCode
            )
        }
    }

    Box {
        when (currentScreen) {
            "login" -> LoginScreen(onJoin = { currentScreen = "chat" })
            "chat" -> ChatScreen(onOpenMembers = { currentScreen = "members" })
            "members" -> MembersScreen(onBack = { currentScreen = "chat" })
        }

        // Update Dialog
        updateUrl?.let { url ->
            AlertDialog(
                onDismissRequest = { if (!isDownloading) updateUrl = null },
                title = { Text("Update Available") },
                text = { 
                    Text(if (isDownloading) "Downloading update..." else "A new version of Zehtin is available. Would you like to update now?")
                },
                confirmButton = {
                    if (!isDownloading) {
                        Button(onClick = {
                            isDownloading = true
                            scope.launch {
                                UpdateManager.downloadAndInstallApk(context, url)
                                isDownloading = false
                                updateUrl = null
                            }
                        }) {
                            Text("Update")
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { updateUrl = null }) {
                            Text("Later")
                        }
                    }
                }
            )
        }
    }
}