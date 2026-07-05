package com.zeka.sandbox

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

object AgentQueueProcessor {
    private val queue = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            for (sessionId in queue) {
                processNextStep(sessionId)
            }
        }
    }

    fun enqueueSessionStep(sessionId: String) {
        scope.launch {
            queue.send(sessionId)
        }
    }

    private fun processNextStep(sessionId: String) {
        val session = AgentLoopManager.getSession(sessionId) ?: return
        
        // Execute the next planned task
        val updatedSession = AgentLoopManager.executeNextStep(sessionId)
        if (updatedSession == null) {
            NotificationManager.sendNotification(sessionId, "Hata", "Görev yürütülürken hata oluştu.")
            return
        }

        when (updatedSession.status) {
            "planned" -> {
                val nextTask = updatedSession.tasks.getOrNull(updatedSession.currentTaskIndex)
                NotificationManager.sendNotification(
                    sessionId = sessionId,
                    title = "Onay Bekliyor",
                    message = "Komut onayınızı bekliyor: ${nextTask?.command ?: ""}"
                )
            }
            "completed" -> {
                NotificationManager.sendNotification(
                    sessionId = sessionId,
                    title = "Görev Tamamlandı",
                    message = "Ajan tüm plan adımlarını başarıyla tamamladı."
                )
            }
            "failed" -> {
                NotificationManager.sendNotification(
                    sessionId = sessionId,
                    title = "Görev Başarısız",
                    message = "Ajan bir adımda hata ile karşılaştı ve durdu."
                )
            }
        }
    }
}
