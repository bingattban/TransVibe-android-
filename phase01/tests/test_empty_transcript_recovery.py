"""Regression for the user-observed 20-Nov phone log: 41s AAC decoded, no transcript saved.
Source checks supplement Kotlin JVM/device testing; never assert actual ASR quality.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

def source(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

class EmptyTranscriptRecoveryContracts(unittest.TestCase):
    def test_orchestrator_checks_transcript_before_translation(self):
        s = source("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertIn("EmptyTranscriptRecovery.classify(", s)
        self.assertLess(s.index("EmptyTranscriptRecovery.classify("),
                        s.index("// E05: Enforce translation success"))
        self.assertIn("JobState.FAILED", s)
        self.assertIn("decodedFrames.get()", s)
        self.assertIn("asrCalls.get()", s)

    def test_resume_never_skips_audio_when_only_checkpoint_exists_without_text(self):
        s = source("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertIn("validResume && transcriptDao.getSegmentsForJob(jobId).any", s)

    def test_fallback_is_bounded_and_never_uses_all_silence(self):
        s = source("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        p = source("app/src/main/java/com/transvibe/app/processing/EmptyTranscriptRecovery.kt")
        self.assertIn("if (regions.isEmpty() && EmptyTranscriptRecovery.isAudibleSpeechCandidate(pcm))", s)
        self.assertIn("Timebase.samplesToMicros(pcm.size.toLong())", s)
        self.assertIn("fun isAudibleSpeechCandidate(", p)
        self.assertIn("isFinite()", p)

    def test_no_private_transcript_text_in_device_logs(self):
        s = source("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertIn("TransVibeASR", s)
        self.assertNotIn('Log.i("TransVibeASR", ts.text', s)

    def test_failed_state_does_not_advertise_cached_transcript_when_absent(self):
        s = source("app/src/main/java/com/transvibe/app/presentation/screens/ProcessingScreen.kt")
        for code in ("AUDIO_NO_PCM", "ASR_NO_TRANSCRIPT", "VAD_NO_SPEECH"):
            self.assertIn(code, s)

if __name__ == "__main__":
    unittest.main()
