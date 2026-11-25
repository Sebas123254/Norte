package com.sierracode.norte.feature.debts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.Debt
import com.sierracode.norte.data.local.entity.DebtStatus
import com.sierracode.norte.data.local.entity.DebtStrategy
import com.sierracode.norte.data.local.repository.DebtRepository
import com.sierracode.norte.util.money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebtFilter { ACTIVAS, TODAS, PAGADAS }

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repo: DebtRepository
) : ViewModel() {

    val selectedFilter = MutableStateFlow(DebtFilter.ACTIVAS)

    val totalActive = repo.observeTotalActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val debts = combine(repo.observeAll(), selectedFilter) { list, filter ->
        when (filter) {
            DebtFilter.ACTIVAS -> list.filter { it.status == DebtStatus.ACTIVA }
            DebtFilter.PAGADAS -> list.filter { it.status == DebtStatus.PAGADA }
            DebtFilter.TODAS -> list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: DebtFilter) {
        selectedFilter.value = filter
    }

    fun addDebt(
        creditorName: String,
        description: String,
        amountCents: Long,
        interestRatePercent: Float,
        minimumPaymentCents: Long,
        strategy: DebtStrategy
    ) = viewModelScope.launch {
        repo.upsert(
            Debt(
                creditorName = creditorName,
                description = description,
                amountCents = amountCents,
                interestRatePercent = interestRatePercent,
                minimumPaymentCents = minimumPaymentCents,
                strategy = strategy,
                status = DebtStatus.ACTIVA
            )
        )
    }

    fun makePayment(debt: Debt, paymentCents: Long) = viewModelScope.launch {
        repo.makePayment(debt, paymentCents)
    }

    fun delete(debt: Debt) = viewModelScope.launch {
        repo.delete(debt)
    }
}

@Composable
fun DebtsScreen(
    nav: NavController,
    vm: DebtsViewModel = hiltViewModel()
) {
    val items by vm.debts.collectAsState()
    val totalActiveCents by vm.totalActive.collectAsState()
    val currentFilter by vm.selectedFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var deletingDebt by remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nueva Deuda") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tarjeta de Resumen
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Total en Deudas Activas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            money(totalActiveCents ?: 0L),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Reduce intereses liquidando con Avalancha o liquida deudas pequeñas con Bola de Nieve.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Chips de Filtro
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DebtFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { vm.setFilter(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        DebtFilter.ACTIVAS -> "Activas"
                                        DebtFilter.PAGADAS -> "Pagadas"
                                        DebtFilter.TODAS -> "Todas"
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Estado Vacío
            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (currentFilter == DebtFilter.PAGADAS) "No hay deudas pagadas aún"
                                else "¡Sin deudas en esta sección! 🎉",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Usa el botón de abajo para registrar una deuda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(items, key = { it.id }) { debt ->
                    DebtItemCard(
                        debt = debt,
                        onPaymentClick = { payingDebt = debt },
                        onDeleteClick = { deletingDebt = debt }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { creditor, desc, amount, interest, minPayment, strat ->
                vm.addDebt(creditor, desc, amount, interest, minPayment, strat)
                showAddDialog = false
            }
        )
    }

    payingDebt?.let { debt ->
        MakePaymentDialog(
            debt = debt,
            onDismiss = { payingDebt = null },
            onConfirm = { paymentCents ->
                vm.makePayment(debt, paymentCents)
                payingDebt = null
            }
        )
    }

    deletingDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { deletingDebt = null },
            title = { Text("¿Eliminar deuda?") },
            text = { Text("Se eliminará el registro de la deuda con \"${debt.creditorName}\". Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(debt)
                    deletingDebt = null
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDebt = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DebtItemCard(
    debt: Debt,
    onPaymentClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPaid = debt.status == DebtStatus.PAGADA

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        debt.creditorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (debt.description.isNotBlank()) {
                        Text(
                            debt.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Badge de Estado
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isPaid) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = if (isPaid) "PAGADA" else "ACTIVA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("Monto pendiente:", style = MaterialTheme.typography.labelSmall)
                    Text(
                        money(debt.amountCents),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Interés: ${debt.interestRatePercent}%", style = MaterialTheme.typography.bodySmall)
                    if (debt.minimumPaymentCents > 0L) {
                        Text("Mínimo: ${money(debt.minimumPaymentCents)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            if (debt.strategy == DebtStrategy.SNOWBALL) "Bola de Nieve" else "Avalancha",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                Row {
                    if (!isPaid) {
                        Button(
                            onClick = onPaymentClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Abonar")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDebtDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        creditor: String,
        desc: String,
        amountCents: Long,
        interestPercent: Float,
        minimumPaymentCents: Long,
        strategy: DebtStrategy
    ) -> Unit
) {
    var creditor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("") }
    var minPaymentText by remember { mutableStateOf("") }
    var selectedStrategy by remember { mutableStateOf(DebtStrategy.SNOWBALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Deuda") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text("Acreedor / Banco *") },
                    placeholder = { Text("Ej. BBVA, Liverpool") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Concepto") },
                    placeholder = { Text("Ej. Tarjeta de crédito, Préstamo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto total (MXN) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = { Text("Interés % anual") },
                        placeholder = { Text("30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minPaymentText,
                        onValueChange = { minPaymentText = it },
                        label = { Text("Pago mín. (MXN)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text("Estrategia de pago:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStrategy == DebtStrategy.SNOWBALL,
                        onClick = { selectedStrategy = DebtStrategy.SNOWBALL },
                        label = { Text("Bola de nieve") }
                    )
                    FilterChip(
                        selected = selectedStrategy == DebtStrategy.AVALANCHE,
                        onClick = { selectedStrategy = DebtStrategy.AVALANCHE },
                        label = { Text("Avalancha") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = (amountText.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    val interest = interestText.toFloatOrNull() ?: 0f
                    val minPayment = (minPaymentText.toDoubleOrNull() ?: 0.0).times(100).toLong()

                    if (creditor.isNotBlank() && amount > 0L) {
                        onConfirm(
                            creditor.trim(),
                            description.trim(),
                            amount,
                            interest,
                            minPayment,
                            selectedStrategy
                        )
                    }
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun MakePaymentDialog(
    debt: Debt,
    onDismiss: () -> Unit,
    onConfirm: (paymentCents: Long) -> Unit
) {
    var paymentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a ${debt.creditorName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Deuda actual: ${money(debt.amountCents)}", style = MaterialTheme.typography.titleSmall)

                OutlinedTextField(
                    value = paymentText,
                    onValueChange = { paymentText = it },
                    label = { Text("Monto a abonar (MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (debt.minimumPaymentCents in 1 until debt.amountCents) {
                        OutlinedButton(
                            onClick = { paymentText = (debt.minimumPaymentCents / 100.0).toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("Pago mín.", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = { paymentText = (debt.amountCents / 100.0).toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text("Liquidar todo", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = (paymentText.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    if (cents > 0L) {
                        onConfirm(cents)
                    }
                }
            ) {
                Text("Registrar Abono")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
