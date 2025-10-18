package com.example.appscheduler.presentation.adapter

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import androidx.recyclerview.widget.RecyclerView
import com.example.appscheduler.R
import com.example.appscheduler.presentation.model.AppInfo
import java.text.SimpleDateFormat
import java.util.*

class AppListAdapter(
    private val context: Context,
    var items: MutableList<AppInfo>,
    private val onScheduleSaved: (item: AppInfo, epochMs: Long) -> Unit,
    private val onScheduleRemoved: (item: AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    private val timeFormatter = SimpleDateFormat("MMM d, yyyy — hh:mm a", Locale.getDefault())

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val icon = view.findViewById<android.widget.ImageView>(R.id.app_icon)
        val name = view.findViewById<TextView>(R.id.app_name)
        val scheduleRow = view.findViewById<View>(R.id.schedule_row)
        val scheduledTime = view.findViewById<TextView>(R.id.scheduled_time)
        val removeSchedule = view.findViewById<TextView>(R.id.remove_schedule)
        val cardRoot = view.findViewById<androidx.cardview.widget.CardView>(R.id.card_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.icon.setImageDrawable(item.icon)
        holder.name.text = item.appName

        // Show/hide scheduled time row
        if (item.scheduledEpochMs != null) {
            holder.scheduleRow.visibility = View.VISIBLE
            holder.scheduledTime.text = timeFormatter.format(Date(item.scheduledEpochMs!!))
        } else {
            holder.scheduleRow.visibility = View.GONE
        }

        // Row click -> show time picker dialog
        holder.cardRoot.setOnClickListener {
            showTimePickerDialog(item, position)
        }

        // Remove click -> clear schedule for this item
        holder.removeSchedule.setOnClickListener {
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

    private fun showTimePickerDialog(item: AppInfo, position: Int) {
        val builder = AlertDialog.Builder(context)
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.setCancelable(true)

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        val timePicker = dialogView.findViewById<TimePicker>(R.id.time_picker)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)

        title.text = "Set time for ${item.appName}"

        // Initialize TimePicker to existing scheduled time or current time
        val initialCalendar = Calendar.getInstance()
        item.scheduledEpochMs?.let {
            initialCalendar.timeInMillis = it
        }

        // TimePicker API differs by SDK for getting/setting:
        timePicker.setIs24HourView(false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            timePicker.hour = initialCalendar.get(Calendar.HOUR_OF_DAY)
            timePicker.minute = initialCalendar.get(Calendar.MINUTE)
        } else {
            @Suppress("DEPRECATION")
            timePicker.currentHour = initialCalendar.get(Calendar.HOUR_OF_DAY)
            @Suppress("DEPRECATION")
            timePicker.currentMinute = initialCalendar.get(Calendar.MINUTE)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            // read hour/min
            val chosenCal = Calendar.getInstance()
            chosenCal.set(Calendar.SECOND, 0)
            chosenCal.set(Calendar.MILLISECOND, 0)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                chosenCal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                chosenCal.set(Calendar.MINUTE, timePicker.minute)
            } else {
                @Suppress("DEPRECATION")
                chosenCal.set(Calendar.HOUR_OF_DAY, timePicker.currentHour)
                @Suppress("DEPRECATION")
                chosenCal.set(Calendar.MINUTE, timePicker.currentMinute)
            }

            // If chosen time is earlier than now -> schedule for next day (common alarm behavior)
            if (chosenCal.timeInMillis <= System.currentTimeMillis()) {
                chosenCal.add(Calendar.DAY_OF_MONTH, 1)
            }

            val scheduledMs = chosenCal.timeInMillis
            item.scheduledEpochMs = scheduledMs
            notifyItemChanged(position)
            onScheduleSaved(item, scheduledMs)

            dialog.dismiss()
        }

        dialog.show()
    }
}
