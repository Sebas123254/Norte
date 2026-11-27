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
    version = 4,                 // ← subimos de 3 a 4
    exportSchema = false
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
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_dateEpochDay` ON `expenses` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_category` ON `expenses` (`category`)")
                db.execSQL("DROP TABLE IF EXISTS `budget_categories`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN `lastNotifiedHito` INTEGER")
            }
        }

        // ⬇️ NUEVA: agrega las columnas que necesita el nuevo Expense
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ---------------------------------------------------------------
                // 1. Primero creamos las columnas nuevas (sin esto el UPDATE de
                //    abajo fallaría porque `customCategoryName` aún no existe).
                // ---------------------------------------------------------------
                db.execSQL("ALTER TABLE expenses ADD COLUMN `customCategoryName` TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `merchant` TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'EFECTIVO'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `notes` TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `photoUri` TEXT")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `type` TEXT NOT NULL DEFAULT 'GASTO'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0")

                // ---------------------------------------------------------------
                // 2. Migrar los VIAJES existentes → OTROS con nombre "Viajes".
                //    El enum se guarda como TEXT con el nombre (TypeConverter).
                //    Como VIAJES desaparece del enum, cualquier fila con
                //    category='VIAJES' rompería al leerla (valueOf lanzaría).
                // ---------------------------------------------------------------
                db.execSQL(
                    """
                    UPDATE expenses
                    SET category = 'OTROS',
                        customCategoryName = 'Viajes'
                    WHERE category = 'VIAJES'
                    """.trimIndent()
                )

                // ---------------------------------------------------------------
                // 3. Rellenar createdAt de filas viejas con algo razonable.
                //    Usamos la fecha del gasto si existe; si no, ahora.
                // ---------------------------------------------------------------
                db.execSQL(
                    """
                    UPDATE expenses
                    SET createdAt = CASE
                        WHEN dateEpochDay > 0 THEN dateEpochDay * 86400000
                        ELSE ${System.currentTimeMillis()}
                    END
                    WHERE createdAt = 0
                    """.trimIndent()
                )

                // ---------------------------------------------------------------
                // 4. Índices útiles para los nuevos filtros
                // ---------------------------------------------------------------
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_type` ON `expenses` (`type`)")
            }
        }
    }
}