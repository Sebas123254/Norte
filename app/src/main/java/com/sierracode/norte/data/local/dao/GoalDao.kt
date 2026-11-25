package com.sierracode.norte.data.local.dao

import androidx.room.*
import com.sierracode.norte.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): Goal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(g: Goal)

    @Delete
    suspend fun delete(g: Goal)
}