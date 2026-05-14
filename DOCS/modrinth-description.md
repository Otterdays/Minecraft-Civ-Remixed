## Otters Civ. Revived

A lightweight Fabric civ and economy mod for Minecraft. It gives a world a persistent money system, player transfers, configurable block/mob rewards, server-authoritative jobs progression, guild land control, and an in-game civ menu so players and operators can actually use the systems without digging through code.

---

## What it does

- **Economy:** persistent per-world balances, `/pay` transfers between players, admin money tools, and an immutable transaction log so every coin movement is auditable
- **Rewards:** configurable mining and combat payouts driven by tags and per-id overrides, with broad vanilla block/entity coverage out of the box
- **Jobs:** a server-authoritative JSON jobs catalog with configurable triggers, progression, and boosts; the shipped starter pack is `miner`, `lumberjack`, `farmer`, `excavator`, and `fighter`
- **Guilds:** player-run groups with officer ranks, chunk claims, protections, home teleport, chat/GUI claim maps, and chunk border visuals
- **Player UX:** the `/otter` hub surfaces wallet, jobs, rewards, and guild info in one place instead of making players learn the config first

---

## Commands

### Economy

- `/money` — show your wallet balance
- `/pay <player> <amount>` — send coins to another player (atomic; server-enforced per-command cap, per-sender cooldown, optional flat fee — all tunable in `economy.json`)
- `/money set <player> <amount>` — replace a player's balance (gamemaster / OP only)
- `/money add <player> <amount>` — credit a player's balance (gamemaster / OP only)
- `/money take <player> <amount>` — deduct from a player's balance, clamped to 0 (gamemaster / OP only)
- `/economy reload` — re-read `economy.json` without restart (gamemaster / OP only)
- `/economy log [count]` — show the most recent ledger entries server-wide, newest first (gamemaster / OP only)
- `/economy log player <player> [count]` — filter the ledger to a single player (gamemaster / OP only)

### Jobs

- `/job` — show your current job(s) and progression
- `/job list` — list all jobs the server has configured
- `/job info <id>` — details on a specific job (tags, XP/event, boosts)
- `/job join <id>` — join a job
- `/job leave [id]` — leave a job (or all jobs if omitted)
- `/job stats` — your level/XP across all active jobs
- `/job reload` — reload `jobs.json` (gamemaster / OP only)
- `/job validate` — sanity-check the loaded jobs config (gamemaster / OP only)

### Guilds

- `/guild create <name>` — create a guild ($250 default, tunable)
- `/guild disband` — owner-only; refunds claim costs
- `/guild invite <player>` — invite a player (officer+)
- `/guild join` — accept a pending invite
- `/guild leave` — leave your guild
- `/guild kick <player>` — kick a member (officer+)
- `/guild transfer <player>` — transfer ownership to another member
- `/guild promote <player>` / `/guild demote <player>` — owner-only officer management
- `/guild claim` — claim the chunk you stand in ($100 each, max 16 per guild)
- `/guild unclaim` — release the current chunk
- `/guild unclaimall` — release every chunk your guild owns
- `/guild map` — ASCII chunk map in chat plus a 30-second GUI overlay (top-right corner)
- `/guild sethome` — set guild teleport point (officer+)
- `/guild home` — teleport to guild home (any member)
- `/guild info` — your guild's details
- `/guild list` — every guild on the server
- `/guild reload` — reload `guilds.json` (gamemaster / OP only)

### Help

- `/otter` — opens the in-game hub (HOME · WALLET · JOBS · REWARDS · CIV · HELP) on clients with the mod; falls back to a chat command list on vanilla clients

---

## Features

### Economy

- Money is persisted server-side per world
- Player-to-player transfers are atomic — both balances update or neither does
- `/pay` policy is operator-tunable: per-command cap, per-sender cooldown, optional flat fee (see `economy.json`)
- Admin set/add/take for fast moderator action
- Every mutation (admin op, pay, reward) is written to an append-only `transactions.log` (CSV: `timestamp,playerId,delta,balanceAfter,reason,note`)
- Moderators can audit any player's history live in chat with `/economy log player <player>` — no need to open the file
- Wallet file is human-readable: optional `# Name: PlayerName` hint above each `uuid=balance` line

### Rewards

- Breaking configured blocks pays coins
- Killing configured mobs pays coins
- Per-block and per-entity payouts override tag-wide defaults
- Reward behavior is fully editable from JSON files — no Java required
- Three-tier customization: edit sibling value files, inline in `rewards.json`, or extend bundled tags via server datapack
- Reward chat splits cleanly: `+N coins` for money, separate `[job] +N xp · Lvl X · in/range` for matching active jobs

### Coverage

- Every current vanilla block has a slot in the editable reward surface
- Every current vanilla living entity has a slot in the editable reward surface
- Future blocks and mobs flow through the same tag + value-file system without code changes

### Jobs

- Operators add arbitrary jobs without Java edits — just edit `jobs.json`
- Each job defines its own triggers, XP curve, payout multiplier, boosts, and display data
- Activation policy supports single-active-job or multi-active-job setups
- Starter pack ships miner / lumberjack / farmer / excavator / fighter in single-slot mode
- Default starter tuning is fast early, long-tail later, and intentionally modest on boosts
- `/job validate` now highlights overlap and reward-surface dead zones for operators
- Jobs HUD shows the server-synced primary job's label, icon, level, and progress bar
- Level-up announcements fire only on threshold crossings

### Guilds

- Create named guilds with a one-time fee ($250 default, tunable)
- Officer rank system: promote/demote members; officers can invite and claim land
- Owner can transfer guild ownership to another member
- Chunk claims with configurable cost and per-guild cap ($100 each, max 16 default)
- Guild home: set a teleport point (officer+) and use it (any member)
- `/guild map` paints a 9×9 chunk grid in chat plus a temporary GUI overlay
- Block-break, place, and container-access protection inside claimed chunks
- Guild data lives in `config/otters_civ_revived/guilds_data.json`; operator tuning in `config/otters_civ_revived/guilds.json`

### Player UX

- First-join welcome: three onboarding chat lines pointing to `/otter`, `/money`, and rewards
- Returning players see a shorter welcome-back line
- `/otter` opens a stylized in-game menu on modded clients with live wallet, job, rewards, and guild panels
- `/guild map` also drives a temporary top-right claim overlay and chunk border particles
- Vanilla clients still get a full chat command help list from `/otter`

---

## Config files

All under `config/otters_civ_revived/` unless noted:

- `economy.json` — `/pay` policy: max transfer per command, per-sender cooldown, optional flat fee
- `rewards.json` — mining/combat reward rules (tags + per-id overrides)
- `block_values.json` — per-block coin payouts (auto-prefilled from tags)
- `entity_values.json` — per-entity coin payouts (auto-prefilled from tags)
- `jobs.json` — server jobs catalog (triggers, progression, boosts, activation policy)
- `jobs_state.json` — per-player job state (active jobs, XP)
- `wallet.properties` — per-UUID balances (with operator-friendly name hints)
- `transactions.log` — append-only CSV audit of every balance change
- `guilds.json` — guild costs, claim cap, member cap, protection toggles
- `guilds_data.json` — live guild + claim data
- `config/project_ooga/jobs_hud.properties` — client-side HUD position/scale (only on clients with the mod)

---

## Permissions

- **Open to all players:** `/otter`, `/money`, `/pay`, the self-service `/job` set, and most `/guild` commands (create, invite, join, leave, claim, unclaim, map, sethome, home, info, list)
- **Gamemaster / OP only** (same band as vanilla cheat commands): `/money set|add|take`, `/economy reload`, `/economy log`, `/job reload`, `/job validate`, `/guild reload`
- A dedicated permission apparatus (string-id permission nodes, Fabric Permissions API / LuckPerms compatibility) is planned

---

## Good to know

- Requires Fabric API
- Economy, rewards, jobs, and guild logic all run on the host/server side
- Client install is optional but unlocks the jobs HUD, guild chunk overlay, and the stylized `/otter` menu
- The technical mod ID is `project_ooga`
- Reward coverage is broad today and future-proof — new vanilla blocks and mobs flow through the same tag system

---

## Planned next

- Player shops (M4) — listings, escrow, market UI
- Friends list and private messaging (M4.5)
- Civ governance: diplomacy, treaties, faction projects (M5)
- Deeper jobs/profession trees and richer admin tooling

---

## Usage and content policy

You are allowed to:

- Use the mod in singleplayer
- Use the mod on multiplayer servers
- Include it in videos, streams, reviews, showcases, and tutorials
- Include the official unmodified release in modpacks

You are not allowed to:

- Reverse engineer it
- Crack it
- Fork it
- Reupload modified builds
- Redistribute repacks or altered versions
- Repost the source or bundled materials as your own

If you want to do something outside those boundaries, ask first.

---

## Short version

Otters Civ. Revived turns a Fabric world into the start of a real civ server: persistent money, atomic player transfers, an immutable transaction log, configurable mining/combat rewards covering every vanilla block and mob, a fully server-authoritative jobs catalog with a shipped 5-job starter pack, and guilds with chunk claims/protection — usable from day one in singleplayer or multiplayer.
