package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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

    private fun refreshTasks() {
        val allTasks = repo.getAll()
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // Pintar fondos de los cuadrantes
        findViewById<MaterialCardView>(R.id.cardQ1).setCardBackgroundColor(settings.colorQ1)
        findViewById<MaterialCardView>(R.id.cardQ2).setCardBackgroundColor(settings.colorQ2)
        findViewById<MaterialCardView>(R.id.cardQ3).setCardBackgroundColor(settings.colorQ3)
        findViewById<MaterialCardView>(R.id.cardQ4).setCardBackgroundColor(settings.colorQ4)

        val textColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val secondaryColor = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569")

        findViewById<TextView>(R.id.titleQ1View).apply { text = settings.titleQ1; setTextColor(textColor) }
        findViewById<TextView>(R.id.titleQ2View).apply { text = settings.titleQ2; setTextColor(textColor) }
        findViewById<TextView>(R.id.titleQ3View).apply { text = settings.titleQ3; setTextColor(textColor) }
        findViewById<TextView>(R.id.titleQ4View).apply { text = settings.titleQ4; setTextColor(textColor) }

        setupQuadrant(rvQ1, Quadrant.DO_FIRST, R.id.headerCompletedQ1, R.id.containerCompletedQ1, allTasks, isDark, textColor, secondaryColor)
        setupQuadrant(rvQ2, Quadrant.SCHEDULE, R.id.headerCompletedQ2, R.id.containerCompletedQ2, allTasks, isDark, textColor, secondaryColor)
        setupQuadrant(rvQ3, Quadrant.DELEGATE, R.id.headerCompletedQ3, R.id.containerCompletedQ3, allTasks, isDark, textColor, secondaryColor)
        setupQuadrant(rvQ4, Quadrant.ELIMINATE, R.id.headerCompletedQ4, R.id.containerCompletedQ4, allTasks, isDark, textColor, secondaryColor)
    }

    private fun setupQuadrant(
        rv: RecyclerView, quadrant: Quadrant, headerId: Int, containerId: Int, 
        allTasks: List<Task>, isDark: Boolean, textColor: Int, secondaryColor: Int
    ) {
        val quadrantTasks = allTasks.filter { it.quadrant == quadrant }
        val activeTasks = quadrantTasks.filter { !it.done }.sortedBy { it.order }.toMutableList()
        val completedTasks = quadrantTasks.filter { it.done }.sortedByDescending { it.updatedAt }

        // El Adaptador ahora maneja sus propios colores
        val activeAdapter = TaskAdapter(activeTasks, 
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
            completedHeader.setTextColor(secondaryColor)
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
                title.text = task.title
                title.setTextColor(textColor)
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                meta.text = DueDateUtils.format(task.dueAt)
                meta.setTextColor(secondaryColor)
                
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
