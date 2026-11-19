package com.transvibe.app.translation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class PackTransferState {
    NOT_INSTALLED,
    PENDING_NETWORK,
    DOWNLOADING,
    INSTALLED,
    FAILED
}

data class TranslationPackInfo(
    val code: String,
    val nameEn: String,
    val nameAr: String,
    val isCore: Boolean,
    val state: PackTransferState,
    val offlineReady: Boolean,
    val sizeLabel: String = "ML Kit managed (~30 MB)",
    val lastError: String? = null
)

/**
 * Independent manager for ML Kit offline translation packs:
 * - Auto-downloads core EN + AR packs on first launch when online, or queues PENDING_NETWORK + retries on connectivity.
 * - Never blocks app startup or ASR model management.
 * - Protects packs currently in use by an active translation job from deletion.
 */
class TranslationPackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activePackLeases = ConcurrentHashMap<String, Int>()
    private var networkCallbackRegistered = false

    private val supportedPacks = listOf(
        Triple("en", "English", "الإنجليزية"),
        Triple("ar", "Arabic", "العربية"),
        Triple("fr", "French", "الفرنسية"),
        Triple("de", "German", "الألمانية"),
        Triple("es", "Spanish", "الإسبانية"),
        Triple("tr", "Turkish", "التركية"),
        Triple("ur", "Urdu", "الأردية"),
        Triple("ja", "Japanese", "اليابانية"),
        Triple("ko", "Korean", "الكورية"),
        Triple("zh", "Chinese", "الصينية")
    )

    private val _packs = MutableStateFlow<List<TranslationPackInfo>>(emptyList())
    val packs: StateFlow<List<TranslationPackInfo>> = _packs.asStateFlow()

    fun acquirePackLease(langCode: String) {
        activePackLeases.compute(langCode.lowercase()) { _, c -> (c ?: 0) + 1 }
    }

    fun releasePackLease(langCode: String) {
        activePackLeases.compute(langCode.lowercase()) { _, c ->
            val next = (c ?: 1) - 1
            if (next <= 0) null else next
        }
    }

    suspend fun refreshInventory() = withContext(Dispatchers.IO) {
        val downloadedCodes = try {
            RemoteModelManager.getInstance()
                .getDownloadedModels(TranslateRemoteModel::class.java)
                .await()
                .map { it.language }
                .toSet()
        } catch (_: Throwable) {
            emptySet()
        }
        val prevMap = _packs.value.associateBy { it.code }
        _packs.value = supportedPacks.map { (code, nameEn, nameAr) ->
            val mlCode = MlKitTranslationEngine.mapCode(code)
            val isInstalled = mlCode != null && downloadedCodes.contains(mlCode)
            val prev = prevMap[code]
            val state = when {
                isInstalled -> PackTransferState.INSTALLED
                prev?.state == PackTransferState.DOWNLOADING -> PackTransferState.DOWNLOADING
                prev?.state == PackTransferState.PENDING_NETWORK -> PackTransferState.PENDING_NETWORK
                else -> PackTransferState.NOT_INSTALLED
            }
            TranslationPackInfo(
                code = code,
                nameEn = nameEn,
                nameAr = nameAr,
                isCore = (code == "en" || code == "ar"),
                state = state,
                offlineReady = isInstalled,
                lastError = if (isInstalled) null else prev?.lastError
            )
        }
    }

    fun ensureCoreEnArOnFirstLaunch() {
        scope.launch {
            refreshInventory()
            val missingCore = _packs.value.filter { it.isCore && !it.offlineReady }.map { it.code }
            if (missingCore.isEmpty()) return@launch
            if (isNetworkAvailable()) {
                missingCore.forEach { downloadPack(it) }
            } else {
                missingCore.forEach { markState(it, PackTransferState.PENDING_NETWORK, "Waiting for network") }
                registerConnectivityRetry()
            }
        }
    }

    suspend fun downloadPack(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        val mlCode = MlKitTranslationEngine.mapCode(code)
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported language pack: $code"))
        if (!isNetworkAvailable()) {
            markState(code, PackTransferState.PENDING_NETWORK, "Network unavailable")
            registerConnectivityRetry()
            return@withContext Result.failure(IllegalStateException("NETWORK_UNAVAILABLE"))
        }
        markState(code, PackTransferState.DOWNLOADING, null)
        return@withContext try {
            val model = TranslateRemoteModel.Builder(mlCode).build()
            RemoteModelManager.getInstance()
                .download(model, DownloadConditions.Builder().build())
                .await()
            refreshInventory()
            Result.success(Unit)
        } catch (t: Throwable) {
            markState(code, PackTransferState.FAILED, t.message)
            Result.failure(t)
        }
    }

    suspend fun deletePack(code: String): Result<Unit> = withContext(Dispatchers.IO) {
        if ((activePackLeases[code.lowercase()] ?: 0) > 0) {
            return@withContext Result.failure(IllegalStateException("Pack '$code' is currently in use by an active job"))
        }
        val mlCode = MlKitTranslationEngine.mapCode(code)
            ?: return@withContext Result.failure(IllegalArgumentException("Unsupported language pack: $code"))
        return@withContext try {
            val model = TranslateRemoteModel.Builder(mlCode).build()
            RemoteModelManager.getInstance().deleteDownloadedModel(model).await()
            refreshInventory()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun retryPending() {
        scope.launch {
            val pending = _packs.value.filter {
                it.state == PackTransferState.PENDING_NETWORK || it.state == PackTransferState.FAILED
            }
            pending.forEach { downloadPack(it.code) }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerConnectivityRetry() {
        if (networkCallbackRegistered) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkCallbackRegistered = true
        val req = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                retryPending()
            }
        })
    }

    private fun markState(code: String, state: PackTransferState, error: String?) {
        _packs.value = _packs.value.map {
            if (it.code == code) it.copy(state = state, lastError = error) else it
        }
    }
}
