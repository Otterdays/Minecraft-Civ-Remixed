<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

[AMENDED 2026-05-11]: Canonical path restored under `DOCS/` (tooling & docs reference `DOCS/modrinth-description.md`). A parallel copy may exist under `DOCS/Ryan-Made-Docs/`; prefer this path for CI and automation.

[AMENDED 2026-05-11]: README + `index.html` add a short **Words we use** gloss (player name vs **Project OOGA** vs `fpsmod`; “server” = whoever hosts the world; tags vs optional `blockRewards` / `entityRewards`). Modrinth listing content below is unchanged in behavior—see repo for the full glossary.

[AMENDED 2026-05-11]: **Wallet path** — `config/otters_civ_revived/wallet.properties` (economy beside `rewards.json`); `config/fpsmod/` HUD-only (`hud.properties`). Legacy fpsmod wallet file auto-migrates once.

[AMENDED 2026-05-11]: **Permissions:** `/money` anyone; **`/money set`** needs vanilla gamemaster / OP-band (`PermissionLevel.GAMEMASTERS`). Roadmap appendix **Permissions apparatus (planned)** covers future LuckPerms-style nodes.

[AMENDED 2026-05-11]: **Join UX:** **first** session on **this world save** → three onboarding system-chat lines. **Returning** players get a gold/aqua **welcome back ~** _(display name)_ line and a shorter `/otter` / `/money` refresher (**`fpsmod:join_attendance`** in overworld saved data). The **Join message** bullet below still describes the onboarding side for first-time connect copy.


# Otters Civ. Revived


-is the player-facing name for this Fabric civ remix (repo/codename Project OOGA): one mod aimed at the loops people actually play (earn, belong, trade, defend).

What’s new...? Civilization building gameplay on the server.

 What’s included today

- **Join message** — when you connect, a few system-chat lines identify Otters Civ. Revived on the server and point you at **`/otter`** and **`/money`**.
- **`/otter`** — in-game list of this mod’s commands (economy + where reward config lives).
- **`/money`** — show your wallet balance on the server.
- **`/money set <player> <amount>`** — set a player’s balance (**operators / vanilla gamemaster band** today; richer permission nodes planned—see repo **`DOCS/ROADMAP.md`**).
- **Persistent wallets** — balances stored server-side (`config/otters_civ_revived/wallet.properties`; older `config/fpsmod/wallet.properties` moves there on first load).
[AMENDED 2026-05-10]: **Per-id payouts** live beside **`rewards.json`**: **`block_values.json`** / **`entity_values.json`**. Logical-server **startup** expands **tag payouts** (`blockTag`, `entityTag`) into those maps when unparsed/unseeded yet, merges inline overrides + operator edits.

- **Mining & combat payouts (Otters Civ.)** — breaking blocks tagged by **`blockTag`** and killing mobs typed by **`entityTag`** pays coins; turnkey JSON lists (**`block_values.json`/`entity_values.json`**) populate from tags at first **`SERVER_STARTED`**, tweak every id’s amount there (or stash sparse overrides inline in **`rewards.json`**). Fallback tag logic still catches brand-new mods/blocks absent from persisted JSON until you rerun with an empty sibling file again. Tune `config/otters_civ_revived/rewards.json` plus those value files (**rewards.json**: cooldowns, skips, blacklist, announcements). Ranged/indirect kills do not pay in this v1 (direct player hit only).
- **Legacy client HUD** — optional FPS readout + screen toggle from the original template; grandfathered extra, not core product.

## Roadmap

*Order is approximate; milestones match the project roadmap.*

**Economy & integrity (near-term)**  
- Mod-specific permission **nodes** (beyond vanilla **`/money set`** gamemaster gate) for admin money ops.  
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

