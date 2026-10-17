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

    // Nueva función para saber qué opción del menú mostrar al editar
    fun findPosition(dueAt: Long?): Int {
        if (dueAt == null) return 0
        for (i in 1..3) {
            val calc = resolve(i)
            if (calc != null && Math.abs(calc - dueAt) < 120000) { // 2 minutos de margen
                return i
            }
        }
        return 0 
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
