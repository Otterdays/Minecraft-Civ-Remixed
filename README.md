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

> **Otters Civ. Revived** is the player-facing name. Repository codename **Project OOGA**. Mod identifier on disk remains **`fpsmod`** until a planned rename pass.

Otters Civ. Revived is a **Fabric** companion for Minecraft Java that adds a **persistent server wallet**, **slash commands**, **join-time tips**, and **configurable payouts** when players mine tagged blocks or defeat hostile mobs. Long-term horizon: factions-style territory, jobs, shops, and a fuller economy—tracked in **`DOCS/ROADMAP.md`**.

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

## Commands (server)

| Command | Purpose |
|---------|---------|
| `/otter` | Help listing and pointers to reward config paths |
| `/money` | Show your wallet balance |
| `/money set <player> <amount>` | Bootstrap / admin balance set (permissions still on the roadmap) |

Reward tuning lives in **`config/otters_civ_revived/rewards.json`** (restart required after edits).

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
