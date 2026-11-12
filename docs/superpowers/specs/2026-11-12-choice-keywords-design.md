# Choice keyword highlighting

Extend the existing private keyword editor to stem and answer text. One Edit Keyword / Save Keywords control owns the draft. Editing disables answer cards and confirmation, including an answer selected before editing. Saving resumes answering; failed persistence retains the draft and edit mode. Navigation or disabling highlighting discards unsaved changes.

Use shared Compose state passed to existing surfaces, rather than independent editors per choice. Keep legacy stem storage keys and word indices. Assign punctuation/symbol tokens after legacy word IDs; identify choices by a digest of exact choice text scoped to the stem, track and question. Shuffling preserves identity. No bank, entitlement, licensing or account changes.

Alternatives: independent choice editors would violate single-save behavior; moving drafts into the ViewModel would add unnecessary lifecycle and persistence scope. Shared question-local state fits existing UI lifetimes.
