# ROADMAP — Project OOGA

---

## Milestone Order

- [ ] M0 — Foundation and Data Integrity
- [ ] M1 — Economy MVP
- [ ] M2 — Jobs and Professions MVP
- [ ] M3 — Factions and Claims MVP
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
- [ ] Add `/pay` and `/ooga money set|add|take`
- [ ] Add immutable transaction log entries for every balance mutation

---

## M0 — Foundation and Data Integrity

### Scope
- [ ] Define module boundaries and service interfaces
- [ ] Implement persistence abstraction and schema migration framework
- [ ] Configure SQLite baseline (WAL, constraints, backup hooks)
- [ ] Stand up initial audit-log framework

### Deliverables
- [ ] `PersistenceService` + repos for player / wallet / faction / claim / shop / friend / message
- [ ] Schema version table + migration runner + startup validation
- [ ] Operator command stubs for DB status and migration state
- [ ] Structured state-mutation log format

### Acceptance Gate
- [ ] Boot with empty DB → all required schema objects created
- [ ] Incompatible schema version → fast-fail with clear log output
- [ ] Every balance write → immutable audit entry
- [ ] Hot-path reads → no main-thread blocking I/O

### Risks
- [ ] Migration drift mitigation: deterministic ledger + CI startup check
- [ ] SQLite contention mitigation: single-writer queue + retry policy

---

## M1 — Economy MVP

### Scope
- [ ] Authoritative wallet + transaction lifecycle
- [ ] Player-to-player transfers and admin money operations
- [ ] Configurable fees, taxes, money sinks

### Deliverables
- [ ] `/money`, `/pay`, `/ooga money set|add|take`
- [ ] Transaction reason enums + anti-dup safeguards
- [ ] Transfer caps, cooldowns, fee policy config
- [ ] Audit/mod views for suspicious balance movement

### Acceptance Gate
- [ ] Every balance mutation has transaction reason + record
- [ ] Double-spend prevention holds under rapid command spam
- [ ] Invalid transfers rejected with actionable feedback
- [ ] Admin can query transaction history per player / faction treasury

### Risks
- [ ] Inflation mitigation: conservative faucet defaults + visible sink toggles
- [ ] Race-condition mitigation: transactional wallet updates + row-level lock semantics

---

## M2 — Jobs and Professions MVP

### Scope
- [ ] Job enrollment and profession progression lifecycle
- [ ] Event-driven rewards with anti-farm controls
- [ ] Minimal progression feedback and UI loop

### Deliverables
- [ ] `/job join|leave|info`, `/profession info`
- [ ] Reward engine: mining / farming / hunting / crafting
- [ ] Cooldowns, diminishing returns, event validation
- [ ] Configurable XP and payout curves (data-driven via `jobs.json`)

### Acceptance Gate
- [ ] Player can join valid job path and earn expected rewards
- [ ] Basic repetitive abuse patterns blocked
- [ ] Progression persists across relog/restart
- [ ] All payouts flow through transaction service only

### Risks
- [ ] Parity mitigation: baseline reward matrix + early telemetry review
- [ ] TPS mitigation: event throttling + batched off-thread calculations

---

## M3 — Factions and Claims MVP

### Scope
- [ ] Faction lifecycle: create, membership, role permissions
- [ ] Chunk claim + protection enforcement
- [ ] Faction treasury integration

### Deliverables
- [ ] `/f create|invite|join|leave|promote|demote`
- [ ] Role-based claim interaction flags
- [ ] Claim lookup cache for hot interaction paths
- [ ] Treasury deposit/withdraw policies tied to roles

### Acceptance Gate
- [ ] Unauthorized block/place/container actions in claims consistently blocked
- [ ] Permission updates apply live without restart
- [ ] Treasury transactions auditable and role-validated
- [ ] Claim checks performant under multiplayer contention

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
- [ ] Treasury-funded faction projects with staged unlocks
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

- [ ] Testing: unit-heavy logic + targeted integration + smoke E2E
- [ ] Security: server-authoritative writes + strict input validation
- [ ] UX: actionable error messages + compact HUD signals + clear command help
- [ ] Docs: update `SUMMARY`, `SCRATCHPAD`, `SBOM`, `CHANGELOG` each milestone
- [ ] Release rhythm: branch cut + freeze window per milestone
- [ ] DoD met per milestone before marking complete

---

## Definition of Done (per milestone)

- [ ] Feature complete against scope
- [ ] Acceptance criteria verified
- [ ] No blocker-severity defects open
- [ ] Operator/config docs updated
- [ ] Migration and rollback path validated (where applicable)
