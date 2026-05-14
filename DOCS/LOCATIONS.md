<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

[AMENDED 2026-05-12]:
- **Mod ID changed** from `fpsmod` to `project_ooga` to avoid mod ID conflict with the standalone FPS overlay mod (the original template this project was forked from). Both mods can now coexist in the same modpack.
- **FPS HUD overlay deprecated and disabled** (`FpsHudOverlay`, `FpsHudScreenButton`, `FpsHudConfig` in `src/client/java/com/fpsmod/client/`). The standalone FPS overlay mod handles FPS display.
- Networking/saved-data namespaces updated: `fpsmod:job_status` → `project_ooga:job_status`, `fpsmod:join_attendance` → `project_ooga:join_attendance`.
- Icon moved: `src/main/resources/assets/fpsmod/icon.png` → `src/main/resources/assets/project_ooga/icon.png`.
- Package names remain `com.fpsmod.*` (internal only, do not cause mod ID conflicts).

[AMENDED 2026-05-07]:
- Repository moved to `https://github.com/Otterdays/Minecraft-Civ-Remixed`.
- Project codename is now "Project OOGA".
- Strategic product direction is documented in `whitepaper.md`.
- Current codebase remains FPS-template-first and is pending civ-module implementation.

# LOCATIONS.md — quick codebase map

Use this as the first stop for quick discovery.

## First-check workflow (for future agents/builders)

1. Read **`AGENTS.md`** and **`CLAUDE.md`** for repo-wide agent rules + git conventions.
2. Read `LOCATIONS.md` (this file) for fast pathing.
3. Read `README.md` for behavior, compatibility, and run/test commands.
4. Read `build.gradle` and `gradle.properties` before changing versions/deps; read `gradle/wrapper/gradle-wrapper.properties` when changing the Gradle distribution.
5. Read `src/main/resources/fabric.mod.json` before changing IDs/entrypoints.

## Core implementation

- Main mod initializer: `src/main/java/com/fpsmod/FpsMod.java`
- Client initializer: `src/client/java/com/fpsmod/FpsModClient.java`
- FPS HUD feature: `src/client/java/com/fpsmod/client/FpsHudOverlay.java`
- Screen-level FPS toggle button: `src/client/java/com/fpsmod/client/FpsHudScreenButton.java`
- FPS HUD config persistence: `src/client/java/com/fpsmod/client/FpsHudConfig.java`
- Unit test starter: `src/test/java/com/fpsmod/FpsModTest.java`

## Mod metadata + wiring

- Mod metadata and entrypoints: `src/main/resources/fabric.mod.json` (`environment: client` → Mod Menu **Client** badge; `contact.issues` → Mod Menu **Issues** link; optional `suggests.modmenu` for nicer in-game mod list when Mod Menu is installed)
- Prism/MultiMC mod icon asset: `src/main/resources/assets/project_ooga/icon.png`
- Readme-only cropped icon (not packaged in the mod JAR): `readme-assets/icon_modrinth_cropped.png`
- Mod id constant: `FpsMod.MOD_ID` in `src/main/java/com/fpsmod/FpsMod.java`
- Client entrypoint target class: `com.fpsmod.FpsModClient` in `fabric.mod.json`

## Build + toolchain

- Gradle build config: `build.gradle`
- Version pins (Minecraft/Fabric/Loader/Loom): `gradle.properties`
- **Gradle wrapper** (exact distribution ZIP): `gradle/wrapper/gradle-wrapper.properties` — this repo uses a **9.6 nightly** from `services.gradle.org/distributions-snapshots/` (not `distributions/`).
- Project name + plugin repos: `settings.gradle`
- Windows build shortcut: `build.bat`
- Gradle wrappers: `gradlew.bat`, `gradlew`, `gradle/wrapper/`

## Feature behavior map (FPS HUD)

- HUD layer registration: `FpsHudOverlay.register()` in `FpsHudOverlay.java`
- HUD render method: `FpsHudOverlay.render(...)`
- Shared toggle state methods: `FpsHudOverlay.toggleHud()`, `setHudShown(...)`, `isHudShown()`
- 1-second FPS sampling: `ClientTickEvents.END_CLIENT_TICK` block in `FpsHudOverlay.java`
- Screen-button registration: `FpsHudScreenButton.register()` (`ScreenEvents.AFTER_INIT`)
- Config file path runtime target: `config/fpsmod/hud.properties`

## Logging conventions

- Startup + debug log patterns: `src/main/java/com/fpsmod/FpsMod.java`
- HUD toggle log (`🔁`): `src/client/java/com/fpsmod/client/FpsHudOverlay.java`
- Rule: keep emoji prefixes for high-visibility console scanning.

## Commands you will use most

- Build jar: `gradlew.bat build` (or `build.bat`)
- Run dev client: `gradlew.bat runClient`
- Run tests: `gradlew.bat test`

## Otters Civ. / economy (server gameplay)

[AMENDED 2026-05-12]:
- **`currency_blocks` tag massively expanded (~260+ blocks).** Source file: `src/main/resources/data/otters_civ_revived/tags/block/currency_blocks.json`. Covers: all stone/brick/polished/chiseled variants, ores via `#minecraft:*_ores` + nether_quartz_ore + ancient_debris, dirt/sand/gravel/clay via `#minecraft:dirt`/`#minecraft:sand`, all wood via `#minecraft:logs`/`#minecraft:leaves`/`#minecraft:planks`, bamboo blocks, all 16 colors of wool (`#minecraft:wool`) / terracotta / glazed terracotta / concrete / concrete powder / stained glass, sandstone, bricks, prismarine, full nether set, end blocks, `#minecraft:ice`, snow, `#minecraft:coral_blocks`, amethyst, all copper oxidation+waxed+grate+bulb permutations, raw ore + mineral storage blocks, glass, organic (melon/pumpkin/hay/kelp/honey/bone/sponge/mushroom), sculk family, and common utility blocks. All default to `blockReward` (1); operators tune per-block amounts in `block_values.json`.

[AMENDED 2026-05-11]:
- Runtime wallet path: **`config/otters_civ_revived/wallet.properties`** (`FileWalletStore`); migrate-from legacy **`config/fpsmod/wallet.properties`** once if present. Optional **`# Name:`** plaintext lines precede **`uuid=balance`**; persist via **`WalletService`** (`WalletLedger` load/save).

[AMENDED 2026-05-12 — `/otter` all-inclusive UI]:
- **`OttersCivScreen` (client)** carries every roadmap surface. Tab enum: `HOME / WALLET / JOBS / REWARDS / CIV / HELP`. Panel 480×268, sidebar 132. `Status` enum (LIVE/PARTIAL/SOON/FUTURE) + `drawBadge` + `drawCommandRow` + `drawMilestone` helpers. HOME paints a 7-segment M0–M6 strip; CIV stacks M3–M6 cards; WALLET/REWARDS/HELP enumerate shipped + roadmap commands with chips. Quick-action key `action:jobs` jumps to the JOBS tab from HOME.

[AMENDED 2026-05-12 — jobs HUD bar]:
- **Server→client sync:** `src/main/java/com/fpsmod/jobs/net/JobStatusPayload.java` (record, `CustomPacketPayload.createType("fpsmod:job_status")`, `StreamCodec.composite` codec), `JobsNetworking.java` (registers on `PayloadTypeRegistry.clientboundPlay()`, hooks `ServerPlayConnectionEvents.JOIN`, exposes `sendStatusFor`). `JobsService.setStatusListener(Consumer<ServerPlayer>)` keeps the service module networking-agnostic — `FpsMod.onInitialize` wires the listener after building both.
- **Client side:** `src/client/java/com/fpsmod/client/jobs/` — `JobsClientState` (volatile latest payload), `JobsClientNetworking` (registers payload + receiver), `JobsHudConfig` (`config/fpsmod/jobs_hud.properties`: visible/offsetX/offsetY/scale; clamps 0.75–2.0 scale, ±400 px offsets), `JobsHudOverlay` (attaches after `VanillaHudElements.EXPERIENCE_LEVEL` — `EXPERIENCE_BAR` field does NOT exist in fabric-rendering-v1 of this release; available ids: HOTBAR, EXPERIENCE_LEVEL, INFO_BAR, etc.).
- **UI controls:** `OttersCivScreen` JOBS tab — toggle/reset + X/Y/scale nudge buttons + `/job`, `/job list` shortcut buttons. Action keys prefixed `jobs:` route through `handleJobsAction(String)`.
- **Fabric API method names (this version, do not regress):** `PayloadTypeRegistry.clientboundPlay()` / `serverboundPlay()` (not `playS2C`/`playC2S`); `ClientPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {})`; `ServerPlayNetworking.send(serverPlayer, payload)`.

[AMENDED 2026-05-13 — jobs HUD UX cleanup]:
- **Client config path:** `JobsHudConfig` now persists to **`config/project_ooga/jobs_hud.properties`** (matching `OogaMod.MOD_ID` after the mod-id rename); older docs claiming `config/fpsmod/jobs_hud.properties` are stale.
- **HUD layer anchor:** `JobsHudOverlay` now attaches after **`VanillaHudElements.INFO_BAR`** so the job bar rides with the vanilla XP/jump/locator strip rather than the experience-level text layer specifically.
- **Jobs tab usability:** `OttersCivScreen` JOBS tab now renders a preview of the live HUD bar and exposes one-click join buttons for miner / lumberjack / farmer / fighter in addition to the existing toggle / X / Y / scale controls.

[AMENDED 2026-05-12 — jobs MVP]:
- **Jobs package:** `src/main/java/com/fpsmod/jobs/` — `Job` enum (shipped slugs + default tag ids + Kind.BLOCK/ENTITY), `JobsConfig` (operator-editable curve + per-job overrides), `JobsConfigLoader` (`config/otters_civ_revived/jobs.json` bootstrap/load), `JobState` (active slot + EnumMap XP), `JobsLedger` record, `JobsStore` interface, `FileJobsStore` (UUID-keyed properties at `config/otters_civ_revived/jobs.properties`), `JobsService` (impl of `JobsHooks` w/ `multiplyPayout` + `onEconomyReward`; reloads `jobs.json` and refreshes per-job id caches on SERVER_STARTED + END_DATA_PACK_RELOAD).
- **Command:** `src/main/java/com/fpsmod/command/JobCommand.java` — `/job`, `/job list|stats|leave`, `/job join <slug>` (brigadier suggestions over the 4 slugs).
- **`JobsHooks` interface** (`com.fpsmod.ottersciv.reward.JobsHooks`) now has default `multiplyPayout(player, ctx, basePayout)` → returns base by default; `NO_OP` still safe for tests/no-jobs deployments. `RewardOrchestrator.onBlockBroken` / `onMobKilled` call the multiplier between payout-resolution and `wallets.addBalance`.
- **Bundled job tags (singular dirs):** `src/main/resources/data/otters_civ_revived/tags/block/job/miner_blocks.json`, `lumberjack_blocks.json`, `farmer_blocks.json`; `tags/entity_type/job/fighter_mobs.json` (currently `#otters_civ_revived:hostile_mobs`).
- **Wiring:** `OogaMod.onInitialize` builds `JobsService.createDefault()`, hands it to `OttersCivGameplay.register(wallets, rules, jobsHooks)`, registers `JobCommand`, and calls `jobsService.refresh(server)` from `onLogicalServerFullyStarted`.
- **Test:** `src/test/java/com/fpsmod/jobs/JobsConfigTest.java` (curve monotonicity + round-trip + multiplier clamp + state add/active).

[AMENDED 2026-05-13 — reward chat flow]:
- **Economy message source:** `src/main/java/com/fpsmod/ottersciv/reward/RewardOrchestrator.java` now owns only the money line and formats it as **`+N coins`** via `coinMessageText(...)`, sent immediately after `wallets.addBalance(...)`.
- **Jobs progress message source:** `src/main/java/com/fpsmod/jobs/JobsService.java` now owns the job-side line via `progressMessageText(...)`, emitting **`[job] +5 xp · Lvl X · inLevel/range`** only when the reward event matches the player's active job. Level-up remains a separate follow-up line.
- **Why this split matters:** block rewards from the shared `currency_blocks` tag can still pay coins even when they do **not** match the active job (for example, a miner breaking a rewarded log). The new chat flow avoids falsely labeling that as miner XP.

[AMENDED 2026-05-13 — broad entity rewards]:
- **Bundled reward entity tag expanded:** `src/main/resources/data/otters_civ_revived/tags/entity_type/currency_mobs.json` now carries a broad current vanilla living-entity roster for reward prefills, so `entity_values.json` seeds much more like `block_values.json` instead of listing only hostiles.
- **Hostile-only companion tag:** `src/main/resources/data/otters_civ_revived/tags/entity_type/hostile_mobs.json` preserves the combat-only grouping. `tags/entity_type/job/fighter_mobs.json` now points at `#otters_civ_revived:hostile_mobs`, so the fighter job remains hostile-focused even though the economy reward surface is broader.

[AMENDED 2026-05-11 — root-cause pass]:
- **Bundled-tag resource paths are SINGULAR (MC 1.21+ requirement):**
  - `src/main/resources/data/otters_civ_revived/tags/block/currency_blocks.json` (NOT `tags/blocks/`)
  - `src/main/resources/data/otters_civ_revived/tags/entity_type/currency_mobs.json` (NOT `tags/entity_types/`)
  - `src/main/resources/data/otters_civ_revived/tags/entity_type/hostile_mobs.json`
  Minecraft's `TagLoader` silently ignores plural directory names — confirmed by enumerating `data/minecraft/tags/` in the deobf jar (`block`, `entity_type`, `damage_type`, `enchantment`, `fluid`, etc. — all singular).
- **Default `entityTag` is `otters_civ_revived:currency_mobs`** (our bundled tag with explicit hostile-mob ids). Vanilla has **no** `minecraft:hostile` entity-type tag in 1.21+; do not change the default back to that.
- [AMENDED 2026-05-13]: `currency_mobs` is now the broad default reward-entity tag. Use bundled `otters_civ_revived:hostile_mobs` when you specifically want the hostile-only subset (the fighter job now does).

[AMENDED 2026-05-11]:
- **Tag expansion lookup path (do not regress):** **`RewardTagExpansion`** queries **`BuiltInRegistries.X.getTagOrEmpty(TagKey)`** on the **static** registry — not `level.registryAccess().lookupOrThrow(...).get(TagKey)`. In MC 26.1.2 (1.21.11 internal mappings) the `RegistryAccess.Frozen` view returns `Optional.empty()` for every tag (verified at runtime against vanilla `minecraft:hostile`). `TagLoader` binds datapack tags onto static `BuiltInRegistries` during resource reload; that's the only reliable enumeration source for the prefill.
- **Hydrate triggers:** **`FpsMod.onInitialize`** registers BOTH **`ServerLifecycleEvents.SERVER_STARTED`** (initial fill) and **`ServerLifecycleEvents.END_DATA_PACK_RELOAD`** (re-fill after `/reload`); both route through **`RewardRulesLoader.finalizeRewardsForRunningServer`** → **`RewardOrchestrator.replaceRules`**. Empty-tag diagnostic logs total bound-tag count so future regressions are obvious in `logs/latest.log`.
- **Test access seam:** **`RewardRulesLoader.composeEffectiveIdMap`** is package-private and directly unit-tested (precedence, sibling-empty persist, preserve-existing-sibling). Tag expansion itself is integration-only.

[AMENDED 2026-05-10]:
- Passive rewards orchestration: `src/main/java/com/fpsmod/ottersciv/reward/RewardOrchestrator.java`
- Reward rules / expansion: `RewardRules.java`, `RewardRulesLoader.java` (**`loadBootstrapRewards`**, **`finalizeRewardsForRunningServer`**), **`RewardTagExpansion.java`**, **`KillRewardTagChecks.java`** (+ `reward/RewardOrchestrator.java` **`replaceRules`**); sibling JSON under **`config/otters_civ_revived/`**. Tests: `RewardRulesLoaderTest.java`
- Gameplay wiring: `src/main/java/com/fpsmod/ottersciv/OttersCivGameplay.java` (`JoinWelcome.java` registers join chat; **`JoinAttendanceSavedData.java`** — per-save returning-player flag, **`fpsmod:join_attendance`**)
- Commands: `src/main/java/com/fpsmod/command/OtterCommand.java`, `MoneyCommand.java` (`/money set` · `Permission.HasCommandLevel(GAMEMASTERS)`); wallet: `src/main/java/com/fpsmod/economy/`
- Shipped datapack tags: `src/main/resources/data/otters_civ_revived/tags/`

### Quick-path file index (economy / rewards)

For fast agent discovery — the files you need for any reward/economy task:

| What | Path |
|------|------|
| **Block tag (source of truth for which blocks pay)** | `src/main/resources/data/otters_civ_revived/tags/block/currency_blocks.json` |
| **Entity tag (source of truth for which entities pay by default)** | `src/main/resources/data/otters_civ_revived/tags/entity_type/currency_mobs.json` |
| **Hostile-only entity tag (fighter job / combat-only setups)** | `src/main/resources/data/otters_civ_revived/tags/entity_type/hostile_mobs.json` |
| **Reward rules model** | `src/main/java/com/fpsmod/ottersciv/config/RewardRules.java` |
| **Reward config loader + merge logic** | `src/main/java/com/fpsmod/ottersciv/config/RewardRulesLoader.java` |
| **Tag → payout expansion** | `src/main/java/com/fpsmod/ottersciv/config/RewardTagExpansion.java` |
| **Kill-side tag checks** | `src/main/java/com/fpsmod/ottersciv/config/KillRewardTagChecks.java` |
| **Reward orchestrator (runtime)** | `src/main/java/com/fpsmod/ottersciv/reward/RewardOrchestrator.java` |
| **Wallet service** | `src/main/java/com/fpsmod/economy/WalletService.java` |
| **Wallet persistence** | `src/main/java/com/fpsmod/economy/FileWalletStore.java` |
| **`/money` command** | `src/main/java/com/fpsmod/command/MoneyCommand.java` |
| **`/otter` command (server)** | `src/main/java/com/fpsmod/command/OtterCommand.java` |
| **`/otter` GUI (client)** | `src/client/java/com/fpsmod/client/ui/OttersCivScreen.java` |
| **`/otter` client cmd reg** | `src/client/java/com/fpsmod/client/OtterClientCommand.java` |
| **Join welcome** | `src/main/java/com/fpsmod/ottersciv/JoinWelcome.java` |
| **Join attendance (per-save)** | `src/main/java/com/fpsmod/ottersciv/JoinAttendanceSavedData.java` |
| **Gameplay wiring** | `src/main/java/com/fpsmod/ottersciv/OttersCivGameplay.java` |
| **Tests** | `src/test/java/com/fpsmod/ottersciv/config/RewardRulesLoaderTest.java` |
| **Runtime config dir** | `config/otters_civ_revived/` (rewards.json, block_values.json, entity_values.json, wallet.properties) |

## Output paths

- Main mod jar: `BUILT/libs/project-ooga-1.0.0.jar` (Gradle `mod_version`; older docs may cite `fps-mod-1.0.0.jar`—prefer `gradle.properties`)
- Sources jar: `BUILT/libs/project-ooga-1.0.0-sources.jar`
- Test report: `BUILT/reports/tests/test/index.html`
- Gradle problems report: `BUILT/reports/problems/problems-report.html`

## Safe-edit hotspots

- UI/HUD-only changes: `src/client/java/com/fpsmod/client/`
- Cross-cutting metadata changes: `fabric.mod.json` + `gradle.properties` + `README.md`
- Version upgrades: keep `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties` (when bumping Gradle), and this README’s compatibility table aligned.

## Reference / research assets

| What | Path |
|------|------|
| **MC block list reference URL** | `WEB-SOURCES-FOR-MC-INFO/BLOCKS_WEBSITE.md` |

Any web sources saved for Minecraft data (block lists, entity lists, etc.) live under **`WEB-SOURCES-FOR-MC-INFO/`** — check before web-searching.

## Git / upstream

- Default remote is set when you clone; this template was pushed to `https://github.com/Otterdays/Minecraft-Fabric-Sample-Mod` (if you fork or rename the repo, update all `contact` fields in `fabric.mod.json`: `homepage`, `sources`, and `issues`).
