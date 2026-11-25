package com.sierracode.norte.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sierracode.norte.data.local.entity.Expense
import com.sierracode.norte.util.money

@Composable
fun ExpenseRow(e: Expense) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(e.category.name, style = MaterialTheme.typography.titleSmall)
                if (e.merchantName != null) {
                    Text(e.merchantName, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                (if (e.type.name == "INGRESO") "+" else "-") + money(e.amountCents),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}