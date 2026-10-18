package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var repo: TaskRepository
    private lateinit var quadrantSpinner: Spinner
    private lateinit var taskInput: EditText
    private lateinit var settings: AppSettings
    private var selectedDueAt: Long? = null
    private lateinit var mainDateBtn: MaterialButton

    private lateinit var rvQ1: RecyclerView
    private lateinit var rvQ2: RecyclerView
    private lateinit var rvQ3: RecyclerView
    private lateinit var rvQ4: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = TaskRepository(this); settings = AppSettings(this)

        quadrantSpinner = findViewById(R.id.quadrantSpinner)
        taskInput = findViewById(R.id.taskInput)
        mainDateBtn = findViewById(R.id.mainDateBtn)

        rvQ1 = findViewById(R.id.listQ1); rvQ2 = findViewById(R.id.listQ2)
        rvQ3 = findViewById(R.id.listQ3); rvQ4 = findViewById(R.id.listQ4)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))

        // Lógica del botón de fecha principal
        mainDateBtn.setOnClickListener {
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
                    updateMainDateBtnText()
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
                timePicker.show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            
            datePicker.setButton(android.app.Dialog.BUTTON_NEGATIVE, "Borrar") { _, _ ->
                selectedDueAt = null
                updateMainDateBtnText()
            }
            datePicker.show()
        }

        findViewById<Button>(R.id.btnAdd).setOnClickListener { addTask() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        askPermissions()
    }

    private fun updateMainDateBtnText() {
        if (selectedDueAt == null) {
            mainDateBtn.text = "Sin recordatorio"
        } else {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            mainDateBtn.text = sdf.format(Date(selectedDueAt!!))
        }
    }

    override fun onResume() { super.onResume(); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }

    private fun addTask() {
        val title = taskInput.text.toString().trim()
        if (title.isBlank()) { Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show(); return }
        val quadrant = Quadrant.values()[quadrantSpinner.selectedItemPosition]
        
        var calendarId: Long? = null
        if (selectedDueAt != null) calendarId = insertCalendarEvent(title, selectedDueAt!!)
        
        val task = repo.addTask(title, quadrant, selectedDueAt, calendarId)
        
        // Programar alarma exacta
        if (task != null) {
            ReminderScheduler.schedule(this, task)
        }

        taskInput.setText("")
        selectedDueAt = null
        updateMainDateBtnText()
        refreshTasks()
        EisenhowerWidgetProvider.updateAllWidgets(this)
    }

    private fun insertCalendarEvent(title: String, dueAt: Long): Long? {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, dueAt); put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title); put(CalendarContract.Events.CALENDAR_ID, 1); put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            return contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) { return null }
    }

    private fun refreshTasks() {
        val allTasks = repo.getAll()
        findViewById<TextView>(R.id.titleQ1View).text = settings.titleQ1
        findViewById<TextView>(R.id.titleQ2View).text = settings.titleQ2
        findViewById<TextView>(R.id.titleQ3View).text = settings.titleQ3
        findViewById<TextView>(R.id.titleQ4View).text = settings.titleQ4

        setupRecyclerView(rvQ1, Quadrant.DO_FIRST, settings.colorQ1, allTasks)
        setupRecyclerView(rvQ2, Quadrant.SCHEDULE, settings.colorQ2, allTasks)
        setupRecyclerView(rvQ3, Quadrant.DELEGATE, settings.colorQ3, allTasks)
        setupRecyclerView(rvQ4, Quadrant.ELIMINATE, settings.colorQ4, allTasks)
    }

    private fun setupRecyclerView(rv: RecyclerView, quadrant: Quadrant, color: Int, allTasks: List<Task>) {
        val tasks = allTasks.filter { it.quadrant == quadrant }.sortedBy { it.order }.toMutableList()
        
        val adapter = TaskAdapter(tasks, color, 
            onCheckClick = { task -> repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) },
            onItemClick = { task -> 
                val editIntent = Intent(this, QuickAddActivity::class.java).apply {
                    putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
                    putExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT, task.quadrant.key)
                }
                startActivity(editIntent)
            }
        )
        
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                Collections.swap(tasks, fromPos, toPos)
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                for (i in tasks.indices) {
                    repo.updateOrder(tasks[i].id, i + 1)
                }
                EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity)
            }
        })
        itemTouchHelper.attachToRecyclerView(rv)
    }

    private fun askPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CALENDAR)
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.WRITE_CALENDAR)
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 42)
    }
}
