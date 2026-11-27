package com.sierracode.norte.feature.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.repository.ExpenseRepository
import com.sierracode.norte.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddEditExpenseViewModel @Inject constructor(
    private val repo: ExpenseRepository,
    settingsStore: SettingsStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingId: String? = savedStateHandle["expenseId"]
    val isEditing: Boolean = editingId != null

    // ---- Form state (públicos para leer, solo se mutan desde los on*) ----
    var amountText by mutableStateOf(""); private set
    var type by mutableStateOf(TransactionType.GASTO); private set
    var category by mutableStateOf(ExpenseCategory.COMIDA); private set
    var customCategoryName by mutableStateOf(""); private set
    var date by mutableStateOf(LocalDate.now()); private set
    var merchant by mutableStateOf(""); private set
    var paymentMethod by mutableStateOf(PaymentMethod.EFECTIVO); private set
    var notes by mutableStateOf(""); private set
    var photoUri by mutableStateOf<String?>(null); private set
    var showAdvanced by mutableStateOf(false); private set

    // ---- Feedback ----
    var error by mutableStateOf<String?>(null); private set
    var duplicateWarning by mutableStateOf<List<Expense>?>(null); private set
    var savedEvent by mutableStateOf(false); private set

    val baseCurrency = settingsStore.settings
        .map { it.baseCurrency }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "MXN")

    init {
        if (editingId != null) {
            viewModelScope.launch {
                repo.getById(editingId)?.let { e ->
                    amountText = formatMoneyRaw(e.amountCents)
                    type = e.type
                    category = e.category
                    customCategoryName = e.customCategoryName.orEmpty()
                    date = LocalDate.ofEpochDay(e.dateEpochDay)
                    merchant = e.merchant.orEmpty()
                    paymentMethod = e.paymentMethod
                    notes = e.notes.orEmpty()
                    photoUri = e.photoUri
                }
            }
        }
    }

    // ---- Setters públicos (renombrados para no chocar con los setters JVM) ----
    fun onAmountChange(raw: String) {
        val cleaned = raw.filter { it.isDigit() || it == '.' }
        if (cleaned.count { it == '.' } <= 1) { amountText = cleaned; error = null }
    }

    fun onTypeChange(t: TransactionType) { type = t }

    fun onCategoryChange(c: ExpenseCategory) {
        category = c
        if (c != ExpenseCategory.OTROS) customCategoryName = ""
        error = null
    }

    fun onCustomCategoryNameChange(s: String) { customCategoryName = s; error = null }
    fun onDateChange(d: LocalDate) { date = d }
    fun onMerchantChange(s: String) { merchant = s }
    fun onPaymentMethodChange(p: PaymentMethod) { paymentMethod = p }
    fun onNotesChange(s: String) { notes = s }
    fun onPhotoUriChange(uri: String?) { photoUri = uri }

    fun toggleAdvanced() { showAdvanced = !showAdvanced }
    fun consumeSavedEvent() { savedEvent = false }
    fun dismissDuplicate() { duplicateWarning = null }

    // ---- Save flow ----
    fun attemptSave(skipDuplicateCheck: Boolean = false) {
        val cents = parseCents(amountText)
        val validation = validate(cents)
        if (validation != null) { error = validation; return }

        viewModelScope.launch {
            if (!skipDuplicateCheck) {
                val dups = repo.findDuplicates(
                    amountCents = cents!!,
                    categoryName = category.name,
                    dateEpochDay = date.toEpochDay(),
                    excludeId = editingId.orEmpty()
                )
                if (dups.isNotEmpty()) { duplicateWarning = dups; return@launch }
            }
            persist(cents!!)
        }
    }

    fun confirmSaveDespiteDuplicates() {
        duplicateWarning = null
        attemptSave(skipDuplicateCheck = true)
    }

    private suspend fun persist(cents: Long) {
        val expense = Expense(
            id = editingId ?: UUID.randomUUID().toString(),
            amountCents = cents,
            currency = baseCurrency.value,
            category = category,
            customCategoryName = if (category == ExpenseCategory.OTROS)
                customCategoryName.trim().ifBlank { null } else null,
            dateEpochDay = date.toEpochDay(),
            merchant = merchant.trim().ifBlank { null },
            paymentMethod = paymentMethod,
            notes = notes.trim().ifBlank { null },
            photoUri = photoUri,
            type = type
        )
        repo.upsert(expense)
        savedEvent = true
    }

    private fun validate(cents: Long?): String? = when {
        cents == null || cents <= 0L -> "Ingresa un monto mayor a 0"
        cents > MAX_AMOUNT_CENTS ->
            "El monto excede el máximo permitido (${formatMoney(MAX_AMOUNT_CENTS)})"
        category == ExpenseCategory.OTROS && customCategoryName.isBlank() ->
            "Escribe el nombre de la categoría"
        else -> null
    }

    private fun parseCents(text: String): Long? {
        val d = text.toDoubleOrNull() ?: return null
        if (d <= 0.0) return null
        return (d * 100).toLong()
    }

    companion object {
        const val MAX_AMOUNT_CENTS = 100_000_000L
    }
}