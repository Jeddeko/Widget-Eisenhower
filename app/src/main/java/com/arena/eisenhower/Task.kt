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
    val quadrant: Quadrant, // Cuadrante base (solo para tareas normales)
    val done: Boolean = false,
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val calendarEventId: Long? = null,
    val order: Int = 0,
    val reminders: List<Long> = emptyList(),
    
    val isEvent: Boolean = false,
    
    // NUEVOS CAMPOS: 3 FASES DINÁMICAS
    val advanceNoticeDays1: Int = 30, 
    val quadrantPhase1: Quadrant = Quadrant.DELEGATE, 
    
    val advanceNoticeDays2: Int = 7, 
    val quadrantPhase2: Quadrant = Quadrant.SCHEDULE, 
    
    val advanceNoticeDays3: Int = 0, 
    val quadrantPhase3: Quadrant = Quadrant.DO_FIRST,
    
    val recurrence: RecurrenceType = RecurrenceType.NONE,
    val recurrenceDayOfWeek: Int? = null,
    val recurrenceDayOfMonth: Int? = null
) {
    fun getEffectiveQuadrant(): Quadrant? {
        if (!isEvent) return quadrant
        
        if (dueAt == null && recurrence == RecurrenceType.NONE) return quadrant

        var targetDate = dueAt
        if (recurrence != RecurrenceType.NONE && dueAt != null) {
            val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
            val today = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) 
            }.timeInMillis
            
            var iterations = 0
            while (cal.timeInMillis < today && iterations < 100) {
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
            targetDate = cal.timeInMillis
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
            daysUntil <= advanceNoticeDays3 -> quadrantPhase3
            daysUntil <= advanceNoticeDays2 -> quadrantPhase2
            daysUntil <= advanceNoticeDays1 -> quadrantPhase1
            else -> null // No se muestra aún en la matriz
        }
    }

    fun getOccurrences(startMillis: Long, endMillis: Long): List<Long> {
        if (!isEvent || dueAt == null) {
            return if (dueAt != null && dueAt in startMillis..endMillis) listOf(dueAt) else emptyList()
        }
        
        val occurrences = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply { timeInMillis = dueAt }
        var iterations = 0

        while (cal.timeInMillis < startMillis && iterations < 100) {
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

        iterations = 0
        while (cal.timeInMillis <= endMillis && iterations < 100) {
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
        return occurrences
    }
}
