package com.example.appscheduler.presentation.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.appscheduler.presentation.model.AppInfo
import com.example.appscheduler.databinding.LayoutMainActivityBinding
import com.example.appscheduler.presentation.viewmodel.ActivityViewModel
import androidx.activity.viewModels
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appscheduler.presentation.adapter.AppListAdapter
import com.example.appscheduler.presentation.viewmodel.ScheduleViewModel
import com.example.appscheduler.utils.APLog

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private val REQUEST_NOTIFICATION_PERMISSION = 1001
    private lateinit var binding: LayoutMainActivityBinding
    private val appListViewModel: ActivityViewModel by viewModels()
    private val scheduleViewModel: ScheduleViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportActionBar != null) {
            supportActionBar?.title = "App Scheduler";
        }
        binding = LayoutMainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkNotificationPermission()
        initAppList()
    }

    private fun initAppList() {
        appListViewModel.appsList.observe(this) { apps ->
            apps?.let {
                initAdapter(it)
            }
        }
    }

    private fun initAdapter(apps: List<AppInfo>) {
        val appList: MutableList<AppInfo> = apps.toMutableList()
        APLog.d(TAG, "initAdapter: appList: $appList")

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = AppListAdapter(
            context = this@MainActivity,
            items = appList,
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
                if (!appInfo.scheduleId.isNullOrBlank()) {
                    scheduleViewModel.cancelSchedule(appInfo.scheduleId!!)
                } else {
                    appInfo.scheduledEpochMs = null
                    binding.recyclerView.adapter?.notifyDataSetChanged()
                }
            }
        )
        binding.recyclerView.adapter = adapter
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Allowed notifications", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Scheduling will not work in background mode, allow notifications from settings", Toast.LENGTH_SHORT).show()
                openNotificationSettings()
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }
}
