"""Package a fixed allowlist; no .env, credentials, tests, virtualenv or Docker files."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
FILES = ("main.py", "openrouter_contract.py", "safety.py", "infrastructure.py", "worker_settings.py")


def package(destination=None):
    destination = Path(destination or ROOT / "target" / "azure-worker.zip")
    destination.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(destination, "w", ZIP_DEFLATED) as archive:
        for name in FILES:
            archive.write(ROOT / "ai-service" / name, name)
        archive.write(ROOT / "ai-service" / "requirements.txt", "requirements-base.txt")
        azure = (ROOT / "ai-service" / "requirements-azure.txt").read_text(encoding="utf-8")
        archive.writestr("requirements.txt", azure.replace("-r requirements.txt", "-r requirements-base.txt"))
    print("Worker archive created from the deployment allowlist.")


if __name__ == "__main__":
    package()
