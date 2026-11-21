#!/usr/bin/env python3
"""Build fixtures, run PiPixiv migration upgrades, and summarize the measurements."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import platform
import plistlib
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import time
import uuid
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from statistics import median
from urllib.parse import unquote, urlparse


ROOT = Path(__file__).resolve().parents[1]
FIXTURES_DIR = ROOT / "build/migration-fixtures"
RESULTS_DIR = ROOT / "build/migration-device"
JVM_REPORT_DIR = ROOT / "build/reports/migration-benchmark"
SUMMARY_PATH = ROOT / "build/reports/migration/summary.md"
BASE_TIERS = ("typical", "heavy", "extreme")
IOS_TIERS = tuple(f"{tier}-ios" for tier in BASE_TIERS)
TIERS = (*BASE_TIERS, "typical-corrupt-session", *IOS_TIERS)
FAULTS = {
    "low-space": {"availableBytes": 0},
    "global-database": {"fail": ["GlobalDatabase"]},
    "browsing-history": {"fail": ["BrowsingHistory"]},
}
SCENARIOS = ("normal", *FAULTS, "corrupt-session", "storage-unavailable")
MIGRATION_ITEMS = {
    "Session", "Settings", "SearchAndTags", "SeriesProgress", "GlobalDatabase",
    "BrowsingHistory", "ReadingProgress", "ReadLater", "TranslationCache", "LegacyBlocking",
}
FIXTURE_FILES = {
    "pixiv_db", "mmkv/pixiv.app", "mmkv/pixiv.app.crc", "mmkv/pixiv.user", "mmkv/pixiv.user.crc",
}
TIMING_PREFIX = "MigrationTiming "


def run_command(*args, check=True, input=None, timeout=120):
    return subprocess.run(args, check=check, input=input, timeout=timeout, capture_output=True)


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for data in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(data)
    return digest.hexdigest()


def write_json(path, value):
    temporary = path.with_suffix(".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(path)


def run_fixtures(args):
    environment = {**os.environ, "PIPIXIV_MIGRATION_FIXTURES": "1"}
    if args.corrupt_session:
        environment["PIPIXIV_MIGRATION_CORRUPT_SESSION"] = "1"
    if args.ios:
        environment["PIPIXIV_MIGRATION_IOS"] = "1"
    gradle_flags = ["--offline"] if args.offline else []
    subprocess.run(
        [
            str(ROOT / "gradlew"), ":composeApp:jvmTest", "--tests",
            "*StartupMigrationBenchmarkTest.exportFixtures*", "--rerun",
            "-PcomposePreview=true", "--no-build-cache", "--no-parallel", "--max-workers=1", *gradle_flags,
        ],
        cwd=ROOT,
        env=environment,
        check=True,
    )
    print(FIXTURES_DIR)


def load_fixture(tier):
    directory = FIXTURES_DIR / tier
    manifest = json.loads((directory / "manifest.json").read_text(encoding="utf-8"))
    if manifest["tier"] != tier or set(manifest["files"]) != FIXTURE_FILES:
        raise ValueError(f"Invalid fixture manifest in {directory}")
    for name, metadata in manifest["files"].items():
        path = directory / name
        if path.stat().st_size != metadata["bytes"] or sha256(path) != metadata["sha256"]:
            raise ValueError(f"Fixture file does not match its manifest: {path}")
    return directory, manifest


def collect_timing(target, raw_path, accepted, previous_line=None):
    deadline = time.monotonic() + 120
    process_check_at = time.monotonic() + 3
    while time.monotonic() < deadline:
        logs = target.logs()
        for line in reversed(logs.get("current.log", b"").splitlines()):
            if line == previous_line or TIMING_PREFIX.encode() not in line:
                continue
            try:
                entry = json.loads(line)
                message = entry["message"]
                if not message.startswith(TIMING_PREFIX):
                    continue
                timing = json.loads(message[len(TIMING_PREFIX):])
            except (KeyError, TypeError, AttributeError, ValueError):
                continue
            if timing.get("outcome") in accepted:
                raw_path.write_bytes(line + b"\n")
                return timing, line
        if time.monotonic() >= process_check_at:
            if not target.is_running():
                raise RuntimeError("PiPixiv exited before writing MigrationTiming")
            process_check_at = time.monotonic() + 3
        time.sleep(0.5)
    raise RuntimeError(f"120 s elapsed without a fresh MigrationTiming ({', '.join(sorted(accepted))})")


def verify_migration(timing, scenario):
    items = timing["items"]
    expected = {
        "Session": {"type": "Done"},
        "Settings": {"type": "Done"},
        "SearchAndTags": {"type": "Done"},
        "SeriesProgress": {"type": "Done"},
        "GlobalDatabase": {"type": "Done"},
        "BrowsingHistory": {"type": "Done"},
        "ReadingProgress": {"type": "Done"},
        "ReadLater": {"type": "Done"},
        "TranslationCache": {"type": "Skipped"},
    }
    degraded = scenario in {"low-space", "global-database"}
    if degraded:
        reason = "StorageSpace" if scenario == "low-space" else "Corrupted"
        expected["TranslationCache"] = {"type": "Pending", "reason": reason}
    if scenario == "browsing-history":
        expected["BrowsingHistory"] = {"type": "Failed", "attempts": 1, "reason": "Unknown"}
    if scenario == "corrupt-session":
        expected["Session"] = {"type": "Failed", "attempts": 1, "reason": "Corrupted"}
        for item in ("Settings", "SearchAndTags"):
            expected[item] = {"type": "Pending", "reason": "Corrupted"}
    outcome = "blocked" if scenario == "storage-unavailable" else "success"
    checks = {"outcome": timing["outcome"] == outcome, "allItemsRecorded": set(items) == MIGRATION_ITEMS}
    if scenario != "storage-unavailable":
        checks["degraded"] = timing["degraded"] == degraded
        checks["itemStates"] = all(items.get(item) == status for item, status in expected.items())
        checks["legacyBlockingHandled"] = items.get("LegacyBlocking", {}).get("type") in {"Done", "Skipped"}
    return checks


def run_upgrade(target, args, fixture, manifest, output, evidence):
    result = {
        "platform": target.platform,
        "device": target.device,
        "simulator": target.simulator,
        "tier": args.tier,
        "scenario": args.scenario,
        "migration": None,
        "completedStartup": None,
        "mmkvMatches": None,
        "verification": {"passed": False},
        "screenshot": None,
    }
    try:
        target.reset()
        target.install(args.baseline)
        target.launch()
        target.wait_for_baseline()
        target.stop()

        readonly = args.scenario == "storage-unavailable"
        target.prepare_fixture(fixture, FAULTS.get(args.scenario), readonly, output)
        target.install(args.feature)
        target.launch()
        timing, first_line = collect_timing(target, output / "migration.log", {"success", "blocked", "alreadyComplete"})
        result["migration"] = timing
        if timing["outcome"] == "alreadyComplete":
            raise RuntimeError("覆盖安装后的首次冷启动未执行迁移，已停止测量。")
        checks = verify_migration(timing, args.scenario)
        result["verification"].update(checks)
        if timing["outcome"] != "blocked" or timing.get("mmkvCounts", {}).get("accounts") is not None:
            result["mmkvMatches"] = timing["mmkvCounts"] == manifest["mmkvCounts"]
            result["mmkvExpected"] = manifest["mmkvCounts"]
        if result["mmkvMatches"] is False:
            raise RuntimeError("MMKV 文件不可移植，已停止测量。详见本轮 result.json。")
        if timing["outcome"] == "blocked" and not readonly:
            raise RuntimeError("迁移意外阻塞，已停止测量。详见本轮 result.json。")

        if evidence is not None and hasattr(target, "screenshot"):
            try:
                time.sleep(args.screenshot_delay)
                target.screenshot(evidence)
                if evidence.is_file():
                    result["screenshot"] = str(evidence.relative_to(ROOT)) if evidence.is_relative_to(ROOT) else str(evidence)
            except (OSError, RuntimeError, subprocess.SubprocessError) as error:
                result["verification"]["screenshotError"] = str(error)
            if target.platform == "android" and args.scenario in {"corrupt-session", "storage-unavailable"}:
                try:
                    hierarchy = evidence.with_suffix(".xml")
                    target.hierarchy(hierarchy)
                    result["hierarchy"] = str(hierarchy.relative_to(ROOT)) if hierarchy.is_relative_to(ROOT) else str(hierarchy)
                except (OSError, RuntimeError, subprocess.SubprocessError) as error:
                    result["verification"]["hierarchyError"] = str(error)
        if not target.is_running():
            raise RuntimeError("PiPixiv 在迁移后退出，已停止测量。")
        if args.scenario in {"normal", *FAULTS} and timing["outcome"] == "success":
            target.stop()
            preserved = "pixiv_db.legacy" if timing["degraded"] else "pixiv_db.bak"
            if args.scenario in {"normal", "global-database", "low-space"}:
                actual = target.database_sha256(preserved)
                expected = manifest["files"]["pixiv_db"]["sha256"]
                result["verification"].update(
                    preservedDatabase=preserved, originalSha256=expected, preservedSha256=actual,
                    sha256Matches=actual == expected,
                )
            target.launch()
            completed, _ = collect_timing(target, output / "completed-startup.log", {"alreadyComplete", "blocked", "success"}, first_line)
            result["completedStartup"] = completed
            result["verification"]["completedStartup"] = completed["outcome"] == "alreadyComplete"
            if completed["outcome"] == "blocked":
                raise RuntimeError("再次启动出现阻塞，已停止测量。详见本轮 result.json。")
        result["verification"]["passed"] = all(checks.values()) and result["mmkvMatches"] is not False and all(
            result["verification"].get(key, True) for key in ("sha256Matches", "completedStartup")
        )
        if not result["verification"]["passed"]:
            raise RuntimeError(f"Migration did not match the scenario expectations: {output / 'result.json'}")
    except (OSError, ValueError, RuntimeError, subprocess.SubprocessError) as error:
        result["verification"]["error"] = str(error)
        if isinstance(target, DesktopTarget) and target.process is not None:
            result["verification"]["desktopExitCode"] = target.process.poll()
        raise
    finally:
        try:
            try:
                target.stop()
            finally:
                target.restore_permissions()
        finally:
            write_json(output / "result.json", result)
    print(f"{target.platform}/{target.device} {args.tier} {args.scenario}: {timing['totalMs']} ms ({output})", flush=True)


def run_device(args):
    if args.platform == "ios" and not args.device:
        raise ValueError("run --platform ios requires an explicit --device")
    if args.tier in IOS_TIERS and args.platform != "ios":
        raise ValueError("The -ios fixture tiers are available only with --platform ios")
    if args.runs < 1:
        raise ValueError("--runs must be at least 1")
    if (args.scenario == "corrupt-session") != (args.tier == "typical-corrupt-session"):
        raise ValueError("Use --tier typical-corrupt-session with --scenario corrupt-session")
    if args.scenario == "storage-unavailable" and args.platform != "android":
        raise ValueError("--scenario storage-unavailable is available on Android only")
    if args.screenshot_delay < 0:
        raise ValueError("--screenshot-delay must be nonnegative")
    for package in (args.baseline, args.feature):
        if not package.exists():
            raise ValueError(f"Package does not exist: {package}")
        if args.platform == "android" and (not package.is_file() or package.suffix != ".apk"):
            raise ValueError(f"Expected an Android APK: {package}")
    fixture, manifest = load_fixture(args.tier)
    target = make_target(args)
    if args.scenario == "storage-unavailable" and not target.simulator:
        raise ValueError("The storage-unavailable scenario requires an Android emulator")
    if args.platform == "desktop":
        target.install(args.feature)
        target.install(args.baseline)
    device = re.sub(r"[^A-Za-z0-9_.-]", "_", target.device)
    run_root = RESULTS_DIR / f"{target.platform}-{device}" / args.tier / args.scenario
    group = datetime.now().strftime("%Y%m%d-%H%M%S-%f")
    for index in range(1, args.runs + 1):
        output = run_root / f"{group}-{index:02d}"
        output.mkdir(parents=True)
        evidence = None
        if args.evidence_dir:
            args.evidence_dir.mkdir(parents=True, exist_ok=True)
            evidence = args.evidence_dir / f"{target.platform}-{args.tier}-{args.scenario}-{group}-{index:02d}.png"
        run_upgrade(target, args, fixture, manifest, output, evidence)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    fixtures = commands.add_parser("fixtures", help="generate or reuse JVM fixture packages")
    fixtures.add_argument("--offline", action="store_true", help="use Gradle's local dependency cache")
    fixtures.add_argument("--corrupt-session", action="store_true", help="also export the corrupt-session fixture")
    fixtures.add_argument("--ios", action="store_true", help="also export iOS fixtures without MMKV string sets")
    fixtures.set_defaults(handler=run_fixtures)
    run = commands.add_parser("run", help="reset the target and run in-place upgrades")
    run.add_argument("--platform", required=True, choices=("desktop", "android", "ios"))
    run.add_argument("--device", help="ADB serial or required iOS physical device identifier")
    run.add_argument("--tier", required=True, choices=TIERS)
    run.add_argument("--runs", type=int, required=True)
    run.add_argument("--baseline", type=lambda path: Path(path).expanduser().resolve(), required=True)
    run.add_argument("--feature", type=lambda path: Path(path).expanduser().resolve(), required=True)
    run.add_argument("--scenario", choices=SCENARIOS, default="normal")
    run.add_argument("--bundle-id", help="explicitly select the iOS test bundle (com.mrl.pixiv.PiPixiv)")
    run.add_argument("--evidence-dir", type=lambda path: Path(path).expanduser().resolve(), help="save a screenshot after each migration where supported")
    run.add_argument("--screenshot-delay", type=float, default=4, help="seconds to wait for the first page after migration")
    run.set_defaults(handler=run_device)
    report = commands.add_parser("report", help="combine JVM and device results")
    report.set_defaults(handler=run_report)
    args = parser.parse_args()
    try:
        args.handler(args)
    except (OSError, RuntimeError, ValueError, subprocess.SubprocessError) as error:
        print(f"error: {error}", file=sys.stderr)
        return 1
    return 0
ANDROID_PACKAGE = "com.mrl.pixiv"
IOS_TEST_BUNDLE_ID = "com.mrl.pixiv.PiPixiv"
DESKTOP_TEST_MARKER = ".pipixiv-migration-device-benchmark"
LOG_NAMES = ("current.log", "previous.log")


def device_label(identifier):
    label = re.sub(r"[^A-Za-z0-9_-]", "_", identifier)
    if label != identifier or len(label) > 80:
        digest = hashlib.sha256(identifier.encode()).hexdigest()[:10]
        return f"{label[:69]}-{digest}"
    return label


def fixture_payload(fixture, debug_config, scratch, databases, mmkv, files):
    payload = [(fixture / "pixiv_db", f"{databases}/pixiv_db")]
    for name in ("pixiv_db-wal", "pixiv_db-shm"):
        sidecar = scratch / name
        sidecar.touch()
        payload.append((sidecar, f"{databases}/{name}"))
    for name in ("pixiv.user", "pixiv.user.crc", "pixiv.app", "pixiv.app.crc"):
        payload.append((fixture / "mmkv" / name, f"{mmkv}/{name}"))
    if debug_config is not None:
        debug_file = scratch / "migration-debug.json"
        debug_file.write_text(json.dumps(debug_config), encoding="utf-8")
        payload.append((debug_file, f"{files}/migration-debug.json"))
    return payload


def wait_until_ready(target, ready):
    deadline = time.monotonic() + 30
    while time.monotonic() < deadline:
        if not target.is_running():
            raise RuntimeError(f"PiPixiv exited during the {target.platform} baseline launch.")
        if ready():
            return
        time.sleep(0.5)
    raise RuntimeError(f"PiPixiv did not finish the {target.platform} baseline launch within 30 seconds.")


class DesktopTarget:
    platform = "desktop"
    simulator = False

    def __init__(self):
        if sys.platform != "darwin":
            raise ValueError("Desktop benchmarking currently requires macOS.")
        self.data_dir = Path.home() / "Library" / "Application Support" / "PiPixiv"
        self.device = device_label(platform.node() or platform.machine())
        self.executable = None
        self.process = None

    def ensure_no_other_process(self):
        process_names = run_command("/bin/ps", "-axo", "comm=").stdout.decode(errors="replace").splitlines()
        process_arguments = run_command("/bin/ps", "-axo", "args=").stdout.decode(errors="replace").splitlines()
        packaged = any(
            Path(name.strip()).name == "PiPixiv" and "/CoreSimulator/Devices/" not in name
            for name in process_names
        )
        gradle = any(
            re.search(r"(?:^|/)java\b.*\scom\.mrl\.pixiv\.MainKt(?:\s|$)", command)
            or re.search(r"(?:^|\s):desktopApp:(?:hotRun|hotDev|run)(?:Async)?(?:\s|$)", command)
            for command in process_arguments
        )
        if packaged or gradle:
            raise RuntimeError("PiPixiv desktop is running. Quit it before benchmarking.")

    def reset(self):
        self.ensure_no_other_process()
        if self.data_dir.exists() or self.data_dir.is_symlink():
            self.ensure_test_directory()
            shutil.rmtree(self.data_dir)
        self.data_dir.mkdir()
        (self.data_dir / DESKTOP_TEST_MARKER).write_text("PiPixiv migration benchmark\n", encoding="utf-8")

    def ensure_test_directory(self):
        marker = self.data_dir / DESKTOP_TEST_MARKER
        if (
            self.data_dir.is_symlink() or marker.is_symlink() or not marker.is_file()
            or marker.read_text(encoding="utf-8") != "PiPixiv migration benchmark\n"
        ):
            raise RuntimeError(
                f"Refusing to benchmark with existing desktop data at {self.data_dir}. "
                "Verify a backup and move the data directory aside before running."
            )

    def install(self, package):
        self.executable = package / "Contents" / "MacOS" / "PiPixiv"
        if not self.executable.is_file() or not os.access(self.executable, os.X_OK):
            raise ValueError(f"Expected a macOS PiPixiv.app distribution at {package}.")

    def launch(self):
        self.ensure_test_directory()
        self.ensure_no_other_process()
        self.process = subprocess.Popen(
            [str(self.executable)],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
        )

    def is_running(self):
        return self.process is not None and self.process.poll() is None

    def stop(self):
        if self.is_running():
            self.process.terminate()
            try:
                self.process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=10)

    def prepare_fixture(self, fixture, debug_config, readonly_db, output):
        if readonly_db:
            raise ValueError("Read-only database injection is supported only on the Android emulator.")
        self.ensure_test_directory()
        actual_paths = ("databases", "mmkv", "logs")
        for name in actual_paths:
            if not (self.data_dir / name).is_dir():
                raise RuntimeError(f"Desktop baseline did not create {self.data_dir / name}.")
        (output / "container-paths.txt").write_text(
            "".join(f"{self.data_dir / name}\n" for name in actual_paths), encoding="utf-8"
        )
        with tempfile.TemporaryDirectory(prefix="pipixiv-migration-") as scratch:
            for source, relative in fixture_payload(fixture, debug_config, Path(scratch), "databases", "mmkv", "."):
                shutil.copyfile(source, self.data_dir / relative)

    def restore_permissions(self):
        pass

    def logs(self):
        result = {}
        for name in LOG_NAMES:
            try:
                result[name] = (self.data_dir / "logs" / name).read_bytes()
            except FileNotFoundError:
                pass
        return result

    def database_sha256(self, name):
        return sha256(self.data_dir / "databases" / name)

    def wait_for_baseline(self):
        def ready():
            return all((self.data_dir / name).is_dir() for name in ("databases", "mmkv", "logs")) and self.window_id()

        wait_until_ready(self, ready)

    def window_id(self):
        # Match this app's PID so captures exclude all other desktop content.
        window_query = """
ObjC.import('CoreGraphics');
function run(args) {
    const pid = Number(args[0]);
    const windows = ObjC.deepUnwrap(ObjC.castRefToObject($.CGWindowListCopyWindowInfo(17, 0)));
    const area = w => w.kCGWindowBounds.Width * w.kCGWindowBounds.Height;
    const visible = windows.filter(w => w.kCGWindowOwnerPID === pid && w.kCGWindowLayer === 0 && area(w) > 0);
    visible.sort((a, b) => area(b) - area(a));
    return visible.length > 0 ? String(visible[0].kCGWindowNumber) : '';
}
"""
        result = run_command("/usr/bin/osascript", "-l", "JavaScript", "-e", window_query, str(self.process.pid))
        window = result.stdout.strip()
        return window.decode() if window.isdigit() else None

    def screenshot(self, path):
        if not self.is_running():
            raise RuntimeError("PiPixiv exited before the window screenshot.")
        window = self.window_id()
        if window is None:
            raise RuntimeError("The PiPixiv window was not visible to macOS screen capture.")
        capture = run_command("/usr/sbin/screencapture", "-x", "-o", f"-l{window}", str(path), check=False)
        if capture.returncode:
            reason = capture.stderr.decode(errors="replace").strip()
            raise RuntimeError(reason or f"macOS window capture exited with status {capture.returncode}.")
        if not path.is_file():
            raise RuntimeError("No PiPixiv window screenshot was captured.")


class AndroidTarget:
    platform = "android"
    databases = "databases"
    mmkv = "files/mmkv"
    files = "files"

    def __init__(self, serial):
        if not serial:
            devices = run_command("adb", "devices").stdout.decode().splitlines()[1:]
            online = [line.split()[0] for line in devices if line.split()[1:] == ["device"]]
            if len(online) != 1:
                raise ValueError("Specify --device for Android if exactly one authorized device is not connected.")
            serial = online[0]
        self.serial = serial
        self.device = device_label(serial)
        self.permission_modes = {}
        self.simulator = serial.startswith("emulator-") or self.adb("shell", "getprop", "ro.kernel.qemu").stdout.strip() == b"1"

    def adb(self, *args, **kwargs):
        return run_command("adb", "-s", self.serial, *args, **kwargs)

    def run_as(self, *args, **kwargs):
        # adb passes a command line to the device shell rather than an exec argv list.
        return self.adb("shell", "-T", shlex.join(("run-as", ANDROID_PACKAGE, *args)), **kwargs)

    def reset(self):
        self.restore_permissions()
        packages = self.adb("shell", "pm", "list", "packages", ANDROID_PACKAGE).stdout.splitlines()
        if f"package:{ANDROID_PACKAGE}".encode() in packages:
            self.adb("uninstall", ANDROID_PACKAGE)

    def install(self, package):
        self.adb("install", "-r", str(package), timeout=120)

    def launch(self):
        result = self.adb("shell", "am", "start", "-W", "-S", "-n", f"{ANDROID_PACKAGE}/.MainActivity")
        if b"Status: ok" not in result.stdout:
            raise RuntimeError(f"Android did not launch PiPixiv: {result.stdout.decode(errors='replace').strip()}")

    def is_running(self):
        return self.adb("shell", "pidof", ANDROID_PACKAGE, check=False).returncode == 0

    def stop(self):
        self.adb("shell", "am", "force-stop", ANDROID_PACKAGE)

    def wait_for_baseline(self):
        def ready():
            return self.run_as("ls", "-d", self.databases, self.mmkv, "files/logs", check=False).returncode == 0

        wait_until_ready(self, ready)

    def prepare_fixture(self, fixture, debug_config, readonly_db, output):
        if readonly_db and not self.simulator:
            raise ValueError("Read-only database injection is supported only on the Android emulator.")
        (output / "container-paths.txt").write_bytes(self.run_as("ls", "-R", self.databases, self.mmkv, "files/logs").stdout)
        with tempfile.TemporaryDirectory(prefix="pipixiv-migration-") as scratch:
            for source, target in fixture_payload(fixture, debug_config, Path(scratch), self.databases, self.mmkv, self.files):
                remote_temp = f"/data/local/tmp/pipixiv-migration-{uuid.uuid4().hex}"
                try:
                    self.adb("push", str(source), remote_temp, timeout=120)
                    self.adb("shell", "chmod", "644", remote_temp)
                    self.run_as("cp", remote_temp, target, timeout=120)
                finally:
                    self.adb("shell", "rm", "-f", remote_temp, check=False)
        if readonly_db:
            paths = [f"databases/{name}" for name in ("pixiv_db", "pixiv_db-wal", "pixiv_db-shm")]
            permissions = [(path, "400") for path in paths] + [("databases", "500")]
            for path, permission in permissions:
                mode = self.run_as("stat", "-c", "%a", path).stdout.decode().strip()
                if not re.fullmatch(r"[0-7]{3,4}", mode):
                    raise RuntimeError(f"Cannot determine Android permissions for {path}.")
                self.permission_modes[path] = mode
                self.run_as("chmod", permission, path)
            (output / "readonly-database.txt").write_bytes(self.run_as("ls", "-ld", "databases", *paths).stdout)

    def restore_permissions(self):
        for path in reversed(tuple(self.permission_modes)):
            self.run_as("chmod", self.permission_modes[path], path)
            del self.permission_modes[path]

    def logs(self):
        result = {}
        for name in LOG_NAMES:
            entry = self.run_as("cat", f"files/logs/{name}", check=False)
            if entry.returncode == 0:
                result[name] = entry.stdout
            elif b"No such file or directory" not in entry.stderr + entry.stdout:
                raise RuntimeError(f"Cannot read Android app log {name}: {entry.stderr.decode(errors='replace').strip()}")
        return result

    def database_sha256(self, name):
        return self.run_as("sha256sum", f"{self.databases}/{name}").stdout.decode().split()[0]

    def screenshot(self, path):
        path.write_bytes(self.adb("exec-out", "screencap", "-p").stdout)

    def hierarchy(self, path):
        remote = f"/data/local/tmp/pipixiv-migration-{uuid.uuid4().hex}.xml"
        try:
            self.adb("shell", "uiautomator", "dump", remote, timeout=30)
            path.write_bytes(self.adb("exec-out", "cat", remote).stdout)
        finally:
            self.adb("shell", "rm", "-f", remote, check=False)


class IOSTarget:
    platform = "ios"
    simulator = False
    databases = "Documents/databases"
    mmkv = "Documents/mmkv"
    files = "Documents"
    log_dir = "Documents/logs"

    def __init__(self, identifier, bundle_id, baseline, feature):
        if not identifier:
            raise ValueError("--device is required for iOS. Select a trusted physical device explicitly.")
        self.identifier = identifier
        self.device = device_label(identifier)
        baseline_id = self.package_id(baseline)
        feature_id = self.package_id(feature)
        self.bundle_id = bundle_id or feature_id
        if any(value != IOS_TEST_BUNDLE_ID for value in (self.bundle_id, baseline_id, feature_id)):
            raise ValueError(f"iOS measurements are limited to the test bundle {IOS_TEST_BUNDLE_ID}.")
        self.pid = None

    @staticmethod
    def package_id(package):
        with (Path(package) / "Info.plist").open("rb") as manifest:
            return plistlib.load(manifest)["CFBundleIdentifier"]

    def tool(self, group, action, *args, **kwargs):
        try:
            return run_command("xcrun", "devicectl", "device", group, action, "--device", self.identifier, *args, **kwargs)
        except subprocess.CalledProcessError as error:
            message = (error.stderr + error.stdout).decode(errors="replace").strip()
            raise RuntimeError(f"devicectl {group} {action} failed ({error.returncode}): {message}") from error

    def container_command(self, group, action, *args, **kwargs):
        return self.tool(group, action, "--domain-type", "appDataContainer", "--domain-identifier", self.bundle_id, *args, **kwargs)

    def installed_app(self):
        result = self.tool("info", "apps", "--include-all-apps", "--json-output", "-")
        entries = json.loads(result.stdout)["result"]["apps"]
        apps = {entry["bundleIdentifier"]: entry for entry in entries}
        other_apps = sorted(bundle for bundle in apps if bundle.startswith(IOS_TEST_BUNDLE_ID) and bundle != IOS_TEST_BUNDLE_ID)
        if other_apps:
            raise RuntimeError(f"Other PiPixiv apps are installed on the iOS device: {', '.join(other_apps)}")
        return apps.get(self.bundle_id)

    def info_files(self, directory=None, *, recurse=False):
        location = () if directory is None else ("--subdirectory", directory)
        result = self.container_command(
            "info", "files", *location, "--recurse" if recurse else "--no-recurse", "--json-output", "-",
        )
        return {entry["relativePath"]: entry for entry in json.loads(result.stdout)["result"]["files"]}

    def test_process_ids(self, app):
        app_path = unquote(urlparse(app["url"]).path).rstrip("/")
        if not app_path.endswith(".app"):
            raise RuntimeError("Could not identify the executable directory of the iOS test app.")
        return [
            process["processIdentifier"] for process in self.running_processes()
            if unquote(urlparse(process["executable"]).path).startswith(app_path + "/")
        ]

    def reset(self):
        app = self.installed_app()
        if app is None:
            return
        if app.get("containerAccessible") is not True:
            raise RuntimeError("The installed iOS test app container is not accessible.")
        for pid in self.test_process_ids(app):
            self.tool("process", "terminate", "--pid", str(pid), check=False)
        if self.test_process_ids(app):
            raise RuntimeError("The iOS test app process is still running.")
        self.pid = None

        root_entries = self.info_files()
        paths = [path for path in ("Documents", "Library", "tmp") if path in root_entries]
        for path in paths:
            resources = root_entries[path]["resources"]
            if resources["isDirectory"] is not True or resources["isSymbolicLink"] is not False:
                raise RuntimeError(f"Expected a regular iOS test container directory: {path}")

        # Replacing the data directories preserves developer trust across overwrites.
        with tempfile.TemporaryDirectory(prefix="pipixiv-ios-reset-") as temporary:
            for path in paths:
                empty = Path(temporary) / path
                empty.mkdir()
                self.container_command(
                    "copy", "to", "--source", str(empty),
                    "--destination", path, "--remove-existing-content", "true",
                )
                remaining = self.info_files(path, recurse=True)
                if path == "Library":
                    # iOS can recreate these two empty directories after replacing Library.
                    for name in ("Caches", "Preferences"):
                        resources = remaining.get(name, {}).get("resources", {})
                        if resources.get("isDirectory") is True and resources.get("isSymbolicLink") is False:
                            remaining.pop(name)
                if remaining:
                    raise RuntimeError(f"The iOS test container directory did not empty: {path}")
        print(f"Verified empty iOS test container data directories for {self.bundle_id}.", flush=True)

    def install(self, package):
        self.tool("install", "app", str(package), timeout=120)

    def launch(self):
        result = self.tool("process", "launch", "--terminate-existing", "--json-output", "-", self.bundle_id)
        record = json.loads(result.stdout)["result"]
        process = record.get("process", record)
        self.pid = process["processIdentifier"]

    def running_processes(self):
        result = self.tool("info", "processes", "--json-output", "-")
        return json.loads(result.stdout)["result"]["runningProcesses"]

    def is_running(self):
        return self.pid is not None and any(
            str(process["processIdentifier"]) == str(self.pid) for process in self.running_processes()
        )

    def stop(self):
        if self.pid is not None:
            result = self.tool("process", "terminate", "--pid", str(self.pid), check=False)
            if result.returncode and self.is_running():
                raise RuntimeError(f"Could not stop PiPixiv on iOS: {result.stderr.decode(errors='replace').strip()}")
            self.pid = None

    @staticmethod
    def missing_path(message):
        return any(fragment in message.lower() for fragment in ("no such file", "does not exist"))

    def wait_for_baseline(self):
        def ready():
            entries = self.info_files(self.files)
            return all(
                entries.get(Path(path).name, {}).get("resources", {}).get("isDirectory") is True
                for path in (self.databases, self.mmkv, self.log_dir)
            )

        wait_until_ready(self, ready)

    def prepare_fixture(self, fixture, debug_config, readonly_db, output):
        if readonly_db:
            raise ValueError("Read-only database injection is supported only on the Android emulator.")
        with (output / "container-paths.txt").open("wb") as evidence:
            for name in (self.databases, self.mmkv, self.log_dir):
                result = self.container_command("info", "files", "--subdirectory", name, "--no-recurse")
                evidence.write(f"{name}\n".encode())
                evidence.write(result.stdout)
                evidence.write(b"\n")
        with tempfile.TemporaryDirectory(prefix="pipixiv-migration-") as scratch:
            for source, target in fixture_payload(fixture, debug_config, Path(scratch), self.databases, self.mmkv, self.files):
                # devicectl treats the destination as the full path of the copied file.
                self.container_command("copy", "to", "--source", str(source), "--destination", target)
        manifest = json.loads((fixture / "manifest.json").read_text(encoding="utf-8"))
        database_bytes = manifest["files"]["pixiv_db"]["bytes"]
        entries = self.info_files(self.databases)
        sidecars = {"pixiv_db-wal": 0, "pixiv_db-shm": 0}
        for name, expected_size in {"pixiv_db": database_bytes, **sidecars}.items():
            entry = entries.get(name, {})
            resources = entry.get("resources", {})
            actual_size = entry.get("metadata", {}).get("size")
            if (
                resources.get("isDirectory") is not False
                or resources.get("isSymbolicLink") is not False
                or type(actual_size) is not int
                or actual_size != expected_size
            ):
                raise RuntimeError(f"Expected {name} to be a regular iOS container file of {expected_size} bytes.")
        write_json(output / "zero-sidecars.json", sidecars)

    def restore_permissions(self):
        pass

    def pull(self, source, destination, *, missing_ok=False):
        file = destination / Path(source).name
        result = self.container_command("copy", "from", "--source", source, "--destination", str(file), check=False)
        if result.returncode:
            message = (result.stderr + result.stdout).decode(errors="replace").strip()
            if missing_ok and self.missing_path(message):
                return None
            raise RuntimeError(f"Cannot copy {source} from the iOS app container: {message}")
        if not file.is_file():
            raise RuntimeError(f"devicectl did not copy {source} to {file}.")
        return file

    def logs(self):
        result = {}
        entries = self.info_files(self.log_dir)
        with tempfile.TemporaryDirectory(prefix="pipixiv-migration-") as temporary:
            for name in LOG_NAMES:
                if name in entries:
                    source = self.pull(f"{self.log_dir}/{name}", Path(temporary), missing_ok=True)
                    if source is not None:
                        result[name] = source.read_bytes()
        return result

    def database_sha256(self, name):
        with tempfile.TemporaryDirectory(prefix="pipixiv-migration-") as temporary:
            return sha256(self.pull(f"{self.databases}/{name}", Path(temporary)))


def make_target(args):
    if args.platform == "desktop":
        return DesktopTarget()
    if args.platform == "android":
        return AndroidTarget(args.device)
    return IOSTarget(args.device, args.bundle_id, args.baseline, args.feature)


def load_report_runs():
    jvm_path = JVM_REPORT_DIR / "result.json"
    if jvm_path.is_file():
        jvm_report = json.loads(jvm_path.read_text(encoding="utf-8"))
        jvm_scenarios = {
            "Normal": "normal",
            "LowStorage": "low-space",
            "V10Failure": "global-database",
        }
        for tier in jvm_report["tiers"]:
            for run in tier["runs"]:
                yield {
                    "platform": "JVM",
                    "device": run["timing"]["platform"],
                    "simulator": False,
                    "tier": tier["tier"],
                    "scenario": jvm_scenarios[tier["scenario"]],
                    "migration": run["timing"],
                    "completedStartup": {
                        "outcome": "alreadyComplete",
                        "totalMs": run["completedStartupMs"],
                        "steps": run["completedSteps"],
                    },
                    "extraMemoryBytes": run["extraHeapBytes"],
                    # The benchmark asserts migration and MMKV correctness before writing its report.
                    "mmkvMatches": True,
                    "verification": {"passed": True},
                    "historyJsonBytes": (tier["averageIllustJsonBytes"], tier["averageNovelJsonBytes"]),
                    "source": jvm_path,
                }
    for path in sorted(RESULTS_DIR.rglob("result.json")):
        run = json.loads(path.read_text(encoding="utf-8"))
        run["source"] = path
        yield run


def report_number(value):
    return f"{value:.3f}".rstrip("0").rstrip(".")


def report_median_max(values):
    return f"{report_number(median(values))} / {report_number(max(values))}" if values else "—"


def report_mib(values):
    return f"{max(values) / 1024**2:.2f}" if values else "—"


def report_table(headers, rows):
    yield "| " + " | ".join(headers) + " |"
    yield "| " + " | ".join("---" for _ in headers) + " |"
    for row in rows:
        cells = (str(cell).replace("|", "&#124;").replace("\n", " ") for cell in row)
        yield "| " + " | ".join(cells) + " |"


def report_performance(platform, simulator, tier, scenario, count, totals, completed, memory):
    if simulator:
        return "模拟器，仅记录"
    if tier in IOS_TIERS:
        tier = tier.removesuffix("-ios")
    if tier not in BASE_TIERS or scenario == "storage-unavailable":
        return "故障验证"
    if platform in {"desktop", "JVM"}:
        time_limit = {"typical": 300, "heavy": 2000, "extreme": 8000}[tier]
        startup_limit = 5
        memory_limit = 64
    else:
        time_limit = {"typical": 1000, "heavy": 5000, "extreme": 15000}[tier]
        startup_limit = 20
        memory_limit = 128
    limits = f"{time_limit} / {startup_limit} ms / {memory_limit} MiB"
    checks = ((totals, time_limit), (completed, startup_limit), (memory, memory_limit * 1024**2))
    if any(values and max(values) > limit for values, limit in checks):
        return f"失败（{limits}）"
    if any(len(values) != count for values, _ in checks):
        return f"未验证（{limits}）"
    return f"通过（{limits}）"


def report_group_summary(key, runs):
    platform, device, simulator, tier, scenario = key
    migrations = [run.get("migration") or {} for run in runs]
    totals = [timing["totalMs"] for timing in migrations if "totalMs" in timing]
    completed = [
        run["completedStartup"]["totalMs"]
        for run in runs
        if (run.get("completedStartup") or {}).get("outcome") == "alreadyComplete"
    ]
    extra_memory = []
    for run, timing in zip(runs, migrations):
        if "extraMemoryBytes" in run:
            extra_memory.append(run["extraMemoryBytes"])
        elif timing.get("memoryPeakBytes") is not None and timing.get("memoryStartBytes") is not None:
            extra_memory.append(timing["memoryPeakBytes"] - timing["memoryStartBytes"])
    memory_peaks = [
        timing["memoryPeakBytes"] for timing in migrations if timing.get("memoryPeakBytes") is not None
    ]
    disk_peaks = [timing["peakDiskBytes"] for timing in migrations if timing.get("peakDiskBytes") is not None]
    slow = [timing["slowUiShown"] for timing in migrations if "slowUiShown" in timing]
    mmkv = [run.get("mmkvMatches") for run in runs]
    checks = [(run.get("verification") or {}).get("passed") for run in runs]
    migration_verdict = "未验证"
    if False in checks or False in mmkv:
        migration_verdict = "失败"
    elif all(value is True for value in checks):
        migration_verdict = "通过"
    outcomes = " / ".join(dict.fromkeys(timing.get("outcome", "未取得日志") for timing in migrations))
    devices = sorted({timing["deviceModel"] for timing in migrations if timing.get("deviceModel")})
    hosts = sorted({timing["osVersion"] for timing in migrations if timing.get("osVersion")})
    versions = sorted({timing["appVersion"] for timing in migrations if timing.get("appVersion")})
    return (
        platform, device, ", ".join(devices) or "—", ", ".join(hosts) or "—",
        ", ".join(versions) or "—", tier, scenario, len(runs),
        report_median_max(totals), report_median_max(completed),
        f"{report_mib(memory_peaks)} / {report_mib(extra_memory)}", report_mib(disk_peaks),
        f"{sum(slow)}/{len(slow)}" if slow else "—",
        f"{mmkv.count(True)} 通过 / {mmkv.count(False)} 失败 / {mmkv.count(None)} 未验证",
        outcomes, migration_verdict,
        report_performance(platform, simulator, tier, scenario, len(runs), totals, completed, extra_memory),
    )


def report_step_rows(groups):
    for key, runs in groups:
        platform, device, simulator, tier, scenario = key
        label = f"{platform} / {device}" + ("（模拟器）" if simulator else "")
        steps = defaultdict(list)
        for run in runs:
            for phase, timing_key in (("迁移", "migration"), ("再次启动", "completedStartup")):
                timing = run.get(timing_key) or {}
                if timing_key == "completedStartup" and timing.get("outcome") != "alreadyComplete":
                    continue
                for step in timing.get("steps", []):
                    steps[(phase, step["name"])].append(step)
        for (phase, name), measurements in steps.items():
            times = [step["elapsedMs"] for step in measurements]
            row_counts = [step.get("rows", 0) for step in measurements]
            rows = str(min(row_counts))
            if min(row_counts) != max(row_counts):
                rows += f"–{max(row_counts)}"
            yield label, tier, scenario, phase, name, len(times), report_number(median(times)), max(times), rows


def report_source_path(source):
    return os.path.relpath(source, SUMMARY_PATH.parent).replace(os.sep, "/").replace(" ", "%20")


def run_report(_args):
    groups = defaultdict(list)
    for run in load_report_runs():
        key = run["platform"], run["device"], run["simulator"], run["tier"], run.get("scenario", "normal")
        groups[key].append(run)

    def group_order(group):
        (platform, device, simulator, tier, scenario), _ = group
        return platform, device, simulator, TIERS.index(tier), scenario != "normal", scenario

    groups = sorted(groups.items(), key=group_order)
    lines = [
        "# 跨平台迁移基准汇总",
        "",
        "耗时按各档的中位数和最大值汇总，以单轮最大值核对第 3.7 节阈值。"
        "额外内存为本轮迁移较起始值的增量，JVM 使用测试进程另采样的堆增量。",
        "",
    ]
    if any(key[3] in IOS_TIERS for key, _ in groups):
        lines.extend(("`-ios` 档位使用 iOS 专用数据包，不含字符串集合。", ""))
    headers = (
        "平台", "设备标识", "机型", "系统版本", "应用版本", "档位", "场景", "轮次",
        "迁移中位 / 最大 ms", "再次启动中位 / 最大 ms", "峰值 / 额外内存 MiB",
        "峰值磁盘 MiB", "慢提示（出现 / 观测）", "MMKV", "迁移结果",
        "迁移校验", "性能阈值",
    )
    for simulator, title in ((False, "JVM、桌面与真机"), (True, "模拟器")):
        lines.extend((f"## {title}", ""))
        rows = [report_group_summary(key, runs) for key, runs in groups if key[2] == simulator]
        lines.extend(report_table(headers, rows) if rows else ("未找到测量结果。",))
        lines.append("")
    if groups:
        lines.extend(("## 各步骤耗时", ""))
        lines.extend(report_table(
            ("平台 / 设备", "档位", "场景", "阶段", "步骤",
             "样本数", "中位 ms", "最大 ms", "行数"),
            report_step_rows(groups),
        ))
        lines.extend(("", "## 原始结果", ""))
        sources = list(dict.fromkeys(run["source"] for _, runs in groups for run in runs))
        for source in sources:
            lines.append(f"- [{source.relative_to(ROOT).as_posix()}]({report_source_path(source)})")
        sizes = {
            key[3]: runs[0]["historyJsonBytes"]
            for key, runs in groups
            if key[0] == "JVM"
        }
        if sizes:
            lines.extend(("", "## 样本平均 JSON 大小", ""))
            lines.extend(report_table(
                ("档位", "插画字节", "小说字节"),
                ((tier, *average) for tier, average in sizes.items()),
            ))
    SUMMARY_PATH.parent.mkdir(parents=True, exist_ok=True)
    SUMMARY_PATH.write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    print(SUMMARY_PATH)


if __name__ == "__main__":
    raise SystemExit(main())
