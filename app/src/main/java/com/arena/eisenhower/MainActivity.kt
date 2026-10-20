// ... (imports y código anterior igual hasta renderAppCalendar) ...

    private fun renderAppCalendar() {
        containerWeeksApp.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvAppCurrentMonth.text = monthFormat.format(appCurrentMonth.time).replaceFirstChar { it.uppercase() }

        val year = appCurrentMonth.get(Calendar.YEAR)
        val month = appCurrentMonth.get(Calendar.MONTH)
        
        val firstOfMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val firstDayOfWeek = firstOfMonth.get(Calendar.DAY_OF_WEEK) - 2 
        val offset = if (firstDayOfWeek < 0) 6 else firstDayOfWeek
        
        val monthStartMillis = firstOfMonth.timeInMillis
        val monthEndMillis = monthStartMillis + (maxDays * 24 * 60 * 60 * 1000L)
        
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val allTasks = repo.getAll()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        
        val eventsByDay = mutableMapOf<Int, MutableList<Pair<Task, Boolean>>>()
        
        allTasks.forEach { task ->
            val occurrences = task.getOccurrences(monthStartMillis, monthEndMillis)
            occurrences.forEach { time ->
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    eventsByDay.getOrPut(day) { mutableListOf() }.add(Pair(task, false))
                }
            }

            task.reminders.forEach { reminderTime ->
                // ARREGLO: Solo agregamos el recordatorio si es de hoy en adelante
                if (reminderTime >= todayStart) {
                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                    if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                        val day = cal.get(Calendar.DAY_OF_MONTH)
                        eventsByDay.getOrPut(day) { mutableListOf() }.add(Pair(task, true))
                    }
                }
            }
        }

        // ... (El resto del código de dibujo de las celdas sigue exactamente igual) ...
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
                            set(Calendar.YEAR, year); set(Calendar.MONTH, month); set(Calendar.DAY_OF_MONTH, dayNumber)
                            set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                        }
                        val intent = Intent(this, QuickAddActivity::class.java)
                        intent.putExtra(CalendarWidgetProvider.EXTRA_DUE_AT, cal.timeInMillis)
                        startActivity(intent)
                    }

                    val dayEvents = eventsByDay[dayNumber] ?: emptyList()
                    val sortedDayEvents = dayEvents.sortedBy { it.second }

                    sortedDayEvents.forEach { pair ->
                        val task = pair.first
                        val isReminder = pair.second
                        val eventView = inflater.inflate(R.layout.app_calendar_event, eventsContainer, false) as TextView
                        
                        if (isReminder) {
                            val remTime = task.reminders.firstOrNull { 
                                val rCal = Calendar.getInstance().apply { timeInMillis = it }
                                rCal.get(Calendar.MONTH) == month && rCal.get(Calendar.YEAR) == year && rCal.get(Calendar.DAY_OF_MONTH) == dayNumber
                            } ?: 0L
                            eventView.text = "🔔 ${timeFormat.format(Date(remTime))} - ${task.title}"
                        } else {
                            eventView.text = task.title
                        }
                        
                        val qColor = when(task.quadrant) {
                            Quadrant.DO_FIRST -> settings.colorQ1
                            Quadrant.SCHEDULE -> settings.colorQ2
                            Quadrant.DELEGATE -> settings.colorQ3
                            Quadrant.ELIMINATE -> settings.colorQ4
                        }
                        
                        val drawable = GradientDrawable().apply {
                            setColor(qColor)
                            cornerRadius = 8f
                        }
                        eventView.background = drawable
                        eventView.setTextColor(if (isColorDark(qColor)) Color.WHITE else Color.BLACK)
                        
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

// ... (El resto del código sigue igual) ...
