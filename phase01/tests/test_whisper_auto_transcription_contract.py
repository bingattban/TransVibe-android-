"""Regression for Whisper Tiny: 'auto' language MUST continue to transcription.

The pinned whisper.cpp commit d1be6fde11ac6e0407606b4e42fe72d34add8037
returns success with zero transcript segments when params.detect_language is true.
The actual language auto-detection is already activated by params.language="auto".
Do not infer acceptance from source tests alone; device speech must still pass.
"""
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class WhisperAutoTranscriptionContract(unittest.TestCase):
    def test_auto_language_does_not_activate_detection_only_mode(self):
        cpp = (ROOT / "app/src/main/cpp/whisper_jni.cpp").read_text(encoding="utf-8")
        branch = re.search(
            r'if\s*\(lang\s*==\s*"auto"\s*\|\|\s*lang\.empty\(\)\)\s*\{([^}]+)\}',
            cpp
        )
        self.assertIsNotNone(branch, "Must handle language=auto explicitly")
        code = branch.group(1)
        self.assertIn('params.language = "auto";', code)
        self.assertIn("params.detect_language = false;", code)
        self.assertNotIn("params.detect_language = true;", cpp)

    def test_transcription_must_reach_whisper_full_and_return_segments(self):
        cpp = (ROOT / "app/src/main/cpp/whisper_jni.cpp").read_text(encoding="utf-8")
        self.assertIn("whisper_full(holder->ctx, params, samples, numSamples)", cpp)
        self.assertIn("whisper_full_n_segments(holder->ctx)", cpp)
        self.assertIn("JNIEXPORT jobjectArray JNICALL", cpp)
        self.assertIn("whisper_full completed", cpp)

    def test_native_provenance_must_match_source_or_build_fails_closed(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        for token in ("native-provenance.json", 'jsonHash("jni_source_sha256")',
                      'jsonHash("binary_sha256")', "check(hasPrebuiltWhisperJni)"):
            self.assertIn(token, gradle)

if __name__ == "__main__":
    unittest.main()
