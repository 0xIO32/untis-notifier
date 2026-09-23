package utils

import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

fun LocalDate.daysString(): String {
    if (this.toEpochDay() == LocalDate.now().toEpochDay()) {
        return ""
    }

    return " in ${LocalDate.now().until(this,ChronoUnit.DAYS)} days (${this.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE,
        Locale.getDefault())})"
}
