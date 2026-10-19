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
import android.widget.*
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class QuickAddActivity : Activity() {
    private lateinit var repo: TaskRepository
    private var selectedDueAt: Long? = null
    private val selectedReminders = mutableListOf<Long>() // Lista de alertas

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
        val btnAddReminder = findViewById<MaterialButton>(R.id.btnAddReminder)
        val btnClearReminders = findViewById<Button>(R.id.btnClearReminders)
        val tvRemindersList = findViewById<TextView>(R.id.tvRemindersList)
        val calendarCheck = findViewById<CheckBox>(R.id.quickCalendarCheck)
        val save = findViewById<Button>(R.id.quickSave)
        val titleText = findViewById<TextView>(R.id.quickTitle)
        val btnDelete = findViewById<ImageButton>(R.id.btnDeleteTask)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))

        val taskId = intent.getLongExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, -1L)
        val incomingQuadrant = Quadrant.fromKey(intent.getStringExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT))

        fun updateDateButtonText() {
            if (selectedDueAt == null) dateBtn.text = "Sin fecha límite"
            else {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateBtn.text = sdf.format(Date(selectedDueAt!!))
            }
        }

        fun updateRemindersUI() {
            if (selectedReminders.isEmpty()) {
                tvRemindersList.text = "Sin alertas adicionales"
                btnClearReminders.visibility = View.GONE
            } else {
                val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                tvRemindersList.text = selectedReminders.sorted().joinToString("\n") { "🔔 " + sdf.format(Date(it)) }
                btnClearReminders.visibility = View.VISIBLE
            }
        }

        if (taskId != -1L) {
            val task = repo.getAll().firstOrNull { it.id == taskId }
            if (task != null) {
                titleText.text = "Editar tarea"
                input.setText(task.title)
                quadrantSpinner.setSelection(task.quadrant.ordinal)
                selectedDueAt = task.dueAt
                selectedReminders.clear()
                selectedReminders.addAll(task.reminders) // Cargar alertas existentes
                save.text = "ACTUALIZAR TAREA"
                calendarCheck.visibility = View.GONE 
                
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    repo.deleteTask(taskId)
                    ReminderScheduler.cancel(this, taskId)
                    CloudSync.uploadTasks(this)
                    EisenhowerWidgetProvider.updateAllWidgets(this)
                    finish()
                    Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            quadrantSpinner.setSelection(incomingQuadrant.ordinal)
        }
        
        updateDateButtonText()
        updateRemindersUI()

        dateBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            if (selectedDueAt != null) cal.timeInMillis = selectedDueAt!!
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0)
                    selectedDueAt = cal.timeInMillis
                    updateDateButtonText()
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePicker.setButton(android.app.Dialog.BUTTON_NEGATIVE, "Borrar") { _, _ -> selectedDueAt = null; updateDateButtonText() }
            datePicker.show()
        }

        // Lógica de Añadir Alerta
        btnAddReminder.setOnClickListener {
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0)
                    selectedReminders.add(cal.timeInMillis)
                    updateRemindersUI()
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        btnClearReminders.setOnClickListener {
            selectedReminders.clear()
            updateRemindersUI()
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
                    val updatedTask = existingTask.copy(title = title, quadrant = quadrant, dueAt = selectedDueAt, reminders = selectedReminders)
                    repo.updateTask(updatedTask)
                    ReminderScheduler.schedule(this, updatedTask)
                }
            } else {
                var calendarId: Long? = null
                if (calendarCheck.isChecked && selectedDueAt != null) {
                    calendarId = insertCalendarEvent(title, selectedDueAt!!)
                }
                val newTask = repo.addTask(title, quadrant, selectedDueAt, calendarId, selectedReminders)
                if (newTask != null) {
                    ReminderScheduler.schedule(this, newTask)
                }
            }

            EisenhowerWidgetProvider.updateAllWidgets(this)
            CloudSync.uploadTasks(this)
            finish()
        }
    }

    private fun insertCalendarEvent(title: String, dueAt: Long): Long? {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, dueAt); put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title); put(CalendarContract.Events.CALENDAR_ID, 1); put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) { return null }
    }
}
