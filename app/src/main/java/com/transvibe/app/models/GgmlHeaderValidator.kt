package com.transvibe.app.models

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GgmlHeaderValidator {
    // Known whisper.cpp GGML / GGJT magic numbers (little-endian)
    private val VALID_MAGICS = setOf(
        0x67676d6c, // "lmgg" / "ggml"
        0x67676d66, // "ggmf"
        0x67676a74  // "ggjt"
    )
    private const val MIN_GGML_BYTES = 1024L * 1024L // >= 1 MiB

    fun validate(file: File): Result<Unit> {
        if (!file.exists() || !file.isFile) {
            return Result.failure(IllegalArgumentException("GGML file does not exist"))
        }
        if (file.length() < MIN_GGML_BYTES) {
            return Result.failure(IllegalArgumentException("GGML file too small (${file.length()} bytes)"))
        }
        return RandomAccessFile(file, "r").use { raf ->
            val buf = ByteArray(4)
            val read = raf.read(buf)
            if (read != 4) {
                return@use Result.failure(IllegalArgumentException("Unable to read GGML magic header"))
            }
            val magicLE = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
            val magicBE = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN).int
            if (magicLE in VALID_MAGICS || magicBE in VALID_MAGICS) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Invalid GGML magic: 0x${magicLE.toUInt().toString(16)}"))
            }
        }
    }
}
