package com.transvibe.app.platform

import android.content.Context
import android.os.Build
import android.os.PowerManager

class ThermalMonitor(private val context: Context) {
    fun currentThermalStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.currentThermalStatus
    }

    fun isSeverelyThrottled(): Boolean {
        val status = currentThermalStatus()
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            status >= PowerManager.THERMAL_STATUS_SEVERE
    }
}
