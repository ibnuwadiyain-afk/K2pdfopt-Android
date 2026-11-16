package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.R

data class DeviceThermalBatteryState(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val thermalStatusLabel: String,
    val recommendedOcrThreads: Int,
    val isThrottled: Boolean
)

object DeviceHealthMonitor {

    fun readCurrentState(context: Context, requestedThreads: Int): DeviceThermalBatteryState {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(1, 100) ?: 85

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        var thermalCode = 0
        var thermalLabel = "Nominal (Cool)"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            thermalCode = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
            thermalLabel = when (thermalCode) {
                PowerManager.THERMAL_STATUS_NONE -> "Nominal (0)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light Warmth (1)"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Warmth (2 — Throttling)"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe Thermal (3 — 1 Thread Cap)"
                else -> "Critical Thermal ($thermalCode — 1 Thread Cap)"
            }
        }

        val throttled = thermalCode >= 2 || (batteryPct < 20 && !isCharging)
        val safeThreads = when {
            thermalCode >= 3 -> 1
            throttled -> 1
            else -> requestedThreads.coerceIn(1, Runtime.getRuntime().availableProcessors().coerceAtLeast(2))
        }

        return DeviceThermalBatteryState(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            thermalStatusLabel = thermalLabel,
            recommendedOcrThreads = safeThreads,
            isThrottled = throttled
        )
    }
}

class K2ConversionService : Service() {

    companion object {
        const val CHANNEL_ID = "k2pdfopt_conversion_channel"
        const val NOTIF_ID = 2550
        const val ACTION_START = "com.example.service.START_CONVERSION"
        const val ACTION_STOP = "com.example.service.STOP_CONVERSION"
        const val EXTRA_FILE_NAME = "extra_file_name"
        const val EXTRA_CURRENT_PAGE = "extra_current_page"
        const val EXTRA_TOTAL_PAGES = "extra_total_pages"
    }

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val fileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: "Document"
                val cur = intent?.getIntExtra(EXTRA_CURRENT_PAGE, 1) ?: 1
                val total = intent?.getIntExtra(EXTRA_TOTAL_PAGES, 1) ?: 1
                val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setContentTitle(getString(R.string.notif_converting_title, fileName, cur, total))
                    .setProgress(total.coerceAtLeast(1), cur.coerceIn(0, total), false)
                    .setOngoing(true)
                    .build()
                try {
                    startForeground(NOTIF_ID, notif)
                } catch (_: Exception) {
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm?.createNotificationChannel(channel)
        }
    }
}
