package com.sierracode.norte.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sierracode.norte.data.local.entity.Document
import java.time.LocalDate

@Composable
fun DocumentCard(doc: Document, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(doc.name, style = MaterialTheme.typography.titleMedium)
            Text("${doc.category} · ${doc.ownerName}", style = MaterialTheme.typography.bodySmall)
            Text("Vence: ${LocalDate.ofEpochDay(doc.expiryDateEpochDay)}")
            Text("Estado: ${doc.status}", style = MaterialTheme.typography.labelSmall)
        }
    }
}