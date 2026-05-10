<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# STYLE GUIDE

## Project Conventions
- Prioritize UX and operational stability over abstract elegance.
- Keep implementations simple, reversible, and testable.
- Use fail-fast boundaries for I/O and async operations.

## Trace Tag Convention
- Format: `// [TRACE: <doc-file>.md]`
- Purpose: tie code to design intent and implementation notes.
- Use on non-obvious logic paths only.

## Comment Rules
- Comments explain "why", not "what".
- Use prefixes consistently: `TODO:`, `FIXME:`, `NOTE:`.
- Avoid noisy comments for self-evident code.

## Naming and Structure
- Java classes: `PascalCase`
- Java methods/fields: `camelCase`
- Keep methods focused and compact; split complex branches early.
- Keep docs and config names explicit and discoverable.

## Error Handling
- Never swallow exceptions silently.
- Log actionable context for operator-visible failures.
- Preserve state integrity first; reject invalid operations early.

## Website parity (`index.html`)

[2026-05-10]: Repo root **`index.html`** is the canonical **offline / browser-facing** Otters Civ. reference (commands, configs, roadmap, bundled tags). It must stay aligned with **`README.md`**, **`DOCS/modrinth-description.md`**, and the actual code/config surface.

[AMENDED 2026-05-11]: **Words we use** glossaries — the README subsection and **`index.html`** `#words-we-use` region (linked from the sidebar as `#words-we-use-h`) must stay aligned when player-facing terminology shifts (Otters Civ. vs **Project OOGA**, `fpsmod`, world host / “server”, tags vs `blockRewards` / `entityRewards`).

### When to update `index.html` in the same PR / chat turn

Finish an `index.html` pass before you consider civ-facing work **done**, if any of these changed:

- Economy or rewards: **`RewardRules`**, **`RewardRulesLoader`**, **`RewardOrchestrator`**, **`OttersCivGameplay`**, **`WalletService`** / **`FileWalletStore`**, **`MoneyCommand`** / **`OtterCommand`**, **`JoinWelcome`** (or any new server chat/UX hooked on join/play).
- Shipped datapack tags under **`src/main/resources/data/otters_civ_revived/`**.
- Versions or dependencies in **`gradle.properties`** / **`fabric.mod.json`** that appear in README or docs tables.
- **README** civ sections, **`DOCS/modrinth-description.md`**, or other player-facing bullets that **`index.html`** mirrors.

Do **not** strip existing explanatory copy unless the user explicitly asks; **extend and correct**.

### Sidebar / structure hygiene

New major **`h2`** / **`id=`** anchors should gain a **`nav.side-nav`** link (nested where it matches the article **Contents**) so scrolling discoverability stays consistent.

### Housekeeping paired with edits

Touch **`DOCS/CHANGELOG.md`** `[Unreleased]` when behavior or schema is user-visible; **`DOCS/SCRATCHPAD.md`** active tasks / last actions for multi-step work; **`DOCS/SUMMARY.md`** **[AMENDED …]** blocks when snapshot-level status shifts.

When **agent workflow** or **git hygiene** policy changes, update **`AGENTS.md`** and **`CLAUDE.md`** together and note the change in **`DOCS/CHANGELOG.md`**.

