# Hyperion development instructions

## Project and safety

This repository is `hyperion-reviewer`: the Kotlin/Jetpack Compose Android app in `app/`. Consult `app/README.md`, `app/build.gradle.kts`, and `.github/workflows/android-ci.yml` for current setup and checks. CI uses JDK 17, Gradle 8.14.4, and Android SDK 36. Use an existing compatible Gradle installation; this checkout does not contain a complete executable Gradle wrapper.

Preserve existing project instructions and task-specific requirements. Security, correctness, licensing enforcement, payment processing, accessibility, and user-data preservation take priority over shorter answers, smaller diffs, and skill preferences. Never remove validation, authorization, error handling, tests, or recovery guarantees merely to simplify code. Keep secrets out of source, logs, client bundles or APKs, and chat.

The encrypted question banks in `app/src/main/assets/questions` are also the private source used by `hyperion-pwa`. Preserve server-side entitlement checks, expiration/revocation enforcement, encrypted storage, account isolation, historical revisions, sessions, bookmarks, and mistakes. Free access must not expose paid content through delivery or caches. Check PWA compatibility before changing shared formats or identifiers. Preserve Google Play account binding, purchase verification, signed licenses, and offline entitlement expiry. Do not delete production records or backups or run real payments as tests.

Preserve the existing [Android setup guidance](app/README.md) and [account sign-in setup](ANDROID_ACCOUNT_SIGN_IN_SETUP.md). Do not commit OAuth client secrets, `google-services.json`, signing credentials, or new keystores. Production signing and internal-testing bundle generation require separately supplied credentials and authorization; a configuration change does not authorize a release.

## Repository skills

Repository-native skills live in `.agents/skills/<name>/SKILL.md`. Read the relevant skill before applying it. In Codex use `$skill-name` or explicitly name the skill and its file. See [the tooling guide](docs/codex-cloud-tools.md) for installation provenance, invocation, and runtime limitations.

- **Superpowers:** start with [using-superpowers](.agents/skills/using-superpowers/SKILL.md). Use brainstorming and writing-plans to establish scope and a reviewable plan; executing-plans for implementation; systematic-debugging for failures; test-driven-development for substantive behavior changes; verification-before-completion before success claims. Apply review and finishing skills when appropriate. Scale artifacts to the task and reuse an existing approved specification or plan. A complete user instruction already authorizes its stated reversible work; skill workflow preferences do not require repeating that approval.
- **Ponytail:** use [ponytail](.agents/skills/ponytail/SKILL.md) for the smallest complete, readable implementation. Reuse existing patterns and dependencies. Avoid speculative abstractions and clever one-liners. Simplification must preserve every safety and product guarantee above; do not use the optional ultra mode to drop requested work.
- **Caveman:** use [caveman](.agents/skills/caveman/SKILL.md) for concise communication. Lead with the result, keep essential evidence, exact commands, limitations, risks, and next actions. Use clear grammatical prose when compression could confuse. Keep persisted code, comments, documentation, and commit messages in normal technical prose. Report material risks or unverified checks once; no redundant closing boilerplate.

These roles complement each other: Superpowers controls process, Ponytail guides implementation simplicity, and Caveman guides chat style. Follow the user's requested detail level and mode changes. If a session also supplies the same skills through a plugin, use one copy for each workflow, not both. Upstream `superpowers:<name>` references refer to the corresponding repository skill when no plugin namespace is available.

Use only tools and permissions actually provided by the current Codex Cloud session. Optional subagent, model-routing, hook, visual-server, and machine-configuration instructions in upstream references are runtime-specific guidance, not requirements to install or enable them here. Follow the session's delegation and model-selection rules. If delegation is unavailable, execute and review inline and disclose that limitation. Do not install machine-wide plugins, lifecycle hooks, proxies, telemetry, or CLI middleware for this repository setup. Do not run bundled helper scripts without checking their relevance and prerequisites.

## Verification and delivery

For skill configuration, run `node scripts/tests/agent-skills-test.mjs`. For application changes, follow Android CI: `gradle --no-daemon :app:testDebugUnitTest`, `gradle --no-daemon :app:lintDebug`, and `gradle --no-daemon :app:assembleDebug` with the required JDK/SDK available. Changes to progress, recovery, billing, or licenses need the relevant account-isolation, synchronization, and entitlement regressions. Use test accounts rather than real payments or production entitlement changes. State precisely what passed and what was not tested. Do not claim cross-device or production acceptance from static tests alone.

Inspect diffs and preserve unrelated work. Use a focused branch for review. Do not trigger signed bundles, store publishing, or unrelated PWA deployment as part of repository tooling changes. Commit configuration when requested; do not infer permission for unrelated application changes or production data operations.

## Graphify code navigation (optional)

Use [the Graphify source-map guide](docs/graphify.md) before broad source searches when a current code-only graph is available. Query the local `graphify-out/graph.json` first to identify related files and dependencies, then verify findings in current source and tests. If the graph is absent and the CLI/network is available, generate it once using the guide's pinned `uvx` command. Do not block urgent work if Graphify cannot run. Never use a graph as proof of permissions, licensing, or production correctness. No cloud graph service or machine-wide hooks are enabled.

## UI/UX Pro Max — native repository skill

For interface, visual design, layout, accessibility, responsiveness, touch interaction, typography, and animation, read .agents/skills/ui-ux-pro-max/SKILL.md. Use its Python search script with react for Hyperion PWA, jetpack-compose for Android, and flutter for Hyperion Books. Preserve Hyperion branding and approved layouts. Do not use for purely backend changes.

The skill is pinned to the ui-ux-pro-max-cli@2.15.0 package, with local file hashes in .agents/upstream-lock.json. No app dependencies or automatic updates. From the repository root, use: python3 .agents/skills/ui-ux-pro-max/scripts/search.py "mobile navigation touch" --domain ux.
