package com.example.ipcmonitorclient

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

/**
 * Ресивер для перехвата событий из модифицированного AOSP.
 * Принимает лямбда-функцию для передачи данных дальше.
 */
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

            Log.d(TAG, "Processing IpcMonitorReceiver.onReceive, jsonRaw = $jsonRaw")

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