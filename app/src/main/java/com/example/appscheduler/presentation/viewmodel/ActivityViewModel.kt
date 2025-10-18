package com.example.appscheduler.presentation.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appscheduler.AppLaunchReceiver
import com.example.appscheduler.presentation.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val _appsList = MutableLiveData<List<AppInfo>>()
    val appsList: LiveData<List<AppInfo>> = _appsList

    init {
        loadInstalledUIApps()
    }

    private fun loadInstalledUIApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val packageManager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val appList = packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )

            val apps = mutableListOf<AppInfo>()
            val packageNames = mutableSetOf<String>()

            for (resolveInfo in appList) {
                val packageName = resolveInfo.activityInfo.packageName
                val applicationInfo = resolveInfo.activityInfo.applicationInfo

                if (packageName in packageNames) {
                    continue
                }

                val isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                if (!isSystemApp || hasLauncherIcon(packageManager, packageName)) {
                    val appName = resolveInfo.loadLabel(packageManager).toString()
                    val appIcon = resolveInfo.loadIcon(packageManager)
                    apps.add(AppInfo(appName, packageName, appIcon))
                    packageNames.add(packageName)
                }
            }
            withContext(Dispatchers.Main) {
                _appsList.value = apps
            }
        }
    }

    private fun hasLauncherIcon(packageManager: PackageManager, packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        return intent != null
    }

    fun scheduleAppLaunch(packageName: String, epochMs: Long) {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create a Calendar instance with the target time
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.MILLISECOND, epochMs.toInt())
        }

        // If the scheduled time has already passed for today, set it for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, AppLaunchReceiver::class.java).apply {
            putExtra("packageName", packageName)
        }

        // Create a PendingIntent to be triggered by the alarm
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            packageName.hashCode(), // Use a unique request code for each app
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Schedule the exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        val context = getApplication<Application>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}