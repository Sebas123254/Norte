package com.sierracode.norte.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Document
import com.sierracode.norte.data.local.entity.DocumentStatus
import com.sierracode.norte.data.local.entity.computedStatus

@Composable
fun DocumentCard(doc: Document, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(doc.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.documents_card_category_owner, doc.category, doc.ownerName),
                style = MaterialTheme.typography.bodySmall
            )
            val statusText = when (doc.computedStatus()) {
                DocumentStatus.VIGENTE -> "VIGENTE"
                DocumentStatus.POR_VENCER -> "POR VENCER"
                DocumentStatus.VENCIDO -> "VENCIDO"
            }
            Text(statusText, style = MaterialTheme.typography.labelSmall)
        }
    }
}