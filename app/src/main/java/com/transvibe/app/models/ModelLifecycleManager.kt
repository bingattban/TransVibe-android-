package com.transvibe.app.models

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.transvibe.app.domain.ArtifactFormat
import com.transvibe.app.domain.ChecksumTrust
import com.transvibe.app.domain.EngineType
import com.transvibe.app.domain.ErrorCode
import com.transvibe.app.domain.InstalledModelRecord
import com.transvibe.app.domain.ModelDefinition
import com.transvibe.app.domain.ModelExporter
import com.transvibe.app.domain.ModelStatus
import com.transvibe.app.domain.ModelVerifier
import com.transvibe.app.nativebridge.SherpaNativeBridge
import com.transvibe.app.nativebridge.WhisperNativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ModelLifecycleManager(
    private val context: Context
) : ModelVerifier, ModelExporter {

    private val modelsRoot: File = File(context.filesDir, "models")
    private val stagingRoot: File = File(context.filesDir, "staging")
    private val activeLeases = ConcurrentHashMap<String, Int>()

    private val _records = MutableStateFlow<Map<String, InstalledModelRecord>>(emptyMap())
    val records: StateFlow<Map<String, InstalledModelRecord>> = _records.asStateFlow()

    init {
        modelsRoot.mkdirs()
        stagingRoot.mkdirs()
        refreshInventoryQuick()
    }

    fun refreshInventoryQuick() {
        val map = mutableMapOf<String, InstalledModelRecord>()
        for (def in ModelCatalog.ALL) {
            val target = File(modelsRoot, def.modelId)
            val readyMarker = File(target, ".ready")
            if (target.exists() && readyMarker.exists()) {
                val mainFile = resolvePrimaryArtifact(def, target)
                map[def.modelId] = InstalledModelRecord(
                    modelId = def.modelId,
                    status = if (mainFile != null) ModelStatus.READY else ModelStatus.REPAIRABLE,
                    installedPath = target.absolutePath,
                    localSha256 = null,
                    checksumTrust = ChecksumTrust.UNVERIFIED_LOCAL_ONLY,
                    sizeBytes = directorySize(target),
                    profileVersion = 1,
                    validatedAtEpochMs = readyMarker.lastModified()
                )
            } else {
                map[def.modelId] = InstalledModelRecord(
                    modelId = def.modelId,
                    status = ModelStatus.NOT_INSTALLED,
                    installedPath = null,
                    localSha256 = null,
                    checksumTrust = ChecksumTrust.UNVERIFIED_LOCAL_ONLY,
                    sizeBytes = 0L,
                    profileVersion = 1,
                    validatedAtEpochMs = null
                )
            }
        }
        _records.value = map
    }

    fun acquireLease(modelId: String) {
        activeLeases.compute(modelId) { _, count -> (count ?: 0) + 1 }
    }

    fun releaseLease(modelId: String) {
        activeLeases.compute(modelId) { _, count ->
            val next = (count ?: 1) - 1
            if (next <= 0) null else next
        }
    }

    fun isLeased(modelId: String): Boolean = (activeLeases[modelId] ?: 0) > 0

    fun availableStorageMb(): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return (stat.availableBytes / (1024L * 1024L))
    }

    suspend fun downloadModelHttps(
        httpsUrl: String,
        expectedModel: ModelDefinition,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<InstalledModelRecord> = withContext(Dispatchers.IO) {
        if (!httpsUrl.startsWith("https://", ignoreCase = true)) {
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.MODEL_UNTRUSTED)
            return@withContext Result.failure(SecurityException("HTTPS is required for model downloads"))
        }
        if (availableStorageMb() < expectedModel.minStorageMb) {
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.LOW_STORAGE)
            return@withContext Result.failure(IllegalStateException("LOW_STORAGE: Need ${expectedModel.minStorageMb} MB"))
        }

        val stageDir = File(stagingRoot, "${expectedModel.modelId}_dl").apply { mkdirs() }
        val stagedFile = File(stageDir, expectedModel.physicalFileName)
        val etagFile = File(stageDir, ".etag")

        var conn: HttpURLConnection? = null
        try {
            updateStatus(expectedModel.modelId, ModelStatus.TRANSFERRING, null)
            val existingBytes = if (stagedFile.exists()) stagedFile.length() else 0L
            val savedEtag = if (etagFile.exists()) etagFile.readText().trim() else null

            conn = (URL(httpsUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                if (existingBytes > 0L && !savedEtag.isNullOrBlank()) {
                    setRequestProperty("Range", "bytes=$existingBytes-")
                    setRequestProperty("If-Range", savedEtag)
                }
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.NETWORK_UNAVAILABLE)
                return@withContext Result.failure(IllegalStateException("HTTP $code while downloading model"))
            }

            val appendMode = (code == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L)
            val totalExpected = if (appendMode) {
                existingBytes + conn.contentLengthLong.coerceAtLeast(0L)
            } else {
                conn.contentLengthLong
            }
            conn.getHeaderField("ETag")?.let { etagFile.writeText(it) }

            conn.inputStream.use { input ->
                FileOutputStream(stagedFile, appendMode).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var copied = if (appendMode) existingBytes else 0L
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastReport >= 250L) {
                            onProgress(copied, totalExpected)
                            lastReport = now
                        }
                    }
                    onProgress(copied, totalExpected)
                }
            }

            val res = verifyAndActivateStaged(stageDir, stagedFile, expectedModel)
            if (res.isSuccess) stageDir.deleteRecursively()
            res
        } catch (t: Throwable) {
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.TRANSFER_INTERRUPTED)
            Result.failure(t)
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun importFromSaf(
        sourceUri: Uri,
        expectedModel: ModelDefinition,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Result<InstalledModelRecord> = withContext(Dispatchers.IO) {
        if (isLeased(expectedModel.modelId)) {
            return@withContext Result.failure(IllegalStateException("Model is currently leased by an active job"))
        }
        val displayName = queryDisplayName(sourceUri)
        if (displayName != expectedModel.physicalFileName) {
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.FILE_NAME_MISMATCH)
            return@withContext Result.failure(
                IllegalArgumentException("FILE_NAME_MISMATCH: Expected ${expectedModel.physicalFileName}, got $displayName")
            )
        }
        if (availableStorageMb() < expectedModel.minStorageMb) {
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.LOW_STORAGE)
            return@withContext Result.failure(IllegalStateException("LOW_STORAGE: Need ${expectedModel.minStorageMb} MB"))
        }

        val stageDir = File(stagingRoot, "${expectedModel.modelId}_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            updateStatus(expectedModel.modelId, ModelStatus.TRANSFERRING, null)
            val stagedFile = File(stageDir, expectedModel.physicalFileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(stagedFile).use { output ->
                    val buf = ByteArray(64 * 1024)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastReport >= 250L) {
                            onProgress(copied, -1L)
                            lastReport = now
                        }
                    }
                    onProgress(copied, copied)
                }
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open SAF URI"))

            verifyAndActivateStaged(stageDir, stagedFile, expectedModel)
        } finally {
            stageDir.deleteRecursively()
        }
    }

    private suspend fun verifyAndActivateStaged(
        stageDir: File,
        stagedFile: File,
        expectedModel: ModelDefinition
    ): Result<InstalledModelRecord> {
        updateStatus(expectedModel.modelId, ModelStatus.VERIFYING, null)
        val installContentDir = File(stageDir, "content").apply { mkdirs() }
        when (expectedModel.artifactFormat) {
            ArtifactFormat.GGML_BIN -> {
                GgmlHeaderValidator.validate(stagedFile).getOrElse { err ->
                    updateStatus(expectedModel.modelId, ModelStatus.CORRUPT, ErrorCode.INVALID_GGML_HEADER)
                    return Result.failure(err)
                }
                val destBin = File(installContentDir, expectedModel.physicalFileName)
                if (!stagedFile.renameTo(destBin)) {
                    stagedFile.copyTo(destBin, overwrite = true)
                    stagedFile.delete()
                }
            }
            ArtifactFormat.DISTRIBUTION_ZIP -> {
                FileInputStream(stagedFile).use { fis ->
                    ZipSecurityValidator.extractSafely(fis, installContentDir).getOrElse { err ->
                        updateStatus(expectedModel.modelId, ModelStatus.CORRUPT, ErrorCode.INVALID_ZIP)
                        return Result.failure(err)
                    }
                }
                validateZipModelManifest(expectedModel, installContentDir).getOrElse { err ->
                    updateStatus(expectedModel.modelId, ModelStatus.CORRUPT, ErrorCode.MISSING_MODEL_ASSET)
                    return Result.failure(err)
                }
            }
        }

        // E13: Native smoke test BEFORE marking READY
        runNativeSmokeTest(expectedModel, installContentDir).getOrElse { err ->
            updateStatus(expectedModel.modelId, ModelStatus.INCOMPATIBLE, ErrorCode.NATIVE_LIBRARY_NOT_FOUND)
            return Result.failure(err)
        }

        // E14: Transactional swap keeping .bak intact until ALL verification + .ready complete
        updateStatus(expectedModel.modelId, ModelStatus.INSTALLING, null)
        val liveDir = File(modelsRoot, expectedModel.modelId)
        val backupDir = File(modelsRoot, "${expectedModel.modelId}.bak")
        if (backupDir.exists()) backupDir.deleteRecursively()
        if (liveDir.exists() && !liveDir.renameTo(backupDir)) {
            return Result.failure(IllegalStateException("Failed to stage backup of existing model"))
        }

        return try {
            if (!installContentDir.renameTo(liveDir)) {
                throw IllegalStateException("Failed atomic model activation")
            }
            val record = verifyArtifact(expectedModel, liveDir.absolutePath).getOrThrow()
            File(liveDir, ".ready").writeText("verified=${System.currentTimeMillis()}")
            if (backupDir.exists()) backupDir.deleteRecursively()

            val updated = _records.value.toMutableMap()
            updated[expectedModel.modelId] = record
            _records.value = updated
            Result.success(record)
        } catch (t: Throwable) {
            if (liveDir.exists()) liveDir.deleteRecursively()
            if (backupDir.exists()) backupDir.renameTo(liveDir)
            updateStatus(expectedModel.modelId, ModelStatus.FAILED, ErrorCode.UNKNOWN)
            Result.failure(t)
        }
    }

    private fun validateZipModelManifest(model: ModelDefinition, dir: File): Result<Unit> {
        val files = dir.walkTopDown().filter { it.isFile }.toList()
        val hasTokens = files.any { it.name.contains("tokens", ignoreCase = true) }
        if (!hasTokens) {
            return Result.failure(IllegalStateException("MISSING_MODEL_ASSET: tokens file missing"))
        }
        return when (model.engineType) {
            EngineType.SHERPA_MOONSHINE_AR -> {
                val hasPre = files.any { it.name.contains("preprocess") }
                val hasEnc = files.any { it.name.contains("encode") }
                val hasDec = files.any { it.name.contains("decode") || it.name.contains("cached") }
                if (hasPre && hasEnc && hasDec) Result.success(Unit)
                else Result.failure(IllegalStateException("MISSING_MODEL_ASSET: Incomplete Moonshine bundle"))
            }
            EngineType.SHERPA_SENSEVOICE -> {
                val hasOnnx = files.any { it.extension == "onnx" || it.extension == "ort" }
                if (hasOnnx) Result.success(Unit)
                else Result.failure(IllegalStateException("MISSING_MODEL_ASSET: Missing SenseVoice .onnx/.ort"))
            }
            EngineType.WHISPER_CPP -> Result.success(Unit)
        }
    }

    private fun runNativeSmokeTest(model: ModelDefinition, dir: File): Result<Unit> {
        return when (model.engineType) {
            EngineType.WHISPER_CPP -> {
                if (!WhisperNativeBridge.isAvailable()) {
                    return Result.failure(IllegalStateException("NATIVE_LIBRARY_NOT_FOUND: whisper.cpp JNI not linked"))
                }
                val bin = File(dir, model.physicalFileName)
                val handle = WhisperNativeBridge.nativeInit(bin.absolutePath)
                if (handle == 0L) {
                    Result.failure(IllegalStateException("INVALID_GGML_HEADER: nativeInit smoke test failed"))
                } else {
                    WhisperNativeBridge.nativeFree(handle)
                    Result.success(Unit)
                }
            }
            EngineType.SHERPA_MOONSHINE_AR, EngineType.SHERPA_SENSEVOICE -> {
                if (!SherpaNativeBridge.isSherpaAarPresent()) {
                    Result.failure(IllegalStateException("NATIVE_LIBRARY_NOT_FOUND: sherpa-onnx AAR not bundled"))
                } else {
                    Result.success(Unit)
                }
            }
        }
    }

    override suspend fun verifyArtifact(
        model: ModelDefinition,
        filePath: String
    ): Result<InstalledModelRecord> = withContext(Dispatchers.IO) {
        val dir = File(filePath)
        if (!dir.exists()) return@withContext Result.failure(IllegalStateException("Model directory missing"))
        val primary = resolvePrimaryArtifact(model, dir)
            ?: return@withContext Result.failure(IllegalStateException("Primary model artifact missing"))

        if (model.artifactFormat == ArtifactFormat.GGML_BIN) {
            GgmlHeaderValidator.validate(primary).getOrElse { return@withContext Result.failure(it) }
        } else {
            validateZipModelManifest(model, dir).getOrElse { return@withContext Result.failure(it) }
        }

        val sha = computeSha256(primary.absolutePath)
        val trust = if (model.pinnedSha256 != null && model.pinnedSha256.equals(sha, ignoreCase = true)) {
            ChecksumTrust.VERIFIED_PINNED
        } else {
            ChecksumTrust.UNVERIFIED_LOCAL_ONLY
        }
        val rec = InstalledModelRecord(
            modelId = model.modelId,
            status = ModelStatus.READY,
            installedPath = dir.absolutePath,
            localSha256 = sha,
            checksumTrust = trust,
            sizeBytes = directorySize(dir),
            profileVersion = 1,
            validatedAtEpochMs = System.currentTimeMillis()
        )
        val map = _records.value.toMutableMap()
        map[model.modelId] = rec
        _records.value = map
        Result.success(rec)
    }

    override suspend fun computeSha256(filePath: String): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(File(filePath)).use { fis ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = fis.read(buf)
                if (read <= 0) break
                digest.update(buf, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    // E12: Exports full ZIP bundle for DISTRIBUTION_ZIP models so re-import succeeds with all assets
    override suspend fun exportToSaf(
        model: ModelDefinition,
        installedPath: String,
        targetUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val dir = File(installedPath)
        if (!dir.exists()) return@withContext Result.failure(IllegalStateException("Model directory missing"))
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                when (model.artifactFormat) {
                    ArtifactFormat.GGML_BIN -> {
                        val bin = File(dir, model.physicalFileName)
                        if (!bin.exists()) return@withContext Result.failure(IllegalStateException("GGML file missing"))
                        FileInputStream(bin).use { it.copyTo(out, 64 * 1024) }
                    }
                    ArtifactFormat.DISTRIBUTION_ZIP -> {
                        ZipOutputStream(out.buffered()).use { zos ->
                            val buf = ByteArray(64 * 1024)
                            dir.walkTopDown()
                                .filter { it.isFile && it.name != ".ready" }
                                .forEach { file ->
                                    val rel = file.relativeTo(dir).invariantSeparatorsPath
                                    zos.putNextEntry(ZipEntry(rel))
                                    FileInputStream(file).use { fis ->
                                        while (true) {
                                            val r = fis.read(buf)
                                            if (r <= 0) break
                                            zos.write(buf, 0, r)
                                        }
                                    }
                                    zos.closeEntry()
                                }
                        }
                    }
                }
            } ?: return@withContext Result.failure(IllegalStateException("Cannot open export SAF URI"))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun deleteModel(modelId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (isLeased(modelId)) {
            return@withContext Result.failure(IllegalStateException("Model is currently in use by an active job"))
        }
        val dir = File(modelsRoot, modelId)
        if (dir.exists()) dir.deleteRecursively()
        refreshInventoryQuick()
        Result.success(Unit)
    }

    private fun resolvePrimaryArtifact(model: ModelDefinition, dir: File): File? {
        return when (model.artifactFormat) {
            ArtifactFormat.GGML_BIN -> File(dir, model.physicalFileName).takeIf { it.exists() }
            ArtifactFormat.DISTRIBUTION_ZIP -> dir.walkTopDown()
                .firstOrNull { it.isFile && (it.extension == "onnx" || it.extension == "ort") }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun directorySize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun updateStatus(modelId: String, status: ModelStatus, error: ErrorCode?) {
        val map = _records.value.toMutableMap()
        val prev = map[modelId]
        map[modelId] = (prev ?: InstalledModelRecord(
            modelId = modelId,
            status = status,
            installedPath = null,
            localSha256 = null,
            checksumTrust = ChecksumTrust.UNVERIFIED_LOCAL_ONLY,
            sizeBytes = 0L,
            profileVersion = 1,
            validatedAtEpochMs = null
        )).copy(status = status, lastError = error)
        _records.value = map
    }
}
