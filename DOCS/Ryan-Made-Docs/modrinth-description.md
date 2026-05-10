# Otters Civ. Revived


-is the player-facing name for this Fabric civ remix (repo/codename Project OOGA): one mod aimed at the loops people actually play (earn, belong, trade, defend).

What’s new...? Civilization building gameplay on the server.

 What’s included today

- **Join message** — when you connect, a few system-chat lines identify Otters Civ. Revived on the server and point you at **`/otter`** and **`/money`**.
- **`/otter`** — in-game list of this mod’s commands (economy + where reward config lives).
- **`/money`** — show your wallet balance on the server.
- **`/money set <player> <amount>`** — set a player’s balance (bootstrap / admin testing; permission gating is still on the todo list).
- **Persistent wallets** — balances stored server-side (`config/fpsmod/wallet.properties`).
- **Mining & combat payouts (Otters Civ.)** — breaking blocks in tag `otters_civ_revived:currency_blocks` and killing entities in `#minecraft:hostile` (defaults) grant coins, **or** set per-block / per-mob payouts in the same file via **`blockRewards`** and **`entityRewards`** (registry id → amount); flat `blockReward` / `entityReward` still apply when an id is not listed. Tune `config/otters_civ_revived/rewards.json` (cooldowns, skip creative/spectator, dimensions). Ranged/indirect kills do not pay in this v1 (direct player hit only).
- **Legacy client HUD** — optional FPS readout + screen toggle from the original template; grandfathered extra, not core product.

## Roadmap

*Order is approximate; milestones match the project roadmap.*

**Economy & integrity (near-term)**  
- Permission nodes for **`/money set`** and admin money ops.  
- **`/pay`** — player-to-player transfers.  
- **`/otter money set | add | take`** — consolidated admin wallet commands.  
- Immutable **transaction log** for every balance change; anti-spam caps and clearer player feedback where it matters.

**Foundation**  
- Versioned persistence layer, migrations, and SQLite-backed storage (replacing flat wallet files for authoritative state long-term).  
- Audit-friendly operator tooling as the ledger grows.

**Jobs & professions**  
- **`/job join | leave | info`** and **`/profession info`**.  
- Event-driven payouts (mining, farming, hunting, crafting) with cooldowns / anti-farm checks; progression that persists across restarts.

**Factions & claims**  
- **`/f create | invite | join | leave | promote | demote`** (and related grouping).  
- Chunk claims with protection flags, faction treasury hooks, role checks.

**Player shops**  
- Listings, buy/sell flows, GUI-first shop screens, taxes and listing limits so markets stay playable.

**Later / polish**  
- Diplomacy, territory projects, governance knobs.  
- Hardening, balance passes, optional PostgreSQL backend for heavy servers.

If you install today, you’re on the foundation track—watch the changelog as each slice lands.

---


<details>
<summary>Spoiler</summary>

License

Copyright is **all rights reserved**. The bundled { LICENSE }
https://github.com/Otterdays/Minecraft-Civ-Remixed/blob/main/LICENSE) (same file in the jar) adds **narrow allowances** without turning the project open source:

- **Videos, streams, reviews, montages, tutorials** that show gameplay with this mod are fine (including monetized platforms).
- **Mod packs**: you may include the **official, unmodified** release JAR obtained from Modrinth or another channel the author designates—**no modified or rebuilt jars**, no shady repacks.
- **No reproduction** of substantial source/materials beyond those carve-outs; forks and redistribution of custom builds still need explicit permission.

If in doubt, open an issue before redistributing anything beyond an unmodified pack binary.


</details>

