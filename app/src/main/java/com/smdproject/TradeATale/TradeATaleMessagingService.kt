package com.smdproject.TradeATale

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class TradeATaleMessagingService : FirebaseMessagingService() {

    private val CHANNEL_ID = "chat_messages_channel"
    private val CHANNEL_NAME = "Chat Messages"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference

        db.child("userTokens").child(user.uid).setValue(token)
            .addOnSuccessListener {
                android.util.Log.d("FCM", "FCM token saved successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FCM", "Failed to save FCM token", e)
            }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        // Handle notification payload
        val title = message.notification?.title 
            ?: message.data["title"] 
            ?: "New message"
        val body = message.notification?.body 
            ?: message.data["body"] 
            ?: "You have a new message in TradeATale"
        
        // Get conversation and sender info from data payload
        val conversationId = message.data["conversationId"]
        val senderId = message.data["senderId"]
        val messageType = message.data["type"] ?: "message"

        showNotification(title, body, senderId, conversationId, messageType)
    }

    private fun showNotification(
        title: String, 
        body: String, 
        senderId: String? = null,
        conversationId: String? = null,
        type: String = "message"
    ) {
        createNotificationChannel()

        // Create intent to open ChatPage when notification is clicked
        val intent = when (type) {
            "message" -> {
                if (senderId != null) {
                    Intent(this, ChatPage::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("chat_user_id", senderId)
                        conversationId?.let { putExtra("conversation_id", it) }
                    }
                } else {
                    Intent(this, MessagesPage::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                }
            }
            "barter_request" -> {
                Intent(this, BarterRequestPage::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            else -> {
                Intent(this, MessagesPage::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo_orange_right)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages and barter requests"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}


