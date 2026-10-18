package com.arena.eisenhower

import android.content.res.Configuration
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onCheckClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.rowCheck)
        val title: TextView = view.findViewById(R.id.rowTitle)
        val meta: TextView = view.findViewById(R.id.rowMeta)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_task_main, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val context = holder.itemView.context
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        holder.check.isChecked = task.done
        val titleText = SpannableString(task.title)
        if (task.done) titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        holder.title.text = titleText
        
        // Colores dinámicos de texto
        holder.title.setTextColor(if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A"))
        holder.meta.setTextColor(if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#475569"))
        
        holder.meta.text = "Orden: ${task.order} · ${DueDateUtils.format(task.dueAt)}"
        holder.btnDelete.visibility = View.GONE

        holder.check.setOnClickListener { onCheckClick(task) }
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = tasks.size
}
