package com.sierracode.norte.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.*
import com.sierracode.norte.data.settings.SettingsStore
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
    debtRepo: DebtRepository,
    settingsStore: SettingsStore
) : ViewModel() {
    private val today = LocalDate.now()
    private val startOfMonth = today.withDayOfMonth(1).toEpochDay()
    private val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).toEpochDay()

    val baseCurrency = settingsStore.settings.map { it.baseCurrency }
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
    val currency by vm.baseCurrency.collectAsState("MXN")
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
        Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.dashboard_summary_month), style = MaterialTheme.typography.titleMedium)

        SummaryCard(
            title = stringResource(R.string.dashboard_card_documents),
            value = stringResource(R.string.dashboard_card_documents_format, docs, expiring)
        )
        SummaryCard(stringResource(R.string.dashboard_card_spent_month), money(spent, currency))
        SummaryCard(stringResource(R.string.dashboard_card_income_month), money(income, currency))
        SummaryCard(stringResource(R.string.dashboard_card_balance), money(income - spent, currency))
        SummaryCard(stringResource(R.string.dashboard_card_active_goals), "$goals")
        SummaryCard(stringResource(R.string.dashboard_card_total_debt), money(debt, currency))

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { nav.navigate("settings") }) {
            Text(stringResource(R.string.dashboard_settings_button))
        }
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