package com.zeka.sandbox

object NotificationManager {
    fun sendNotification(sessionId: String, title: String, message: String) {
        println("[NOTIFICATION] Session $sessionId | $title : $message")
        // Ready for future FCM integration:
        // FirebaseMessaging.getInstance().send(message)
    }
}
