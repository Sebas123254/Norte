package com.sierracode.norte.feature.expenses

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.sierracode.norte.R
import com.sierracode.norte.data.local.entity.ExpenseCategory
import com.sierracode.norte.data.local.entity.PaymentMethod
import com.sierracode.norte.data.local.entity.TransactionType
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.COMIDA                     -> Icons.Filled.Restaurant
    ExpenseCategory.TRANSPORTE                 -> Icons.Filled.DirectionsCar
    ExpenseCategory.VIVIENDA                   -> Icons.Filled.Home
    ExpenseCategory.EDUCACION                  -> Icons.Filled.School
    ExpenseCategory.ENTRETENIMIENTO            -> Icons.Filled.Movie
    ExpenseCategory.SALUD                      -> Icons.Filled.MedicalServices
    ExpenseCategory.ROPA                       -> Icons.Filled.Checkroom
    ExpenseCategory.IMPUESTOS_FONDO_EMERGENCIA -> Icons.Filled.AccountBalance
    ExpenseCategory.OTROS                      -> Icons.Filled.Category
}

@Composable
fun ExpenseCategory.label(): String = when (this) {
    ExpenseCategory.COMIDA                     -> stringResource(R.string.expenses_category_comida)
    ExpenseCategory.TRANSPORTE                 -> stringResource(R.string.expenses_category_transporte)
    ExpenseCategory.VIVIENDA                   -> stringResource(R.string.expenses_category_vivienda)
    ExpenseCategory.EDUCACION                  -> stringResource(R.string.expenses_category_educacion)
    ExpenseCategory.ENTRETENIMIENTO            -> stringResource(R.string.expenses_category_entretenimiento)
    ExpenseCategory.SALUD                      -> stringResource(R.string.expenses_category_salud)
    ExpenseCategory.ROPA                       -> stringResource(R.string.expenses_category_ropa)
    ExpenseCategory.IMPUESTOS_FONDO_EMERGENCIA -> stringResource(R.string.expenses_category_impuestos_fondo)
    ExpenseCategory.OTROS                      -> stringResource(R.string.expenses_category_otros)
}

@Composable
fun ExpenseCategory.color(): Color = when (this) {
    ExpenseCategory.COMIDA                     -> Color(0xFFE67E22)
    ExpenseCategory.TRANSPORTE                 -> Color(0xFF3498DB)
    ExpenseCategory.VIVIENDA                   -> Color(0xFF9B59B6)
    ExpenseCategory.EDUCACION                  -> Color(0xFF1ABC9C)
    ExpenseCategory.ENTRETENIMIENTO            -> Color(0xFFE91E63)
    ExpenseCategory.SALUD                      -> Color(0xFF2ECC71)
    ExpenseCategory.ROPA                       -> Color(0xFFF39C12)
    ExpenseCategory.IMPUESTOS_FONDO_EMERGENCIA -> Color(0xFF795548)
    ExpenseCategory.OTROS                      -> Color(0xFF607D8B)
}

@Composable
fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.EFECTIVO        -> stringResource(R.string.payment_efectivo)
    PaymentMethod.TARJETA_DEBITO  -> stringResource(R.string.payment_tarjeta_debito)
    PaymentMethod.TARJETA_CREDITO -> stringResource(R.string.payment_tarjeta_credito)
    PaymentMethod.TRANSFERENCIA   -> stringResource(R.string.payment_transferencia)
    PaymentMethod.OTRO            -> stringResource(R.string.payment_otro)
}

@Composable
fun TransactionType.label(): String = when (this) {
    TransactionType.GASTO   -> stringResource(R.string.expenses_type_expense)
    TransactionType.INGRESO -> stringResource(R.string.expenses_type_income)
}

fun formatMoney(cents: Long, currencyCode: String = "MXN"): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { fmt.currency = Currency.getInstance(currencyCode) }
    return fmt.format(cents / 100.0)
}

fun formatMoneyRaw(cents: Long): String = String.format(Locale.US, "%.2f", cents / 100.0)