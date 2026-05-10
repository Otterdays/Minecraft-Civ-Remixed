<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- **`AGENTS.md`** (Cursor / universal agent handbook), **`CLAUDE.md`** (Claude Code shim). Git hygiene: `.gitattributes` (`*.html`, `*.mdc` LF); `.gitignore` (`.env*`, `.claude/cache/`). Cross-links in **SUMMARY**, **LOCATIONS** workflow, README contributors.

- **Join welcome**: on `ServerPlayConnectionEvents.JOIN`, players get three system-chat lines branding Otters Civ. Revived and pointing to `/otter`, `/money`, and passive rewards (`JoinWelcome`).

- **`index.html`**: expanded offline/project reference — requirements stack, bundled default `currency_blocks` / `currency_mobs` tag contents, full `rewards.json` defaults table, wallet/HUD config notes, M0–M6 roadmap snapshot, links (issues, whitepaper, architecture). [2026-05-10]: sticky **On this page** sidebar with nested section links, responsive collapse, skip link, `aria-current` highlighting.

- **`/otter`** command: in-game summary of fpsmod/Otters Civ. commands (`OtterCommand`); passive rewards pointers.
- **Otters Civ. Revived** — gameplay money for mining and combat: break blocks in `otters_civ_revived:currency_blocks`, kill mobs in `#minecraft:hostile` (default); `WalletService.addBalance`; `RewardOrchestrator` + `JobsHooks` no-op stub; config `config/otters_civ_revived/rewards.json` (created on first run); Fabric `PlayerBlockBreakEvents.AFTER` + `ServerLivingEntityEvents.AFTER_DEATH` (direct melee-style player damage source only — no bow/trident credit in v1). Legacy **fpsmod** mod id and FPS HUD unchanged.
- Added **Imagery Optimizery** utility under `images/optimize-here/`: Pillow batch pipeline + optional tkinter GUI (`python optimize_pngs.py --gui`, or `optimize-pngs.bat`); shared logic with CLI (`optimize_pngs.py` without `--gui`). [2026-05-08]: GUI file list + per-row size estimates (in-memory), optional explicit files vs whole input folder.
- Added Project OOGA whitepaper defining civ-platform vision and delivery roadmap.
- Added DOCS core set: `SUMMARY`, `SCRATCHPAD`, `SBOM`, `STYLE_GUIDE`, `ARCHITECTURE`, `CHANGELOG`, and `My_Thoughts`.
- Added economy bootstrap internals: `WalletStore`, `FileWalletStore`, and `WalletService`.
- Added `/money` and `/money set <player> <amount>` command paths.

### Fixed
- **`DOCS/modrinth-description.md`** restored at canonical path (`STYLE_GUIDE`, `SUMMARY`, `index.html` reference); preservation header added. Parallel copy under `DOCS/Ryan-Made-Docs/` may remain for author drafts.

### Changed
- **README** + **`index.html`**: naming table (player name vs codename **Project OOGA** vs `fpsmod`); “commands” clarified as normal **slash chat** commands (single-player + multiplayer); “server” explained in plain language; features heading **World side**; **fabric.mod.json** description calls out codename.

- **README**: GitHub-ready layout (shields badges, Highlights table, condensed install, Commands table, Documentation index pointing at `DOCS/`, roadmap/contributing stubs); deep technical/agent workflow delegated to **`AGENTS.md`** / **`DOCS/`**.

- **Contributor rules:** **`DOCS/STYLE_GUIDE.md`** § Website parity (`index.html`); **Cursor** `.cursor/rules/index-html-parity.mdc` (`alwaysApply`) + README contributor note + **SUMMARY** quick link pointing at parity checklist.

- **Docs + site:** README passive-rewards + **`index.html`** row; **SUMMARY**, **LOCATIONS** (Otters Civ file map + jar path), **FEATURES** (environment / HUD vs server amend), **ARCHITECTURE** (“Runtime today” bootstrap); `index.html` intro, infobox, commands join blurb, references, contents label for rewards.

- **`rewards.json`**: optional **`blockRewards`** / **`entityRewards`** JSON objects map block or entity-type ids to payout amounts (validated resource locations; wins over `blockTag` / `entityTag` flat `blockReward` / `entityReward` when the id is listed). `RewardOrchestrator` + `RewardRulesLoader` (+ unit test).

- **`LICENSE`**: clarified ARR with carve-outs for gameplay/video/montage use and including the **official unmodified JAR** in mod packs; source reproduction and custom fork redistribution remain reserved without permission.
- Updated repository remote and metadata references to `Minecraft-Civ-Remixed`.
- Updated project naming/branding to Project OOGA.
- Moved location and readme image artifacts into docs/readme-friendly structure.
- Updated `fabric.mod.json` environment from `client` to `*` to allow server command registration.
