package com.example.ipchubtestapp

import android.content.*
import android.database.*
import android.net.Uri

class HomeStateProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor {
        val cursor = MatrixCursor(arrayOf("key", "value"))
        cursor.addRow(arrayOf("vacation_mode", "active"))
        cursor.addRow(arrayOf("temp_celsius", "22.5"))
        return cursor
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, v: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, sa: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, sa: Array<out String>?): Int = 0
}