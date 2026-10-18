package com.arena.eisenhower

import android.content.res.ColorStateList
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
    private val quadrantColor: Int,
    private val onCheckClick: (Task) -> Unit,
    private val onItemClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.rowCheck)
        val title: TextView = view.findViewById(R.id.rowTitle)
        val meta: TextView = view.findViewById(R.id.rowMeta)
        val btnDelete: View = view.findViewById(R.id.btnDelete)
    }

    // Función para saber si un color es oscuro
    private fun isColorDark(color: Int): Boolean {
        val darkness = 1.0 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness > 0.5
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_task_main, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        val isDark = isColorDark(quadrantColor)
        
        // El color del texto cambia según el fondo
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val subTextColor = if (isDark) Color.parseColor("#D1D5DB") else Color.parseColor("#4B5563") // Gris claro u oscuro
        val checkColor = if (isDark) Color.WHITE else Color.BLACK

        holder.check.isChecked = task.done
        holder.check.buttonTintList = ColorStateList.valueOf(checkColor)
        
        val titleText = SpannableString(task.title)
        if (task.done) titleText.setSpan(StrikethroughSpan(), 0, titleText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        holder.title.text = titleText
        holder.title.setTextColor(textColor)
        
        holder.meta.setTextColor(subTextColor)
        holder.meta.text = "Orden: ${task.order} · ${DueDateUtils.format(task.dueAt)}"
        
        holder.btnDelete.visibility = View.GONE

        holder.check.setOnClickListener { onCheckClick(task) }
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = tasks.size
}
