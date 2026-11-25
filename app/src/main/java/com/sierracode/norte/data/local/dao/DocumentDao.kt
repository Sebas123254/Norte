package com.sierracode.norte.data.local.dao

import androidx.room.*
import com.sierracode.norte.data.local.entity.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY expiryDateEpochDay ASC")
    fun observeAll(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: String): Document?

    @Query("SELECT * FROM documents WHERE status = :status")
    fun observeByStatus(status: String): Flow<List<Document>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: Document)

    @Delete
    suspend fun delete(doc: Document)
}