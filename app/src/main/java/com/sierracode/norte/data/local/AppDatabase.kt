package com.sierracode.norte.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sierracode.norte.data.local.dao.*
import com.sierracode.norte.data.local.entity.*

@Database(
    entities = [
        Document::class, Expense::class,
        Goal::class, Debt::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Agregar índices en la tabla expenses
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_dateEpochDay` ON `expenses` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
                // Eliminar tabla no utilizada budget_categories si existía
                db.execSQL("DROP TABLE IF EXISTS `budget_categories`")
            }
        }
    }
}