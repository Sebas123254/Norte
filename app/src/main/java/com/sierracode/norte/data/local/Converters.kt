package com.sierracode.norte.data.local

import androidx.room.TypeConverter
import com.sierracode.norte.data.local.entity.*

class Converters {
    @TypeConverter fun docTypeToString(v: DocumentType): String = v.name
    @TypeConverter fun stringToDocType(v: String): DocumentType = DocumentType.valueOf(v)

    @TypeConverter fun docStatusToString(v: DocumentStatus): String = v.name
    @TypeConverter fun stringToDocStatus(v: String): DocumentStatus = DocumentStatus.valueOf(v)

    @TypeConverter fun txTypeToString(v: TransactionType): String = v.name
    @TypeConverter fun stringToTxType(v: String): TransactionType = TransactionType.valueOf(v)

    @TypeConverter fun expCatToString(v: ExpenseCategory): String = v.name
    @TypeConverter fun stringToExpCat(v: String): ExpenseCategory = ExpenseCategory.valueOf(v)

    @TypeConverter fun goalStatusToString(v: GoalStatus): String = v.name
    @TypeConverter fun stringToGoalStatus(v: String): GoalStatus = GoalStatus.valueOf(v)

    @TypeConverter fun goalPriorityToString(v: GoalPriority): String = v.name
    @TypeConverter fun stringToGoalPriority(v: String): GoalPriority = GoalPriority.valueOf(v)

    @TypeConverter fun debtStrategyToString(v: DebtStrategy): String = v.name
    @TypeConverter fun stringToDebtStrategy(v: String): DebtStrategy = DebtStrategy.valueOf(v)

    @TypeConverter fun debtStatusToString(v: DebtStatus): String = v.name
    @TypeConverter fun stringToDebtStatus(v: String): DebtStatus = DebtStatus.valueOf(v)
}