package com.example.appscheduler.presentation.adapter

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.appscheduler.databinding.ItemAppCardBinding
import com.example.appscheduler.databinding.DialogTimePickerBinding
import com.example.appscheduler.presentation.model.AppInfo
import com.example.appscheduler.utils.APLog
import java.text.SimpleDateFormat
import java.util.*


class AppListAdapter(
    private val context: Context,
    var items: MutableList<AppInfo>,
    private val onScheduleSaved: (item: AppInfo, epochMs: Long, onSaved: (scheduleId: String) -> Unit) -> Unit,
    private val onScheduleRemoved: (item: AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {
    private val TAG = "AppListAdapter"
    private val timeFormatter = SimpleDateFormat("MMM d, yyyy — hh:mm a", Locale.getDefault())

    inner class VH(val binding: ItemAppCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        APLog.d(TAG, "onCreateViewHolder: viewType $viewType")
        val binding = ItemAppCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.binding.appIcon.setImageDrawable(item.icon)
        holder.binding.appName.text = item.appName

        if (item.scheduledEpochMs != null) {
            holder.binding.scheduleRow.visibility = View.VISIBLE
            holder.binding.scheduledTime.visibility = View.VISIBLE
            holder.binding.scheduledTime.text = timeFormatter.format(Date(item.scheduledEpochMs!!))
        } else {
            holder.binding.scheduleRow.visibility = View.GONE
            holder.binding.scheduledTime.visibility = View.GONE
        }

        holder.binding.cardRoot.setOnClickListener {
            showTimePickerDialog(item, position)
        }

        holder.binding.removeSchedule.setOnClickListener {
            item.scheduledEpochMs = null
            notifyItemChanged(position)
            onScheduleRemoved(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateApps(updatedApps: List<AppInfo>) {
        items = updatedApps.toMutableList()
        notifyDataSetChanged()
    }

    fun applySavedScheduleInfo(packageName: String, scheduleId: String, scheduledEpochMs: Long) {
        val idx = items.indexOfFirst { it.packageName == packageName }
        if (idx >= 0) {
            val it = items[idx]
            it.scheduleId = scheduleId
            it.scheduledEpochMs = scheduledEpochMs
            notifyItemChanged(idx)
        }
    }

    fun clearScheduleForScheduleId(scheduleId: String) {
        val idx = items.indexOfFirst { it.scheduleId == scheduleId }
        if (idx >= 0) {
            val it = items[idx]
            it.scheduleId = null
            it.scheduledEpochMs = null
            notifyItemChanged(idx)
        }
    }

    private fun showTimePickerDialog(item: AppInfo, position: Int) {
        val builder = AlertDialog.Builder(context)
        val dialogBinding = DialogTimePickerBinding.inflate(LayoutInflater.from(context))

        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.setCancelable(true)

        dialogBinding.dialogTitle.text = "Set time for ${item.appName}"

        val initialCalendar = Calendar.getInstance()
        item.scheduledEpochMs?.let {
            initialCalendar.timeInMillis = it
        }

        dialogBinding.timePicker.setIs24HourView(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            dialogBinding.timePicker.hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
            dialogBinding.timePicker.minute = initialCalendar.get(Calendar.MINUTE)
        } else {
            @Suppress("DEPRECATION")
            dialogBinding.timePicker.currentHour = initialCalendar.get(Calendar.HOUR_OF_DAY)
            @Suppress("DEPRECATION")
            dialogBinding.timePicker.currentMinute = initialCalendar.get(Calendar.MINUTE)
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val chosenCal = Calendar.getInstance()
            chosenCal.set(Calendar.SECOND, 0)
            chosenCal.set(Calendar.MILLISECOND, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                chosenCal.set(Calendar.HOUR_OF_DAY, dialogBinding.timePicker.hour)
                chosenCal.set(Calendar.MINUTE, dialogBinding.timePicker.minute)
            } else {
                chosenCal.set(Calendar.HOUR_OF_DAY, dialogBinding.timePicker.currentHour)
                chosenCal.set(Calendar.MINUTE, dialogBinding.timePicker.currentMinute)
            }

            if (chosenCal.timeInMillis <= System.currentTimeMillis()) {
                chosenCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            val scheduledMs = chosenCal.timeInMillis

            item.scheduledEpochMs = scheduledMs
            notifyItemChanged(position)

            onScheduleSaved(item, scheduledMs) { generatedScheduleId ->
                item.scheduleId = generatedScheduleId
                notifyItemChanged(position)
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}
