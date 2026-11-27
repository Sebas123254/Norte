package com.sierracode.norte.data.local.repository

import com.sierracode.norte.data.local.dao.DocumentDao
import com.sierracode.norte.data.local.entity.Document
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(private val dao: DocumentDao) {
    fun observeAll(): Flow<List<Document>> = dao.observeAll()
    suspend fun getById(id: String): Document? = dao.getById(id)
    suspend fun upsert(d: Document) = dao.upsert(d)
    suspend fun delete(d: Document) = dao.delete(d)
    suspend fun updateLastNotifiedHito(id: String, hito: Int) = dao.updateLastNotifiedHito(id, hito)
}
