package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var repo: TaskRepository
    private lateinit var settings: AppSettings

    private lateinit var rvQ1: RecyclerView
    private lateinit var rvQ2: RecyclerView
    private lateinit var rvQ3: RecyclerView
    private lateinit var rvQ4: RecyclerView

    private val selectedTaskIds = mutableSetOf<Long>()
    private lateinit var selectionHeader: LinearLayout
    private lateinit var normalHeader: LinearLayout
    private lateinit var tvSelectionCount: TextView

    private lateinit var viewMatrix: View
    private lateinit var viewCalendar: View
    
    private var appCurrentMonth = Calendar.getInstance()
    private lateinit var tvAppCurrentMonth: TextView
    private lateinit var containerWeeksApp: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = TaskRepository(this)
        settings = AppSettings(this)

        rvQ1 = findViewById(R.id.listQ1)
        rvQ2 = findViewById(R.id.listQ2)
        rvQ3 = findViewById(R.id.listQ3)
        rvQ4 = findViewById(R.id.listQ4)

        selectionHeader = findViewById(R.id.selectionHeader)
        normalHeader = findViewById(R.id.normalHeader)
        tvSelectionCount = findViewById(R.id.tvSelectionCount)
        
        viewMatrix = findViewById(R.id.viewMatrix)
        viewCalendar = findViewById(R.id.viewCalendar)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        tvAppCurrentMonth = findViewById(R.id.tvAppCurrentMonth)
        containerWeeksApp = findViewById(R.id.containerWeeksApp)
        
        findViewById<ImageButton>(R.id.btnAppPrevMonth).setOnClickListener {
            appCurrentMonth.add(Calendar.MONTH, -1)
            renderAppCalendar()
        }
        
        findViewById<ImageButton>(R.id.btnAppNextMonth).setOnClickListener {
            appCurrentMonth.add(Calendar.MONTH, 1)
            renderAppCalendar()
        }

        findViewById<FloatingActionButton>(R.id.fabAddTaskCalendar).setOnClickListener {
            val intent = Intent(this, QuickAddActivity::class.java)
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }
            intent.putExtra(CalendarWidgetProvider.EXTRA_DUE_AT, cal.timeInMillis)
            startActivity(intent)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_matrix -> {
                    viewMatrix.visibility = View.VISIBLE
                    viewCalendar.visibility = View.GONE
                    refreshTasks()
                    true
                }
                R.id.nav_calendar -> {
                    viewMatrix.visibility = View.GONE
                    viewCalendar.visibility = View.VISIBLE
                    renderAppCalendar()
                    true
                }
                else -> false
            }
        }

        findViewById<MaterialButton>(R.id.btnAddQ1).setOnClickListener { openQuickAdd(Quadrant.DO_FIRST) }
        findViewById<MaterialButton>(R.id.btnAddQ2).setOnClickListener { openQuickAdd(Quadrant.SCHEDULE) }
        findViewById<MaterialButton>(R.id.btnAddQ3).setOnClickListener { openQuickAdd(Quadrant.DELEGATE) }
        findViewById<MaterialButton>(R.id.btnAddQ4).setOnClickListener { openQuickAdd(Quadrant.ELIMINATE) }

        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        
        findViewById<ImageButton>(R.id.btnDeleteSelected).setOnClickListener {
            if (selectedTaskIds.isNotEmpty()) {
                selectedTaskIds.forEach { id -> repo.deleteTask(id) }
                CloudSync.uploadTasks(this)
                selectedTaskIds.clear()
                updateSelectionHeader()
                refreshTasks()
                EisenhowerWidgetProvider.updateAllWidgets(this)
                CalendarWidgetProvider.updateAllWidgets(this)
                Toast.makeText(this, "Tareas eliminadas", Toast.LENGTH_SHORT).show()
            }
        }

        askPermissions()
    }

    private fun openQuickAdd(quadrant: Quadrant) {
        val intent = Intent(this, QuickAddActivity::class.java)
        intent.putExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT, quadrant.key)
        startActivity(intent)
    }

    override fun onResume() { 
        super.onResume() 
        refreshTasks()
        EisenhowerWidgetProvider.updateAllWidgets(this) 
        if (viewCalendar.visibility == View.VISIBLE) {
            renderAppCalendar()
        }
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    private fun updateSelectionHeader() {
        if (selectedTaskIds.isEmpty()) {
            selectionHeader.visibility = View.GONE
            normalHeader.visibility = View.VISIBLE
        } else {
            selectionHeader.visibility = View.VISIBLE
            normalHeader.visibility = View.GONE
            tvSelectionCount.text = "${selectedTaskIds.size} seleccionada(s)"
        }
    }

    private fun refreshTasks() {
        val allTasks = repo.getAll().filter { it.getEffectiveQuadrant() != null }
        
        setupQuadrantUI(Quadrant.DO_FIRST, R.id.cardQ1, R.id.titleQ1View, R.id.btnAddQ1, R.id.headerCompletedQ1, R.id.containerCompletedQ1, rvQ1, settings.colorQ1, settings.titleQ1, allTasks)
        setupQuadrantUI(Quadrant.SCHEDULE, R.id.cardQ2, R.id.titleQ2View, R.id.btnAddQ2, R.id.headerCompletedQ2, R.id.containerCompletedQ2, rvQ2, settings.colorQ2, settings.titleQ2, allTasks)
        setupQuadrantUI(Quadrant.DELEGATE, R.id.cardQ3, R.id.titleQ3View, R.id.btnAddQ3, R.id.headerCompletedQ3, R.id.containerCompletedQ3, rvQ3, settings.colorQ3, settings.titleQ3, allTasks)
        setupQuadrantUI(Quadrant.ELIMINATE, R.id.cardQ4, R.id.titleQ4View, R.id.btnAddQ4, R.id.headerCompletedQ4, R.id.containerCompletedQ4, rvQ4, settings.colorQ4, settings.titleQ4, allTasks)
    }

    private fun renderAppCalendar() {
        containerWeeksApp.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvAppCurrentMonth.text = monthFormat.format(appCurrentMonth.time).replaceFirstChar { it.uppercase() }

        val year = appCurrentMonth.get(Calendar.YEAR)
        val month = appCurrentMonth.get(Calendar.MONTH)
        
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 2 
        val offset = if (firstDayOfWeek < 0) 6 else firstDayOfWeek
        
        val monthStartMillis = firstOfMonth.timeInMillis
        val monthEndMillis = monthStartMillis + (maxDays * 24 * 60 * 60 * 1000L)
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val allTasks = repo.getAll()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val eventsByDay = mutableMapOf<Int, MutableList<Triple<Task, Boolean, Boolean>>>()
        
        allTasks.forEach { task ->
            val occurrences = task.getOccurrences(monthStartMillis, monthEndMillis)
            occurrences.forEach { time ->
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    
                    if (task.done) {
                        val completionCal = Calendar.getInstance().apply { timeInMillis = task.updatedAt }
                        val dueCal = Calendar.getInstance().apply { timeInMillis = task.dueAt ?: task.updatedAt }
                        
                        val isSameDay = completionCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR) && 
                                        completionCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR)

                        if (cal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR) && isSameDay) {
                            eventsByDay.getOrPut(day) { mutableListOf() }.add(Triple(task, false, true))
                        } else if (cal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)) {
                            eventsByDay.getOrPut(day) { mutableListOf() }.add(Triple(task, false, true))
                        } else if (cal.get(Calendar.DAY_OF_YEAR) == completionCal.get(Calendar.DAY_OF_YEAR)) {
                            eventsByDay.getOrPut(day) { mutableListOf() }.add(Triple(task, true, true))
                        }
                    } else {
                        eventsByDay.getOrPut(day) { mutableListOf() }.add(Triple(task, false, false))
                    }
                }
            }

            task.reminders.forEach { reminderTime ->
                if (reminderTime >= todayStart) {
                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                    if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        eventsByDay.getOrPut(day) { mutableListOf() }.add(Triple(task, true, false))
                    }
                }
            }
        }

        val inflater = LayoutInflater.from(this)
        val totalCells = offset + maxDays
        val totalWeeks = Math.ceil(totalCells / 7.0).toInt()

        for (w in 0 until totalWeeks) {
            val weekView = inflater.inflate(R.layout.app_calendar_week_row, containerWeeksApp, false)
            
            for (d in 0 until 7) {
                val cellIndex = (w * 7) + d
                val dayNumber = cellIndex - offset + 1
                
                val cellId = resources.getIdentifier("cell_${d+1}", "id", packageName)
                val cellLayout = weekView.findViewById<LinearLayout>(cellId)
                
                if (dayNumber in 1..maxDays) {
                    val dayCellView = inflater.inflate(R.layout.app_calendar_day_cell, cellLayout, false)
                    val tvDayNumber = dayCellView.findViewById<TextView>(R.id.tvDayNumber)
                    val eventsContainer = dayCellView.findViewById<LinearLayout>(R.id.eventsContainer)
                    
                    tvDayNumber.text = dayNumber.toString()
                    tvDayNumber.setTextColor(Color.parseColor(if (settings.isDarkMode) "#F8FAFC" else "#0F172A"))

                    val todayCal = Calendar.getInstance()
                    if (todayCal.get(Calendar.YEAR) == year && todayCal.get(Calendar.MONTH) == month && todayCal.get(Calendar.DAY_OF_MONTH) == dayNumber) {
                        val drawable = ContextCompat.getDrawable(this, R.drawable.bg_calendar_cell_today)
                        tvDayNumber.background = drawable
                        tvDayNumber.setTextColor(Color.WHITE)
                    } else {
                        tvDayNumber.background = null
                    }

                    if (settings.isDarkMode) {
                        dayCellView.setBackgroundColor(Color.parseColor("#1E293B"))
                    } else {
                        dayCellView.setBackgroundColor(Color.parseColor("#FFFFFF"))
                    }

                    dayCellView.setOnClickListener {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                            set(Calendar.HOUR_OF_DAY, 9)
                            set(Calendar.MINUTE, 0)
                        }
                        val intent = Intent(this, QuickAddActivity::class.java)
                        intent.putExtra(CalendarWidgetProvider.EXTRA_DUE_AT, cal.timeInMillis)
                        startActivity(intent)
                    }

                    val dayEvents = eventsByDay[dayNumber] ?: emptyList()
                    val sortedDayEvents = dayEvents.sortedBy { it.second }

                    sortedDayEvents.forEach { triple ->
                        val task = triple.first
                        val isReminder = triple.second
                        val isDone = triple.third
                        val eventView = inflater.inflate(R.layout.app_calendar_event, eventsContainer, false) as TextView
                        
                        var displayText = ""
                        if (isReminder) {
                            val remTime = task.reminders.firstOrNull { 
                                val rCal = Calendar.getInstance().apply { timeInMillis = it }
                                rCal.get(Calendar.MONTH) == month && rCal.get(Calendar.YEAR) == year && rCal.get(Calendar.DAY_OF_MONTH) == dayNumber
                            } ?: 0L
                            displayText = "🔔 ${timeFormat.format(Date(remTime))} - ${task.title}"
                        } else {
                            if (isDone && task.dueAt != null) {
                                val dueCal = Calendar.getInstance().apply { 
                                    timeInMillis = task.dueAt 
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val completionCal = Calendar.getInstance().apply { 
                                    timeInMillis = task.updatedAt 
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val currentCellCal = Calendar.getInstance().apply { 
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                }
                                
                                if (completionCal.after(dueCal) && currentCellCal.get(Calendar.DAY_OF_YEAR) == completionCal.get(Calendar.DAY_OF_YEAR)) {
                                    val daysLate = TimeUnit.MILLISECONDS.toDays(completionCal.timeInMillis - dueCal.timeInMillis).toInt()
                                    val prefix = if (daysLate == 1) "COMPLETADO 1 DÍA DESPUÉS: " else "COMPLETADO $daysLate DÍAS DESPUÉS: "
                                    displayText = prefix + task.title
                                } else {
                                    displayText = task.title
                                }
                            } else {
                                displayText = task.title
                            }
                        }

                        eventView.text = displayText
                        
                        var qColor = when(task.quadrant) {
                            Quadrant.DO_FIRST -> settings.colorQ1
                            Quadrant.SCHEDULE -> settings.colorQ2
                            Quadrant.DELEGATE -> settings.colorQ3
                            Quadrant.ELIMINATE -> settings.colorQ4
                        }
                        
                        if (isDone && task.dueAt != null) {
                            val dueCal = Calendar.getInstance().apply { 
                                timeInMillis = task.dueAt
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0) 
                            }
                            val completionCal = Calendar.getInstance().apply { 
                                timeInMillis = task.updatedAt
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0) 
                            }
                            val currentCellCal = Calendar.getInstance().apply { 
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayNumber) 
                            }
                            
                            if (completionCal.after(dueCal) && currentCellCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)) {
                                qColor = Color.parseColor("#94A3B8")
                            }
                        }

                        val drawable = GradientDrawable().apply {
                            setColor(qColor)
                            cornerRadius = 8f
                        }
                        eventView.background = drawable
                        eventView.setTextColor(if (isColorDark(qColor)) Color.WHITE else Color.BLACK)
                        
                        if (isDone) {
                            eventView.paintFlags = eventView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        } else {
                            eventView.paintFlags = eventView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                        }
                        
                        eventView.setOnClickListener {
                            val intent = Intent(this, QuickAddActivity::class.java)
                            intent.putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
                            startActivity(intent)
                        }
                        eventsContainer.addView(eventView)
                    }
                    
                    cellLayout.addView(dayCellView)
                } else {
                    val emptyView = View(this)
                    cellLayout.addView(emptyView)
                }
            }
            containerWeeksApp.addView(weekView)
        }
    }

    private fun setupQuadrantUI(
        quadrant: Quadrant, cardId: Int, titleId: Int, btnAddId: Int, headerId: Int, containerId: Int,
        rv: RecyclerView, color: Int, titleStr: String, allTasks: List<Task>
    ) {
        val isDark = isColorDark(color)
        val mainTextColor = if (isDark) Color.WHITE else Color.BLACK
        val secondaryTextColor = if (isDark) Color.parseColor("#D1D5DB") else Color.parseColor("#4B5563")

        findViewById<MaterialCardView>(cardId).setCardBackgroundColor(color)
        findViewById<TextView>(titleId).apply { text = titleStr; setTextColor(mainTextColor) }
        findViewById<MaterialButton>(btnAddId).setTextColor(mainTextColor)

        val quadrantTasks = allTasks.filter { it.getEffectiveQuadrant() == quadrant }
        val activeTasks = quadrantTasks.filter { !it.done }.sortedBy { it.order }.toMutableList()
        val completedTasks = quadrantTasks.filter { it.done }.sortedByDescending { it.updatedAt }

        val activeAdapter = TaskAdapter(
            activeTasks, color, selectedTaskIds,
            onCheckClick = { task -> 
                repo.toggleTask(task.id)
                refreshTasks()
                EisenhowerWidgetProvider.updateAllWidgets(this)
                CloudSync.uploadTasks(this) 
            },
            onItemClick = { task -> 
                if (selectedTaskIds.isEmpty()) {
                    openQuickAddEdit(task)
                } else {
                    if (selectedTaskIds.contains(task.id)) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                    updateSelectionHeader()
                    refreshTasks()
                }
            },
            onItemLongClick = { task ->
                if (selectedTaskIds.contains(task.id)) selectedTaskIds.remove(task.id) else selectedTaskIds.add(task.id)
                updateSelectionHeader()
                refreshTasks()
            },
            onStartDrag = { }
        )
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = activeAdapter

        val completedHeader = findViewById<TextView>(headerId)
        val completedContainer = findViewById<LinearLayout>(containerId)

        if (completedTasks.isEmpty()) {
            completedHeader.visibility = View.GONE
            completedContainer.visibility = View.GONE
        } else {
            completedHeader.visibility = View.VISIBLE
            completedHeader.setTextColor(secondaryTextColor)
            completedHeader.text = "Completadas (${completedTasks.size}) ▼"
            completedContainer.visibility = View.GONE
            
            completedHeader.setOnClickListener {
                if (completedContainer.visibility == View.GONE) {
                    completedContainer.visibility = View.VISIBLE
                    completedHeader.text = "Completadas (${completedTasks.size}) ▲"
                } else {
                    completedContainer.visibility = View.GONE
                    completedHeader.text = "Completadas (${completedTasks.size}) ▼"
                }
            }

            completedContainer.removeAllViews()
            val inflater = LayoutInflater.from(this)
            completedTasks.forEach { task ->
                val row = inflater.inflate(R.layout.row_task_main, completedContainer, false)
                val check = row.findViewById<CheckBox>(R.id.rowCheck)
                val title = row.findViewById<TextView>(R.id.rowTitle)
                val meta = row.findViewById<TextView>(R.id.rowMeta)
                
                check.isChecked = true
                check.buttonTintList = android.content.res.ColorStateList.valueOf(mainTextColor)
                title.text = task.title
                title.setTextColor(mainTextColor)
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                meta.text = DueDateUtils.format(task.dueAt)
                meta.setTextColor(secondaryTextColor)
                
                check.setOnClickListener { 
                    repo.toggleTask(task.id)
                    refreshTasks()
                    EisenhowerWidgetProvider.updateAllWidgets(this)
                    CloudSync.uploadTasks(this) 
                }
                completedContainer.addView(row)
            }
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT 
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos < activeTasks.size && toPos < activeTasks.size && selectedTaskIds.isEmpty()) {
                    Collections.swap(activeTasks, fromPos, toPos)
                    activeAdapter.notifyItemMoved(fromPos, toPos)
                    return true
                }
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION && direction == ItemTouchHelper.LEFT) {
                    val taskToDelete = activeTasks[position]
                    repo.deleteTask(taskToDelete.id)
                    CloudSync.uploadTasks(this@MainActivity)
                    refreshTasks()
                    EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity)
                    Toast.makeText(this@MainActivity, "Tarea eliminada", Toast.LENGTH_SHORT).show()
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.translationX = 0f
                viewHolder.itemView.alpha = 1f
                if (selectedTaskIds.isEmpty()) {
                    for (i in activeTasks.indices) { 
                        repo.updateOrder(activeTasks[i].id, i + 1) 
                    }
                    EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity)
                    CloudSync.uploadTasks(this@MainActivity)
                }
            }

            override fun isLongPressDragEnabled(): Boolean = selectedTaskIds.isEmpty()
            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.5f

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.parseColor("#EF4444"))
                val deleteIcon: Drawable? = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_delete)
                
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)
                    if (deleteIcon != null) {
                        val iconMargin = (itemView.height - deleteIcon.intrinsicHeight) / 2
                        deleteIcon.setBounds(itemView.right - iconMargin - deleteIcon.intrinsicWidth, itemView.top + iconMargin, itemView.right - iconMargin, itemView.bottom - iconMargin)
                        deleteIcon.setTint(Color.WHITE)
                        deleteIcon.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(rv)
    }

    private fun openQuickAddEdit(task: Task) {
        val intent = Intent(this, QuickAddActivity::class.java).apply {
            putExtra(EisenhowerWidgetProvider.EXTRA_TASK_ID, task.id)
            putExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT, task.quadrant.key)
        }
        startActivity(intent)
    }

    private fun askPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.READ_CALENDAR)
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) permissions.add(Manifest.permission.WRITE_CALENDAR)
        if (permissions.isNotEmpty()) requestPermissions(permissions.toTypedArray(), 42)
    }
}
