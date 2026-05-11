# Otters Civ. Revived

**Fabric · Minecraft Java 26.1.2**

<p align="center">
  <a href="https://github.com/Otterdays/Minecraft-Civ-Remixed"><img src="https://img.shields.io/badge/GitHub-Minecraft--Civ--Remixed-181717?style=flat-square&logo=github" alt="GitHub repository"/></a>
  <a href="https://www.minecraft.net/"><img src="https://img.shields.io/badge/Minecraft-26.1.2-5a7f3f?style=flat-square" alt="Minecraft 26.1.2"/></a>
  <a href="https://fabricmc.net/"><img src="https://img.shields.io/badge/Fabric%20Loader-0.19.2-blue?style=flat-square" alt="Fabric Loader"/></a>
  <a href="https://fabricmc.net/develop"><img src="https://img.shields.io/badge/Fabric%20API-0.146.1%20%2826.1.2%29-1c84c6?style=flat-square" alt="Fabric API"/></a>
  <a href="https://openjdk.org/"><img src="https://img.shields.io/badge/Java-25%2B-orange?style=flat-square&logo=openjdk&logoColor=white" alt="Java 25+"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-ARR%20%2B%20carve--outs-darkred?style=flat-square" alt="License"/></a>
</p>

| What to call it | Name |
|-----------------|------|
| **The mod (for players)** | **Otters Civ. Revived** |
| **Our internal codename (for the team / repo chatter)** | **Project OOGA** |
| **Technical mod id inside the jar** | **`fpsmod`** (planned rename later) |

**Project OOGA** is the nickname we use in docs and Discord-style talk; players still see **Otters Civ. Revived**.

Otters Civ. Revived is a **Fabric** add-on that saves **money per world**, shows **slash commands** in chat, sends **join system messages** (full onboarding the first time you connect to that save; a shorter **welcome back ~**name line when you’ve played there before), and can **pay you** when you mine certain blocks or defeat certain mobs. Long-term: land claims, jobs, shops—see **`DOCS/ROADMAP.md`**.

### Words we use

| Term | Meaning |
|------|---------|
| **Otters Civ. Revived** | Player-facing mod name |
| **Project OOGA** | Internal codename (repo / team chat) |
| **`fpsmod`** | Jar’s technical mod ID (rename planned) |
| **Server / host** | The game instance that owns the save—your single-player session or multiplayer host |
| **Tag** | A vanilla datapack grouping of blocks or mob types (`blockTag`, `entityTag` in **`rewards.json`**) |
| **`blockRewards` / `entityRewards`** | Optional per-block or per–mob-type payout overrides in **`rewards.json`** (merged with **`block_values.json`** / **`entity_values.json`**; sibling files patch same keys on top at startup) |

---

## Highlights (current release)

One-line version: **wallet + chat commands + optional mining/kill payouts + join onboarding / welcome-back**; details in the table.

| Area | What you get |
|------|----------------|
| **Wallet & commands** | `/money`, `/money set`, `/otter`; balances in `config/otters_civ_revived/wallet.properties` (legacy `config/fpsmod/wallet.properties` migrates once on load) |
| **Payouts** | Tag-driven mining & combat rewards; **per-block** / **per-entity-type** amounts via inline `blockRewards` / `entityRewards` **or** dedicated **`block_values.json`** / **`entity_values.json`** next to **`rewards.json`** (merged; sibling files override same keys after load) |
| **Onboarding** | System chat on join: **first visit per save** — three lines; **returning** — short **welcome back ~**name + `/otter` / `/money` refresher (stored per world save, not only in `config/`) |
| **Client extra** | Optional legacy FPS HUD (cosmetic template carry-over) |

For full mechanics, defaults, and operator notes, open **`index.html`** in the repo or see **`DOCS/`** below.

---

## Install

1. Install **Fabric Loader** for **Minecraft 26.1.2** ([Fabric installer](https://fabricmc.net/use/installer/)).
2. Add **Fabric API** **0.146.1+26.1.2** (or matching line for your exact game build) from [Modrinth](https://modrinth.com/mod/fabric-api) or your launcher’s browser.
3. Add this project’s artifact (e.g. **`project-ooga-1.0.0.jar`** after a local build; see **`mod_version`** in **`gradle.properties`**, output under **`BUILT/libs/`**) next to Fabric API in **`mods/`**.
4. **Dedicated server:** same `mods/` setup on the server; clients only need the JAR if you want the HUD sidecar.

Development stack (build / run from source): **Java 25+**. Runtime for the game follows Minecraft’s launcher requirements.

<details>
<summary><strong>Expand: build locally</strong></summary>

Windows:

```bat
gradlew.bat build
```

Output file name is **`project-ooga-` + `mod_version` + `.jar`** under **`BUILT/libs/`** (pinned in **`gradle.properties`**).

Tests: **`gradlew.bat test`** · Dev client: **`gradlew.bat runClient`**

</details>

---

## Slash commands (what you type in Minecraft chat)

**Plain English:** These are the same kind of commands as `/gamemode` or `/give`. Press **T** (or your chat key), type something that **starts with `/`**, press **Enter**. You can do that **any time you are inside a world** with the mod loaded—**your single-player world counts too**. You are **not** opening a black terminal or a website; it is just the game chat.

When docs say **“server,”** they mean **“the game side that stores your balance and checks the rules,”** not “only for multiplayer pros.” Multiplayer **realms / rented servers** use the same commands; single-player uses them too.

| Command | What it does |
|---------|----------------|
| `/otter` | Shows a short help list and where reward settings live |
| `/money` | Shows **your** money |
| `/money set <player> <amount>` | Sets someone’s balance (**gamemaster / OP-equivalent** only—same band as many vanilla cheat commands; see below) |

Money rules load from **`config/otters_civ_revived/rewards.json`**; **`block_values.json`** / **`entity_values.json`** list per-block / per-mob payouts. On logical server startup the mod expands your configured **`blockTag`**/**`entityTag`** into those maps when empty, merges inline overrides from **`rewards.json`**, persists sorted JSON, then applies it—restart after edits.

### Command permissions (current)

- **Everyone** who can open chat: **`/money`** (read-only balance).
- **Gamemaster-tier** command sources (vanilla **operators** in the usual cheat band, console, etc.): **`/money set`**. Implemented with Minecraft 26.x `CommandSourceStack.permissions()` and `Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)`.
- **Future:** dedicated mod permission strings / Fabric Permissions API compatibility so you can grant econ admin without full OP — see **`DOCS/ROADMAP.md`** → **Permissions apparatus (planned)**.

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

Browser-friendly reference (**commands, configs, defaults**): [`index.html`](index.html) · Modrinth-style listing copy: [`DOCS/modrinth-description.md`](DOCS/modrinth-description.md)

---

## Roadmap & contributing

**Next up (short list):** richer econ **permission nodes** / plugin integration (beyond vanilla gamemaster for **`/money set`**), **`/pay`**, an audit trail, sinks and caps. The full checklist lives in **`[DOCS/ROADMAP.md](DOCS/ROADMAP.md)`**.

For pull requests or automation-assisted work: read **`AGENTS.md`** first, then **`DOCS/STYLE_GUIDE.md`**.

**Issues:** [github.com/Otterdays/Minecraft-Civ-Remixed/issues](https://github.com/Otterdays/Minecraft-Civ-Remixed/issues)

---

## License

All rights reserved. Narrow carve-outs appear in **`[LICENSE](LICENSE)`** (gameplay / video content, distributing the official unmodified JAR in mod packs, etc.). Forks or substantial redistribution of modified sources remain outside those carve-outs without permission.
