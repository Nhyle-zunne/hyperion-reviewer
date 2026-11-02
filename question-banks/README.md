# Hyperion question-bank snapshot

This directory contains 69 existing encrypted `.csv.enc` objects copied byte-for-byte from the PWA's active Supabase `question-banks` bucket on November 3, 2026 (Asia/Manila). `manifest.json` records each path, byte count and SHA-256 checksum for integrity and version checks.

The files retain the original track folders, competence/part filenames and encrypted contents. The transfer does not reorder questions, regenerate question IDs, replace Android app assets, or migrate bookmarks, mistakes, sessions, licenses or user accounts. No decryption key or privileged credential is included in this directory.

This is a source snapshot. Adding these files to GitHub does not by itself change the PWA's active delivery endpoint or remove Supabase download traffic. The existing Supabase objects remain in place. Switching delivery requires a separately verified implementation that preserves license checks, offline access, question ID generation, and compatibility with saved study data.

Treat the manifest hashes as the content versions when preparing a future delivery change. Do not rename files or edit/reorder the underlying CSV rows during a storage migration: existing question IDs depend on their competence/part mapping and row position.
