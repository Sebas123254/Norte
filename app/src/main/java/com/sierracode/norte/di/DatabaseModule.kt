package com.sierracode.norte.di

import android.content.Context
import androidx.room.Room
import com.sierracode.norte.data.local.AppDatabase
import com.sierracode.norte.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "norte.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides fun provideDocumentDao(db: AppDatabase): DocumentDao = db.documentDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideDebtDao(db: AppDatabase): DebtDao = db.debtDao()
}