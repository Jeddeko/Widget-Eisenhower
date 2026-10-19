package com.arena.eisenhower

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object CloudSync {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun uploadTasks(context: Context) {
        val user = auth.currentUser ?: return
        val repo = TaskRepository(context)
        val tasks = repo.getAll()

        val taskMaps = tasks.map { task ->
            mapOf(
                "id" to task.id, "title" to task.title, "quadrant" to task.quadrant.key, "done" to task.done,
                "dueAt" to task.dueAt, "createdAt" to task.createdAt, "updatedAt" to task.updatedAt,
                "calendarEventId" to task.calendarEventId, "order" to task.order, "reminders" to task.reminders
            )
        }

        db.collection("users").document(user.uid).collection("tasks")
            .document("sync_data").set(mapOf("tasks" to taskMaps))
            .addOnSuccessListener { Log.d("CloudSync", "Nube actualizada") }
    }

    fun downloadTasks(context: Context, onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        val repo = TaskRepository(context)

        db.collection("users").document(user.uid).collection("tasks").document("sync_data").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val cloudTasksList = document.get("tasks") as? List<Map<String, Any>>
                    if (cloudTasksList != null) {
                        val newTasks = cloudTasksList.map { map ->
                            Task(
                                id = (map["id"] as? Number)?.toLong() ?: 0L, title = map["title"] as? String ?: "",
                                quadrant = Quadrant.fromKey(map["quadrant"] as? String), done = map["done"] as? Boolean ?: false,
                                dueAt = (map["dueAt"] as? Number)?.toLong(), createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                calendarEventId = (map["calendarEventId"] as? Number)?.toLong(), order = (map["order"] as? Number)?.toInt() ?: 0,
                                reminders = (map["reminders"] as? List<Number>)?.map { it.toLong() } ?: emptyList()
                            )
                        }
                        repo.saveAll(newTasks)
                        EisenhowerWidgetProvider.updateAllWidgets(context)
                    }
                }
                onComplete()
            }
            .addOnFailureListener { e -> Log.e("CloudSync", "Error al descargar", e); onComplete() }
    }
}
