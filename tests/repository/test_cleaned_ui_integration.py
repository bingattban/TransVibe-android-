"""Integration contracts for the user-supplied cleaned TransVibe UI (2026-11-21).

These are structural regressions, NOT proof that Android compiled or launched.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
UI = ROOT / "app/src/main/java/com/transvibe/app/presentation"


def source(path):
    return (UI / path).read_text(encoding="utf-8")


class CleanedUiIntegrationTest(unittest.TestCase):
    def test_about_screen_is_reachable_and_bilingual(self):
        nav = source("navigation/TransVibeNavGraph.kt")
        screen = source("screens/AboutScreen.kt")
        settings = source("screens/SettingsScreen.kt")
        self.assertIn('const val ABOUT = "about"', nav)
        self.assertIn("composable(Routes.ABOUT)", nav)
        self.assertIn("AboutScreen(", nav)
        self.assertIn("onAbout = { navController.navigate(Routes.ABOUT)", nav)
        self.assertIn("onClick = onAbout", settings)
        self.assertIn("isArabic: Boolean", screen)
        self.assertIn("BuildConfig.VERSION_NAME", screen)

    def test_dropdown_selector_is_real_and_used(self):
        selector = source("components/DropdownChoice.kt")
        setup = source("screens/ProcessingSetupScreen.kt")
        self.assertIn("fun DropdownChoice(", selector)
        self.assertIn("DropdownMenu(", selector)
        self.assertIn("DropdownMenuItem(", selector)
        self.assertIn("if (value != selected) onSelected(value)", selector)
        self.assertIn("DropdownChoice(", setup)

    def test_processing_result_and_preview_routes_preserved(self):
        nav = source("navigation/TransVibeNavGraph.kt")
        for marker in ("composable(Routes.PROCESSING)", "Routes.RESULTS",
                       "Routes.PLAYER", "Routes.SETUP", "Routes.TRANSLATION_PACKS",
                       "currentBackStackEntryAsState", "saveState = true",
                       "restoreState = true"):
            with self.subTest(marker=marker):
                self.assertIn(marker, nav)
        for path in ("processing/ProcessingOrchestrator.kt",
                     "nativebridge/WhisperNativeBridge.kt",
                     "persistence/TransVibeDatabase.kt"):
            self.assertTrue((ROOT / "app/src/main/java/com/transvibe/app" / path).is_file())

    def test_release_configuration_and_native_lib_preserved(self):
        gradle = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
        self.assertIn('applicationId = "com.transvibe.app"', gradle)
        self.assertIn("verifyReleaseNativeDependencies", gradle)
        self.assertTrue((ROOT / "app/libs/arm64-v8a/libtransvibe_whisper.so").is_file())
        self.assertTrue((ROOT / ".github/workflows/android-ci.yml").is_file())


if __name__ == "__main__":
    unittest.main()
