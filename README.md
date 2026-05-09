# Project OOGA - Minecraft Civ Remixed

Project OOGA is an all-in-one civ mod for Fabric focused on classic server loops:
factions, jobs, professions, economy, and player shops.

**Player-facing name:** Otters Civ. Revived (JAR/mod id is still `fpsmod` until a rename pass.)

The project is in active bootstrap. The economy foundation is live (`/money`, wallet file) plus
**Otters Civ.** mining/combat payouts driven by `config/otters_civ_revived/rewards.json`.
Use **`/otter`** in-game for a command summary.

## Current State

- Vision and product direction: `whitepaper.md`
- Execution tracker: `DOCS/ROADMAP.md`
- Active implementation notes: `DOCS/SCRATCHPAD.md`

### Implemented Right Now

- Server-side commands: **`/otter`** (help list), **`/money`**, **`/money set <player> <amount>`**
- Persistent wallet storage via `config/fpsmod/wallet.properties`
- **Otters Civ. Revived** payouts: break blocks in `otters_civ_revived:currency_blocks`,
  kill `#minecraft:hostile` mobs by default (direct player hit only in v1); config
  `config/otters_civ_revived/rewards.json`, datapack tags under `data/otters_civ_revived/`
- `JobsHooks` no-op stub for future jobs/professions
- Existing FPS HUD module still present as legacy client extra

## Quick Start

### Requirements

- Java 25+
- Minecraft 26.1.2
- Fabric Loader 0.19.2+
- Fabric API 0.146.1+26.1.2

### Build

```bat
gradlew.bat build
```

### Run Dev Client

```bat
gradlew.bat runClient
```

### Run Tests

```bat
gradlew.bat test
```

### Output Jar

- `BUILT/libs/project-ooga-1.0.0.jar`

## Commands (server)

- `/otter` — list Otters Civ. / fpsmod commands and pointers to reward config
- `/money` — show your balance
- `/money set <player> <amount>` — set balance (bootstrap/admin; permission gate planned)

Passive rewards are configured separately (tags, cooldowns, amounts): see
`config/otters_civ_revived/rewards.json`.

NOTE: Permission gating and full admin command tree are next roadmap items.

## Roadmap Focus (Near-Term)

1. Lock down `/money set` permissions
2. Add `/pay` and `/ooga money set|add|take`
3. Add immutable transaction logging
4. Add transfer caps/cooldowns and anti-abuse controls

Track checklist progress in `DOCS/ROADMAP.md`.

## Tech Snapshot

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.146.1+26.1.2`
- Loom: `1.16-SNAPSHOT` (resolved at build time)
- Java: `25+`
- Build outputs: `BUILT/`

## Notes for Contributors

- This repo is no longer positioned as a generic FPS template.
- README reflects the civ-mod target first, with current bootstrap status made explicit.
- Preserve status docs under `DOCS/` and keep roadmap checklists current while shipping features.

## License

All rights reserved (ARR) with **explicit carve-outs** in `LICENSE` for gameplay/video/montage
use and for including the **official unmodified JAR** in mod packs—no redistribution of
forks or substantial source reproduction without permission. Details: [`LICENSE`](LICENSE).
