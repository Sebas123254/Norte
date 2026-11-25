package com.sierracode.norte.feature.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.repository.ExpenseRepository
import com.sierracode.norte.data.settings.SettingsStore
import com.sierracode.norte.ui.components.ExpenseRow
import com.sierracode.norte.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repo: ExpenseRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {
    val baseCurrency = settingsStore.settings.map { it.baseCurrency }

    val expenses = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(amountCents: Long, category: ExpenseCategory, currency: String) = viewModelScope.launch {
        repo.upsert(
            Expense(
                amountCents = amountCents,
                currency = currency,
                category = category,
                dateEpochDay = LocalDate.now().toEpochDay()
            )
        )
    }
}

@Composable
fun ExpensesScreen(nav: NavController, vm: ExpensesViewModel = hiltViewModel()) {
    val items by vm.expenses.collectAsState()
    val baseCurrency by vm.baseCurrency.collectAsState("MXN")
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                text = { Text(stringResource(R.string.action_add)) },
                icon = {}
            )
        }
    ) { p ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(p),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(items, key = { it.id }) { ExpenseRow(it) }
        }
    }

    if (showDialog) {
        AddExpenseDialog(
            currency = baseCurrency,
            onDismiss = { showDialog = false },
            onConfirm = { cents, cat ->
                vm.add(cents, cat, baseCurrency); showDialog = false
            }
        )
    }
}

@Composable
fun expenseCategoryLabel(category: ExpenseCategory): String {
    return when (category) {
        ExpenseCategory.COMIDA -> stringResource(R.string.expenses_category_comida)
        ExpenseCategory.VIVIENDA -> stringResource(R.string.expenses_category_vivienda)
        ExpenseCategory.TRANSPORTE -> stringResource(R.string.expenses_category_transporte)
        ExpenseCategory.ROPA -> stringResource(R.string.expenses_category_ropa)
        ExpenseCategory.ENTRETENIMIENTO -> stringResource(R.string.expenses_category_entretenimiento)
        ExpenseCategory.SALUD -> stringResource(R.string.expenses_category_salud)
        ExpenseCategory.EDUCACION -> stringResource(R.string.expenses_category_educacion)
        ExpenseCategory.VIAJES -> stringResource(R.string.expenses_category_viajes)
        ExpenseCategory.OTROS -> stringResource(R.string.expenses_category_otros)
    }
}

@Composable
private fun AddExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, ExpenseCategory) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(ExpenseCategory.COMIDA) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.expenses_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("${stringResource(R.string.expenses_amount_label)} ($currency)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.expenses_category_label, expenseCategoryLabel(cat)))
                Row {
                    ExpenseCategory.entries.take(4).forEach { c ->
                        TextButton(onClick = { cat = c }) { Text(expenseCategoryLabel(c)) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = (text.toDoubleOrNull() ?: 0.0).times(100).toLong()
                if (cents > 0) onConfirm(cents, cat)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}