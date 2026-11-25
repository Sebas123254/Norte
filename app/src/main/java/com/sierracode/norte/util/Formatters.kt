package com.sierracode.norte.util

fun money(cents: Long): String =
    "MXN %.2f".format(cents / 100.0)
