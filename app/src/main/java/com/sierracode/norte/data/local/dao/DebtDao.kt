package com.sierracode.norte.data.local.dao

import androidx.room.*
import com.sierracode.norte.data.local.entity.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE status = 'ACTIVA' ORDER BY amountCents DESC")
    fun observeActive(): Flow<List<Debt>>

    @Query("SELECT * FROM debts ORDER BY CASE WHEN status = 'ACTIVA' THEN 0 ELSE 1 END, amountCents DESC")
    fun observeAll(): Flow<List<Debt>>

    @Query("SELECT SUM(amountCents) FROM debts WHERE status = 'ACTIVA'")
    fun observeTotalActive(): Flow<Long?>

    @Query("SELECT * FROM debts")
    suspend fun getAll(): List<Debt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(debts: List<Debt>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(d: Debt)

    @Delete
    suspend fun delete(d: Debt)
}