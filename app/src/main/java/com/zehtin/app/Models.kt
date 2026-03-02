package com.zehtin.app

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class Member(
    val id: String,
    val name: String,
    val initials: String,
    val isOnline: Boolean = false,
    val isAdmin: Boolean = false,
    val lastSeen: Long? = null
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