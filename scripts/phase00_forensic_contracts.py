#!/usr/bin/env python3
"""Phase 00 ONLY: deterministic READ-ONLY source/ZIP verification.

Usage:
    python3 scripts/phase00_forensic_contracts.py --suite baseline --archive /path/to/TransVibe_Android_Project.zip
    python3 scripts/phase00_forensic_contracts.py --suite defects

The defects suite intentionally FAILS on known existing defects. It never modifies source,
fetches models, makes network calls, or invokes an Android compiler.
"""
import argparse
import hashlib
import pathlib
import re
import stat
import sys
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
ARCHIVE = None
SOURCE = pathlib.Path('app/src/main/java/com/transvibe/app')


def read(relative):
    return (ROOT / relative).read_text(encoding='utf-8')


class ArchiveAndSourceBaseline(unittest.TestCase):
    def test_zip_members_integrity_and_path_safety(self):
        if ARCHIVE is None:
            self.skipTest('Provide --archive to check the original ZIP')
        with zipfile.ZipFile(ARCHIVE) as z:
            self.assertIsNone(z.testzip(), 'Corrupt member (CRC failure)')
            files = [i for i in z.infolist() if not i.is_dir()]
            self.assertEqual(len(files), 98, 'Expected exact ORIGINAL supplied archive, 98 files')
            self.assertEqual(len(set(x.filename for x in files)), 98, 'Duplicate archive paths')
            for info in files:
                p = pathlib.PurePosixPath(info.filename)
                self.assertFalse(p.is_absolute() or '..' in p.parts, f'Unsafe path: {p}')
                self.assertEqual(p.parts[0], 'TransVibe')
                self.assertFalse(stat.S_ISLNK(info.external_attr >> 16), f'Symlink: {p}')
                on_disk = ROOT.joinpath(*p.parts[1:])
                self.assertTrue(on_disk.is_file(), f'Missing original: {p}')
                self.assertEqual(hashlib.sha256(z.read(info)).digest(),
                                 hashlib.sha256(on_disk.read_bytes()).digest(),
                                 f'Original file modified: {p}')

    def test_identity_and_variants(self):
        gradle = read('app/build.gradle.kts')
        settings = read('settings.gradle.kts')
        self.assertIn('rootProject.name = "TransVibe"', settings)
        self.assertIn('namespace = "com.transvibe.app"', gradle)
        self.assertIn('applicationId = "com.transvibe.app"', gradle)
        self.assertIn('applicationIdSuffix = ".preview"', gradle)
        self.assertIn('versionCode = 6', gradle)
        self.assertIn('minSdk = 26', gradle)
        self.assertIn('targetSdk = 35', gradle)

    def test_exact_model_catalog_and_seven_profiles(self):
        c = read(SOURCE / 'models/ModelCatalog.kt')
        p = read(SOURCE / 'asr/ModelProfiles.kt')
        physical = re.findall(r'physicalFileName\s*=\s*"([^"]+)"', c)
        self.assertEqual(len(physical), 7)
        self.assertCountEqual(physical, [
            'ggml-base.bin', 'ggml-large-v3-turbo.bin', 'ggml-medium.bin',
            'ggml-small.bin', 'ggml-tiny.bin', 'moonshine-base-ar.zip',
            'sensevoice-small-int8.zip'
        ])
        self.assertEqual(p.count('= ModelProfile('), 7)

    def test_current_room_database_is_version_3(self):
        db = read(SOURCE / 'persistence/TransVibeDatabase.kt')
        self.assertRegex(db, r'version\s*=\s*3\b')
        self.assertIn('"transvibe.db"', db)
        self.assertIn('MIGRATION_1_2', db)
        self.assertIn('MIGRATION_2_3', db)
        self.assertNotIn('fallbackToDestructiveMigration(', db)

    def test_shipped_native_and_asr_weights_are_absent(self):
        self.assertFalse((ROOT / 'app/src/main/cpp/third_party/whisper.cpp/CMakeLists.txt').exists())
        self.assertFalse(list((ROOT / 'app').rglob('*.aar')))
        self.assertFalse(list((ROOT / 'app').rglob('*.so')))
        self.assertFalse((ROOT / 'app/src/main/assets/vad/silero_vad.onnx').exists())
        for model_file in ['ggml-tiny.bin','ggml-base.bin','ggml-small.bin',
                           'ggml-medium.bin','ggml-large-v3-turbo.bin',
                           'moonshine-base-ar.zip','sensevoice-small-int8.zip']:
            self.assertFalse(list((ROOT / 'app').rglob(model_file)))

    def test_documented_defect_ids_exist_in_original_report(self):
        lines = read('PHASE_REPORT.md').splitlines()
        observed = set()
        for line in lines:
            if not line.startswith('- **E'):
                continue
            for match in re.finditer(r'E(\d{2})(?:[–-]E(\d{2}))?', line):
                start = int(match.group(1))
                end = int(match.group(2) or start)
                observed.update(range(start, end + 1))
        self.assertEqual(observed, set(range(1, 19)),
                         'The old source report must identify all E01..E18 before comparison')



class KnownDefectsRegression(unittest.TestCase):
    """The failures below reproduce currently unresolved defects; NOT acceptance passes."""
    def test_build_001_exporter_constructor_resolves(self):
        app = read(SOURCE / 'TransVibeApplication.kt')
        exporter = read(SOURCE / 'subtitles/SrtVttExporter.kt')
        self.assertIn('class SrtVttExporter(', exporter)
        self.assertIn('SrtVttExporter(app)', app,
                      'BUILD-001: undefined SrtVibeExporter(app) remains in AppContainer')

    def test_mig_001_existing_legacy_database_name_has_migration_entrypoint(self):
        db = read(SOURCE / 'persistence/TransVibeDatabase.kt')
        self.assertIn('processing_jobs.db', db,
                      'MIG-001: real legacy Room DB filename not handled by new Room builder')

    def test_mig_002_existing_legacy_datastore_name_has_migration_entrypoint(self):
        prefs = read(SOURCE / 'persistence/UserPreferencesStore.kt')
        self.assertIn('transvibe_preferences', prefs,
                      'MIG-002: legacy preferences DataStore name is absent')

    def test_trans_001_arabic_requires_explicit_pivot(self):
        src = read(SOURCE / 'translation/TranslationRoutingPolicy.kt')
        self.assertNotIn('PRESERVE_ARABIC_TRANSCRIPT', src,
                         'TRANS-001: AR->AR bypass conflicts with owner literal routing policy')

    def test_build_002_real_native_whisper_required_for_release(self):
        cmake = read('app/src/main/cpp/CMakeLists.txt')
        self.assertNotIn('TRANSVIBE_HAS_WHISPER_CPP=0', cmake,
                         'BUILD-002: build can emit diagnostic bridge without real engine')

    def test_native_001_sherpa_model_smoke_does_real_init(self):
        source = read(SOURCE / 'models/ModelLifecycleManager.kt')
        i = source.index('EngineType.SHERPA_MOONSHINE_AR, EngineType.SHERPA_SENSEVOICE -> {',
                         source.index('private fun runNativeSmokeTest'))
        block = source[i:source.index('\n        }\n    }', i)]
        self.assertNotIn('Result.success(Unit)', block,
                         'NATIVE-001: Sherpa library-presence check is not model init/transcription')

    def test_build_003_gradle_wrapper_launchers_are_present(self):
        self.assertTrue((ROOT / 'gradlew').exists() and
                        (ROOT / 'gradle/wrapper/gradle-wrapper.jar').exists(),
                        'BUILD-003: wrapper launcher and JAR are absent')

    def test_resource_001_actual_silero_asset_is_packaged(self):
        self.assertTrue((ROOT / 'app/src/main/assets/vad/silero_vad.onnx').exists(),
                        'RESOURCE-001: Silero is absent; RMS fallback != Silero')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--suite', choices=['baseline','defects','all'],default='all')
    parser.add_argument('--archive',type=pathlib.Path)
    args = parser.parse_args()
    ARCHIVE=args.archive
    loader=unittest.defaultTestLoader
    suites=[]
    if args.suite in ('baseline','all'): suites.append(loader.loadTestsFromTestCase(ArchiveAndSourceBaseline))
    if args.suite in ('defects','all'): suites.append(loader.loadTestsFromTestCase(KnownDefectsRegression))
    result=unittest.TextTestRunner(verbosity=2).run(unittest.TestSuite(suites))
    sys.exit(0 if result.wasSuccessful() else 1)
