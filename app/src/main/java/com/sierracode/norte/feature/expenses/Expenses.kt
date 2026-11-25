package com.sierracode.norte.feature.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.repository.ExpenseRepository
import com.sierracode.norte.ui.components.ExpenseRow
import com.sierracode.norte.data.local.entity.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repo: ExpenseRepository
) : ViewModel() {
    val expenses = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(amountCents: Long, category: ExpenseCategory) = viewModelScope.launch {
        repo.upsert(
            Expense(
                amountCents = amountCents,
                category = category,
                dateEpochDay = LocalDate.now().toEpochDay()
            )
        )
    }
}

@Composable
fun ExpensesScreen(nav: NavController, vm: ExpensesViewModel = hiltViewModel()) {
    val items by vm.expenses.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                text = { Text("Agregar") },
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
            onDismiss = { showDialog = false },
            onConfirm = { cents, cat ->
                vm.add(cents, cat); showDialog = false
            }
        )
    }
}

@Composable
private fun AddExpenseDialog(onDismiss: () -> Unit, onConfirm: (Long, ExpenseCategory) -> Unit) {
    var text by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(ExpenseCategory.COMIDA) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo gasto") },
        text = {
            Column {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Monto (MXN)") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Categoría: ${cat.name}")
                Row {
                    ExpenseCategory.values().take(4).forEach { c ->
                        TextButton(onClick = { cat = c }) { Text(c.name) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = (text.toDoubleOrNull() ?: 0.0).times(100).toLong()
                if (cents > 0) onConfirm(cents, cat)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}