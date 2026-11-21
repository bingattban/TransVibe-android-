"""Regression contract for the user-observed 41/41s stuck TRANSLATING job.
Source checks supplement, never replace, Android JVM/device tests.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class TranslationStallRegression(unittest.TestCase):
    def test_translation_wait_bounded_and_offline_packs_required(self):
        s = read("app/src/main/java/com/transvibe/app/translation/TranslationPipeline.kt")
        for part in ("withTimeoutOrNull", "isPackInstalled", "TRANSLATION_PACK_MISSING",
                     "TRANSLATION_TIMEOUT", "engine.translate"):
            self.assertIn(part, s)

    def test_mlkit_must_not_swallow_cancellation(self):
        s = read("app/src/main/java/com/transvibe/app/translation/MlKitTranslationEngine.kt")
        self.assertIn("catch (ce: CancellationException)", s)
        self.assertIn("throw ce", s)

    def test_failed_translation_visible_and_cached_original_recoverable(self):
        orchestrator = read("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        screen = read("app/src/main/java/com/transvibe/app/presentation/screens/ProcessingScreen.kt")
        self.assertIn("JobState.FAILED", orchestrator)
        self.assertIn("warningMessage", orchestrator)
        self.assertIn("JobState.FAILED", screen)
        self.assertIn("onOpenResults", screen)
        # Current Compose UI renders the current stage through the pipeline
        # timeline and preserves error visibility and recoverability.
        self.assertIn("PipelineTimelineCard(", screen)
        self.assertIn("ProcessingErrorBanner(", screen)
        self.assertIn("SystemClock.elapsedRealtime()", screen)

    def test_no_false_completed_before_translation(self):
        s = read("app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt")
        self.assertLess(s.index("val trRes = retranslateCachedTranscript"),
                        s.index("jobDao.updateProgress(jobId, JobState.COMPLETED.name"))

    def test_release_and_preview_ksp_tasks_are_not_interleaved(self):
        workflow = read(".github/workflows/android-ci.yml")
        self.assertNotIn("./gradlew --no-daemon :app:assemblePreview :app:assembleRelease", workflow)
        self.assertIn(":app:assemblePreview", workflow)
        self.assertIn(":app:assembleRelease", workflow)
        self.assertIn("--no-build-cache", workflow)

    def test_failed_job_logged_without_transcript(self):
        s = read("app/src/main/java/com/transvibe/app/platform/TransVibeProcessingService.kt")
        self.assertIn('Log.e(', s)
        self.assertIn('.onFailure', s)

if __name__ == "__main__":
    unittest.main()
