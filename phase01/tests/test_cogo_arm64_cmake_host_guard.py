#!/usr/bin/env python3
"""Phase01 deterministic regression check against Code On The Go CMake host-toolchain failure.

This test is source-level only: it does not simulate an Android build or native inference.
"""
import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
GRADLE = ROOT / 'app/build.gradle.kts'

class CoGoArm64NativeBuildGuardTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.content = GRADLE.read_text('utf-8')

    def test_mobile_host_detected_by_jvm_arch_and_cogo_home(self):
        self.assertIn('val isCoGoArm64Host =', self.content)
        self.assertIn('System.getProperty("os.arch")', self.content)
        self.assertIn('gradle.gradleUserHomeDir', self.content)
        self.assertIn('/data/data/com.itsaky.androidide/', self.content)

    def test_native_gradle_integration_disabled_only_for_this_mobile_host(self):
        self.assertRegex(self.content, r'if \(!isCoGoArm64Host && !hasPrebuiltWhisperJni\)\s*\{\s*externalNativeBuild\s*\{\s*cmake\s*\{\s*path\s*=')

    def test_release_gated_when_mobile_native_build_skipped(self):
        self.assertRegex(self.content, r'doLast\s*\{\s*check\(!isCoGoArm64Host\)')
        self.assertIn('RELEASE BLOCKED', self.content)
        self.assertIn('verifyReleaseNativeDependencies', self.content)

    def test_debug_preview_explicit_runtime_absence_is_not_success(self):
        self.assertIn('COGO ARM64 DIAGNOSTIC-ONLY', self.content)
        self.assertIn('DIAGNOSTIC_NATIVE_BUILD_SKIPPED', self.content)
        self.assertIn('applicationIdSuffix = ".preview"', self.content)
        self.assertIn('DIAGNOSTIC_NATIVE_ALLOWED', self.content)
        self.assertIn('com.transvibe.app', self.content)

    def test_preview_ui_warns_that_native_asr_is_not_built(self):
        home = (ROOT / 'app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt').read_text('utf-8')
        self.assertIn('if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable())', home)
        self.assertIn('Whisper', home)
        self.assertIn('لم تُكتشف مكتبة Whisper', home)

    def test_diagnostic_build_flag_is_declared_once_per_build_type(self):
        self.assertEqual(3, self.content.count('buildConfigField("boolean", "DIAGNOSTIC_NATIVE_BUILD_SKIPPED",'))

    def test_android_identity_room_and_native_sources_untouched(self):
        self.assertEqual('6', re.search(r'versionCode\s*=\s*(\d+)', self.content).group(1))
        self.assertIn('minSdk = 26', self.content)
        self.assertIn('targetSdk = 35', self.content)
        self.assertTrue((ROOT / 'app/src/main/cpp/whisper_jni.cpp').is_file())
        self.assertTrue((ROOT / 'app/src/main/cpp/CMakeLists.txt').is_file())
        self.assertTrue((ROOT / 'app/src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt').is_file())

if __name__ == '__main__':
    unittest.main()
