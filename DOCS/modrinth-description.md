## Otters Civ. Revived

A lightweight Fabric civ and economy mod for Minecraft that gives your world a stronger multiplayer backbone without burying you in setup.

---

## What you get

- Persistent money per world
- `/otter` in-game hub
- `/money` balance command
- Admin `/money set <player> <amount>`
- Join onboarding for first-time players
- Welcome-back message for returning players
- Configurable mining rewards
- Configurable combat rewards
- Four starter jobs:
  - `miner`
  - `lumberjack`
  - `farmer`
  - `fighter`
- Jobs HUD overlay with in-game controls

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
- `/job join <job>`
- `/job leave`

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

### Jobs

- One active job at a time
- Earn job XP from matching actions
- Level up over time
- Jobs HUD shows job, level, and progress

### Player UX

- New players get quick onboarding chat messages
- Returning players get a shorter reminder
- `/otter` gives players a cleaner in-game reference point

---

## Config files

- `config/otters_civ_revived/rewards.json`
- `config/otters_civ_revived/block_values.json`
- `config/otters_civ_revived/entity_values.json`
- `config/otters_civ_revived/wallet.properties`
- `config/project_ooga/jobs_hud.properties`

---

## Good to know

- Fabric API is required
- Economy, rewards, and persistence run on the host/server side
- Installing the mod on the client adds the jobs HUD and richer `/otter` UI
- The mod ID is `project_ooga`

---

## Planned next

- Factions and land claims
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

Otters Civ. Revived turns a Fabric world into the start of a real civ server with money, jobs, configurable rewards, and simple in-game tools players can use immediately.

