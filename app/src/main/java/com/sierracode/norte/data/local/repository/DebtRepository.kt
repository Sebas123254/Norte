package com.sierracode.norte.data.local.repository

import com.sierracode.norte.data.local.dao.DebtDao
import com.sierracode.norte.data.local.entity.Debt
import com.sierracode.norte.data.local.entity.DebtStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebtRepository @Inject constructor(private val dao: DebtDao) {
    fun observeActive(): Flow<List<Debt>> = dao.observeActive()
    fun observeAll(): Flow<List<Debt>> = dao.observeAll()
    fun observeTotalActive(): Flow<Long?> = dao.observeTotalActive()
    suspend fun upsert(d: Debt) = dao.upsert(d)
    suspend fun delete(d: Debt) = dao.delete(d)

    suspend fun makePayment(debt: Debt, paymentCents: Long) {
        val remaining = (debt.amountCents - paymentCents).coerceAtLeast(0L)
        val updated = debt.copy(
            amountCents = remaining,
            status = if (remaining == 0L) DebtStatus.PAGADA else debt.status
        )
        dao.upsert(updated)
    }
}