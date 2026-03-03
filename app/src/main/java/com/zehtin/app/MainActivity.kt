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
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // Check for updates on startup
        UpdateManager.checkForUpdates(context) { info ->
            updateInfo = info
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
            "chat" -> ChatScreen(
                onOpenMembers = { currentScreen = "members" },
                onOpenSettings = { currentScreen = "settings" }
            )
            "members" -> MembersScreen(onBack = { currentScreen = "chat" })
            "settings" -> SettingsScreen(onBack = { currentScreen = "chat" })
        }

        // Update Dialog
        updateInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { if (!isDownloading) updateInfo = null },
                title = { Text("Update Available") },
                text = { 
                    Column {
                        Text(if (isDownloading) "Downloading update…" else "A new version of Zehtin is available. Would you like to update now?")
                        if (isDownloading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = ZehtinAccent,
                                trackColor = ZehtinSurface
                            )
                        } else if (info.description != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = info.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    if (!isDownloading) {
                        Button(onClick = {
                            isDownloading = true
                            scope.launch {
                                UpdateManager.downloadAndInstallApk(context, info.apkUrl) { progress ->
                                    downloadProgress = progress
                                }
                                isDownloading = false
                                updateInfo = null
                            }
                        }) {
                            Text("Update")
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading) {
                        TextButton(onClick = { updateInfo = null }) {
                            Text("Later")
                        }
                    }
                }
            )
        }
    }
}