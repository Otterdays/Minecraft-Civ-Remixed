<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

[AMENDED 2026-05-14]:
- **Jobs are now fully configurable and server-authoritative.** The current Otters Civ. release no longer stops at a fixed starter set for runtime behavior. `config/otters_civ_revived/jobs.json` now defines the live jobs catalog itself: arbitrary job ids, display metadata, triggers, progression, boosts, and activation policy (`single` or `multi` active slots). Client HUD/UI surfaces render the server-synced catalog; player state persists in `config/otters_civ_revived/jobs_state.json` with one-time migration from legacy `jobs.properties`.

[AMENDED 2026-05-13]:
- **FPS HUD is fully deprecated and disabled.** Mod ID changed from `fpsmod` to `project_ooga`. Entrypoint classes renamed from `FpsMod`/`FpsModClient` to `OogaMod`/`ProjectOogaClient` (class name collision with standalone FPS overlay mod). The section below is kept for historical reference only — the standalone FPS overlay mod handles FPS display. See `DOCS/CHANGELOG.md` and `DOCS/ARCHITECTURE.md` for full details.

[AMENDED 2026-05-07]:
- Feature scope above documents the legacy FPS template module.
- New target scope is the civ platform in `whitepaper.md`:
  factions, jobs, professions, economy, and player shops.
- Existing FPS module should be treated as bootstrap code, not final product scope.

# FPS Mod — Feature doc

Client-only Fabric mod (`fpsmod`): **FPS HUD** + **persisted toggle**, minimal CPU use.

---

## Summary

| Area | Behavior |
|------|----------|
| **FPS readout** | Top-left HUD text (`FPS: N`), color ~light green + shadow |
| **Refresh rate** | At most **once per second** (client tick + 1000 ms throttle; uses Minecraft’s FPS value) |
| **Visibility toggle** | **Hide FPS** / **Show FPS** button on screens (inventory, pause, chat, etc.) |
| **When it runs** | World + player present only; respects **Hide GUI (F1)** |
| **Persistence** | `config/fpsmod/hud.properties` — key `showFpsHud` |
| **HUD ordering** | `HudElementRegistry` layer **before** `MISC_OVERLAYS` |

---

## User-facing behavior

1. **In world** — FPS line appears top-left unless hidden via toggle or F1.
2. **Toggle** — Open a GUI screen **in a world**; top-left **70×20** button flips HUD and relabels. No title-menu button (no level/player).
3. **Startup** — Common mod init logs (`FpsMod`); toggle changes log with `🔁` prefix.

---

## Configuration

| File | Purpose |
|------|---------|
| `config/fpsmod/hud.properties` | `showFpsHud=true|false` |

Read failures → default **show** (`true`). Write failures logged; in-memory toggle still applies for the session.

---

## Composition (for extenders)

- **Entry:** `FpsMod` (main), `FpsModClient` → registers `FpsHudOverlay`, `FpsHudScreenButton`.
- **HUD:** `FpsHudOverlay` — sampling on `ClientTickEvents.END_CLIENT_TICK`, render via `HudElementRegistry`.
- **UI:** `FpsHudScreenButton` — `ScreenEvents.AFTER_INIT`, `Screens.getWidgets(screen).add(...)`.
- **Config:** `FpsHudConfig` — Properties I/O under config dir.

---

## Explicit non-features (current scope)

- No keybinding for toggle (screen button only).
- No frame-by-frame FPS; not a profiler.
- No dedicated server / datapack logic — **`environment`: `client`** in `fabric.mod.json`.

[AMENDED 2026-05-12]:
- **Bundled `currency_blocks` tag expanded to ~260+ blocks** — covers essentially every breakable vanilla block category (stone, bricks, ores, dirt/sand, logs/leaves/planks, wool, all 16-color sets, sandstone, nether, end, copper, ore storage, glass, organics, sculk, utility blocks). Operators tune per-block values in `block_values.json` after the tag prefill.

[AMENDED 2026-05-10]:
- The **released mod** uses **`environment`: `*`** in `fabric.mod.json` so server-side economy and Otters Civ. rewards run; the HUD sections above describe the **optional client-only FPS overlay**. Server civ gameplay (wallet, `/money`, `/otter`, `rewards.json` with optional `blockRewards`/`entityRewards` plus optional `block_values.json`/`entity_values.json`, join chat) lives in README and Otters Civ. packages — not in this HUD-only subsection.

[AMENDED 2026-05-11]:
- **Command permissions (bootstrap):** **`/money`** (read balance) is open to ordinary players; **`/money set`** is restricted to vanilla **gamemaster** tier (`PermissionLevel.GAMEMASTERS`, OP-band). A dedicated permission node system is **not** shipped yet — see **`DOCS/ROADMAP.md`** *Permissions apparatus (planned)* for the future approach; admin **give/add**–style commands (when added) should follow the same gate until then.
- **Join copy:** **`JoinWelcome`** sends the full onboarding trio the **first** time a UUID is seen **on this world save**; later joins use a styled **welcome back** line with a **`~`-prefixed** display name plus a condensed command refresher (**`JoinAttendanceSavedData`** in overworld **`SavedDataStorage`**).
