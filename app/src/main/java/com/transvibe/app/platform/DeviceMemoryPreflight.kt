package com.transvibe.app.platform

import android.app.ActivityManager
import android.content.Context
import com.transvibe.app.domain.ModelDefinition

data class MemoryPreflightResult(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val compatible: Boolean,
    val warningMessage: String?
)

/**
 * Checks device total and available RAM before loading heavy models (Medium / Large V3 Turbo).
 */
object DeviceMemoryPreflight {
    fun check(context: Context, model: ModelDefinition): MemoryPreflightResult {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return MemoryPreflightResult(0L, 0L, true, null)
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024L * 1024L)
        val availMb = memInfo.availMem / (1024L * 1024L)
        val ok = totalMb >= model.minRamMb && (!model.highMemoryPolicy || availMb >= 1024L)
        val warning = if (!ok) {
            "LOW_MEMORY: ${model.displayNameEn} requires >= ${model.minRamMb} MB RAM (Device total: $totalMb MB, available: $availMb MB)"
        } else null
        return MemoryPreflightResult(totalMb, availMb, ok, warning)
    }
}
