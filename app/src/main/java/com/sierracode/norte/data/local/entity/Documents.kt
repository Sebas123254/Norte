package com.sierracode.norte.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class DocumentType { OFICIAL, PERSONAL }

enum class DocumentStatus { PENDIENTE, ACTIVO, PROXIMO_VENCIMIENTO, EXPIRADO, PERDIDO }

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerName: String,
    val category: String,
    val name: String,
    val documentType: DocumentType,
    val issueDateEpochDay: Long?,
    val expiryDateEpochDay: Long,
    val reminderLeadDays: Int = 60,
    val fileUri: String? = null,
    val notes: String? = null,
    val status: DocumentStatus = DocumentStatus.ACTIVO
)