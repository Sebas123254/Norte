package com.sierracode.norte.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { GASTO, INGRESO }

enum class PaymentMethod { EFECTIVO, TARJETA_DEBITO, TARJETA_CREDITO, TRANSFERENCIA, OTRO }

enum class ExpenseCategory {
    COMIDA,
    TRANSPORTE,
    VIVIENDA,
    EDUCACION,
    ENTRETENIMIENTO,
    SALUD,
    ROPA,
    IMPUESTOS_FONDO_EMERGENCIA,
    OTROS
}

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amountCents: Long,
    val currency: String = "MXN",
    val category: ExpenseCategory,
    val customCategoryName: String? = null,
    val dateEpochDay: Long = 0L,
    val merchant: String? = null,
    @ColumnInfo(defaultValue = "EFECTIVO")
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val notes: String? = null,
    val photoUri: String? = null,
    @ColumnInfo(defaultValue = "GASTO")
    val type: TransactionType = TransactionType.GASTO,
    @ColumnInfo(defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayCategory: String
        get() = if (category == ExpenseCategory.OTROS && !customCategoryName.isNullOrBlank())
            customCategoryName.trim()
        else category.name
}