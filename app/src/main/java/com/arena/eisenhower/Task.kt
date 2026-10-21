package com.arena.eisenhower

import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class Quadrant(val key: String) {
    DO_FIRST("do"), SCHEDULE("schedule"), DELEGATE("delegate"), ELIMINATE("eliminate");
    companion object {
        fun fromKey(key: String?): Quadrant = values().firstOrNull { it.key == key } ?: DO_FIRST
    }
}

enum class RecurrenceType { NONE, DAILY, WEEKLY, MONTHLY }

data class Task(
    val id: Long,
    val title: String,
    val quadrant: Quadrant,
    val done: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val calendarEventId: Long? = null,
    val order: Int = 0,
    val reminders: List<Long> = emptyList(),
    
    val isEvent: Boolean = false,
    val advanceNoticeDays1: Int = 30, 
    val quadrantPhase1: Quadrant = Quadrant.DELEGATE, 
    val advanceNoticeDays2: Int? = null, 
    val quadrantPhase2: Quadrant? = null, 
    val advanceNoticeDays3: Int? = null, 
    val quadrantPhase3: Quadrant? = null,
    
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDayOfWeek: Int? = null,
    val recurrenceDayOfMonth: Int? = null
) {
    fun getEffectiveQuadrant(): Quadrant? {
        if (!isEvent) return quadrant
        if (dueAt == null && recurrence == RecurrenceType.NONE) return quadrant

        var targetDate = dueAt
        if (recurrence != RecurrenceType.NONE && dueAt != null) {
            val now = System.currentTimeMillis()
            if (now > dueAt) {
                val diff = now - dueAt
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                when (recurrence) {
                    RecurrenceType.DAILY -> targetDate = dueAt + (days * 24 * 60 * 60 * 1000L)
                    RecurrenceType.WEEKLY -> targetDate = dueAt + ((days / 7) * 7 * 24 * 60 * 60 * 1000L)
                    RecurrenceType.MONTHLY -> {
                        val months = (days / 30).toInt()
                        val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
                        cal.add(Calendar.MONTH, months)
                        if (recurrenceDayOfMonth != null) {
                            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            cal.set(Calendar.DAY_OF_MONTH, minOf(recurrenceDayOfMonth, maxDay))
                        }
                        targetDate = cal.timeInMillis
                    }
                    else -> {}
                }
            }
        }

        if (targetDate == null) return quadrant

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val eventDay = Calendar.getInstance().apply {
            timeInMillis = targetDate
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffMillis = eventDay - today
        val daysUntil = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()

        return when {
            advanceNoticeDays3 != null && quadrantPhase3 != null && daysUntil <= advanceNoticeDays3 -> quadrantPhase3
            advanceNoticeDays2 != null && quadrantPhase2 != null && daysUntil <= advanceNoticeDays2 -> quadrantPhase2
            daysUntil <= advanceNoticeDays1 -> quadrantPhase1
            else -> null 
        }
    }

    fun getOccurrences(startMillis: Long, endMillis: Long): List<Long> {
        if (dueAt == null) return emptyList()
        val occurrences = mutableListOf<Long>()
        
        // Lógica normal o recurrente (SIN ROLLOVER PARA EVITAR COLAPSOS)
        if (recurrence == RecurrenceType.NONE || !isEvent) {
            if (dueAt in startMillis..endMillis) occurrences.add(dueAt)
            return occurrences
        }
        
        val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
        
        if (startMillis > dueAt) {
            val diff = startMillis - dueAt
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            when (recurrence) {
                RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, days.toInt())
                RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, (days / 7).toInt())
                RecurrenceType.MONTHLY -> {
                    val months = (days / 30).toInt()
                    cal.add(Calendar.MONTH, months)
                    if (recurrenceDayOfMonth != null) {
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, minOf(recurrenceDayOfMonth, maxDay))
                    }
                }
                else -> {}
            }
        }

        var iterations = 0
        while (cal.timeInMillis <= endMillis && iterations < 50) {
            if (cal.timeInMillis >= startMillis) {
                occurrences.add(cal.timeInMillis)
            }
            when (recurrence) {
                RecurrenceType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurrenceType.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                RecurrenceType.MONTHLY -> {
                    cal.add(Calendar.MONTH, 1)
                    if (recurrenceDayOfMonth != null) {
                        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, minOf(recurrenceDayOfMonth, maxDay))
                    }
                }
                else -> break
            }
            iterations++
        }
        return occurrences.distinct()
    }
}
