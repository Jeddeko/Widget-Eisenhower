package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.Collections

class MainActivity : Activity() {
    private lateinit var repo: TaskRepository
    private lateinit var settings: AppSettings

    private lateinit var rvQ1: RecyclerView; private lateinit var rvQ2: RecyclerView
    private lateinit var rvQ3: RecyclerView; private lateinit var rvQ4: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = TaskRepository(this); settings = AppSettings(this)

        rvQ1 = findViewById(R.id.listQ1); rvQ2 = findViewById(R.id.listQ2)
        rvQ3 = findViewById(R.id.listQ3); rvQ4 = findViewById(R.id.listQ4)

        findViewById<MaterialButton>(R.id.btnAddQ1).setOnClickListener { openQuickAdd(Quadrant.DO_FIRST) }
        findViewById<MaterialButton>(R.id.btnAddQ2).setOnClickListener { openQuickAdd(Quadrant.SCHEDULE) }
        findViewById<MaterialButton>(R.id.btnAddQ3).setOnClickListener { openQuickAdd(Quadrant.DELEGATE) }
        findViewById<MaterialButton>(R.id.btnAddQ4).setOnClickListener { openQuickAdd(Quadrant.ELIMINATE) }

        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        askPermissions()
    }

    private fun openQuickAdd(quadrant: Quadrant) {
        val intent = Intent(this, QuickAddActivity::class.java)
        intent.putExtra(EisenhowerWidgetProvider.EXTRA_QUADRANT, quadrant.key)
        startActivity(intent)
    }

    override fun onResume() { 
        super.onResume(); 
        refreshTasks(); 
        EisenhowerWidgetProvider.updateAllWidgets(this) 
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    private fun refreshTasks() {
        val allTasks = repo.getAll()
        
        // Configurar cada cuadrante
        setupQuadrantUI(Quadrant.DO_FIRST, R.id.cardQ1, R.id.titleQ1View, R.id.btnAddQ1, R.id.headerCompletedQ1, R.id.containerCompletedQ1, rvQ1, settings.colorQ1, settings.titleQ1, allTasks)
        setupQuadrantUI(Quadrant.SCHEDULE, R.id.cardQ2, R.id.titleQ2View, R.id.btnAddQ2, R.id.headerCompletedQ2, R.id.containerCompletedQ2, rvQ2, settings.colorQ2, settings.titleQ2, allTasks)
        setupQuadrantUI(Quadrant.DELEGATE, R.id.cardQ3, R.id.titleQ3View, R.id.btnAddQ3, R.id.headerCompletedQ3, R.id.containerCompletedQ3, rvQ3, settings.colorQ3, settings.titleQ3, allTasks)
        setupQuadrantUI(Quadrant.ELIMINATE, R.id.cardQ4, R.id.titleQ4View, R.id.btnAddQ4, R.id.headerCompletedQ4, R.id.containerCompletedQ4, rvQ4, settings.colorQ4, settings.titleQ4, allTasks)
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

        val quadrantTasks = allTasks.filter { it.quadrant == quadrant }
        val activeTasks = quadrantTasks.filter { !it.done }.sortedBy { it.order }.toMutableList()
        val completedTasks = quadrantTasks.filter { it.done }.sortedByDescending { it.updatedAt }

        val activeAdapter = TaskAdapter(activeTasks, color, 
            onCheckClick = { task -> 
                repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this); CloudSync.uploadTasks(this) 
            },
            onItemClick = { task -> openQuickAddEdit(task) }
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
                    repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this); CloudSync.uploadTasks(this) 
                }
                completedContainer.addView(row)
            }
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos < activeTasks.size && toPos < activeTasks.size) {
                    Collections.swap(activeTasks, fromPos, toPos)
                    activeAdapter.notifyItemMoved(fromPos, toPos)
                    return true
                }
                return false
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                for (i in activeTasks.indices) { repo.updateOrder(activeTasks[i].id, i + 1) }
                EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity)
                CloudSync.uploadTasks(this@MainActivity)
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
