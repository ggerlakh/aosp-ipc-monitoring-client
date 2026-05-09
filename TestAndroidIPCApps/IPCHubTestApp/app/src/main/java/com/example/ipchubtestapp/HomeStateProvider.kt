package com.example.ipchubtestapp

import android.content.*
import android.database.MatrixCursor
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log

class HomeStateProvider : ContentProvider() {
    
    companion object {
        private const val TAG = "HomeStateProvider"
        const val AUTHORITY = "com.example.ipchubtestapp.provider"
        val CONTENT_URI = Uri.parse("content://$AUTHORITY/state")
        
        // Call методы
        const val CALL_METHOD_GET_STATUS = "getStatus"
        const val CALL_METHOD_ECHO = "echo"
        const val CALL_METHOD_PING = "ping"
    }
    
    override fun onCreate(): Boolean {
        Log.d(TAG, "ContentProvider создан")
        return true
    }
    
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.d(TAG, "QUERY method")
        Log.d(TAG, "   URI: $uri")
        Log.d(TAG, "   Projection: ${projection?.joinToString() ?: "null"}")
        Log.d(TAG, "   Selection: $selection")
        Log.d(TAG, "   SelectionArgs: ${selectionArgs?.joinToString() ?: "null"}")
        Log.d(TAG, "   SortOrder: $sortOrder")
        
        // Создаем тестовый курсор с данными
        val cursor = MatrixCursor(arrayOf("key", "value", "timestamp"))
        cursor.addRow(arrayOf("vacation_mode", "active", System.currentTimeMillis().toString()))
        cursor.addRow(arrayOf("temp_celsius", "22.5", System.currentTimeMillis().toString()))
        cursor.addRow(arrayOf("test_response", "Query работает!", System.currentTimeMillis().toString()))
        
        Log.d(TAG, "   Returned ${cursor.count} rows")
        return cursor
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        Log.d(TAG, "INSERT method")
        Log.d(TAG, "   URI: $uri")
        
        if (values != null) {
            for (key in values.keySet()) {
                Log.d(TAG, "   $key = ${values.getAsString(key)}")
            }
        } else {
            Log.d(TAG, "   values = null")
        }
        
        // Возвращаем URI с сгенерированным ID
        val insertedId = System.currentTimeMillis()
        val resultUri = Uri.parse("$CONTENT_URI/$insertedId")
        Log.d(TAG, "   Return URI: $resultUri")
        
        return resultUri
    }
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        Log.d(TAG, "UPDATE method")
        Log.d(TAG, "   URI: $uri")
        
        if (values != null) {
            for (key in values.keySet()) {
                Log.d(TAG, "   $key = ${values.getAsString(key)}")
            }
        } else {
            Log.d(TAG, "   values = null")
        }
        
        Log.d(TAG, "   Selection: $selection")
        Log.d(TAG, "   SelectionArgs: ${selectionArgs?.joinToString() ?: "null"}")
        
        // Возвращаем количество обновленных строк (тестовое)
        val updatedCount = 1
        Log.d(TAG, "   Rows updated: $updatedCount")
        
        return updatedCount
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        Log.d(TAG, "DELETE вызван")
        Log.d(TAG, "   URI: $uri")
        Log.d(TAG, "   Selection: $selection")
        Log.d(TAG, "   SelectionArgs: ${selectionArgs?.joinToString() ?: "null"}")
        
        // Возвращаем количество удаленных строк (тестовое)
        val deletedCount = 1
        Log.d(TAG, "   Rows deleted: $deletedCount")
        
        return deletedCount
    }
    
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        Log.d(TAG, "CALL method")
        Log.d(TAG, "   Method: $method")
        Log.d(TAG, "   Arg: $arg")
        
        if (extras != null) {
            Log.d(TAG, "   Extras: $extras")
        }
        
        val result = Bundle()
        
        when (method) {
            CALL_METHOD_GET_STATUS -> {
                result.putString("status", "active")
                result.putString("message", "ContentProvider работает нормально")
                result.putLong("timestamp", System.currentTimeMillis())
                result.putString("version", "1.0")
                Log.d(TAG, "   Return data: status=active")
            }
            
            CALL_METHOD_ECHO -> {
                val echoMessage = arg ?: "nothing to echo"
                result.putString("echo", echoMessage)
                result.putLong("timestamp", System.currentTimeMillis())
                Log.d(TAG, "   Return data: echo=$echoMessage")
            }
            
            CALL_METHOD_PING -> {
                result.putString("pong", "OK")
                result.putLong("timestamp", System.currentTimeMillis())
                Log.d(TAG, "   Return data: pong=OK")
            }
            
            else -> {
                result.putString("result", "Method '$method' not implemented")
                result.putBoolean("success", false)
                Log.d(TAG, "   Unknown method: $method")
            }
        }
        
        return result
    }
    
    override fun getType(uri: Uri): String? = null
}