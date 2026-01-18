package com.example.ipccallertestapp

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var step = 0

    companion object {
        const val HUB_PACKAGE = "com.example.ipchubtestapp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаем UI программно для простоты
        logTextView = TextView(this).apply { textSize = 14f; setPadding(30, 30, 30, 30) }
        scrollView = ScrollView(this).apply { addView(logTextView) }
        setContentView(scrollView)

        addLog("Автоматический запуск IPC мониторинга...")

        startAutoCycle()
    }

    private fun startAutoCycle() {
        val runnable = object : Runnable {
            override fun run() {
                when (step % 3) {
                    0 -> runContentProviderIPC()
                    1 -> runServiceIPC()
//                    2 -> runBroadcastIPC()
                }
                step++
                handler.postDelayed(this, 15000) // Повтор через 15 секунд
            }
        }
        handler.post(runnable)
    }

    private fun runContentProviderIPC() {
        addLog("[Step $step] Запрос к ContentProvider...")
        try {
            val uri = Uri.parse("content://${HUB_PACKAGE}.provider/state")
            contentResolver.query(uri, null, null, null, null)?.use {
                addLog(" -> Успешно: Данные получены")
            }
        } catch (e: Exception) { addLog(" -> Ошибка CP: ${e.message}") }
    }

    private fun runServiceIPC() {
        addLog("[Step $step] Запуск Service...")
        try {
            val intent = Intent().apply {
                component = ComponentName(HUB_PACKAGE, "${HUB_PACKAGE}.CommandCenterService")
                putExtra("command", "AUTO_PING_${step}")
            }
            startService(intent)
            addLog(" -> Успешно: Интент в Service отправлен")
        } catch (e: Exception) { addLog(" -> Ошибка Service: ${e.message}") }
    }

//    private fun runBroadcastIPC() {
//        addLog("[Step $step] Отправка Broadcast...")
//        val intent = Intent("${HUB_PACKAGE}.action.NIGHT_MODE")
//        sendBroadcast(intent)
//        addLog(" -> Успешно: Broadcast разослан")
//    }

    private fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logTextView.append("\n$time: $msg")
        scrollView.post { scrollView.fullScroll(android.view.View.FOCUS_DOWN) }
    }
}