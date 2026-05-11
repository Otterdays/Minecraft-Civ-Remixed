<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SUMMARY

## Project Snapshot (2026-05-07)
- Codename: Project OOGA
- Repository: `https://github.com/Otterdays/Minecraft-Civ-Remixed`
- Current baseline: Fabric FPS template code still present
- Strategic direction: all-in-one civ mod suite (factions/jobs/professions/economy/player shops)

## Quick Links
- Agent handbooks: **`AGENTS.md`** (Cursor / universal), **`CLAUDE.md`** (Claude Code shim → full detail in **`AGENTS.md`**)
- Offline wiki-style reference (**update with civ/player-facing edits**): **`index.html`** (repo root); rule: `DOCS/STYLE_GUIDE.md` § Website parity
- Roadmap: `DOCS/ROADMAP.md`
- Whitepaper: `whitepaper.md`
- Scratchpad: `DOCS/SCRATCHPAD.md`
- Feature snapshot: `DOCS/FEATURES.md`
- Code map: `DOCS/LOCATIONS.md`
- Architecture: `DOCS/ARCHITECTURE.md`
- Style conventions: `DOCS/STYLE_GUIDE.md`
- Security bill of materials: `DOCS/SBOM.md`
- Changelog: `DOCS/CHANGELOG.md`

## Current Delivery Focus
1. Economy foundation and transaction integrity.
2. Jobs/professions progression loop.
3. Factions and land-claim control boundary.
4. Player-shop market loop and admin controls.

[AMENDED 2026-05-11]:
- **Commands:** **`/money`** for all chat users; **`/money set`** requires vanilla **gamemaster** (`PermissionLevel.GAMEMASTERS`). Roadmap **`Permissions apparatus (planned)`** describes future plugin-style nodes.
- **Wallet path:** **`config/otters_civ_revived/wallet.properties`** (economy grouped with **`rewards.json`**). **`config/fpsmod/hud.properties`** stays FPS-overlay-only; legacy **`config/fpsmod/wallet.properties`** auto-migrates on first wallet read. Optional **`# Name:`** plaintext above each **`uuid=balance`** for operators; refreshed on join, **`/money`**, **`/money set`**, and reward events.
- **Join UX:** first join per **world save** still gets three onboarding lines; **`JoinAttendanceSavedData`** (**`fpsmod:join_attendance`**, overworld **`SavedDataStorage`**) remembers returning UUIDs **for that save** so later joins get a concise **welcome back ~** _(display name)_ line (gold/aqua formatting) plus a shortened `/otter` / `/money` tip—not tied to wiping global **`config/`**.

[AMENDED 2026-05-10]:
- **Passive rewards tuning:** same `config/otters_civ_revived/rewards.json` supports optional **`blockRewards`** and **`entityRewards`** maps (validated block/entity ids → payouts; precedence over tag-wide `blockReward`/`entityReward` when an id is listed), plus sibling **`block_values.json`** / **`entity_values.json`** (whole-file per-id maps merged after startup parse; overlapping keys prefer the sibling files). Join UX: **`JoinWelcome`** broadcasts a few system-chat lines on player connect (`/otter`, `/money`, rewards pointer). Offline **reference site:** repo root **`index.html`** (sidebar TOC, configs, roadmap links—including **`#config-block-values`**, **`#config-entity-values`**).
- **Docs surface:** README + `DOCS/modrinth-description.md` + `DOCS/CHANGELOG`; repo root **`index.html`** tracks player-facing feature copy where listed (no separate `website/` mirror).

[AMENDED 2026-05-09]:
- Commands: **`/otter`** (mod command list / config pointers), **`/money`**, **`/money set`**. Licensing: **`LICENSE`** is ARR with explicit carve-outs (gameplay/video/montage; official unmodified JAR in mod packs); see file for boundaries.
- **Otters Civ. Revived** (player-facing civ layer; mod id remains `fpsmod`): server rewards for blocks in `otters_civ_revived:currency_blocks` and kills in `#minecraft:hostile` by default; settings in `config/otters_civ_revived/rewards.json`. `JobsHooks` stub for future professions. Ranged/indirect kills not credited in v1.

[AMENDED 2026-05-08]:
- **Imagery Optimizery** (desktop helper, not part of the Minecraft mod): `images/optimize-here/` — `optimize_pngs.py` (`--gui` or `optimize-pngs.bat`) batch-resizes and exports web-friendly PNG + JPEG; CLI mode runs without `--gui`. GUI styling is considered stable; change only on request.

[AMENDED 2026-05-07]:
- Execution roadmap published at `DOCS/ROADMAP.md`.
- Whitepaper remains strategic context; roadmap is the implementation driver.
