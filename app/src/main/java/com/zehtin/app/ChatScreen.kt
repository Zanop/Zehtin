package com.zehtin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch


@Composable
fun ChatScreen(onOpenMembers: () -> Unit) {
    val messages by WebSocketManager.messages.collectAsState()
    val memberCount by WebSocketManager.memberCount.collectAsState()
    val members by WebSocketManager.members.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto scroll to bottom when new message arrives
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text("Zeh", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ZehtinDeep)
                Text("ti", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ZehtinAccent)
                Text("n", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ZehtinDeep)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onOpenMembers,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ZehtinSurface)
                ) {
                    Icon(Icons.Default.People, contentDescription = "Members",
                        tint = ZehtinDeep, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ZehtinSurface)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Encrypted",
                        tint = ZehtinOlive, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Member avatars row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(members) { member ->
                MemberAvatar(member)
            }
        }

        HorizontalDivider(color = ZehtinBorder, modifier = Modifier.padding(horizontal = 20.dp))

        Text(
            text = "Group · ${if (memberCount > 0) memberCount else sampleMembers.size} members",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            color = ZehtinMuted,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = ZehtinSurface,
                        border = ButtonDefaults.outlinedButtonBorder
                    ) {
                        Text(
                            text = "Today",
                            fontSize = 10.sp,
                            color = ZehtinMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            items(messages) { message ->
                MessageRow(message)
            }
        }

        // Input bar
        HorizontalDivider(color = ZehtinBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZehtinBg)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    WebSocketManager.sendMedia("photo_shared.jpg", "1.8 MB")
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ZehtinSurface)
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = "Attach",
                    tint = ZehtinDeep, modifier = Modifier.size(18.dp))
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message…", color = ZehtinMuted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZehtinBorder,
                    unfocusedBorderColor = ZehtinBorder,
                    focusedContainerColor = ZehtinSurface,
                    unfocusedContainerColor = ZehtinSurface,
                    focusedTextColor = ZehtinDeep,
                    unfocusedTextColor = ZehtinDeep,
                    cursorColor = ZehtinAccent
                ),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        WebSocketManager.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ZehtinAccent)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send",
                    tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun MemberAvatar(member: Member) {
    val avatarColors = listOf(ZehtinOlive, ZehtinAccent, ZehtinGreen,
        Color(0xFF7A6A5A), Color(0xFF5A7A8A))
    val colorIndex = member.id.hashCode().let { if (it < 0) -it else it } % avatarColors.size

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                Text(member.initials, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            if (member.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6DB86D))
                        .align(Alignment.BottomEnd)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(member.name.split(" ").first(),
            fontSize = 10.sp, color = ZehtinMuted)
    }
}

@Composable
fun MessageRow(message: Message) {
    val avatarColors = listOf(ZehtinOlive, ZehtinAccent, ZehtinGreen,
        Color(0xFF7A6A5A), Color(0xFF5A7A8A))
    val colorIndex = message.senderId.hashCode().let { if (it < 0) -it else it } % avatarColors.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.isOutgoing) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(avatarColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                Text(message.senderName.take(2).uppercase(),
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start) {
            if (!message.isOutgoing) {
                Text(
                    text = message.senderName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ZehtinAccent,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            if (message.isMedia) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = ZehtinSurface,
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.widthIn(max = 200.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(
                                    if (message.isOutgoing) Color(0xFF2A2A1E)
                                    else Color(0xFFE8E0D0)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📄", fontSize = 28.sp)
                        }
                        Text(
                            text = "${message.mediaName} · ${message.mediaSize}",
                            fontSize = 11.sp,
                            color = ZehtinMuted,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (message.isOutgoing) 18.dp else 4.dp,
                        bottomEnd = if (message.isOutgoing) 4.dp else 18.dp
                    ),
                    color = if (message.isOutgoing) ZehtinDeep else Color.White,
                    modifier = Modifier.widthIn(max = 240.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 13.sp,
                        color = if (message.isOutgoing) Color(0xFFF0EBE0) else ZehtinDeep,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            ) {
                Text(message.time, fontSize = 10.sp, color = ZehtinMuted)
                if (message.isOutgoing) {
                    Text("✓✓", fontSize = 10.sp, color = ZehtinMuted)
                }
            }
        }
    }
}