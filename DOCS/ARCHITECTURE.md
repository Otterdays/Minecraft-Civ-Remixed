<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# ARCHITECTURE

## System Overview
Project OOGA evolves the current Fabric baseline into a modular civ platform with five primary
domains: economy, jobs/professions, factions, land claims, and player shops.

## High-Level Components
- Data Layer: persistent player/faction/claim/wallet/shop records + schema versioning.
- Domain Services: wallet, progression, faction governance, claim checks, market transactions.
- Event Layer: Fabric event listeners routed through validation and service boundaries.
- Interface Layer: commands + Screen Handler UX.
- Operator Layer: config profiles, audit logs, balancing controls.

## Data Model v1 (Planned)
- `player_profile`: wallet balance, profession/job state, faction reference.
- `faction_profile`: members, rank/roles, treasury, diplomacy.
- `claim_record`: chunk ownership + permission flags.
- `shop_record`: listings, stock, pricing, taxes, transaction references.
- `transaction_log`: immutable event-style economy audit entries.

## Runtime Flow (Simplified)
1. Player action emits game event.
2. Validation checks permissions/limits.
3. Domain service computes result.
4. Transaction/state changes persist atomically.
5. Player feedback is returned (UI/chat/action bar) with clear outcome.

## Reliability Goals
- No main-thread blocking I/O on hot gameplay paths.
- Deterministic transaction updates with rollback-safe boundaries.
- Explicit error paths for all state-mutating operations.

## Runtime Today (Bootstrap; 2026-05-10)

Shipped subset before the planned data layer above: **`WalletService`** + file-backed **`wallet.properties`**; **`RewardOrchestrator`** starts from **`RewardRulesLoader.loadBootstrapRewards()`** (**`rewards.json`**) then swaps in **`finalizeRewardsForRunningServer()`** output on **`ServerLifecycleEvents.SERVER_STARTED`**, which expands **`blockTag`/`entityTag`**, unions inline maps plus sibling overlays, persists sorted **`block_values.json`/`entity_values.json`** when sibling files lacked keys (`RewardRulesLoader` + **`RewardTagExpansion`**); Fabric events bridge block breaks and melee kills. Welcome copy on **`ServerPlayConnectionEvents.JOIN`** (**`JoinWelcome`**). Commands **`/otter`**, **`/money`**. Player-facing prose + config tables: **`index.html`** (repo root).

[AMENDED 2026-05-11]: **`wallet.properties`** path is **`config/otters_civ_revived/wallet.properties`** (alongside **`rewards.json`**). **`config/fpsmod/`** retains only grandfathered HUD settings (**`hud.properties`**). **`FileWalletStore`** migrates a legacy **`config/fpsmod/wallet.properties`** on first load if the new path is missing.

[AMENDED 2026-05-11]: Wallet file may include optional **`# Name:`** hint lines (**`WalletLedger`** / **`WalletService`**); **`uuid=balance`** remains authoritative for balances.

[AMENDED 2026-05-11]: **`JoinWelcome`** sends full onboarding on a player’s **first** join **per world save** and a shorter **welcome back ~** _(name)_ path afterward, backed by **`JoinAttendanceSavedData`** (**`fpsmod:join_attendance`**) in overworld **`SavedDataStorage`**.

[AMENDED 2026-05-12]: **`currency_blocks` tag** expanded from ~23 entries to ~260+ blocks (all breakable vanilla categories). Uses `#minecraft:` nested tags for logs/leaves/planks/wool/dirt/sand/ice/coral_blocks; individual entries for colored sets and all other blocks. Data flow unchanged — tag expansion → merge inline → merge sibling → persist if sibling was empty.

[AMENDED 2026-05-11] **Tag binding lookup contract (post-debug):** `RewardTagExpansion` enumerates tag members via **`BuiltInRegistries.X.getTagOrEmpty(TagKey)`** on the **static** built-in registry — that’s where `TagLoader` deposits datapack tag bindings during the resource reload. The `level.registryAccess()` / `HolderLookup.RegistryLookup` path was tried and verified empty against vanilla `minecraft:hostile` at runtime in MC 26.1.2 (1.21.11 internal); that view does not carry tag data. Hydrate now also wires **`ServerLifecycleEvents.END_DATA_PACK_RELOAD`** alongside `SERVER_STARTED` so `/reload` re-runs `finalizeRewardsForRunningServer`. Empty-lookup warning includes the registry’s total bound-tag count for future diagnosis. This unblocked the operator-editable `block_values.json` / `entity_values.json` UX that was silently persisting as `{}`.

[AMENDED 2026-05-12 — atomic write crash safety]: Wallet (`FileWalletStore`), jobs (`FileJobsStore`), and reward-config (`RewardRulesLoader`) persistence no longer write directly to their target files. All writes go through `AtomicFileWriter` (`com.fpsmod.io`): content is written to a `target.tmp` sibling file, flushed and fsynced (`FileChannel.force(true)`), then atomically renamed to the target via `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`. If the JVM / OS crashes mid-write only the `.tmp` is lost; the previous target file is untouched. On `load()`, any stale `.tmp` is deleted and logged. The `writeAtomicallyWithBackup()` variant additionally preserves a `target.bak` as last-known-good insurance — the backup is restored if the atomic rename itself fails (ENOSPC, disk failure). `WalletService.persist()` and `JobsService.persist()` are `synchronized` to prevent concurrent temp-file races on hot gameplay paths. Unit tests cover crash simulation, stale cleanup, concurrent writes, backup rotation, and store-level round-trips.

[AMENDED 2026-05-13 — class rename to avoid mod collision]: Mod ID changed from `fpsmod` to `project_ooga`. Main entrypoint `FpsMod` → `OogaMod`; client entrypoint `FpsModClient` → `ProjectOogaClient`. The standalone FPS overlay mod (the original template) defines identical class names; with Knot's parent-first classloading, Fabric Loader would load the FPS mod's `FpsMod` instead of ours — our entire server-side initialization (wallet, rewards, jobs, commands) would silently not run. Renaming to unique class names eliminates the collision. Package remains `com.fpsmod.*` (internal; Java packages don't cause Fabric mod conflicts). The legacy FPS HUD overlay (`FpsHudOverlay`, `FpsHudScreenButton`, `FpsHudConfig`) is deprecated and no longer wired in the client initializer. FPS display is handled by the standalone mod.

[AMENDED 2026-05-12 — atomic write crash safety]: Wallet (`FileWalletStore`), jobs (`FileJobsStore`), and reward-config (`RewardRulesLoader`) persistence no longer write directly to their target files. All writes go through `AtomicFileWriter` (`com.fpsmod.io`): content is written to a `target.tmp` sibling file, flushed and fsynced (`FileChannel.force(true)`), then atomically renamed to the target via `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`.

[AMENDED 2026-05-12 — jobs MVP]: New `com.fpsmod.jobs` package wires a second runtime layer atop the reward pipeline. `JobsService` impls `JobsHooks` with two stages: `multiplyPayout(player, ctx, basePayout)` runs **between** payout resolution and `wallets.addBalance` in `RewardOrchestrator` (only multiplies when the broken block / killed mob matches the player's active job tag); `onEconomyReward(player, ctx)` runs **after** credit lands and awards XP if the same tag matches. Active job and per-job XP persist to `config/otters_civ_revived/jobs.properties` via `FileJobsStore` (UUID-keyed, mirrors wallet store layout w/ `# Name:` hints). Per-job tag id sets are cached at SERVER_STARTED + END_DATA_PACK_RELOAD via the same `BuiltInRegistries.X.getTagOrEmpty(TagKey)` path the rewards prefill uses. Curve: `100 * L^1.5` cap 50, multiplier `1.0 + L/MAX_LEVEL` (lvl 50 = ×2.0). Bundled tags under `data/otters_civ_revived/tags/block/job/` + `tags/entity_type/job/` (singular dir rule preserved). `JobsHooks.NO_OP` retained so test/no-jobs runs stay decoupled. Commands: `/job`, `/job list|join|leave|stats` via `JobCommand`.

[AMENDED 2026-05-14 — jobs tuning file]: Jobs progression math is no longer locked to compiled constants. `JobsConfigLoader` now bootstraps `config/otters_civ_revived/jobs.json`, and `JobsService.refresh(...)` reloads that file before rebuilding tag caches. The shipped slugs remain fixed (`miner`, `lumberjack`, `farmer`, `fighter`) so saved player state, command suggestions, HUD icon mapping, and client payload shape stay stable, but operators can now tune `maxLevel`, `xpBase`, `xpExponent`, `multiplierTopBonus`, top-level `xpPerEvent`, and each job's `tagId` plus optional per-job XP override without touching Java. `jobs.properties` remains strictly player-state persistence.

[AMENDED 2026-05-14 — fully configurable jobs engine]: Jobs are now server-authoritative runtime data rather than a compiled enum roster. `jobs.json` defines global activation policy plus the live job catalog itself (job ids, display metadata, triggers, progression, and boosts). `CompiledJobCatalog` validates that config, expands block/entity/item tags through `BuiltInRegistries.*.getTagOrEmpty(...)`, and becomes the lookup surface for command UX, payout modifiers, gameplay XP, and client sync.

[AMENDED 2026-05-14 — event decoupling and client sync]: Jobs XP no longer relies only on post-wallet callbacks. `OttersCivGameplay` now emits dedicated `JobEventContext` snapshots for block breaks and mob kills, while `RewardOrchestrator` separately asks `JobsService.modifyPayout(...)` for money adjustments. This lets jobs award XP even when economy payout is zero (subject to trigger rules) and keeps boosts/rules explicit. Client correctness now uses two payload families: `JobCatalogPayload` (server-declared job metadata) and `JobStatusPayload` (local player active jobs + progression numbers). Remote clients never need a matching local `jobs.json`.

[AMENDED 2026-05-12 — jobs HUD]: New s2c payload `fpsmod:job_status` (`JobStatusPayload` record: slug + level + xp + xpForLevel + xpForNextLevel) pushed on player JOIN, every `/job join|leave`, and every matching reward event. `JobsService.setStatusListener(Consumer<ServerPlayer>)` is the integration seam — service module stays decoupled from `fabric-networking-api-v1`. Client-side: `JobsClientNetworking` registers the payload + a global receiver that updates a volatile `JobsClientState`. `JobsHudOverlay` (attached after `VanillaHudElements.EXPERIENCE_LEVEL`) renders a compact bar above the vanilla XP bar — operator-tunable via `JobsHudConfig` (`config/fpsmod/jobs_hud.properties`: visible/offsetX/offsetY/scale). `/otter` Jobs tab provides the GUI controls (toggle, reset, ±4 px nudges, ±0.1 scale, slash-command shortcuts).

[AMENDED 2026-05-13 — jobs HUD UX]: The live jobs bar now anchors after **`VanillaHudElements.INFO_BAR`** instead of the experience-level text layer, which is a better fit for an overlay that rides above the XP/jump/locator strip. The client config path is **`config/project_ooga/jobs_hud.properties`** (matching the renamed mod id). `/otter` → Jobs also gained an in-menu preview of the bar plus direct join buttons for the four shipped jobs, reducing the "controls exist but I can't see the bar" confusion during first-time setup.

[AMENDED 2026-05-13 — reward chat ownership]: Reward feedback is now intentionally split by responsibility. `RewardOrchestrator` owns the economy-side confirmation only and emits **`+N coins`** after `wallets.addBalance(...)`. `JobsService.onEconomyReward(...)` owns the progression-side confirmation and emits **`[job] +5 xp · Lvl X · inLevel/range`** only when the broken block / killed mob matches the player's active job, with a separate `level up` line on threshold crossings. This prevents globally rewarded block breaks from being mislabeled as miner/farmer/lumberjack XP in chat while keeping the wallet and progression systems independently understandable.
