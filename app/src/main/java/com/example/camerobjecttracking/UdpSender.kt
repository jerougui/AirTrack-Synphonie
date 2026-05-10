package com.example.camerobjecttracking

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class UdpSender(private val context: Context) {

    private var socket: DatagramSocket? = null
    private var scheduledFuture: ScheduledFuture<*>? = null
    private var executor: ScheduledExecutorService? = null
    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 5005
    private var isSending = false

    private var centerX: Float = 0f
    private var centerY: Float = 0f

    fun configureFromPreferences() {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val ip = prefs.getString("udp_ip", "192.168.1.100") ?: "192.168.1.100"
        targetPort = prefs.getString("udp_port", "5005")?.toIntOrNull() ?: 5005

        try {
            targetAddress = InetAddress.getByName(ip)
            Log.d(TAG, "UDP configured: $ip:$targetPort")
        } catch (e: Exception) {
            Log.e(TAG, "Invalid IP address: $ip", e)
        }
    }

    fun initialize() {
        try {
            if (socket == null || socket?.isClosed == true) {
                socket = DatagramSocket()
            }
            if (executor == null) {
                executor = Executors.newSingleThreadScheduledExecutor()
            }
            configureFromPreferences()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize UDP socket", e)
        }
    }

    fun startSending() {
        if (isSending) return
        isSending = true
        val frequency = 30
        val periodMs = 1000L / frequency

        scheduledFuture = executor?.scheduleAtFixedRate({
            sendCoordinates()
        }, 0, periodMs, TimeUnit.MILLISECONDS)
        Log.d(TAG, "UDP sending started at ${frequency}Hz")
    }

    fun stopSending() {
        isSending = false
        scheduledFuture?.cancel(false)
        scheduledFuture = null
    }

    fun updateCoordinates(x: Float, y: Float) {
        centerX = x
        centerY = y
    }

    private fun sendCoordinates() {
        if (targetAddress == null) {
            configureFromPreferences()
        }

        try {
            val payload = JSONObject().apply {
                put("x", centerX)
                put("y", centerY)
                put("t", System.currentTimeMillis())
            }

            val data = payload.toString().toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(data, data.size, targetAddress, targetPort)
            socket?.send(packet)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send UDP packet", e)
        }
    }

    fun isReady(): Boolean = socket != null && !socket!!.isClosed && targetAddress != null

    fun release() {
        stopSending()
        executor?.shutdown()
        executor = null
        socket?.close()
        socket = null
    }

    companion object {
        private const val TAG = "UdpSender"
    }
}