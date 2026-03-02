package com.zehtin.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Normally you'd send this to your server
        // For now we'll store it in preferences
        getSharedPreferences("zehtin", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
        
        // If we're already connected, send it now
        WebSocketManager.sendFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val senderName = remoteMessage.data["senderName"] ?: "New Message"
            val text = remoteMessage.data["text"] ?: ""
            val isMedia = remoteMessage.data["isMedia"]?.toBoolean() ?: false
            
            // We reuse the notification logic from WebSocketManager or similar
            // For FCM, the system handles notifications automatically if "notification" payload is present,
            // but for "data" payloads we show it manually.
            
            val message = Message(
                id = "",
                senderId = remoteMessage.data["senderId"] ?: "",
                senderName = senderName,
                text = text,
                time = "", // Server should ideally provide this or we use current
                isOutgoing = false,
                isMedia = isMedia,
                mediaName = remoteMessage.data["mediaName"] ?: "",
                mediaSize = remoteMessage.data["mediaSize"] ?: "",
                fileUrl = remoteMessage.data["fileUrl"] ?: "",
                isImage = remoteMessage.data["isImage"]?.toBoolean() ?: false
            )
            
            // We'll add a static method to WebSocketManager to trigger the notification
            // since we already have the logic there.
            WebSocketManager.triggerManualNotification(message)
        }
    }
}