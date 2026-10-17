package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

class QuickAddActivity : Activity() {
    private lateinit var repo: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_add)

        val params = window.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.attributes = params

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        repo = TaskRepository(this)

        val input = findViewById<EditText>(R.id.quickInput)
        val quadrantSpinner = findViewById<Spinner>(R.id.quickQuadrant)
        val dueSpinner = findViewById<Spinner>(R.id.quickDue)
        val calendarCheck = findViewById<CheckBox>(R.id.quickCalendarCheck)
        val save = findViewById<Button>(R.id.quickSave)
        val titleText = findViewById<TextView>(R.id.quickTitle)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))
        dueSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, DueDateUtils.labels.toList())

        // Recibimos si venimos a editar o a crear
        val taskId = intent.getLongExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, -1L)
        val incomingQuadrant = Quadrant.fromKey(intent.getStringExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT))

        if (taskId != -1L) {
            // MODO EDICIÓN
            val task = repo.getAll().firstOrNull { it.id == taskId }
            if (task != null) {
                titleText.text = "Editar tarea"
                input.setText(task.title)
                quadrantSpinner.setSelection(task.quadrant.ordinal)
                dueSpinner.setSelection(DueDateUtils.findPosition(task.dueAt))
                save.text = "ACTUALIZAR TAREA"
                calendarCheck.visibility = View.GONE // Corregido aquí
            }
        } else {
            // MODO CREACIÓN
            quadrantSpinner.setSelection(incomingQuadrant.ordinal)
        }

        input.requestFocus()

        save.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
            val dueAt = DueDateUtils.resolve(dueSpinner.selectedItemPosition)
            
            if (taskId != -1L) {
                val existingTask = repo.getAll().firstOrNull { it.id == taskId }
                if (existingTask != null) {
                    repo.updateTask(existingTask.copy(title = title, quadrant = quadrant, dueAt = dueAt))
                }
            } else {
                var calendarId: Long? = null
                if (calendarCheck.isChecked && dueAt != null) {
                    calendarId = insertCalendarEvent(title, dueAt)
                }
                repo.addTask(title, quadrant, dueAt, calendarId)
            }

            EisenhowerWidgetProvider.updateAllWidgets(this)
            finish()
        }
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
}
