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

[AMENDED 2026-05-10]:
- **Passive rewards tuning:** same `config/otters_civ_revived/rewards.json` supports optional **`blockRewards`** and **`entityRewards`** maps (validated block/entity ids → payouts; precedence over tag-wide `blockReward`/`entityReward` when an id is listed). Join UX: **`JoinWelcome`** broadcasts a few system-chat lines on player connect (`/otter`, `/money`, rewards pointer). Offline **reference site:** repo root **`index.html`** (sidebar TOC, configs, roadmap links).
- **Docs surface:** README + `DOCS/modrinth-description.md` + `DOCS/CHANGELOG`; website copy tracks those features where listed.

[AMENDED 2026-05-09]:
- Commands: **`/otter`** (mod command list / config pointers), **`/money`**, **`/money set`**. Licensing: **`LICENSE`** is ARR with explicit carve-outs (gameplay/video/montage; official unmodified JAR in mod packs); see file for boundaries.
- **Otters Civ. Revived** (player-facing civ layer; mod id remains `fpsmod`): server rewards for blocks in `otters_civ_revived:currency_blocks` and kills in `#minecraft:hostile` by default; settings in `config/otters_civ_revived/rewards.json`. `JobsHooks` stub for future professions. Ranged/indirect kills not credited in v1.

[AMENDED 2026-05-08]:
- **Imagery Optimizery** (desktop helper, not part of the Minecraft mod): `images/optimize-here/` — `optimize_pngs.py` (`--gui` or `optimize-pngs.bat`) batch-resizes and exports web-friendly PNG + JPEG; CLI mode runs without `--gui`. GUI styling is considered stable; change only on request.

[AMENDED 2026-05-07]:
- Execution roadmap published at `DOCS/ROADMAP.md`.
- Whitepaper remains strategic context; roadmap is the implementation driver.
