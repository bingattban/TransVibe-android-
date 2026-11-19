"""Regression tests for the actual Code On The Go 9.6.1 / AGP 9.3.1 build environment.

Structural contracts only. A PASS here does NOT mean Gradle compiled an APK.
Run as part of the existing unittest discovery; no shell or terminal is required on the user's phone.
"""
from pathlib import Path
import re
import unittest
import hashlib

ROOT = Path(__file__).resolve().parents[2]

def source(relative):
    return (ROOT / relative).read_text(encoding='utf-8')

class CodeOnTheGoUIBuildContracts(unittest.TestCase):
    def test_official_cogo_toolchain_is_selected(self):
        catalog = source('gradle/libs.versions.toml')
        self.assertRegex(catalog, r'(?m)^agp = "9\.3\.1"$')
        self.assertRegex(catalog, r'(?m)^kotlin = "2\.3\.21"$')
        self.assertRegex(catalog, r'(?m)^ksp = "2\.3\.14"$')

    def test_root_buildscript_provides_injected_plugin_kotlin_classpath(self):
        script = source('build.gradle.kts')
        self.assertIn('buildscript {', script)
        self.assertIn('classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")', script)
        self.assertIn('alias(libs.plugins.android.application) apply false', script)
        self.assertNotIn('alias(libs.plugins.kotlin.android) apply false', script)

    def test_android_9_builtin_kotlin_and_jvm_target(self):
        script = source('app/build.gradle.kts')
        self.assertNotIn('alias(libs.plugins.kotlin.android)', script)
        self.assertNotIn('kotlinOptions', script)
        self.assertRegex(script, r'kotlin\s*\{\s*compilerOptions\s*\{')
        self.assertIn('JvmTarget.JVM_17', script)
        self.assertIn('alias(libs.plugins.kotlin.compose)', script)
        self.assertIn('alias(libs.plugins.kotlin.serialization)', script)
        self.assertIn('alias(libs.plugins.ksp)', script)

    def test_wrapper_distribution_synchronized_with_cogo(self):
        props = source('gradle/wrapper/gradle-wrapper.properties')
        self.assertIn('gradle-9.6.1-bin.zip', props)
        self.assertIn('9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14', props)
        self.assertTrue((ROOT / 'gradle/wrapper/gradle-wrapper.jar').is_file())

    def test_no_app_identity_or_release_gate_regression(self):
        script = source('app/build.gradle.kts')
        self.assertIn('applicationId = "com.transvibe.app"', script)
        self.assertIn('applicationIdSuffix = ".preview"', script)
        self.assertIn('verifyReleaseNativeDependencies', script)
        self.assertIn('TRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF', script)
        self.assertIn('minSdk = 26', script)
        self.assertIn('targetSdk = 35', script)

    def test_ui_first_handoff_documents_correct_build_action(self):
        guide = source('phase01/COTG_UI_ONLY_AR.md')
        self.assertIn('Build', guide)
        self.assertIn('9.6.1', guide)
        self.assertIn('9.3.1', guide)
        self.assertIn('Preview', guide)
        self.assertIn('بدون طرفية', guide)
        self.assertIn('لا تحذف', guide)

if __name__ == '__main__':
    unittest.main()
