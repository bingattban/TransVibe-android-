package com.transvibe.app.media

/**
 * Optional FFmpegKit reflection bridge for unsupported audio codecs or verified MP4 subtitle burn-in.
 * Only active when a compatible, license-verified FFmpegKit AAR is placed in app/libs/.
 */
object FfmpegFallbackBridge {
    fun isAvailable(): Boolean {
        return try {
            Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    fun executeCommand(command: String): Result<Unit> {
        if (!isAvailable()) {
            return Result.failure(
                IllegalStateException("NOT_SUPPORTED_YET: Optional FFmpegKit AAR is not bundled in app/libs/")
            )
        }
        return try {
            val cls = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
            val session = cls.getMethod("execute", String::class.java).invoke(null, command)
            val rcObj = session.javaClass.getMethod("getReturnCode").invoke(session)
            val isSuccess = rcObj.javaClass.getMethod("isValueSuccess").invoke(rcObj) as Boolean
            if (isSuccess) Result.success(Unit)
            else Result.failure(IllegalStateException("FFmpeg execution failed"))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
