package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
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

        quadrantSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Hazlo ya", "Planifica", "Delega", "Elimina")
        )

        dueSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            DueDateUtils.labels.toList()
        )

        val incoming = Quadrant.fromKey(
            intent.getStringExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT)
        )
        quadrantSpinner.setSelection(incoming.ordinal)

        input.requestFocus()

        save.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isBlank()) {
                Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
            val dueAt = DueDateUtils.resolve(dueSpinner.selectedItemPosition)
            
            // Insertar en el calendario si el usuario dejó la casilla marcada
            var calendarId: Long? = null
            if (calendarCheck.isChecked && dueAt != null) {
                calendarId = insertCalendarEvent(title, dueAt)
            }

            repo.addTask(title, quadrant, dueAt, calendarId)
            EisenhowerWidgetProvider.updateAllWidgets(this)
            finish()
        }
    }

    private fun insertCalendarEvent(title: String, dueAt: Long): Long? {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Sin permiso de calendario", Toast.LENGTH_SHORT).show()
            return null
        }
        
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, dueAt)
                put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000) // 1 hora de duración
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, "Tarea creada desde Eisenhower Widget")
                put(CalendarContract.Events.CALENDAR_ID, 1) 
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri: Uri? = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
