Ah, you want the **Pre-Big Bang, Golden Era MapleStory**. The era where the game wasn't about soloing a boss in two seconds, but about the grueling, highly social, and deeply iconic communal grind.

If we strip away the modern bloat and focus purely on the legendary features that defined classic MapleStory, we get mechanics that perfectly align with a Minecraft civilization mod. Here is the architectural report on adapting the classic, iconic MapleStory experience for *Otters Civ. Revived*.

---

## I. The Free Market (FM) & Shop Permits

*Before auction houses, there was the Free Market. A chaotic, bustling hub where players set up literal shop-stands, and your physical location in "Room 1, Channel 1" was prime real estate.*

* **The Hub Dimension/Zone:** Designate a specific chunk or a custom dimension as the "Free Market." PvP and monster spawns are disabled.
* **Shop Permits:** Players buy a "Permit" (e.g., a Mushroom or Store block) from an admin shop. They place it in the FM, which spawns an AFK merchant entity.
* **Prime Real Estate (The Rent System):**
* **Mechanic:** To prevent the FM from becoming a graveyard of inactive players, shops in "prime spots" (closer to the entrance) drain a daily rent from the player's SQLite wallet. If they run out of money, their shop is packed up and items are sent to a "Recovery Chest" table.
* **Visuals:** Players can buy cosmetic upgrades for their shop entity via `upgrades.json` (e.g., turning their simple chest into a Villager, a Golem, or a custom block).



---

## II. Party Quests (PQs) - e.g., Kerning City / Ludibrium

*PQs were the ultimate test of cooperation. You didn’t just need high damage; you needed specific classes, communication, and puzzle-solving.*

* **Job-Gated Entry:**
* **Mechanic:** Create a `party_quests.json` that defines instanced or reset-able dungeon chunks. To enter, a party of exactly 4 players must stand on pressure plates.
* **The Twist:** The JSON enforces *Job requirements*. For example, the "Deep Mines PQ" requires at least 1 Miner (to break a specific wall) and 1 Fighter (to kill the boss).


* **Stage-Based Progression:**
* Translate classic MS stages (like collecting passes from alligators) into Minecraft. Players kill mobs in Room 1 to collect "Dungeon Keys." Once 20 keys are dropped in a hopper, the iron door to Room 2 opens.


* **The "Track" Mechanic:**
* Because classic PQs were heavily contested, parties would "Track" whoever was currently inside. You can implement a `/pq status` command that shows which Guild is currently running the dungeon and how long until the instance is free.



---

## III. The Gachapon Machine

*The original loot box. It was a massive coin sink that kept the economy in check while offering the tantalizing dream of game-breaking gear.*

* **Regional Gachapons:**
* **Mechanic:** Different biomes or Guild Towns can construct a "Gachapon Machine."
* **The Ticket:** Players buy Gachapon Tickets for a massive amount of coins, or earn them as incredibly rare drops (1 in 10,000) from normal mining/combat.
* **The Loot Tables:** Create a `gachapon.json` with weighted drops.
* *Common (70%):* Apples, basic iron ingots.
* *Rare (29%):* Diamond blocks, Job XP boosts.
* *Jackpot (1%):* Unique, uncraftable items like "Pink Adventurer Cape" or custom enchanted weapons.


* **Global Announcements:** When a player hits the 1% Jackpot, broadcast it server-wide: `[Gachapon] PlayerX has won the legendary Pink Cape in the Desert town!`



---

## IV. Jump Quests (JQs)

*Infamous, rage-inducing platforming challenges (like the Sleepywood JQ) where movement skills were disabled, and one mistake sent you to the bottom.*

* **The JQ Architecture:**
* Guilds or Admins can designate a "Jump Quest Claim." Inside this claim, a custom Fabric event hook disables all teleportation, Ender Pearls, Elytras, and blocks breaking/placing.


* **The Reward:**
* At the very top is a specific NPC or Block. Clicking it grants a unique cosmetic (like a special hat or a unique Title in chat) and logs their clear time to a `jump_quests` SQLite table.
* You can display a "Fastest Times" hologram leaderboard at the bottom to drive competitive server engagement.



---

## V. Natural Recovery & "Sitting on Chairs"

*In classic MS, HP and MP didn't regenerate quickly on their own. If you were poor, you couldn't afford potions, so you had to find a safe spot in town and sit in a chair for 5 minutes.*

* **Disabling Vanilla Regen:** Use Fabric to drastically slow down natural Minecraft health regeneration, even with a full hunger bar.
* **The "Chair" Healing Mechanic:**
* **Mechanic:** Add a system where right-clicking a Stair block (or a custom Chair item) puts the player in a sitting entity state.
* **The Benefit:** While sitting, players regenerate Health and "Job Stamina" 5x faster.
* **Social Impact:** This forces players to congregate in Guild Halls or Town Squares to heal between mining or combat trips, naturally facilitating the social chatting that made MapleStory famous.



---

### Actionable Technical Implementation

#### 1. SQLite: `market_shops` Table

```sql
CREATE TABLE market_shops (
    shop_id INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_uuid TEXT NOT NULL,
    chunk_x INTEGER,
    chunk_z INTEGER,
    daily_rent_cost INTEGER,
    shop_model_id TEXT, -- e.g., 'BASIC_CHEST', 'MUSHROOM_STAND'
    is_open BOOLEAN DEFAULT 1
);

```

#### 2. JSON: `gachapon.json`

```json
{
  "machines": {
    "desert_gacha": {
      "ticket_cost": 50000,
      "rewards": [
        { "item": "minecraft:golden_apple", "weight": 700, "broadcast": false },
        { "item": "project_ooga:exp_coupon", "weight": 290, "broadcast": false },
        { "item": "minecraft:elytra", "weight": 10, "broadcast": true, "message": "§d[Gachapon] {player} won an Elytra!" }
      ]
    }
  }
}

```

### The "Otter" Developer's Take:

> "Classic MapleStory was a masterclass in making players suffer together, which inadvertently built the strongest communities in gaming history. By making them sit in chairs to heal, gating dungeons behind specific job roles, and adding a literal slot machine, we aren't just making a civilization—we're making a virtual society."

Which of these classic pillars fits your current roadmap best? We could draft the logic for the **Job-Gated Party Quests**, or build out the **Free Market Rent System**.