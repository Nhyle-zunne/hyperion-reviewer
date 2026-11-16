"""Regression tests for the tracked plaintext CSV policy."""
from __future__ import annotations

import hashlib
import importlib.util
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest


MODULE_PATH = Path(__file__).with_name("question-bank-asset-safety.py")
SPEC = importlib.util.spec_from_file_location("question_bank_asset_safety", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
policy = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(policy)

CATALOG = ".agents/skills/ui-ux-pro-max/data/ux-guidelines.csv"


class TrackedCsvPolicyTest(unittest.TestCase):
    def setUp(self):
        self.temp = TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        data = b"Category,Issue\\nTouch,44px\\n"
        file = self.root / CATALOG
        file.parent.mkdir(parents=True)
        file.write_bytes(data)
        lock = self.root / ".agents/upstream-lock.json"
        lock.parent.mkdir(parents=True, exist_ok=True)
        lock.write_text(json.dumps({"sources": [{
            "name": "ui-ux-pro-max",
            "files": [{"destination": CATALOG, "sha256": hashlib.sha256(data).hexdigest()}],
        }]}), encoding="utf-8")

    def test_pinned_ui_design_data_is_permitted(self):
        policy.verify_tracked_csv_policy({CATALOG}, self.root)

    def test_plaintext_bank_is_still_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unencrypted .csv"):
            policy.verify_tracked_csv_policy({"app/src/main/assets/questions/c1.csv", CATALOG}, self.root)

    def test_unpinned_catalog_is_rejected(self):
        other = ".agents/skills/ui-ux-pro-max/data/new-unreviewed.csv"
        with self.assertRaisesRegex(ValueError, "Unencrypted .csv"):
            policy.verify_tracked_csv_policy({other}, self.root)

    def test_tampered_pinned_catalog_is_rejected(self):
        (self.root / CATALOG).write_bytes(b"changed")
        with self.assertRaisesRegex(ValueError, "Modified or untrusted"):
            policy.verify_tracked_csv_policy({CATALOG}, self.root)


if __name__ == "__main__":
    unittest.main()
