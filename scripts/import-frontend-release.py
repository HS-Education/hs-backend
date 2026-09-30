"""Verify and extract a known frontend release into the Spring Boot static resources."""
import hashlib
import json
import os
import re
from pathlib import Path, PurePosixPath
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]


def import_release(folder, tag, destination=None):
    folder = Path(folder)
    destination = Path(destination or ROOT / "src" / "main" / "resources" / "static")
    manifest = json.loads((folder / "frontend-manifest.json").read_text(encoding="utf-8"))
    archive = folder / "frontend.zip"
    if (not re.fullmatch(r"v[0-9]+\.[0-9]+\.[0-9]+", tag) or manifest["tag"] != tag
            or not re.fullmatch(r"[a-f0-9]{40}", manifest["commit"])
            or manifest["apiBaseUrl"] != "/api/v1"
            or archive.stat().st_size > 50 * 1024 * 1024
            or hashlib.sha256(archive.read_bytes()).hexdigest() != manifest["sha256"]):
        raise ValueError("Invalid frontend release integrity or same-origin configuration")
    with ZipFile(archive) as zipped:
        entries = zipped.infolist()
        if len(entries) > 2000 or sum(entry.file_size for entry in entries) > 100 * 1024 * 1024:
            raise ValueError("Frontend archive exceeds limits")
        for entry in entries:
            path = PurePosixPath(entry.filename)
            if (path.is_absolute() or ".." in path.parts or "\\" in entry.filename or ":" in entry.filename
                    or any(part.startswith(".") for part in path.parts)
                    or (entry.external_attr >> 16) & 0o170000 == 0o120000):
                raise ValueError("Unsafe frontend archive entry")
        if not {"index.html", "runtime-config.json"}.issubset(zipped.namelist()):
            raise ValueError("Missing frontend entry points")
        config = json.loads(zipped.read("runtime-config.json"))
        if config != {"apiBaseUrl": "/api/v1"}:
            raise ValueError("Frontend runtime configuration must be same-origin and contain no credentials")
        destination.mkdir(parents=True, exist_ok=True)
        zipped.extractall(destination)
    print("Frontend release integrity and same-origin configuration verified.")
    return manifest


if __name__ == "__main__":
    import_release(ROOT / "target" / "frontend-release", os.environ["RELEASE_TAG"])
