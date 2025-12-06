package com.smdproject.TradeATale

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotification

object OneSignalManager {
    private const val ONESIGNAL_APP_ID = "c9f696d4-3e2f-4de4-9308-26c282b08ed3"

    fun initialize(context: android.content.Context) {
        // Set log level for debugging (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE

        // Initialize OneSignal
        OneSignal.initWithContext(context, ONESIGNAL_APP_ID)

        // Handle notification opened (when user taps notification)
        OneSignal.Notifications.addClickListener(object : INotificationClickListener {
            override fun onClick(event: INotificationClickEvent) {
                val notification: INotification = event.notification
                val additionalData = notification.additionalData

                val type = additionalData?.optString("type") ?: "message"
                val senderId = additionalData?.optString("senderId")
                val conversationId = additionalData?.optString("conversationId")

                when (type) {
                    "message" -> {
                        if (senderId != null) {
                            val intent = Intent(context, ChatPage::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                putExtra("chat_user_id", senderId)
                                if (conversationId != null) {
                                    putExtra("conversation_id", conversationId)
                                }
                            }
                            context.startActivity(intent)
                        } else {
                            val intent = Intent(context, MessagesPage::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            context.startActivity(intent)
                        }
                    }
                    "barter_request" -> {
                        val intent = Intent(context, BarterRequestPage::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                    else -> {
                        val intent = Intent(context, MessagesPage::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            }
        })

        // Save Player ID to Firebase (with delay to ensure OneSignal is fully initialized)
        Handler(Looper.getMainLooper()).postDelayed({
            savePlayerIdIfAvailable()
        }, 2000) // Wait 2 seconds for OneSignal to initialize
    }

    private fun savePlayerIdIfAvailable() {
        try {
            val playerId = OneSignal.User.pushSubscription.id
            if (playerId != null && playerId.isNotEmpty()) {
                savePlayerIdToFirebase(playerId)
            } else {
                // Retry after a delay if Player ID is not yet available (max 3 retries)
                var retryCount = 0
                Handler(Looper.getMainLooper()).postDelayed({
                    if (retryCount < 3) {
                        retryCount++
                        savePlayerIdIfAvailable()
                    } else {
                        Log.w("OneSignal", "Player ID not available after multiple retries")
                    }
                }, 3000)
            }
        } catch (e: Exception) {
            Log.e("OneSignal", "Error getting Player ID", e)
        }
    }
    
    // Public function to manually save Player ID (call this after login)
    fun savePlayerIdAfterLogin() {
        Handler(Looper.getMainLooper()).postDelayed({
            savePlayerIdIfAvailable()
        }, 1000)
    }

    private fun savePlayerIdToFirebase(playerId: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        val db = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference

        db.child("oneSignalPlayerIds").child(user.uid).setValue(playerId)
            .addOnSuccessListener {
                Log.d("OneSignal", "Player ID saved successfully: $playerId")
            }
            .addOnFailureListener { e ->
                Log.e("OneSignal", "Failed to save Player ID", e)
            }
    }

    fun getPlayerId(): String? {
        return OneSignal.User.pushSubscription.id
    }
}

