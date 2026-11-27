package com.sierracode.norte.data.local.dao

import androidx.room.*
import com.sierracode.norte.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, createdAt DESC")
    fun observeAll(): Flow<List<Expense>>

    /** ✅ Nuevo — lo usa CurrencyConverter */
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, createdAt DESC")
    suspend fun getAll(): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("""
        SELECT * FROM expenses
        WHERE amountCents = :amountCents
          AND category = :categoryName
          AND dateEpochDay = :dateEpochDay
          AND id != :excludeId
    """)
    suspend fun findDuplicates(
        amountCents: Long,
        categoryName: String,
        dateEpochDay: Long,
        excludeId: String
    ): List<Expense>

    /** ✅ Nuevo — lo usa Dashboard (con rango de fechas) */
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM expenses
        WHERE type = 'GASTO' AND dateEpochDay BETWEEN :start AND :end
    """)
    fun observeTotalExpenses(start: Long, end: Long): Flow<Long>

    /** ✅ Nuevo — lo usa Dashboard (con rango de fechas) */
    @Query("""
        SELECT COALESCE(SUM(amountCents), 0) FROM expenses
        WHERE type = 'INGRESO' AND dateEpochDay BETWEEN :start AND :end
    """)
    fun observeTotalIncome(start: Long, end: Long): Flow<Long>
}