<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# Changelog

All notable changes to this project are documented here.

## [Unreleased]

### Added
- **`/otter` is now a stylized in-game menu (client-side).** Client-only command (`OtterClientCommand`, registered via `ClientCommandRegistrationCallback`) opens a custom `OttersCivScreen` instead of dumping chat text. Modern dark-navy panel with gold/aqua accents, animated fade-in, sidebar tabs (Home · Wallet · Rewards · Civ · Help), hover/glow states, and quick-action buttons that send `/money`, jump between tabs, or open config files (`rewards.json`, `block_values.json`, `entity_values.json`, `wallet.properties`) via the OS file manager (`Util.getPlatform().openFile`). Uses the new 26.1 `Screen` render-graph API (`extractRenderState` / `extractBackground` / `MouseButtonEvent`). Server-side `OtterCommand` chat-fallback remains for clients without the mod. Pure-geometry — no texture assets shipped.
- **`index.html` `#add-custom-payouts`** + README "Adding your own blocks or mobs" + **`/otter`** in-game help: three-tier walkthrough for operators (edit sibling value files / inline `blockRewards`–`entityRewards` in `rewards.json` / extend bundled `currency_blocks` · `currency_mobs` tags via server datapack at the singular `tags/block/` · `tags/entity_type/` paths). Precedence callout (sibling > inline > tag fallback) so it's obvious any per-id line beats tag-wide defaults. Sidebar TOC anchor added.

- **`config/otters_civ_revived/block_values.json`** and **`entity_values.json`**: whole-file registry id → payout maps. On **`ServerLifecycleEvents.SERVER_STARTED`** they are **filled from resolved tag memberships** (**`blockTag`** / **`entityTag`** with flat **`blockReward`** / **`entityReward`**) when the file had no keys, unions **`rewards.json`** inline maps plus any existing sibling overlays, persists sorted JSON, then replaces **`RewardOrchestrator`** rules (**`finalizeRewardsForRunningServer`**). Operators keep a turnkey editable spreadsheet-of-ids UX; **`/otter`**, **`index.html`**, README.

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
- **THE actual root cause — three stacked bugs (2026-05-11):** the runtime warning `registry holds 374 bound tags total` proved the registry was fully populated yet our specific tag ids returned zero. Investigation against the installed `minecraft-common-deobf-26.1.2.jar`:
  - **Resource paths were plural** (`data/otters_civ_revived/tags/blocks/`, `tags/entity_types/`) — Minecraft 1.21+ loads tags only from **singular** directories (`tags/block/`, `tags/entity_type/`). The mod's bundled `currency_blocks` / `currency_mobs` tags were silently dropped by `TagLoader`. Resource files moved to the singular paths so `TagLoader` picks them up.
  - **`minecraft:hostile` does not exist as an entity-type tag in vanilla 1.21+** (verified by enumerating `data/minecraft/tags/entity_type/` in the deobf jar; no such file). Default `RewardRules.entityTag` changed from `"minecraft:hostile"` → `"otters_civ_revived:currency_mobs"` (the mod's own bundled tag).
  - **`currency_mobs.json` previously referenced `#minecraft:hostile`** (recursive include of the same non-existent tag) so even after fixing the path it would have stayed empty. Replaced with an explicit list of every vanilla hostile mob (zombie/skeleton/creeper/spider/enderman/witch/slime/magma_cube/phantom/blaze/ghast/piglin family/wither/warden/illager family/guardian family/shulker/silverfish/endermite/breeze).
- **Tag expansion now uses `Registry.getTagOrEmpty(TagKey)` on `BuiltInRegistries`** instead of the level's `registryAccess()` `HolderLookup` view. Diagnosed from runtime logs: `level.registryAccess().lookupOrThrow(Registries.X).get(TagKey)` returned `Optional.empty()` for every tag — even vanilla `minecraft:hostile` — meaning the `RegistryAccess.Frozen` view doesn't carry tag bindings in this Minecraft version. `TagLoader` binds datapack tags onto the **static** `BuiltInRegistries` registry's tag map during resource reload, so the static `getTagOrEmpty` path is the reliable enumeration. Also registers an `END_DATA_PACK_RELOAD` handler in `FpsMod` so the prefill re-runs after `/reload`. Diagnostic now also logs the total bound-tag count when a lookup yields zero entries.
- **Tag expansion rewritten to use direct `HolderSet.Named` lookup** instead of iterating every registry entry and asking `holder.is(TagKey)`. Both block and entity prefill now call `level.registryAccess().lookupOrThrow(Registries.X).get(tagKey)` and iterate the returned `HolderSet.Named` members, then map each holder's value back to its identifier via `BuiltInRegistries.X::getKey`. This is the canonical way to enumerate tag members in modern Minecraft and reliably reflects datapack-bound tags at `SERVER_STARTED`. Adds explicit `INFO` log on success (entry count) and `WARN` on empty/missing tag — operators can confirm in `logs/latest.log` whether the prefill found members.
- **`block_values.json` / `entity_values.json` were persisting as empty `{}`** when the default `otters_civ_revived:currency_blocks` tag (and any other datapack-defined block tag) was used. Block tag expansion previously walked `BuiltInRegistries.BLOCK` and called `BlockState.is(TagKey)` — that path checks the static built-in holders, but in modern Minecraft datapack tag bindings live on the **server's live registry** layer. `RewardTagExpansion.payoutsForTaggedBlocks` now takes a `ServerLevel` and resolves membership via `level.registryAccess().lookupOrThrow(Registries.BLOCK)`, mirroring the entity path. Also: tag expansion no longer bails when the flat `blockReward`/`entityReward` is `0` — empty/zero payouts still get a prefilled id list so operators have something concrete to edit. Logs a warning when zero blocks/entities resolve for a tag id. End user-visible: editing `block_values.json` / `entity_values.json` now actually has content to customize.
- **`RewardRulesLoader.writeDefaults`**: append a trailing newline when generating the default `config/otters_civ_revived/rewards.json`, matching the sibling `block_values.json` / `entity_values.json` writer (`persistSortedLongMapJson`). Cosmetic POSIX-friendly fix; no behavior change.
- **Test coverage:** added direct unit tests for `RewardRulesLoader.composeEffectiveIdMap` (production merge path used by `finalizeRewardsForRunningServer`) covering tier1 → inline → sibling precedence, sibling-empty persistence with trailing newline, and the "do not overwrite existing sibling file" invariant. Closes the gap where only the test-only `mergeExternalValueFiles` helper was exercised.

- **`DOCS/modrinth-description.md`** restored at canonical path (`STYLE_GUIDE`, `SUMMARY`, `index.html` reference); preservation header added. Parallel copy under `DOCS/Ryan-Made-Docs/` may remain for author drafts.

### Changed
- **`index.html` `#limits`:** states join onboarding vs welcome-back is tracked **per world save** (`fpsmod:join_attendance` in overworld data, not global `config/`). **SUMMARY** docs-surface line: player-facing HTML is repo root **`index.html`** only (no **`website/`** mirror).

- **`JoinWelcome` messaging:** First visit on **this save** still gets three onboarding system lines. Returning players see a concise **welcome back `~`** + display-name line (**`ChatFormatting`** gold / aqua) and a shorter `/otter` / `/money` refresher (`JoinWelcome` + **`JoinAttendanceSavedData`**, **`fpsmod:join_attendance`** via overworld **`SavedDataStorage`**).

- **`/money set`** gated with **`Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)`** (gamemaster / vanilla cheat band); **`/money`** read remains open. Docs: **`OtterCommand`**, **`README`**, **`index.html`** (`#command-permissions`), **`DOCS/ROADMAP.md`** (**Permissions apparatus (planned)**).

- **`wallet.properties` operator hints:** optional UTF‑8 **`# Name: …`** lines above each **`uuid=balance`** (UUID line still authoritative); hints refresh on join, **`/money`**, **`/money set`**, mining, and kill payouts. **`WalletLedger`** + **`WalletStore.save(balances, displayHints)`**; **`JoinWelcome`** updates labels. **`FileWalletStore`** parses this layout and still loads legacy **`Properties`** files when no UUID lines parse.

- **Wallet persistence:** **`config/otters_civ_revived/wallet.properties`** (beside **`rewards.json`**); **`config/fpsmod/`** is FPS HUD only (**`hud.properties`**). On first **`FileWalletStore.load()`**, if the new file is missing and **`config/fpsmod/wallet.properties`** exists, **`Files.move`** migrates it and logs. **`/otter`** help updated.

- **README** + **`index.html`**: readability pass—**Words we use** glossary (player name, **Project OOGA**, `fpsmod`, world host / “server,” tags, per-id reward maps); short **lead** lines before dense sections; friendlier feature bullets and command `<dd>` text; `.glossary` / `.lead` styles on the site; sidebar/Contents anchor `#words-we-use-h` for scroll highlighting. **STYLE_GUIDE** § website parity + **`.cursor/rules/index-html-parity.mdc`** amended to require README/site glossary sync.

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
