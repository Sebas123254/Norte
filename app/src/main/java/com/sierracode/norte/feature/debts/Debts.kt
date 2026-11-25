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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Debt
import com.sierracode.norte.data.local.entity.DebtStatus
import com.sierracode.norte.data.local.entity.DebtStrategy
import com.sierracode.norte.data.local.repository.DebtRepository
import com.sierracode.norte.data.settings.SettingsStore
import com.sierracode.norte.util.money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebtFilter { ACTIVAS, TODAS, PAGADAS }

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val repo: DebtRepository,
    settingsStore: SettingsStore
) : ViewModel() {

    val baseCurrency = settingsStore.settings.map { it.baseCurrency }

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
    val currency by vm.baseCurrency.collectAsState("MXN")

    var showAddDialog by remember { mutableStateOf(false) }
    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var deletingDebt by remember { mutableStateOf<Debt?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.debts_new_action)) }
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
                            stringResource(R.string.debts_total_active_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            money(totalActiveCents ?: 0L, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.debts_summary_subtitle),
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
                                        DebtFilter.ACTIVAS -> stringResource(R.string.debts_filter_active)
                                        DebtFilter.PAGADAS -> stringResource(R.string.debts_filter_paid)
                                        DebtFilter.TODAS -> stringResource(R.string.debts_filter_all)
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
                                if (currentFilter == DebtFilter.PAGADAS) stringResource(R.string.debts_empty_paid)
                                else stringResource(R.string.debts_empty_section),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.debts_empty_hint),
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
                        currency = currency,
                        onPaymentClick = { payingDebt = debt },
                        onDeleteClick = { deletingDebt = debt }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDebtDialog(
            currency = currency,
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
            currency = currency,
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
            title = { Text(stringResource(R.string.debts_delete_dialog_title)) },
            text = { Text(stringResource(R.string.debts_delete_dialog_message, debt.creditorName)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(debt)
                    deletingDebt = null
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDebt = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun DebtItemCard(
    debt: Debt,
    currency: String,
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
                        text = if (isPaid) stringResource(R.string.debts_status_paid) else stringResource(R.string.debts_status_active),
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
                    Text(stringResource(R.string.debts_pending_amount_label), style = MaterialTheme.typography.labelSmall)
                    Text(
                        money(debt.amountCents, currency),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.debts_interest_format, debt.interestRatePercent.toString()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (debt.minimumPaymentCents > 0L) {
                        Text(
                            stringResource(R.string.debts_min_payment_format, money(debt.minimumPaymentCents, currency)),
                            style = MaterialTheme.typography.bodySmall
                        )
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
                            if (debt.strategy == DebtStrategy.SNOWBALL) stringResource(R.string.debts_strategy_snowball)
                            else stringResource(R.string.debts_strategy_avalanche),
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
                            Text(stringResource(R.string.debts_action_pay))
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
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
    currency: String,
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
        title = { Text(stringResource(R.string.debts_dialog_add_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = creditor,
                    onValueChange = { creditor = it },
                    label = { Text(stringResource(R.string.debts_field_creditor_label)) },
                    placeholder = { Text(stringResource(R.string.debts_field_creditor_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.debts_field_concept_label)) },
                    placeholder = { Text(stringResource(R.string.debts_field_concept_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${stringResource(R.string.debts_field_amount_label)} ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = { Text(stringResource(R.string.debts_field_interest_label)) },
                        placeholder = { Text(stringResource(R.string.debts_field_interest_placeholder)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minPaymentText,
                        onValueChange = { minPaymentText = it },
                        label = { Text("${stringResource(R.string.debts_field_min_payment_label)} ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.debts_payment_strategy_label), style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedStrategy == DebtStrategy.SNOWBALL,
                        onClick = { selectedStrategy = DebtStrategy.SNOWBALL },
                        label = { Text(stringResource(R.string.debts_strategy_snowball)) }
                    )
                    FilterChip(
                        selected = selectedStrategy == DebtStrategy.AVALANCHE,
                        onClick = { selectedStrategy = DebtStrategy.AVALANCHE },
                        label = { Text(stringResource(R.string.debts_strategy_avalanche)) }
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
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

@Composable
private fun MakePaymentDialog(
    debt: Debt,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (paymentCents: Long) -> Unit
) {
    var paymentText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debts_dialog_payment_title, debt.creditorName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.debts_current_debt_format, money(debt.amountCents, currency)),
                    style = MaterialTheme.typography.titleSmall
                )

                OutlinedTextField(
                    value = paymentText,
                    onValueChange = { paymentText = it },
                    label = { Text("${stringResource(R.string.debts_field_payment_amount_label)} ($currency)") },
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
                            Text(stringResource(R.string.debts_btn_min_payment), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = { paymentText = (debt.amountCents / 100.0).toString() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(stringResource(R.string.debts_btn_pay_all), style = MaterialTheme.typography.labelSmall)
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
                Text(stringResource(R.string.debts_btn_confirm_payment))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
