"""Cross-repo Whisper JNI/Kotlin contract must fail closed at native import."""
import hashlib
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from tools.verify_native_import import import_verified

ROOT = Path(__file__).resolve().parents[2]


class NativeHandoffContractV2(unittest.TestCase):
    def test_gradle_checks_kotlin_and_jni_hashes(self):
        build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        self.assertIn('jsonHash("jni_source_sha256")', build)
        self.assertIn('jsonHash("kotlin_source_sha256")', build)

    def test_import_workflow_requires_source_read_token_and_exact_app_commit(self):
        workflow = (ROOT / '.github/workflows/import-native.yml').read_text(encoding='utf-8')
        self.assertIn('TRANSVIBE_SO_READ_TOKEN', workflow)
        self.assertIn('.app_source_commit', workflow)
        self.assertIn('git rev-parse HEAD', workflow)

    def test_rejects_mismatched_kotlin_contract_before_loading_elf(self):
        with TemporaryDirectory() as tmp:
            root = Path(tmp)
            incoming = root / 'incoming'
            target = root / 'app'
            native = incoming / 'app/libs/arm64-v8a/libtransvibe_whisper.so'
            cpp = target / 'src/main/cpp/whisper_jni.cpp'
            kotlin = target / 'src/main/java/com/transvibe/app/nativebridge/WhisperNativeBridge.kt'
            for item in (native, cpp, kotlin):
                item.parent.mkdir(parents=True, exist_ok=True)
            native.write_bytes(b'bad-binary')
            cpp.write_text('JNI source', encoding='utf8')
            kotlin.write_text('Kotlin bridge', encoding='utf8')
            manifest = dict(
                schema_version=2, engine='whisper.cpp', first_model='ggml-tiny.bin',
                builder_repository='bingattban/TransVibe-so-',
                app_repository='bingattban/TransVibe-android-',
                abi='arm64-v8a', min_sdk=26,
                app_source_commit='a' * 40, whisper_commit='b' * 40,
                jni_source_sha256=hashlib.sha256(cpp.read_bytes()).hexdigest(),
                kotlin_source_sha256='0' * 64,
                binary_sha256=hashlib.sha256(native.read_bytes()).hexdigest())
            (incoming / 'native-manifest.json').write_text(json.dumps(manifest))
            with self.assertRaisesRegex(AssertionError, 'Kotlin source changed'):
                import_verified(incoming, target)


if __name__ == '__main__':
    unittest.main()
