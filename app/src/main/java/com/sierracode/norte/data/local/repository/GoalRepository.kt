package com.sierracode.norte.data.local.repository
import com.sierracode.norte.data.local.dao.GoalDao
import com.sierracode.norte.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(private val dao: GoalDao) {
    fun observeAll(): Flow<List<Goal>> = dao.observeAll()
    suspend fun getById(id: String): Goal? = dao.getById(id)
    suspend fun upsert(g: Goal) = dao.upsert(g)
    suspend fun delete(g: Goal) = dao.delete(g)
}