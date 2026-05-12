# Repository instructions for coding agents

**Project:** repo **Minecraft Civ Remixed**. **Internal codename:** **Project OOGA**. **Players see:** **Otters Civ. Revived**. **Mod id:** `fpsmod`. Remote: Otterdays/Minecraft-Civ-Remixed.

## Before substantive work

1. Read **`DOCS/SUMMARY.md`** → **`DOCS/SBOM.md`** → **`DOCS/SCRATCHPAD.md`** → **`DOCS/STYLE_GUIDE.md`** (init order aligns with contributor workflow).
2. Use **`DOCS/LOCATIONS.md`** as the code map; **`README.md`** for run/build/version facts.
3. Respect **DOCS preservation:** every markdown under **`DOCS/`** must keep `<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->`; never delete substantive history — append or `[AMENDED YYYY-MM-DD]:` notes instead.

## Product scope reminder

Economy bootstrap is live: **`/money`**, **`/otter`**, wallet file, **`rewards.json`** (tags + optional **`blockRewards`** / **`entityRewards`**) plus optional sibling **`block_values.json`** / **`entity_values.json`**, **`JoinWelcome`**, datapack **`otters_civ_revived`**. Larger civ roadmap: **`DOCS/ROADMAP.md`**.

## Mandatory site parity

**`index.html`** (repo root) is the wiki-style offline reference. Do **not** maintain a second HTML copy under **`website/`** (or elsewhere); one file only.

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

## AI IDE tooling — use everything available

Agents in this workspace have access to powerful tools beyond basic file editing and terminal. **Use them proactively** — don't default to manual workarounds when a purpose-built tool exists.

### Web search

- **Always prefer web search** over guessing when you need Minecraft/Fabric API signatures, Gradle plugin versions, library docs, or anything version-sensitive.
- Use the built-in **WebSearch** / **WebFetch** tools (Cursor) or the **Exa MCP** (`web_search_exa`, `web_fetch_exa`) for neural/semantic web search.
- When checking Fabric API, Minecraft mappings, or mod-loader conventions, search first — don't assume from training data.

### MCP servers (Model Context Protocol)

The workspace has these MCP servers enabled — read their tool schemas before calling:

| Server | Key tools | When to use |
|---|---|---|
| **Context7** | `resolve-library-id`, `query-docs` | Fetch up-to-date library/framework docs instead of relying on training data. Use for Fabric API, Gradle, Minecraft modding references. |
| **Exa** | `web_search_exa`, `web_fetch_exa` | Neural web search + page fetch. Prefer for broad research, finding examples, checking compatibility. |
| **GitHub** | `search_code`, `list_issues`, `create_pull_request`, `list_pull_requests`, etc. | Searching upstream repos, managing PRs/issues on `Otterdays/Minecraft-Civ-Remixed`. |
| **Memory** | `create_relations`, `add_observations`, `search_nodes`, `read_graph` | Persistent knowledge graph across sessions. Store project decisions, hard-won lessons, cross-session context. |
| **Playwright** | `browser_navigate`, `browser_snapshot`, `browser_click`, etc. | Visual testing of `index.html`, verifying rendered output, scraping pages when web fetch isn't enough. |
| **Sequential Thinking** | `sequentialthinking` | Complex multi-step reasoning, architecture decisions, debugging chains. Use when a problem needs structured breakdown. |
| **Docker** | *(currently errored — check Cursor Settings if needed)* | Container operations when available. |

### Subagents / Task tool (Cursor)

Cursor provides specialized subagents via the **Task** tool. Use them for parallel or domain-specific work:

- **`explore`** — fast read-only codebase exploration (find files, search code, answer "where/how" questions).
- **`generalPurpose`** — multi-step research or implementation tasks.
- **`build-error-resolver`** — when `gradlew.bat build` fails, delegate to this specialist.
- **`java-reviewer`** / **`code-reviewer`** — use proactively when touching Java code.
- **`security-reviewer`** — after changes to config loading, wallet files, or command permissions.
- **`architect`** — system design decisions, feature planning.
- **`planner`** / **`dev-planner`** — breaking down complex features into steps.

Launch multiple subagents **in parallel** when tasks are independent (e.g., reviewing code in one while searching docs in another).

### Built-in tools to prefer

- **Grep** / **Glob** / **SemanticSearch** over shell `grep`/`find` commands.
- **Read** / **Write** / **StrReplace** over shell `cat`/`sed`/`echo`.
- **ReadLints** after editing Java files to catch problems early.
- **WebSearch** over assuming API details from memory.

### Key principle

> If you're unsure about an API, a version, a Minecraft behavior, or a Fabric convention — **search the web or query docs first**. Wrong assumptions cost more than a tool call.

## Cross-links

**Cursor-specific rules:** `.cursor/rules/` (incl. `use-all-tools.mdc`)  
**Anthropic Claude project file:** **`CLAUDE.md`** (mirror + slim variant)

**Listing / store copy:** `DOCS/modrinth-description.md` (keep synced with **`index.html`** civ bullets per STYLE_GUIDE).
