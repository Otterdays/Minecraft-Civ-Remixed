# Repository instructions for coding agents

**Project:** repo **Minecraft Civ Remixed**. **Internal codename:** **Project OOGA**. **Players see:** **Otters Civ. Revived**. **Mod id:** `fpsmod`. Remote: Otterdays/Minecraft-Civ-Remixed.

## Before substantive work

1. Read **`DOCS/SUMMARY.md`** → **`DOCS/SBOM.md`** → **`DOCS/SCRATCHPAD.md`** → **`DOCS/STYLE_GUIDE.md`** (init order aligns with contributor workflow).
2. Use **`DOCS/LOCATIONS.md`** as the code map; **`README.md`** for run/build/version facts.
3. Respect **DOCS preservation:** every markdown under **`DOCS/`** must keep `<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->`; never delete substantive history — append or `[AMENDED YYYY-MM-DD]:` notes instead.

## Product scope reminder

Economy bootstrap is live: **`/money`**, **`/otter`**, wallet file, **`rewards.json`** (tags + optional **`blockRewards`** / **`entityRewards`**), **`JoinWelcome`**, datapack **`otters_civ_revived`**. Larger civ roadmap: **`DOCS/ROADMAP.md`**.

## Mandatory site parity

**`index.html`** (repo root) is the wiki-style offline reference.

- Cursor rule **`.cursor/rules/index-html-parity.mdc`** is **`alwaysApply: true`**.
- **`DOCS/STYLE_GUIDE.md`** § **Website parity (`index.html`)** defines the full checklist.
- Civ- / player-facing edits are **not done** until **`index.html`**, **`DOCS/CHANGELOG.md`** `[Unreleased]` (if user-visible), and **`DOCS/SCRATCHPAD.md`** checkpoints are addressed as described there.

## Code & docs conventions

- **Java:** idiomatic Minecraft/Fabric Fabric API; meaningful tests for config/business parsing where feasible (`gradle test`).
- **`index.html`:** no casual removal of user-facing prose; additive fixes unless asked.
- **`DOCS/CHANGELOG.md`:** Keep a Changelog-style `[Unreleased]` section current for notable changes.

## Build / verify

```bat
gradlew.bat build
gradlew.bat test
```

Output jar: **`BUILT/libs/project-ooga-*.jar`** (see `gradle.properties` `mod_version`).

## Git & branches

Use **Conventional Commits**:

`feat|fix|docs|refactor|chore|test(<scope>): <description>`

**Branches:** prefer `feature/`, `fix/`, `chore/`, `docs/` prefixes. Do not commit secrets; keep **`LICENSE`** / ARR carve-outs unchanged unless the author requests a legal pass.

Prefer **LF** endings for tracked text per **`.gitattributes`** (Gradle/Java/JSON/MD; Windows **`.bat`** CRLF).

## Cross-links

**Cursor-specific rules:** `.cursor/rules/`  
**Anthropic Claude project file:** **`CLAUDE.md`** (mirror + slim variant)

**Listing / store copy:** `DOCS/modrinth-description.md` (keep synced with **`index.html`** civ bullets per STYLE_GUIDE).
