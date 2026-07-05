package com.zeka.sandbox

import com.zeka.data.db.tables.UserQuotas
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

object QuotaManager {

    fun initQuotaForUser(userId: UUID) {
        transaction {
            val exists = UserQuotas.selectAll().where { UserQuotas.userId eq userId }.any()
            if (!exists) {
                UserQuotas.insert {
                    it[UserQuotas.userId] = userId
                    it[UserQuotas.tokenLimit] = 50000
                    it[UserQuotas.tokenUsed] = 0
                    it[UserQuotas.cpuLimitSeconds] = 3600
                    it[UserQuotas.cpuUsedSeconds] = 0
                    it[UserQuotas.updatedAt] = LocalDateTime.now()
                }
            }
        }
    }

    fun checkQuota(userId: UUID): Boolean {
        initQuotaForUser(userId)
        return transaction {
            val row = UserQuotas.selectAll().where { UserQuotas.userId eq userId }.firstOrNull() ?: return@transaction false
            val tokenLimit = row[UserQuotas.tokenLimit]
            val tokenUsed = row[UserQuotas.tokenUsed]
            val cpuLimit = row[UserQuotas.cpuLimitSeconds]
            val cpuUsed = row[UserQuotas.cpuUsedSeconds]

            (tokenUsed < tokenLimit) && (cpuUsed < cpuLimit)
        }
    }

    fun consumeTokens(userId: UUID, tokens: Int) {
        transaction {
            val current = UserQuotas.selectAll().where { UserQuotas.userId eq userId }.firstOrNull() ?: return@transaction
            val newUsed = current[UserQuotas.tokenUsed] + tokens
            UserQuotas.update({ UserQuotas.userId eq userId }) {
                it[UserQuotas.tokenUsed] = newUsed
                it[UserQuotas.updatedAt] = LocalDateTime.now()
            }
        }
    }

    fun consumeCpu(userId: UUID, seconds: Int) {
        transaction {
            val current = UserQuotas.selectAll().where { UserQuotas.userId eq userId }.firstOrNull() ?: return@transaction
            val newUsed = current[UserQuotas.cpuUsedSeconds] + seconds
            UserQuotas.update({ UserQuotas.userId eq userId }) {
                it[UserQuotas.cpuUsedSeconds] = newUsed
                it[UserQuotas.updatedAt] = LocalDateTime.now()
            }
        }
    }
}
