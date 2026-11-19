"""Phase01 CODE ON THE GO validation collector: test-first source gate.

These checks verify the validator is present and fail-closed; they cannot
replace real Android Gradle execution or physical device smoke testing.
"""
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
VALIDATOR = ROOT / 'phase01' / 'validate_on_code_on_the_go.sh'


class DeviceValidationCollectorTests(unittest.TestCase):
    def test_validation_script_exists_and_is_valid_posix_shell(self):
        self.assertTrue(VALIDATOR.is_file(), 'Missing required phone-side build-evidence collector')
        with subprocess.Popen(['sh', '-n', str(VALIDATOR)], stdout=subprocess.PIPE, stderr=subprocess.PIPE) as p:
            out, err = p.communicate(timeout=5)
        self.assertEqual(p.returncode, 0, (out + err).decode())

    def test_validation_runs_test_and_both_apk_builds_and_fails_closed(self):
        src = VALIDATOR.read_text(encoding='utf-8')
        self.assertIn(':app:clean', src)
        self.assertIn(':app:testDebugUnitTest', src)
        self.assertIn(':app:assembleDebug', src)
        self.assertIn(':app:assemblePreview', src)
        self.assertIn("grep -F 'Gradle 9.6.1'", src)
        self.assertIn('app-debug.apk', src)
        self.assertIn('app-preview.apk', src)
        self.assertIn('INCOMPLETE', src)
        self.assertIn('PASS', src)
        self.assertNotIn('adb uninstall', src)
        self.assertNotIn('rm -rf', src)

    def test_cotg_handoff_requires_separate_real_install_launch(self):
        notes = (ROOT / 'phase01' / 'completion' / 'COTG_VERIFICATION_GUIDE_AR.md').read_text(encoding='utf-8')
        self.assertIn('com.transvibe.app.preview', notes)
        self.assertIn('لا تحذف', notes)
        self.assertIn('تثبيت', notes)
        self.assertIn('التشغيل', notes)


if __name__ == '__main__':
    unittest.main()
