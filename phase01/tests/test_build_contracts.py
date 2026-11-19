"""Phase 01 pre-fix reproducible build-contract tests.

These are static contracts, NOT substitutes for Gradle compilation or an Android device.
Run: python3 -m unittest discover -s phase01/tests -p 'test_*.py' -v
"""
from pathlib import Path
import hashlib
import re
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
APP = ROOT / 'app'


def source(path):
    return (ROOT / path).read_text(encoding='utf-8')


class BuildContracts(unittest.TestCase):
    def test_exporter_constructor_is_defined(self):
        app = source('app/src/main/java/com/transvibe/app/TransVibeApplication.kt')
        exporter = source('app/src/main/java/com/transvibe/app/subtitles/SrtVttExporter.kt')
        self.assertRegex(exporter, r'class\s+SrtVttExporter\s*\(')
        self.assertIn('SrtVibeExporterWrapper(SrtVttExporter(app))', app)
        self.assertNotIn('SrtVibeExporter(app)', app)

    def test_escaped_newlines_and_whitespace_regex_are_valid_source(self):
        exporter = source('app/src/main/java/com/transvibe/app/subtitles/SrtVttExporter.kt')
        detector = source('app/src/main/java/com/transvibe/app/subtitles/HallucinationDetector.kt')
        translation = source('app/src/main/java/com/transvibe/app/translation/MlKitTranslationEngine.kt')
        self.assertIn("append('\\n')", exporter)
        self.assertIn('StringBuilder("WEBVTT\\n\\n")', exporter)
        for content in (detector, translation):
            self.assertIn('"\\\\s+".toRegex()', content)
        self.assertNotIn('"\\s+".toRegex()', detector)
        self.assertNotIn('"\\s+".toRegex()', translation)

    def test_wrapper_present_with_verified_published_sha(self):
        wrapper = ROOT / 'gradle/wrapper/gradle-wrapper.jar'
        self.assertTrue((ROOT / 'gradlew').exists())
        self.assertTrue((ROOT / 'gradlew.bat').exists())
        self.assertTrue(wrapper.is_file())
        digest = hashlib.sha256(wrapper.read_bytes()).hexdigest()
        self.assertIn(digest, {
            # Gradle 8.11.1 wrapper jar, published at gradle.org/release-checksums/
            '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046',
            # Official Gradle 9.7.0 wrapper jar SHA-256; host uses 9.6.1 distribution
            '7d3a4ac4de1c32b59bc6a4eb8ecb8e612ccd0cf1ae1e99f66902da64df296172',
        })
        self.assertIn('org.gradle.wrapper.GradleWrapperMain', source('gradlew'))
        self.assertIn('org.gradle.wrapper.GradleWrapperMain', source('gradlew.bat'))

    def test_native_diagnostic_requires_explicit_non_release_opt_in(self):
        cmake = source('app/src/main/cpp/CMakeLists.txt')
        build = source('app/build.gradle.kts')
        self.assertIn('TRANSVIBE_ALLOW_DIAGNOSTIC_JNI', cmake)
        self.assertIn('message(FATAL_ERROR', cmake)
        self.assertIn('TRANSVIBE_ALLOW_DIAGNOSTIC_JNI=ON', build)
        self.assertIn('TRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF', build)

    def test_first_model_release_requires_signed_verified_real_whisper(self):
        build = source('app/build.gradle.kts')
        # Stage 1 is Whisper Tiny; Sherpa is a later, separately gated model.
        # Missing real prebuilt JNI OR signing credentials MUST still fail closed.
        for token in ('verifyReleaseNativeDependencies', 'preReleaseBuild',
                      'check(hasPrebuiltWhisperJni)', 'check(hasValidSigningProps)',
                      'native-provenance.json', 'libs/arm64-v8a/libtransvibe_whisper.so',
                      'jsonHash("binary_sha256")', 'jsonHash("jni_source_sha256")',
                      'jsonHash("kotlin_source_sha256")'):
            self.assertIn(token, build)
        self.assertIn('sherpa-onnx-', build)
        self.assertNotIn('check(sherpaArchives.size == 1)', build)

    def test_preview_separate_and_debug_warning_visible(self):
        build = source('app/build.gradle.kts')
        settings = source('app/src/main/java/com/transvibe/app/presentation/screens/SettingsScreen.kt')
        manifest = source('app/src/main/AndroidManifest.xml')
        self.assertIn('applicationId = "com.transvibe.app"', build)
        self.assertIn('applicationIdSuffix = ".preview"', build)
        self.assertIn('DIAGNOSTIC_NATIVE_ALLOWED', settings)
        self.assertIn('${applicationId}.fileprovider', manifest)
        self.assertNotIn('android:sharedUserId', manifest)

    def test_current_gradle_toolchain_pins_are_stable(self):
        libs = source('gradle/libs.versions.toml')
        props = source('gradle/wrapper/gradle-wrapper.properties')
        build = source('app/build.gradle.kts')
        self.assertRegex(libs, r'agp = "9\.3\.1"')
        self.assertRegex(libs, r'kotlin = "2\.3\.21"')
        self.assertIn('gradle-9.6.1-bin.zip', props)
        for t in ['minSdk = 26','targetSdk = 35','compileSdk = 36','"arm64-v8a"']:
            self.assertIn(t, build)


if __name__ == '__main__':
    unittest.main()

# Run the native CMake configure without an Android toolchain. This checks only
# the fail-closed gate: it is NOT Android JNI compilation or ASR validation.
class NativeCmakeGate(unittest.TestCase):
    def test_release_cmake_default_fails_closed(self):
        import shutil, subprocess, tempfile
        if not shutil.which('cmake'):
            self.skipTest('cmake unavailable')
        with tempfile.TemporaryDirectory(prefix='transvibe_release_gate_') as tmp:
            completed = subprocess.run(['cmake', '-S', str(APP/'src/main/cpp'),
                                        '-B',tmp,'-DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF'],
                                       text=True, capture_output=True, timeout=30)
        self.assertNotEqual(completed.returncode, 0)
        self.assertIn('RELEASE BLOCKED:', completed.stdout + completed.stderr)

    def test_debug_cmake_configuration_is_explicitly_diagnostic(self):
        import shutil, subprocess, tempfile
        if not shutil.which('cmake'):
            self.skipTest('cmake unavailable')
        with tempfile.TemporaryDirectory(prefix='transvibe_debug_gate_') as tmp:
            completed = subprocess.run(['cmake', '-S', str(APP/'src/main/cpp'),
                                        '-B',tmp,'-DTRANSVIBE_ALLOW_DIAGNOSTIC_JNI=ON'],
                                       text=True, capture_output=True, timeout=30)
        self.assertEqual(completed.returncode, 0, completed.stdout + completed.stderr)
        self.assertIn('DIAGNOSTIC ONLY', completed.stdout + completed.stderr)

class Phase00SourceProtection(unittest.TestCase):
    def test_unchanged_original_members_preserve_phase00_hashes(self):
        # Phase 00's archive test deliberately expects NO files to change; use
        # its original 98-file manifest plus an explicit Phase 01 allowlist.
        # Unrelated app code/data must remain byte-identical.
        import csv
        expected_modified = {
            'CODE_ON_THE_GO_BUILD.md',
            'app/build.gradle.kts',
            'app/src/main/cpp/CMakeLists.txt',
            'app/src/main/java/com/transvibe/app/TransVibeApplication.kt',
            'app/src/main/java/com/transvibe/app/presentation/screens/SettingsScreen.kt',
            'app/src/main/java/com/transvibe/app/presentation/TransVibeViewModel.kt',
            'app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt',
            'gradle/wrapper/gradle-wrapper.properties',
            'gradle/libs.versions.toml',
            'build.gradle.kts',
            'app/src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt',
            'app/src/main/cpp/whisper_jni.cpp',
            # CI-discovered source-language routing bug fixed during Phase 01.
            'app/src/main/java/com/transvibe/app/domain/Policies.kt',
            'app/src/test/java/com/transvibe/app/ModelCatalogTest.kt',
            # On-device Whisper Tiny 41s/41s translation-stall regression.
            'app/src/main/java/com/transvibe/app/translation/TranslationPipeline.kt',
            'app/src/main/java/com/transvibe/app/translation/MlKitTranslationEngine.kt',
            'app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt',
            'app/src/main/java/com/transvibe/app/presentation/screens/ProcessingScreen.kt',
            'app/src/main/java/com/transvibe/app/platform/TransVibeProcessingService.kt',
            'app/src/test/java/com/transvibe/app/TranslationRoutingAndFixesTest.kt',
            'app/src/main/AndroidManifest.xml',
            'settings.gradle.kts',
            'gradle.properties',
        }
        inventory = ROOT / 'phase00/PHASE00_SOURCE_INVENTORY_SHA256.csv'
        with inventory.open(encoding='utf-8-sig', newline='') as inp:
            rows = list(csv.DictReader(inp))
        self.assertEqual(len(rows), 98)
        changed = set()
        for item in rows:
            relative = item['ZIP relative path'].removeprefix('TransVibe/')
            file_path = ROOT / relative
            self.assertTrue(file_path.is_file(), f'Original file disappeared: {relative}')
            actual = hashlib.sha256(file_path.read_bytes()).hexdigest()
            previous = item['SHA-256 original member']
            if actual != previous:
                changed.add(relative)
            elif relative in expected_modified:
                self.fail(f'Expected Phase01 fix missing: {relative}')
            elif relative not in expected_modified:
                self.assertEqual(actual, previous, f'Unrelated source modified: {relative}')
        # A source-only Git checkout intentionally has NO native binary yet.
        # Validate the exact JNI source both before and after a verified SO import.
        import json
        jni_hash = hashlib.sha256((ROOT/'app/src/main/cpp/whisper_jni.cpp').read_bytes()).hexdigest()
        self.assertEqual(jni_hash, 'cd05863b8b34552daf76019c4ee85678401df55c3ee1096923661661a60de00a')
        proof_path = ROOT/'app/libs/native-provenance.json'
        native = ROOT/'app/libs/arm64-v8a/libtransvibe_whisper.so'
        if proof_path.is_file():
            proof = json.loads(proof_path.read_text(encoding='utf-8'))
            self.assertTrue(native.is_file(), 'Native provenance must accompany the ELF')
            self.assertEqual(proof['jni_source_sha256'], jni_hash)
            self.assertEqual(proof['binary_sha256'], hashlib.sha256(native.read_bytes()).hexdigest())
            self.assertEqual(proof['builder_repository'], 'bingattban/TransVibe-so-')
        else:
            self.assertFalse(native.exists(), 'Unverified native binary must never be accepted')
        self.assertEqual(changed, expected_modified)
