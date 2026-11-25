package com.sierracode.norte.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.*
import com.sierracode.norte.util.money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    docRepo: DocumentRepository,
    expenseRepo: ExpenseRepository,
    goalRepo: GoalRepository,
    debtRepo: DebtRepository
) : ViewModel() {
    private val today = LocalDate.now()
    private val startOfMonth = today.withDayOfMonth(1).toEpochDay()
    private val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    val docCount = docRepo.observeAll().map { it.size }
    val expiringSoon = docRepo.observeAll().map { docs ->
        docs.count { it.expiryDateEpochDay - today.toEpochDay() in 0..it.reminderLeadDays }
    }
    val totalSpentMonth = expenseRepo.observeTotalExpenses(startOfMonth, endOfMonth).map { it ?: 0L }
    val totalIncomeMonth = expenseRepo.observeTotalIncome(startOfMonth, endOfMonth).map { it ?: 0L }
    val goalsActive = goalRepo.observeAll().map { gs -> gs.count { it.status != GoalStatus.COMPLETADO } }
    val totalDebt = debtRepo.observeTotalActive().map { it ?: 0L }
}

@Composable
fun DashboardScreen(nav: NavController, vm: DashboardViewModel = hiltViewModel()) {
    val docs by vm.docCount.collectAsState(0)
    val expiring by vm.expiringSoon.collectAsState(0)
    val spent by vm.totalSpentMonth.collectAsState(0L)
    val income by vm.totalIncomeMonth.collectAsState(0L)
    val goals by vm.goalsActive.collectAsState(0)
    val debt by vm.totalDebt.collectAsState(0L)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Norte", style = MaterialTheme.typography.headlineLarge)
        Text("Resumen del mes", style = MaterialTheme.typography.titleMedium)

        SummaryCard("Documentos", "$docs registrados · $expiring por vencer")
        SummaryCard("Gastos del mes", money(spent))
        SummaryCard("Ingresos del mes", money(income))
        SummaryCard("Balance", money(income - spent))
        SummaryCard("Metas activas", "$goals")
        SummaryCard("Deuda total", money(debt))

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { nav.navigate("settings") }) { Text("Ajustes") }
    }
}

@Composable
private fun SummaryCard(title: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}