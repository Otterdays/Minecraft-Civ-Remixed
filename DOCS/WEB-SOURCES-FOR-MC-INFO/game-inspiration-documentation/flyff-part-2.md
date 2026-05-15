In **Flyff**, Sunstones and Moonstones are the "bread and butter" of the endgame economy. They turn standard gear into specialized powerhouses through two distinct systems: **General Upgrading** and **Piercing**.

For *Otters Civ. Revived*, these provide the perfect "resource sink" to keep your SQLite economy from inflating. Here is how to adapt these mechanics into your Fabric mod architecture.

---

## I. Sunstones: General Upgrading (+1 to +10)

Sunstones are used to increase the base stats of weapons and armor. In Flyff, this process can fail, potentially breaking the item (a massive risk/reward mechanic).

* **The Upgrade UI:** Add an "Upgrade" tab to your `/otter` hub. Players place a piece of gear and a **Sunstone** (a custom item with a specific NBT tag) into the slots.
* **Actionable Mechanic:**
* **+1 to +3:** 100% success rate.
* **+4 to +10:** Success rate decreases (e.g., 50%, 25%, 10%).
* **Failure Consequence:** On failure at higher levels, the item is destroyed. To prevent this, players must use a "Scroll of Protection" (another high-value JSON-configurable item).


* **Visual Prestige:** Upgraded weapons should gain a glow or particle effect.
* *Example:* A +5 sword has a faint white trail; a +10 sword has a massive golden aura (using Minecraft's `DustParticleOptions`).



---

## II. Moonstones: Piercing & Jewelry

Moonstones are for **horizontal** power—adding "Sockets" (Piercing) to gear or upgrading accessories like rings and necklaces.

* **The "Piercing" Process:**
* Using a Moonstone on a Chestplate (Suit) has a chance to open a **Socket** (up to 4 slots).
* **Actionable:** Once a slot is open, players can insert **Attribute Cards** (e.g., Fire Card +2% DMG, Land Card +5% HP).
* **SQLite Storage:** Your `items` table (or NBT data) needs to track `pierce_slots_total` and `pierce_slots_filled`.


* **Jewelry Refinement:** Use Moonstones to upgrade Rings (`+STR`) or Earrings (`+Attack`). These never break but have very low success rates, making a "+20 Stamina Ring" a legendary status symbol in your Civ.

---

## III. The "Shining Oricalkum" (The High-Tier Sink)

In Flyff, you can combine **5 Sunstones + 5 Moonstones** to create a **Shining Oricalkum**.

* **Advanced Upgrading:** Normal Sunstones only go to +10. Shining Oricalkum is required for "Ultimate Upgrading" (+11 to +20).
* **Civ Implementation:** This creates a natural demand for both stones. Even top-tier players will buy basic Sunstones from new players (who earn them via your `rewards.json` mob drops) just to craft the higher-tier materials.

---

## IV. Technical Implementation for Otters Civ

### 1. Item Definition (`items.json`)

Define these as "Catalyst Items" so your logic knows how to handle them when used in the UI.

```json
{
  "catalysts": {
    "project_ooga:sunstone": {
      "type": "UPGRADE",
      "max_safe_level": 3,
      "base_success_rate": 0.8
    },
    "project_ooga:moonstone": {
      "type": "PIERCE",
      "max_slots": 4,
      "success_rate": 0.4
    }
  }
}

```

### 2. SQLite: `item_upgrades` Table

Since Minecraft items are usually handled by NBT, you can either store this in the item's NBT or use a "Relational Item ID" in SQLite to prevent NBT data bloat.

```sql
CREATE TABLE item_upgrades (
    item_instance_id TEXT PRIMARY KEY,
    upgrade_level INTEGER DEFAULT 0,
    pierce_slots INTEGER DEFAULT 0,
    socket_1_data TEXT,
    socket_2_data TEXT
);

```

### 3. Reward Integration (`rewards.json`)

Make these stones rare drops from all mobs to encourage "grinding" (a core Flyff pillar).

```json
{
  "global_drops": [
    { "item": "project_ooga:sunstone", "chance": 0.01, "message": "§6You found a rare Sunstone!" },
    { "item": "project_ooga:moonstone", "chance": 0.005, "message": "§bA Moonstone fell from the sky!" }
  ]
}

```

### The "Otter" Developer's Take:

> "Sunstones and Moonstones are the reason players keep killing zombies at 3 AM. They aren't just rocks; they're the gambling chips of the apocalypse. Just make sure the 'item break' sound effect is sufficiently heartbreaking."

[Flyff Upgrading and Piercing Guide](https://www.youtube.com/watch?v=0jHSlsIVhRw)

This video demonstrates the "Safe Piercing" UI and the visual feedback players expect when using these stones to upgrade their gear.