package com.example.ipchubtestapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this).apply {
            text = "HUB SERVER: Ожидание IPC запросов..."
            textSize = 18f
            setPadding(50, 500, 50, 50)
        }
        setContentView(tv)
    }
}