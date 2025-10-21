package com.example.appscheduler.presentation.activity   // ← use your actual package name

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appscheduler.presentation.model.AppInfo
import com.example.appscheduler.databinding.LayoutMainActivityBinding
import com.example.appscheduler.presentation.viewmodel.ActivityViewModel
import androidx.activity.viewModels
import android.provider.Settings
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.work.WorkManager
import com.example.appscheduler.presentation.adapter.AppListAdapter
import com.example.appscheduler.utils.APLog
import com.example.appscheduler.utils.UIUtils
import java.util.Date
import java.util.UUID
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: LayoutMainActivityBinding
    private val viewModel: ActivityViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutMainActivityBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)



        initAdapter()
    }

    private fun initAdapter() {

        val appItems = mutableListOf<AppInfo>() // fill this list with installed apps

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = AppListAdapter(
            context = this,
            items = appItems,
            onScheduleSaved = { item, epochMs ->
                 Toast.makeText(this, "Scheduled ${item.appName} at ${Date(epochMs)}", Toast.LENGTH_SHORT).show()
                APLog.d(TAG, "check: package: ${item.appName}, time: $${Date(epochMs)}")

                val scheduleId = UUID.randomUUID().toString()
                // TODO
                //viewModel.saveScheduleToDb(scheduleId, app.packageName, scheduledMs) // persist schedule

                viewModel.scheduleWithWorkManager(context = this, scheduleId = scheduleId, packageName = item.packageName, scheduledEpochMs = epochMs)
            },
            onScheduleRemoved = { item ->
                  Toast.makeText(this, "Removed schedule for ${item.appName}", Toast.LENGTH_SHORT).show()
                viewModel.cancelScheduledWork(this, scheduleId = item.scheduledEpochMs.toString())
            //TODO keep scheduleid on roomdb
            //viewModel.markScheduleCancelled(scheduleId)
            }
        )
        binding.recyclerView.adapter = adapter

        // Observe the LiveData from the ViewModel
        viewModel.appsList.observe(this) { apps ->
            // Update the adapter with the new list of apps
            apps?.let {
                adapter.updateApps(it)
            }
        }
    }

    fun getWorkerStatus() {
        val workManager = WorkManager.getInstance(this)
//        workManager.getWorkInfosForUniqueWorkLiveData("schedule-$scheduleId")
//            .observe(this) { workInfos ->
//                // inspect workInfos list: state, outputData, runAttemptCount, etc.
//            }
    }
}
