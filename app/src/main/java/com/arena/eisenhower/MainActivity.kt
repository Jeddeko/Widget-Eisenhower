package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class MainActivity : Activity() {
    private lateinit var repo: TaskRepository
    private lateinit var quadrantSpinner: Spinner
    private lateinit var dueSpinner: Spinner
    private lateinit var taskInput: EditText
    private lateinit var tasksContainer: LinearLayout
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = TaskRepository(this); settings = AppSettings(this)

        quadrantSpinner = findViewById(R.id.quadrantSpinner); dueSpinner = findViewById(R.id.dueSpinner)
        taskInput = findViewById(R.id.taskInput); tasksContainer = findViewById(R.id.tasksContainer)

        quadrantSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Hazlo ya", "Planifica", "Delega", "Elimina"))
        dueSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, DueDateUtils.labels.toList())

        findViewById<Button>(R.id.btnAdd).setOnClickListener { addTask() }
        findViewById<Button>(R.id.btnWidget).setOnClickListener { EisenhowerWidgetProvider.updateAllWidgets(this); Toast.makeText(this, "Widget actualizado", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<Button>(R.id.btnNotify).setOnClickListener { askPermissions() }
        findViewById<Button>(R.id.btnTestNotify).setOnClickListener {
            askPermissions()
            val testTask = Task(id = -1L, title = "¡Prueba de notificación!", quadrant = Quadrant.DO_FIRST, dueAt = System.currentTimeMillis() + 3600000)
            NotificationHelper.showReminder(this, listOf(testTask), 0)
            Toast.makeText(this, "Notificación enviada", Toast.LENGTH_SHORT).show()
        }
        askPermissions()
    }

    override fun onResume() { super.onResume(); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }

    private fun addTask() {
        val title = taskInput.text.toString().trim()
        if (title.isBlank()) { Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show(); return }
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
                put(CalendarContract.Events.DTSTART, dueAt); put(CalendarContract.Events.DTEND, dueAt + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title); put(CalendarContract.Events.CALENDAR_ID, 1); put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            return contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) { return null }
    }

    private fun refreshTasks() {
        tasksContainer.removeAllViews()
        val allTasks = repo.getAll()
        val quadrants = listOf(
            Triple(Quadrant.DO_FIRST, settings.titleQ1, settings.colorQ1), Triple(Quadrant.SCHEDULE, settings.titleQ2, settings.colorQ2),
            Triple(Quadrant.DELEGATE, settings.titleQ3, settings.colorQ3), Triple(Quadrant.ELIMINATE, settings.titleQ4, settings.colorQ4)
        )
        val inflater = LayoutInflater.from(this)

        quadrants.forEach { (quadrant, titleStr, color) ->
            tasksContainer.addView(TextView(this).apply { text = titleStr; setTextColor(Color.parseColor("#0F172A")); textSize = 18f; setTypeface(null, android.graphics.Typeface.BOLD); setPadding(16, 24, 16, 8) })
            val quadrantTasks = allTasks.filter { it.quadrant == quadrant }.sortedBy { it.order }

            if (quadrantTasks.isEmpty()) {
                tasksContainer.addView(TextView(this).apply { text = "No hay tareas aquí."; setTextColor(Color.parseColor("#64748B")); textSize = 14f; setPadding(16, 4, 16, 16) })
            } else {
                quadrantTasks.forEachIndexed { index, task ->
                    val row = inflater.inflate(R.layout.row_task_main, tasksContainer, false)
                    row.background = GradientDrawable().apply { setColor(color); cornerRadius = 8f }
                    
                    val check = row.findViewById<CheckBox>(R.id.rowCheck); val title = row.findViewById<TextView>(R.id.rowTitle)
                    val meta = row.findViewById<TextView>(R.id.rowMeta); val delete = row.findViewById<Button>(R.id.btnDelete)
                    delete.visibility = View.GONE
                    
                    row.addView(Button(this).apply { text = "⬆️"; textSize = 12f; setOnClickListener { if (index > 0) { val prev = quadrantTasks[index - 1]; repo.updateOrder(task.id, prev.order); repo.updateOrder(prev.id, task.order); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity) } } })
                    row.addView(Button(this).apply { text = "⬇️"; textSize = 12f; setOnClickListener { if (index < quadrantTasks.size - 1) { val next = quadrantTasks[index + 1]; repo.updateOrder(task.id, next.order); repo.updateOrder(next.id, task.order); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity) } } })

                    check.isChecked = task.done
                    val titleText = SpannableString(task.title); if (task.done) titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    title.text = titleText; meta.text = "Orden: ${task.order} · ${DueDateUtils.format(task.dueAt)}"
                    check.setOnClickListener { repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }
                    tasksContainer.addView(row)
                }
            }
        }
    }

    private fun askPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CALENDAR)
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.WRITE_CALENDAR)
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 42)
    }
}
