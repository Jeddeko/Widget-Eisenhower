package com.arena.eisenhower

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

object CalendarHelper {
    fun getWritableCalendarId(context: Context): Long {
        // Verificar permisos primero
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return -1L
        }

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY
        )
        
        // Buscar calendarios donde tengamos permiso de contribuidor (escritura)
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                var primaryId = -1L
                do {
                    val id = it.getLong(0)
                    val isPrimary = it.getInt(1) == 1
                    if (isPrimary) {
                        primaryId = id
                        break
                    }
                    if (primaryId == -1L) {
                        primaryId = id
                    }
                } while (it.moveToNext())
                return primaryId
            }
        }
        return -1L
    }
}
