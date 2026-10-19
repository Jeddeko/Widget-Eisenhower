package com.arena.eisenhower

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAll(): List<Task> {
        val raw = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val due = if (obj.has("dueAt") && !obj.isNull("dueAt")) obj.getLong("dueAt") else null
                    val calId = if (obj.has("calendarEventId") && !obj.isNull("calendarEventId")) obj.getLong("calendarEventId") else null
                    
                    val remindersList = mutableListOf<Long>()
                    if (obj.has("reminders")) {
                        val remArray = obj.getJSONArray("reminders")
                        for (j in 0 until remArray.length()) {
                            remindersList.add(remArray.getLong(j))
                        }
                    }

                    add(Task(
                        id = obj.getLong("id"), 
                        title = obj.optString("title", ""), 
                        quadrant = Quadrant.fromKey(obj.optString("quadrant", "do")),
                        done = obj.optBoolean("done", false), 
                        dueAt = due, 
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()), 
                        calendarEventId = calId, 
                        order = obj.optInt("order", 0),
                        reminders = remindersList,
                        
                        isEvent = obj.optBoolean("isEvent", false),
                        advanceNoticeDays = obj.optInt("advanceNoticeDays", 0),
                        quadrantBeforeEvent = obj.optString("quadrantBeforeEvent", "").let { if (it.isBlank()) null else Quadrant.fromKey(it) },
                        quadrantOnEventDay = obj.optString("quadrantOnEventDay", "").let { if (it.isBlank()) null else Quadrant.fromKey(it) },
                        
                        recurrence = try { RecurrenceType.valueOf(obj.optString("recurrence", "NONE")) } catch(e: Exception) { RecurrenceType.NONE },
                        recurrenceDayOfWeek = if (obj.has("recurrenceDayOfWeek") && !obj.isNull("recurrenceDayOfWeek")) obj.getInt("recurrenceDayOfWeek") else null,
                        recurrenceDayOfMonth = if (obj.has("recurrenceDayOfMonth") && !obj.isNull("recurrenceDayOfMonth")) obj.getInt("recurrenceDayOfMonth") else null
                    ))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    fun addTask(task: Task): Task? {
        val clean = task.title.trim(); if (clean.isBlank()) return null
        val all = getAll()
        val id = (all.maxOfOrNull { it.id } ?: 0L) + 1L
        val now = System.currentTimeMillis()
        
        val newTask = task.copy(
            id = id, title = clean, createdAt = now, updatedAt = now,
            order = (all.filter { it.quadrant == task.quadrant }.maxOfOrNull { it.order } ?: 0) + 1
        )
        saveAll(all + newTask)
        return newTask
    }

    @Synchronized
    fun toggleTask(id: Long) {
        val now = System.currentTimeMillis()
        saveAll(getAll().map { if (it.id == id) it.copy(done = !it.done, updatedAt = now) else it })
    }

    @Synchronized
    fun deleteTask(id: Long) { saveAll(getAll().filterNot { it.id == id }) }

    @Synchronized
    fun updateTask(task: Task) { saveAll(getAll().map { if (it.id == task.id) task.copy(updatedAt = System.currentTimeMillis()) else it }) }

    @Synchronized
    fun updateOrder(id: Long, newOrder: Int) { saveAll(getAll().map { if (it.id == id) it.copy(order = newOrder) else it }) }

    fun saveAll(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            val obj = JSONObject()
                .put("id", task.id).put("title", task.title).put("quadrant", task.quadrant.key)
                .put("done", task.done).put("createdAt", task.createdAt).put("updatedAt", task.updatedAt)
                .put("order", task.order)
                .put("isEvent", task.isEvent).put("advanceNoticeDays", task.advanceNoticeDays)
                .put("quadrantBeforeEvent", task.quadrantBeforeEvent?.key ?: "")
                .put("quadrantOnEventDay", task.quadrantOnEventDay?.key ?: "")
                .put("recurrence", task.recurrence.name)

            obj.put("dueAt", if (task.dueAt == null) JSONObject.NULL else task.dueAt)
            obj.put("calendarEventId", if (task.calendarEventId == null) JSONObject.NULL else task.calendarEventId)
            obj.put("recurrenceDayOfWeek", task.recurrenceDayOfWeek ?: JSONObject.NULL)
            obj.put("recurrenceDayOfMonth", task.recurrenceDayOfMonth ?: JSONObject.NULL)
            
            val remArray = JSONArray()
            task.reminders.forEach { remArray.put(it) }
            obj.put("reminders", remArray)
            
            array.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    companion object { private const val PREFS_NAME = "eisenhower_tasks"; private const val KEY_TASKS = "tasks_json" }
}
