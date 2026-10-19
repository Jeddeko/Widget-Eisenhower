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
                        id = obj.getLong("id"), title = obj.optString("title", ""), quadrant = Quadrant.fromKey(obj.optString("quadrant", "do")),
                        done = obj.optBoolean("done", false), dueAt = due, createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()), calendarEventId = calId, order = obj.optInt("order", 0),
                        reminders = remindersList
                    ))
                }
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    fun addTask(title: String, quadrant: Quadrant, dueAt: Long? = null, calendarEventId: Long? = null, reminders: List<Long> = emptyList()): Task? {
        val clean = title.trim(); if (clean.isBlank()) return null
        val all = getAll()
        val id = (all.maxOfOrNull { it.id } ?: 0L) + 1L
        val now = System.currentTimeMillis()
        val newOrder = (all.filter { it.quadrant == quadrant }.maxOfOrNull { it.order } ?: 0) + 1
        val task = Task(id, clean, quadrant, false, dueAt, now, now, calendarEventId, newOrder, reminders)
        saveAll(all + task)
        return task
    }

    @Synchronized
    fun toggleTask(id: Long): Task? {
        val now = System.currentTimeMillis()
        var updatedTask: Task? = null
        saveAll(getAll().map { 
            if (it.id == id) { 
                updatedTask = it.copy(done = !it.done, updatedAt = now)
                updatedTask!!
            } else it 
        })
        return updatedTask
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
            val obj = JSONObject().put("id", task.id).put("title", task.title).put("quadrant", task.quadrant.key)
                .put("done", task.done).put("createdAt", task.createdAt).put("updatedAt", task.updatedAt).put("order", task.order)
            obj.put("dueAt", if (task.dueAt == null) JSONObject.NULL else task.dueAt)
            obj.put("calendarEventId", if (task.calendarEventId == null) JSONObject.NULL else task.calendarEventId)
            
            val remArray = JSONArray()
            task.reminders.forEach { remArray.put(it) }
            obj.put("reminders", remArray)
            
            array.put(obj)
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    companion object { private const val PREFS_NAME = "eisenhower_tasks"; private const val KEY_TASKS = "tasks_json" }
}
