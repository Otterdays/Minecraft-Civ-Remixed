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

Otters Civ. Revived is a **Fabric** add-on that saves **money per world**, shows **slash commands** in chat, sends a **welcome tip** when you join, and can **pay you** when you mine certain blocks or defeat certain mobs. Long-term: land claims, jobs, shops—see **`DOCS/ROADMAP.md`**.

---

## Highlights (current release)

| Area | What you get |
|------|----------------|
| **Wallet & commands** | `/money`, `/money set`, `/otter`; balances in `config/fpsmod/wallet.properties` |
| **Payouts** | Tag-driven mining & combat rewards; optional **per-block** / **per-entity-type** amounts via `blockRewards` & `entityRewards` in **`config/otters_civ_revived/rewards.json`** |
| **Onboarding** | Short system-chat message when joining (points to `/otter` & `/money`) |
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
| `/money set <player> <amount>` | Sets someone’s balance (bootstrap / admin; proper permissions later) |

Money rules and mining/combat payouts are edited in **`config/otters_civ_revived/rewards.json`** (restart the game after you change that file).

---

## Documentation

Detailed architecture, changelog, roadmap, SBOM, and contributor conventions live under **`DOCS/`**.

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

Near-term priorities (summarized): hardened **`/money set`** permissions, **`/pay`**, ledger-style auditing, sinks and caps — see **`[DOCS/ROADMAP.md](DOCS/ROADMAP.md)`** for the authoritative checklist.

For pull requests or automation-assisted work: read **`AGENTS.md`** first, then **`DOCS/STYLE_GUIDE.md`**.

**Issues:** [github.com/Otterdays/Minecraft-Civ-Remixed/issues](https://github.com/Otterdays/Minecraft-Civ-Remixed/issues)

---

## License

All rights reserved. Narrow carve-outs appear in **`[LICENSE](LICENSE)`** (gameplay / video content, distributing the official unmodified JAR in mod packs, etc.). Forks or substantial redistribution of modified sources remain outside those carve-outs without permission.
