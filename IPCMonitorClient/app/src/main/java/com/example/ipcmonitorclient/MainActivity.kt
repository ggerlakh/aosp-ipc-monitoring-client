package com.example.ipcmonitorclient

import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.ipcmonitorclient.databinding.ActivityMainBinding
import org.json.JSONObject
import android.provider.Settings
import android.widget.Toast
import android.graphics.Color
import android.content.Context

class MainActivity : AppCompatActivity(), IpcWebSocketListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wsManager: WebSocketManager
    private var ipcReceiver: IpcMonitorReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wsManager = WebSocketManager(this)

        ipcReceiver = IpcMonitorReceiver { jsonObject ->
            processAndDisplayData(jsonObject)
        }

        setupUI()
        setupIpcReceiver()
    }

    private fun setupUI() {
        // WebSocket Connect
        binding.btnConnect.setOnClickListener {
            val url = binding.etUrl.text.toString()
            if (wsManager.isSocketConnected()) {
                wsManager.disconnect()
            } else {
                if (url.isNotEmpty()) wsManager.connect(url)
            }
        }

        // Инициализация Switch (читаем текущее состояние из Settings.Global)
        try {
            val currentStatus = Settings.Global.getInt(contentResolver, "itmo_yandex.ipc.monitoring_enabled", 0)
            val currentPackages = Settings.Global.getString(contentResolver, "itmo_yandex.ipc.monitoring_packages") ?: "*"

            binding.swAospMonitor.isChecked = (currentStatus == 1)
//            if (currentPackages.isNotEmpty() && currentPackages != "*") {
            if (currentPackages.isNotEmpty()) {
                binding.etFilter.setText(currentPackages)
            }
        } catch (e: SecurityException) {
            appendLogSystem("Error reading settings: Need WRITE_SECURE_SETTINGS permission!")
        }

        // Слушатель переключения Switch
        binding.swAospMonitor.setOnCheckedChangeListener { _, isChecked ->
            // Берем текст из поля фильтра как список пакетов
            val packages = binding.etFilter.text.toString().trim()

            // Сохраняем в системные настройки
            saveSettings(isChecked, packages)

            val stateText = if (isChecked) "ENABLED" else "DISABLED"
            appendLogSystem("Monitor $stateText for targets: ${packages.ifEmpty { "ALL" }}")
        }
    }

    /**
     * Метод для записи настроек в системную таблицу Settings.Global
     */
    private fun saveSettings(enabled: Boolean, packages: String) {
        try {
            val resolver = contentResolver
            // Записываем флаг включения (1 или 0)
            Settings.Global.putInt(resolver, "itmo_yandex.ipc.monitoring_enabled", if (enabled) 1 else 0)

            // Записываем список пакетов (если пусто -> "*")
            Settings.Global.putString(resolver, "itmo_yandex.ipc.monitoring_packages", packages.ifEmpty { "*" })

        } catch (e: Exception) {
            val errorMsg = "Failed to save settings: ${e.message}"
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            appendLogSystem(errorMsg)
        }
    }

    private fun processAndDisplayData(json: JSONObject) {
        val uiText = IpcDataProcessor.formatForUi(json)
        val prefix = if (wsManager.isSocketConnected()) "[WS:LIVE]" else "[WS:OFF]"

        runOnUiThread {
            binding.tvLogs.append("\n----------------\n$prefix $uiText")
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }

        wsManager.send(json.toString())
    }

    private fun appendLogSystem(msg: String) {
        runOnUiThread {
            binding.tvLogs.append("\n>>> SYSTEM: $msg")
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun setupIpcReceiver() {
        val filter = IntentFilter(IpcMonitorReceiver.ACTION_IPC_MONITOR)
        registerReceiver(ipcReceiver, filter, Context.RECEIVER_EXPORTED)
    }

    // --- WebSocket Callbacks ---
    override fun onStatusChanged(connected: Boolean) {
        runOnUiThread {
            if (connected) {
                binding.tvStatus.text = "Status: Connected"
                binding.tvStatus.setTextColor(Color.GREEN)
                binding.btnConnect.text = "Disconnect"
            } else {
                binding.tvStatus.text = "Status: Disconnected"
                binding.tvStatus.setTextColor(Color.GRAY)
                binding.btnConnect.text = "Connect"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ipcReceiver)
        wsManager.disconnect()
    }
}