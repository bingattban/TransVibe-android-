"""Reference visual migration contract; tests-first gate for UI-only changes."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[2]
def read(rel): return (ROOT/rel).read_text(encoding="utf-8")

class UserReferenceUI(unittest.TestCase):
    def test_dark_and_light_theme(self):
        s=read("app/src/main/java/com/transvibe/app/presentation/theme/TransVibeTheme.kt")
        for token in ("lightColorScheme(", "darkColorScheme(", "themeMode:", "Brush.linearGradient", "RoundedCornerShape(22.dp)"):
            self.assertIn(token,s)
    def test_branding_and_launcher(self):
        self.assertTrue((ROOT/"app/src/main/res/drawable/transvibe_welcome_logo.xml").is_file())
        self.assertTrue((ROOT/"app/src/main/res/drawable/transvibe_launcher_icon.xml").is_file())
        self.assertIn("@drawable/transvibe_launcher_icon", read("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"))
        self.assertIn("TransVibe", read("app/src/main/java/com/transvibe/app/presentation/screens/WelcomeScreen.kt"))
    def test_bottombar_retains_processing_routes(self):
        s=read("app/src/main/java/com/transvibe/app/presentation/navigation/TransVibeNavGraph.kt")
        for token in ("TransVibeBottomNavigation","currentBackStackEntryAsState","restoreState = true","saveState = true", "Routes.PROCESSING","Routes.RESULTS"):
            self.assertIn(token,s)
    def test_saved_theme_additive(self):
        s=read("app/src/main/java/com/transvibe/app/persistence/UserPreferencesStore.kt")
        self.assertIn('val themeMode: String = "system"',s)
        self.assertIn("setThemeMode(",s)
        self.assertIn('preferencesDataStore(name = "transvibe_prefs")',s)
    def test_processing_does_not_confuse_audio_and_translation(self):
        s=read("app/src/main/java/com/transvibe/app/presentation/screens/ProcessingScreen.kt")
        for token in ("JobState.TRANSLATING","LinearProgressIndicator","viewModel.cancelProcessing(app)","onOpenResults(jobId!!)"):
            self.assertIn(token,s)
    def test_welcome_keeps_language_and_onboarding(self):
        s=read("app/src/main/java/com/transvibe/app/presentation/screens/WelcomeScreen.kt")
        for token in ("R.drawable.transvibe_welcome_logo",'onSelectLanguage("ar")','onSelectLanguage("en")',"onContinue"):
            self.assertIn(token,s)
if __name__=="__main__": unittest.main()
