package com.arena.eisenhower

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DueDateUtils {
    val labels = arrayOf(
        "Sin recordatorio",
        "Hoy 20:00",
        "Mañana 09:00",
        "En 3 días 09:00"
    )

    fun resolve(position: Int): Long? {
        if (position == 0) return null

        val calendar = Calendar.getInstance()

        when (position) {
            1 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 20)
                calendar.set(Calendar.MINUTE, 0)
            }

            2 -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
            }

            3 -> {
                calendar.add(Calendar.DAY_OF_YEAR, 3)
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
            }
        }

        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun format(dueAt: Long?): String {
        if (dueAt == null) return "sin fecha"

        val base = SimpleDateFormat(
            "dd/MM HH:mm",
            Locale.getDefault()
        ).format(Date(dueAt))

        return if (dueAt < System.currentTimeMillis()) {
            "⚠ vencía $base"
        } else {
            base
        }
    }
}
