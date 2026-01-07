package com.example.ipcmonitorclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


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
    private lateinit var etTargetPackages: EditText
    private lateinit var swMonitor: Switch

    private var ipcMonitorReceiver: IpcMonitorReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()

        // 1. Привязываем логику ресивера к UI
        ipcMonitorReceiver = IpcMonitorReceiver { json ->
            renderIpcEvent(json)
        }

        // 2. Логика управления настройками AOSP
        swMonitor.setOnCheckedChangeListener { _, isChecked ->
            saveSettings(isChecked, etTargetPackages.text.toString())
            val status = if (isChecked) "ON" else "OFF"
            Toast.makeText(this, "Monitor $status", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings(enabled: Boolean, packages: String) {
        try {
            val resolver = contentResolver
            // Записываем флаг включения
            Settings.Global.putInt(resolver, "ipc_monitor_enabled", if (enabled) 1 else 0)
            // Записываем список пакетов (например: "com.android.gallery3d,com.android.contacts")
            Settings.Global.putString(resolver, "ipc_monitor_targets", packages.ifEmpty { "*" })
        } catch (e: Exception) {
            Toast.makeText(this, "Error! ${e}", Toast.LENGTH_LONG).show()
        }
    }

    private fun renderIpcEvent(json: JSONObject) {
        runOnUiThread {
            val type = json.optString("type")
            val sender = json.optString("sender")
            val receiver = json.optString("receiver")
            val payload = json.optJSONObject("payload")
            val uri = payload?.optString("uri") ?: "no-uri"
            val timestamp = json.optLong("timestamp")

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

            // Формируем красивый лог
            val entry = "[$time] TYPE $type \n" +
                    "FROM: $sender\n" +
                    "TO:   $receiver\n" +
                    "payload:  $payload\n" +
                    "---------------------------\n"

            logTextView.append(entry)

            // Автопрокрутка вниз
            scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onStart() {
        super.onStart()
        ipcMonitorReceiver?.let {
            registerReceiver(it, IntentFilter("com.custom.aosp.IPC_MONITOR_EVENT"), Context.RECEIVER_EXPORTED)
        }
    }

    override fun onStop() {
        super.onStop()
        ipcMonitorReceiver?.let { unregisterReceiver(it) }
    }

    private fun setupUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 500, 30, 30)
        }

        swMonitor = Switch(this).apply {
            text = "Enable IPC Monitoring "
            textSize = 18f
        }

        etTargetPackages = EditText(this).apply {
            hint = "Target packages (comma separated or *)"
            setText("*")
        }

        val btnClear = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { logTextView.text = "" }
        }

        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        logTextView = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.BLACK)
            movementMethod = ScrollingMovementMethod()
        }

        root.addView(swMonitor)
        root.addView(etTargetPackages)
        root.addView(btnClear)
        scrollView.addView(logTextView)
        root.addView(scrollView)
        setContentView(root)
    }
}