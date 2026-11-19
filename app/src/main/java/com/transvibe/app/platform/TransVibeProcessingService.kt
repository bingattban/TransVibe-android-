package com.transvibe.app.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.transvibe.app.MainActivity
import com.transvibe.app.R
import com.transvibe.app.TransVibeApplication
import com.transvibe.app.models.ModelCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service using FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING.
 * Owns the long-lived processing CoroutineScope so jobs survive ViewModel destruction/rotation.
 */
class TransVibeProcessingService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            activeJob?.cancel()
            stopSelf()
            return START_NOT_STICKY
        }

        val jobId = intent?.getStringExtra(EXTRA_JOB_ID) ?: return START_NOT_STICKY
        val mediaUriStr = intent.getStringExtra(EXTRA_MEDIA_URI) ?: return START_NOT_STICKY
        val mediaTitle = intent.getStringExtra(EXTRA_MEDIA_TITLE) ?: "video.mp4"
        val trackIndex = intent.getIntExtra(EXTRA_TRACK_INDEX, 0)
        val modelId = intent.getStringExtra(EXTRA_MODEL_ID) ?: ModelCatalog.WHISPER_TINY_ID
        val sourceLang = intent.getStringExtra(EXTRA_SOURCE_LANG) ?: "auto"
        val targetLang = intent.getStringExtra(EXTRA_TARGET_LANG) ?: "ar"

        val notification = buildNotification(jobId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TransVibe:ProcessingWakeLock")
            .apply { acquire(30 * 60 * 1000L) }

        val app = application as TransVibeApplication
        val model = ModelCatalog.findById(modelId) ?: run {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        activeJob?.cancel()
        activeJob = serviceScope.launch {
            try {
                app.container.orchestrator.runJob(
                    mediaUri = Uri.parse(mediaUriStr),
                    mediaTitle = mediaTitle,
                    trackIndex = trackIndex,
                    model = model,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    existingJobId = jobId
                )
            } catch (_: CancellationException) {
            } finally {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        activeJob?.cancel()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    override fun onDestroy() {
        activeJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Throwable) {
        } finally {
            wakeLock = null
        }
    }

    private fun buildNotification(jobId: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.processing_notification_title))
            .setContentText(if (jobId.isNotBlank()) "Job: ${jobId.take(8)}" else "TransVibe")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        putExtra(EXTRA_JOB_ID, jobId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_processing),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_processing_desc)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "transvibe_media_processing"
        const val NOTIFICATION_ID = 1001
        const val ACTION_CANCEL = "com.transvibe.app.action.CANCEL_PROCESSING"
        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_MEDIA_URI = "extra_media_uri"
        const val EXTRA_MEDIA_TITLE = "extra_media_title"
        const val EXTRA_TRACK_INDEX = "extra_track_index"
        const val EXTRA_MODEL_ID = "extra_model_id"
        const val EXTRA_SOURCE_LANG = "extra_source_lang"
        const val EXTRA_TARGET_LANG = "extra_target_lang"
    }
}
