"""Regression contract: keep Code On The Go LogSender and fail broken local APKs.

The APK check runs in Gradle after assembling every installable variant.
This test is source-level; actual APK packaging is validated by Gradle/CI.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class CoGoRuntimeBundlingContract(unittest.TestCase):
    def test_debug_logsender_is_kept_not_disabled(self):
        manifest = read("app/src/main/AndroidManifest.xml")
        bridge = read("app/src/main/java/com/transvibe/app/platform/LogSenderBridge.kt")
        self.assertIn("android.permission.FOREGROUND_SERVICE_DATA_SYNC", manifest)
        self.assertIn("android.permission.FOREGROUND_SERVICE", manifest)
        self.assertIn("LogSenderBridge", bridge)
        self.assertIn("LOG_SERVICE", bridge)
        self.assertNotIn("tools:node=\"remove\"", manifest)
        self.assertNotIn("LogSenderInstaller", manifest)

    def test_core_and_mlkit_common_are_explicit_runtime_libraries(self):
        catalog = read("gradle/libs.versions.toml")
        build = read("app/build.gradle.kts")
        self.assertIn("mlkitCommon = \"18.11.0\"", catalog)
        self.assertIn("mlkit-common = { group = \"com.google.mlkit\", name = \"common\"", catalog)
        self.assertIn("androidx-core = { group = \"androidx.core\", name = \"core\"", catalog)
        for token in ("implementation(libs.androidx.core)", "implementation(libs.mlkit.common)",
                      "implementation(libs.mlkit.translate)", "implementation(libs.mlkit.language.id)"):
            self.assertIn(token, build)

    def test_each_local_apk_fails_if_runtime_classes_or_whisper_missing(self):
        build = read("app/build.gradle.kts")
        for token in (
            "verifyDebugPackagedRuntime", "verifyPreviewPackagedRuntime",
            "verifyReleasePackagedRuntime", "finalizedBy",
            "classes(\\\\d+)?\\\\.dex",
            "Lcom/google/mlkit/common/internal/MlKitInitProvider;",
            "Landroidx/core/app/CoreComponentFactory;",
            "Lcom/transvibe/app/MainActivity;",
            "Lcom/transvibe/app/TransVibeApplication;",
            "lib/arm64-v8a/libtransvibe_whisper.so",
            "classDefsSize", "classDefsOffset",
            "APK RUNTIME CHECK FAILED",
        ):
            self.assertIn(token, build)

    def test_source_retains_provenance_and_build_guidance(self):
        build = read("app/build.gradle.kts")
        guide = read("CODE_ON_THE_GO_BUILD.md")
        self.assertIn('jniLibs.srcDir("libs")', build)
        self.assertTrue((ROOT / "app/libs/arm64-v8a/libtransvibe_whisper.so").is_file())
        self.assertTrue((ROOT / "app/libs/native-provenance.json").is_file())
        self.assertIn("LogSender", guide)
        self.assertIn("MlKitInitProvider", guide)
        self.assertIn("Preview", guide)
        self.assertIn("DO NOT", guide)

if __name__ == "__main__":
    unittest.main()
