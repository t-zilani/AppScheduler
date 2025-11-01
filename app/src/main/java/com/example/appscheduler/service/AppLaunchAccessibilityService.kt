package com.example.appscheduler.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.appscheduler.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppLaunchAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppLaunchService"
        var instance: AppLaunchAccessibilityService? = null
        var onAppExecuted: ((String) -> Unit)? = null
    }

    private val scheduleDao by lazy { AppDatabase.getInstance(this).scheduleDao() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only handle TYPE_WINDOW_STATE_CHANGED to detect app launches
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            if (!packageName.isNullOrEmpty()) {
                Log.d(TAG, "Detected app launch: $packageName")

                CoroutineScope(Dispatchers.IO).launch {
                    val schedule = scheduleDao.getScheduleByPackage(packageName)
                    if (schedule != null && !schedule.isExecuted) {
                        scheduleDao.update(schedule.copy(isExecuted = true))
                        Log.d(TAG, "Marked schedule executed for $packageName")

                        withContext(Dispatchers.Main) {
                            onAppExecuted?.invoke(packageName)
                        }
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onInterrupt() {
        // Required override
    }

    fun launchAppSilently(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (launchIntent != null) {
                startActivity(launchIntent)
                Log.d(TAG, "Launched app silently: $packageName")
            } else {
                Log.e(TAG, "Launch intent not found for $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: ${e.message}")
        }
    }
}
