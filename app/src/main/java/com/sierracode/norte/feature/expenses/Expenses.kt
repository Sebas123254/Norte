package com.sierracode.norte.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.Expense
import com.sierracode.norte.data.local.entity.ExpenseCategory
import com.sierracode.norte.data.local.entity.TransactionType
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    nav: NavController,
    vm: ExpensesViewModel = hiltViewModel()
) {
    val items by vm.filtered.collectAsState()
    val query by vm.query.collectAsState()
    val monthFilter by vm.monthFilter.collectAsState()
    val categoryFilter by vm.categoryFilter.collectAsState()
    val totals by vm.totals.collectAsState()
    val baseCurrency by vm.baseCurrency.collectAsState()
    val pendingUndo by vm.pendingUndo.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Mostrar snackbar con "Deshacer"
    LaunchedEffect(pendingUndo) {
        val pending = pendingUndo ?: return@LaunchedEffect
        val result = snackbarHost.showSnackbar(
            message = "Movimiento eliminado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
        else vm.consumeUndo()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Gastos") },
                actions = {
                    Text(
                        text = formatMoney(totals.balance, baseCurrency),
                        color = if (totals.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("expenses/add") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo") }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- Búsqueda ---
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Buscar por texto o monto…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- Filtro mes ---
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(monthFilter is MonthFilter.All, "Todos") { vm.setMonth(MonthFilter.All) }
                FilterChip(monthFilter is MonthFilter.ThisMonth, "Este mes") { vm.setMonth(MonthFilter.ThisMonth) }
                FilterChip(monthFilter is MonthFilter.LastMonth, "Mes anterior") { vm.setMonth(MonthFilter.LastMonth) }
            }

            // --- Filtro categoría ---
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(categoryFilter == null, "Todas") { vm.setCategory(null) }
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        label = cat.label(),
                        icon = {
                            Icon(
                                cat.icon(), contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) { vm.setCategory(if (categoryFilter == cat) null else cat) }
                }
            }

            // --- Totales ---
            TotalsCard(totals, baseCurrency)

            // --- Lista ---
            if (items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Sin movimientos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { exp ->
                        ExpenseRow(
                            expense = exp,
                            currency = baseCurrency,
                            onClick = { nav.navigate("expenses/edit/${exp.id}") },
                            onDelete = { vm.deleteWithUndo(exp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    label: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, leadingIcon = icon)
}

@Composable
private fun TotalsCard(totals: FilterTotals, currency: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Total del filtro", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Gastos", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatMoney(totals.expenses, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFC62828)
                    )
                }
                Column {
                    Text("Ingresos", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatMoney(totals.incomes, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2E7D32)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Balance", style = MaterialTheme.typography.labelSmall)
                    Text(
                        formatMoney(totals.balance, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totals.balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: Expense,
    currency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isIncome = expense.type == TransactionType.INGRESO

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(expense.category.color().copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(expense.category.icon(), null, tint = expense.category.color())
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    expense.merchant?.takeIf { it.isNotBlank() } ?: expense.displayCategory,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${expense.displayCategory} · ${LocalDate.ofEpochDay(expense.dateEpochDay)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                (if (isIncome) "+" else "-") + formatMoney(expense.amountCents, currency),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = { menuOpen = false; onClick() },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = { menuOpen = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, null) }
                    )
                }
            }
        }
    }
}