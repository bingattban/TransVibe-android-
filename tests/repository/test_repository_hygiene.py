"""Regression gates for keeping the actual Android project free of retired assets and logs.

The Android app, verified JNI, Gradle infrastructure and tests remain part
of the project even though not all of them ship inside the APK.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]

REQUIRED = (
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/transvibe/app/MainActivity.kt",
    "app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt",
    "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml",
    "app/src/main/res/mipmap-hdpi/ic_launcher.png",
    "app/libs/arm64-v8a/libtransvibe_whisper.so",
    "app/libs/native-provenance.json",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradlew",
    ".github/workflows/android-ci.yml",
    "tests/repository/test_build_contracts.py",
)

RETIRED = (
    "app/src/main/res/layout/activity_main.xml",
    "app/src/main/res/drawable/transvibe_launcher_icon.xml",
    "app/src/main/res/drawable-v24/ic_launcher_foreground.xml",
    "app/src/main/res/drawable/ic_launcher_background.xml",
    "app/src/main/res/drawable/ic_launcher_round.webp",
    "app/libs/arm64-v8a/.keep",
    "docs/GITHUB_TWO_REPO_SETUP_AR.md",
    "docs/REFERENCE_UI_REFRESH_AR.md",
)


class RepositoryHygieneTest(unittest.TestCase):
    def test_runtime_build_and_test_dependencies_are_preserved(self):
        for rel in REQUIRED:
            with self.subTest(path=rel):
                self.assertTrue((ROOT / rel).is_file(), f"Required project file is missing: {rel}")

    def test_retired_starter_assets_are_absent(self):
        for rel in RETIRED:
            with self.subTest(path=rel):
                self.assertFalse((ROOT / rel).exists(), f"Unused starter file reintroduced: {rel}")

    def test_historical_logs_and_reports_not_checked_in(self):
        for rel in ("phase00", "phase01", "scripts/cotg_device_logs"):
            with self.subTest(path=rel):
                self.assertFalse((ROOT / rel).exists(), f"Archived directory reintroduced: {rel}")
        for rel in ("PHASE_REPORT.md", "GITHUB_SOURCE_IMPORT_AR.md",
                    "scripts/phase00_forensic_contracts.py"):
            with self.subTest(path=rel):
                self.assertFalse((ROOT / rel).exists(), f"Retired project history reintroduced: {rel}")
        for path in (ROOT / "tests/repository").rglob("*"):
            if path.is_file():
                self.assertNotEqual(path.suffix, ".log", f"Archived build log: {path}")

    def test_compose_entrypoint_replaces_obsolete_xml_activity(self):
        activity = (ROOT / "app/src/main/java/com/transvibe/app/MainActivity.kt").read_text(encoding="utf-8")
        self.assertIn("setContent {", activity)
        self.assertNotIn("R.layout.activity_main", activity)


if __name__ == "__main__":
    unittest.main()
