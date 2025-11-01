package com.example.appscheduler.presentation.activity

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appscheduler.presentation.model.AppInfo
import com.example.appscheduler.databinding.LayoutMainActivityBinding
import com.example.appscheduler.presentation.viewmodel.ActivityViewModel
import androidx.activity.viewModels
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appscheduler.data.repository.ScheduleRepository
import com.example.appscheduler.presentation.adapter.AppListAdapter
import com.example.appscheduler.presentation.viewmodel.ScheduleViewModel
import com.example.appscheduler.service.AppLaunchAccessibilityService
import com.example.appscheduler.utils.APLog
import com.example.appscheduler.utils.AccessibilityUtils

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: LayoutMainActivityBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var appsList: MutableList<AppInfo>
    private val appListViewModel: ActivityViewModel by viewModels()
    private val scheduleViewModel: ScheduleViewModel by viewModels()

    private var isAccessibilitySettingsLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.title = "App Scheduler";
        }
        binding = LayoutMainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        checkAccessibilityPermission()
    }

    override fun onStart() {
        super.onStart()
        AppLaunchAccessibilityService.onAppExecuted = { packageName ->
            scheduleViewModel.markScheduleExecutedByPackage(packageName)
            appsList.find { it.packageName == packageName }?.scheduleId = null
            appsList.find { it.packageName == packageName }?.scheduledEpochMs = null
            adapter.updateApps(appsList)
        }
    }

    override fun onStop() {
        super.onStop()
        AppLaunchAccessibilityService.onAppExecuted = null
    }

    override fun onResume() {
        super.onResume()

        if (AccessibilityUtils.isAccessibilityServiceEnabled(this, AppLaunchAccessibilityService::class.java)) {
            onAccessibilityEnabled()
        } else {
            onAccessibilityDisabled()
        }
    }

    private fun checkAccessibilityPermission() {
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this, AppLaunchAccessibilityService::class.java)) {
            showEnableAccessibilityDialog()
        } else {
            onAccessibilityEnabled()
        }
    }

    private fun showEnableAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To allow scheduled apps to launch automatically, please enable App Scheduler in Accessibility -> Installed Apps settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                isAccessibilitySettingsLaunched = true
                AccessibilityUtils.openAccessibilitySettings(this)
            }
            .setNegativeButton("Cancel") { _, _ ->
                finishAffinity()
            }
            .show()
    }

    private fun onAccessibilityEnabled() {
        // You can update your UI here, e.g., show a green indicator
        // Or show toast
        Toast.makeText(this, "Accessibility Service is ON", Toast.LENGTH_SHORT).show()
        initAppList()
    }

    private fun onAccessibilityDisabled() {
        // Update UI / disable auto-launch features if needed
        Toast.makeText(this, "Accessibility Service is OFF", Toast.LENGTH_SHORT).show()
        if(isAccessibilitySettingsLaunched) {
            finishAffinity()
        }
    }

    private fun initAppList() {
        appListViewModel.appsList.observe(this) { apps ->
            apps?.let {
                initAdapter(it)
            }
        }
    }

    private fun initAdapter(apps: List<AppInfo>) {
        appsList = apps.toMutableList()
        APLog.d(TAG, "initAdapter: appList: $appsList")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppListAdapter(
            context = this@MainActivity,
            items = appsList,
            onScheduleSaved = { appInfo, epochMs, onSaved ->
                scheduleViewModel.createOrUpdateSchedule(appInfo.appName, appInfo.packageName, epochMs) { success, result ->
                    if (success) {
                        val scheduleId = result
                        onSaved(scheduleId) // adapter stores scheduleId on the AppInfo item
                    } else {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show()
                        appInfo.scheduledEpochMs = null
                        binding.recyclerView.adapter?.notifyDataSetChanged()
                    }
                }
            },

            onScheduleRemoved = { appInfo ->
                if (appInfo.packageName.isNotBlank()) {
                    scheduleViewModel.cancelSchedule(appInfo.packageName, appInfo.scheduleId!!)
                } else {
                    appInfo.scheduledEpochMs = null
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
            }
        )
        binding.recyclerView.adapter = adapter

        scheduleViewModel.allSchedules.observe(this) { schedules ->
            appsList.forEach { app->
                schedules.forEach { schedule->
                    if (app.packageName == schedule.packageName) {
                        if (schedule.scheduledEpochMs < System.currentTimeMillis() || schedule.isExecuted) {
                            scheduleViewModel.cancelSchedule(schedule.packageName,
                                app.scheduleId.toString()
                            )
                        } else {
                            app.scheduledEpochMs = schedule.scheduledEpochMs
                            app.scheduleId = schedule.id
                        }
                    }
                }
            }
            adapter.updateApps(appsList)
        }

        // Optional: observe single executed schedule for row update
        scheduleViewModel.executedSchedule.observe(this) { updatedSchedule ->
            //adapter.updateRow(updatedSchedule)
        }
    }

}
