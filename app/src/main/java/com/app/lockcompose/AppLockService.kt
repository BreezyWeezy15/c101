package com.app.lockcompose

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat


class AppLockService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "AppLockServiceChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var appLockManager: AppLockManager
    private lateinit var sharedPreferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 1000)
        }
    }

    private val packageRemovalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "PACKAGE_REMOVED") {
                val packageName = intent.getStringExtra("PACKAGE_NAME")
                packageName?.let {
                    removePackageFromAccessList(it)
                    // Send an update broadcast
                    val updateIntent = Intent("UPDATE_APP_LIST")
                    sendBroadcast(updateIntent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        appLockManager = AppLockManager(this)
        sharedPreferences = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.post(runnable)
        registerReceiver(packageRemovalReceiver, IntentFilter("PACKAGE_REMOVED"))
        Log.d("AppLockService", "Service created and runnable posted.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        unregisterReceiver(packageRemovalReceiver)
        Log.d("AppLockService", "Service destroyed and runnable removed.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 1000 * 1000, now)

        if (stats.isNotEmpty()) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            val currentApp = sortedStats.firstOrNull()?.packageName
            Log.d("AppLockService", "Current top app: $currentApp")

            val lockedPackages = appLockManager.getSelectedPackages()

            if (currentApp in lockedPackages) {
                if (currentApp != null) {
                    showLockScreen(currentApp)
                }
            }
        } else {
            Log.d("AppLockService", "No usage stats available.")
        }
    }

    private fun showLockScreen(packageName: String) {
        val lockIntent = Intent(this, LockScreenActivity::class.java)
        lockIntent.putExtra("PACKAGE_NAME", packageName)
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(lockIntent)
        Log.d("AppLockService", "Lock screen shown for package: $packageName")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Lock Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("App Lock Service")
            .setContentText("App lock service is running")
            .setSmallIcon(R.drawable.baseline_lock_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun removePackageFromAccessList(packageName: String) {
        val selectedPackageNames = sharedPreferences.getStringSet("selected_package_names", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (selectedPackageNames.contains(packageName)) {
            selectedPackageNames.remove(packageName)
            with(sharedPreferences.edit()) {
                putStringSet("selected_package_names", selectedPackageNames)
                apply()
            }

            // Update access list in SharedPreferences
            val accessList = sharedPreferences.getStringSet("access_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            accessList.remove(packageName)
            with(sharedPreferences.edit()) {
                putStringSet("access_list", accessList)
                apply()
            }
        }
    }
}