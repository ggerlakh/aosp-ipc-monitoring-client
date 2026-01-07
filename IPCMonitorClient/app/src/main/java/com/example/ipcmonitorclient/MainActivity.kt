package com.example.ipcmonitorclient

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.json.JSONObject
import android.util.Log
import com.example.ipcmonitorclient.IpcMonitorReceiver.Companion.TAG

//class NetworkChangeReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if (isNetworkAvailable(context)) {
//            Toast.makeText(context, "Network is available >>>>>>>>>", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(context, "Network is not available", Toast.LENGTH_SHORT).show()
//        }
//    }
//    private fun isNetworkAvailable(context: Context): Boolean {
//        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val activeNetworkInfo = connectivityManager.activeNetworkInfo
//        return activeNetworkInfo != null && activeNetworkInfo.isConnected
//    }
//}

class IpcMonitorReceiver(
    private val onDataReceived: (JSONObject) -> Unit
) : BroadcastReceiver() {

    companion object {
        const val ACTION_IPC_MONITOR = "com.custom.aosp.IPC_MONITOR_EVENT"
        private const val TAG = "IpcMonitorReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_IPC_MONITOR) {
            val jsonRaw = intent.getStringExtra("ipc_data")

            Log.d(TAG, "Processing IpcMonitorReceiver.onReceive, jsonRaw = ${jsonRaw}")

            if (jsonRaw != null) {
                try {
                    val jsonObject = JSONObject(jsonRaw)
                    // Передаем распарсенный объект во внешний обработчик
                    onDataReceived(jsonObject)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга IPC JSON: ${e.message}")
                }
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView

    // Ссылка на наш кастомный ресивер
    // private var ipcMonitorReceiver: IpcMonitorReceiver? = null

    private lateinit var ipcMonitorReceiver: IpcMonitorReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()

        // Инициализируем ресивер и передаем логику обработки JSON
        ipcMonitorReceiver = IpcMonitorReceiver { json ->
            renderIpcEvent(json)
        }
    }

    private fun renderIpcEvent(json: JSONObject) {
        // Всегда обновляем UI в основном потоке
        runOnUiThread {
            Log.d("IpcMonitorReceiver", "Processing MainActivity.renderIpcEvent")
            val type = json.optString("type", "UNKNOWN")
            val sender = json.optString("sender", "n/a")
            val receiver = json.optString("receiver", "n/a")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

            val sb = StringBuilder("[$time] TYPE: $type\n")
            sb.append("SENDER: $sender\n")
            sb.append("TARGET: $receiver\n")

            // Добавляем специфичные поля в зависимости от типа
            when (type) {
                "ContentProvider" -> sb.append("AUTH: ${json.optJSONObject("payload").optString("authority")}\n")
                "Broadcast" -> sb.append("ACTION: ${json.optString("action")}\n")
            }
            sb.append("\n")

            appendLog(sb.toString(), type)
        }
    }

    private fun appendLog(text: String, type: String) {
        Log.d("IpcMonitorReceiver", "Processing MainActivity.appendLog")
        val color = when (type) {
            "Broadcast" -> Color.parseColor("#2E7D32") // Зеленый
            "ContentProvider" -> Color.parseColor("#1565C0") // Синий
            else -> Color.BLACK
        }

        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundColorSpan(color), 0, text.indexOf("\n"), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        logTextView.append(spannable)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onStart() {
        Log.d("IpcMonitorReceiver", "Processing MainActivity.onStart")
        super.onStart()
        // Регистрация ресивера
        // ipcMonitorReceiver?.let {
        //     val filter = IntentFilter(IpcMonitorReceiver.ACTION_IPC_MONITOR)
        //     registerReceiver(it, filter, Context.RECEIVER_EXPORTED)
        // }

        val filter = IntentFilter(IpcMonitorReceiver.ACTION_IPC_MONITOR)
        registerReceiver(ipcMonitorReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onStop() {
        Log.d("IpcMonitorReceiver", "Processing MainActivity.onStop")
        super.onStop()
        // Отмена регистрации для предотвращения утечек памяти
        // ipcMonitorReceiver?.let { unregisterReceiver(it) }
        
        unregisterReceiver(ipcMonitorReceiver)
    }

    private fun setupUI() {
        Log.d("IpcMonitorReceiver", "Processing MainActivity.setupUI")
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        val btnClear = Button(this).apply {
            text = "Clear"
            setOnClickListener { logTextView.text = "" }
        }
        scrollView = ScrollView(this)
        logTextView = TextView(this).apply { textSize = 12f }

        scrollView.addView(logTextView)
        root.addView(btnClear)
        root.addView(scrollView)
        setContentView(root)
    }
}