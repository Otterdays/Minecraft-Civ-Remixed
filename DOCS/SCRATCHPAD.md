<!-- PRESERVATION RULE: Never delete or replace content. Append or annotate only. -->

# SCRATCHPAD

## Active Tasks (Newest First)
- [2026-05-11] Readability pass (tasteful): README **Words we use** table + Highlights one-liner + docs/roadmap soft leads; **`index.html`** same gloss (styled region), `.lead` captions, plain command/feature copy, `#words-we-use-h` for scroll spy; **`DOCS/modrinth-description.md`** annotated; **CHANGELOG** / **SCRATCHPAD** bumped.

- [2026-05-11] Naming + clarity: README table (**Otters Civ. Revived** vs codename **Project OOGA**); slash-command explainer (“server” = game side that remembers money); `index.html` infobox codename row, World side heading, Chat commands section; installation note simplified; Mod Menu `fabric.mod.json` description.

- [2026-05-11] **README** restyle for GitHub: badges row, structured sections, tables, install + docs index; jargon moved to `DOCS/` / `AGENTS.md`.

- [2026-05-10] **`AGENTS.md` + `CLAUDE.md`**, `.gitattributes` (`*.html`, `*.mdc` LF), `.gitignore` (`.env*`, `.claude/cache/`); SUMMARY/LOCATIONS/README cross-links; CHANGELOG Added bullet.

- [2026-05-10] **Rules for `index.html` upkeep:** STYLE_GUIDE § Website parity (when to edit, sidebar hygiene, housekeeping); Cursor rule `.cursor/rules/index-html-parity.mdc` (alwaysApply); README contributors + SUMMARY quick link.

- [2026-05-10] **Docs + `index.html` sync:** README (join + per-id rewards + offline page), DOCS `SUMMARY`, `LOCATIONS`, `FEATURES`, `ARCHITECTURE`, `CHANGELOG`; website infobox/intro/commands note/see-also/contents rewards label.

- [2026-05-10] **`rewards.json` per-id payouts**: `blockRewards` / `entityRewards` maps (normalized ids, invalid keys skipped + warn); precedence over tag flat amounts; `RewardOrchestrator`, loader, `OtterCommand` hint, `index.html`, Modrinth description, `RewardRulesLoaderTest`.

- [2026-05-10] **`JoinWelcome`**: `ServerPlayConnectionEvents.JOIN` → three system messages (branding, `/otter` + `/money`, payouts pointer); registered from `OttersCivGameplay.register`.
- [2026-05-10] **`index.html`**: sticky left **On this page** sidebar (nested anchors, narrow layout + responsive stack), `#main` landmark, skip link, subsection ids for Features/Design goals/config files/`#see-also`, `aria-current` via `IntersectionObserver` + hash.
- [2026-05-10] **`index.html`** reference page: added requirements/stack table, wallet file format, bundled `currency_blocks` / `currency_mobs` defaults, full `rewards.json` field table (code defaults), config file quick reference, M0–M6 roadmap snapshot, meta description, infobox (artifact + pinned deps + issues), whitepaper/architecture links in references — no removal of prior copy.
- [2026-05-09] **`/otter`** help command (`OtterCommand`); **`LICENSE`** rewritten (ARR + carve-outs: video/montage, official JAR in mod packs, no source/fork reproduction without permission); README + `fabric.mod.json` description + `modrinth-description` + `CHANGELOG` synced.
- [2026-05-09] **Otters Civ. Revived** block-break + hostile-kill payouts shipped: `RewardOrchestrator`, `config/otters_civ_revived/rewards.json`, datapack tags `otters_civ_revived:*`, `WalletService.addBalance`, `JobsHooks` NO_OP; wired in `FpsMod`; docs `CHANGELOG` + `modrinth-description`.
- [2026-05-08] Imagery Optimizery GUI: optional per-file list (Add files / Add folder / Remove / Clear), table with input px+KB, output px, estimated PNG/JPG KB (in-memory encode); Optimize uses list if non-empty else full input folder; debounced estimates + generation guard for stale threads.
- [2026-05-08] [NOTE] Imagery Optimizery (`images/optimize-here/`): maintainer likes current GUI look/feel. Do **not** restyle, “simplify,” or re-theme the tkinter UI unless asked explicitly — prefer docs/behavior-only changes.
- [2026-05-08] Imagery Optimizery GUI: expanded “About” copy, grid layout for paths + browse, run bar (Clear log + hint + Optimize), output log section, footer path; primary action bottom-right of run strip above log.
- [2026-05-08] [AMENDED] Imagery Optimizery GUI: create `Tk()` before `StringVar`/`IntVar` (Python 3.14+ requires default root).
- [2026-05-08] Added Imagery Optimizery minimal tkinter GUI (`--gui`) + batch launcher branding; refactor shared `run_batch`/CLI.
- [2026-05-08] Fixed `images/optimize-here/optimize-pngs.bat` working directory (`cd /d "%~dp0"`); anchored `optimize_pngs.py` paths to script dir + PNG zlib level 9, default max width 960, progressive JPG.
- [2026-05-07] Recentered README on Project OOGA civ-mod identity and `/money` bootstrap usage.
- [2026-05-07] Implemented `/money` bootstrap command and wallet persistence; tracking in `DOCS/ROADMAP.md`.
- [2026-05-07] Created dedicated execution roadmap in `DOCS/ROADMAP.md`.
- [2026-05-07] Docs alignment pass for Project OOGA rule compliance.
- [2026-05-07] Whitepaper upgraded to premier civ-mod strategy document.
- [2026-05-07] Repo branding/remote migration to `Minecraft-Civ-Remixed`.

## Current Status
- Codebase still uses **fpsmod** mod id/package; branding for new civ gameplay is **Otters Civ. Revived** (`otters_civ_revived` config/datapack namespace).
- Server rewards: mined tagged blocks + direct-kill hostile mobs credit wallet (`addBalance`).
- Legacy FPS HUD remains an optional client-side extra.
- Core DOCS baseline files created for stable multi-agent handoff.
- Economy + help bootstrap: **`/otter`**, **`/money`**, **`/money set`** compile and build successfully.

## Last 5 Actions
1. Expanded repo `index.html` mod reference (configs, default tags, roadmap M0–M6, requirements).
2. Shipped `/otter` help, LICENSE carve-outs, README/modrinth/`fabric.mod.json` doc pass.
3. Implemented Otters Civ. mining/combat payouts, reward config + tags, changelog/modrinth copy.
4. Documented Imagery Optimizery in `SUMMARY` + `CHANGELOG`; SCRATCHPAD note to leave GUI as-is unless explicitly requested.
5. Imagery Optimizery tkinter GUI + `--gui`; batch opens GUI by default; shared `run_batch()` for CLI/UI.
6. Patched promo image optimizer batch + Python under `images/optimize-here/` for cwd-safe runs and smaller web outputs.
7. Rewrote `README.md` to center Project OOGA civ-mod scope and current command bootstrap.
8. Marked README recentering progress in `DOCS/ROADMAP.md` ASAP tracker.

[AMENDED 2026-05-10]: Renumbered backlog items after inserting the `index.html` line so the list stays ordered (section title unchanged).
- [AMENDED 2026-05-08]: Prior “last actions” still valid: server-safe `fabric.mod.json` environment `*`; implemented `WalletStore` / `FileWalletStore` / `WalletService`; registered `/money` and `/money set <player> <amount>` paths.

## Blockers
- None currently.

## Out-of-Scope Observations
- Java package/mod id still references `fpsmod`; full namespace migration not started.
- Existing feature docs describe FPS module behavior; civ module specs are in whitepaper only.

## Next Steps
1. Add permission gate for `/money set` so only operators/admins can mutate balances.
2. Add `/pay` and `/ooga money set|add|take` command surface.
3. Add immutable transaction log records for balance mutations.
4. Add transfer caps/cooldowns config for abuse control.
5. Mark completed M1 checklist items as they land.
