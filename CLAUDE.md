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

## Tooling — use everything available

Full details in **`AGENTS.md`** § **AI IDE tooling**. The short version:

- **Web search first** — don't guess Minecraft/Fabric APIs, Gradle versions, or library signatures. Use **WebSearch**, **Exa MCP** (`web_search_exa`), or **Context7** (`resolve-library-id` → `query-docs`) to get current docs.
- **MCP servers** are live: **Context7** (library docs), **Exa** (neural web search), **GitHub** (PRs/issues/code search), **Memory** (persistent knowledge graph), **Playwright** (browser testing), **Sequential Thinking** (structured reasoning). Read their tool schemas before calling.
- **Subagents** (Cursor Task tool): use `explore` for codebase search, `build-error-resolver` for Gradle failures, `java-reviewer` / `code-reviewer` for code quality, `security-reviewer` for wallet/config/permission changes.
- Prefer **Grep/Glob/SemanticSearch** over shell search commands; **Read/Write/StrReplace** over shell file ops.

> If unsure about an API or behavior — **search first**, don't assume.

## Build

`gradlew.bat build` · `gradlew.bat test`
