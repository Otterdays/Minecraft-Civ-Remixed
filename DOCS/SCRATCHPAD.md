<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD

## Active Tasks (Newest First)
- [2026-05-13] **Entrypoint classes renamed to avoid mod collision:** `FpsMod` → `OogaMod`, `FpsModClient` → `ProjectOogaClient`. The standalone FPS overlay mod defines identical class names — with Knot's parent-first classloading, our mod's init code was silently replaced by the FPS mod's copy (both server-side wallet/economy init AND client-side FPS overlay registration). Unique class names fix this. `fabric.mod.json` entrypoints updated. 54 `FpsMod.*` references across 47 files bulk-renamed to `OogaMod.*`. File + test renamed. Build green.

- [2026-05-12] **Atomic write crash safety shipped:** `AtomicFileWriter` (`com.fpsmod.io`) with `.tmp` + fsync + `Files.move(ATOMIC_MOVE)`. `writeAtomicallyWithBackup()` preserves `.bak` as last-known-good. Applied to `FileWalletStore`, `FileJobsStore`, `RewardRulesLoader` (rewards.json/block_values.json/entity_values.json). Legacy migration now atomic (read → atomic-write → delete). `WalletService.persist()` / `JobsService.persist()` synchronized. 17 new tests: crash simulation, stale cleanup, concurrent writes, backup rotation, store round-trips. Build green.

- [2026-05-12] **`/otter` all-inclusive pass:** every roadmap milestone (M0–M6) surfaced in-game. New `Status` chips (LIVE/PARTIAL/SOON/FUTURE). HOME shows M0–M6 progress strip + 4 quick actions. WALLET/REWARDS/HELP list shipped + planned commands with badges. CIV rewritten as 4 milestone cards (M3 Factions, M4 Shops, M5 Governance, M6 Scale). Panel resized 460×248 → 480×268. Tabs: HOME · WALLET · JOBS · REWARDS · CIV · HELP. New `action:jobs` key.

- [2026-05-12] **Jobs HUD bar + in-game configurator (M2 slice 2):** server→client `JobStatusPayload` (fabric `clientboundPlay()` registry — note method renamed from `playS2C` in fabric-networking-api-v1 6.3.0); pushed on JOIN + every `/job` mutation + every matching reward event via `JobsService.setStatusListener` callback (keeps service networking-agnostic). Client HUD `JobsHudOverlay` draws above vanilla XP bar (attaches after `VanillaHudElements.EXPERIENCE_LEVEL` — note no `EXPERIENCE_BAR` field exists in this fabric-rendering version; use EXPERIENCE_LEVEL or HOTBAR for anchoring). BMP-only icons (⛏▲✿⚔) for unifont safety. Operator-tunable via `/otter` → new Jobs tab (toggle/X/Y/scale/reset + slash shortcuts). Persistence `config/fpsmod/jobs_hud.properties`. Build green.

- [2026-05-12] **Jobs/professions MVP shipped (M2 slice 1):** fixed set miner/lumberjack/farmer/fighter, 1 active slot, XP/level curve `100*L^1.5` cap 50, payout multiplier 1.0→2.0. New package `com.fpsmod.jobs` (`Job`, `JobsConfig`, `JobState`, `JobsLedger`, `JobsStore`, `FileJobsStore`, `JobsService`). `JobsHooks` interface gained `multiplyPayout` stage; `RewardOrchestrator` calls it pre-`addBalance`. 4 bundled tags under `tags/block/job/*.json` + `tags/entity_type/job/fighter_mobs.json` (singular dirs). `JobCommand` brigadier with suggestions. `FpsMod.onInitialize` wires `JobsService.createDefault()` + refresh on SERVER_STARTED/END_DATA_PACK_RELOAD. `OtterCommand` chat fallback updated. Test: `JobsConfigTest`. Build green.

- [2026-05-12] **Expanded `currency_blocks` tag to ~260+ blocks:** rewrote `tags/block/currency_blocks.json` from the original ~23 entries to cover essentially every breakable vanilla block. Uses `#minecraft:` tags for logs, leaves, planks, wool, dirt, sand, ice, coral_blocks; individual entries for all colored terracotta/glazed/concrete/concrete_powder/stained_glass (16 each), every stone/brick/polished variant, full nether + end sets, all copper oxidation/waxed/grate/bulb permutations, ore storage blocks, organic blocks, sculk family, and common utility blocks. Build green. Operators customize amounts in `block_values.json` after prefill.

- [2026-05-12 — polish] **`/otter` menu scaling & edge cases:** panel widened 420×240 → 460×248 and sidebar 110 → 128 for breathing room; tab subtitles shortened (Home → "Quick actions", Wallet → "Balance & commands", Rewards → "Mining & combat", Civ → "Coming soon", Help → "Commands & configs") so they stop bleeding into the content column on the screenshot user reported. Added `OttersCivScreen.fit(String, int)` — ellipsis-truncates any label that exceeds the sidebar width (defensive for future longer strings & resource-pack font swaps; uses real `Font.width`). Replaced overlapping bottom-right hint + Home content mod-id line with a single footer band (divider + "Mod id: fpsmod · /otter · /money" left, "ESC to close" right); content area `ch` reduced so per-tab bodies never draw under it. `gradlew.bat compileClientJava` green.

- [2026-05-12] **`/otter` stylized GUI menu (client-side):** new `OttersCivScreen` (panel-style hub w/ sidebar tabs Home·Wallet·Rewards·Civ·Help, gold/aqua palette, animated fade, hover glow) opened by client-side `/otter` (`OtterClientCommand` via `ClientCommandRegistrationCallback`, takes precedence over server fallback). Built on the 26.1 `Screen` render-graph API (`extractRenderState`, `extractBackground`, `MouseButtonEvent`) — `GuiGraphicsExtractor.text/fill/outline` for all drawing, no texture assets. Buttons invoke `/money` via `LocalPlayer.connection.sendCommand` or open config files (`rewards.json`, `block_values.json`, `entity_values.json`, `wallet.properties`) via `Util.getPlatform().openFile`. Server-side `OtterCommand` chat help retained for vanilla clients. `gradlew.bat build` green.

- [2026-05-12] **AI IDE tooling — explicit tool-usage guidance in agent files:** Added `AGENTS.md` § **AI IDE tooling** with full MCP server table (Context7, Exa, GitHub, Memory, Playwright, Sequential Thinking, Docker status), web search mandate, subagent roster, built-in tool preferences, and key principle. `CLAUDE.md` mirrors with a compact summary + cross-link. New Cursor rule `.cursor/rules/use-all-tools.mdc` (`alwaysApply: true`) — covers web search, MCP usage, subagents, preferred built-in tools. Docker MCP noted as currently errored.

- [2026-05-11] **Operator "add your own blocks/mobs" doc surface:** `index.html` new section `#add-custom-payouts` (three-tier: edit sibling files / inline `rewards.json` / extend bundled tag via server datapack — singular dir names called out); sidebar TOC anchor; README payout subsection with same three-tier and link back to `index.html`; **`/otter`** in-chat help reflows with numbered steps + precedence note. CHANGELOG `Added`.

- [2026-05-11] **Tag expansion — static `Registry.getTagOrEmpty` (the actual fix):** runtime log showed `level.registryAccess().lookupOrThrow(...).get(TagKey)` returns `Optional.empty()` for `minecraft:hostile` and every other tag in MC 26.1.2 (1.21.11 internal). Tags only resolve via the static `BuiltInRegistries.X.getTagOrEmpty(TagKey)` path where `TagLoader` binds them during reload. Also wires `ServerLifecycleEvents.END_DATA_PACK_RELOAD` to re-finalize after `/reload`. Empty-lookup warning now logs total bound-tag count. CHANGELOG `Fixed`.

- [2026-05-11] **Tag expansion rewrite — `HolderSet.Named` direct lookup:** `RewardTagExpansion` now asks the server registry for the tag's members directly (`lookupOrThrow(Registries.X).get(tagKey)` → `Optional<HolderSet.Named<T>>`) and iterates those holders, mapping `holder.value()` back to id via `BuiltInRegistries.X::getKey`. Replaces the "walk every block, ask holder.is(tag)" approach which produced empty prefill files. Adds `INFO` log on tag resolution (entry count) and `WARN` on empty/missing. CHANGELOG `Fixed`.

- [2026-05-11] **Empty `block_values.json` bug fix:** block tag expansion now resolves membership via `level.registryAccess().lookupOrThrow(Registries.BLOCK)` (mirrors entity path); `payoutsForTaggedBlocks` takes a `ServerLevel`. Datapack tags (incl. mod-bundled `otters_civ_revived:currency_blocks`) bind to the live server registry, not the static `BuiltInRegistries.BLOCK` holders — old static-holder lookup returned zero matches and the persisted sibling file was `{}`. Zero-reward tags no longer short-circuit expansion; ids prefill with `0` so operators can still edit. Warn-log on zero matches. CHANGELOG `Fixed`.

- [2026-05-11] **Reward config debug pass:** `RewardRulesLoader.writeDefaults` now appends `System.lineSeparator()` (parity with sibling JSON writer); `composeEffectiveIdMap` made package-private and directly unit-tested (precedence + sibling-empty persistence + preserve-existing-sibling). Closes test gap where only `mergeExternalValueFiles` was exercised. CHANGELOG `Fixed`.

- [2026-05-10] **Prefilled per-id payouts:** logical-server **started** expands `blockTag`/`entityTag` into default amounts (`RewardTagExpansion`), persists sibling JSON when unparsed/nonexistent, applies `finalizeRewardsForRunningServer` + orchestrator **`replaceRules`**. Bootstrap **`loadBootstrapRewards`**. CHANGELOG/`index`/ARCHITECTURE/`/otter`.

- [2026-05-10] **Per-id reward files:** `block_values.json` / `entity_values.json` beside `rewards.json` merged after inline maps (`RewardRulesLoader`); CHANGELOG/README/`index.html`/modrinth/ARCHITECTURE/LOCATIONS/`/otter`; `RewardRulesLoaderTest`.

- [2026-05-11] **`index.html` + docs surface:** **`#limits`** states join onboarding vs welcome-back is **per world save** (`fpsmod:join_attendance`, not `config/`). **SUMMARY** amended: canonical reference is repo root **`index.html`** only (no **`website/`** mirror).

- [2026-05-11] **`JoinWelcome` returning players:** **`JoinAttendanceSavedData`** (**`fpsmod:join_attendance`**, overworld **`SavedDataStorage`**); styled **welcome back ~name** + short refresher vs full three-line first join. CHANGELOG **`Changed`**, **LOCATIONS**, **FEATURES** / **SUMMARY** amended, **`index.html`** / README join copy.

- [2026-05-11] **Command permissions:** `/money` open; **`/money set`** requires **`Permission.HasCommandLevel(GAMEMASTERS)`** (26.x APIs). **`#command-permissions`** on **`index.html`**, README subsection, roadmap **Permissions apparatus (planned)**; CHANGELOG.

- [2026-05-11] **`wallet.properties` name hints:** optional `# Name:` lines before `uuid=balance`; `WalletLedger` + joined `WalletStore.save`; join + `/money` + rewards refresh labels; **`/otter`** note; **`index.html`** / README / CHANGELOG.

- [2026-05-11] **Wallet file** moved from `config/fpsmod/wallet.properties` → **`config/otters_civ_revived/wallet.properties`** (`FileWalletStore` + one-time migrate from legacy path). Docs/README/`index.html`/modrinth/CHANGELOG updated; **`fpsmod/`** HUD-only remains.

- [2026-05-11] Readability pass (tasteful): README **Words we use** table + Highlights one-liner + docs/roadmap soft leads; **`index.html`** same gloss (styled region), `.lead` captions, plain command/feature copy, `#words-we-use-h` for scroll spy; **`DOCS/modrinth-description.md`** annotated; **CHANGELOG** / **SCRATCHPAD** bumped.

- [2026-05-11] Naming + clarity: README table (**Otters Civ. Revived** vs codename **Project OOGA**); slash-command explainer (“server” = game side that remembers money); `index.html` infobox codename row, World side heading, Chat commands section; installation note simplified; Mod Menu `fabric.mod.json` description.

- [2026-05-11] **README** restyle for GitHub: badges row, structured sections, tables, install + docs index; jargon moved to `DOCS/` / `AGENTS.md`.

- [2026-05-10] **`AGENTS.md` + `CLAUDE.md`**, `.gitattributes` (`*.html`, `*.mdc` LF), `.gitignore` (`.env*`, `.claude/cache/`); SUMMARY/LOCATIONS/README cross-links; CHANGELOG Added bullet.

- [2026-05-10] **Rules for `index.html` upkeep:** STYLE_GUIDE § Website parity (when to edit, sidebar hygiene, housekeeping); Cursor rule `.cursor/rules/index-html-parity.mdc` (alwaysApply); README contributors + SUMMARY quick link.

- [2026-05-10] **Docs + `index.html` sync:** README (join + per-id rewards + offline page), DOCS `SUMMARY`, `LOCATIONS`, `FEATURES`, `ARCHITECTURE`, `CHANGELOG`; repo root **`index.html`** infobox/intro/commands/see-also/contents rewards label.

- [2026-05-10] **`rewards.json` per-id payouts**: `blockRewards` / `entityRewards` maps (normalized ids, invalid keys skipped + warn); precedence over tag flat amounts; `RewardOrchestrator`, loader, `OtterCommand` hint, `index.html`, Modrinth description, `RewardRulesLoaderTest`.

- [2026-05-10] **`JoinWelcome`**: `ServerPlayConnectionEvents.JOIN` → three system messages (branding, `/otter` + `/money`, payouts pointer); registered from `OttersCivGameplay.register`.
- [2026-05-10] **`index.html`**: sticky left **On this page** sidebar (nested anchors, narrow layout + responsive stack), `#main` landmark, skip link, subsection ids for Features/Design goals/config files/`#see-also`, `aria-current` via `IntersectionObserver` + hash.
- [2026-05-10] **`index.html`** reference page: added requirements/stack table, wallet file format, bundled `currency_blocks` / `currency_mobs` defaults, full `rewards.json` field table (code defaults), config file quick reference, M0–M6 roadmap snapshot, meta description, infobox (artifact + pinned deps + issues), whitepaper/architecture links in references — no removal of prior copy.
- [2026-05-09] **`/otter`** help command (`OtterCommand`); **`LICENSE`** rewritten (ARR + carve-outs: video/montage, official JAR in mod packs, no source/fork reproduction without permission); README + `fabric.mod.json` description + `modrinth-description` + `CHANGELOG` synced.
- [2026-05-09] **Otters Civ. Revived** block-break + hostile-kill payouts shipped: `RewardOrchestrator`, `config/otters_civ_revived/rewards.json`, datapack tags `otters_civ_revived:*`, `WalletService.addBalance`, `JobsHooks` NO_OP; wired in `FpsMod`; docs `CHANGELOG` + `modrinth-description`.
- [2026-05-08] Imagery Optimizery GUI: optional per-file list (Add files / Add folder / Remove / Clear), table with input px+KB, output px, estimated PNG/JPG KB (in-memory encode); Optimize uses list if non-empty else full input folder; debounced estimates + generation guard for stale threads.
- [2026-05-08] [NOTE] Imagery Optimizery (`images/optimize-here/`): maintainer likes current GUI look/feel. Do **not** restyle, “simplify,” or re-theme the tkinter UI unless asked explicitly — prefer docs/behavior-only changes.
- [2026-05-08] Imagery Optimizery GUI: expanded “About” copy, grid layout for paths + browse, run bar (Clear log + hint + Optimize), output log section, footer path; primary action bottom-right of run strip above log.
- [2026-05-08] [AMENDED] Imagery Optimizery GUI: create `Tk()` before `StringVar`/`IntVar` (Python 3.14+ requires default root).
- [2026-05-08] Added Imagery Optimizery minimal tkinter GUI (`--gui`) + batch launcher branding; refactor shared `run_batch`/CLI.
- [2026-05-08] Fixed `images/optimize-here/optimize-pngs.bat` working directory (`cd /d "%~dp0"`); anchored `optimize_pngs.py` paths to script dir + PNG zlib level 9, default max width 960, progressive JPG.
- [2026-05-07] Recentered README on Project OOGA civ-mod identity and `/money` bootstrap usage.
- [2026-05-07] Implemented `/money` bootstrap command and wallet persistence; tracking in `DOCS/ROADMAP.md`.
- [2026-05-07] Created dedicated execution roadmap in `DOCS/ROADMAP.md`.
- [2026-05-07] Docs alignment pass for Project OOGA rule compliance.
- [2026-05-07] Whitepaper upgraded to premier civ-mod strategy document.
- [2026-05-07] Repo branding/remote migration to `Minecraft-Civ-Remixed`.

## Current Status
- Codebase still uses **fpsmod** mod id/package; branding for new civ gameplay is **Otters Civ. Revived** (`otters_civ_revived` config/datapack namespace).
- Server rewards: mined tagged blocks + direct-kill hostile mobs credit wallet (`addBalance`).
- Legacy FPS HUD remains an optional client-side extra.
- Core DOCS baseline files created for stable multi-agent handoff.
- Economy + help bootstrap: **`/otter`**, **`/money`**, **`/money set`** compile and build successfully.

## Last 5 Actions
1. Expanded repo `index.html` mod reference (configs, default tags, roadmap M0–M6, requirements).
2. Shipped `/otter` help, LICENSE carve-outs, README/modrinth/`fabric.mod.json` doc pass.
3. Implemented Otters Civ. mining/combat payouts, reward config + tags, changelog/modrinth copy.
4. Documented Imagery Optimizery in `SUMMARY` + `CHANGELOG`; SCRATCHPAD note to leave GUI as-is unless explicitly requested.
5. Imagery Optimizery tkinter GUI + `--gui`; batch opens GUI by default; shared `run_batch()` for CLI/UI.
6. Patched promo image optimizer batch + Python under `images/optimize-here/` for cwd-safe runs and smaller web outputs.
7. Rewrote `README.md` to center Project OOGA civ-mod scope and current command bootstrap.
8. Marked README recentering progress in `DOCS/ROADMAP.md` ASAP tracker.

[AMENDED 2026-05-10]: Renumbered backlog items after inserting the `index.html` line so the list stays ordered (section title unchanged).
- [AMENDED 2026-05-08]: Prior “last actions” still valid: server-safe `fabric.mod.json` environment `*`; implemented `WalletStore` / `FileWalletStore` / `WalletService`; registered `/money` and `/money set <player> <amount>` paths.

## Blockers
- None currently.

## Hard-won lessons (2026-05-11)
- **Tag resource directories are SINGULAR in MC 1.21+** (`tags/block/`, `tags/entity_type/`, `tags/item/`, `tags/fluid/`, `tags/damage_type/`, `tags/worldgen/...`). The plural forms (`blocks/`, `entity_types/`) are silently ignored by `TagLoader` — no error, just empty tags. When a bundled tag returns zero entries, check the directory name FIRST by enumerating `data/minecraft/tags/` in the deobf jar.
- **`minecraft:hostile` is not a vanilla entity-type tag** in 1.21+. Real vanilla entity tags include `skeletons`, `zombies`, `undead`, `illager`, `raiders`, `arthropod`, `aquatic`. There is no umbrella "hostile mobs" tag — hostility is a code-level `MobCategory.MONSTER` thing. Ship your own tag (we ship `otters_civ_revived:currency_mobs`) listing actual mob ids.
- **Do not** query tag membership through `level.registryAccess().lookupOrThrow(Registries.X).get(TagKey)` in this MC version — that view returns empty for every tag. Use `BuiltInRegistries.X.getTagOrEmpty(TagKey)` on the static registry instead.
- **Do not** short-circuit prefill when the flat reward is 0 — operators still need the id list to edit.
- **Do** hydrate on both `SERVER_STARTED` and `END_DATA_PACK_RELOAD` so `/reload` recovers without a server restart.
- **Do** log the registry's total bound-tag count whenever a lookup yields zero — that diagnostic was what cracked this. "374 bound tags but ours returns 0" pointed directly at "our tag isn't being loaded," which led to discovering the plural-vs-singular directory bug.

## Out-of-Scope Observations
- Java package/mod id still references `fpsmod`; full namespace migration not started.
- Existing feature docs describe FPS module behavior; civ module specs are in whitepaper only.

## Next Steps
1. Add richer econ permission layer (explicit nodes / Fabric Permissions API)—beyond vanilla **`GAMEMASTERS`** gate now on **`/money set`**.
2. Add `/pay` and `/ooga money set|add|take` command surface.
3. Add immutable transaction log records for balance mutations.
4. Add transfer caps/cooldowns config for abuse control.
5. Mark completed M1 checklist items as they land.
