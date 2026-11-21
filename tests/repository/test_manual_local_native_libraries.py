"""Manual app/libs import contract; must fail on the preceding delivered baseline."""
import unittest
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[2]
APP = PROJECT / 'app'
BUILD = (APP / 'build.gradle.kts').read_text(encoding='utf8')
BRIDGE = (APP / 'src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt').read_text(encoding='utf8')
HOME = (APP / 'src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt').read_text(encoding='utf8')

class ManualNativeLibraryImports(unittest.TestCase):
    def test_local_aar_jar_import_kept(self):
        self.assertIn('"*.aar", "*.jar"',BUILD)
    def test_abi_folder_auto_packaged(self):
        self.assertIn('jniLibs.srcDir("libs")', BUILD)
    def test_prebuilt_whisper_disables_cmake_to_avoid_duplicate(self):
        self.assertIn('hasPrebuiltWhisperJni', BUILD)
        self.assertIn('!hasPrebuiltWhisperJni', BUILD)
    def test_prebuild_asset_validation(self):
        self.assertIn('verifyManualLocalLibraries', BUILD)
        self.assertIn('preDebugBuild', BUILD)
        self.assertIn('prePreviewBuild', BUILD)
        self.assertIn('elf[18]', BUILD)
        self.assertIn('ZipFile(archive)', BUILD)
    def test_release_is_still_restricted(self):
        self.assertIn('verifyReleaseNativeDependencies',BUILD)
        self.assertIn('RELEASE BLOCKED',BUILD)
    def test_runtime_bridge_catches_invalid_prebuilt(self):
        self.assertIn('catch (_: UnsatisfiedLinkError)', BRIDGE)
        self.assertIn('fun isAvailable(): Boolean',BRIDGE)
    def test_diagnostic_warning_depends_on_runtime_link(self):
        self.assertIn('!WhisperNativeBridge.isAvailable()', HOME)
    def test_manual_installation_instructions(self):
        doc=(APP/'libs'/'README_AR.md').read_text(encoding='utf8')
        self.assertIn('sherpa-onnx-',doc)
        self.assertIn('libtransvibe_whisper.so',doc)
        self.assertIn('arm64-v8a',doc)
        self.assertIn('Code On The Go',doc)
    def test_native_binaries_are_never_unverified_or_fabricated(self):
        import hashlib
        import json
        native=list((APP/'libs'/'arm64-v8a').glob('*.so'))
        archives=list((APP/'libs').glob('*.aar'))
        if native:
            self.assertEqual(['libtransvibe_whisper.so'], sorted(x.name for x in native))
            provenance=APP/'libs'/'native-provenance.json'
            self.assertTrue(provenance.is_file(), 'Native .so must come from verified SO GitHub handoff')
            metadata=json.loads(provenance.read_text(encoding='utf8'))
            self.assertEqual('bingattban/TransVibe-so-', metadata.get('builder_repository'))
            self.assertEqual('ggml-tiny.bin', metadata.get('first_model'))
            self.assertEqual(hashlib.sha256(native[0].read_bytes()).hexdigest(), metadata.get('binary_sha256'))
        self.assertFalse(archives, 'Sherpa AAR must not be fabricated during Whisper Tiny stage')

if __name__=='__main__': unittest.main()
