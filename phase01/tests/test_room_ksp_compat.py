#!/usr/bin/env python3
"""Offline, read-only contract checks for Phase 01 KSP/Room configuration.

This is not a substitute for building with Code On The Go or running Room migrations.
"""
from pathlib import Path
import re
import tomllib
import unittest

ROOT = Path(__file__).resolve().parents[2]


class RoomKspCompatibilityContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = tomllib.loads((ROOT / 'gradle/libs.versions.toml').read_text(encoding='utf-8'))
        cls.build = (ROOT / 'app/build.gradle.kts').read_text(encoding='utf-8')
        cls.database = (ROOT / 'app/src/main/java/com/transvibe/app/persistence/TransVibeDatabase.kt').read_text(encoding='utf-8')
        cls.daos = (ROOT / 'app/src/main/java/com/transvibe/app/persistence/Daos.kt').read_text(encoding='utf-8')

    def test_room_compiler_has_ksp2_void_signature_fix(self):
        # Room 2.6.1 caused the exact phone failure: unexpected jvm signature V.
        # Room 2.7+ includes the upstream fix; use a pinned, published stable.
        self.assertEqual('2.8.5', self.catalog['versions']['room'])

    def test_all_room_artifacts_pin_the_same_version(self):
        for name in ('androidx-room-runtime', 'androidx-room-ktx',
                     'androidx-room-compiler', 'androidx-room-testing'):
            with self.subTest(name=name):
                lib = self.catalog['libraries'][name]
                self.assertEqual('androidx.room', lib['group'])
                self.assertEqual('room', lib['version']['ref'])

    def test_room_ksp_wiring_remains_enabled(self):
        self.assertIn('alias(libs.plugins.ksp)', self.build)
        self.assertIn('ksp(libs.androidx.room.compiler)', self.build)
        self.assertIn('implementation(libs.androidx.room.runtime)', self.build)
        self.assertIn('implementation(libs.androidx.room.ktx)', self.build)
        self.assertIn('testImplementation(libs.androidx.room.testing)', self.build)
        self.assertEqual('2.3.14', self.catalog['versions']['ksp'])

    def test_database_identity_and_migrations_are_unchanged(self):
        self.assertRegex(self.database, r'\bversion\s*=\s*3\b')
        self.assertIn('"transvibe.db"', self.database)
        for fragment in ('MIGRATION_1_2', 'MIGRATION_2_3',
                         '.addMigrations(MIGRATION_1_2, MIGRATION_2_3)'):
            self.assertIn(fragment, self.database)
        self.assertNotIn('fallbackToDestructiveMigration', self.database)

    def test_original_unit_returning_suspend_daos_unchanged(self):
        # Keep the reproducing DAO signatures so this tests the processor fix,
        # not a workaround that alters the application's API/data behavior.
        for method in ('upsertJob', 'updateProgress', 'deleteJob',
                       'upsertSegments', 'upsertTranslations', 'upsertCueEdit',
                       'insertRun', 'upsertFreezeRecord'):
            self.assertRegex(self.daos, rf'\bsuspend\s+fun\s+{method}\s*\(')

    def test_code_on_the_go_toolchain_and_app_id_preserved(self):
        self.assertEqual('9.3.1', self.catalog['versions']['agp'])
        self.assertEqual('2.3.21', self.catalog['versions']['kotlin'])
        self.assertIn('applicationId = "com.transvibe.app"', self.build)
        self.assertIn('applicationIdSuffix = ".preview"', self.build)
        self.assertIn('minSdk = 26', self.build)
        self.assertIn('targetSdk = 35', self.build)
        self.assertIn('verifyReleaseNativeDependencies', self.build)


if __name__ == '__main__':
    unittest.main(verbosity=2)
