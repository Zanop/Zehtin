package com.zehtin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var showEditInviteCode by remember { mutableStateOf(false) }
    var showEditName by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    val myName by WebSocketManager.myName.collectAsState()
    val savedInviteCode by WebSocketManager.savedInviteCode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZehtinBg)
            .systemBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ZehtinSurface)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ZehtinDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ZehtinDeep
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Account",
                subtitle = myName.ifEmpty { "Guest" },
                onClick = { showEditName = true }
            )
            SettingsItem(
                icon = Icons.Default.Key,
                title = "Invite Code",
                subtitle = savedInviteCode.ifEmpty { "None" },
                onClick = { showEditInviteCode = true }
            )
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                subtitle = "Sound, vibration and alerts",
                onClick = {}
            )
        }
    }

    if (showEditInviteCode) {
        EditInviteCodeDialog(
            currentCode = savedInviteCode,
            onConfirm = { newCode ->
                WebSocketManager.saveCredentials(context, myName, newCode)
                WebSocketManager.disconnect()
                WebSocketManager.connect(myName, newCode)
                showEditInviteCode = false
            },
            onDismiss = { showEditInviteCode = false }
        )
    }

    if (showEditName) {
        EditNameDialog(
            currentName = myName,
            onConfirm = { newName ->
                WebSocketManager.updateMemberName(newName)
                showEditName = false
            },
            onDismiss = { showEditName = false }
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ZehtinSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ZehtinAccent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = ZehtinDeep, fontSize = 15.sp)
                Text(subtitle, color = ZehtinMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EditInviteCodeDialog(
    currentCode: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCode by remember { mutableStateOf(currentCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ZehtinSurface,
        title = {
            Text(
                "Edit Invite Code",
                fontWeight = FontWeight.Bold,
                color = ZehtinDeep
            )
        },
        text = {
            OutlinedTextField(
                value = newCode,
                onValueChange = { newCode = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZehtinAccent,
                    unfocusedBorderColor = ZehtinBorder,
                    focusedTextColor = ZehtinDeep,
                    unfocusedTextColor = ZehtinDeep,
                    cursorColor = ZehtinAccent,
                    focusedContainerColor = ZehtinBg,
                    unfocusedContainerColor = ZehtinBg
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newCode.isNotBlank()) onConfirm(newCode) },
                colors = ButtonDefaults.buttonColors(containerColor = ZehtinAccent)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ZehtinMuted)
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ZehtinTheme {
        SettingsScreen(onBack = {})
    }
}