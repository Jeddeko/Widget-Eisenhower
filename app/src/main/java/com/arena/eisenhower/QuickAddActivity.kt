package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class QuickAddActivity : Activity() {
    private lateinit var repo: TaskRepository
    private var selectedDueAt: Long? = null

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
        val dateBtn = findViewById<MaterialButton>(R.id.quickDateBtn)
        val calendarCheck = findViewById<CheckBox>(R.id.quickCalendarCheck)
        val save = findViewById<Button>(R.id.quickSave)
        val titleText = findViewById<TextView>(R.id.quickTitle)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))

        val taskId = intent.getLongExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, -1L)
        val incomingQuadrant = Quadrant.fromKey(intent.getStringExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT))

        fun updateDateButtonText() {
            if (selectedDueAt == null) {
                dateBtn.text = "Sin recordatorio"
            } else {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateBtn.text = sdf.format(Date(selectedDueAt!!))
            }
        }

        if (taskId != -1L) {
            val task = repo.getAll().firstOrNull { it.id == taskId }
            if (task != null) {
                titleText.text = "Editar tarea"
                input.setText(task.title)
                quadrantSpinner.setSelection(task.quadrant.ordinal)
                selectedDueAt = task.dueAt
                save.text = "ACTUALIZAR TAREA"
                calendarCheck.visibility = View.GONE 
            }
        } else {
            quadrantSpinner.setSelection(incomingQuadrant.ordinal)
        }
        updateDateButtonText()

        dateBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            if (selectedDueAt != null) cal.timeInMillis = selectedDueAt!!

            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    cal.set(Calendar.MINUTE, minute)
                    cal.set(Calendar.SECOND, 0)
                    selectedDueAt = cal.timeInMillis
                    updateDateButtonText()
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                
                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            
            datePicker.setButton(android.app.Dialog.BUTTON_NEGATIVE, "Borrar") { _, _ ->
                selectedDueAt = null
                updateDateButtonText()
            }
            datePicker.show()
        }

        input.requestFocus()

        save.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
            
            if (taskId != -1L) {
                val existingTask = repo.getAll().firstOrNull { it.id == taskId }
                if (existingTask != null) {
                    val updatedTask = existingTask.copy(title = title, quadrant = quadrant, dueAt = selectedDueAt)
                    repo.updateTask(updatedTask)
                    ReminderScheduler.schedule(this, updatedTask)
                }
            } else {
                var calendarId: Long? = null
                if (calendarCheck.isChecked && selectedDueAt != null) {
                    calendarId = insertCalendarEvent(title, selectedDueAt!!)
                }
                val newTask = repo.addTask(title, quadrant, selectedDueAt, calendarId)
                if (newTask != null) {
                    ReminderScheduler.schedule(this, newTask)
                }
            }

            EisenhowerWidgetProvider.updateAllWidgets(this)
            CloudSync.uploadTasks(this) // ESTA ES LA LÍNEA MÁGICA AÑADIDA
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
