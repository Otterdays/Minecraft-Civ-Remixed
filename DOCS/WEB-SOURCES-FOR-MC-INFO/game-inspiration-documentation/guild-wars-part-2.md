To truly capture that **Guild Wars 1** magic, your Guild Hall shouldn't just be a static location—it should be a "sink" for the massive amounts of capital your players are earning through jobs.

Here is the architectural blueprint for the `upgrades.json` schema and the logic for the "Guild Lord" protector.

---

## 1. The `upgrades.json` Template

This file defines what guilds can "research" or "purchase" to improve their base. It ties directly into your existing `guilds` and `economy` logic.

```json
{
  "categories": {
    "logistics": {
      "guild_merchant": {
        "display_name": "Guild Quartermaster",
        "levels": [
          { "cost": 5000, "benefit": "Unlock basic trade NPC", "requirement": "guild_level_1" },
          { "cost": 15000, "benefit": "5% discount on server shop prices", "requirement": "guild_level_2" }
        ]
      },
      "teleport_hub": {
        "display_name": "Runic Waypoint",
        "levels": [
          { "cost": 10000, "benefit": "Removes cooldown on /guild home", "requirement": "guild_level_3" }
        ]
      }
    },
    "warfare": {
      "guild_lord_fortification": {
        "display_name": "Throne Room Integrity",
        "levels": [
          { "cost": 2500, "benefit": "+50 Guild Lord Max HP", "requirement": "none" },
          { "cost": 7500, "benefit": "Guild Lord gains Thorns II effect", "requirement": "guild_level_2" }
        ]
      },
      "sentry_guards": {
        "display_name": "Hall Sentries",
        "levels": [
          { "cost": 3000, "benefit": "Spawns 2 Iron Golems during sieges", "requirement": "none" }
        ]
      }
    }
  }
}

```

---

## 2. The Guild Lord Logic (Protector NPC)

The **Guild Lord** is the "King" of the guild's claims. If the Lord dies, the guild loses its territory or enters a "Broken" state where taxes double.

### The Lifecycle of the Lord:

1. **Placement:** When a guild reaches Level 1, a `project_ooga:guild_lord` entity (extending a Villager or Iron Golem) spawns at the `/guild sethome` location.
2. **Persistence:** The Lord's UUID, Health, and GuildID are stored in a new `guild_protectors` table in your SQLite DB.
3. **The "Siege" State:**
* Normally, the Lord is **Invulnerable**.
* When a rival guild uses `/guild war declare <name>`, a 24-hour countdown starts.
* Once the "Siege Window" opens, the Lord becomes vulnerable to players from the attacking guild.


4. **Defeat Consequence:**
* If the Lord dies, the attacking guild is credited with a "Conquest."
* The defending guild's claims are locked (cannot build/break) for 1 hour, and the attacker "plunders" 10% of the Guild Treasury automatically.



### The SQLite Table Structure:

```sql
CREATE TABLE guild_protectors (
    guild_id TEXT PRIMARY KEY,
    entity_uuid TEXT NOT NULL,
    current_hp REAL,
    max_hp REAL,
    last_spawn_pos_x DOUBLE,
    last_spawn_pos_y DOUBLE,
    last_spawn_pos_z DOUBLE,
    is_under_attack BOOLEAN DEFAULT 0,
    FOREIGN KEY(guild_id) REFERENCES guilds(id)
);

```

---

## 3. Integrating with the `/otter` UI

To make this user-friendly, add two new sub-menus to your Hub:

### **The "Hall" Tab**

* **Visuals:** A list of purchased NPCs and Trophies.
* **Action:** Click an icon to "Summon" your Guild Merchant or "Upgrade" your Lord's armor.
* **Status:** Shows the current "Tax Rate" the hall is generating (larger halls cost more to maintain!).

### **The "War Room" Tab**

* **Leaderboard:** Displays the top "GvG" winners based on Lord kills.
* **War Bell:** A button to initiate a declaration of war (checking `economy.json` for the `declarationCost`).
* **Defense Status:** Shows your Lord’s current HP and whether your "Vulnerability Window" is active.

---

## Actionable Next Step for You:

Since you already have **SQLite-backed guilds**, you should add a `guild_level` column to your `guilds` table first. This will act as the "Gatekeeper" for all the GW1-style upgrades.

> **Architect’s Pro-Tip:** Don't let the Guild Lord wander! In your Fabric code, override the Lord's AI goals so it stays within 10 blocks of the `sethome`. Nothing is more embarrassing than a Guild Lord wandering into a nearby ravine during a siege.

**Should I draft the Java logic for the "Siege Window" timer system next?**