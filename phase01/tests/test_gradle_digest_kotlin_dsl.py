"""Prevent Gradle Kotlin DSL `java` extension shadowing JDK MessageDigest."""
from pathlib import Path
import unittest

APP = Path(__file__).resolve().parents[2] / 'app/build.gradle.kts'

class GradleDigestRegression(unittest.TestCase):
    def test_message_digest_uses_explicit_import(self):
        script = APP.read_text(encoding='utf-8')
        self.assertIn('import java.security.MessageDigest', script)
        self.assertIn('MessageDigest.getInstance("SHA-256")', script)
        self.assertNotIn('val digest = java.security.MessageDigest', script)

    def test_signed_bytes_are_formatted_explicitly(self):
        script = APP.read_text(encoding='utf-8')
        self.assertIn('joinToString("") { byte: Byte -> "%02x".format(byte) }',script)

if __name__ == '__main__': unittest.main()
