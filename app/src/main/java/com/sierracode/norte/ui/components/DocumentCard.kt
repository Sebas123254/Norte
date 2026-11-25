package com.sierracode.norte.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Document
import java.time.LocalDate

@Composable
fun DocumentCard(doc: Document, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp).verticalAlignment(Alignment.Top)) {
            Text(doc.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.documents_card_category_owner, doc.category, doc.ownerName),
                style = MaterialTheme.typography.bodySmall
            )
            
            val today = LocalDate.now()
            val expiryDate = doc.expiryDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) } ?: java.time.LocalDate.MAX
            
            // Determinar status basado en fecha de vencimiento
            val status = if (doc.expiryDateEpochDay == null) {
                "VIGENTE" // No tiene fecha, se considera válido
            } else if (expiryDate.isAfter(today)) {
                "VIGENTE"
            } else if (today.isBefore(expiryDate.minusDays(doc.reminderLeadDays.toLong()))) {
                "POR_VENCER"
            } else {
                "EXPIRADO"
            }
            
            val statusText = when (status) {
                "VIGENTE" -> "VIGENTE"
                "POR_VENCER" -> "POR VENCER"
                "EXPIRADO" -> "EXPIRADO"
                else -> "VIGENTE"
            }
            
            Text(statusText, style = MaterialTheme.typography.labelSmall)
        }
    }
}
