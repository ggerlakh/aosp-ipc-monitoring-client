package com.example.ipccallertestapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * Этот класс отвечает за прием широковещательных сообщений (Broadcasts).
 * Он "просыпается", когда в системе пролетает Intent с нужным Action.
 */
class LightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Проверяем, тот ли это экшен, который мы ждем
        if (intent.action == "com.example.ipchubtestapp.action.NIGHT_MODE") {

            val message = "LightReceiver: Получен BroadcastReceiver от ${intent.`package`}."

            // Логируем для отладки
            Log.d("SmartLight", message)

            // Показываем уведомление пользователю
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}