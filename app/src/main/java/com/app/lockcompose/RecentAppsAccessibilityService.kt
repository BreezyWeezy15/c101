package com.app.lockcompose


import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat

class RecentAppsAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RecentAppsService"
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d(TAG, "Event: $event")
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val className = event.className?.toString() ?: return
            Log.d(TAG, "Class: $className")

            if (isRecentAppsScreen(packageName, className)) {
                Toast.makeText(this, "Recent Apps opened", Toast.LENGTH_SHORT).show()
                triggerLockScreen()
            }
        }
    }
    private fun isRecentAppsScreen(packageName: String, className: String): Boolean {
        val recentAppsPackages = listOf(
            "com.android.systemui",      // Stock Android
            "com.samsung.android.systemui", // Samsung
            "com.google.android.systemui", // Pixel
            "com.oneplus.systemui",      // OnePlus
            "com.huawei.android.launcher", // Huawei
            "com.miui.home",             // Xiaomi
            "com.oppo.launcher"          // Oppo
        )

        val recentAppsClasses = listOf(
            "RecentsActivity",
            "RecentApplicationsActivity",
            "RecentAppsActivity",
            "RecentTasksActivity",
            "RecentAppActivity"
        )

        return recentAppsPackages.contains(packageName) && recentAppsClasses.any { className.contains(it, ignoreCase = true) }
    }
    override fun onInterrupt() {
        // Handle interruptions if needed
    }
    private fun triggerLockScreen() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ContextCompat.startActivity(this, intent, null)
    }


}


