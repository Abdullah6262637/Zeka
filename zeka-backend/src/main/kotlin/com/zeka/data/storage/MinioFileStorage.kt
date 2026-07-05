package com.zeka.data.storage

import io.minio.BucketExistsArgs
import io.minio.GetObjectArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import java.io.InputStream

object MinioFileStorage {
    private val minioUrl = System.getenv("MINIO_URL") ?: "http://localhost:9000"
    private val accessKey = System.getenv("MINIO_ACCESS_KEY") ?: "zeka_admin"
    private val secretKey = System.getenv("MINIO_SECRET_KEY") ?: "zeka_minio_secret_password"
    private const val BUCKET_NAME = "zeka-attachments"

    private val minioClient: MinioClient by lazy {
        val client = MinioClient.builder()
            .endpoint(minioUrl)
            .credentials(accessKey, secretKey)
            .build()
        
        try {
            val exists = client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build())
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build())
            }
        } catch (e: Exception) {
            println("MinIO connection failed: ${e.message}. Ensure MinIO is running.")
        }
        client
    }

    fun uploadFile(objectName: String, inputStream: InputStream, size: Long, contentType: String): String {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .`object`(objectName)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        )
        return objectName
    }

    fun getFileStream(objectName: String): InputStream {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(BUCKET_NAME)
                .`object`(objectName)
                .build()
        )
    }
}
