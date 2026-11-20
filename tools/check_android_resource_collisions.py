#!/usr/bin/env python3
"""Read-only Android drawable/mipmap collision preflight (Python standard library).

For example, mipmap-hdpi/ic_launcher.png and ic_launcher.webp both compile to
@mipmap/ic_launcher and Gradle's mergeResources task cannot merge them.
"""
import argparse
from collections import defaultdict
from pathlib import Path
import sys

IMAGE_TYPES = {".png", ".webp", ".xml", ".jpg", ".jpeg", ".gif", ".avif"}


def find_collisions(res: Path) -> list[tuple[str, list[Path]]]:
    """Find resource-name collisions *within the same qualified resource folder*."""
    found = []
    if not res.is_dir():
        raise FileNotFoundError(f"Android res directory does not exist: {res}")
    for folder in sorted(res.iterdir()):
        if not folder.is_dir() or folder.name.split("-", 1)[0] not in {"drawable", "mipmap"}:
            continue
        by_name = defaultdict(list)
        for path in sorted(folder.iterdir()):
            if not path.is_file() or path.suffix.lower() not in IMAGE_TYPES:
                continue
            # Android nine-patch images use name.9.png but resource name is name.
            name = path.stem.removesuffix(".9")
            by_name[name].append(path)
        for name, paths in sorted(by_name.items()):
            if len(paths) > 1:
                found.append((f"{folder.name}/{name}", paths))
    return found


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--res", type=Path, default=Path(__file__).resolve().parents[1] / "app/src/main/res")
    args = parser.parse_args()
    try:
        collisions = find_collisions(args.res)
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2
    if collisions:
        print("Duplicate Android drawable/mipmap resources detected:", file=sys.stderr)
        for name, paths in collisions:
            print(f"  @{name}: " + ", ".join(str(p) for p in paths), file=sys.stderr)
        print("Keep the approved original PNG icons; remove only verified stale conflicting ",
              "ic_launcher.webp copies after backing up the project. ",
              "See docs/ANDROID_COGO_RESOURCE_FIX_AR.md.", sep="", file=sys.stderr)
        print("No files were changed.", file=sys.stderr)
        return 1
    print("PASS: Android drawable/mipmap resource names are unique")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
