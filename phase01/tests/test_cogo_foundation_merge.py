"""Phase 01 CoGo foundation merger: test-first, no Android-build claims.

Expected to fail against the untouched CoGo starter.  A PASS after merging
proves only source/Gradle composition, NOT a successful Android build.
"""
from pathlib import Path
import re
import csv
import unittest
import hashlib

ROOT = Path(__file__).resolve().parents[2]
CODE_ROOT = ROOT / 'app/src/main/java/com/transvibe/app'


class CoGoFoundationContract(unittest.TestCase):
    def test_toolchain_from_user_created_starter(self):
        cat = (ROOT/'gradle/libs.versions.toml').read_text()
        self.assertIn('agp = "9.3.1"', cat)
        self.assertIn('kotlin = "2.3.21"', cat)
        self.assertIn('ksp = "2.3.14"', cat)
        wrapper = ROOT/'gradle/wrapper/gradle-wrapper.jar'
        self.assertEqual(hashlib.sha256(wrapper.read_bytes()).hexdigest(),
                         '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046')

    def test_transvibe_identity_and_safe_preview(self):
        settings = (ROOT/'settings.gradle.kts').read_text()
        app = (ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('rootProject.name = "TransVibe"',settings)
        for token in ('namespace = "com.transvibe.app"','applicationId = "com.transvibe.app"',
                      'applicationIdSuffix = ".preview"','versionCode = 6'):
            self.assertIn(token,app)
        self.assertIn('TransVibeApplication', (ROOT/'app/src/main/AndroidManifest.xml').read_text())
        self.assertIn('name="app_name">TransVibe</string>',(ROOT/'app/src/main/res/values/strings.xml').read_text())

    def test_all_previous_production_sources_preserved(self):
        manifest=ROOT/'phase01/merge/TRANSVIBE_PREVIOUS_SOURCE_SHA256.csv'
        with manifest.open(encoding='utf-8',newline='') as src:
            rows=list(csv.DictReader(src))
        self.assertEqual(len(rows),73)
        for item in rows:
            rel=item['relative_path']
            target=ROOT/rel
            self.assertTrue(target.is_file(),f'Missing {rel}')
            if rel=='app/src/main/AndroidManifest.xml':
                xml=target.read_text()
                self.assertIn('android:icon="@mipmap/ic_launcher"',xml)
                self.assertIn('android:roundIcon="@mipmap/ic_launcher_round"',xml)
                self.assertIn('android:name=".TransVibeApplication"',xml)
                continue
            if rel == 'app/src/main/cpp/whisper_jni.cpp':
                import json
                proof=json.loads((ROOT/'app/libs/native-provenance.json').read_text())
                self.assertEqual(proof['jni_source_sha256'], hashlib.sha256(target.read_bytes()).hexdigest())
                self.assertEqual(proof['binary_sha256'], hashlib.sha256((ROOT/'app/libs/arm64-v8a/libtransvibe_whisper.so').read_bytes()).hexdigest())
                continue
            if rel == 'app/src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt':
                # Approved Phase 01: guard JNI linkage probes if the installed .so
                # is incompatible; all external method names must remain unchanged.
                source=target.read_text(encoding='utf-8')
                self.assertIn('catch (_: UnsatisfiedLinkError)', source)
                self.assertIn('nativeIsLinked()', source)
                for symbol in ('nativeInit', 'nativeTranscribe', 'nativeFree', 'nativeCancel'):
                    self.assertIn(symbol,source)
                continue
            if rel == 'app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt':
                # The only approved UI change is the explicit diagnostic warning;
                # removing it must recreate the exact previous source SHA.
                current = target.read_text(encoding='utf-8')
                start = current.index('        if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable()) {\n')
                end = current.index('        GlassCard(modifier = Modifier.fillMaxWidth()) {\n',
                                    start + len('        if (BuildConfig.DIAGNOSTIC_NATIVE_BUILD_SKIPPED && !WhisperNativeBridge.isAvailable()) {\n'))
                # The first GlassCard is the diagnostic banner; the second is the
                # existing video-pick card, after the banner's closing brace.
                end = current.index('        GlassCard(modifier = Modifier.fillMaxWidth()) {\n', end + 1)
                before_banner = current[:start] + current[end:]
                before_banner = before_banner.replace(
                    'import com.transvibe.app.nativebridge.WhisperNativeBridge\n', ''
                )
                self.assertEqual(hashlib.sha256(before_banner.encode()).hexdigest(),
                                 item['sha256'], 'Unexpected non-banner changes in HomeScreen')
                continue
            self.assertEqual(hashlib.sha256(target.read_bytes()).hexdigest(),item['sha256'],f'Changed source {rel}')

    def test_old_regression_and_phase_reports_preserved(self):
        self.assertTrue((ROOT/'app/src/test/java/com/transvibe/app/ZipSecurityValidatorTest.kt').is_file())
        self.assertTrue((ROOT/'phase00/PHASE00_FORENSIC_REPORT.md').is_file())
        self.assertTrue((ROOT/'phase01/PHASE01_COGO_GRADLE_ZIPFILE_FIX_REPORT_AR.md').is_file())

    def test_cogo_starter_resources_preserved(self):
        file=ROOT/'phase01/merge/COGO_STARTER_RESOURCES_SHA256.csv'
        with file.open(encoding='utf-8',newline='') as src:
            rows=list(csv.DictReader(src))
        self.assertGreaterEqual(len(rows),15)
        for item in rows:
            rel=item['relative_path']
            if rel in {'app/src/main/res/values/strings.xml','app/src/main/res/values/themes.xml',
                       'app/src/main/res/xml/backup_rules.xml','app/src/main/res/xml/data_extraction_rules.xml'}:
                continue # TransVibe's real UI, backup and strings supersede the empty starter's values.
            actual=ROOT/rel
            self.assertTrue(actual.exists(),rel)
            self.assertEqual(hashlib.sha256(actual.read_bytes()).hexdigest(),item['sha256'],rel)

    def test_release_fail_closed_and_no_secret_bundling(self):
        app=(ROOT/'app/build.gradle.kts').read_text()
        self.assertIn('verifyReleaseNativeDependencies',app)
        self.assertIn('TRANSVIBE_ALLOW_DIAGNOSTIC_JNI=OFF',app)
        self.assertIn('ZipFile(sherpaArchives.single())',app)
        self.assertIn('import java.util.zip.ZipFile',app)
        self.assertTrue((ROOT/'app/src/main/cpp/CMakeLists.txt').exists())
        for path in ['release.properties','release.keystore','.git','build','.gradle','.cg']:
            self.assertFalse((ROOT/path).exists(),f'Secret/cache leaked: {path}')

    def test_cogo_icons_and_full_app_both_present(self):
        self.assertTrue((ROOT/'app/src/main/res/mipmap-hdpi/ic_launcher.webp').exists())
        self.assertIn('ComponentActivity',(CODE_ROOT/'MainActivity.kt').read_text())
        self.assertTrue((CODE_ROOT/'presentation/navigation/TransVibeNavGraph.kt').exists())
        self.assertTrue((CODE_ROOT/'models/ModelCatalog.kt').exists())

if __name__=='__main__': unittest.main()
