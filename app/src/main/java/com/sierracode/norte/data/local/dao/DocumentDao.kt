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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(doc: Document)

    @Delete
    suspend fun delete(doc: Document)

    @Query("UPDATE documents SET lastNotifiedHito = :hito WHERE id = :id")
    suspend fun updateLastNotifiedHito(id: String, hito: Int)
}
