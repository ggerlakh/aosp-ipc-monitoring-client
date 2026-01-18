package com.example.ipcmonitorclient

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.json.JSONObject

/**
 * Логика форматирования данных для визуализации.
 */
object IpcDataProcessor {

    fun timestampToIsoUtc(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_INSTANT)
    }
    fun formatForUi(json: JSONObject): String {
        val type = json.optString("type", "UNKNOWN")
        val sender = json.optString("sender", "unknown")
        val receiver = json.optString("receiver", "unknown")
        val timestamp = json.optLong("timestamp")
        val payload = json.optJSONObject("payload")

        return buildString {
            append("● [$type] - ${timestampToIsoUtc(timestamp)}\n")
            append("From: $sender\n")
            append("To:   $receiver\n")

            payload?.let {
                // Превращаем payload в красивый JSON со всеми вложенными полями
                append("Data: ${it.toString(2)}")
            }
        }
    }
}