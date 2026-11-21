"""Exact-brand-asset and design token regression against the owner's legacy app.

UI identity must reuse the owner's real bitmap assets, Cairo typography and
navigation silhouette. Viewmodels, ASR, Room, model manifests and signing
must never be replaced by the old app's incompatible processing logic.
"""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
UI = ROOT / "app/src/main/java/com/transvibe/app/presentation"

def source(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")

class LegacyBrandFidelityTest(unittest.TestCase):
    def test_exact_legacy_launcher_adaptive_layers_present(self):
        for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
            for layer in ("ic_launcher", "ic_launcher_background", "ic_launcher_foreground", "ic_launcher_monochrome"):
                path = RES / f"mipmap-{density}/{layer}.png"
                self.assertTrue(path.is_file(), f"Legacy original PNG absent: {path}")
                self.assertGreater(path.stat().st_size, 200)
        adaptive = source("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        self.assertIn('@mipmap/ic_launcher_background', adaptive)
        self.assertIn('@mipmap/ic_launcher_foreground', adaptive)
        self.assertIn('@mipmap/ic_launcher_monochrome', adaptive)

    def test_original_welcome_brand_image(self):
        path = RES / "drawable-nodpi/transvibe_welcome_logo.png"
        self.assertTrue(path.is_file(), "Use legacy bitmap logo, never an approximate vector replacement")
        self.assertGreater(path.stat().st_size, 100_000)
        welcome = source("app/src/main/java/com/transvibe/app/presentation/screens/WelcomeScreen.kt")
        self.assertIn("R.drawable.transvibe_welcome_logo", welcome)
        self.assertIn("welcome_slogan", welcome)
        self.assertIn("welcome_title", welcome)

    def test_legacy_cairo_theme_and_exact_palette(self):
        for weight in ("regular", "medium", "semibold", "bold", "extrabold"):
            self.assertTrue((RES / f"font/cairo_{weight}.ttf").exists())
        theme = source("app/src/main/java/com/transvibe/app/presentation/theme/TransVibeTheme.kt")
        for code in ("0xFF090D16", "0xFF38BDF8", "0xFFFBBF24", "0xFF0284C7", "0xFFF8FAFC"):
            self.assertIn(code, theme)
        self.assertIn("CairoFontFamily", theme)
        self.assertIn("LocalStatusColors", theme)

    def test_bottom_bar_legacy_silhouette_and_stateful_navigation(self):
        nav = source("app/src/main/java/com/transvibe/app/presentation/navigation/TransVibeNavGraph.kt")
        for name in ("FilledHome", "OutlinedHome", "FilledTune", "OutlinedTune",
                     "FilledHistory", "OutlinedHistory", "FilledSettings", "OutlinedSettings"):
            self.assertIn(name, nav)
        self.assertIn("RoundedCornerShape(26.dp)", nav)
        self.assertIn("AnimatedVisibility", nav)
        self.assertIn("saveState = true", nav)
        self.assertIn("restoreState = true", nav)

    def test_home_reuses_legacy_visual_primitives(self):
        home = source("app/src/main/java/com/transvibe/app/presentation/screens/HomeScreen.kt")
        for name in ("ScreenHeroHeader", "PrimaryActionButton", "StatusPill"):
            self.assertIn(name, home)
        self.assertIn("onNewVideo", home)
        self.assertIn("onOpenJob", home)

    def test_native_and_data_paths_not_replaced_by_legacy(self):
        self.assertTrue((ROOT / "app/libs/arm64-v8a/libtransvibe_whisper.so").is_file())
        native = source("app/src/main/cpp/whisper_jni.cpp")
        self.assertIn("params.detect_language = false;", native)
        self.assertTrue((ROOT / "app/src/main/java/com/transvibe/app/processing/ProcessingOrchestrator.kt").is_file())

if __name__ == "__main__":
    unittest.main()
