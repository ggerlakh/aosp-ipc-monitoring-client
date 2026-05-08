package com.example.ipccallertestapp

import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import android.net.Uri
import android.content.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var buttonContainer: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    
    private var boundService: IBinder? = null
    private var isServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            boundService = service
            isServiceBound = true
            addLog("Service bound successfully")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isServiceBound = false
            addLog("Service disconnected unexpectedly")
        }
    }

    companion object {
        const val HUB_PACKAGE = "com.example.ipchubtestapp"
        const val PROVIDER_AUTHORITY = "${HUB_PACKAGE}.provider"
        const val PROVIDER_URI = "content://$PROVIDER_AUTHORITY/state"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Создаем UI программно
        setupUI()
        
        addLog("IPC Test App готов к работе")
        addLog("Нажмите на кнопки для тестирования различных IPC операций")
    }
    
    private fun setupUI() {
        // Основной контейнер
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Кнопки для ContentProvider
        buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 60, 30, 30)
        }
        
        // Кнопка Query
        Button(this).apply {
            text = "1. ContentProvider - QUERY"
            setOnClickListener { performQuery() }
            buttonContainer.addView(this)
        }
        
        // Кнопка Insert
        Button(this).apply {
            text = "2. ContentProvider - INSERT"
            setOnClickListener { performInsert() }
            buttonContainer.addView(this)
        }
        
        // Кнопка Update
        Button(this).apply {
            text = "3. ContentProvider - UPDATE"
            setOnClickListener { performUpdate() }
            buttonContainer.addView(this)
        }
        
        // Кнопка Delete
        Button(this).apply {
            text = "4. ContentProvider - DELETE"
            setOnClickListener { performDelete() }
            buttonContainer.addView(this)
        }
        
        // Кнопка Call
        Button(this).apply {
            text = "5. ContentProvider - CALL"
            setOnClickListener { performCall() }
            buttonContainer.addView(this)
        }
        
        // Разделитель
        addDivider()
        
        // Кнопка StartService
        Button(this).apply {
            text = "6. Service - START SERVICE"
            setOnClickListener { performStartService() }
            buttonContainer.addView(this)
        }
        
        // Кнопка BindService
        Button(this).apply {
            text = "7. Service - BIND SERVICE"
            setOnClickListener { performBindService() }
            buttonContainer.addView(this)
        }
        
        // Кнопка UnbindService
        Button(this).apply {
            text = "8. Service - UNBIND SERVICE"
            setOnClickListener { performUnbindService() }
            buttonContainer.addView(this)
        }
        
        // Логи
        logTextView = TextView(this).apply {
            textSize = 12f
            setPadding(30, 20, 30, 20)
            text = "=== IPC TEST APP LOGS ===\n\n"
        }
        
        scrollView = ScrollView(this).apply {
            addView(logTextView)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // Вес для заполнения оставшегося места
            )
        }
        
        mainLayout.addView(buttonContainer)
        mainLayout.addView(scrollView)
        setContentView(mainLayout)
    }
    
    private fun addDivider() {
        val divider = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            )
            setBackgroundColor(android.graphics.Color.GRAY)
            setPadding(0, 20, 0, 20)
        }
        buttonContainer.addView(divider)
        
        // Добавляем отступ после разделителя
        val spacer = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                10
            )
        }
        buttonContainer.addView(spacer)
    }
    
    // ContentProvider операции
    
    private fun performQuery() {
        addLog("[QUERY] Query to ContentProvider...")
        try {
            val uri = Uri.parse(PROVIDER_URI)
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val rowCount = cursor.count
                if (rowCount > 0 && cursor.moveToFirst()) {
                    val columns = cursor.columnNames.joinToString(", ")
                    addLog("QUERY succeded: Found $rowCount rows")
                    addLog("   columns: $columns")
                    
                    // Показываем первую строку для примера
                    if (rowCount > 0) {
                        val firstRow = (0 until cursor.columnCount)
                            .map { "${cursor.getColumnName(it)}: ${cursor.getString(it)}" }
                            .joinToString(", ")
                        addLog("   First row data example: $firstRow")
                    }
                } else {
                    addLog("QUERY succeded: No data")
                }
            } ?: addLog("QUERY returned null")
        } catch (e: SecurityException) {
            addLog("QUERY access error: ${e.message}")
        } catch (e: Exception) {
            addLog("QUERY error: ${e.message}")
        }
    }
    
    private fun performInsert() {
        addLog("[INSERT] Insert data in ContentProvider...")
        try {
            val uri = Uri.parse(PROVIDER_URI)
            val values = ContentValues().apply {
                put("timestamp", System.currentTimeMillis())
                put("data", "Test data ${SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
                put("source", "IPC Test App")
            }
            val resultUri = contentResolver.insert(uri, values)
            if (resultUri != null) {
                addLog("INSERT succeded: URI = $resultUri")
            } else {
                addLog("INSERT return null")
            }
        } catch (e: SecurityException) {
            addLog("INSERT access error: ${e.message}")
        } catch (e: Exception) {
            addLog("INSERT error: ${e.message}")
        }
    }
    
    private fun performUpdate() {
        addLog("[UPDATE] Update data in ContentProvider...")
        try {
            val uri = Uri.parse(PROVIDER_URI)
            val values = ContentValues().apply {
                put("data", "Updated at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
            }
            val rowsUpdated = contentResolver.update(uri, values, null, null)
            addLog("UPDATE succeded: Rows $rowsUpdated updated")
        } catch (e: SecurityException) {
            addLog("UPDATE access error: ${e.message}")
        } catch (e: Exception) {
            addLog("UPDATE error: ${e.message}")
        }
    }
    
    private fun performDelete() {
        addLog("[DELETE] Delete data from ContentProvider...")
        try {
            val uri = Uri.parse(PROVIDER_URI)
            val rowsDeleted = contentResolver.delete(uri, null, null)
            addLog("DELETE успешен: Удалено $rowsDeleted строк")
        } catch (e: SecurityException) {
            addLog("DELETE ошибка доступа: ${e.message}")
        } catch (e: Exception) {
            addLog("DELETE ошибка: ${e.message}")
        }
    }
    
    private fun performCall() {
        addLog("[CALL] Invoke custom method ContentProvider...")
        try {
            val uri = Uri.parse(PROVIDER_URI)
            val method = "getStatus"
            val arg = null
            val extras = null
            
            // Пробуем разные варианты вызова
            val result = contentResolver.call(uri, method, arg, extras)
            if (result != null) {
                addLog("CALL succeded: Результат = $result")
            } else {
                addLog("CALL returned null (возможно метод не поддерживается)")
            }
        } catch (e: SecurityException) {
            addLog("CALL access error: ${e.message}")
        } catch (e: Exception) {
            addLog("CALL error: ${e.message}")
        }
    }
    
    // Service операции
    
    private fun performStartService() {
        addLog("[START_SERVICE] Start Service...")
        try {
            val intent = Intent().apply {
                component = ComponentName(HUB_PACKAGE, "${HUB_PACKAGE}.CommandCenterService")
                putExtra("command", "START_SERVICE_CMD")
                putExtra("timestamp", System.currentTimeMillis())
                putExtra("caller", "IPC Test App")
            }
            startService(intent)
            addLog("START_SERVICE: Intent send")
        } catch (e: Exception) {
            addLog("START_SERVICE error: ${e.message}")
        }
    }
    
    private fun performBindService() {
        if (isServiceBound) {
            addLog("[BIND_SERVICE] Service already binded")
            return
        }
        
        addLog("[BIND_SERVICE] bind to Service...")
        try {
            val intent = Intent().apply {
                component = ComponentName(HUB_PACKAGE, "${HUB_PACKAGE}.CommandCenterService")
                putExtra("command", "BIND_SERVICE_CMD")
            }
            val bound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                addLog("BIND_SERVICE: bind performed")
            } else {
                addLog("BIND_SERVICE: falied to bind")
            }
        } catch (e: Exception) {
            addLog("BIND_SERVICE error: ${e.message}")
        }
    }
    
    private fun performUnbindService() {
        if (!isServiceBound) {
            addLog("[UNBIND_SERVICE] Service not binded")
            return
        }
        
        addLog("[UNBIND_SERVICE] Отвязка от Service...")
        try {
            unbindService(serviceConnection)
            isServiceBound = false
            boundService = null
            addLog("UNBIND_SERVICE: Successuflly unbinded")
        } catch (e: Exception) {
            addLog("UNBIND_SERVICE error: ${e.message}")
        }
    }
    
    
    private fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        handler.post {
            logTextView.append("[$time] $msg\n")
            scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Отвязываем сервис при уничтожении активности
        if (isServiceBound) {
            unbindService(serviceConnection)
        }
    }
}