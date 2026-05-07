<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# ROADMAP - Project OOGA

[AMENDED 2026-05-07]: Checklist-first execution format added below. Use this section as the
primary tracker. Existing sections remain as detailed reference.

## Master Checklist
- [ ] M0 Foundation and Data Integrity
- [ ] M1 Economy MVP
- [ ] M2 Jobs and Professions MVP
- [ ] M3 Factions and Claims MVP
- [ ] M4 Player Shops MVP
- [ ] M5 Civ Governance Layer
- [ ] M6 Stabilization, Balance, and Scale

## ASAP Sprint Tracker - `/money` Bootstrap (2026-05-07)
- [x] Enabled server-side command support by changing mod environment to `*`.
- [x] Added wallet persistence layer (`WalletStore`, `FileWalletStore`).
- [x] Added wallet runtime service (`WalletService`) with persistent set/get.
- [x] Registered `/money` command for player balance read.
- [x] Added `/money set <player> <amount>` for rapid server-side validation.
- [x] Verified build success with `./gradlew.bat build`.
- [x] Recentered `README.md` on Project OOGA civ-mod direction and current economy bootstrap.
- [ ] Add permission gate to `/money set` (currently open during bootstrap).
- [ ] Add `/pay` and `/ooga money set|add|take` to satisfy M1 deliverables.
- [ ] Add immutable transaction log entries for every balance mutation.

## M0 Checklist - Foundation and Data Integrity
### Scope
- [ ] Define module boundaries and service interfaces.
- [ ] Implement persistence abstraction and migration framework.
- [ ] Configure SQLite baseline (WAL, constraints, backup hooks).
- [ ] Stand up initial audit-log framework.
### Deliverables
- [ ] `PersistenceService` and repositories for player/wallet/faction/claim/shop.
- [ ] Schema version table, migration runner, startup validation.
- [ ] Operator command stubs for DB status and migration state.
- [ ] Structured state-mutation log format.
### Acceptance Gate
- [ ] Boot with empty DB creates required schema.
- [ ] Incompatible schema version fails fast with clear logs.
- [ ] Balance-changing writes create immutable transaction/audit entries.
- [ ] Hot-path reads avoid main-thread blocking I/O.
### Dependencies and Risks
- [ ] Entity contracts finalized from architecture doc.
- [ ] Storage backend selection strategy confirmed.
- [ ] Mitigation in place for migration drift (deterministic ledger + CI check).
- [ ] Mitigation in place for SQLite contention (single-writer queue + retry policy).

## M1 Checklist - Economy MVP
### Scope
- [ ] Implement authoritative wallet transaction lifecycle.
- [ ] Add player transfer and admin money operations.
- [ ] Add configurable fees/taxes and initial money sinks.
### Deliverables
- [ ] Commands: `/money`, `/pay`, `/ooga money set|add|take`.
- [ ] Transaction reason enums and anti-dup safeguards.
- [ ] Transfer caps, cooldowns, fee policy config.
- [ ] Audit/mod views for suspicious balance movement.
### Acceptance Gate
- [ ] Every balance mutation has transaction reason + record.
- [ ] Double-spend prevention holds under rapid command spam.
- [ ] Invalid transfers are rejected with clear feedback.
- [ ] Admin can query player/faction treasury transaction history.
### Dependencies and Risks
- [ ] M0 persistence/audit base complete.
- [ ] Admin permission-node map complete.
- [ ] Inflation mitigation defaults tuned conservatively.
- [ ] Race-condition mitigation via transactional updates validated.

## M2 Checklist - Jobs and Professions MVP
### Scope
- [ ] Job enrollment and progression lifecycle.
- [ ] Event-driven rewards with anti-farm controls.
- [ ] Minimal progression feedback/UI loop.
### Deliverables
- [ ] Commands: `/job join|leave|info`, `/profession info`.
- [ ] Reward engine for mining/farming/hunting/crafting.
- [ ] Cooldowns, diminishing returns, and event validation.
- [ ] Configurable XP and payout curves.
### Acceptance Gate
- [ ] Players can join valid job path and earn expected rewards.
- [ ] Basic repetitive abuse patterns are blocked.
- [ ] Progression persists across relog/restart.
- [ ] Payouts flow only through transaction service.
### Dependencies and Risks
- [ ] M1 wallet service complete.
- [ ] Event validation helper layer available.
- [ ] Job reward parity matrix reviewed.
- [ ] Event throttling protects TPS under load.

## M3 Checklist - Factions and Claims MVP
### Scope
- [ ] Faction lifecycle: create, membership, role permissions.
- [ ] Chunk claim and protection enforcement.
- [ ] Faction treasury integration.
### Deliverables
- [ ] Commands: `/f create|invite|join|leave|promote|demote`.
- [ ] Role-based claim interaction flags.
- [ ] Claim lookup cache for hot interaction paths.
- [ ] Treasury role policy for deposit/withdraw.
### Acceptance Gate
- [ ] Unauthorized interactions in claims are consistently blocked.
- [ ] Permission updates apply live without restart.
- [ ] Treasury transactions remain auditable and role-validated.
- [ ] Claim checks remain performant under multiplayer contention.
### Dependencies and Risks
- [ ] M1 transaction framework complete.
- [ ] M2 identity/progression model integrated.
- [ ] Claim denial precedence rules tested.
- [ ] Cache invalidation strategy validated.

## M4 Checklist - Player Shops MVP
### Scope
- [ ] Listing, purchase, and stock-management flow.
- [ ] Tax and anti-manipulation controls.
- [ ] First GUI-centric market workflow.
### Deliverables
- [ ] Shop primitives: listing create/buy/close/restock.
- [ ] Escrow and rollback-safe purchase flow.
- [ ] Screen Handler market UI.
- [ ] Listing caps and tax-rate config.
### Acceptance Gate
- [ ] Purchase is atomic: currency + inventory transfer or rollback.
- [ ] Listings survive restart/offline-owner scenarios.
- [ ] Tax sink reports in economy analytics.
- [ ] Core market flow is UI-first and usable.
### Dependencies and Risks
- [ ] M1 wallet/transaction guarantees complete.
- [ ] Inventory + permission validation utilities complete.
- [ ] Item metadata exploit guards in place.
- [ ] Listing-spam controls tuned.

## M5 Checklist - Civ Governance Layer
### Scope
- [ ] Diplomacy state system.
- [ ] Territory projects and regional bonuses.
- [ ] Governance controls for long-term play.
### Deliverables
- [ ] Diplomacy states: ally/neutral/rival/war.
- [ ] Treasury-funded faction projects with staged unlocks.
- [ ] Optional regional buffs with upkeep tie-in.
- [ ] Governance policy commands and role checks.
### Acceptance Gate
- [ ] Diplomacy state correctly changes interaction/conflict rules.
- [ ] Project unlocks are deterministic and reversible.
- [ ] Governance actions are permission-scoped and auditable.
### Dependencies and Risks
- [ ] Stable faction/claim model from M3.
- [ ] Economic durability from M1 and M4.
- [ ] Snowball mitigation knobs validated.
- [ ] Complexity control via progressive unlocks validated.

## M6 Checklist - Stabilization, Balance, and Scale
### Scope
- [ ] Load testing and exploit hardening.
- [ ] PostgreSQL backend completion and migration tooling.
- [ ] Release candidate hardening.
### Deliverables
- [ ] Performance harness + synthetic scenarios.
- [ ] Telemetry dashboards (latency/errors/throughput).
- [ ] SQLite -> PostgreSQL migration + verification.
- [ ] Release checklist + upgrade notes.
### Acceptance Gate
- [ ] No critical duping/data-loss exploit in adversarial tests.
- [ ] Performance targets met for expected server profile.
- [ ] Migration verification passes row-count + checksum checks.
- [ ] Operator playbook complete (backup/restore/incident).
### Dependencies and Risks
- [ ] M0-M5 functional completion.
- [ ] Scale race-condition soak testing complete.
- [ ] Migration dry-run and rollback checkpoints validated.

## Cross-Cutting Checklist
- [ ] Testing: unit-heavy logic + targeted integration + smoke E2E.
- [ ] Security: server-authoritative writes + strict input validation.
- [ ] UX: clear player messaging and command help.
- [ ] Docs: update `SUMMARY`, `SCRATCHPAD`, `SBOM`, `CHANGELOG` each milestone.
- [ ] Release rhythm: branch cut + freeze window per milestone.
- [ ] DoD met per milestone before marking complete.

## Purpose
This roadmap translates the Project OOGA whitepaper into an execution plan with phased deliverables,
acceptance criteria, dependencies, risks, and release gates.

## Planning Assumptions
- Baseline code is currently FPS-template-first and will be incrementally replaced.
- Target platform is modern Fabric + Java 25+.
- Initial persistence backend is SQLite; PostgreSQL support is added as scale matures.
- Delivery prioritizes playable increments over architectural completeness.

## Milestone Timeline (Execution Order)
1. M0 Foundation and Data Integrity
2. M1 Economy MVP
3. M2 Jobs and Professions MVP
4. M3 Factions and Claims MVP
5. M4 Player Shops MVP
6. M5 Civ Governance Layer
7. M6 Stabilization, Balance, and Scale

---

## M0 - Foundation and Data Integrity

### Scope
- Internal module boundaries and service interfaces.
- Persistence abstraction + schema migration framework.
- SQLite production baseline (WAL, constraints, backup hooks).
- Initial audit log framework.

### Deliverables
- `PersistenceService` and repositories for player, wallet, faction, claim, shop entities.
- Schema version table + migration runner + startup validation.
- Operator command stubs for DB status and migration state.
- Structured log format for state-mutating operations.

### Acceptance Criteria
- Server boots with empty database and creates all required schema objects.
- Corrupted or incompatible schema version fails fast with clear error output.
- Balance-changing write path records immutable transaction/audit entries.
- Hot-path reads complete without main-thread blocking I/O.

### Dependencies
- Finalized entity contracts from architecture doc.
- Config strategy for storage backend selection.

### Risks and Mitigations
- Risk: migration drift across environments.
  - Mitigation: deterministic migration ledger + CI startup migration test.
- Risk: SQLite lock contention.
  - Mitigation: single-writer queue + measured busy timeout + retry policy.

### Exit Gate
- Data layer survives restart/crash simulation with no ledger loss.

---

## M1 - Economy MVP

### Scope
- Authoritative wallet and transaction lifecycle.
- Player-to-player transfers and admin operations.
- Fees/taxes and first money sinks.

### Deliverables
- Commands: `/money`, `/pay`, `/ooga money set|add|take`.
- Transaction reason enums and anti-dup safeguards.
- Configurable transfer caps, cooldowns, and fee policies.
- Audit and moderation views for suspicious movement.

### Acceptance Criteria
- Every balance change has a transaction record and reason.
- Double-spend prevention validated under rapid command spam.
- Transfer constraints reject invalid operations with actionable feedback.
- Admin can query recent transactions for a player or faction treasury.

### Dependencies
- M0 persistence and audit base.
- Permission node map for admin/elevated commands.

### Risks and Mitigations
- Risk: inflation from overtuned faucets.
  - Mitigation: default conservative faucet values + visible sink toggles.
- Risk: exploit via race conditions.
  - Mitigation: transactional wallet updates + row-level lock semantics.

### Exit Gate
- Economy commands stable under synthetic load + no unreconciled balances.

---

## M2 - Jobs and Professions MVP

### Scope
- Job enrollment and profession progression.
- Event-driven reward hooks with anti-farm controls.
- Minimal progression UI/feedback loop.

### Deliverables
- Commands: `/job join|leave|info`, `/profession info`.
- Reward engine for core loops: mining, farming, hunting, crafting.
- Cooldowns, diminishing returns, and location/target validation.
- Configurable XP and payout curves.

### Acceptance Criteria
- Players can join one valid job path and earn rewards from intended events.
- Anti-farm checks block at least basic repetitive abuse patterns.
- Progression values persist across relog/restart.
- Economy integration reflects payouts through transaction service only.

### Dependencies
- M1 wallet service.
- Event interception + standardized validation helper layer.

### Risks and Mitigations
- Risk: one job dominates economy.
  - Mitigation: baseline reward parity matrix + early telemetry review.
- Risk: event spam impacts TPS.
  - Mitigation: event throttling + batched calculations off-thread.

### Exit Gate
- At least 5 balanced profession tracks playable on live test server.

---

## M3 - Factions and Claims MVP

### Scope
- Faction creation, membership, and role permissions.
- Chunk claims and protection enforcement.
- Faction treasury linkage.

### Deliverables
- Commands: `/f create|invite|join|leave|promote|demote`.
- Claim system with role-based interaction flags.
- Claim lookup cache optimized for interaction hot path.
- Faction treasury deposit/withdraw policies tied to roles.

### Acceptance Criteria
- Unauthorized block/place/container actions in claims are blocked consistently.
- Faction permissions update in real time without restart.
- Treasury transactions are auditable and permission-validated.
- Claim map remains performant during active multiplayer contention.

### Dependencies
- M1 transaction framework.
- M2 player progression identity model.

### Risks and Mitigations
- Risk: false-positive claim denials.
  - Mitigation: explicit precedence rules + debug trace mode.
- Risk: claim lookup latency.
  - Mitigation: chunk-indexed cache + bounded invalidation strategy.

### Exit Gate
- Two+ factions can coexist, claim, and enforce boundaries without regression.

---

## M4 - Player Shops MVP

### Scope
- Listing, purchasing, and stock management.
- Taxation and anti-manipulation controls.
- First GUI-centric market workflow.

### Deliverables
- Shop primitives: create listing, buy item, close listing, restock.
- Escrow flow for purchase integrity and rollback-safe failure handling.
- Basic market UI via Screen Handler.
- Server-configurable listing caps and tax rates.

### Acceptance Criteria
- Purchase atomically transfers currency + inventory or fails with rollback.
- Listing lifecycle survives restarts and owner offline states.
- Tax sink integrates with economic analytics.
- UI supports core flow without command-only dependency.

### Dependencies
- M1 wallet + transaction guarantees.
- Inventory validation utilities and claim/faction permission checks.

### Risks and Mitigations
- Risk: market exploits via invalid stack metadata.
  - Mitigation: strict item validation and canonical serialization checks.
- Risk: shop spam clutter.
  - Mitigation: listing caps, cooldowns, and optional fees.

### Exit Gate
- End-to-end buy/sell loop works reliably in multiplayer stress test.

---

## M5 - Civ Governance Layer

### Scope
- Diplomacy and faction-level strategic systems.
- Territory projects and regional bonuses.
- Governance controls for long-term retention loops.

### Deliverables
- Diplomacy states: ally, neutral, rival, war.
- Faction projects funded through treasury with staged unlocks.
- Optional regional buffs tied to territory and upkeep.
- Governance commands for policy voting or leader-defined rules.

### Acceptance Criteria
- Diplomacy changes affect allowed interactions and conflict rules correctly.
- Project unlocks produce deterministic, reversible state changes.
- Governance actions are permission-scoped and auditable.

### Dependencies
- Stable faction and claim model from M3.
- Economic durability from M1/M4.

### Risks and Mitigations
- Risk: runaway snowball factions.
  - Mitigation: upkeep scaling + contested-zone balancing knobs.
- Risk: rule complexity overwhelms players.
  - Mitigation: progressive unlocks + sane default policy templates.

### Exit Gate
- Full civ loop (economy -> profession -> faction -> market -> governance) playable.

---

## M6 - Stabilization, Balance, and Scale

### Scope
- Load testing, balancing, exploit hardening, and operator tooling maturity.
- PostgreSQL backend completion and migration path.
- Release candidate hardening.

### Deliverables
- Performance test harness and synthetic scenario scripts.
- Telemetry dashboards: latency, error rates, transaction throughput.
- SQLite -> PostgreSQL migration tooling and verification checks.
- Release checklist and upgrade notes.

### Acceptance Criteria
- No critical duplication/data-loss exploit in adversarial test pass.
- Performance targets met for expected server profile.
- Migration completes with row-count and checksum verification.
- Operator playbook documented for backup, restore, incident triage.

### Dependencies
- Functional completion of M0-M5.

### Risks and Mitigations
- Risk: hidden race conditions at scale.
  - Mitigation: soak tests + chaos/failure injection for persistence paths.
- Risk: migration edge-case data drift.
  - Mitigation: dry-run validation + reversible migration checkpoints.

### Exit Gate
- Release candidate approved for public multiplayer deployment.

---

## Cross-Cutting Workstreams
- Testing: unit-heavy business logic, targeted integration tests, minimal E2E smoke runs.
- Security: strict server-authoritative mutations, input validation, anti-exploit policy updates.
- UX: actionable error messages, compact HUD signals, clear command help output.
- Docs: keep `SUMMARY`, `SCRATCHPAD`, `SBOM`, and changelog current each milestone.

## Release Rhythm
- Milestone branch cut at each exit gate.
- One integration freeze window per milestone for stability checks.
- Patch releases for exploits and data-integrity issues ship immediately.

## Definition of Done (Per Milestone)
- Feature complete against scope.
- Acceptance criteria verified.
- No blocker severity defects open.
- Operator/config documentation updated.
- Migration and rollback path validated where applicable.
