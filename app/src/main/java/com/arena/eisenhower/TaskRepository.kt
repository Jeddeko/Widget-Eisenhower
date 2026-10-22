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
                    val compAt = if (obj.has("completedAt") && !obj.isNull("completedAt")) obj.getLong("completedAt") else null
                    
                    val remindersList = mutableListOf<Long>()
                    if (obj.has("reminders")) {
                        val remArray = obj.getJSONArray("reminders")
                        for (j in 0 until remArray.length()) {
                            remindersList.add(remArray.getLong(j))
                        }
                    }

                    val days2 = if (obj.has("advanceNoticeDays2") && !obj.isNull("advanceNoticeDays2")) obj.getInt("advanceNoticeDays2") else null
                    val quad2 = if (obj.has("quadrantPhase2") && !obj.isNull("quadrantPhase2")) Quadrant.fromKey(obj.getString("quadrantPhase2")) else null
                    
                    val days3 = if (obj.has("advanceNoticeDays3") && !obj.isNull("advanceNoticeDays3")) obj.getInt("advanceNoticeDays3") else null
                    val quad3 = if (obj.has("quadrantPhase3") && !obj.isNull("quadrantPhase3")) Quadrant.fromKey(obj.getString("quadrantPhase3")) else null

                    add(Task(
                        id = obj.getLong("id"), 
                        title = obj.optString("title", ""), 
                        quadrant = Quadrant.fromKey(obj.optString("quadrant", "do")),
                        done = obj.optBoolean("done", false), 
                        dueAt = due, 
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()), 
                        completedAt = compAt,
                        calendarEventId = calId, 
                        order = obj.optInt("order", 0),
                        reminders = remindersList,
                        
                        isEvent = obj.optBoolean("isEvent", false),
                        advanceNoticeDays1 = obj.optInt("advanceNoticeDays1", 30),
                        quadrantPhase1 = Quadrant.fromKey(obj.optString("quadrantPhase1", "delegate")),
                        advanceNoticeDays2 = days2,
                        quadrantPhase2 = quad2,
                        advanceNoticeDays3 = days3,
                        quadrantPhase3 = quad3,
                        
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
            completedAt = if (task.done) now else null,
            order = (all.filter { it.quadrant == task.quadrant }.maxOfOrNull { it.order } ?: 0) + 1
        )
        saveAll(all + newTask)
        return newTask
    }

    @Synchronized
    fun toggleTask(id: Long) {
        val now = System.currentTimeMillis()
        saveAll(getAll().map { 
            if (it.id == id) {
                val newDone = !it.done
                it.copy(done = newDone, updatedAt = now, completedAt = if (newDone) now else null)
            } else it 
        })
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
                .put("order", task.order).put("isEvent", task.isEvent)
                .put("advanceNoticeDays1", task.advanceNoticeDays1).put("quadrantPhase1", task.quadrantPhase1.key)
                .put("recurrence", task.recurrence.name)

            obj.put("completedAt", task.completedAt ?: JSONObject.NULL)
            obj.put("advanceNoticeDays2", task.advanceNoticeDays2 ?: JSONObject.NULL)
            obj.put("quadrantPhase2", task.quadrantPhase2?.key ?: JSONObject.NULL)
            obj.put("advanceNoticeDays3", task.advanceNoticeDays3 ?: JSONObject.NULL)
            obj.put("quadrantPhase3", task.quadrantPhase3?.key ?: JSONObject.NULL)
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
