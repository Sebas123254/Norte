package com.sierracode.norte.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sierracode.norte.data.local.entity.Expense
import com.sierracode.norte.data.local.entity.ExpenseCategory
import com.sierracode.norte.data.local.entity.TransactionType
import com.sierracode.norte.data.local.repository.ExpenseRepository
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

sealed interface MonthFilter {
    data object All : MonthFilter
    data object ThisMonth : MonthFilter
    data object LastMonth : MonthFilter
}

data class FilterTotals(val expenses: Long, val incomes: Long) {
    val balance: Long get() = incomes - expenses
}

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repo: ExpenseRepository,
    settingsStore: SettingsStore
) : ViewModel() {

    // ---- Estado de filtros ----
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _monthFilter = MutableStateFlow<MonthFilter>(MonthFilter.All)
    val monthFilter: StateFlow<MonthFilter> = _monthFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    val categoryFilter: StateFlow<ExpenseCategory?> = _categoryFilter.asStateFlow()

    val baseCurrency = settingsStore.settings
        .map { it.baseCurrency }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "MXN")

    // ---- Lista filtrada ----
    val filtered: StateFlow<List<Expense>> = combine(
        repo.observeAll(), _query, _monthFilter, _categoryFilter
    ) { all, q, month, cat ->
        all.filter { e ->
            matchesQuery(e, q) && matchesMonth(e, month) && (cat == null || e.category == cat)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---- Totales del filtro ----
    val totals: StateFlow<FilterTotals> = filtered
        .map { list ->
            var gasto = 0L; var ingreso = 0L
            list.forEach {
                if (it.type == TransactionType.GASTO) gasto += it.amountCents
                else ingreso += it.amountCents
            }
            FilterTotals(gasto, ingreso)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FilterTotals(0, 0))

    // ---- Undo ----
    private val _pendingUndo = MutableStateFlow<Expense?>(null)
    val pendingUndo: StateFlow<Expense?> = _pendingUndo.asStateFlow()

    fun deleteWithUndo(expense: Expense) = viewModelScope.launch {
        repo.deleteById(expense.id)
        _pendingUndo.value = expense
    }

    fun undoDelete() = viewModelScope.launch {
        _pendingUndo.value?.let { repo.upsert(it) }
        _pendingUndo.value = null
    }

    fun consumeUndo() { _pendingUndo.value = null }

    // ---- Setters ----
    fun setQuery(q: String) { _query.value = q }
    fun setMonth(m: MonthFilter) { _monthFilter.value = m }
    fun setCategory(c: ExpenseCategory?) { _categoryFilter.value = c }

    // ---- Helpers ----
    private fun matchesQuery(e: Expense, raw: String): Boolean {
        val q = raw.trim()
        if (q.isEmpty()) return true
        val haystack = buildString {
            append(e.merchant ?: "")
            append(' ').append(e.notes ?: "")
            append(' ').append(e.displayCategory)
            append(' ').append(e.paymentMethod.name)
        }
        if (haystack.contains(q, ignoreCase = true)) return true
        // Búsqueda por monto
        if (formatMoneyRaw(e.amountCents).contains(q)) return true
        if (e.amountCents.toString().contains(q)) return true
        return false
    }

    private fun matchesMonth(e: Expense, filter: MonthFilter): Boolean {
        val date = LocalDate.ofEpochDay(e.dateEpochDay)
        val ym = YearMonth.from(date)
        return when (filter) {
            MonthFilter.All       -> true
            MonthFilter.ThisMonth -> ym == YearMonth.now()
            MonthFilter.LastMonth -> ym == YearMonth.now().minusMonths(1)
        }
    }
}