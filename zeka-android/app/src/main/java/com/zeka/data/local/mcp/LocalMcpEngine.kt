package com.zeka.data.local.mcp

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object LocalMcpEngine {

    fun readContacts(context: Context, searchQuery: String? = null): String {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            return "{\"error\": \"READ_CONTACTS permission is not granted. Lütfen rehber okuma izni verin.\"}"
        }
        
        val jsonArray = JSONArray()
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        val selection = if (!searchQuery.isNullOrBlank()) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        } else null
        val selectionArgs = if (!searchQuery.isNullOrBlank()) {
            arrayOf("%$searchQuery%")
        } else null
        
        val cursor = resolver.query(uri, projection, selection, selectionArgs, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            var count = 0
            while (it.moveToNext() && count < 30) {
                val name = it.getString(nameIndex)
                val number = it.getString(numberIndex)
                jsonArray.put(JSONObject().apply {
                    put("name", name)
                    put("number", number)
                })
                count++
            }
        }
        return jsonArray.toString()
    }

    fun readCalendarEvents(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) 
            != PackageManager.PERMISSION_GRANTED) {
            return "{\"error\": \"READ_CALENDAR permission is not granted. Lütfen takvim okuma izni verin.\"}"
        }
        
        val jsonArray = JSONArray()
        val resolver = context.contentResolver
        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.EVENT_LOCATION
        )
        
        val now = System.currentTimeMillis()
        val end = now + (24 * 60 * 60 * 1000)
        
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), end.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        
        val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            val titleIndex = it.getColumnIndex(CalendarContract.Events.TITLE)
            val startIndex = it.getColumnIndex(CalendarContract.Events.DTSTART)
            val locIndex = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
            var count = 0
            while (it.moveToNext() && count < 20) {
                val title = it.getString(titleIndex)
                val startTime = it.getLong(startIndex)
                val location = it.getString(locIndex) ?: "Belirtilmemiş"
                
                jsonArray.put(JSONObject().apply {
                    put("title", title)
                    put("start_time", Date(startTime).toString())
                    put("location", location)
                })
                count++
            }
        }
        return jsonArray.toString()
    }
}
