package com.sierracode.norte.data.local.dao

import androidx.room.*
import com.sierracode.norte.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<Expense>>

    @Query("SELECT SUM(amountCents) FROM expenses WHERE type = 'GASTO' AND dateEpochDay BETWEEN :from AND :to")
    fun observeTotalExpenses(from: Long, to: Long): Flow<Long?>

    @Query("SELECT SUM(amountCents) FROM expenses WHERE type = 'INGRESO' AND dateEpochDay BETWEEN :from AND :to")
    fun observeTotalIncome(from: Long, to: Long): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: Expense)

    @Delete
    suspend fun delete(e: Expense)
}