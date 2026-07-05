package com.zeka.data.local.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

class LocalModelManager {

    /**
     * Simulates local device-level ONNX/Gemma model inference.
     * Processes input queries and streams back tokenized responses without network access.
     */
    fun generateLocalResponseStream(prompt: String): Flow<String> = flow {
        val query = prompt.lowercase(Locale.getDefault()).trim()
        val response = when {
            query.contains("kuantum") || query.contains("quantum") -> {
                "Yerel Çevrimdışı Model (Gemma Cihaz İçi Çıkarım):\n\n" +
                "Kuantum fiziği ilkelerine (süperpozisyon ve dolaşıklık) dayanarak çalışan bilgisayarlardır.\n" +
                "Klasik bitler (0 veya 1) yerine kuantum bitleri (kubit) kullanırlar. Kubitler süperpozisyon sayesinde " +
                "aynı anda hem 0 hem de 1 durumunda bulunabilirler, bu da işlem kapasitesini üstel olarak artırır."
            }
            query.contains("merhaba") || query.contains("selam") -> {
                "Yerel Çevrimdışı Model (Gemma Cihaz İçi Çıkarım):\n\n" +
                "Merhaba! Ben Zeka uygulamasının cihazınızda tamamen çevrimdışı çalışan yapay zeka modeliyim. " +
                "Size nasıl yardımcı olabilirim? Şu an herhangi bir internet bağlantısına ihtiyaç duymuyorum."
            }
            query.contains("kod") || query.contains("yazılım") || query.contains("program") -> {
                "Yerel Çevrimdışı Model (Gemma Cihaz İçi Çıkarım):\n\n" +
                "Cihaz içi çıkarım kullanarak kodlama sorularınıza yanıt verebilirim. Örneğin, basit bir Kotlin fonksiyonu:\n\n" +
                "```kotlin\nfun main() {\n    println(\"Zeka Çevrimdışı Model Çalışıyor!\")\n}\n```"
            }
            query.contains("kimsin") || query.contains("adın") -> {
                "Yerel Çevrimdışı Model (Gemma Cihaz İçi Çıkarım):\n\n" +
                "Ben Zeka'nın cihaz içi (Local) yapay zeka motoruyum. İnternetiniz olmadığında veya veri gizliliğinizi " +
                "yüzde yüz korumak istediğinizde doğrudan telefonunuzun işlemcisini kullanarak çalışırım."
            }
            else -> {
                "Yerel Çevrimdışı Model (Gemma Cihaz İçi Çıkarım):\n\n" +
                "Sorunuzu cihaz içi yerel zeka motoru ile analiz ettim. İnternet bağlantısı olmadan yerel çıkarım yapıyorum. " +
                "Bu modda sorularınızı son derece güvenli ve gizlilik odaklı olarak cevaplayabilirim."
            }
        }

        // Split the response into words/tokens to simulate real-time LLM token streaming
        val tokens = response.split(" ")
        for (token in tokens) {
            emit("$token ")
            delay(80) // Simulate 80ms delay per token generation
        }
    }
}
