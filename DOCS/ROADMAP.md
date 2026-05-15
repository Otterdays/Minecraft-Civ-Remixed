# ROADMAP — Project OOGA

**Completion gauges:** Under each major `##` heading, the blockquote line counts every `- [x]`
/ `- [ ]` bullet in that section’s **Scope**, **Deliverables** (including `Deliverables — …`
blocks), **Acceptance Gate**, and **Risks** only (prose like `[AMENDED …]` does not count).
Percent = `round(100 × done / total)`. **Milestone Order** is scored as shipped milestones
checked ÷ 8.

---

## Milestone Order

> **~44%** · shipped milestones **4 / 9** (M0–M3 ✓ · M3.5–M6 + M4.5 open)

- [x] M0 — Foundation and Data Integrity
- [x] M1 — Economy MVP
- [x] M2 — Jobs and Professions MVP
- [x] M3 — Guilds and Claims MVP
- [ ] M3.5 — Job Loot Layer (Bonus Drops + Exclusive Yields)
- [ ] M4 — Player Shops MVP
- [ ] M4.5 — Social Layer (Friends + Private Messages)
- [ ] M5 — Civ Governance Layer
- [ ] M6 — Stabilization, Balance, and Scale

---

## ASAP Sprint — `/money` Bootstrap (shipped 2026-05-07)

> **100%** · checklist **10 / 10**

- [x] Set mod environment to `*` (enable server-side commands)
- [x] Add `WalletStore` / `FileWalletStore` persistence layer
- [x] Add `WalletService` with persistent set/get
- [x] Register `/money` command for player balance read
- [x] Add `/money set <player> <amount>` (gamemaster-gated)
- [x] Verify build passes `./gradlew.bat build`
- [x] Rewrite `README.md` around Project OOGA civ direction
- [x] Gate `/money set` behind `PermissionLevel.GAMEMASTERS`
- [x] Add `/pay` and `/money add|take` (admin ops)
- [x] Add immutable transaction log entries for every balance mutation

---

## M0 — Foundation and Data Integrity (shipped 2026-05-14)

> **100%** · checklist **20 / 20** (Scope 4/4 · Deliverables 9/9 · Acceptance 5/5 · Risks 2/2)

[AMENDED 2026-05-14]: **Whitepaper Phase 0** and this milestone are **100% complete** for their
defined scope. Follow-on persistence (shop/social repos → **M4** / **M4.5**; Postgres/dialect → **M6**)
lives only in those milestones.

### Scope
- [x] Define module boundaries and service interfaces
- [x] Implement persistence abstraction and schema migration framework
- [x] Configure SQLite baseline (WAL, constraints, backup hooks)
- [x] Stand up initial audit-log framework

### Deliverables
- [x] `IEconomyService`, `IJobsService`, `IGuildService` interfaces with clean module boundaries
- [x] Cross-module deps go through interfaces (GuildService → IEconomyService)
- [x] Circular dependency eliminated (JobsService → ottersciv/config extracted to `JobRewardDiagnostics`)
- [x] Each service has its own logger (no static OogaMod.LOGGER coupling)
- [x] Atomic crash-safe writes (`AtomicFileWriter`) deployed to all FileStores
- [x] `PersistenceService` with SQLite-backed stores (wallet, ledger, guilds, claims, jobs_state)
- [x] Schema version table (`schema_version`) + migration runner (`SchemaMigrator`) + startup validation
- [x] Operator command stubs: `/ooga db status|migrate`
- [x] Structured state-mutation log format (SQLite `wallet_ledger` table with id/delta/balance_after/reason/note/timestamp)

### Acceptance Gate
- [x] Module boundaries defined: economy, jobs, guilds each expose interface
- [x] Boot with empty DB → all required schema objects created (auto-migration on startup)
- [x] Incompatible schema version → fast-fail with clear log output (version > CURRENT_VERSION check)
- [x] Every balance write → immutable audit entry (`wallet_ledger` table with reason + note)
- [x] Hot-path reads → no main-thread blocking I/O (in-memory ConcurrentHashMap caches in services, SQLite for persistence only)

### Risks
- [x] Migration drift mitigation (bounded): `gradlew test` runs `SqlitePersistenceIntegrationTest`
  (fresh DB migrations, PRAGMAs, wallet/ledger/guild/jobs round-trips). Optional hardening: widen
  CI matrix + publish operator rollback notes (restore prior `project_ooga.db` + pinned jar;
  SQL migrations remain forward-only).
- [x] SQLite contention mitigation: WAL mode + busy_timeout=5000 + synchronous=NORMAL

---

## M1 — Economy MVP (shipped 2026-05-07)

> **100%** · checklist **14 / 14** (Scope 3/3 · Deliverables 4/4 · Acceptance 5/5 · Risks 2/2)

### Scope
- [x] Authoritative wallet + transaction lifecycle
- [x] Player-to-player transfers and admin money operations
- [x] Configurable fees, taxes, money sinks

### Deliverables
- [x] `/money`, `/money set <player> <amount>`
- [x] Transaction reason enums + anti-dup safeguards
- [x] Transfer caps, cooldowns, fee policy config (`EconomyConfig`)
- [x] Audit/mod views for suspicious balance movement (`/economy log`)

### Acceptance Gate
- [x] Every balance mutation has transaction reason + record
- [x] Double-spend prevention holds under rapid command spam
- [x] Invalid transfers rejected with actionable feedback
- [x] Admin can query transaction history per player

### Risks
- [x] Inflation mitigation: conservative faucet defaults + visible sink toggles
- [x] Race-condition mitigation: transactional wallet updates + row-level lock semantics

---

## M2 — Jobs and Professions MVP (shipped 2026-05-12)

> **100%** · checklist **18 / 18** (Scope 3/3 · Deliverables 9/9 · Acceptance 4/4 · Risks 2/2)

### Scope
- [x] Job enrollment and profession progression lifecycle
- [x] Event-driven rewards with configurable triggers
- [x] Progression feedback, HUD, and UI

### Deliverables
- [x] `/job join|leave|info|list|stats|reload|validate`
- [x] Reward engine: block break / mob kill events with XP and payout boosts
- [x] Cooldowns, configurable triggers, tag-based matching
- [x] Configurable XP and payout curves (`config/otters_civ_revived/jobs.json`)
- [x] Shipped starter pack tuned to 5 low-overlap roles (`miner`, `lumberjack`, `farmer`, `excavator`, `fighter`) with single-slot defaults
- [x] Jobs HUD overlay with server-synced catalog and status
- [x] `/otter` JOBS tab with preview, pageable catalog, join/leave/info buttons
- [x] Sliding-window diminishing returns (anti-grind) tunable in `jobs.json`
- [x] Batched persistence (`persistEveryNEvents`) with shutdown / reload flush

### Acceptance Gate
- [x] Player can join valid job and earn expected rewards
- [x] Progression persists across relog/restart
- [x] All payouts flow through economy service
- [x] Basic repetitive abuse patterns blocked (sliding-window diminishing returns: linear ramp from `antiAbuseSoftCap` → `antiAbuseFloor` at `antiAbuseHardCap`)

### Risks
- [x] Parity mitigation: baseline reward matrix + early telemetry review
- [x] TPS mitigation: event throttling (per-trigger `cooldownMs` + sliding-window cap) + batched persistence (`persistEveryNEvents` flushes on shutdown/reload)

---

## M3 — Guilds and Claims MVP (shipped 2026-05-14)

> **100%** · checklist **21 / 21** (Scope 3/3 · Deliverables 11/11 · Acceptance 5/5 · Risks 2/2)

### Scope
- [x] Guild lifecycle: create, membership, officer ranks
- [x] Chunk claim + protection enforcement
- [x] Guild home teleport, chunk map, visual borders

### Deliverables
- [x] `/guild create|disband|invite|join|leave|kick`
- [x] `/guild promote|demote` — officer rank system
- [x] `/guild claim|unclaim|unclaimall` — chunk claims ($100/ea, max 16 configurable)
- [x] `/guild sethome|home` — guild teleport point
- [x] `/guild map` — ASCII grid + client-side GUI overlay (30s) + particle chunk borders
- [x] `/guild info|list` — guild details and server-wide listing
- [x] Chunk protections — block break, block place, container use blocked for non-members
- [x] Config via `config/otters_civ_revived/guilds.json` (costs, limits, protection toggles)
- [x] Dedicated GUILDS tab in `/otter` with live synced guild info and quick-action buttons
- [x] Chunk border particles (end rod = own guild, flame = other guild)
- [x] Guild status sync to client on join + after mutations

### Acceptance Gate
- [x] Unauthorized block/place/container actions in claimed chunks consistently blocked
- [x] Permission updates apply live without restart
- [x] Claim checks performant under multiplayer contention (ConcurrentHashMap-based)
- [x] Treasury transactions auditable and role-validated
- [x] In-world visual chunk borders (done: particles on `/guild map`)

### Risks
- [x] False-positive denial mitigation: explicit precedence rules + debug trace mode
- [x] Lookup latency mitigation: chunk-indexed cache + bounded invalidation strategy

---

## M3.5 — Job Loot Layer (Bonus Drops + Exclusive Yields)

> **0%** · checklist **0 / 16** (Scope 0/3 · Deliverables 0/7 · Acceptance 0/4 · Risks 0/2)

The classic Jobs Reborn / mcMMO mechanic: members of a job get extra or exclusive drops from
their job-relevant blocks and mobs. Higher job level = higher drop chance or quantity. Some
items are **job-gated** — unattainable without the right job. Creates real demand for
specialized labor and feeds directly into the player shop economy.

### Scope
- [ ] Bonus drop engine: job members get additional loot rolls on relevant block-break / mob-kill
- [ ] Exclusive drop table: certain items only fall for players actively enrolled in the matching job
- [ ] Level-scaled rates: drop chance and quantity scale with the player's job level

### Deliverables
- [ ] `bonusDrops` block in each job entry in `jobs.json`: list of `{trigger, item, minQty,
  maxQty, basChance, levelScaling}` entries — same trigger format already used by the reward
  engine, zero new event hooks needed
- [ ] `exclusiveDrops` block: same schema but items are suppressed entirely for non-job players
  (server-side item spawn; never touches the normal loot table)
- [ ] Level-scaling formula: `finalChance = baseChance + (jobLevel × levelScaling)` capped
  at `maxChance` — all values operator-tunable
- [ ] Starter exclusive drops wired into the shipped 5-job pack:
  - `miner` → chance for raw ore double-drop + exclusive **Rich Ore Fragment** (craft into
    bonus ingots)
  - `lumberjack` → chance for extra sapling/apple + exclusive **Hardwood Plank** (better
    fuel / crafting material)
  - `farmer` → chance for double crop yield + exclusive **Plump Seeds** (faster-growing
    plant variant)
  - `excavator` → extra gravel/sand drops + exclusive **Packed Silt** (compactable
    building block)
  - `fighter` → extra mob loot rolls + exclusive **Predator's Trophy** (mob-specific token
    redeemable at NPC shops for bonus coins)
- [ ] Chat/action-bar pop on exclusive drop: `§6[Job Perk] You found a Rich Ore Fragment!`
- [ ] `/job info` updated to list bonus and exclusive drops for the player's active job

### Acceptance Gate
- [ ] Non-job player breaks a job-gated block → zero exclusive drops, normal vanilla loot only
- [ ] Job member at level 1 vs level 50 → measurably different drop rate (log output in debug
  mode confirms formula)
- [ ] All drops route through economy/loot service — no direct inventory injection bypassing
  audit trail
- [ ] `jobs.json` reload (`/job reload`) picks up drop table changes without restart

### Risks
- [ ] Inflation mitigation: exclusive items must feed into sinks (shop fees, crafting recipes,
  NPC redemption) — default rates conservative, operator-tunable
- [ ] Abuse mitigation: bonus drops share the existing sliding-window anti-grind cap; a
  dedicated `bonusDropCooldownMs` per trigger prevents clock-tick spam

---

## M4 — Player Shops MVP

> **~13%** · checklist **2 / 16** (Scope 0/3 · Deliverables 1/5 · Acceptance 1/4 · Risks 0/2)

### Scope
- [ ] Listing, purchase, and stock-management flow
- [ ] Tax and anti-manipulation controls
- [ ] First GUI-centric market workflow

### Deliverables
- [x] Persistence layer for shop entities (listings, stock, escrow / purchase records) —
  schema v2 `shop_listings` table + `ShopStore` interface + `SqliteShopStore` with atomic
  guarded-UPDATE stock decrement (state='OPEN' AND stock>=units), wired into
  `PersistenceService.shopStore()`. Round-trip + oversell-prevention covered by
  `SqlitePersistenceIntegrationTest.shopStorePersistsAndAtomicallyDecrementsStock`.
- [ ] Shop primitives: create listing / buy item / close listing / restock
- [ ] Escrow + rollback-safe purchase flow
- [ ] Screen Handler market UI
- [ ] Server-configurable listing caps and tax rates
- [ ] Guild service NPC framework: hire Quartermaster / Armorer / Skill Trainer via guild
  treasury; persistent spawn tied to guild home chunk; per-NPC upkeep ticks against treasury
- [ ] Daily player earnings cap (`maxDailyCoinsFromRewards` in `economy.json`) tracked in
  SQLite per-UUID with midnight reset — chokes botting + farm-loop inflation
- [ ] Recycler station: convert N×rotten-flesh / bones into crafting catalysts with a small
  fee (soft money sink + cleans up inventories)
- [ ] Damage-gated loot for shared bosses ("5% rule"): per-fight damage map per UUID;
  rewards only granted to contributors at/above the threshold
- [ ] Instanced loot delivery: server spawns drops visible only to the eligible player UUID
  via packet filtering — prevents loot-stealing on shared boss kills

### Acceptance Gate
- [ ] Purchase atomically transfers currency + inventory or fully rolls back
- [x] Listings survive restart and offline-owner scenarios (rows live in `shop_listings`; reload via `ShopStore.find` / `loadForOwner` does not require the owner to be online)
- [ ] Tax sink reports in economy analytics
- [ ] Core market flow is UI-first (no command-only dependency)

### Risks
- [ ] Dupe exploit mitigation: strict item NBT validation + canonical serialization checks
- [ ] Shop spam mitigation: listing caps + cooldowns + optional fees

---

## M4.5 — Social Layer (Friends + Private Messages)

> **0%** · checklist **0 / 34** (Scope 0/3 · Friends 0/9 · Private Messages 0/11 · UX 0/4 · Acceptance 0/5 · Risks 0/2)

### Scope
- [ ] Friends list: add / accept / remove / block
- [ ] Private messaging: send / reply / ignore
- [ ] Block list shared between friends and messaging

### Deliverables — Friends
- [ ] `FriendService` + `FriendRecord` (per-player: uuid → list\<uuid\>, status: pending/accepted/blocked)
- [ ] Friend persistence for graph + requests + blocks — `SavedData` (same pattern as
  `JoinAttendanceSavedData`) and/or SQLite tables with the same migration discipline as M0/M4
- [ ] `/friend add <name>` — sends request
- [ ] `/friend accept <name>` — confirm pending request
- [ ] `/friend remove <name>` — remove accepted friend
- [ ] `/friend block <name>` — block; hides requests + msgs from that player
- [ ] `/friend list` — show friends, pending requests, blocked
- [ ] Offline-safe: queue pending requests; deliver on login
- [ ] Online presence indicator in `/friend list`

### Deliverables — Private Messages
- [ ] Persistence layer for DM sessions / ignore state / optional offline inbox (same stores
  as friends where practical)
- [ ] `MessageService` — direct send via `ServerPlayerEntity.sendMessage`
- [ ] Last-sender tracking per session (for `/r` reply)
- [ ] `/msg <player> <text>` — send private message
- [ ] `/r <text>` — reply to last sender
- [ ] `/ignore <player>` — mute incoming messages (separate from block)
- [ ] Rate-limit: max N messages per M seconds per player (anti-spam)
- [ ] Respect friend block list: blocked player cannot msg you
- [ ] Server-side audit log toggle for moderators
- [ ] Optional: persist offline inbox; deliver on login (configurable)
- [ ] Fame/reputation system: `/fame <player> [up|down]` once per real-world day; persisted
  on the player row (`fame_score`, `last_fame_timestamp`). Negative fame can lock premium
  NPC shop access; positive fame unlocks prestige cosmetics (configurable thresholds)
- [ ] Guild chat channel + officer-only channel routed through the same message service
  (respects ignore/block lists)

### Deliverables — UX
- [ ] New FRIENDS tab in `OttersCivScreen` (status chips: LIVE/PENDING/BLOCKED)
- [ ] `/otter` HELP catalog entries for all new commands
- [ ] `index.html` parity: commands listed, CIV milestone card for M4.5
- [ ] `DOCS/CHANGELOG.md` `[Unreleased]` entries per shipped item

### Acceptance Gate
- [ ] Friend request requires mutual consent (no one-sided adds)
- [ ] Block prevents all messages and friend requests from target
- [ ] Rate-limit enforced server-side (cannot be bypassed client-side)
- [ ] Offline queued requests survive restart and deliver correctly
- [ ] Msg/ignore is session-auth only — server always decides delivery

### Risks
- [ ] Spam mitigation: rate-limit defaults + `/ignore` available from day one
- [ ] Privacy mitigation: opt-in online presence display (per-player toggle)

---

## M5 — Civ Governance Layer

> **0%** · checklist **0 / 12** (Scope 0/3 · Deliverables 0/4 · Acceptance 0/3 · Risks 0/2)

### Scope
- [ ] Diplomacy state system
- [ ] Territory projects and regional bonuses
- [ ] Governance controls for long-term play

### Deliverables
- [ ] Diplomacy states: ally / neutral / rival / war
- [ ] Treasury-funded guild projects with staged unlocks
- [ ] Optional regional buffs with upkeep tie-in
- [ ] Governance policy commands and role checks
- [ ] **Guild upgrade tree** (`upgrades.json`): `logistics` (Quartermaster discounts, Runic
  Waypoint home-cooldown removal) and `warfare` (Throne fortification, Hall Sentry golems)
  categories; level-gated by new `guild_level` column on `guilds` table
- [ ] **Guild Lord protector NPC**: spawn on `/guild sethome` (extends Villager/Iron Golem);
  invulnerable normally; vulnerable during declared-war siege window. Defeat = 1h claim lock
  for defender + auto-plunder 10% of defender treasury to attacker. Tracked in new
  `guild_protectors` table (uuid, hp, last spawn pos, is_under_attack)
- [ ] **War declaration system**: `/guild war declare <name>` with treasury cost from
  `warfare.json` (`declarationCost`, `siegeWindowTicks`, `guildLordHealth`); 24h prep
  countdown before siege window opens; cooldown to prevent harassment cycles
- [ ] **Bodyguard / Sentry NPCs**: treasury-spawned in claimed chunks; configurable
  HP/damage/follow-range; respawn after a configurable cooldown when killed mid-siege
- [ ] **War Room tab in `/otter`**: live Lord HP, vulnerability-window state, active wars,
  GvG leaderboard (wins by Lord kills), and a "War Bell" declare button
- [ ] **Hall tab in `/otter`**: visual grid of hired NPCs + purchased upgrades; current
  treasury upkeep rate; one-click summon for service NPCs
- [ ] **Faction allegiance layer**: 2–3 server-wide factions defined in `economy.json`;
  guilds pledge to one; contested zones tally weekly faction job-XP totals; winning faction
  gets a 1.2x reward multiplier in that zone the following week
- [ ] **Faction-specific resource items**: biome-locked materials (e.g., Amber / Jade)
  required for tier-3+ hall upgrades — forces inter-faction trade or territorial conquest
- [ ] **Guild contribution points + guild skills**: members generate contribution by earning
  job XP; guild master spends contribution on temporary server-wide skills
  (`guild_skills.json`) like mass-Haste, instant member recall to home during sieges
- [ ] **Hall of Monuments**: milestone trophies (1M coins earned, 50 siege wins, etc.)
  unlock unique decorative blocks, capes/emblems, and tiny persistent perks (+1% job XP)
- [ ] **Job family switching**: scaling-cost class change preserving a fraction of XP within
  a family (Miner ↔ Excavator); cost curve defined in `jobs.json` `families` block
- [ ] **Victory Emblem cosmetics**: GvG season wins grant a colored name-tag aura or
  particle trail visible in `/otter` and over the player's head

### Acceptance Gate
- [ ] Diplomacy state correctly changes interaction and conflict rules
- [ ] Project unlocks are deterministic and reversible
- [ ] Governance actions are permission-scoped and auditable

### Risks
- [ ] Snowball mitigation: upkeep scaling + contested-zone balancing knobs
- [ ] Complexity mitigation: progressive unlocks + sane default policy templates

---

## M6 — Stabilization, Balance, and Scale

> **0%** · checklist **0 / 14** (Scope 0/3 · Deliverables 0/5 · Acceptance 0/4 · Risks 0/2)

### Scope
- [ ] Load testing and exploit hardening
- [ ] Scale persistence (second JDBC backend, SQL dialects, SQLite → Postgres move — see Deliverables)
- [ ] Release candidate hardening

### Deliverables
- [ ] Performance harness + synthetic scenario scripts
- [ ] Telemetry dashboards (latency / errors / throughput)
- [ ] Pluggable JDBC backend + SQL dialect layer (PostgreSQL primary scale target; optional
  MySQL/MariaDB community path per `DOCS/whitepaper.md` persistence architecture)
- [ ] SQLite → PostgreSQL migration tooling + verification checks
- [ ] Release checklist and operator upgrade notes
- [ ] **Gear upgrade catalysts** (Sunstone / Moonstone): catalyst items defined in
  `items.json`; configurable mob-drop chances via `rewards.json`; combine-recipe for
  high-tier "Shining Oricalkum" (5 Sunstones + 5 Moonstones) gating +11..+20 tier upgrades
- [ ] **"Star Force" gear upgrading**: `/otter` Upgrade tab; +1..+3 safe, +4..+10 declining
  success, +11+ "Boom" risk → equipment-trace record. Scroll of Protection consumable
  mitigates destruction. Tracked in `item_upgrades` table to avoid NBT bloat
- [ ] **Piercing / sockets**: Moonstone on gear opens up to 4 sockets; Attribute Cards
  slot in for stat modifiers; pierce-state persisted in `item_upgrades`
  (`pierce_slots_total`, `pierce_slots_filled`, `socket_N_data`)
- [ ] **Hidden Potential / Cubing**: drops carry hidden stat lines revealed by Magnifying
  Glass; tiered Cubes (Rare/Epic/Unique) re-roll the lines using a server-controlled
  `potentials.json` catalog (no client-trustable rolls)
- [ ] **Pet system**: monster-egg drops; hatch via off-hand mob-kill incubation; 5-tier
  evolution (D→S) with hunger upkeep; perma-death at hunger zero; bonuses applied while
  summoned. New `player_pets` table (owner uuid, type, tier, xp, hunger, bonus_type)
- [ ] **Pet feed crafting + Beastmaster job**: recycle rotten flesh / bones into pet feed
  at the Recycler (shared with M4 recycler). New `beastmaster` entry in `jobs.json` reduces
  pet hunger drain and increases evolution success rate
- [ ] **Elemental combat layer**: 5-element rock-paper-scissors (Fire / Water / Wind /
  Earth / Electricity); elemental enchant cards apply to weapons + armor; advantage 1.5x,
  disadvantage 0.5x defined in `elements.json`; visual particle hints per element

### Acceptance Gate
- [ ] No critical dupe/data-loss exploit in adversarial test pass
- [ ] Performance targets met for expected server profile
- [ ] Migration verified via row-count + checksum checks
- [ ] Operator playbook complete (backup / restore / incident triage)

### Risks
- [ ] Race-condition mitigation: soak tests + failure injection for persistence paths
- [ ] Migration drift mitigation: dry-run validation + reversible checkpoints

---

## Cross-Cutting

> **~67%** · checklist **4 / 6**

- [x] Testing: unit-heavy logic + targeted integration + smoke E2E
- [x] Security: server-authoritative writes + strict input validation
- [x] UX: actionable error messages + compact HUD signals + clear command help
- [x] Docs: update `SUMMARY`, `SCRATCHPAD`, `SBOM`, `CHANGELOG`, `LOCATIONS.md` each milestone
- [ ] Release rhythm: branch cut + freeze window per milestone
- [ ] DoD met per milestone before marking complete

---

## Definition of Done (per milestone)

> **~80%** · checklist **4 / 5** (global template; migration line still open for later milestones)

- [x] Feature complete against scope
- [x] Acceptance criteria verified
- [x] No blocker-severity defects open
- [x] Operator/config docs updated
- [ ] Migration and rollback path validated (where applicable) — **M0 satisfied 2026-05-14:**
  forward-only SQL migrations + startup fast-fail on newer DB than code; operator rollback =
  restore backed-up `project_ooga.db` + matching mod version. Later milestones (e.g. M6
  Postgres) carry their own migration/rollback requirements.
