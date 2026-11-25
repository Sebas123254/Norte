package com.sierracode.norte.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sierracode.norte.data.local.dao.*
import com.sierracode.norte.data.local.entity.*
import com.sierracode.norte.data.local.Converters


@Database(
    entities = [
        Document::class, Expense::class, BudgetCategory::class,
        Goal::class, Debt::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
}