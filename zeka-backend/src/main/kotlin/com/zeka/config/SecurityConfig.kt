package com.zeka.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityConfig {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    
    // Fallback 32-byte key for local development
    private val secretKeyString = System.getenv("ENCRYPTION_KEY") ?: "ZEKA_SUPER_SECRET_AES_KEY_32_BYTES"
    private val secretKey = SecretKeySpec(secretKeyString.toByteArray(Charsets.UTF_8).copyOf(32), "AES")

    val jwtSecret = System.getenv("JWT_SECRET") ?: "zeka_jwt_secret_key_for_signing_tokens_12345"
    val jwtIssuer = "com.zeka"
    val jwtAudience = "zeka-users"

    fun makeJwtAlgorithm(): Algorithm = Algorithm.HMAC256(jwtSecret)

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        
        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(encryptedText: String): String {
        val combined = Base64.getDecoder().decode(encryptedText)
        if (combined.size < IV_LENGTH_BYTE) {
            throw IllegalArgumentException("Ciphertext too short")
        }
        val iv = combined.copyOfRange(0, IV_LENGTH_BYTE)
        val cipherText = combined.copyOfRange(IV_LENGTH_BYTE, combined.size)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        
        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun maskApiKey(apiKey: String): String {
        if (apiKey.length <= 8) return "***"
        return apiKey.take(4) + "..." + apiKey.takeLast(4)
    }
}
