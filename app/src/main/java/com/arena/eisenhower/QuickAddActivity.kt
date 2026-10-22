package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class QuickAddActivity : Activity() {
    private lateinit var repo: TaskRepository
    private var selectedDueAt: Long? = null
    private val selectedReminders = mutableListOf<Long>()
    private lateinit var containerReminders: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quick_add)

        val params = window.attributes
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        window.attributes = params
        
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        repo = TaskRepository(this)

        val input = findViewById<EditText>(R.id.quickInput)
        val layoutBaseQuadrant = findViewById<LinearLayout>(R.id.layoutBaseQuadrant)
        val quadrantSpinner = findViewById<Spinner>(R.id.quickQuadrant)
        val dateBtn = findViewById<MaterialButton>(R.id.quickDateBtn)
        val spinnerRecurrence = findViewById<Spinner>(R.id.spinnerRecurrence)
        val checkIsEvent = findViewById<CheckBox>(R.id.checkIsEvent)
        val layoutEventOptions = findViewById<LinearLayout>(R.id.layoutEventOptions)
        
        val layoutPhase2 = findViewById<LinearLayout>(R.id.layoutPhase2)
        val layoutPhase3 = findViewById<LinearLayout>(R.id.layoutPhase3)
        val btnAddPhase = findViewById<MaterialButton>(R.id.btnAddPhase)
        val btnRemovePhase2 = findViewById<ImageButton>(R.id.btnRemovePhase2)
        val btnRemovePhase3 = findViewById<ImageButton>(R.id.btnRemovePhase3)

        val etDaysPhase1 = findViewById<EditText>(R.id.etDaysPhase1)
        val btnCalPhase1 = findViewById<ImageButton>(R.id.btnCalPhase1)
        val spinnerPhase1Quadrant = findViewById<Spinner>(R.id.spinnerPhase1Quadrant)
        
        val etDaysPhase2 = findViewById<EditText>(R.id.etDaysPhase2)
        val btnCalPhase2 = findViewById<ImageButton>(R.id.btnCalPhase2)
        val spinnerPhase2Quadrant = findViewById<Spinner>(R.id.spinnerPhase2Quadrant)
        
        val etDaysPhase3 = findViewById<EditText>(R.id.etDaysPhase3)
        val btnCalPhase3 = findViewById<ImageButton>(R.id.btnCalPhase3)
        val spinnerPhase3Quadrant = findViewById<Spinner>(R.id.spinnerPhase3Quadrant)
        
        val btnAddReminder = findViewById<MaterialButton>(R.id.btnAddReminder)
        val calendarCheck = findViewById<CheckBox>(R.id.quickCalendarCheck)
        val save = findViewById<Button>(R.id.quickSave)
        val titleText = findViewById<TextView>(R.id.quickTitle)
        val btnDelete = findViewById<ImageButton>(R.id.btnDeleteTask)
        containerReminders = findViewById(R.id.containerReminders)

        val quadrantLabels = listOf("Hazlo ya", "Planifica", "Delega", "Elimina")
        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, quadrantLabels)
        spinnerPhase1Quadrant.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, quadrantLabels)
        spinnerPhase2Quadrant.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, quadrantLabels)
        spinnerPhase3Quadrant.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, quadrantLabels)
        
        spinnerRecurrence.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("No se repite", "Diario", "Semanal", "Mensual"))

        checkIsEvent.setOnCheckedChangeListener { _, isChecked ->
            layoutEventOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
            layoutBaseQuadrant.visibility = if (isChecked) View.GONE else View.VISIBLE
        }

        btnAddPhase.setOnClickListener {
            if (layoutPhase2.visibility == View.GONE) {
                layoutPhase2.visibility = View.VISIBLE
            } else if (layoutPhase3.visibility == View.GONE) {
                layoutPhase3.visibility = View.VISIBLE
                btnAddPhase.visibility = View.GONE
            }
        }

        btnRemovePhase2.setOnClickListener {
            layoutPhase2.visibility = View.GONE
            btnAddPhase.visibility = View.VISIBLE
        }
        btnRemovePhase3.setOnClickListener {
            layoutPhase3.visibility = View.GONE
            btnAddPhase.visibility = View.VISIBLE
        }

        val taskId = intent.getLongExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, -1L)
        val incomingQuadrant = Quadrant.fromKey(intent.getStringExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT))

        fun updateDateButtonText() {
            if (selectedDueAt == null) dateBtn.text = "Sin fecha"
            else {
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateBtn.text = sdf.format(Date(selectedDueAt!!))
            }
        }

        fun renderReminders() {
            containerReminders.removeAllViews()
            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            selectedReminders.sort()
            selectedReminders.forEach { time ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 8, 0, 8) }
                val tvTime = TextView(this).apply { text = "🔔 " + sdf.format(Date(time)); setTextColor(ContextCompat.getColor(this@QuickAddActivity, R.color.text_primary)); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f) }
                val btnRemove = ImageButton(this).apply { setImageResource(android.R.drawable.ic_menu_close_clear_cancel); background = null; setColorFilter(Color.GRAY); setOnClickListener { selectedReminders.remove(time); renderReminders() } }
                row.addView(tvTime); row.addView(btnRemove); containerReminders.addView(row)
            }
        }

        fun showCalendarForPhase(editText: EditText) {
            if (selectedDueAt == null) {
                Toast.makeText(this, "Primero selecciona la fecha del evento principal", Toast.LENGTH_SHORT).show()
                return
            }
            val cal = Calendar.getInstance()
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val eventCal = Calendar.getInstance().apply {
                    timeInMillis = selectedDueAt!!
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val diffMillis = eventCal.timeInMillis - chosenCal.timeInMillis
                val days = TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
                
                if (days < 0) {
                    Toast.makeText(this, "La fecha debe ser antes del evento", Toast.LENGTH_SHORT).show()
                } else {
                    editText.setText(days.toString())
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        btnCalPhase1.setOnClickListener { showCalendarForPhase(etDaysPhase1) }
        btnCalPhase2.setOnClickListener { showCalendarForPhase(etDaysPhase2) }
        btnCalPhase3.setOnClickListener { showCalendarForPhase(etDaysPhase3) }

        if (taskId != -1L) {
            val task = repo.getAll().firstOrNull { it.id == taskId }
            if (task != null) {
                titleText.text = "Editar tarea"
                input.setText(task.title)
                quadrantSpinner.setSelection(task.quadrant.ordinal)
                selectedDueAt = task.dueAt
                selectedReminders.clear(); selectedReminders.addAll(task.reminders)
                save.text = "ACTUALIZAR"
                calendarCheck.visibility = View.GONE 
                
                checkIsEvent.isChecked = task.isEvent
                etDaysPhase1.setText(task.advanceNoticeDays1.toString())
                spinnerPhase1Quadrant.setSelection(task.quadrantPhase1.ordinal)
                
                if (task.advanceNoticeDays2 != null && task.quadrantPhase2 != null) {
                    layoutPhase2.visibility = View.VISIBLE
                    etDaysPhase2.setText(task.advanceNoticeDays2.toString())
                    spinnerPhase2Quadrant.setSelection(task.quadrantPhase2.ordinal)
                } else {
                    layoutPhase2.visibility = View.GONE
                }

                if (task.advanceNoticeDays3 != null && task.quadrantPhase3 != null) {
                    layoutPhase3.visibility = View.VISIBLE
                    etDaysPhase3.setText(task.advanceNoticeDays3.toString())
                    spinnerPhase3Quadrant.setSelection(task.quadrantPhase3.ordinal)
                    btnAddPhase.visibility = View.GONE
                } else {
                    layoutPhase3.visibility = View.GONE
                }
                
                spinnerRecurrence.setSelection(task.recurrence.ordinal)
                
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    repo.deleteTask(taskId)
                    ReminderScheduler.cancel(this, taskId)
                    CloudSync.uploadTasks(this)
                    EisenhowerWidgetProvider.updateAllWidgets(this)
                    CalendarWidgetProvider.updateAllWidgets(this)
                    finish()
                    Toast.makeText(this, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            quadrantSpinner.setSelection(incomingQuadrant.ordinal)
            spinnerPhase1Quadrant.setSelection(Quadrant.DELEGATE.ordinal)
            spinnerPhase2Quadrant.setSelection(Quadrant.SCHEDULE.ordinal)
            spinnerPhase3Quadrant.setSelection(Quadrant.DO_FIRST.ordinal)
        }
        
        updateDateButtonText()
        renderReminders()

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

        btnAddReminder.setOnClickListener {
            val cal = Calendar.getInstance()
            if (selectedDueAt != null) cal.timeInMillis = selectedDueAt!!
            val datePicker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year); cal.set(Calendar.MONTH, month); cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hourOfDay); cal.set(Calendar.MINUTE, minute); cal.set(Calendar.SECOND, 0)
                    selectedReminders.add(cal.timeInMillis)
                    renderReminders()
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        input.requestFocus()

        save.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isBlank()) { Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

            val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
            val isEvent = checkIsEvent.isChecked
            
            val days1 = etDaysPhase1.text.toString().toIntOrNull() ?: 30
            val quad1 = Quadrant.values()[spinnerPhase1Quadrant.selectedItemPosition]
            
            val days2 = if (layoutPhase2.visibility == View.VISIBLE) (etDaysPhase2.text.toString().toIntOrNull() ?: 7) else null
            val quad2 = if (layoutPhase2.visibility == View.VISIBLE) Quadrant.values()[spinnerPhase2Quadrant.selectedItemPosition] else null
            
            val days3 = if (layoutPhase3.visibility == View.VISIBLE) (etDaysPhase3.text.toString().toIntOrNull() ?: 0) else null
            val quad3 = if (layoutPhase3.visibility == View.VISIBLE) Quadrant.values()[spinnerPhase3Quadrant.selectedItemPosition] else null

            if (isEvent) {
                if (days2 != null && days2 > days1) {
                    Toast.makeText(this, "La Fase 2 no puede tener más días que la Fase 1", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (days3 != null && days3 > (days2 ?: days1)) {
                    Toast.makeText(this, "La Fase 3 no puede tener más días que la fase anterior", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            val recurrence = RecurrenceType.values()[spinnerRecurrence.selectedItemPosition]
            
            var recDayOfWeek: Int? = null
            var recDayOfMonth: Int? = null
            if (selectedDueAt != null) {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDueAt!! }
                if (recurrence == RecurrenceType.WEEKLY) recDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (recurrence == RecurrenceType.MONTHLY) recDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            }

            if (taskId != -1L) {
                val existingTask = repo.getAll().firstOrNull { it.id == taskId }
                if (existingTask != null) {
                    val updatedTask = existingTask.copy(
                        title = title, quadrant = quadrant, dueAt = selectedDueAt, reminders = selectedReminders,
                        isEvent = isEvent, 
                        advanceNoticeDays1 = days1, quadrantPhase1 = quad1,
                        advanceNoticeDays2 = days2, quadrantPhase2 = quad2,
                        advanceNoticeDays3 = days3, quadrantPhase3 = quad3,
                        recurrence = recurrence, recurrenceDayOfWeek = recDayOfWeek, recurrenceDayOfMonth = recDayOfMonth
                    )
                    repo.updateTask(updatedTask)
                    ReminderScheduler.schedule(this, updatedTask)
                }
            } else {
                var calendarId: Long? = null
                if (calendarCheck.isChecked && selectedDueAt != null) {
                    calendarId = insertCalendarEvent(title, selectedDueAt!!)
                }
                val newTask = repo.addTask(Task(
                    id = 0, title = title, quadrant = quadrant, dueAt = selectedDueAt, calendarEventId = calendarId,
                    reminders = selectedReminders, isEvent = isEvent, 
                    advanceNoticeDays1 = days1, quadrantPhase1 = quad1,
                    advanceNoticeDays2 = days2, quadrantPhase2 = quad2,
                    advanceNoticeDays3 = days3, quadrantPhase3 = quad3,
                    recurrence = recurrence, recurrenceDayOfWeek = recDayOfWeek, recurrenceDayOfMonth = recDayOfMonth
                ))
                if (newTask != null) ReminderScheduler.schedule(this, newTask)
            }

            CloudSync.uploadTasks(this)
            EisenhowerWidgetProvider.updateAllWidgets(this)
            CalendarWidgetProvider.updateAllWidgets(this)
            finish()
        }
    }

    // ARREGLO: Usamos CalendarHelper para buscar el ID real
    private fun insertCalendarEvent(title: String, dueAt: Long): Long? {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        
        val calId = CalendarHelper.getWritableCalendarId(this)
        if (calId == -1L) {
            Toast.makeText(this, "No se encontró un calendario editable", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, dueAt); put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.CALENDAR_ID, calId) // Usamos el ID real
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) { return null }
    }
}
