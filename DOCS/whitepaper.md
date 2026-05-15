# Project OOGA Whitepaper

## Vision
Project OOGA is the all-in-one Minecraft civilization mod platform for modern Fabric servers.
Its mission is to unify the most popular legacy gameplay loops into one coherent system:
factions, jobs, professions, economy, and player-driven shops.

The goal is not to copy old mods one-to-one. The goal is to preserve what players loved, remove
friction, and deliver a stable, scalable civ experience that feels native to current Minecraft.

## Product Thesis
- Old civ stacks are fragmented: admins install many plugins/mods and fight integration conflicts.
- Players want one clear progression path from solo survival to city-scale civilization.
- Server owners need predictable performance, anti-abuse controls, and admin-grade observability.
- Project OOGA wins by shipping a complete civ loop with clean defaults and modular expansion.

## Core Pillars
- Economy first: one authoritative wallet, deterministic transactions, and transparent money sinks.
- Identity and progression: jobs/professions that reward useful behavior and long-term play.
- Territorial gameplay: factions, claims, and diplomacy that create meaningful social stakes.
- Commerce and trade: player shops and market mechanics that keep goods and currency moving.
- Stability and trust: fail-fast data integrity, auditability, and anti-exploit protections.

## Experience Principles
- UX over elegance: actions should be obvious in-game and low-friction for new players.
- KISS and YAGNI: small reliable systems first, then iterate.
- PvP/PvE compatibility: civ systems should work on combat-heavy and chill community servers.
- Server-owner control: every economic lever and protection rule must be configurable.

## System Architecture (High Level)

### 1) Data Layer (Source of Truth)
- Persist player, faction, claim, wallet, shop, and profession data with versioned schemas.
- Use atomic writes and snapshots to prevent corruption during crashes.
- Keep migration pipeline append-only for forward compatibility.
- Expose a typed internal service API for all game systems.
- Backend is pluggable (see Data Storage Strategy) so servers can scale from solo to large populations without rewrites.

### 2) Domain Services
- Wallet service: balance updates, transfers, sinks, taxes, transaction history.
- Profession service: XP, levels, rewards, cooldowns, anti-farm validation.
- Faction service: membership, ranks, diplomacy, treasury, governance.
- Claim service: chunk ownership, flags, protection checks, siege/raid rules (configurable).
- Shop service: listings, stock, pricing rules, taxes, escrow/refund flows.

### 3) Game Event Integration
- Listen to Fabric events for blocks, entities, crafting, trading, and player interactions.
- Route events through validation + domain service layer before mutating state.
- Deny unauthorized interactions early (fail fast) with clear player feedback.

### 4) Interface Layer
- Command surface for power users and admins.
- Screen-based UX for player shops, profession progression, and faction management.
- Action bars, boss bars, and compact HUD cues for fast understanding.

### 5) Admin/Operator Layer
- Config profiles for casual, hardcore, and economy-heavy servers.
- Audit logs for money movement and permission-sensitive actions.
- Debug and metrics hooks for incident triage and balancing.

## Data Storage Strategy

Project OOGA stores authoritative civ state (wallets, factions, claims, shops, professions, audit logs)
behind a single persistence abstraction. Servers pick a backend that matches their scale, and the
mod ships with sane defaults so small servers work out of the box while large servers can scale up
without code changes.

### Design Goals
- Single typed repository API in front of every backend; game services never talk to a driver directly.
- ACID transactions for any multi-row mutation (transfers, faction treasury moves, shop escrow).
- Off-thread I/O: persistence runs on a dedicated executor; main server tick never blocks on disk or network.
- Schema versioning + forward-only migration ledger that runs on startup and is identical across backends.
- Snapshot/backup hooks usable by operators and admins without taking the server down.

### Default Backend: SQLite (Embedded)
SQLite is the default backend and ships embedded. It fits the vast majority of community and small-to-mid
servers (target: up to a few hundred concurrent players, a few million ledger rows).

Why SQLite first:
- Zero-ops: no separate process, no port, no credentials. Works on any host that can run Fabric.
- Real ACID: WAL mode gives concurrent readers + a single writer with strong durability guarantees.
- Crash safe: WAL + `synchronous=NORMAL` (or `FULL` for paranoid servers) survives power loss without corruption.
- Predictable performance: indexed lookups on hot paths (wallet read, claim lookup) stay sub-millisecond
  on a single SSD-backed file.
- Backups: `VACUUM INTO` and online backup API allow hot snapshots without freezing gameplay.

SQLite configuration we adopt:
- WAL journal mode, `synchronous=NORMAL`, `foreign_keys=ON`, `busy_timeout` tuned for tick spikes.
- One writer thread funneled through a write queue to eliminate `SQLITE_BUSY` under load.
- Read pool for non-mutating queries (claim checks, balance reads, shop lookups).
- Prepared statement cache per connection.

### Scale Backend: PostgreSQL (Recommended for Large Servers)
For large servers (500+ concurrent players, network/proxy setups, dedicated DB hosts, multi-world
clusters, or anyone wanting external observability), the same repository API targets PostgreSQL.

Why Postgres for scale:
- True multi-writer concurrency: removes the SQLite single-writer bottleneck under economic-event spikes
  (mass PvP, market rushes, faction wars).
- Mature replication, point-in-time recovery, and managed-host options (RDS, Cloud SQL, self-hosted).
- Rich indexing (partial, GIN, BRIN) for audit-log queries and analytics dashboards.
- Network reachable: lets external admin tooling, dashboards, and Discord bots read state safely
  via a read replica without touching the game server.
- Cross-server economy (a stated non-goal for the initial milestone) becomes feasible later without
  re-platforming.

Schema is kept portable: ANSI SQL where possible, vendor-specific features isolated to a thin dialect layer.

### MySQL/MariaDB Compatibility
Many existing Minecraft hosting stacks ship MariaDB. We support it as a community-tier backend through
the same dialect layer, with the caveat that Postgres remains the recommended scale target due to
stricter transactional guarantees and richer indexing.

### What We Deliberately Avoid
- **Flat JSON/YAML files for authoritative economy state.** No real transactions, easy to corrupt on
  crash, and brutal to migrate. We may still use flat files for static config and per-world tunables,
  never for wallets, ledgers, or claims.
- **NoSQL document stores as the primary backend.** Civ economy is intrinsically relational
  (transactions, foreign keys, joins on faction → member → wallet). Forcing it into documents pushes
  consistency burden onto application code.
- **Custom binary formats.** Fast to write, painful to migrate, hostile to operators who want to
  inspect or repair state.

### Scaling Triggers (When to Move from SQLite to Postgres)
Operators should consider migrating when any of the following appear in production telemetry:
- Sustained `SQLITE_BUSY`/write-queue backpressure during peak hours.
- Database file growth past ~10–20 GB or audit-log query latency degrading.
- Need for read replicas, off-box dashboards, or cross-shard economy.
- Hosting topology shifts to multi-node (proxy + multiple game servers sharing economy state).

A first-class export/import tool ships with the mod to move between backends without data loss; the
schema and migration ledger are identical, so the move is a data copy, not a rewrite.

### Operator Tooling
- `/ooga db status` — backend, schema version, pending migrations, write queue depth.
- `/ooga db backup` — snapshot to a timestamped file (SQLite) or trigger logical dump (Postgres).
- `/ooga db migrate` — dry-run + apply pending migrations with audit entry.
- Metrics: write latency p50/p95/p99, transaction rollback count, backend round-trip time.

## Feature Modules

### Economy
- Single currency ledger with transaction reasons and optional categories.
- Transfer limits, anti-duplication checks, and configurable taxes/fees.
- Money sinks (repairs, upkeep, teleport fees, claim upkeep) to avoid inflation.

### Jobs and Professions
- Jobs define broad income channels (miner, farmer, hunter, crafter, merchant).
- Professions specialize jobs with unique perks and progression tracks.
- Reward logic prioritizes anti-grind abuse controls and server balance.

### Factions and Land
- Chunk claims with role-based permissions and granular flags.
- Faction treasury and optional upkeep model tied to territory size.
- Diplomacy layer: ally, neutral, rival, war (server-rule dependent).

### Player Shops and Markets
- Chest/NPC/GUI shop models (server chooses enabled modes).
- Optional regional market bonuses to make towns and routes meaningful.
- Tax and listing constraints to discourage market manipulation.

## Progression Loop
1. Nomad: earn first currency through basic jobs.
2. Specialist: level professions for efficiency and identity.
3. Settler: buy/shop/trade and invest in tools and infrastructure.
4. Founder: create or join a faction and claim land.
5. Governor: run faction economy, logistics, diplomacy, and expansion.
6. Civilizer: build durable institutions (trade hubs, public services, alliances).

## Economy Balance Strategy
- Target moderate velocity: money should move, not stagnate.
- Keep faucets and sinks visible and tunable from config.
- Prevent single-meta dominance by diversifying profitable activities.
- Reset nothing by default; prefer controlled decay and maintenance costs.

## Security and Anti-Exploit Policy
- Every balance mutation must be validated and auditable.
- Never trust client-side state for economy-critical actions.
- Throttle or block suspicious repeated event patterns.
- Prefer typed results/errors over silent failures.

## Performance Targets
- Constant-time or near-constant-time checks on hot claim interaction paths.
- Bounded memory growth for active player data caches.
- Async-safe persistence boundaries; no main-thread blocking I/O.
- Configurable tick budgets for heavy scans and periodic jobs.

## Compatibility and Migration
- Build for modern Fabric first, with clear version support matrix.
- Provide migration tools for legacy civ ecosystems where feasible.
- Keep module toggles independent so servers can adopt incrementally.

## Delivery Roadmap

### Phase 0 - Foundation (100% complete — repo milestone **M0**)
- Relational schemas, `schema_version` + forward migration runner, SQLite default backend
  (`project_ooga.db`, WAL, busy timeout, foreign keys), store-facing interfaces with SQLite
  implementations (wallets, immutable `wallet_ledger`, guilds/claims, jobs state), operator
  `/ooga db status|migrate`, and authoritative wallet service wired through persistence.

### Phase 1 - Economy MVP
- Wallet commands, transfer logic, transaction logs, and admin controls.

### Phase 2 - Jobs/Professions MVP
- Core professions, reward hooks, anti-farm heuristics, and progression UI.

### Phase 3 - Factions and Claims MVP
- Factions, chunk claims, permission checks, treasury integration.

### Phase 4 - Player Shops MVP
- Listing, purchase, tax flow, and first market UX.
- Durable shop persistence (listings, stock, escrow state) in the civ data layer — same
  migration and backup discipline as economy/guilds.

### Phase 4.5 - Social Layer (Friends + Messages)
- Friend graph, requests/blocks, private messaging, and rate limits with persistence aligned
  to the civ data layer (SavedData and/or relational stores).

### Phase 5 - Civ Layer
- Diplomacy, faction projects, regional bonuses, and governance tooling.

### Phase 6 - Polish and Scale
- Profiling, balancing, anti-abuse hardening, and operator observability.
- **Scale persistence:** pluggable JDBC backend + dialect layer (PostgreSQL as the primary
  large-server target; optional MySQL/MariaDB community path per architecture notes), plus
  SQLite → Postgres migration/export tooling — picks up the portable SQL and repository
  boundaries shipped in Phase 0 without expanding Phase 0 scope.

## Success Metrics
- Server retention and average play-session length.
- Economic health (currency velocity, inflation trend, sink/faucet ratio).
- Faction participation rate and inter-faction trade volume.
- Shop usage and repeat trade activity.
- Admin-reported stability and moderation workload.

## Non-Goals (Current Scope)
- Full MMO quest framework.
- Complex tech-tree simulation at launch.
- Cross-server economy sync in the initial milestone set.

## Build Ethos
Ship reliable systems fast, then iterate with real server feedback.
Project OOGA should feel like the civ mod stack players remember, but cleaner, fairer,
and built for today.