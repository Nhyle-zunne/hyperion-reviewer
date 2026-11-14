#!/usr/bin/env python3
"""Reject plaintext question banks and verify encrypted assets before Android releases.

Does not decrypt, export or modify questions. Runs on repository and optional
APK/AAB. Keep this check independent of Gradle so it runs early on CI.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
from zipfile import BadZipFile, ZipFile

ROOT = Path(__file__).resolve().parents[2]
ASSET_ROOT = Path("app/src/main/assets/questions")
MEDIA_ROOT = Path("app/src/main/assets/question-media")
ENCRYPTED_MEDIA = re.compile(r"^(?:DECK_OIC_NW|GMDSS_GOC|ENGINE_OIC_EW|DECK_MGT|ENGINE_MGT)/[a-f0-9]{64}\.(?:webp|png|jpg)\.enc$")
ALLOWED_METADATA = {"hyperion-manifest.json", "hyperion-release.json"}
LOGICAL_BANK = re.compile(r"^(?:deck|deck_mgt|engine_ew|engine_mgt|gmdss)/[a-z0-9_]+\.csv\.enc$")
ENCRYPTED_BANK = re.compile(r"^(?:deck|deck_mgt|engine_ew|engine_mgt|pro)/[a-z0-9_]+\.csv\.enc$")


def verify(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def logical_to_asset_path(logical: str) -> str:
    verify(bool(LOGICAL_BANK.fullmatch(logical)), f"Unsupported manifest question path: {logical}")
    if logical.startswith("gmdss/"):
        return "pro/" + logical.split("/", 1)[1]
    return logical


def verify_tracked_csv_policy(files: set[str], root: Path = ROOT) -> None:
    """Reject plaintext banks; allow only hash-pinned, non-runtime UI design tables."""
    plaintext = sorted(path for path in files if path.lower().endswith(".csv"))
    if not plaintext:
        return
    manifest_path = root / ".agents/upstream-lock.json"
    verify(manifest_path.is_file(), "Plaintext CSV files require a pinned UI/UX catalog manifest.")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    sources = [source for source in manifest.get("sources", [])
               if source.get("name") == "ui-ux-pro-max"]
    verify(len(sources) == 1, "UI/UX catalog provenance must be unique.")
    entries = sources[0].get("files", [])
    pins = {entry.get("destination"): entry.get("sha256") for entry in entries
            if isinstance(entry, dict) and str(entry.get("destination", "")).endswith(".csv")}
    for name in plaintext:
        verify(name.startswith(".agents/skills/ui-ux-pro-max/data/") and name in pins,
               f"Unencrypted .csv outside a verified design catalog: {name}")
        expected = pins[name]
        verify(isinstance(expected, str) and re.fullmatch(r"[a-f0-9]{64}", expected) is not None,
               f"Invalid catalog checksum: {name}")
        verify(hashlib.sha256((root / name).read_bytes()).hexdigest() == expected,
               f"Modified or untrusted UI/UX catalog: {name}")


def verify_tracked_files() -> set[str]:
    result = subprocess.run(["git", "ls-files", "--cached", "-z"],
                            cwd=ROOT, capture_output=True, check=True)
    files = {raw.decode("utf-8") for raw in result.stdout.split(b"\0") if raw}
    verify(not any(path.startswith("tmp/") for path in files),
           "Temporary question generation files must not be tracked: remove tmp/")
    verify_tracked_csv_policy(files)
    for path in files:
        if path.startswith(MEDIA_ROOT.as_posix() + "/"):
            relative = path[len(MEDIA_ROOT.as_posix()) + 1:]
            verify(bool(ENCRYPTED_MEDIA.fullmatch(relative)),
                   f"Unexpected or plaintext diagram asset: {path}")
        if not path.startswith(ASSET_ROOT.as_posix() + "/"):
            continue
        relative = path[len(ASSET_ROOT.as_posix()) + 1:]
        verify(bool(ENCRYPTED_BANK.fullmatch(relative)) or relative in ALLOWED_METADATA,
               f"Unexpected item inside packaged question assets: {path}")
    return files


def expected_bank_hashes(tracked: set[str]) -> dict[str, str]:
    root = ROOT / ASSET_ROOT
    manifest_path = root / "hyperion-manifest.json"
    verify(manifest_path.is_file(), "Missing verified encrypted question manifest.")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    verify(manifest.get("version") == 1 and manifest.get("encrypted") is True,
           "Question manifest must describe version 1 encrypted assets.")
    entries = manifest.get("files")
    verify(isinstance(entries, list) and len(entries) > 0,
           "Encrypted-bank manifest cannot be empty.")
    expected: dict[str, str] = {}
    for entry in entries:
        verify(isinstance(entry, dict), "Invalid question manifest entry.")
        logical = entry.get("path")
        verify(isinstance(logical, str), "Question path must be a string.")
        relative = logical_to_asset_path(logical)
        verify(relative not in expected, f"Duplicate encrypted question bank: {relative}")
        recorded_bytes, recorded_hash = entry.get("bytes"), entry.get("sha256")
        verify(isinstance(recorded_bytes, int) and recorded_bytes >= 28,
               f"Invalid encrypted size: {relative}")
        verify(isinstance(recorded_hash, str) and re.fullmatch(r"[a-f0-9]{64}", recorded_hash) is not None,
               f"Invalid checksum: {relative}")
        file_path = root / relative
        verify(file_path.is_file(), f"Missing encrypted question bank: {relative}")
        verify(file_path.stat().st_size == recorded_bytes,
               f"Encrypted file size differs from manifest: {relative}")
        verify(hashlib.sha256(file_path.read_bytes()).hexdigest() == recorded_hash,
               f"Encrypted file digest differs from manifest: {relative}")
        expected[relative] = recorded_hash
    prefix = ASSET_ROOT.as_posix() + "/"
    actual = {path[len(prefix):] for path in tracked
              if path.startswith(prefix) and path.lower().endswith(".csv.enc")}
    verify(actual == set(expected),
           f"Encrypted asset files must exactly match the verified manifest "
           f"(missing={sorted(set(expected) - actual)}, unexpected={sorted(actual - set(expected))}).")
    return expected


def expected_media_hashes(tracked: set[str]) -> dict[str, str]:
    # Decrypt only in memory to verify every reference has authenticated image bytes.
    result = subprocess.run(["node", "scripts/bundle-question-media.mjs", "--verify"],
                            cwd=ROOT, capture_output=True, text=True)
    verify(result.returncode == 0, result.stderr.strip() or "Bundled diagram verification failed.")
    prefix = MEDIA_ROOT.as_posix() + "/"
    expected = {}
    for path in tracked:
        if not path.startswith(prefix):
            continue
        file_path = ROOT / path
        verify(28 < file_path.stat().st_size <= 524288 + 28,
               f"Invalid encrypted diagram size: {path}")
        expected[path[len(prefix):]] = hashlib.sha256(file_path.read_bytes()).hexdigest()
    actual = {file.relative_to(ROOT / MEDIA_ROOT).as_posix()
              for file in (ROOT / MEDIA_ROOT).rglob("*") if file.is_file()}
    verify(actual == set(expected), "All bundled diagrams must be tracked in source.")
    return expected


def verify_artifact(artifact: Path, expected: dict[str, str], media: dict[str, str]) -> None:
    verify(artifact.is_file(), f"Android artifact not found: {artifact}")
    try:
        with ZipFile(artifact) as archive:
            names = archive.namelist()
            verify(not any(name.lower().endswith(".csv") for name in names),
                   "Android APK/AAB contains an unencrypted CSV!")
            packaged: dict[str, str] = {}
            packaged_media: dict[str, str] = {}
            for name in names:
                if not name.endswith("/") and "assets/question-media/" in name:
                    relative = name.split("assets/question-media/", 1)[1]
                    verify(bool(ENCRYPTED_MEDIA.fullmatch(relative)),
                           f"Unexpected or plaintext packaged diagram: {name}")
                    verify(relative not in packaged_media, f"Duplicate packaged diagram: {relative}")
                    packaged_media[relative] = hashlib.sha256(archive.read(name)).hexdigest()
                if name.endswith("/") or "assets/questions/" not in name:
                    continue
                relative = name.split("assets/questions/", 1)[1]
                verify(bool(ENCRYPTED_BANK.fullmatch(relative)) or relative in ALLOWED_METADATA,
                       f"Unexpected packaged question asset: {name}")
                if relative.endswith(".csv.enc"):
                    verify(relative not in packaged,
                           f"Duplicate packaged encrypted bank: {relative}")
                    packaged[relative] = hashlib.sha256(archive.read(name)).hexdigest()
            verify(packaged == expected,
                   "Android artifact question banks differ from verified encrypted manifest. "
                   "Ensure all banks are encrypted and included exactly once.")
            verify(packaged_media == media,
                   "Android artifact diagrams differ from the verified encrypted source assets.")
    except BadZipFile as exc:
        raise ValueError("The Android artifact is not a valid ZIP-based APK/AAB.") from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, help="Optional APK/AAB to inspect for raw CSVs and asset integrity")
    args = parser.parse_args()
    try:
        tracked = verify_tracked_files()
        expected = expected_bank_hashes(tracked)
        media = expected_media_hashes(tracked)
        if args.artifact:
            verify_artifact(args.artifact, expected, media)
    except (OSError, ValueError, subprocess.CalledProcessError, json.JSONDecodeError) as exc:
        print(f"QUESTION-BANK SECURITY CHECK FAILED: {exc}", file=sys.stderr)
        return 1
    print(f"Question-bank security check passed: {len(expected)} encrypted banks, {len(media)} encrypted diagrams; "
          f"no tracked temporary/plaintext CSV banks."
          + (" APK/AAB contents also verified." if args.artifact else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
