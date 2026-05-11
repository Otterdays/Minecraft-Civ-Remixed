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
