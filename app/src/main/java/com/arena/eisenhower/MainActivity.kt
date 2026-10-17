package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class MainActivity : Activity() {
    private lateinit var repo: TaskRepository
    private lateinit var quadrantSpinner: Spinner
    private lateinit var dueSpinner: Spinner
    private lateinit var taskInput: EditText
    private lateinit var tasksContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = TaskRepository(this)

        quadrantSpinner = findViewById(R.id.quadrantSpinner)
        dueSpinner = findViewById(R.id.dueSpinner)
        taskInput = findViewById(R.id.taskInput)
        tasksContainer = findViewById(R.id.tasksContainer)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))
        dueSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, DueDateUtils.labels.toList())

        findViewById<Button>(R.id.btnAdd).setOnClickListener { addTask() }
        findViewById<Button>(R.id.btnWidget).setOnClickListener {
            EisenhowerWidgetProvider.updateAllWidgets(this)
            Toast.makeText(this, "Widget actualizado", Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnNotify).setOnClickListener { askPermissions() }

        findViewById<Button>(R.id.btnTestNotify).setOnClickListener {
            askPermissions()
            // Creamos una tarea de prueba con fecha de hace 1 minuto para forzar la notificación
            val dummyTime = System.currentTimeMillis() - 60000 
            repo.addTask("¡Probar notificación de Eisenhower!", Quadrant.DO_FIRST, dummyTime)
            refreshTasks()
            
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
            Toast.makeText(this, "Recordatorio en cola. Revisa tu cortina.", Toast.LENGTH_SHORT).show()
        }

        askPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshTasks()
        EisenhowerWidgetProvider.updateAllWidgets(this)
    }

    private fun addTask() {
        val title = taskInput.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show()
            return
        }

        val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
        val dueAt = DueDateUtils.resolve(dueSpinner.selectedItemPosition)
        var calendarId: Long? = null
        if (dueAt != null) calendarId = insertCalendarEvent(title, dueAt)

        repo.addTask(title, quadrant, dueAt, calendarId)
        taskInput.setText("")
        refreshTasks()
        EisenhowerWidgetProvider.updateAllWidgets(this)
    }

    private fun insertCalendarEvent(title: String, dueAt: Long): Long? {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, dueAt)
                put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "Tarea creada desde Eisenhower Widget")
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) { return null }
    }

    private fun refreshTasks() {
        tasksContainer.removeAllViews()
        val tasks = repo.getAll().sortedWith(compareBy<Task> { it.done }.thenBy { it.quadrant.ordinal }.thenBy { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.createdAt })

        if (tasks.isEmpty()) {
            val empty = TextView(this).apply {
                text = "Sin tareas todavía. Añade una o toca + en el widget."
                setTextColor(android.graphics.Color.parseColor("#64748B"))
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            tasksContainer.addView(empty)
            return
        }

        val inflater = LayoutInflater.from(this)
        tasks.forEach { task ->
            val row = inflater.inflate(R.layout.row_task_main, tasksContainer, false)
            val check = row.findViewById<CheckBox>(R.id.rowCheck)
            val title = row.findViewById<TextView>(R.id.rowTitle)
            val meta = row.findViewById<TextView>(R.id.rowMeta)
            val delete = row.findViewById<Button>(R.id.btnDelete)

            check.isChecked = task.done
            val titleText = SpannableString(task.title)
            if (task.done) titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            title.text = titleText
            
            val calText = if (task.calendarEventId != null) " 📅" else ""
            meta.text = "${quadrantLabel(task.quadrant)} · ${DueDateUtils.format(task.dueAt)}$calText"

            check.setOnClickListener { repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }
            delete.setOnClickListener { repo.deleteTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }
            tasksContainer.addView(row)
        }
    }

    private fun quadrantLabel(quadrant: Quadrant): String = when (quadrant) {
        Quadrant.DO_FIRST -> "Hazlo ya"
        Quadrant.SCHEDULE -> "Planifica"
        Quadrant.DELEGATE -> "Delega"
        Quadrant.ELIMINATE -> "Elimina"
    }

    private fun askPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CALENDAR)
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.WRITE_CALENDAR)
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 42)
    }
}
