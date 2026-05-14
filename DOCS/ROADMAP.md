# ROADMAP — Project OOGA

---

## Milestone Order

- [x] M0 — Foundation and Data Integrity
- [x] M1 — Economy MVP
- [x] M2 — Jobs and Professions MVP
- [x] M3 — Guilds and Claims MVP
- [ ] M4 — Player Shops MVP
- [ ] M4.5 — Social Layer (Friends + Private Messages)
- [ ] M5 — Civ Governance Layer
- [ ] M6 — Stabilization, Balance, and Scale

---

## ASAP Sprint — `/money` Bootstrap (shipped 2026-05-07)

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

### Scope
- [x] Define module boundaries and service interfaces
- [ ] Implement persistence abstraction and schema migration framework
- [ ] Configure SQLite baseline (WAL, constraints, backup hooks)
- [ ] Stand up initial audit-log framework

### Deliverables
- [x] `IEconomyService`, `IJobsService`, `IGuildService` interfaces with clean module boundaries
- [x] Cross-module deps go through interfaces (GuildService → IEconomyService)
- [x] Circular dependency eliminated (JobsService → ottersciv/config extracted to `JobRewardDiagnostics`)
- [x] Each service has its own logger (no static OogaMod.LOGGER coupling)
- [x] Atomic crash-safe writes (`AtomicFileWriter`) deployed to all FileStores
- [ ] `PersistenceService` + repos for player / wallet / guild / claim / shop / friend / message
- [ ] Schema version table + migration runner + startup validation
- [ ] Operator command stubs for DB status and migration state
- [ ] Structured state-mutation log format

### Acceptance Gate
- [x] Module boundaries defined: economy, jobs, guilds each expose interface
- [ ] Boot with empty DB → all required schema objects created
- [ ] Incompatible schema version → fast-fail with clear log output
- [x] Every balance write → immutable audit entry (`TransactionLog` + `LedgerEntry`)
- [ ] Hot-path reads → no main-thread blocking I/O

### Risks
- [ ] Migration drift mitigation: deterministic ledger + CI startup check
- [ ] SQLite contention mitigation: single-writer queue + retry policy

---

## M1 — Economy MVP (shipped 2026-05-07)

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
- [ ] Double-spend prevention holds under rapid command spam
- [ ] Invalid transfers rejected with actionable feedback
- [x] Admin can query transaction history per player

### Risks
- [ ] Inflation mitigation: conservative faucet defaults + visible sink toggles
- [ ] Race-condition mitigation: transactional wallet updates + row-level lock semantics

---

## M2 — Jobs and Professions MVP (shipped 2026-05-12)

### Scope
- [x] Job enrollment and profession progression lifecycle
- [x] Event-driven rewards with configurable triggers
- [x] Progression feedback, HUD, and UI

### Deliverables
- [x] `/job join|leave|info|list|stats|reload|validate`
- [x] Reward engine: block break / mob kill events with XP and payout boosts
- [x] Cooldowns, configurable triggers, tag-based matching
- [x] Configurable XP and payout curves (`config/otters_civ_revived/jobs.json`)
- [x] Jobs HUD overlay with server-synced catalog and status
- [x] `/otter` JOBS tab with preview, pageable catalog, join/leave/info buttons

### Acceptance Gate
- [x] Player can join valid job and earn expected rewards
- [x] Progression persists across relog/restart
- [x] All payouts flow through economy service
- [ ] Basic repetitive abuse patterns blocked (diminishing returns — planned)

### Risks
- [x] Parity mitigation: baseline reward matrix + early telemetry review
- [ ] TPS mitigation: event throttling + batched off-thread calculations

---

## M3 — Guilds and Claims MVP (shipped 2026-05-14)

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
- [ ] Treasury transactions auditable and role-validated
- [ ] In-world visual chunk borders (done: particles on `/guild map`)

### Risks
- [ ] False-positive denial mitigation: explicit precedence rules + debug trace mode
- [ ] Lookup latency mitigation: chunk-indexed cache + bounded invalidation strategy

---

## M4 — Player Shops MVP

### Scope
- [ ] Listing, purchase, and stock-management flow
- [ ] Tax and anti-manipulation controls
- [ ] First GUI-centric market workflow

### Deliverables
- [ ] Shop primitives: create listing / buy item / close listing / restock
- [ ] Escrow + rollback-safe purchase flow
- [ ] Screen Handler market UI
- [ ] Server-configurable listing caps and tax rates

### Acceptance Gate
- [ ] Purchase atomically transfers currency + inventory or fully rolls back
- [ ] Listings survive restart and offline-owner scenarios
- [ ] Tax sink reports in economy analytics
- [ ] Core market flow is UI-first (no command-only dependency)

### Risks
- [ ] Dupe exploit mitigation: strict item NBT validation + canonical serialization checks
- [ ] Shop spam mitigation: listing caps + cooldowns + optional fees

---

## M4.5 — Social Layer (Friends + Private Messages)

### Scope
- [ ] Friends list: add / accept / remove / block
- [ ] Private messaging: send / reply / ignore
- [ ] Block list shared between friends and messaging

### Deliverables — Friends
- [ ] `FriendService` + `FriendRecord` (per-player: uuid → list\<uuid\>, status: pending/accepted/blocked)
- [ ] Persist to `SavedData` (same pattern as `JoinAttendanceSavedData`) or new repo
- [ ] `/friend add <name>` — sends request
- [ ] `/friend accept <name>` — confirm pending request
- [ ] `/friend remove <name>` — remove accepted friend
- [ ] `/friend block <name>` — block; hides requests + msgs from that player
- [ ] `/friend list` — show friends, pending requests, blocked
- [ ] Offline-safe: queue pending requests; deliver on login
- [ ] Online presence indicator in `/friend list`

### Deliverables — Private Messages
- [ ] `MessageService` — direct send via `ServerPlayerEntity.sendMessage`
- [ ] Last-sender tracking per session (for `/r` reply)
- [ ] `/msg <player> <text>` — send private message
- [ ] `/r <text>` — reply to last sender
- [ ] `/ignore <player>` — mute incoming messages (separate from block)
- [ ] Rate-limit: max N messages per M seconds per player (anti-spam)
- [ ] Respect friend block list: blocked player cannot msg you
- [ ] Server-side audit log toggle for moderators
- [ ] Optional: persist offline inbox; deliver on login (configurable)

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

### Scope
- [ ] Diplomacy state system
- [ ] Territory projects and regional bonuses
- [ ] Governance controls for long-term play

### Deliverables
- [ ] Diplomacy states: ally / neutral / rival / war
- [ ] Treasury-funded guild projects with staged unlocks
- [ ] Optional regional buffs with upkeep tie-in
- [ ] Governance policy commands and role checks

### Acceptance Gate
- [ ] Diplomacy state correctly changes interaction and conflict rules
- [ ] Project unlocks are deterministic and reversible
- [ ] Governance actions are permission-scoped and auditable

### Risks
- [ ] Snowball mitigation: upkeep scaling + contested-zone balancing knobs
- [ ] Complexity mitigation: progressive unlocks + sane default policy templates

---

## M6 — Stabilization, Balance, and Scale

### Scope
- [ ] Load testing and exploit hardening
- [ ] PostgreSQL backend + migration tooling
- [ ] Release candidate hardening

### Deliverables
- [ ] Performance harness + synthetic scenario scripts
- [ ] Telemetry dashboards (latency / errors / throughput)
- [ ] SQLite → PostgreSQL migration tooling + verification checks
- [ ] Release checklist and operator upgrade notes

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

- [x] Testing: unit-heavy logic + targeted integration + smoke E2E
- [x] Security: server-authoritative writes + strict input validation
- [x] UX: actionable error messages + compact HUD signals + clear command help
- [x] Docs: update `SUMMARY`, `SCRATCHPAD`, `SBOM`, `CHANGELOG`, `LOCATIONS.md` each milestone
- [ ] Release rhythm: branch cut + freeze window per milestone
- [ ] DoD met per milestone before marking complete

---

## Definition of Done (per milestone)

- [x] Feature complete against scope
- [x] Acceptance criteria verified
- [x] No blocker-severity defects open
- [x] Operator/config docs updated
- [ ] Migration and rollback path validated (where applicable)
