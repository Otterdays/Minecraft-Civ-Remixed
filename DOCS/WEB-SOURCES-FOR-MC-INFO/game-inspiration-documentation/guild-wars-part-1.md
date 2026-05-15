Integrating **Guild Wars 1 (GW1)** mechanics into *Otters Civ. Revived* is a brilliant move. GW1 excelled at making a Guild Hall feel like a "living" base of operations that provided utility, prestige, and a tactical staging ground for war.

In the context of your Fabric/SQLite architecture, here is how you can implement GW1-inspired **Guild Halls** and **GvG (Guild vs. Guild)** mechanics.

---

### I. The Guild Hall Evolution (Base Building)

*In GW1, the Hall was a private instance. In Minecraft, we can translate this into "Specialized Claim Utility."*

* **Service NPCs (The "Hiring" System):**
* Instead of static blocks, allow Guild Officers to "hire" NPCs (Armorers, Skill Trainers, Merchants) via the `/otter` UI.
* **Actionable:** Use a `guild_services` table in SQLite. When a service is purchased, an NPC (or a specially tagged block/entity) spawns in the Guild Home chunk.
* **Utility:** The "Guild Merchant" could offer a 5% discount or buy items at a higher rate than the global market, funded by the Guild Treasury.


* **The "Sigil of Tenure" (Hall Customization):**
* Implement a "Hall Tier" system in `guilds.json`. Level 1 is a basic campfire; Level 5 is a fortified citadel.
* Higher tiers unlock larger `/guild map` radii and specialized "Room" types (e.g., a "Portal Room" for cheap teleports to allied nations).


* **Guild Trophies & Monuments:**
* A "Hall of Monuments" equivalent. When a guild achieves a milestone (e.g., 1,000,000 coins earned, 50th siege won), they unlock a unique decorative block or a permanent "Statue" that grants a tiny server-wide buff to members (like +1% Job XP).



---

### II. Tactical GvG (The "Battle Isles" Experience)

*GW1 GvG was about objectives, not just team deathmatch. You can simulate this using your chunk-claiming system.*

* **The "Guild Lord" Mechanic:**
* Every Guild Hall/Home has a "Guild Lord" (a high-health, boss-tier NPC or a "Core" block).
* **War Mechanic:** To win a war, the attacking guild doesn't just kill players; they must reach the center of the `sethome` chunk and "slay" the Guild Lord or "capture" the Core (a 5-minute stand-on-point timer).


* **Bodyguard NPCs:**
* Allow guilds to spend Treasury coins to spawn "Guild Guards" in claimed chunks.
* **Architecture:** Use JSON to define guard stats (Health, Damage, Follow Range). Store their spawn coordinates in SQLite so they respawn 10 minutes after being killed during a siege.


* **Victory Emblems & Capes:**
* In GW1, your cape was your identity. In your mod, winning a GvG season could unlock a "Guild Emblem" (a custom particle aura or a glowing name-tag color) visible in the `/otter` UI and over the player's head.



---

### III. Strategic Diplomacy (The "Alliance Battles" System)

*Move from individual guilds to massive "Factions" (Luxons vs. Kurzicks).*

* **Faction Allegiance:**
* In `economy.json`, define two or three global Factions. Guilds can pledge to one.
* **Border Wars:** Create "Contested Zones" in the world. The Faction with the most collective Job XP earned in that zone over a week "owns" the zone, granting their members a 1.2x reward multiplier in that area.


* **The "Kurzick/Luxon" Resource Trade:**
* Add "Faction-Specific Items" (e.g., Amber or Jade) that can only be harvested in specific biomes. These are required for high-tier Guild Hall upgrades, forcing guilds to trade or go to war for territory.



---

### IV. Actionable Technical Implementation

*How to add these to your current "Otters Civ" stack:*

1. **SQLite Table: `guild_upgrades**`
* Columns: `guild_id` (FK), `upgrade_id` (string), `level` (int), `last_maintained` (timestamp).
* *Purpose:* Tracks which NPCs, buffs, or fortifications the guild has purchased.


2. **JSON Config: `warfare.json**`
* `declarationCost`: How much it costs to start a war.
* `warDurationTicks`: How long the "Siege State" lasts (enabling PvP/Griefing).
* `guildLordHealth`: HP for the NPC protector.


3. **UI Addition (`/otter`):**
* **"The War Room" Tab:** Shows current active wars, the health of your Guild Lord, and a "Mercenary" sub-tab to hire guards.
* **"The Hall" Tab:** A visual grid of your Hall's upgrades and available service NPCs.



### The "Otter" Architect's View:

> "A Guild Hall shouldn't just be a place to store your dirt blocks. It should be the beating heart of your empire—where your merchants haggle, your guards sharpen their blades, and your enemies' heads are mounted on the wall. Or at least, where you have a really nice rug."

**Would you like a specific JSON template for the `upgrades.json` or the logic for the "Guild Lord" NPC?**