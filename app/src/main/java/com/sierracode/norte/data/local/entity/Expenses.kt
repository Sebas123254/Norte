package com.sierracode.norte.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { INGRESO, GASTO }

enum class ExpenseCategory {
    COMIDA, VIVIENDA, TRANSPORTE, ROPA,
    ENTRETENIMIENTO, SALUD, EDUCACION, VIAJES, OTROS
}

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["category"])
    ]
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: TransactionType = TransactionType.GASTO,
    val amountCents: Long,
    val currency: String = "MXN",
    val category: ExpenseCategory,
    val subcategory: String? = null,
    val dateEpochDay: Long,
    val merchantName: String? = null,
    val notes: String? = null
)
