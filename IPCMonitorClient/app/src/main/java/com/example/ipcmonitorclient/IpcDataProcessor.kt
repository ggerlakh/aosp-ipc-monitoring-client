package com.example.ipcmonitorclient

import org.json.JSONObject

/**
 * Логика форматирования данных для визуализации.
 */
object IpcDataProcessor {

    fun formatForUi(json: JSONObject): String {
        val type = json.optString("type", "UNKNOWN")
        val sender = json.optString("sender", "unknown")
        val receiver = json.optString("receiver", "unknown")
        val payload = json.optJSONObject("payload")

        return buildString {
            append("● [$type]\n")
            append("From: $sender\n")
            append("To:   $receiver\n")

            payload?.let {
                // Превращаем payload в красивый JSON со всеми вложенными полями
                append("Data: ${it.toString(2)}")
            }
        }
    }
}