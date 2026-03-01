package com.zehtin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(onJoin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val connectionState by WebSocketManager.connectionState.collectAsState()

    LaunchedEffect(connectionState) {
        when (connectionState) {
            is WebSocketManager.ConnectionState.Connected -> {
                isLoading = false
                onJoin()
            }
            is WebSocketManager.ConnectionState.Error -> {
                isLoading = false
                errorMsg = "Invalid invite code or server unreachable."
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZehtinDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                Text("Zeh", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF5F0E8))
                Text("ti", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = ZehtinAccent)
                Text("n", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF5F0E8))
            }

            Text(
                text = "Private. Encrypted. Yours.",
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Color(0xFF6B6B58),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Your name", color = Color(0xFF4A4A38)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZehtinAccent,
                    unfocusedBorderColor = Color(0xFF3A3A2A),
                    focusedTextColor = Color(0xFFF5F0E8),
                    unfocusedTextColor = Color(0xFFF5F0E8),
                    cursorColor = ZehtinAccent,
                    focusedContainerColor = Color(0xFF2A2A1E),
                    unfocusedContainerColor = Color(0xFF2A2A1E)
                )
            )

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                placeholder = { Text("Invite code", color = Color(0xFF4A4A38)) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZehtinAccent,
                    unfocusedBorderColor = Color(0xFF3A3A2A),
                    focusedTextColor = Color(0xFFF5F0E8),
                    unfocusedTextColor = Color(0xFFF5F0E8),
                    cursorColor = ZehtinAccent,
                    focusedContainerColor = Color(0xFF2A2A1E),
                    unfocusedContainerColor = Color(0xFF2A2A1E)
                )
            )

            if (errorMsg.isNotEmpty()) {
                Text(
                    text = errorMsg,
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && inviteCode.isNotBlank()) {
                        isLoading = true
                        errorMsg = ""
                        WebSocketManager.connect(name, inviteCode)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ZehtinAccent),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("JOIN GROUP →", fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Access requires an invite code.\nContact your group admin to get one.",
                fontSize = 11.sp,
                color = Color(0xFF4A4A38),
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}