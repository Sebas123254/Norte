package com.sierracode.norte.data

import androidx.room.withTransaction
import com.sierracode.norte.data.local.AppDatabase
import com.sierracode.norte.data.settings.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class CurrencyConverter @Inject constructor(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore
) {
    private fun convert(cents: Long, rate: Double): Long = (cents * rate).roundToLong()

    suspend fun convertBaseCurrency(newCurrency: String, rate: Double) {
        db.withTransaction {
            val expenses = db.expenseDao().getAll().map {
                it.copy(amountCents = convert(it.amountCents, rate), currency = newCurrency)
            }
            db.expenseDao().upsertAll(expenses)

            val goals = db.goalDao().getAll().map {
                it.copy(
                    targetAmountCents = convert(it.targetAmountCents, rate),
                    currentAmountCents = convert(it.currentAmountCents, rate)
                )
            }
            db.goalDao().upsertAll(goals)

            val debts = db.debtDao().getAll().map {
                it.copy(
                    amountCents = convert(it.amountCents, rate),
                    minimumPaymentCents = convert(it.minimumPaymentCents, rate)
                )
            }
            db.debtDao().upsertAll(debts)
        }
        settingsStore.setBaseCurrency(newCurrency)
    }
}