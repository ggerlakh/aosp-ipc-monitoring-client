package com.example.ipchubtestapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Binder
import android.util.Log
import android.content.Context

class CommandCenterService : Service() {
    companion object {
        private const val TAG = "CommandCenterService"
        const val HUB_PACKAGE = "com.example.ipchubtestapp"
        const val CALLER_PACKAGE = "com.example.ipccallertestapp"
    }

    // Mock binder
    private val binder = object : Binder() {
        fun getService(): CommandCenterService = this@CommandCenterService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("command") ?: "PING"
        val message = "IPC Service Start: Received command $cmd"
        Log.d(TAG, message)
        runBroadcastIPC()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "onBind called")
        Log.d(TAG, "Intent: $intent")
        return binder
    }
    
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind called")
        Log.d(TAG, "Intent: $intent")
        return false // Не требуется onRebind
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private fun runBroadcastIPC() {
        val intent = Intent("${HUB_PACKAGE}.action.NIGHT_MODE")
        intent.setPackage(CALLER_PACKAGE)
        sendBroadcast(intent)
    }
}