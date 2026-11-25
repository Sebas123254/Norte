package com.sierracode.norte.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DebtStrategy { SNOWBALL, AVALANCHE }

enum class DebtStatus { ACTIVA, PAGADA, EN_MORA }

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val creditorName: String,
    val description: String,
    val amountCents: Long,
    val interestRatePercent: Float = 0f,
    val minimumPaymentCents: Long = 0L,
    val dueDateEpochDay: Long? = null,
    val strategy: DebtStrategy = DebtStrategy.SNOWBALL,
    val status: DebtStatus = DebtStatus.ACTIVA,
    val createdAt: Long = System.currentTimeMillis()
)