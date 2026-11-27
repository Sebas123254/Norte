package com.sierracode.norte.data.local.repository

import com.sierracode.norte.data.local.dao.ExpenseDao
import com.sierracode.norte.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(private val dao: ExpenseDao) {

    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    suspend fun getById(id: String): Expense? = dao.getById(id)

    suspend fun upsert(e: Expense) = dao.upsert(e)

    suspend fun upsertAll(list: List<Expense>) = dao.upsertAll(list)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun findDuplicates(
        amountCents: Long,
        categoryName: String,
        dateEpochDay: Long,
        excludeId: String
    ): List<Expense> = dao.findDuplicates(amountCents, categoryName, dateEpochDay, excludeId)

    /** ✅ Nuevos — los consume Dashboard */
    fun observeTotalExpenses(start: Long, end: Long): Flow<Long> =
        dao.observeTotalExpenses(start, end)

    fun observeTotalIncome(start: Long, end: Long): Flow<Long> =
        dao.observeTotalIncome(start, end)
}