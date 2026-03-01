package com.zehtin.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MembersScreen(onBack: () -> Unit) {
    val members by WebSocketManager.members.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All (${members.size})", "Online", "Admins")

    val filteredMembers = when (selectedTab) {
        1 -> members.filter { it.isOnline }
        2 -> members.filter { it.isAdmin }
        else -> members
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
            Row(
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
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ZehtinDeep,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Members",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ZehtinDeep
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ZehtinSurface)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = "Add member",
                    tint = ZehtinDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isActive = selectedTab == index
                Surface(
                    onClick = { selectedTab = index },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isActive) ZehtinDeep else ZehtinSurface,
                    border = if (!isActive) ButtonDefaults.outlinedButtonBorder else null
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) ZehtinBg else ZehtinMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                }
            }
        }

        // Members list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredMembers) { member ->
                MemberRow(member)
            }
        }

        // Invite notice
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFFFF0EC)
        ) {
            Text(
                text = "🔒 Invite-only group. Share your invite code with trusted members only.",
                fontSize = 11.sp,
                color = Color(0xFF7A4030),
                lineHeight = 18.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun MemberRow(member: Member) {
    val avatarColors = listOf(
        ZehtinOlive, ZehtinAccent, ZehtinGreen,
        Color(0xFF7A6A5A), Color(0xFF5A7A8A)
    )
    val colorIndex = member.id.hashCode().let { if (it < 0) -it else it } % avatarColors.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColors[colorIndex]),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.initials,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ZehtinDeep
            )
            Text(
                text = if (member.isOnline) "🟢 Online" else "⚫ Last seen ${member.lastSeen}",
                fontSize = 11.sp,
                color = ZehtinMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Badge
        Surface(
            shape = RoundedCornerShape(99.dp),
            color = if (member.isAdmin)
                Color(0x26C8693A)
            else
                Color(0x268FAE6B)
        ) {
            Text(
                text = if (member.isAdmin) "Admin" else "Member",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (member.isAdmin) ZehtinAccent else ZehtinGreen,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}