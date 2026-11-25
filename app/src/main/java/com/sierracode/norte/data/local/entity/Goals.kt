package com.sierracode.norte.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class GoalStatus { PENDIENTE, EN_PROGRESO, COMPLETADO }

enum class GoalPriority { ALTA, MEDIA, BAJA }

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0,
    val priority: GoalPriority = GoalPriority.MEDIA,
    val targetDateEpochDay: Long? = null,
    val categoryId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: GoalStatus = GoalStatus.PENDIENTE
) {
    /** Propiedad derivada — no se persiste. */
    val progressPercent: Float
        get() = if (targetAmountCents <= 0L) 0f
        else (currentAmountCents.toFloat() / targetAmountCents).coerceIn(0f, 1f)
}