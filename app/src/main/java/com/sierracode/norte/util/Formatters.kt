package com.sierracode.norte.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formatea una cantidad en centavos con la moneda especificada (código ISO 4217, p. ej. "MXN", "USD").
 * Utiliza java.util.Currency y el Locale correspondiente para formatear según estándares internacionales.
 */
fun money(cents: Long, currencyCode: String = "MXN"): String {
    val amount = cents / 100.0
    val validCode = currencyCode.trim().uppercase()

    val locale = when (validCode) {
        "MXN" -> Locale("es", "MX")
        "USD" -> Locale.US
        "EUR" -> Locale.GERMANY
        "CAD" -> Locale.CANADA
        "GBP" -> Locale.UK
        else -> Locale.getDefault()
    }

    return try {
        val currency = Currency.getInstance(validCode)
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        format.format(amount)
    } catch (_: Exception) {
        // Fallback robusto en caso de código no reconocido
        "$validCode %.2f".format(Locale.US, amount)
    }
}
