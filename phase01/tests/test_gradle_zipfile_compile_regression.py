"""Phase 01: static regression for Kotlin DSL's `java` receiver shadowing.

Run before/after the fix. This tests source shape, not the full Android build.
"""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "app/build.gradle.kts"


class ReleaseNativeGateKotlinDslRegression(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = SCRIPT.read_text(encoding="utf-8")

    def test_zipfile_import_is_explicit_not_java_extension(self):
        self.assertRegex(self.source, r"(?m)^import\s+java\.util\.zip\.ZipFile\s*$")
        self.assertRegex(self.source, r"\bZipFile\(sherpaArchives\.single\(\)\)\.use\s*\{")
        self.assertNotIn("java.util.zip.ZipFile(sherpaArchives.single())", self.source)

    def test_release_guard_registers_without_deprecated_delegate(self):
        self.assertRegex(self.source, r"tasks\.register\(\s*\"verifyReleaseNativeDependencies\"\s*\)")
        self.assertNotIn("by tasks.registering", self.source)

    def test_release_gate_stays_closed_but_diagnostic_preview_allowed(self):
        for token in (
            "include/whisper.h", "sherpa-onnx-*.aar", "jni/arm64-v8a/libonnxruntime.so",
            "preReleaseBuild", "externalNativeBuildRelease", "assembleRelease", "bundleRelease",
            "dependsOn(verifyReleaseNativeDependencies)",
        ):
            self.assertIn(token, self.source)
        self.assertIn('applicationIdSuffix = ".preview"', self.source)
        self.assertIn('"-DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF"', self.source)


if __name__ == "__main__":
    unittest.main()
