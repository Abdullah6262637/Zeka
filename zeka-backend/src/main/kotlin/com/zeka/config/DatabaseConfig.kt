package com.zeka.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zeka.data.db.tables.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    private var dataSource: HikariDataSource? = null

    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/zekadb"
            username = System.getenv("DB_USER") ?: "zeka"
            password = System.getenv("DB_PASSWORD") ?: "zeka_password"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        
        dataSource = HikariDataSource(config)
        Database.connect(dataSource!!)

        // Auto-create/migrate tables
        transaction {
            SchemaUtils.create(
                Users,
                ProviderKeys,
                Conversations,
                Messages,
                Attachments,
                Personas
            )
        }
    }

    fun close() {
        dataSource?.close()
    }
}
