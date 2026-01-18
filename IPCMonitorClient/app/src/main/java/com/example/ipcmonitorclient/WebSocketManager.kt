package com.example.ipcmonitorclient

import okhttp3.WebSocketListener
import okhttp3.WebSocket
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.LinkedBlockingQueue

/**
 * Управление WebSocket-соединением с буферизацией сообщений.
 */

interface IpcWebSocketListener {
    fun onStatusChanged(connected: Boolean)
}
class WebSocketManager(private val listener: IpcWebSocketListener) {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var isConnected = false

    // Очередь для хранения данных при отсутствии связи
    private val buffer = LinkedBlockingQueue<String>(2000)

    fun connect(url: String) {
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                listener.onStatusChanged(true)
                flushBuffer()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener.onStatusChanged(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener.onStatusChanged(false)
            }
        })
    }

    fun send(data: String) {
        if (isConnected) {
            webSocket?.send(data)
        } else {
            // Если сокет закрыт, сохраняем в буфер
            if (!buffer.offer(data)) {
                buffer.poll() // Удаляем старое, если очередь забита
                buffer.offer(data)
            }
        }
    }

    private fun flushBuffer() {
        while (isConnected && buffer.isNotEmpty()) {
            buffer.poll()?.let { webSocket?.send(it) }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
        isConnected = false
    }

    fun isSocketConnected() = isConnected
}