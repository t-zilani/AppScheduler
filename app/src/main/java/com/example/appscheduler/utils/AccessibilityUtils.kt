package com.example.appscheduler.utils

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.accessibilityservice.AccessibilityService

object AccessibilityUtils {

    fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = ComponentName(context, service).flattenToString()
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(serviceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
