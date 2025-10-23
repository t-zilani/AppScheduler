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
        // inside your Fragment or Activity
        val appList: MutableList<AppInfo> = apps.toMutableList()
        APLog.d(TAG, "initAdapter: appList: $appList")
// assume viewModel: ScheduleViewModel (from earlier) and recyclerView already set up
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = AppListAdapter(
            context = this@MainActivity,
            items = appList, // initial list
            onScheduleSaved = { appInfo, epochMs, onSaved ->
                // call ViewModel to persist + schedule WorkManager
                scheduleViewModel.createAndSchedule(
                    appInfo.appName,
                    appInfo.packageName,
                    epochMs
                ) { success, result ->
                    if (success) {
                        // result contains scheduleId (as per previous ViewModel implementation)
                        val scheduleId = result
                        // inform adapter about the persisted id
                        onSaved(scheduleId.toString())
                    } else {
                        appInfo.scheduledEpochMs = null
                        binding.recyclerView.adapter?.notifyDataSetChanged()
                        val errMsg = result
                        Toast.makeText(this@MainActivity, errMsg.toString(), Toast.LENGTH_LONG)
                            .show()
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

    // inside Activity/Fragment with viewModel: ScheduleViewModel
    private fun onUserSavedSchedule(appLabel: String, packageName: String, epochMs: Long) {
        scheduleViewModel.createAndSchedule(appLabel, packageName, epochMs) { ok, info ->
            if (ok) {
                Toast.makeText(this@MainActivity, "Scheduled", Toast.LENGTH_SHORT).show()
            } else {
                // info contains message: conflict or error
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Schedule conflict")
                    .setMessage(info.toString())
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
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
