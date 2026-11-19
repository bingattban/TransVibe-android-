package com.transvibe.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

object SafStorageHelper {
    fun takePersistableReadPermission(context: Context, uri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some transient document providers do not support persistable grants
        }
    }

    fun hasReadAccess(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}
