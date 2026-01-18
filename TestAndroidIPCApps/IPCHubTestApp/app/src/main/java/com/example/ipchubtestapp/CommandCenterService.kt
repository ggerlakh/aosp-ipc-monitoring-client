package com.example.ipchubtestapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.content.Context

class CommandCenterService : Service() {
    companion object {
        const val HUB_PACKAGE = "com.example.ipchubtestapp"
        const val CALLER_PACKAGE = "com.example.ipccallertestapp"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cmd = intent?.getStringExtra("command") ?: "PING"
        val message = "IPC Service Start: Received command $cmd"
        Log.d("Hub", message)
        runBroadcastIPC()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun runBroadcastIPC() {
        val intent = Intent("${HUB_PACKAGE}.action.NIGHT_MODE")
        intent.setPackage(CALLER_PACKAGE)
        sendBroadcast(intent)
    }
}