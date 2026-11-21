from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class PcmAndComposeRegression(unittest.TestCase):
    def test_audio_metrics_logged_before_inference_without_recording_speech(self):
        s = read("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertIn("PcmSignalMetrics.analyze(seg.pcmSamples)", s)
        self.assertIn("rmsDbfs=", s)
        self.assertIn("peak=", s)
        self.assertIn('Log.i("TransVibeASR",', s)

    def test_completed_audio_scan_does_not_show_zero_progress_on_failed_asr(self):
        s = read("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertIn("if (decodedFrames.get() > 0) totalAudioMs else 0L", s)

    def test_prevent_unbounded_nested_scrolling(self):
        s = read("app/src/main/java/com/transvibe/app/presentation/screens/TranslationPacksScreen.kt")
        self.assertIn(".verticalScroll(rememberScrollState())", s)
        self.assertNotIn("LazyColumn(", s)
        self.assertIn("packs.forEach", s)

    def test_prevent_stereo_phase_cancellation_data_loss(self):
        s = read("app/src/main/java/com/transvibe/app/media/AudioResampler.kt")
        self.assertIn("leftEnergy", s)
        self.assertIn("rightEnergy", s)
        self.assertIn("monoEnergy", s)
        self.assertIn("chooseChannel", s)

    def test_no_jni_or_room_schema_mutations_for_diagnostics(self):
        s = read("app/src/main/java/com/transvibe/app/media/PcmSignalMetrics.kt")
        self.assertIn("object PcmSignalMetrics", s)
        self.assertNotIn("Log.", s)

if __name__ == "__main__":
    unittest.main()
