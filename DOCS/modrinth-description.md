## Otters Civ. Revived

A lightweight Fabric civ and economy mod for Minecraft that gives your world a stronger multiplayer backbone without burying you in setup.

---

## What you get

- Persistent money per world
- `/otter` in-game hub with dedicated guilds panel
- `/money` balance command
- Admin `/money set <player> <amount>`
- Join onboarding for first-time players
- Welcome-back message for returning players
- Configurable mining rewards
- Configurable combat rewards
- Fully configurable server-authoritative jobs from `jobs.json`
- Dynamic `/job` catalog (`list`, `info`, `join`, `leave`, `stats`, `reload`, `validate`)
- Jobs HUD overlay with in-game controls
- Guilds with `/guild create <name>` ($250)
- Officer ranks (`/guild promote|demote`)
- Chunk claims (`/guild claim`, $100 ea, max 16)
- Guild home teleport (`/guild sethome` / `/guild home`)
- ASCII + GUI chunk claim map (`/guild map`)

---

## Why it is useful

- Fast setup
- Easy to understand
- Easy to tune
- Good foundation for a civ-style server
- Works for singleplayer-hosted worlds and multiplayer servers

---

## Commands

- `/otter`
- `/money`
- `/money set <player> <amount>`
- `/job`
- `/job list`
- `/job info <id>`
- `/job join <id>`
- `/job leave [id]`
- `/job stats`
- `/job reload`
- `/job validate`
- `/guild create <name>`
- `/guild invite <player>`
- `/guild join`
- `/guild leave`
- `/guild kick <player>`
- `/guild promote|demote <player>`
- `/guild claim`
- `/guild unclaim`
- `/guild map`
- `/guild sethome`
- `/guild home`
- `/guild info`
- `/guild list`
- `/guild disband`
- `/guild reload`

---

## Current features

### Economy

- Money is saved server-side per world
- Wallets persist across restarts
- Rewards can be tuned with config files

### Rewards

- Breaking configured blocks can pay coins
- Killing configured mobs can pay coins
- Per-block and per-entity payouts are supported
- Reward behavior is configurable with JSON files

### Coverage

- Every current vanilla block is covered by the editable reward surface
- Every current vanilla living entity is covered by the editable reward surface
- Future blocks and mobs are covered by the same tag + value-file system
- The reward setup is built to stay expandable instead of hardcoded

### Jobs

- Server owners can add arbitrary jobs without Java edits
- Jobs can define their own triggers, progression, boosts, and display data
- Activation policy supports single-slot or multi-slot setups
- Jobs HUD shows synced server job labels, icons, level, and progress

### Guilds

- Create named guilds with a one-time fee ($250 default, tunable)
- Officer rank system: promote/demote members; officers can invite and claim
- Chunk claims: claim the chunk you stand in ($100 each, max 16 per guild)
- Guild home: set a teleport point (officer+) and teleport to it (any member)
- `/guild map` shows an ASCII grid in chat + a 30-second GUI overlay in top-right corner (green = claimed, aqua = you)
- Guild data persists in `config/otters_civ_revived/guilds_data.json`
- Operator tuning via `config/otters_civ_revived/guilds.json`

### Player UX

- New players get quick onboarding chat messages
- Returning players get a shorter reminder
- `/otter` gives players a cleaner in-game reference point with live guild info panel

---

## Config files

- `config/otters_civ_revived/jobs.json`
- `config/otters_civ_revived/jobs_state.json`
- `config/otters_civ_revived/rewards.json`
- `config/otters_civ_revived/block_values.json`
- `config/otters_civ_revived/entity_values.json`
- `config/otters_civ_revived/wallet.properties`
- `config/otters_civ_revived/guilds.json`
- `config/otters_civ_revived/guilds_data.json`
- `config/project_ooga/jobs_hud.properties`

---

## Good to know

- Fabric API is required
- Economy, rewards, and persistence run on the host/server side
- Installing the mod on the client adds the jobs HUD, guild chunk map overlay, and richer `/otter` UI with live guild panel
- The mod ID is `project_ooga`
- Reward coverage is broad now and future-proof by design

---

## Planned next

- Player shops
- Better admin economy controls
- Deeper civ systems
- More progression and server tooling

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

Otters Civ. Revived turns a Fabric world into the start of a real civ server with money, jobs, configurable rewards, and simple in-game tools players can use immediately, with reward coverage for every current vanilla block and living entity and a setup that is ready for future additions too.

