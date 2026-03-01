package com.zehtin.app

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Member(
    val id: String,
    val name: String,
    val initials: String,
    val isOnline: Boolean = false,
    val isAdmin: Boolean = false,
    val lastSeen: String = ""
)

data class Message(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
    val isOutgoing: Boolean = false,
    val isMedia: Boolean = false,
    val mediaName: String = "",
    val mediaSize: String = "",
    val fileUrl: String = "",
    val isImage: Boolean = false
)

val sampleMembers = listOf(
    Member("1", "Kemal A.", "KA", isOnline = true, isAdmin = true),
    Member("2", "Layla Y.", "LY", isOnline = true),
    Member("3", "Nour R.", "NR", lastSeen = "2h ago"),
    Member("4", "Emil M.", "EM", lastSeen = "Yesterday"),
    Member("5", "Sara R.", "SR", lastSeen = "3d ago")
)

val sampleMessages = mutableListOf(
    Message("1", "1", "Kemal", "Everyone on for the call tonight?", "10:14"),
    Message("2", "3", "Nour", "Yes! Sending the files now 📎", "10:15"),
    Message(
        "3", "3", "Nour", "", "10:16",
        isMedia = true, mediaName = "project_brief_v3.pdf", mediaSize = "2.4 MB"
    ),
    Message("4", "me", "You", "Got it, reviewing now. Looks solid 👍",
        "10:18", isOutgoing = true),
    Message("5", "1", "Kemal", "Let's discuss on the call. 7pm works?", "10:22")
)