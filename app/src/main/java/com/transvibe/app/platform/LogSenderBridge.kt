package com.transvibe.app.platform

import android.content.Context
import android.content.Intent

/**
 * Safe integration helper for Code On The Go LogSender without duplicate service instances.
 */
object LogSenderBridge {
    private const val LOGSENDER_ACTION = "com.itsaky.androidide.logsender.LOG_SERVICE"

    fun isLogSenderAvailable(context: Context): Boolean {
        val intent = Intent(LOGSENDER_ACTION)
        return context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
    }
}
