"""Regression tests for Code On The Go's duplicate Android launcher resources."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
from check_android_resource_collisions import find_collisions


class AndroidResourceCollisionTest(unittest.TestCase):
    def test_png_and_webp_same_density_are_a_collision(self):
        with tempfile.TemporaryDirectory() as tmp:
            res = Path(tmp) / "res"
            folder = res / "mipmap-hdpi"
            folder.mkdir(parents=True)
            (folder / "ic_launcher.png").write_bytes(b"PNG")
            (folder / "ic_launcher.webp").write_bytes(b"WEBP")
            found = find_collisions(res)
            self.assertEqual(len(found), 1)
            self.assertEqual(found[0][0], "mipmap-hdpi/ic_launcher")
            self.assertEqual({f.suffix for f in found[0][1]}, {".png", ".webp"})
            self.assertTrue((folder / "ic_launcher.png").exists())
            self.assertTrue((folder / "ic_launcher.webp").exists())

    def test_distinct_density_qualifiers_are_allowed(self):
        with tempfile.TemporaryDirectory() as tmp:
            res = Path(tmp) / "res"
            for density, ext in (("mdpi", "png"), ("hdpi", "webp")):
                folder = res / f"mipmap-{density}"
                folder.mkdir(parents=True)
                (folder / f"ic_launcher.{ext}").write_bytes(b"test")
            self.assertEqual(find_collisions(res), [])

    def test_nine_patch_and_plain_png_collision_is_detected(self):
        with tempfile.TemporaryDirectory() as tmp:
            res = Path(tmp) / "res"
            folder = res / "drawable"
            folder.mkdir(parents=True)
            (folder / "badge.png").write_bytes(b"one")
            (folder / "badge.9.png").write_bytes(b"two")
            found = find_collisions(res)
            self.assertEqual(found[0][0], "drawable/badge")

    def test_real_project_has_no_duplicate_resource_names(self):
        self.assertEqual(find_collisions(ROOT / "app/src/main/res"), [])

    def test_cli_fails_and_preserves_files_when_duplicate_exists(self):
        with tempfile.TemporaryDirectory() as tmp:
            res = Path(tmp) / "res"
            folder = res / "mipmap-xhdpi"
            folder.mkdir(parents=True)
            for ext in ("png", "webp"):
                (folder / f"ic_launcher.{ext}").write_bytes(b"data")
            result = subprocess.run(
                [sys.executable, str(ROOT / "tools/check_android_resource_collisions.py"),
                 "--res", str(res)], text=True, capture_output=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("ic_launcher", result.stderr)
            self.assertIn("Duplicate", result.stderr)
            self.assertEqual(len(list(folder.iterdir())), 2)


if __name__ == "__main__":
    unittest.main()
