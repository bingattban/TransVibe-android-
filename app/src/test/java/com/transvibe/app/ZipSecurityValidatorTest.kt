package com.transvibe.app

import com.transvibe.app.models.ZipSecurityValidator
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipSecurityValidatorTest {

    @Test
    fun rejectsZipSlipPathTraversal() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.onnx"))
            zos.write("bad".toByteArray())
            zos.closeEntry()
        }
        val tmpDir = Files.createTempDirectory("tv_zip_slip").toFile()
        try {
            val res = ZipSecurityValidator.extractSafely(ByteArrayInputStream(baos.toByteArray()), tmpDir)
            assertTrue(res.isFailure)
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun extractsValidArchiveSafely() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            zos.putNextEntry(ZipEntry("model/encoder.onnx"))
            zos.write("onnx-bytes".toByteArray())
            zos.closeEntry()
        }
        val tmpDir = Files.createTempDirectory("tv_zip_ok").toFile()
        try {
            val res = ZipSecurityValidator.extractSafely(ByteArrayInputStream(baos.toByteArray()), tmpDir)
            assertTrue(res.isSuccess)
            assertTrue(res.getOrThrow().any { it.name == "encoder.onnx" })
        } finally {
            tmpDir.deleteRecursively()
        }
    }
}
