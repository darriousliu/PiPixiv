#!/usr/bin/env python3
"""Install a pinned official sentry-cli binary for the desktop and iOS CI runners."""

import hashlib
import os
import platform
import shutil
import subprocess
import urllib.request
from pathlib import Path


VERSION = "3.7.0"
# SHA-256 digests recorded by GitHub for the official release assets:
# https://api.github.com/repos/getsentry/sentry-cli/releases/tags/3.7.0
ASSETS = {
    ("Linux", "x86_64"): (
        "sentry-cli-Linux-x86_64",
        "cec71d46a7cc394c94b6e75f1601985c710d457376c546ef3975567b3671563b",
    ),
    ("Darwin", "arm64"): (
        "sentry-cli-Darwin-arm64",
        "c66564094fbe56ee3b359f7574541f858b8d1df0328a0a759da972fbf1886048",
    ),
    ("Darwin", "x86_64"): (
        "sentry-cli-Darwin-x86_64",
        "fcd74786b4d95c6b7531662607897aadd5ab5d64c5d0468a6f4bd97ad04bedb8",
    ),
    ("Windows", "amd64"): (
        "sentry-cli-Windows-x86_64.exe",
        "8643986aec8d8cf8d69cd476d67427578e5dbbda378eba506d199681082abe5a",
    ),
}


def main():
    target = (platform.system(), platform.machine().lower())
    if target not in ASSETS:
        raise RuntimeError(f"Unsupported sentry-cli runner: {target}")
    asset, expected_sha256 = ASSETS[target]
    install_dir = Path(os.environ["RUNNER_TEMP"]) / f"sentry-cli-{VERSION}"
    install_dir.mkdir(parents=True, exist_ok=True)
    binary = install_dir / ("sentry-cli.exe" if target[0] == "Windows" else "sentry-cli")
    download = install_dir / f"{asset}.download"
    url = f"https://github.com/getsentry/sentry-cli/releases/download/{VERSION}/{asset}"
    try:
        with urllib.request.urlopen(url, timeout=60) as response, download.open("wb") as output:
            shutil.copyfileobj(response, output)
        actual_sha256 = hashlib.sha256(download.read_bytes()).hexdigest()
        if actual_sha256 != expected_sha256:
            raise RuntimeError(f"SHA-256 mismatch for {asset}; refusing to install")
        download.replace(binary)
    finally:
        download.unlink(missing_ok=True)

    binary.chmod(0o755)
    subprocess.run([str(binary), "--version"], check=True)
    with Path(os.environ["GITHUB_PATH"]).open("a", encoding="utf-8") as output:
        output.write(f"{install_dir}\n")
    with Path(os.environ["GITHUB_ENV"]).open("a", encoding="utf-8") as output:
        output.write(f"SENTRY_CLI_EXECUTABLE={binary}\n")


if __name__ == "__main__":
    main()
