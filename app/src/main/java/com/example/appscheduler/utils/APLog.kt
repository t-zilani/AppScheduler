package com.example.appscheduler.utils

import android.util.Log

object APLog {
    val TAG = "[APLog]"

    fun d(tag: String, string: String) {
        Log.i("$TAG : $tag", string)
    }
}