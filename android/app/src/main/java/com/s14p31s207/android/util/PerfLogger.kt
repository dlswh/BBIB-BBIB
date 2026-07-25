package com.s14p31s207.android.util

import android.util.Log
import com.s14p31s207.android.BuildConfig

object PerfLogger {
    private const val TAG = "PERF"

    fun d(message: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(TAG, message)
    }
}

data class ConnectionPerfTrace(
    val category: String,
    val attemptId: String,
    val startedAt: Long
)

data class PairingPerfTrace(
    val attemptId: String,
    val startedAt: Long
)
