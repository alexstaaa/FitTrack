package com.fittrack.app.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.getDefault())

fun formatDate(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

fun formatDateTime(epochMillis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** "1h 12m" / "42m" style duration between two timestamps. */
fun formatDuration(startMillis: Long, endMillis: Long): String {
    val totalMinutes = ((endMillis - startMillis) / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/** Trims trailing ".0" — "60 kg" rather than "60.0 kg". */
fun formatWeight(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else "%.1f kg".format(kg)

fun formatVolume(kg: Double): String =
    if (kg >= 1000) "%.1ft".format(kg / 1000) else "${kg.toInt()} kg"
