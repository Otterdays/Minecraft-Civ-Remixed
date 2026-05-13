<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SUMMARY

## Project Snapshot (2026-05-13)
- Codename: Project OOGA
- Repository: `https://github.com/Otterdays/Minecraft-Civ-Remixed`
- Mod ID: `project_ooga` (changed from `fpsmod` to avoid conflict with standalone FPS overlay mod)
- Entrypoint classes: `OogaMod` / `ProjectOogaClient` (renamed to avoid class name collision)
- Shipping: economy wallet, mining/combat rewards, 4 jobs, join welcome, crash-safe persistence, in-game `/otter` hub, jobs HUD overlay
- Legacy FPS HUD: **deprecated & disabled** (standalone FPS overlay mod handles display)
- Docs parity: `index.html` (repo root) is the canonical offline reference

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

[AMENDED 2026-05-12 — `/otter` covers full roadmap]:
- **In-game hub surfaces every milestone.** HOME paints the M0–M6 progress strip (PARTIAL/PARTIAL/SHIPPED/PLANNED/PLANNED/FUTURE/FUTURE). Tabs: HOME · WALLET · JOBS · REWARDS · CIV · HELP. WALLET/REWARDS/HELP enumerate shipped + planned commands as `[chip] /cmd · note` rows (chips: LIVE/PARTIAL/SOON/FUTURE). CIV stacks 4 milestone cards (M3 Factions & Claims, M4 Player Shops, M5 Governance, M6 Stabilization & Scale). Panel 480×268.

[AMENDED 2026-05-12 — jobs HUD]:
- **In-game job bar.** Compact bar above vanilla XP showing icon + slug + level + XP fill (gold gradient). Server pushes `JobStatusPayload` on join/job-change/reward. Client mirror + HUD overlay. Operator-tunable via `/otter` → Jobs tab (visible toggle, X/Y nudge, scale ±, reset, `/job` shortcuts). Persistence `config/fpsmod/jobs_hud.properties`. BMP-only icons (⛏▲✿⚔) for unifont compatibility.

[AMENDED 2026-05-12 — jobs MVP]:
- **Jobs/professions M2 first slice shipped.** Fixed-set: miner / lumberjack / farmer / fighter. One active slot. XP only on matching block-break / mob-kill events; level curve `100 * L^1.5` cap 50; payout multiplier `1.0 + L/50`. Commands `/job`, `/job list|join|leave|stats`. Persistence `config/otters_civ_revived/jobs.properties` (UUID.active=<slug>, UUID.xp.<slug>=N). Bundled tags `otters_civ_revived:job/{miner,lumberjack,farmer}_blocks` + `fighter_mobs` (singular `tags/block/` · `tags/entity_type/` dirs). New package `com.fpsmod.jobs`; `JobsHooks` interface gained `multiplyPayout` stage; `RewardOrchestrator` calls it pre-`addBalance`. Lessons + paths in `DOCS/LOCATIONS.md` + `DOCS/ARCHITECTURE.md`.

[AMENDED 2026-05-12]:
- **`currency_blocks` tag expanded to ~260+ blocks** (was ~23). Covers all breakable vanilla block categories: stone/brick variants, ores, dirt/sand/gravel/clay, logs/leaves/planks, wool, all 16-color sets (terracotta/glazed/concrete/concrete_powder/stained_glass), sandstone, nether set, end blocks, copper permutations, ore storage blocks, organics, sculk, utility blocks. Source: `src/main/resources/data/otters_civ_revived/tags/block/currency_blocks.json`. Uses `#minecraft:` tags for logs/leaves/planks/wool/dirt/sand/ice/coral_blocks; individual entries elsewhere. All default to flat `blockReward` (1) — operators tune in `block_values.json`.

[AMENDED 2026-05-11 — root cause]:
- **Three stacked bugs**, all proven by live log diagnostic ("registry holds 374 bound tags total" → tags load fine, ours just don't exist):
  1. Bundled tag dirs were **plural** (`tags/blocks/`, `tags/entity_types/`); MC 1.21+ loads only **singular** (`tags/block/`, `tags/entity_type/`). Moved files.
  2. `minecraft:hostile` is **not** a vanilla entity-type tag in 1.21+ (verified against deobf jar). Default `entityTag` changed to `otters_civ_revived:currency_mobs`.
  3. `currency_mobs.json` self-referenced `#minecraft:hostile`. Replaced with explicit hostile-mob id list (zombie/skeleton/creeper/spider/enderman/witch/slime/magma_cube/phantom/blaze/ghast/piglins/wither/warden/illagers/guardians/shulker/silverfish/endermite/breeze).

[AMENDED 2026-05-11]:
- **Reward prefill bug (live-confirmed) and fix:** `block_values.json` / `entity_values.json` were persisting as empty `{}` even though `rewards.json` was generating correctly. Two stacked bugs: (1) flat reward = 0 short-circuited expansion; (2) `level.registryAccess().lookupOrThrow(Registries.X).get(TagKey)` returns `Optional.empty()` for every tag — including vanilla `minecraft:hostile` — in MC 26.1.2 (internal 1.21.11). Fix: enumerate via `BuiltInRegistries.X.getTagOrEmpty(TagKey)` on the static registry (where `TagLoader` actually binds datapack tags); zero-reward tags still prefill ids at `0` so operators can edit; hydrate now wires both `SERVER_STARTED` and `END_DATA_PACK_RELOAD`; empty-tag warnings log the registry's bound-tag total. Operator UX restored: deleting empty sibling files (or running `/reload`) regenerates them with full id lists. Lessons stamped in `DOCS/LOCATIONS.md` and `DOCS/ARCHITECTURE.md`.

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
