# Project OOGA - Minecraft Civ Remixed

Project OOGA is an all-in-one civ mod for Fabric focused on classic server loops:
factions, jobs, professions, economy, and player shops.

The project is in active bootstrap. Right now, the economy foundation has started with a working
`/money` command path while the broader civ systems are being built from the roadmap.

## Current State

- Vision and product direction: `whitepaper.md`
- Execution tracker: `DOCS/ROADMAP.md`
- Active implementation notes: `DOCS/SCRATCHPAD.md`

### Implemented Right Now

- Server-side command bootstrap for economy
- Persistent wallet storage via `config/fpsmod/wallet.properties`
- `/money` command to view your balance
- `/money set <player> <amount>` command for fast admin/bootstrap testing
- Existing FPS HUD module still present as legacy scaffold code

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

## Economy Bootstrap Commands

- `/money` -> shows your current balance
- `/money set <player> <amount>` -> sets balance directly (bootstrap/admin use)

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

All rights reserved. See `LICENSE`.
