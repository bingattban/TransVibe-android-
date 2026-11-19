package com.transvibe.app.models

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object ZipSecurityValidator {
    private const val MAX_ENTRIES = 128
    private const val MAX_UNCOMPRESSED_BYTES = 1_500_000_000L // 1.5 GB ceiling
    private const val MAX_COMPRESSION_RATIO = 100.0

    fun extractSafely(zipInput: InputStream, targetRoot: File): Result<List<File>> {
        if (!targetRoot.exists() && !targetRoot.mkdirs()) {
            return Result.failure(IllegalStateException("Cannot create target directory"))
        }
        val canonicalRoot = targetRoot.canonicalFile
        val extracted = mutableListOf<File>()
        val seenNames = mutableSetOf<String>()
        var totalBytes = 0L
        var entryCount = 0

        return try {
            ZipInputStream(zipInput.buffered()).use { zis ->
                val buffer = ByteArray(16 * 1024)
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRIES) {
                        return Result.failure(SecurityException("ZIP entry count exceeds limit"))
                    }
                    val name = entry.name
                    if (name.startsWith("/") || name.startsWith("\\") || name.contains("..")) {
                        return Result.failure(SecurityException("Unsafe ZIP path traversal entry: $name"))
                    }
                    if (!seenNames.add(name)) {
                        return Result.failure(SecurityException("Duplicate ZIP entry: $name"))
                    }
                    val outFile = File(canonicalRoot, name).canonicalFile
                    if (!outFile.path.startsWith(canonicalRoot.path + File.separator) &&
                        outFile != canonicalRoot
                    ) {
                        return Result.failure(SecurityException("ZIP slip detected for entry: $name"))
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        var entryBytes = 0L
                        FileOutputStream(outFile).use { fos ->
                            while (true) {
                                val read = zis.read(buffer)
                                if (read <= 0) break
                                entryBytes += read
                                totalBytes += read
                                if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                    return Result.failure(SecurityException("ZIP total uncompressed size exceeds limit"))
                                }
                                val compressed = entry.compressedSize
                                if (compressed > 0 && entryBytes.toDouble() / compressed.toDouble() > MAX_COMPRESSION_RATIO) {
                                    return Result.failure(SecurityException("ZIP bomb compression ratio detected"))
                                }
                                fos.write(buffer, 0, read)
                            }
                        }
                        extracted.add(outFile)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            if (extracted.isEmpty()) {
                Result.failure(IllegalArgumentException("ZIP archive contains no files"))
            } else {
                Result.success(extracted)
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
