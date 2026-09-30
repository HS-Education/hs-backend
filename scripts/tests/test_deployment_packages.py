import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[2]


def module(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / f"{name}.py")
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


class PackageTests(unittest.TestCase):
    def test_worker_package_contains_no_environment_or_virtualenv(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "worker.zip"
            module("package-azure-worker").package(path)
            with ZipFile(path) as archive:
                self.assertEqual(len(archive.namelist()), 7)
                self.assertNotIn(".env", archive.namelist())
                self.assertIn(b"-r requirements-base.txt", archive.read("requirements.txt"))

    def asset(self, folder, bad_entry=None):
        path = Path(folder) / "frontend.zip"
        with ZipFile(path, "w") as archive:
            archive.writestr("index.html", "<html>HS Education</html>")
            archive.writestr("runtime-config.json", '{"apiBaseUrl":"/api/v1"}')
            if bad_entry:
                archive.writestr(bad_entry, "bad")
        manifest = {"tag": "v0.2.0", "commit": "a" * 40, "apiBaseUrl": "/api/v1",
                    "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
        (Path(folder) / "frontend-manifest.json").write_text(json.dumps(manifest))

    def test_frontend_release_imports_after_integrity_check(self):
        with tempfile.TemporaryDirectory() as folder:
            self.asset(folder)
            target = Path(folder) / "static"
            module("import-frontend-release").import_release(folder, "v0.2.0", target)
            self.assertTrue((target / "index.html").is_file())

    def test_path_traversal_and_hidden_environment_are_rejected(self):
        for entry in ("../.env", "/tmp/file", "C:\\secret", ".env"):
            with self.subTest(entry=entry), tempfile.TemporaryDirectory() as folder:
                self.asset(folder, entry)
                with self.assertRaises(ValueError):
                    module("import-frontend-release").import_release(folder, "v0.2.0", Path(folder) / "static")

    def test_checksum_mismatch_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            self.asset(folder)
            with (Path(folder) / "frontend.zip").open("ab") as stream:
                stream.write(b"tampered")
            with self.assertRaises(ValueError):
                module("import-frontend-release").import_release(folder, "v0.2.0", Path(folder) / "static")
