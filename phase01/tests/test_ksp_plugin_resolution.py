"""KSP 2.3.14 must resolve from published implementation module on Maven Central."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
class KspMavenCentralResolution(unittest.TestCase):
    def test_direct_ksp_module(self):
        text = (ROOT / 'settings.gradle.kts').read_text(encoding='utf-8')
        self.assertIn('requested.id.id == "com.google.devtools.ksp"', text)
        self.assertIn('useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")', text)
    def test_android_plugin_repositories_remain_in_original_order(self):
        text = (ROOT / 'settings.gradle.kts').read_text(encoding='utf-8')
        plugins = text.split('pluginManagement {', 1)[1].split('dependencyResolutionManagement {', 1)[0]
        self.assertLess(plugins.index('gradlePluginPortal()'), plugins.index('google()'))
        self.assertLess(plugins.index('google()'), plugins.index('mavenCentral()'))

    def test_preserve_phone_toolchain_pins(self):
        text = (ROOT / 'gradle/libs.versions.toml').read_text(encoding='utf-8')
        for name, value in [("ksp","2.3.14"),("kotlin","2.3.21"),("agp","9.3.1")]:
            self.assertIn(f'{name} = "{value}"', text)

if __name__ == '__main__': unittest.main()
