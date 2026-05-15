<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

## Otters Civ. Revived

Otters Civ. Revived is a Fabric mod that turns a normal Minecraft world into a simple civilization-style server. It adds money that players can earn and trade, rewards for mining and fighting, jobs that give people different roles, guilds that can claim land together, and easy in-game menus so players and server hosts can understand and use the mod without needing programming knowledge.

---

## What it does

- **Economy:** persistent SQLite-backed balances, `/pay` transfers between players, admin money tools, and an immutable `wallet_ledger` audit trail
- **Rewards:** configurable mining and combat payouts driven by tags and per-id overrides, with broad vanilla block and living-entity coverage out of the box
- **Jobs:** a server-authoritative JSON jobs catalog with configurable triggers, progression, and boosts; the shipped starter pack is `miner`, `lumberjack`, `farmer`, `excavator`, and `fighter`
- **Guilds:** player-run groups with officer ranks, public or invite-only joining, chunk claims, protections, home teleport, chat/GUI claim maps, and chunk border visuals
- **Player UX:** the `/otter` hub surfaces wallet, jobs, guilds, rewards, and civ roadmap info in one place (optional **four color themes** — Otter, Midnight, Sunset, Mint — cycled from a footer chip and saved to `config/project_ooga/otter_ui.properties`), and `/guide` spawns a glinting in-game handbook with the live server's current basics

---

## Commands

### Economy

- `/guide` — spawn the in-game Otters Civ. handbook for yourself
- `/guide give <player>` — admin-give the handbook to another player (gamemaster / OP only)
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
- `/guild join [name]` — accept a pending invite, or join an open guild by name
- `/guild leave` — leave your guild
- `/guild kick <player>` — kick a member (officer+)
- `/guild transfer <player>` — transfer ownership to another member
- `/guild promote <player>` / `/guild demote <player>` — owner-only officer management
- `/guild claim` — claim the chunk you stand in ($100 each, max 16 per guild)
- `/guild unclaim` — release the current chunk
- `/guild unclaimall` — release every chunk your guild owns
- `/guild map` — ASCII chunk map in chat plus a 30-second GUI overlay (top-right): surface tint per chunk, your guild vs other tint, facing wedge on your cell, and a hotbar hint when you walk into a different claim
- `/guild sethome` — set guild teleport point (officer+)
- `/guild home` — teleport to guild home (any member)
- `/guild open` / `/guild close` — switch between public joining and invite-only mode (owner-only)
- `/guild info` — your guild's details
- `/guild list` — every guild on the server
- `/guild reload` — reload `guilds.json` (gamemaster / OP only)

### Database

- `/ooga db status` — show schema version and database path (gamemaster / OP only)
- `/ooga db migrate` — run pending schema migrations (gamemaster / OP only)

### Help

- `/otter` — opens the in-game hub (HOME · WALLET · JOBS · GUILDS · REWARDS · CIV · HELP) on clients with the mod, with quick actions, config shortcuts, and a **Theme** chip to cycle menu palettes (Otter / Midnight / Sunset / Mint; saved to `config/project_ooga/otter_ui.properties`); if the game window is too small for the full panel, a compact fallback card still exposes money/job/guild shortcuts and the same theme control; on vanilla clients it falls back to a chat command list
- [AMENDED 2026-05-14] **Home tab readouts:** Minecraft + mod version, local vs multiplayer session hint, current chunk + claim name when claim sync is active, richer wallet/guild summary cards, a `/guide` quick button next to `/money` / `/job list` / `/guild info`, and a **Brief** sidebar tab that condenses `DOCS/whitepaper.md` Phases 0–3 + `DOCS/ROADMAP.md` M0–M3 (two-page toggle); **Civ** tab lists M0–M6 milestone cards. Chat `/otter` also prints short tips for the hub, join messages, and the live handbook.

---

## Features

### Economy

- Money, transaction history, jobs state, guilds, and claims are persisted server-side in SQLite at `config/otters_civ_revived/project_ooga.db`
- Player-to-player transfers are atomic — both balances update or neither does
- `/pay` policy is operator-tunable: minimum/maximum transfer, optional self-pay, per-sender cooldown, optional flat fee (see `economy.json`)
- Admin set/add/take for fast moderator action
- Every mutation (admin ops, `/pay`, rewards, starting balance) is written to the SQLite `wallet_ledger` table (immutable, indexed, queryable)
- Moderators can audit recent server-wide or per-player history live in chat with `/economy log`
- The database runs in WAL mode and is auto-created / migrated on startup
- First-join starting balance and the join-welcome toggle both live in `economy.json`

### Rewards

- Breaking configured blocks pays coins
- Killing configured entities pays coins
- Per-block and per-entity payouts override tag-wide defaults
- Reward behavior is fully editable from JSON files — no Java required
- Three-tier customization: edit sibling value files, inline in `rewards.json`, or extend bundled tags via server datapack
- Reward chat splits cleanly: `+N coins` for money, separate `[job] +N xp · Lvl X · in/range` for matching active jobs

### Coverage

- Broad current vanilla block coverage ships out of the box
- Broad current vanilla living-entity coverage ships out of the box
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
- Officer rank system: promote/demote members; officers can invite, claim land, and set the guild home
- Officer count is capped by config, so owners can keep leadership smaller than the full member cap
- Owner can transfer guild ownership to another member
- Guilds can be public or invite-only, with `allowOpenGuilds` controlled by config
- Chunk claims with configurable cost and per-guild cap ($100 each, max 16 default)
- Guild home: set a teleport point (officer+) and use it (any member, optional cooldown)
- `/guild map` paints a 9×9 chunk grid in chat, toggles a temporary GUI overlay (surface map colors + claim tint + facing + claim-crossing hint), and shows chunk borders with particles
- Block-break, place, container-use, and block-attack protection inside claimed chunks
- Guild data lives in SQLite; operator tuning lives in `config/otters_civ_revived/guilds.json`

### Player UX

- First-join welcome: three onboarding chat lines pointing to `/guide`, `/otter`, `/money`, and rewards
- Returning players see a shorter welcome-back line
- `/otter` opens a stylized in-game menu on modded clients with live wallet, job, guild, rewards, and roadmap/help panels
- `/otter` menu colors: four client-only themes (footer **Theme** chip); choice persists in `config/project_ooga/otter_ui.properties` (no effect on server economy or guild data)
- `/guild map` also drives a temporary top-right claim overlay (terrain tint, claim ownership tint, facing line, foot-travel claim hint) and chunk border particles
- Vanilla clients still get a full chat command help list from `/otter`
- `/guide` is fully server-side, so even vanilla or server-only players can still spawn the handbook item; they just need a free inventory slot

---

## Config files — where everything lives

All files are auto-generated on first run. You can edit them while the server is stopped; most JSON config can also be reloaded live.

### Operator-editable JSON (`config/otters_civ_revived/`)

All files below are auto-generated on first run with sensible defaults. Edit any JSON while the server is stopped; most also support `/reload`.

**`economy.json`** — currency display, starting balance, join welcome toggle, and `/pay` policy:
- `currencySymbol` — symbol shown in chat (default `"$"`)
- `currencyName` / `currencyNamePlural` — "coin" / "coins"
- `newPlayerStartingBalance` — coins new players get on first join (default `0`)
- `maxBalance` — hard ceiling (default `0` = no cap)
- `maxTransferPerCommand` — `/pay` limit (default `100000`)
- `minTransferAmount` — minimum `/pay` (default `1`)
- `transferCooldownSeconds` — cooldown between `/pay` calls (default `3`)
- `transferFlatFee` — fee deducted from sender (default `0`)
- `allowSelfPay` — can players pay themselves? (default `false`)
- `showJoinWelcome` — show join messages? (default `true`)

**`rewards.json`** — mining/combat payout rules: block/entity tags, cooldowns, dimension blacklist, flat rewards

**`block_values.json`** — per-block coin values (auto-prefilled from bundled block tags on first run)

**`entity_values.json`** — per-entity coin values (auto-prefilled from bundled entity tags on first run)

**`jobs.json`** — server jobs catalog: job definitions, triggers, progression curves, payout boosts, activation policy (`single`/`multi`)

**`guilds.json`** — guild tuning:
- `creationCost` — cost to create a guild (default `250`)
- `claimCost` — cost per chunk claim (default `100`)
- `maxClaims` — max claims per guild (default `16`)
- `maxMembers` — max members per guild (default `20`)
- `maxOfficers` — officer cap per guild (default `4`)
- `minNameLength` / `maxNameLength` — guild name constraints
- `disbandRefundClaims` — refund claim costs on disband? (default `true`)
- `allowOpenGuilds` — allow public joining mode? (default `true`)
- `homeTeleportCooldownSeconds` — cooldown on `/guild home` (default `60`)
- `protectBlocks` — block breaking blocked for non-members? (default `true`)
- `protectContainers` — chests/furnaces/etc blocked for non-members? (default `true`)
- `protectInteractables` — buttons/levers/doors blocked for non-members? (default `false`)
- `allowMemberBuild` — can regular members build in claimed chunks? (default `true`)
- `allowOfficerBuild` — can officers build in claimed chunks? (default `true`)
- `pvpInClaims` — PVP allowed in claimed chunks? (default `true`)
- `showChunkBorders` — show `/guild map` particle borders? (default `true`)
- `mapRadius` — radius of `/guild map` grid (default `4`, 9×9 grid)
- `overlayDurationSeconds` — how long the GUI overlay stays visible (default `30`)

### Runtime state (do not edit, managed automatically)

- **`config/otters_civ_revived/project_ooga.db`** — SQLite database in WAL mode. Holds wallets, the `wallet_ledger` audit trail, guilds, chunk claims, and jobs state. Auto-created and migrated on startup. Safe to back up while the server is stopped.
- **`config/project_ooga/jobs_hud.properties`** — client-side HUD position/scale. Only present on clients with the mod. Edit via `/otter` → Jobs tab.
- **`config/project_ooga/otter_ui.properties`** — client-only `/otter` menu color theme (`OTTER`, `MIDNIGHT`, `SUNSET`, or `MINT`). Written when you use the footer Theme chip in `/otter`.

---

## Permissions

- **Open to all players:** `/guide`, `/otter`, `/money`, `/pay`, the self-service `/job` set, and most `/guild` commands
- **Role-gated inside guilds:** officers handle invites, land claims, and guild-home setting; owners handle officer promotions and public/invite-only join mode
- **Gamemaster / OP only** (same band as vanilla cheat commands): `/guide give <player>`, `/money set|add|take`, `/economy reload`, `/economy log`, `/job reload`, `/job validate`, `/guild reload`, `/ooga db status|migrate`
- A dedicated permission apparatus (string-id permission nodes, Fabric Permissions API / LuckPerms compatibility) is planned

---

## Good to know

- Requires Fabric API
- No external database setup is needed — the release jar bundles its SQLite runtime and auto-creates `config/otters_civ_revived/project_ooga.db` on first start
- Economy, rewards, jobs, and guild logic all run on the host/server side
- Dynamic state (wallets, ledger, guilds, claims, jobs) persists in SQLite at `config/otters_civ_revived/project_ooga.db` (WAL mode, auto-migrated on startup)
- Client install is optional but unlocks the jobs HUD (`config/project_ooga/jobs_hud.properties`), guild chunk overlay, and the stylized `/otter` menu (optional themes in `config/project_ooga/otter_ui.properties`)
- `/guide` works even without the client extras, because the handbook is just a server-issued written book item
- Operator-facing behavior is mostly JSON-driven (`economy.json`, `rewards.json`, `jobs.json`, `guilds.json`)
- Jobs and guild client views are server-synced, so remote players see the host's live catalog and claim state instead of guessing from local files
- The technical mod ID is `project_ooga`
- Reward coverage is broad today and future-proof — new blocks and entities flow through the same tag + value-file system
- Legacy FPS HUD is deprecated and disabled; the standalone FPS overlay mod handles FPS display

---

## Planned next

- Player shops (M4) — listings, escrow, market UI
- Friends list and private messaging (M4.5)
- Civ governance: diplomacy, treaties, faction projects (M5)
- Deeper jobs/profession trees, richer admin tooling, and a more granular permissions layer

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

Otters Civ. Revived is a Fabric mod that turns a normal Minecraft world into a lightweight civilization server with a persistent economy, configurable mining and combat rewards, a five-job starter pack, guild land claims, and in-game menus plus a handbook for easy setup in singleplayer or multiplayer. With the client mod, the `/otter` hub can switch between a few color themes so the civ menu matches your taste without editing JSON.

---

## Auto-generated config README (new)

The mod now drops a beginner-friendly **`README.md`** into `config/otters_civ_revived/` every time it loads. It is written in plain, high-school-readable English for server hosts who do not work with JSON every day.

What the generated README covers:

- A "don't panic" intro and JSON pitfalls (quotes vs. no quotes, commas, lowercase `true`/`false`)
- Stop-the-server-first / make-a-backup safety steps
- A table of every file in the folder and what it controls
- Per-field tables for `economy.json`, `jobs.json`, `guilds.json`, and `rewards.json` — each setting, what it means, and its default
- How `block_values.json` and `entity_values.json` get auto-prefilled from tags, plus the precedence order (tag default → inline `rewards.json` map → sibling value file)
- "I broke it, now what?" recovery steps (delete the file, restart, defaults are rewritten)
- A short reference of in-game commands

The README is regenerated on every mod load, so it always matches the current build. It is documentation, not user data — edits to it are overwritten on next launch. Edit the `.json` files instead.
