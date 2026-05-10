# Claude Code — project context

This file bootstraps **Claude Code** for **Minecraft-Civ-Remixed** (**Project OOGA** / **Otters Civ. Revived**, mod id `fpsmod`). The shared agent handbook lives in **`AGENTS.md`** — read it for full conventions; this file highlights **Claude** session defaults.

## Do first on a cold start

1. **`AGENTS.md`** (canonical checklist).
2. **`DOCS/SUMMARY.md`** → **`DOCS/SCRATCHPAD.md`** → **`DOCS/STYLE_GUIDE.md`**.

## Non-negotiables

- **DOCS/`***: preserve-all-content rule in each file header; append / amend only.
- **`index.html`:** update in the same delivery when Otters Civ. behavior, **`rewards.json`** schema, bundled tags, version pins, README/Modrinth civ copy, join UX, or economy commands shift. Rules: **`DOCS/STYLE_GUIDE.md`** § Website parity + **`.cursor/rules/index-html-parity.mdc`**.
- **`DOCS/CHANGELOG.md`** `[Unreleased]` for user-visible or schema changes.

## Git hygiene (Concise)

- Commits: `type(scope): subject` (`feat`, `fix`, `docs`, `refactor`, `chore`, `test`).
- Branches: `feature/`, `fix/`, `docs/`, `chore/` as appropriate.
- Match **`.gitattributes`** line endings; never commit `.env` secrets or prism launcher configs from user machines into this repo accidentally.

## Build

`gradlew.bat build` · `gradlew.bat test`
