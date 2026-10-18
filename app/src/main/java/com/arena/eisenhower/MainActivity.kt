package com.arena.eisenhower

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.TextView
import android.widget.Toast
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

        // Botones de "+" para añadir tareas
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

    override fun onResume() { super.onResume(); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this) }

    private fun refreshTasks() {
        val allTasks = repo.getAll()
        
        // Pintar fondos de los cuadrantes
        findViewById<MaterialCardView>(R.id.cardQ1).setCardBackgroundColor(settings.colorQ1)
        findViewById<MaterialCardView>(R.id.cardQ2).setCardBackgroundColor(settings.colorQ2)
        findViewById<MaterialCardView>(R.id.cardQ3).setCardBackgroundColor(settings.colorQ3)
        findViewById<MaterialCardView>(R.id.cardQ4).setCardBackgroundColor(settings.colorQ4)

        findViewById<TextView>(R.id.titleQ1View).text = settings.titleQ1
        findViewById<TextView>(R.id.titleQ2View).text = settings.titleQ2
        findViewById<TextView>(R.id.titleQ3View).text = settings.titleQ3
        findViewById<TextView>(R.id.titleQ4View).text = settings.titleQ4

        // Adaptamos el color para que las tareas se vean blancas y resalten
        val taskColor = android.graphics.Color.parseColor("#FFFFFF")
        setupRecyclerView(rvQ1, Quadrant.DO_FIRST, taskColor, allTasks)
        setupRecyclerView(rvQ2, Quadrant.SCHEDULE, taskColor, allTasks)
        setupRecyclerView(rvQ3, Quadrant.DELEGATE, taskColor, allTasks)
        setupRecyclerView(rvQ4, Quadrant.ELIMINATE, taskColor, allTasks)
    }

    private fun setupRecyclerView(rv: RecyclerView, quadrant: Quadrant, color: Int, allTasks: List<Task>) {
        val tasks = allTasks.filter { it.quadrant == quadrant }.sortedBy { it.order }.toMutableList()
        
        val adapter = TaskAdapter(tasks, color, 
            onCheckClick = { task -> 
                repo.toggleTask(task.id); refreshTasks(); EisenhowerWidgetProvider.updateAllWidgets(this); CloudSync.uploadTasks(this) 
            },
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
                for (i in tasks.indices) { repo.updateOrder(tasks[i].id, i + 1) }
                EisenhowerWidgetProvider.updateAllWidgets(this@MainActivity)
                CloudSync.uploadTasks(this@MainActivity)
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
