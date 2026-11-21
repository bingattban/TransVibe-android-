"""Regression: saved jobs must open their own media, including async Room loads.

Static checks guard the Compose wiring; runtime behavior must still be verified
by Android JVM instrumentation/on-device preview testing.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCREENS = ROOT / 'app/src/main/java/com/transvibe/app/presentation/screens'
NAV = ROOT / 'app/src/main/java/com/transvibe/app/presentation/navigation/TransVibeNavGraph.kt'

class PlayerPreviewJobSourceTest(unittest.TestCase):
    def test_route_passes_job_id_into_preview(self):
        nav = NAV.read_text(encoding='utf-8')
        self.assertIn('Routes.PLAYER,', nav)
        self.assertIn('val previewJobId = backStack.arguments?.getString("jobId") ?: ""', nav)
        self.assertIn('jobId = previewJobId,', nav)

    def test_preview_recovers_persisted_job_and_reacts_to_uri_changes(self):
        preview = (SCREENS/'PlayerPreviewScreen.kt').read_text(encoding='utf-8')
        for text in (
            'jobId: String,',
            'viewModel.activeJobEntity.collectAsState()',
            'LaunchedEffect(jobId)',
            'viewModel.loadJobCues(jobId)',
            'previewMediaUriForJob(jobId, jobMeta?.jobId, jobMeta?.mediaUriString)',
            'LaunchedEffect(player, mediaUriString)',
            'player.clearMediaItems()',
            'player.setMediaItem(MediaItem.fromUri(Uri.parse(mediaUriString)))',
        ):
            self.assertIn(text, preview)
        self.assertNotIn('viewModel.inspectedMedia.collectAsState()', preview)

    def test_pure_job_ownership_helper_is_used(self):
        helper = (SCREENS/'PreviewMediaUri.kt').read_text(encoding='utf-8')
        self.assertIn('internal fun previewMediaUriForJob(', helper)
        self.assertIn('requestedJobId == loadedJobId', helper)
        self.assertIn('takeIf { it.isNotBlank() }', helper)

if __name__ == '__main__':
    unittest.main()
