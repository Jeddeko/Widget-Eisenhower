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

        val collectionRef = db.collection("users").document(user.uid).collection("tasks")

        tasks.forEach { task ->
            val map = HashMap<String, Any?>()
            map["id"] = task.id; map["title"] = task.title; map["quadrant"] = task.quadrant.key
            map["done"] = task.done; map["dueAt"] = task.dueAt; map["createdAt"] = task.createdAt
            map["updatedAt"] = task.updatedAt; map["completedAt"] = task.completedAt
            map["calendarEventId"] = task.calendarEventId
            map["order"] = task.order; map["reminders"] = task.reminders
            map["isEvent"] = task.isEvent; map["advanceNoticeDays1"] = task.advanceNoticeDays1
            map["quadrantPhase1"] = task.quadrantPhase1.key; map["advanceNoticeDays2"] = task.advanceNoticeDays2
            map["quadrantPhase2"] = task.quadrantPhase2?.key; map["advanceNoticeDays3"] = task.advanceNoticeDays3
            map["quadrantPhase3"] = task.quadrantPhase3?.key; map["recurrence"] = task.recurrence.name
            map["recurrenceDayOfWeek"] = task.recurrenceDayOfWeek; map["recurrenceDayOfMonth"] = task.recurrenceDayOfMonth

            collectionRef.document(task.id.toString()).set(map)
        }

        // Limpiar de la nube las tareas que ya borramos en local
        collectionRef.get().addOnSuccessListener { snapshot ->
            val cloudIds = snapshot.documents.map { it.id.toLongOrNull() ?: 0L }.toSet()
            val localIds = tasks.map { it.id }.toSet()
            val toDelete = cloudIds - localIds

            toDelete.forEach { id ->
                if (id > 0) collectionRef.document(id.toString()).delete()
            }
            
            // Borrar el documento viejo si aún existe
            collectionRef.document("sync_data").delete()
            Log.d("CloudSync", "Nube actualizada")
        }
    }

    fun downloadTasks(context: Context, onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        val repo = TaskRepository(context)
        val collectionRef = db.collection("users").document(user.uid).collection("tasks")

        // 1. INTENTO DE MIGRACIÓN (Buscar si había datos en el formato viejo)
        collectionRef.document("sync_data").get().addOnSuccessListener { oldDoc ->
            if (oldDoc.exists()) {
                val cloudTasksList = oldDoc.get("tasks") as? List<Map<String, Any>>
                if (cloudTasksList != null) {
                    val migratedTasks = cloudTasksList.map { map ->
                        Task(
                            id = (map["id"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            title = map["title"] as? String ?: "",
                            quadrant = Quadrant.fromKey(map["quadrant"] as? String),
                            done = map["done"] as? Boolean ?: false,
                            dueAt = (map["dueAt"] as? Number)?.toLong(),
                            createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            completedAt = (map["completedAt"] as? Number)?.toLong(),
                            calendarEventId = (map["calendarEventId"] as? Number)?.toLong(),
                            order = (map["order"] as? Number)?.toInt() ?: 0,
                            reminders = (map["reminders"] as? List<Number>)?.map { it.toLong() } ?: emptyList(),
                            isEvent = map["isEvent"] as? Boolean ?: false,
                            advanceNoticeDays1 = (map["advanceNoticeDays1"] as? Number)?.toInt() ?: 30,
                            quadrantPhase1 = Quadrant.fromKey(map["quadrantPhase1"] as? String),
                            advanceNoticeDays2 = (map["advanceNoticeDays2"] as? Number)?.toInt(),
                            quadrantPhase2 = (map["quadrantPhase2"] as? String)?.let { Quadrant.fromKey(it) },
                            advanceNoticeDays3 = (map["advanceNoticeDays3"] as? Number)?.toInt(),
                            quadrantPhase3 = (map["quadrantPhase3"] as? String)?.let { Quadrant.fromKey(it) },
                            recurrence = try { RecurrenceType.valueOf(map["recurrence"] as? String ?: "NONE") } catch(e: Exception) { RecurrenceType.NONE },
                            recurrenceDayOfWeek = (map["recurrenceDayOfWeek"] as? Number)?.toInt(),
                            recurrenceDayOfMonth = (map["recurrenceDayOfMonth"] as? Number)?.toInt()
                        )
                    }
                    // Guardar localmente y subir al nuevo formato
                    repo.saveAll(migratedTasks)
                    uploadTasks(context) 
                }
            }
        }

        // 2. DESCARGAR TAREAS NUEVAS (Formato de documentos individuales)
        collectionRef.get().addOnSuccessListener { querySnapshot ->
            val newTasks = mutableListOf<Task>()
            for (document in querySnapshot.documents) {
                if (document.id == "sync_data") continue
                
                val map = document.data ?: continue
                newTasks.add(
                    Task(
                        id = (map["id"] as? Number)?.toLong() ?: 0L, 
                        title = map["title"] as? String ?: "",
                        quadrant = Quadrant.fromKey(map["quadrant"] as? String), 
                        done = map["done"] as? Boolean ?: false,
                        dueAt = (map["dueAt"] as? Number)?.toLong(), 
                        createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        completedAt = (map["completedAt"] as? Number)?.toLong(),
                        calendarEventId = (map["calendarEventId"] as? Number)?.toLong(), 
                        order = (map["order"] as? Number)?.toInt() ?: 0,
                        reminders = (map["reminders"] as? List<Number>)?.map { it.toLong() } ?: emptyList(),
                        isEvent = map["isEvent"] as? Boolean ?: false,
                        advanceNoticeDays1 = (map["advanceNoticeDays1"] as? Number)?.toInt() ?: 30,
                        quadrantPhase1 = Quadrant.fromKey(map["quadrantPhase1"] as? String),
                        advanceNoticeDays2 = (map["advanceNoticeDays2"] as? Number)?.toInt(),
                        quadrantPhase2 = (map["quadrantPhase2"] as? String)?.let { Quadrant.fromKey(it) },
                        advanceNoticeDays3 = (map["advanceNoticeDays3"] as? Number)?.toInt(),
                        quadrantPhase3 = (map["quadrantPhase3"] as? String)?.let { Quadrant.fromKey(it) },
                        recurrence = try { RecurrenceType.valueOf(map["recurrence"] as? String ?: "NONE") } catch(e: Exception) { RecurrenceType.NONE },
                        recurrenceDayOfWeek = (map["recurrenceDayOfWeek"] as? Number)?.toInt(),
                        recurrenceDayOfMonth = (map["recurrenceDayOfMonth"] as? Number)?.toInt()
                    )
                )
            }
            
            // Si encontramos tareas nuevas o migradas, actualizamos
            if (newTasks.isNotEmpty()) {
                // Combinar con las que ya tengamos en local para no borrar lo que hicimos offline
                val localTasks = repo.getAll()
                val mergedTasks = (localTasks + newTasks).distinctBy { it.id }
                repo.saveAll(mergedTasks)
            }
            
            EisenhowerWidgetProvider.updateAllWidgets(context)
            CalendarWidgetProvider.updateAllWidgets(context)
            onComplete()
        }.addOnFailureListener { e -> 
            Log.e("CloudSync", "Error al descargar", e)
            onComplete() 
        }
    }
}
