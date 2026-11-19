package com.transvibe.app.diagnostics

import android.os.Build
import com.transvibe.app.BuildConfig

/**
 * Produces redacted diagnostic reports (no raw audio, no full transcripts, no local paths).
 */
object DiagnosticExporter {
    fun buildRedactedReport(
        installedModelIds: List<String>,
        recentJobStates: List<Pair<String, String>>
    ): String {
        return buildString {
            appendLine("=== TransVibe Diagnostic Report (Redacted) ===")
            appendLine("App: TransVibe (${BuildConfig.APPLICATION_ID})")
            appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) [${BuildConfig.VARIANT_LABEL}]")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("ABIs: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Installed Models: ${installedModelIds.joinToString().ifEmpty { "None" }}")
            appendLine("Recent Jobs:")
            recentJobStates.forEach { (id, state) ->
                appendLine("  - ${id.take(8)}... : $state")
            }
        }
    }
}
