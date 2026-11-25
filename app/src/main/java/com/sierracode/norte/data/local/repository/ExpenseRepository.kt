package com.sierracode.norte.data.local.repository

import com.sierracode.norte.data.local.dao.ExpenseDao
import com.sierracode.norte.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(private val dao: ExpenseDao) {
    fun observeAll(): Flow<List<Expense>> = dao.observeAll()
    fun observeTotalExpenses(from: Long, to: Long): Flow<Long?> = dao.observeTotalExpenses(from, to)
    fun observeTotalIncome(from: Long, to: Long): Flow<Long?> = dao.observeTotalIncome(from, to)
    suspend fun upsert(e: Expense) = dao.upsert(e)
    suspend fun delete(e: Expense) = dao.delete(e)
}