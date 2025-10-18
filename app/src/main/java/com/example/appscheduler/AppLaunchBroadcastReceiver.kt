package com.example.appscheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

class AppLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName")
        if (packageName != null) {
            launchApp(context, packageName)
        } else {
            Toast.makeText(context, "Error: No package name specified", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchApp(context: Context, packageName: String) {
        try {
            val launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                // The FLAG_ACTIVITY_NEW_TASK is crucial when launching an app from a context outside an activity
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "App not found: $packageName", Toast.LENGTH_LONG).show()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Toast.makeText(context, "Error launching app: " + e.message, Toast.LENGTH_LONG).show()
        }
    }
}
