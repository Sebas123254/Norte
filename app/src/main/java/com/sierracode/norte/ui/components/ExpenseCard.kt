package com.sierracode.norte.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment                // ← AÑADIR
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp                 // ← AÑADIR
import com.sierracode.norte.data.local.entity.Expense
import com.sierracode.norte.feature.expenses.label
import com.sierracode.norte.feature.expenses.formatMoney

@Composable
fun ExpenseCard(expense: Expense, currency: String = "MXN") {
    Card {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(expense.merchant ?: expense.displayCategory)
                Text(expense.category.label())
            }
            Text(formatMoney(expense.amountCents, currency))
        }
    }
}