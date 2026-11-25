package com.sierracode.norte.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

enum class DocumentType {
    LICENCIA, PASAPORTE, SEGURO_MEDICO, COLEGIATURA, TARJETA, CONTRATO, OTRO
}

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerName: String,
    val category: String,
    val name: String,
    val documentType: DocumentType,
    val issueDateEpochDay: Long?,
    val expiryDateEpochDay: Long? = null,
    val reminderLeadDays: Int = 60,
    val fileUri: String? = null,
    val notes: String? = null
)

enum class DocumentStatus { VIGENTE, POR_VENCER, VENCIDO }

/** Estado calculado, no se persiste. */
fun Document.computedStatus(today: LocalDate = LocalDate.now()): DocumentStatus {
    val exp = expiryDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: return DocumentStatus.VIGENTE
    return when {
        today.isBefore(exp.minusDays(reminderLeadDays.toLong())) -> DocumentStatus.VIGENTE
        today.isBefore(exp) -> DocumentStatus.POR_VENCER
        else -> DocumentStatus.VENCIDO
    }
}