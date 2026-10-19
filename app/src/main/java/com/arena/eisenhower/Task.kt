package com.arena.eisenhower

enum class Quadrant(val key: String) {
    DO_FIRST("do"), SCHEDULE("schedule"), DELEGATE("delegate"), ELIMINATE("eliminate");
    companion object {
        fun fromKey(key: String?): Quadrant = values().firstOrNull { it.key == key } ?: DO_FIRST
    }
}

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
    // NUEVO: Lista de alertas programadas
    val reminders: List<Long> = emptyList()
)
