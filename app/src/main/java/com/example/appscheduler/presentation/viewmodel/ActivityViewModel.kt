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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.appscheduler.AppLaunchWorker
import com.example.appscheduler.presentation.model.AppInfo
import com.example.appscheduler.utils.APLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ActivityViewModel"
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
                    if (appName != "AppScheduler") {
                        apps.add(AppInfo(appName, packageName, appIcon))
                        packageNames.add(packageName)
                    }
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
}