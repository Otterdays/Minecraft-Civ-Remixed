# Otters Civ. Revived

<p align="center"><strong>Fabric civ/economy mod for Minecraft Java 26.1.2</strong></p>

<p align="center">
  <a href="https://github.com/Otterdays/Minecraft-Civ-Remixed"><img src="https://img.shields.io/badge/Repo-GitHub-181717?style=for-the-badge&logo=github" alt="GitHub repository"/></a>
  <a href="index.html"><img src="https://img.shields.io/badge/Docs-Offline%20Wiki-0f766e?style=for-the-badge" alt="Offline wiki"/></a>
  <a href="DOCS/ROADMAP.md"><img src="https://img.shields.io/badge/Roadmap-M0--M6-1d4ed8?style=for-the-badge" alt="Roadmap"/></a>
  <a href="https://github.com/Otterdays/Minecraft-Civ-Remixed/issues"><img src="https://img.shields.io/badge/Support-Issues-7c3aed?style=for-the-badge&logo=github" alt="GitHub issues"/></a>
</p>

<p align="center">
  <a href="https://www.minecraft.net/"><img src="https://img.shields.io/badge/Minecraft-26.1.2-5a7f3f?style=for-the-badge" alt="Minecraft 26.1.2"/></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Fabric%20Loader-%3E%3D0.19.2-2563eb?style=for-the-badge" alt="Fabric Loader 0.19.2 or newer"/></a>
  <a href="https://fabricmc.net/develop"><img src="https://img.shields.io/badge/Fabric%20API-0.146.1%2B26.1.2-0ea5e9?style=for-the-badge" alt="Fabric API 0.146.1+26.1.2"/></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-25%2B-f97316?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 25+"/></a>
  <a href="src/main/resources/fabric.mod.json"><img src="https://img.shields.io/badge/Mod%20ID-project__ooga-475569?style=for-the-badge" alt="Mod ID project_ooga"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-ARR%20%2B%20carve--outs-991b1b?style=for-the-badge" alt="License"/></a>
</p>

<p align="center"><strong>Quick links:</strong> <a href="#install">Install</a> | <a href="#slash-commands-what-you-type-in-minecraft-chat">Commands</a> | <a href="index.html">Offline reference</a> | <a href="DOCS/SUMMARY.md">Docs summary</a></p>

| What to call it | Name |
|-----------------|------|
| **The mod (for players)** | **Otters Civ. Revived** |
| **Our internal codename (for the team / repo chatter)** | **Project OOGA** |
| **Technical mod id inside the jar** | **`project_ooga`** (changed from legacy `fpsmod` to avoid mod ID conflict with standalone FPS overlay mod) |

**Project OOGA** is the nickname we use in docs and Discord-style talk; players still see **Otters Civ. Revived**.

Otters Civ. Revived is a **Fabric** add-on that saves **money per world**, shows **slash commands** in chat, sends **join system messages** (full onboarding the first time you connect to that save; a shorter **welcome back ~name** line when you've played there before), and can **pay you** when you mine configured blocks or defeat configured entities. Long-term: land claims, jobs, shops - see **`DOCS/ROADMAP.md`**.

### Words we use

| Term | Meaning |
|------|---------|
| **Otters Civ. Revived** | Player-facing mod name |
| **Project OOGA** | Internal codename (repo / team chat) |
| **project_ooga** | Jar's technical mod ID (was fpsmod; changed to avoid mod ID conflict with standalone FPS overlay mod) |
| **Server / host** | The game instance that owns the save - your single-player session or multiplayer host |
| **Tag** | A vanilla datapack grouping of blocks or mob types (`blockTag`, `entityTag` in **`rewards.json`**) |
| **`blockRewards` / `entityRewards`** | Optional per-block or per-mob-type payout overrides in **`rewards.json`** (merged with **`block_values.json`** / **`entity_values.json`**; sibling files patch same keys on top at startup) |

---

## Highlights (current release)

One-line version: **wallet + chat commands + jobs + optional mining/kill payouts + join onboarding / welcome-back**; details in the table.

| Area | What you get |
|------|----------------|
| **Wallet & commands** | `/money`, `/money set`, `/otter`; balances in `config/otters_civ_revived/wallet.properties` (legacy `config/fpsmod/wallet.properties` migrates once on load) |
| **Payouts** | Tag-driven mining & combat rewards; **per-block** / **per-entity-type** amounts via inline `blockRewards` / `entityRewards` **or** dedicated **`block_values.json`** / **`entity_values.json`** next to **`rewards.json`** (merged; sibling files override same keys after load). Current vanilla block + living-entity coverage is broad out of the box, and the same tag/value-file system is designed to absorb future additions cleanly. |
| **Jobs** | `/job`, `/job list`, `/job info <id>`, `/job join <id>`, `/job leave [id]`, `/job stats`; jobs are now server-authoritative and fully data-driven from `config/otters_civ_revived/jobs.json`, including arbitrary job count, triggers, progression, boosts, and single-vs-multi active-slot rules. Player job progress persists in `config/otters_civ_revived/jobs_state.json` (legacy `jobs.properties` migrates once). |
| **Onboarding** | System chat on join: **first visit per save** - three lines; **returning** - short **welcome back ~name** + `/otter` / `/money` refresher (stored per world save, not only in `config/`) |
| **Client extra** | Jobs HUD overlay (icon + level + XP bar); legacy FPS HUD is **deprecated & disabled** (standalone FPS overlay mod handles display) |

For full mechanics, defaults, and operator notes, open **`index.html`** in the repo or see **`DOCS/`** below.

---

## Install

1. Install **Fabric Loader** for **Minecraft 26.1.2** ([Fabric installer](https://fabricmc.net/use/installer/)).
2. Add **Fabric API** **0.146.1+26.1.2** (or matching line for your exact game build) from [Modrinth](https://modrinth.com/mod/fabric-api) or your launcher's browser.
3. Add this project's artifact (e.g. **`project-ooga-1.0.0.jar`** after a local build; see **`mod_version`** in **`gradle.properties`**, output under **`BUILT/libs/`**) next to Fabric API in **`mods/`**.
4. **Dedicated server:** same `mods/` setup on the server; clients only need the JAR if you want the HUD sidecar.

Development stack (build / run from source): **Java 25+**. Runtime for the game follows Minecraft's launcher requirements.

<details>
<summary><strong>Expand: build locally</strong></summary>

Windows:

```bat
gradlew.bat build
```

Output file name is **`project-ooga-` + `mod_version` + `.jar`** under **`BUILT/libs/`** (pinned in **`gradle.properties`**).

Tests: **`gradlew.bat test`** | Dev client: **`gradlew.bat runClient`**

</details>

---

## Slash commands (what you type in Minecraft chat)

**Plain English:** These are the same kind of commands as `/gamemode` or `/give`. Press **T** (or your chat key), type something that **starts with `/`**, press **Enter**. You can do that **any time you are inside a world** with the mod loaded - **your single-player world counts too**. You are **not** opening a black terminal or a website; it is just the game chat.

When docs say **"server,"** they mean **"the game side that stores your balance and checks the rules,"** not **"only for multiplayer pros."** Multiplayer **realms / rented servers** use the same commands; single-player uses them too.

| Command | What it does |
|---------|----------------|
| `/otter` | Shows a short help list and where reward settings live |
| `/money` | Shows **your** money |
| `/money set <player> <amount>` | Sets someone's balance (**gamemaster / OP-equivalent** only - same band as many vanilla cheat commands; see below) |
| `/job`, `/job stats` | Shows your active jobs, levels, and XP progress |
| `/job list` | Lists the live server job catalog |
| `/job info <id>` | Shows one job's triggers, progression, and boosts |
| `/job join <id>`, `/job leave [id]` | Activates or clears jobs using the live server catalog |
| `/job reload`, `/job validate` | Reloads and validates jobs config (**gamemaster / OP-equivalent** only) |

Money rules load from **`config/otters_civ_revived/rewards.json`**; **`block_values.json`** / **`entity_values.json`** list per-block / per-mob payouts. On logical server startup the mod expands your configured **`blockTag`**/**`entityTag`** into those maps when empty, merges inline overrides from **`rewards.json`**, persists sorted JSON, then applies it - restart after edits.

Jobs rules load from **`config/otters_civ_revived/jobs.json`**. That file now owns the live server job catalog: global enable/activation rules, arbitrary job ids, display metadata, triggers, progression, and money / XP boosts. Player selections / XP totals live separately in **`config/otters_civ_revived/jobs_state.json`**; older **`jobs.properties`** data migrates once on load.

#### Adding your own blocks or mobs

Three layers, low -> high precedence. Combine freely.

1. **Edit `block_values.json` / `entity_values.json`** - add `"namespace:id": amount` lines (e.g. `"minecraft:ancient_debris": 500`), save, `/reload`. Per-id entries win over tag fallback, so the block/mob does **not** need to be in any tag.
2. **Inline in `rewards.json`** - same idea under the `blockRewards` / `entityRewards` objects. Sibling value files merge on top, overriding overlapping ids.
3. **Server datapack extending the bundled tag** - drop `data/otters_civ_revived/tags/block/currency_blocks.json` (or `tags/entity_type/currency_mobs.json`) into `<world>/datapacks/<your_pack>/` with `{ "replace": false, "values": ["yourmod:custom_ore"] }`. **Use the singular directory names** (`block`, `entity_type`) - MC 1.21+ silently ignores the plural forms. `/reload` and the prefill re-runs. Full walkthrough: [`index.html` -> Adding your own blocks & mobs](index.html#add-custom-payouts).

### Command permissions (current)

- **Everyone** who can open chat: **`/money`** (read-only balance).
- **Gamemaster-tier** command sources (vanilla **operators** in the usual cheat band, console, etc.): **`/money set`**. Implemented with Minecraft 26.x `CommandSourceStack.permissions()` and `Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)`.
- **Future:** dedicated mod permission strings / Fabric Permissions API compatibility so you can grant econ admin without full OP - see **`DOCS/ROADMAP.md`** -> **Permissions apparatus (planned)**.

Balances on disk (**`config/otters_civ_revived/wallet.properties`**) use **`uuid=amount`** keys; when the server knows your name it also writes **`# Name: YourName`** on the line above as a readability hint (the UUID line stays the source of truth for money).

---

## Documentation

For **deep design and contributor workflow**, use **`DOCS/`** and the agent handbooks below. The table is an index only.

| Topic | Entry |
|--------|--------|
| Project snapshot & links | **[`DOCS/SUMMARY.md`](DOCS/SUMMARY.md)** |
| Roadmap & milestones | **[`DOCS/ROADMAP.md`](DOCS/ROADMAP.md)** |
| Strategic direction | **[`DOCS/whitepaper.md`](DOCS/whitepaper.md)** |
| Code map | **[`DOCS/LOCATIONS.md`](DOCS/LOCATIONS.md)** |
| Architecture | **[`DOCS/ARCHITECTURE.md`](DOCS/ARCHITECTURE.md)** |
| Contributor / agent workflow | [`AGENTS.md`](AGENTS.md), [`CLAUDE.md`](CLAUDE.md) |
| Style & `index.html` parity | [`DOCS/STYLE_GUIDE.md`](DOCS/STYLE_GUIDE.md) |

Browser-friendly reference (**commands, configs, defaults**): [`index.html`](index.html) | Modrinth-style listing copy: [`DOCS/modrinth-description.md`](DOCS/modrinth-description.md)

---

## Roadmap & contributing

**Next up (short list):** richer econ **permission nodes** / plugin integration (beyond vanilla gamemaster for **`/money set`**), **`/pay`**, an audit trail, sinks and caps. The full checklist lives in **`[DOCS/ROADMAP.md](DOCS/ROADMAP.md)`**.

For pull requests or automation-assisted work: read **`AGENTS.md`** first, then **`DOCS/STYLE_GUIDE.md`**.

**Issues:** [github.com/Otterdays/Minecraft-Civ-Remixed/issues](https://github.com/Otterdays/Minecraft-Civ-Remixed/issues)

---

## License

All rights reserved. Narrow carve-outs appear in **`[LICENSE](LICENSE)`** (gameplay / video content, distributing the official unmodified JAR in mod packs, etc.). Forks or substantial redistribution of modified sources remain outside those carve-outs without permission.
